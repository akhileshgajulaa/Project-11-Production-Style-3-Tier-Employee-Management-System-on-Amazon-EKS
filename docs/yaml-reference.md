# Kubernetes YAML Reference

This document explains **why each manifest exists and what it does** — not a
line-by-line walkthrough. All files live in [`k8s/`](../k8s) and are applied
in the numbered order shown below.

## Resource table

| File | Resource | Purpose |
|---|---|---|
| `01-namespace.yaml` | Namespace | Creates the isolated `ems` namespace |
| `02-secret.yaml.example` | Secret | Template for sensitive configuration (DB + JWT) |
| `03-configmap.yaml` | ConfigMap | Stores non-sensitive application configuration |
| `04-mysql-service.yaml` | Service (headless) | Stable network identity for the MySQL StatefulSet |
| `05-mysql-statefulset.yaml` | StatefulSet | Runs MySQL with stable identity and persistent storage |
| `06-backend-service.yaml` | Service | Internal ClusterIP access to backend Pods |
| `07-backend-deployment.yaml` | Deployment | Runs the Spring Boot backend replicas |
| `08-backend-hpa.yaml` | HorizontalPodAutoscaler | Automatically scales backend Pods on CPU |
| `09-backend-pdb.yaml` | PodDisruptionBudget | Protects backend availability during voluntary disruption |
| `10-frontend-service.yaml` | Service | Internal ClusterIP access to frontend Pods |
| `11-frontend-deployment.yaml` | Deployment | Runs the React/Nginx frontend replicas |
| `12-frontend-hpa.yaml` | HorizontalPodAutoscaler | Automatically scales frontend Pods on CPU |
| `13-frontend-pdb.yaml` | PodDisruptionBudget | Protects frontend availability during voluntary disruption |
| `14-ingress.yaml` | Ingress | Routes external traffic through the AWS ALB |
| `15-network-policy.yaml` | NetworkPolicy (x2) | Restricts frontend → backend → MySQL traffic only |

## 01-namespace.yaml

**Resource:** Namespace

**Purpose:** Creates an isolated Kubernetes namespace named `ems` for all
application resources.

**Why it is used:** Keeps the Employee Management System resources
logically separated from any other workloads running in the same EKS
cluster.

**Used by:** Every other resource in this project references
`namespace: ems`.

## 02-secret.yaml.example

**Resource:** Secret (`Opaque`)

**Purpose:** Holds the sensitive values MySQL, the backend, and JWT
authentication all need: `MYSQL_ROOT_PASSWORD`, `DB_USERNAME`,
`DB_PASSWORD`, `JWT_SECRET`.

**Why it is used:** Kubernetes Secrets keep credentials out of Pod specs
and ConfigMaps, and can be mounted as environment variables at runtime.

**Important:** This file is a **template** with `<CHANGE_ME>` placeholders.
Copy it to `02-secret.yaml` (already in `.gitignore`) and fill in real
values before applying — never commit the real file. See
[docs/security.md](security.md) for production alternatives.

**Used by:** `mysql` StatefulSet, `backend` Deployment.

## 03-configmap.yaml

**Resource:** ConfigMap

**Purpose:** Stores non-sensitive backend configuration — DB host/port/name,
server port, Hibernate DDL mode, JWT expiration, CORS origins, log levels.

**Why it is used:** Separates configuration that's safe to version-control
from configuration that must stay secret. Nothing in this file needs to be
redacted.

**Used by:** `backend` Deployment (via `configMapKeyRef`).

## 04-mysql-service.yaml

**Resource:** Service (headless, `clusterIP: None`)

**Purpose:** Gives the MySQL StatefulSet a stable DNS identity
(`mysql.ems.svc.cluster.local`) without load-balancing between replicas.

**Why it is used:** StatefulSets require a headless governing Service so
each Pod gets a predictable, stable network identity — important for a
database where you care which instance you're talking to.

**Used by:** `mysql` StatefulSet, backend Deployment's JDBC URL.

## 05-mysql-statefulset.yaml

**Resource:** StatefulSet

**Purpose:** Runs a single MySQL 8.0 Pod with stable storage and identity.

**Why a StatefulSet instead of a Deployment:** Deployments treat Pods as
interchangeable and don't guarantee stable storage across
rescheduling. A StatefulSet gives MySQL a stable Pod name (`mysql-0`) and
binds it to the **same** PersistentVolumeClaim every time it restarts, so
data isn't lost or shuffled.

**Key configuration concepts:**
- Credentials are injected from `ems-secret` (never hardcoded).
- `readinessProbe`/`livenessProbe` use `mysqladmin ping` to confirm MySQL
  is actually accepting connections, not just that the process is running.
- `volumeClaimTemplates` requests a 20Gi `ReadWriteOnce` volume, which EKS
  provisions as an Amazon EBS volume via the EBS CSI driver.

**Dependencies:** `04-mysql-service.yaml` (headless Service), `02-secret.yaml`
(credentials), the EBS CSI driver add-on (dynamic volume provisioning).

## 06-backend-service.yaml

**Resource:** Service (`ClusterIP`)

**Purpose:** Gives the backend Deployment a stable internal address
(`backend-service:8080`).

**Why ClusterIP and not LoadBalancer:** The backend should never be exposed
directly to the internet. All external traffic is meant to arrive through
the ALB Ingress, which routes `/api` requests to this Service internally.

**Used by:** `14-ingress.yaml`.

## 07-backend-deployment.yaml

**Resource:** Deployment

**Purpose:** Runs the Spring Boot backend as 2 replicas with a
zero-downtime rolling update strategy.

**Key configuration concepts:**
- `maxUnavailable: 0, maxSurge: 1` means Kubernetes always keeps the full
  replica count available during a rollout — a new Pod comes up before an
  old one is removed.
- Database and JWT credentials come from `ems-secret`; everything else
  (DB host, DDL mode, CORS, logging) comes from `ems-backend-config`.
- `startupProbe` gives Spring Boot up to 5 minutes (30 × 10s) to finish
  booting before readiness/liveness probes start affecting the Pod.
- Resource `requests`/`limits` make the Pod's actual CPU need visible to
  the HPA and prevent one Pod from starving others on the same node.

**⚠️ Before deploying:** replace `YOUR_DOCKERHUB_USERNAME/...` with your
actual image (Docker Hub or ECR). Never deploy with the `latest` tag.

**Dependencies:** `02-secret.yaml`, `03-configmap.yaml`, `04-mysql-service.yaml`.

## 08-backend-hpa.yaml

**Resource:** HorizontalPodAutoscaler (`autoscaling/v2`)

**Purpose:** Automatically scales the backend Deployment between 2 and 5
replicas based on CPU utilization.

**Why these numbers:** The backend requests `250m` CPU per Pod; a 70%
average-utilization target means Kubernetes adds Pods once usage crosses
roughly `175m` per Pod. Scale-up reacts immediately
(`stabilizationWindowSeconds: 0`); scale-down waits 5 minutes to avoid
flapping.

**Dependencies:** Requires the Metrics Server to be running in the cluster
— without it, HPA shows `<unknown>` for current CPU usage (see
[troubleshooting.md](troubleshooting.md)).

## 09-backend-pdb.yaml

**Resource:** PodDisruptionBudget

**Purpose:** Guarantees at least 1 backend Pod stays available during
**voluntary** disruptions — node drains, cluster upgrades, `kubectl
drain` — as opposed to unexpected crashes.

**Why it matters:** Without a PDB, a node drain could take down both
backend replicas simultaneously if they happen to be co-located.

## 10-frontend-service.yaml

**Resource:** Service (`ClusterIP`)

**Purpose:** Gives the frontend Deployment a stable internal address
(`frontend-service:80`), the same pattern as the backend Service.

**Used by:** `14-ingress.yaml`.

## 11-frontend-deployment.yaml

**Resource:** Deployment

**Purpose:** Runs the Nginx-served React build as 2 replicas.

**Key configuration concepts:**
- Same zero-downtime `RollingUpdate` strategy as the backend.
- Probes hit `/` over HTTP rather than a TCP socket, since Nginx serving
  the SPA's `index.html` is a meaningful readiness signal.
- No backend URL is injected here — see the note below.

**Why the frontend doesn't reference `backend-service` directly:** the
React app executes in the visitor's browser, which cannot resolve internal
Kubernetes DNS names. It calls a relative `/api` path, and the Ingress
routes that to the backend Service inside the cluster. See
[architecture.md](architecture.md#request-flow) for the full explanation.

**⚠️ Before deploying:** replace `YOUR_DOCKERHUB_USERNAME/...` with your
actual frontend image.

## 12-frontend-hpa.yaml

**Resource:** HorizontalPodAutoscaler

**Purpose:** Identical scaling strategy to the backend HPA (2–5 replicas,
70% CPU target), applied to the frontend Deployment.

## 13-frontend-pdb.yaml

**Resource:** PodDisruptionBudget

**Purpose:** Same protection as the backend PDB — keeps at least 1 frontend
Pod available during voluntary disruptions.

## 14-ingress.yaml

**Resource:** Ingress (`networking.k8s.io/v1`, `ingressClassName: alb`)

**Purpose:** The single entry point for all external traffic. Routes `/api`
to `backend-service:8080` and everything else (`/`) to
`frontend-service:80`.

**Why it works this way:** The `alb.ingress.kubernetes.io/*` annotations
tell the **AWS Load Balancer Controller** (installed separately — see
[deployment-guide.md](deployment-guide.md#phase-4--aws-load-balancer-controller))
to provision and configure an internet-facing Application Load Balancer
that targets Pod IPs directly (`target-type: ip`).

**A note on source material:** the original implementation guide pasted
this Ingress from a template and left `namespace: three-tier` /
`name: three-tier-ingress` in the YAML, while every verification command
later in the same guide checks `ems-ingress` in the `ems` namespace. This
repository's `14-ingress.yaml` uses `ems` / `ems-ingress` to match the
namespace used by every other resource and the commands that were actually
run against the cluster.

**Dependencies:** AWS Load Balancer Controller installed in `kube-system`,
`06-backend-service.yaml`, `10-frontend-service.yaml`.

## 15-network-policy.yaml

**Resource:** NetworkPolicy (two policies in one file)

**Purpose:** Locks down pod-to-pod traffic to only the paths the
application actually needs:
- `backend-network-policy`: backend Pods only accept ingress from frontend
  Pods on port 8080, and may only initiate egress to MySQL Pods on port
  3306.
- `mysql-network-policy`: MySQL Pods only accept ingress from backend Pods
  on port 3306.

**Why it matters:** By default, Kubernetes allows all pod-to-pod traffic
within a cluster. These policies enforce the intended three-tier
boundary — a compromised frontend Pod, for example, cannot reach MySQL
directly.

**Dependencies:** Requires a CNI plugin that enforces NetworkPolicy (the
Amazon VPC CNI supports this when configured with the appropriate add-on).
