# Interview Guide

## Project Explanation — 2 Minutes

> I worked on an Employee Management System deployed as a three-tier
> application on Amazon EKS. It's a React frontend talking to a Spring
> Boot REST API, backed by MySQL. The frontend and backend are both
> containerized with Docker — multi-stage builds so the final images are
> small — and deployed as separate Kubernetes Deployments in their own
> namespace.
>
> MySQL runs as a StatefulSet instead of a regular Deployment, because I
> needed stable storage and a stable network identity for the database —
> a Deployment doesn't guarantee either of those across restarts. The
> database's data directory is backed by a PersistentVolumeClaim, which
> EKS provisions as an EBS volume through the EBS CSI driver.
>
> All external traffic comes in through a single AWS Application Load
> Balancer, provisioned automatically by the AWS Load Balancer Controller
> reacting to a Kubernetes Ingress resource. The Ingress splits traffic by
> path — `/` goes to the frontend, `/api` goes to the backend — so I only
> need one load balancer for the whole app, and neither the backend nor
> the database is ever directly reachable from the internet.
>
> On top of that I added the production-style pieces you'd expect: HPAs
> that scale both tiers on CPU, PodDisruptionBudgets so node maintenance
> doesn't take down a whole tier at once, readiness/liveness/startup
> probes tuned for how long Spring Boot actually takes to boot, and
> NetworkPolicies that lock traffic down to frontend → backend → MySQL
> only — nothing else is allowed to talk to the database directly.
>
> Secrets and config are split — database credentials and the JWT secret
> live in a Kubernetes Secret, everything else non-sensitive is in a
> ConfigMap — and I never commit real secret values, only a placeholder
> template. Things I'd still add for a real production rollout are HTTPS
> via ACM, a custom domain through Route 53, monitoring, and a CI/CD
> pipeline — I scoped this build around the Kubernetes deployment itself
> first.

## Interview Questions and Answers

**1. Explain your project architecture.**
A three-tier app on EKS: React frontend and Spring Boot backend, each as
their own Deployment with a ClusterIP Service, and MySQL as a StatefulSet
with a PVC backed by EBS. One Ingress, backed by an ALB, routes `/` to the
frontend and `/api` to the backend. NetworkPolicies restrict traffic to
frontend→backend→MySQL only.

**2. Why did you choose EKS?**
EKS gives you a managed Kubernetes control plane on AWS, so I get native
integration with IAM (via IRSA), EBS for persistent storage, and the ALB
for ingress, without having to run and patch the control plane myself.

**3. Why did you use a StatefulSet for MySQL?**
MySQL needs a stable identity and stable storage across restarts. A
Deployment's Pods are interchangeable and don't guarantee you get the same
PVC back after a reschedule; a StatefulSet does, via
`volumeClaimTemplates` and a predictable Pod name (`mysql-0`).

**4. Why not use a Deployment for MySQL?**
Because a Deployment doesn't guarantee stable storage identity — if the
Pod is rescheduled, it could end up bound to a different PVC, and for a
single-writer relational database that's a correctness problem, not just
an inconvenience.

**5. Why is MySQL using EBS?**
EBS gives durable, block-level storage that persists independently of any
single Pod or node. The StatefulSet's PVC dynamically provisions an EBS
volume through the EBS CSI driver, so `/var/lib/mysql` survives Pod
restarts and rescheduling.

**6. What happens if the MySQL Pod restarts?**
Kubernetes reschedules `mysql-0` and reattaches the same PVC (same EBS
volume), so the data is intact. The readiness probe (`mysqladmin ping`)
keeps it out of rotation until MySQL has actually finished starting back
up.

**7. Why is the MySQL Service headless?**
A headless Service (`clusterIP: None`) is required for a StatefulSet's
governing Service — it gives each Pod a stable, individually addressable
DNS name instead of load-balancing across replicas, which matters for a
stateful, single-instance database.

**8. Why are the frontend/backend Services ClusterIP?**
Neither should be directly reachable from the internet. All external
access is meant to go through the single ALB Ingress, which routes to
these internal ClusterIP Services.

**9. Why isn't the backend exposed using a LoadBalancer Service?**
That would create a second, separate load balancer outside the Ingress
path — more cost, another public endpoint to secure, and it bypasses the
single-entry-point design the Ingress gives you.

**10. How does the ALB reach the frontend?**
The AWS Load Balancer Controller watches the Ingress, provisions an ALB
with `target-type: ip`, and registers the frontend Pods' IPs directly as
targets for the `/` path rule.

**11. How does `/api` reach the backend?**
The same Ingress has a path rule for `/api` pointing at `backend-service`
on port 8080; the ALB routes matching requests straight to backend Pod
IPs.

**12. What is the purpose of the Ingress?**
It's the single external entry point and routing layer — one resource
that maps URL paths to internal Services, instead of exposing multiple
Services externally.

**13. What is the AWS Load Balancer Controller?**
A controller that runs in the cluster (installed via Helm, using an
IAM-backed ServiceAccount) and watches Ingress/Service resources,
automatically provisioning and configuring the corresponding AWS ALB or
NLB resources.

**14. What is the purpose of ACM?**
AWS Certificate Manager issues and manages TLS certificates. In this
project it's a planned addition — the ALB would terminate HTTPS using an
ACM certificate rather than serving plain HTTP.

**15. How would Route 53 work in this architecture?**
Route 53 would host a DNS record (typically an alias record) pointing your
custom domain at the ALB's DNS name, so users hit
`app.yourdomain.com` instead of the raw ALB hostname.

**16. Why use HPA?**
To handle variable load without manual intervention — scale Pods up when
CPU usage rises, and back down once it settles, within defined bounds.

**17. What triggers the HPA?**
Average CPU utilization across the Deployment's Pods crossing 70% of the
requested CPU (`250m` for backend, `100m` for frontend) triggers a
scale-up; usage staying low for the 5-minute stabilization window triggers
scale-down.

**18. Why do you need Metrics Server?**
The HPA controller reads current CPU/memory usage from the Metrics API,
which Metrics Server provides. Without it, `kubectl top` and the HPA both
show `<unknown>` and can't make scaling decisions.

**19. Why use a PodDisruptionBudget?**
To make sure voluntary disruptions — node drains, cluster upgrades —
don't take out every replica of a tier at once. `minAvailable: 1` on both
tiers guarantees at least one Pod stays up.

**20. What does the NetworkPolicy protect?**
It restricts pod-to-pod traffic to only what the app needs: frontend can
reach backend on 8080, backend can reach MySQL on 3306, and nothing else
is implicitly allowed once a Pod is selected by a policy.

**21. How does the frontend communicate with the backend?**
Not directly by Kubernetes DNS — the React app runs in the user's browser,
which can't resolve internal cluster DNS names. It calls a relative `/api`
path, which the Ingress routes to `backend-service` inside the cluster.

**22. How does the backend communicate with MySQL?**
Via the headless `mysql` Service's DNS name
(`mysql.ems.svc.cluster.local`, or just `mysql` inside the namespace),
using a JDBC URL configured through the Deployment's environment
variables.

**23. How do you troubleshoot CrashLoopBackOff?**
`kubectl logs <pod> -n ems` (and `--previous` for the crashed instance's
logs) to see the actual exception, then `kubectl describe pod` for events.
Usually points to a bad environment variable, missing Secret/ConfigMap
key, or an unreachable dependency like MySQL.

**24. How do you troubleshoot ImagePullBackOff?**
Check `kubectl describe pod` events for the exact pull error, confirm the
image name/tag is correct and actually pushed to the registry, and add an
`imagePullSecret` if the registry is private.

**25. How do you troubleshoot Pending Pods?**
`kubectl describe pod` shows the scheduling reason — usually insufficient
node CPU/memory, or an unbound PVC the Pod is waiting on.

**26. How do you troubleshoot a Pending PVC?**
Check that the EBS CSI driver add-on is installed and its controller Pods
are running, and confirm the node IAM role has the
`AmazonEBSCSIDriverPolicy` attached — without both, dynamic provisioning
can't create the underlying EBS volume.

**27. How do you troubleshoot the ALB not being created?**
Check the AWS Load Balancer Controller's logs
(`kubectl logs -n kube-system deployment/aws-load-balancer-controller`) —
it will name the exact AWS API error, commonly an IAM permission gap or
missing subnet tags for ALB discovery.

**28. How do you troubleshoot a 502 from the ALB?**
Usually means the ALB reached a target but got no valid response — check
whether the target Pod is actually `Ready`, and look at its logs for a
crash right around the failed request's timestamp.

**29. How do you troubleshoot a backend 401?**
Confirm the JWT hasn't expired (24h default) and that the `Authorization:
Bearer <token>` header is actually being sent. If `JWT_SECRET` was
recently rotated, every previously issued token is invalid by design.

**30. How do you troubleshoot HPA showing `<unknown>`?**
That means Metrics Server isn't returning data — confirm it's running
(`kubectl get deployment metrics-server -n kube-system`) and that
`kubectl top pods` works at all before looking at the HPA itself.

**31. How do you troubleshoot the backend being unable to connect to MySQL?**
Check the backend logs for the JDBC error, confirm the `mysql` Service
resolves and is reachable (`nc -zv mysql 3306` from a debug pod inside the
namespace), and check whether `mysql-network-policy` actually allows
ingress from Pods labeled `app: backend`.

**32. How do you troubleshoot NetworkPolicy blocking traffic?**
Compare the exact Pod labels against the policy's `podSelector` and
`matchLabels` — a typo or missing label is the most common cause. Remember
NetworkPolicies are default-deny once a Pod is selected by any policy, so
anything not explicitly allowed is blocked.

**33. How would you handle secrets in production?**
Move off plain Kubernetes Secrets (which are only base64-encoded, not
encrypted at rest by default) to AWS Secrets Manager or the External
Secrets Operator, and rotate the JWT secret and DB credentials on a
schedule.

**34. How would you perform a zero-downtime deployment?**
The Deployments already use `RollingUpdate` with `maxUnavailable: 0` and
`maxSurge: 1`, so a new Pod comes up and passes its readiness probe before
an old one is removed — combined with the PDB, this keeps capacity
available throughout the rollout.

**35. How would you scale this architecture further?**
Beyond the existing HPA range, I'd look at moving MySQL to Amazon RDS
(offloading stateful storage concerns from the cluster entirely), adding
read replicas if read load grows, and considering multi-AZ node groups for
better fault tolerance — all listed under
[Future Improvements](../README.md#future-improvements).
