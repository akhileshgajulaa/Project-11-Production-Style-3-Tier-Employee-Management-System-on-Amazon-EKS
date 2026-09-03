# Production Features

Features actually implemented in this project's Kubernetes manifests, why
they were included, and how each contributes to running the app reliably.

## Multiple replicas (frontend + backend)

**What it does:** Both the `frontend` and `backend` Deployments run 2 Pods
by default.

**Why it's useful:** A single Pod is a single point of failure — if it
crashes or its node goes down, the app goes down with it.

**How it helps in production:** Traffic keeps flowing through the
remaining replica(s) while a failed Pod is replaced, and rolling updates
have somewhere to route traffic during a deploy.

## RollingUpdate strategy (`maxUnavailable: 0, maxSurge: 1`)

**What it does:** During a deployment rollout, Kubernetes brings up 1 new
Pod before removing an old one, and never drops below the full replica
count.

**Why it's useful:** Deploys a new version without a capacity dip or a
user-facing gap in availability.

**How it helps in production:** You can ship backend or frontend updates
during business hours without a maintenance window.

## Startup / readiness / liveness probes

**What it does:** `startupProbe` gives the app time to boot before other
probes kick in; `readinessProbe` controls whether a Pod receives traffic;
`livenessProbe` restarts a Pod that's stuck.

**Why it's useful:** Spring Boot can take longer than default probe
timeouts to finish starting up — without a `startupProbe`, Kubernetes
could kill a healthy-but-still-booting Pod in a restart loop.

**How it helps in production:** Traffic is only routed to Pods that are
actually ready, and Pods that hang (deadlock, OOM edge cases) get
automatically restarted instead of silently serving errors.

## Resource requests and limits

**What it does:** Every container declares a CPU/memory `request`
(guaranteed minimum) and `limit` (hard ceiling).

**Why it's useful:** Requests let the Kubernetes scheduler place Pods on
nodes that actually have capacity; limits stop one runaway Pod from
starving its neighbors.

**How it helps in production:** Requests are also what the HPA's
CPU-utilization percentage is measured against — without them, autoscaling
has no baseline.

## Horizontal Pod Autoscaler (HPA)

**What it does:** Scales `frontend` and `backend` between 2 and 5 replicas
based on average CPU utilization (target: 70%).

**Why it's useful:** Traffic isn't constant — this adds capacity
automatically under load and scales back down (gradually, over 5 minutes)
once load drops.

**How it helps in production:** Removes the need to manually watch traffic
and resize Deployments; the app absorbs reasonable spikes on its own.

## PodDisruptionBudget (PDB)

**What it does:** Guarantees at least 1 Pod of each tier (frontend,
backend) stays available during **voluntary** disruptions like node drains
or cluster upgrades.

**Why it's useful:** Kubernetes cluster maintenance (node upgrades, scaling
events) can otherwise evict multiple Pods of the same app at once if
they're co-located.

**How it helps in production:** Cluster operators can safely drain nodes
for maintenance without accidentally taking the whole frontend or backend
tier offline.

## NetworkPolicy

**What it does:** Restricts traffic to only frontend → backend (port 8080)
and backend → MySQL (port 3306); nothing else is allowed by default once a
policy targets a Pod.

**Why it's useful:** By default, any Pod in a Kubernetes cluster can reach
any other Pod. That's a much larger blast radius than this application
needs.

**How it helps in production:** Limits lateral movement if any single Pod
is compromised — a compromised frontend Pod, for example, cannot reach
MySQL directly.

## Persistent storage for MySQL (StatefulSet + PVC + EBS)

**What it does:** MySQL's data directory (`/var/lib/mysql`) is backed by a
20Gi `PersistentVolumeClaim`, dynamically provisioned as an Amazon EBS
volume, and bound to the same StatefulSet Pod identity (`mysql-0`) across
restarts.

**Why it's useful:** Pod restarts, rescheduling, or node replacement don't
wipe the database.

**How it helps in production:** Data survives routine Kubernetes
operations — it does not, by itself, protect against EBS volume loss or
AZ failure (see [Future Improvements](../README.md#future-improvements)
for backup/DR options).

## Namespace isolation

**What it does:** Every resource lives in the `ems` namespace.

**Why it's useful:** Keeps naming, RBAC, and resource quotas scoped
separately from other workloads sharing the cluster.

**How it helps in production:** Simplifies cleanup, access control, and
avoids naming collisions with unrelated apps on the same cluster.

## Secrets separated from ConfigMaps

**What it does:** Credentials (`ems-secret`) and non-sensitive config
(`ems-backend-config`) are two different objects, injected into containers
via `secretKeyRef` / `configMapKeyRef` rather than hardcoded.

**Why it's useful:** ConfigMaps are safe to check into version control;
Secrets are not — separating them makes that boundary explicit.

**How it helps in production:** Rotating a password only touches the
Secret, not application code or the image.

## AWS ALB Ingress (single external entry point)

**What it does:** One Ingress resource, backed by one ALB, routes both
`/` (frontend) and `/api` (backend) — the backend and frontend Services
themselves are never exposed externally (`ClusterIP` only).

**Why it's useful:** A single, managed load balancer is simpler to operate
and secure than exposing multiple `LoadBalancer`-type Services.

**How it helps in production:** Centralizes TLS termination, health
checks, and access logging at one layer (once HTTPS is added — see
[Future Improvements](../README.md#future-improvements)).
