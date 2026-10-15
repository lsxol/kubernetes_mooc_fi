# todo-backend-app

The project backend. A Quarkus app that keeps the todos in PostgreSQL.

## Endpoints

`GET /todos` returns all todos as a JSON array of objects, `[{"id": 1, "value": "Learn Kubernetes", "done": false}, ...]`, ordered by `id`. Without the ordering Postgres tends to return a row that was just updated at the end of the list, so marking a todo done would make it jump to the bottom of the page. Up to 4.4 this was a plain array of strings.

`POST /todos` takes the todo as `text/plain` in the body and stores it, not done. Every request is logged. A todo longer than 140 characters is logged as an error and not stored.

`PUT /todos/<id>` takes `{"done": true}` (or `false`) as `application/json` and sets the todo's `done` field. It answers `200` with the updated todo, or `404` if there is no todo with that id.

`GET /healthz/ready` is the readiness probe. It is `200` only when the database is reachable and migrated (`DatabaseReadiness.java` plus the datasource check that `quarkus-smallrye-health` adds by itself). The frontend's readiness check calls this endpoint too.

`GET /healthz/live` is the liveness probe. It has no checks of its own, so it is `200` as long as the app answers at all. The database is left out on purpose: a database outage shouldn't make Kubernetes restart the backend in a loop.

## Where the todos live

A `todo` table in PostgreSQL. Liquibase creates the table and its sequence from `src/main/resources/db/changeLog.sql`. In tests and dev mode that happens when the app starts. In the cluster it happens on the first readiness check that can reach the database, so the app starts even when Postgres isn't up yet.

The `done` column came in 4.5 as a third changeset, `ALTER TABLE todo ADD COLUMN done BOOLEAN NOT NULL DEFAULT FALSE`, so a database that already has todos keeps them and they all start as not done. The earlier changesets must never be edited: Liquibase stores a checksum of every changeset it has run and refuses to migrate when one of them changes, which would leave the backend stuck at `0/1`.

## Messages to NATS

Since 4.6 every created or updated todo is also published to NATS, on the subject `todos`, as `{"action": "created" | "updated", "id": ..., "value": ..., "done": ...}`. The [broadcaster](../broadcaster) picks it up from there and forwards it to a chat webhook.

`TodoBackend` only fires a CDI event. `TodoPublisher` observes it with `@Observes(during = TransactionPhase.AFTER_SUCCESS)`, so the message goes out after the transaction has committed. A todo that failed to save is never announced.

NATS is optional for the backend. It connects in the background and keeps reconnecting, and while it isn't connected the todo is still saved and the message is skipped with a warning in the log. The exercise allows missing messages, only duplicates are a problem. NATS is not part of the readiness probe either, so a NATS outage doesn't take the todo list down.

## Where Postgres runs

Postgres runs as a StatefulSet with a volumeClaimTemplate, next to a headless Service so the pod has a stable name. The claim has no `storageClassName`, so it gets whatever the cluster's default is: `standard-rwo` on GKE, `local-path` on k3d. Naming `local-path` explicitly, as it was before 3.5, leaves the claim `Pending` on GKE.

## Config

- `PORT` sets the HTTP port, defaults to 8080
- `POSTGRES_HOST` is set in the Deployment to `postgresql-todo-svc`
- `POSTGRES_PASSWORD` comes from the Secret `postgresql-todo-secrets`, key `POSTGRES_PASSWORD_TODO`
- `NATS_URL` is set in the Deployment to `nats://my-nats.nats.svc.cluster.local:4222`, defaults to `nats://localhost:4222`

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

On a fresh deploy the backend usually starts before Postgres. Up to 4.1 that cost one restart; since 4.2 the pod just sits at `0/1` until the database answers.

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
