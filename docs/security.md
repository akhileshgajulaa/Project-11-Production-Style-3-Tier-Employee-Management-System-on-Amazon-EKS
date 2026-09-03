# Security

## Implemented

- **Kubernetes Secrets for credentials.** `MYSQL_ROOT_PASSWORD`,
  `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` are stored in a `Secret`
  (`k8s/02-secret.yaml`, git-ignored), never hardcoded in application code
  or checked into version control. See
  [k8s/02-secret.yaml.example](../k8s/02-secret.yaml.example) for the
  placeholder template.
- **NetworkPolicy segmentation.** Backend Pods only accept traffic from
  frontend Pods on port 8080; MySQL Pods only accept traffic from backend
  Pods on port 3306. See
  [k8s/15-network-policy.yaml](../k8s/15-network-policy.yaml).
- **Non-root backend container.** The backend image runs as a dedicated
  unprivileged user (`emsuser`, uid 1001), not root — see
  [backend/Dockerfile](../backend/Dockerfile).
- **Internal-only backend and database.** `backend-service` and `mysql`
  are both `ClusterIP` — never directly reachable from outside the
  cluster. All external traffic enters through the single ALB Ingress.
- **Resource limits.** CPU and memory limits on every container prevent
  one Pod from starving others sharing a node.
- **Startup/readiness/liveness probes.** Reduce the chance of serving
  traffic to a Pod that isn't actually healthy.
- **Least exposure via Ingress path routing.** Only `/` and `/api` are
  routed anywhere; there's no direct external path to internal services.
- **Application-layer security (from the app itself, not Kubernetes):**
  BCrypt password hashing, stateless JWT authentication, an explicit CORS
  allow-list, and `@PreAuthorize`-enforced role checks for
  admin-only operations. See the backend's `SecurityConfig` and
  `JwtAuthFilter`.

## Recommended production improvements

These are **not** implemented in this repository — they're documented here
so anyone deploying this for real traffic knows what's still missing.

- **A managed secret store.** Replace the plain Kubernetes Secret with AWS
  Secrets Manager or the External Secrets Operator, so credentials are
  never stored as base64-encoded (not encrypted) Kubernetes objects and
  can be rotated centrally.
- **HTTPS end-to-end.** Terminate TLS at the ALB using an ACM certificate,
  and redirect all HTTP traffic to HTTPS. See
  [Future Improvements](../README.md#future-improvements).
- **Restrict `CORS_ALLOWED_ORIGINS`.** The current ConfigMap value is `*`
  for convenience during development — this should be locked to the
  actual frontend origin(s) before going live.
- **Encrypt the EBS volume backing MySQL** at rest, and enable automated
  snapshots/backups.
- **Pod Security Standards / restricted `securityContext`** (e.g.
  `runAsNonRoot: true`, dropped Linux capabilities, read-only root
  filesystem) on all Deployments, not just the backend image's own
  non-root user.
- **Network-level restriction of the AWS Load Balancer Controller's IAM
  policy** to the minimum required actions/resources, reviewed
  periodically.
- **Centralized audit logging** for `kubectl` access and AWS API calls
  (CloudTrail) touching this cluster.

## Never commit real secrets

Never commit real credentials, tokens, or private keys to this
repository. `k8s/02-secret.yaml` is intentionally excluded via
`.gitignore` — always work from `k8s/02-secret.yaml.example` and keep the
real file local, or better, move secret management to AWS Secrets Manager
or the External Secrets Operator for anything beyond a personal lab.
