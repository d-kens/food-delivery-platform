# Infrastructure

| Folder (planned) | Contents |
|---|---|
| `docker/` | `docker-compose.yml` running all services, PostgreSQL, Redis, Kafka, Elasticsearch, Prometheus and Grafana locally |
| `helm/` | One Helm chart per service: deployment, service, probes, ConfigMap, Secret, HPA |
| `terraform/` | Provisions the Kubernetes cluster and supporting infrastructure |
| `argocd/` | Argo CD application definitions for GitOps deployment |

CI runs in GitHub Actions: build, test (including Testcontainers), and push one image per service.
