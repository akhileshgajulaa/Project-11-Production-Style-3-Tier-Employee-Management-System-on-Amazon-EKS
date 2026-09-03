# Deployment Guide

This walks through deploying the Employee Management System to Amazon EKS,
following the same sequence that was actually used during implementation:
**Namespace → Secret → ConfigMap → MySQL → Backend → Frontend → HPA → PDB →
Ingress/ALB → NetworkPolicy → verification.**

> HTTPS/ACM, Route 53, monitoring, and CI/CD were part of the original
> planning outline but were **not** carried through into the implementation
> steps below. They're listed as [future improvements](../README.md#future-improvements)
> rather than documented here as if they exist.

## Phase 1 — Prerequisites

You need, before starting:

- An AWS account with permissions to manage EKS, IAM, and EC2
- AWS CLI, configured (`aws sts get-caller-identity` should succeed)
- `kubectl`, pointed at your EKS cluster
- `eksctl` (used later to create an IAM ServiceAccount)
- `helm` v3 (used to install the AWS Load Balancer Controller)
- Docker, for building the backend and frontend images
- An existing Amazon EKS cluster with a working node group
- A container registry — Docker Hub or Amazon ECR — to push images to
- The **Metrics Server** running in the cluster (required for the HPAs;
  most managed EKS clusters ship this by default — verify with
  `kubectl get deployment metrics-server -n kube-system`)
- The **Amazon EBS CSI driver** add-on (required for MySQL's persistent
  volume — see Phase 3, Step 4)

## Phase 2 — Application container images

### 2.1 Backend image

```bash
cd backend
docker build -t <your-registry>/employee-management-backend:1.0 .
docker push <your-registry>/employee-management-backend:1.0
```

The [`backend/Dockerfile`](../backend/Dockerfile) is a two-stage build:
Maven builds the jar in a `maven:3.9-eclipse-temurin-17` stage, then the
final image copies just the jar into a minimal `eclipse-temurin:17-jre`
image and runs it as a non-root user (`emsuser`, uid 1001) on port 8080.

### 2.2 Frontend image

```bash
cd frontend
docker build -t <your-registry>/employee-management-frontend:1.0 .
docker push <your-registry>/employee-management-frontend:1.0
```

The [`frontend/Dockerfile`](../frontend/Dockerfile) builds the Vite
production bundle in a `node:20-alpine` stage, then serves the static
files with `nginx:alpine` on port 80, using
[`frontend/nginx.conf`](../frontend/nginx.conf).

### 2.3 Update the manifests

Edit the `image:` line in both:
- [`k8s/07-backend-deployment.yaml`](../k8s/07-backend-deployment.yaml)
- [`k8s/11-frontend-deployment.yaml`](../k8s/11-frontend-deployment.yaml)

to point at the images you just pushed. Avoid the `latest` tag.

## Phase 3 — Kubernetes deployment

Apply manifests **one at a time**, verifying each before moving on —
that's how this project was actually built and debugged.

### Step 1 — Namespace

```bash
kubectl apply -f k8s/01-namespace.yaml
kubectl get namespaces
```

Expect to see `ems` in `Active` status, and `kubectl get all -n ems` to
report no resources yet — that's correct at this point.

### Step 2 — Secret

```bash
cp k8s/02-secret.yaml.example k8s/02-secret.yaml
# edit k8s/02-secret.yaml and replace every <CHANGE_ME> placeholder
kubectl apply -f k8s/02-secret.yaml
kubectl get secrets -n ems
```

Expect `ems-secret` with `DATA: 4`. Kubernetes only shows key names here,
never the values.

### Step 3 — ConfigMap

```bash
kubectl apply -f k8s/03-configmap.yaml
kubectl get configmap ems-backend-config -n ems
```

### Step 4 — MySQL (Service + StatefulSet)

First, confirm the EBS CSI driver add-on is present — MySQL's
PersistentVolumeClaim can't bind without it:

```bash
aws eks list-addons --cluster-name <your-cluster-name>
```

If `aws-ebs-csi-driver` is missing, install it as a managed add-on and
attach the required IAM policy to your node role:

```bash
aws eks create-addon --cluster-name <your-cluster-name> --addon-name aws-ebs-csi-driver

# Find the node group and its IAM role
aws eks list-nodegroups --cluster-name <your-cluster-name>
aws eks describe-nodegroup --cluster-name <your-cluster-name> \
  --nodegroup-name <nodegroup-name> --query "nodegroup.nodeRole" --output text

# Attach the EBS CSI policy to that role
aws iam attach-role-policy \
  --role-name <node-instance-role-name> \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy

kubectl rollout restart deployment ebs-csi-controller -n kube-system
kubectl get pods -n kube-system | grep ebs
```

Then deploy MySQL itself:

```bash
kubectl apply -f k8s/04-mysql-service.yaml
kubectl apply -f k8s/05-mysql-statefulset.yaml

kubectl get pods -n ems              # mysql-0 should reach Running
kubectl get statefulset -n ems
kubectl get pvc -n ems               # should be Bound
kubectl logs mysql-0 -n ems
```

### Step 5 — Backend Deployment + Service

```bash
# sanity-check the Secret and ConfigMap keys exist first
kubectl get secret ems-secret -n ems -o jsonpath='{.data}'
kubectl get configmap ems-backend-config -n ems -o jsonpath='{.data}'

kubectl apply -f k8s/07-backend-deployment.yaml
kubectl apply -f k8s/06-backend-service.yaml

kubectl get deployment backend -n ems
kubectl get pods -n ems -l app=backend
kubectl logs -n ems -l app=backend
```

Verify the backend can be reached from inside the cluster and resolves DNS
correctly before moving on (e.g. with a temporary debug pod running `curl
backend-service:8080/actuator/health`).

### Step 6 — Frontend Deployment + Service

```bash
kubectl apply -f k8s/11-frontend-deployment.yaml
kubectl apply -f k8s/10-frontend-service.yaml

kubectl get deployment frontend -n ems
kubectl get pods -n ems -l app=frontend
```

At this point all three tiers exist internally — confirm with a debug pod
that `frontend-service` and `backend-service` both respond before exposing
anything externally.

### Step 7 — Backend HPA

```bash
kubectl get deployment metrics-server -n kube-system   # must be 1/1 first
kubectl top pods -n ems                                # sanity check

kubectl apply -f k8s/08-backend-hpa.yaml
kubectl get hpa -n ems
```

### Step 8 — Backend PDB

```bash
kubectl apply -f k8s/09-backend-pdb.yaml
kubectl get pdb -n ems
```

### Step 9 — Frontend HPA

```bash
kubectl apply -f k8s/12-frontend-hpa.yaml
kubectl get hpa -n ems
```

### Step 10 — Frontend PDB

```bash
kubectl apply -f k8s/13-frontend-pdb.yaml
kubectl get pdb -n ems
```

## Phase 4 — AWS Load Balancer Controller

The controller watches Ingress resources and provisions/manages the ALB.
This uses IRSA (IAM Roles for Service Accounts), which needs the cluster's
OIDC provider already associated (most `eksctl`-created clusters have this
by default).

```bash
export CLUSTER_NAME=<your-cluster-name>
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# 1. Confirm cluster + OIDC
aws eks describe-cluster --name $CLUSTER_NAME --query "cluster.status"
aws eks describe-cluster --name $CLUSTER_NAME \
  --query "cluster.identity.oidc.issuer" --output text

# 2. Create the IAM policy (skip if it already exists)
curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.14.1/docs/install/iam_policy.json
aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json

# 3. Create the IAM-backed ServiceAccount
eksctl create iamserviceaccount \
  --cluster=$CLUSTER_NAME \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --override-existing-serviceaccounts \
  --approve

kubectl get serviceaccount aws-load-balancer-controller -n kube-system

# 4. Install via Helm (serviceAccount.create=false — we already made it)
helm repo add eks https://aws.github.io/eks-charts
helm repo update eks

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=$CLUSTER_NAME \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller \
  --version 1.14.0

# 5. Verify
kubectl get deployment -n kube-system aws-load-balancer-controller   # expect 2/2
kubectl get ingressclass                                             # expect "alb"
```

## Phase 5 — Ingress (ALB)

```bash
kubectl apply -f k8s/14-ingress.yaml
kubectl get ingress -n ems
```

The `ADDRESS` column will be empty for a couple of minutes while the
controller provisions the ALB. If it stays empty, check
`kubectl logs -n kube-system deployment/aws-load-balancer-controller` — see
[troubleshooting.md](troubleshooting.md#alb-not-created--address-stays-empty).

Once populated:

```bash
export ALB_DNS=$(kubectl get ingress ems-ingress -n ems \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

curl http://$ALB_DNS         # frontend
curl http://$ALB_DNS/api     # backend
```

## Phase 6 — NetworkPolicy

```bash
kubectl apply -f k8s/15-network-policy.yaml
kubectl get networkpolicy -n ems
```

## Phase 7 — Final validation

```bash
kubectl get all -n ems
kubectl get ingress -n ems
kubectl get ingressclass
kubectl get networkpolicy -n ems
kubectl get nodes
kubectl get events -n ems --sort-by='.lastTimestamp'
```

At this point you should have: a `mysql-0` Pod backed by an EBS-backed PVC,
2 backend Pods and 2 frontend Pods (both eligible to scale to 5 under
load), an ALB serving traffic through a single Ingress, and NetworkPolicies
restricting traffic to the intended frontend → backend → MySQL path.

For what's next, see [Future Improvements](../README.md#future-improvements)
— HTTPS/ACM, Route 53, monitoring, and CI/CD were planned but not part of
this implementation.
