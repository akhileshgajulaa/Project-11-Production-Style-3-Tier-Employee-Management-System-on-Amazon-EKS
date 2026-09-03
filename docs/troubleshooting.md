# Troubleshooting Guide

Practical scenarios for this project, organized by symptom. Each includes
the commands to run first and what to look for.

## Pod is in CrashLoopBackOff

**Symptoms:** `kubectl get pods -n ems` shows a Pod restarting repeatedly.

**Possible causes:** application error on startup, missing/incorrect
environment variable, failing liveness probe, unhandled exception during
Spring Boot startup.

**Commands to check:**
```bash
kubectl get pods -n ems
kubectl logs <pod> -n ems
kubectl logs <pod> -n ems --previous   # logs from the crashed instance
kubectl describe pod <pod> -n ems
```

**What to check:** Read the actual exception in the logs first — for the
backend, a common cause is a bad `SPRING_DATASOURCE_URL` or an unreachable
MySQL Service. Confirm the Secret and ConfigMap keys referenced in the
Deployment actually exist (`kubectl get secret ems-secret -n ems -o
jsonpath='{.data}'`).

## ImagePullBackOff / ErrImagePull

**Symptoms:** Pod stuck in `Pending`/`ImagePullBackOff`, never starts.

**Possible causes:** wrong image name/tag, image not pushed to the
registry, private registry without an `imagePullSecret`, typo in the
`image:` field.

**Commands to check:**
```bash
kubectl describe pod <pod> -n ems | grep -A5 Events
kubectl get deployment backend -n ems -o jsonpath='{.spec.template.spec.containers[0].image}'
```

**What to check:** Confirm the image and tag exist in your registry
(`docker pull <image>` locally as a sanity check) and that you replaced
`YOUR_DOCKERHUB_USERNAME/...` in the Deployment manifests before applying
them.

## Pod stuck in Pending

**Symptoms:** Pod never leaves `Pending`.

**Possible causes:** no node has enough CPU/memory to satisfy the Pod's
`resources.requests`, no nodes available, a PVC the Pod depends on hasn't
bound yet.

**Commands to check:**
```bash
kubectl describe pod <pod> -n ems
kubectl get nodes
kubectl top nodes
```

**What to check:** The `Events` section of `describe pod` almost always
names the exact reason (`Insufficient cpu`, `pod has unbound
immediate PersistentVolumeClaims`, etc.).

## PVC stuck Pending

**Symptoms:** `kubectl get pvc -n ems` shows `mysql-data-mysql-0` as
`Pending` instead of `Bound`.

**Possible causes:** the Amazon EBS CSI driver add-on isn't installed, or
the node IAM role is missing the `AmazonEBSCSIDriverPolicy`.

**Commands to check:**
```bash
kubectl get pvc -n ems
kubectl describe pvc mysql-data-mysql-0 -n ems
aws eks list-addons --cluster-name <your-cluster-name>
kubectl get pods -n kube-system | grep ebs
```

**What to check:** If `aws-ebs-csi-driver` is missing from
`list-addons`, follow the install steps in
[deployment-guide.md](deployment-guide.md#step-4--mysql-service--statefulset).
If the driver is installed but the PVC still won't bind, confirm the node
role has the EBS CSI IAM policy attached and restart the controller
(`kubectl rollout restart deployment ebs-csi-controller -n kube-system`).

## MySQL not starting

**Symptoms:** `mysql-0` Pod is `Running` but never reaches `Ready`, or
restarts.

**Possible causes:** wrong `MYSQL_ROOT_PASSWORD`/credentials, corrupted
data directory from an earlier failed start, resource limits too low.

**Commands to check:**
```bash
kubectl logs mysql-0 -n ems
kubectl describe pod mysql-0 -n ems
kubectl exec -it mysql-0 -n ems -- mysqladmin ping -h 127.0.0.1 -uroot -p
```

**What to check:** MySQL logs are explicit about auth failures and
initialization errors. If the PVC has stale/corrupted data from a previous
broken attempt, you may need to delete the PVC and let MySQL reinitialize
(this destroys existing data — only do this in a lab environment).

## Backend cannot connect to MySQL

**Symptoms:** Backend Pod logs show a JDBC connection failure or timeout on
startup.

**Possible causes:** `mysql` Service not reachable, wrong host/port in
`SPRING_DATASOURCE_URL`, MySQL not yet ready when the backend starts,
NetworkPolicy blocking the connection.

**Commands to check:**
```bash
kubectl logs -n ems -l app=backend
kubectl get svc mysql -n ems
kubectl exec -it <backend-pod> -n ems -- sh -c 'nc -zv mysql 3306'
kubectl get networkpolicy -n ems
```

**What to check:** Confirm the JDBC URL in
`07-backend-deployment.yaml` points at `mysql:3306` (matching the headless
Service name), and that `mysql-network-policy` allows ingress from Pods
labeled `app: backend`.

## Service has no endpoints

**Symptoms:** `kubectl get endpoints <service> -n ems` returns an empty
list even though Pods are `Running`.

**Possible causes:** Service `selector` doesn't match the Pod labels,
Pods aren't passing their readiness probe.

**Commands to check:**
```bash
kubectl get endpoints backend-service -n ems
kubectl get pods -n ems --show-labels
kubectl get svc backend-service -n ems -o jsonpath='{.spec.selector}'
```

**What to check:** The Service's `selector` (e.g. `app: backend`) must
exactly match the Pod template's `labels`. A Pod that's `Running` but not
`Ready` (failing its readiness probe) is also excluded from endpoints.

## Ingress ADDRESS stays empty / ALB not created

**Symptoms:** `kubectl get ingress -n ems` shows no value in the `ADDRESS`
column for several minutes.

**Possible causes:** AWS Load Balancer Controller isn't running, IAM
permissions are missing, the Ingress is missing required annotations, no
matching subnets are tagged for ALB discovery.

**Commands to check:**
```bash
kubectl get deployment -n kube-system aws-load-balancer-controller
kubectl logs -n kube-system deployment/aws-load-balancer-controller
kubectl describe ingress ems-ingress -n ems
kubectl get ingressclass
```

**What to check:** The controller logs will name the exact AWS API error
(commonly an IAM permission or missing subnet tag). Confirm
`kubectl get ingressclass` shows `alb`, confirming the controller
registered correctly.

## ALB target unhealthy

**Symptoms:** ALB target group in the AWS Console shows targets as
`unhealthy`.

**Possible causes:** health-check path doesn't return a 2xx/3xx, Pod isn't
actually ready, security group blocking the ALB's traffic to Pod IPs.

**Commands to check:**
```bash
kubectl describe ingress ems-ingress -n ems
kubectl get pods -n ems -o wide
kubectl logs -n ems -l app=frontend
```

**What to check:** The Ingress uses `alb.ingress.kubernetes.io/healthcheck-path:
/` and `success-codes: "200-399"` — confirm the frontend actually returns
that at `/`, and that the ALB's auto-managed security group can reach Pod
IPs on the target port.

## 502 Bad Gateway

**Symptoms:** ALB returns 502 for requests that should succeed.

**Possible causes:** target Pods not ready, Pod crashed after being marked
healthy, wrong `target-type` or port in the Ingress.

**Commands to check:**
```bash
kubectl get pods -n ems -l app=frontend
kubectl get pods -n ems -l app=backend
kubectl describe ingress ems-ingress -n ems
```

**What to check:** A 502 usually means the ALB reached a target but got no
valid response — check the target Pod's logs for a crash right around the
time of the failed request.

## 401 Unauthorized

**Symptoms:** API calls return 401 even with a token.

**Possible causes:** JWT expired (default 24h), `Authorization` header not
sent, `JWT_SECRET` mismatch between what signed the token and what's
configured now (e.g. after a Secret rotation without re-issuing tokens).

**Commands to check:**
```bash
kubectl get secret ems-secret -n ems -o jsonpath='{.data.JWT_SECRET}' | base64 -d
kubectl logs -n ems -l app=backend
```

**What to check:** Log in again to get a fresh token. If you recently
rotated `JWT_SECRET`, every previously issued token is now invalid by
design.

## HPA shows `<unknown>` for CPU

**Symptoms:** `kubectl get hpa -n ems` shows `<unknown>/70%` instead of an
actual percentage.

**Possible causes:** Metrics Server isn't running or isn't reachable.

**Commands to check:**
```bash
kubectl get deployment metrics-server -n kube-system
kubectl top pods -n ems
```

**What to check:** If `kubectl top pods` also fails with `error: Metrics
API not available`, Metrics Server needs to be installed/fixed before the
HPA can make scaling decisions — this blocks HPA regardless of how correct
the HPA manifest itself is.

## NetworkPolicy blocking traffic unexpectedly

**Symptoms:** A connection that should work (e.g. frontend → backend)
times out only after applying `15-network-policy.yaml`.

**Possible causes:** Pod labels don't match the policy's `podSelector`,
a needed port/protocol combination isn't listed, DNS egress wasn't
allowed (some clusters require an explicit egress rule for DNS on port
53).

**Commands to check:**
```bash
kubectl get pods -n ems --show-labels
kubectl describe networkpolicy backend-network-policy -n ems
kubectl describe networkpolicy mysql-network-policy -n ems
```

**What to check:** NetworkPolicies are additive but also exclusive once a
Pod is selected by any policy — traffic not explicitly allowed is denied.
Double check the exact label keys/values on both sides of each rule.

## DNS / service connectivity problems

**Symptoms:** A Pod can't resolve `mysql`, `backend-service`, or
`frontend-service` by name.

**Possible causes:** CoreDNS not healthy, querying the wrong short name
(missing namespace suffix from outside `ems`), NetworkPolicy blocking DNS
egress.

**Commands to check:**
```bash
kubectl get pods -n kube-system -l k8s-app=kube-dns
kubectl exec -it <pod> -n ems -- nslookup mysql
kubectl exec -it <pod> -n ems -- nslookup backend-service.ems.svc.cluster.local
```

## EBS CSI driver problems

**Symptoms:** PVCs won't bind; `ebs-csi-controller` Pods are not `Running`.

**Commands to check:**
```bash
kubectl get pods -n kube-system | grep ebs
kubectl describe pod <ebs-csi-controller-pod> -n kube-system
aws eks describe-addon --cluster-name <your-cluster-name> --addon-name aws-ebs-csi-driver
```

**What to check:** Confirm the add-on shows `ACTIVE` and the node IAM role
has `AmazonEBSCSIDriverPolicy` attached (see
[deployment-guide.md](deployment-guide.md#step-4--mysql-service--statefulset)).

## Pod readiness failures

**Symptoms:** Pod is `Running` but never becomes `Ready`; Service has no
endpoints for it.

**Commands to check:**
```bash
kubectl describe pod <pod> -n ems
kubectl logs <pod> -n ems
```

**What to check:** `describe pod` shows the exact readiness probe failure
(timeout, non-2xx response, connection refused). For the backend, this is
often a slow startup exceeding the `startupProbe` window under heavy load
on the node — consider raising `failureThreshold` or the node's available
CPU.

---

*This is a general troubleshooting reference for common Kubernetes
failure modes on this project's stack, not a record of specific incidents
during the original build.*
