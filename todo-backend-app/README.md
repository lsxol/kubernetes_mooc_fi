# todo-backend-app

The project backend. A Quarkus app that keeps the todos in PostgreSQL.

## Endpoints

`GET /todos` returns all todos as a JSON array of strings.

`POST /todos` takes the todo as `text/plain` in the body and stores it. Every request is logged. A todo longer than 140 characters is logged as an error and not stored.

## Where the todos live

A `todo` table in PostgreSQL. Liquibase creates the table and its sequence when the app starts, from `src/main/resources/db/changeLog.sql`.

Postgres runs as a StatefulSet with a volumeClaimTemplate, next to a headless Service so the pod has a stable name. The claim has no `storageClassName`, so it gets whatever the cluster's default is: `standard-rwo` on GKE, `local-path` on k3d. Naming `local-path` explicitly, as it was before 3.5, leaves the claim `Pending` on GKE.

## Config

- `PORT` sets the HTTP port, defaults to 8080
- `POSTGRES_HOST` is set in the Deployment to `postgresql-todo-svc`
- `POSTGRES_PASSWORD` comes from the Secret `postgresql-todo-secrets`, key `POSTGRES_PASSWORD_TODO`

The Secret in the repo holds a dummy password on purpose. This is a public course repo and the database only runs on a throwaway course cluster. `kind: Secret` only base64-encodes the value, don't copy this anywhere real.

## Manifests

Everything goes into the `project` namespace, which is created by [todo-app's manifests](../todo-app/todo-app/manifests/namespace.yaml).

- `manifests/secret.yaml` is the Secret with the database password
- `manifests/postgresql.yaml` has the StatefulSet and its headless Service
- `manifests/deployment.yaml` is the app
- `manifests/service.yaml` is a ClusterIP Service on port 2346, forwarding to 8080
- `manifests/cronjob.yaml` is a CronJob that every hour posts a todo "Read <random Wikipedia article>"
- `manifests/backupcronjob.yaml` is the nightly database backup, see below
- `manifests/kustomization.yaml` lists the files above

There is no Gateway rule for the backend. It is only reachable inside the cluster, by the frontend and by the CronJob, both through `todo-backend-app-service:2346`.

`monitoring/` holds the Helm values for the Prometheus, Loki, Grafana and k8s-monitoring charts used in part 2. They aren't part of the Kustomize deployment.

## Backups

`backupcronjob.yaml` runs `pg_dump` every night at midnight and copies the dump to the bucket `gs://lsxol-todo-backups` as `todo-backup-<date>.sql`. The job runs in a `google/cloud-sdk` image with `postgresql17-client` installed on the fly; the client has to be at least as new as the server, which is Postgres 17.

The pod authenticates to Google Cloud with Workload Identity, so there is no key anywhere. The Kubernetes ServiceAccount `todo-backup-sa` is annotated with the Google service account `todo-backup-gsa`, which has `roles/storage.objectAdmin` on the project and allows `project/todo-backup-sa` to impersonate it (`roles/iam.workloadIdentityUser`). The cluster needs Workload Identity enabled (`--workload-pool=<project>.svc.id.goog`). The annotation holds a `PROJECT_ID` placeholder that the deployment pipeline replaces with the real project ID, so the manifest is only meant to be applied through the pipeline.

To run a backup right away instead of waiting for midnight:

```bash
kubectl create job --from=cronjob/todo-backup-job backup-now -n project
kubectl logs -n project job/backup-now -f
```

## Running it

Frontend and backend are deployed together from the repository root:

```bash
kubectl apply -k .
```

See the [todo-app README](../todo-app/todo-app/README.md) for the Gateway API prerequisite and how to find the address.

The backend pod usually restarts once on a fresh deploy. It starts faster than Postgres, fails to connect, and comes up fine on the second try.

To build and push the image:

```bash
./gradlew build
docker build -f src/main/docker/Dockerfile.jvm -t lsxol/todo-backend-app:latest .
docker push lsxol/todo-backend-app:latest
```

Then `kubectl rollout restart deployment todo-backend-app -n project`, because the tag doesn't change.

## Development

```bash
./gradlew quarkusDev   # live coding, Dev Services gives you a database
./gradlew test         # also starts a Postgres container
```

Both need Docker running.
