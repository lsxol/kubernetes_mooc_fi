# Ping-pong

A Quarkus app that counts how many times you've asked it to play. The count is kept in PostgreSQL, so it survives pod restarts.

## Endpoints

`GET /` answers `pong 0`, then `pong 1`, and so on. It returns the current number and then bumps it by one.

`GET /count` just returns the number without changing anything. The log_output reader calls this one over the cluster network, so both apps have to be in the same namespace.

One thing worth knowing: `/` returns the number from *before* the increment. So after 5 requests the last answer was `pong 4`, but `/count` says `5`.

Since exercise 3.4 the app doesn't know it's published under `/pingpong`. It serves the root path, and the HTTPRoute in log_output rewrites `/pingpong` to `/` before the request reaches the pod. Up to 3.3 the prefix was hard-coded in `@Path("/pingpong")`, which meant the app, the test, the health check and log_output's ConfigMap all had to agree on the cluster URL. Now only the route does.

## Where the count lives

It's a single row in a `counter` table. Liquibase creates the table and inserts that row, using `src/main/resources/db/changeLog.sql`. In tests and dev mode that happens when the app starts. In the cluster it happens on the first readiness check that can reach the database (`DatabaseReadiness.java`), so the pod starts and waits at `0/1` instead of crashing when Postgres isn't up yet. `GET /healthz` answers 200 only once the database is reachable and migrated, and that's what the Deployment's readinessProbe checks. There's nothing to set up in the database by hand.

Postgres itself runs as a StatefulSet with a volumeClaimTemplate, next to a headless Service (`clusterIP: None`) so the pod always has the same name to connect to.

## Config

- `PORT` sets the HTTP port, defaults to 8080
- `POSTGRES_HOST` is set in the Deployment to `postgresql-svc`
- `POSTGRES_PASSWORD` comes from the Secret `postgresql-secrets`, key `POSTGRES_PASSWORD_PINGPONG`

The datasource is only configured under the `%prod` profile, so tests and dev mode never touch the real database. Quarkus Dev Services starts a throwaway Postgres container instead, which means you need Docker running.

## Manifests

Everything goes into the `exercises` namespace.

- `manifests/secret.yaml` is the Secret with the database password
- `manifests/postgresql.yaml` has the StatefulSet and its headless Service
- `manifests/deployment.yaml` is the app. Since 4.4 it is an Argo Rollouts `Rollout`, not a `Deployment`, see below
- `manifests/analysistemplate.yaml` is the AnalysisTemplate the Rollout runs during an update
- `manifests/service.yaml` is a ClusterIP Service on port 80, forwarding to 8080

The routing for `/pingpong` isn't here. It lives in [log_output/manifests/route.yaml](../log_output/manifests/route.yaml), an HTTPRoute attached to the Gateway in the same folder, together with the rule for the log output app. The [HealthCheckPolicy](../log_output/manifests/healthcheckpolicy.yaml) that makes the load balancer check `/count` instead of `/` is there too, next to the Gateway it belongs to. The health check talks to the pod directly, so the rewrite doesn't apply to it, and it has to use the app's own paths. It must not use `/`: that endpoint increments the counter, and the load balancer would bump it every 15 seconds on its own.

## Canary updates

The app is updated as a canary by [Argo Rollouts](https://argoproj.github.io/argo-rollouts/). On every change to the pod template the Rollout first moves half of the replicas to the new version (`setWeight: 50`), then runs the `cpu-usage` AnalysisTemplate before going any further.

The analysis asks Prometheus once a minute, five times, for the CPU used by all containers in the namespace:

```
scalar(sum(rate(container_cpu_usage_seconds_total{namespace="exercises", container!=""}[2m])))
```

If any of those five answers is 0.3 cores or more, the analysis fails, the Rollout aborts and the old version keeps running. Normally the whole namespace uses about 0.001 cores, and starting all the JVMs after a reboot peaked at about 0.1, so 0.3 lets a normal update through and stops one that makes the CPU use jump.

Two details in the query: Prometheus scrapes every minute here, so a `rate` window shorter than `[2m]` has fewer than two samples and returns nothing. And `container!=""` drops the per-pod series that cAdvisor reports next to the per-container ones, which would otherwise double the sum.

Setting the threshold to something below the normal usage, like `result < 0.0001`, makes every update fail its first measurement, so the app is never updated.

## Running it

Namespace and password first. Without the Secret both pods get stuck in `CreateContainerConfigError`:

```bash
kubectl create namespace exercises
kubectl apply -f ping-pong/manifests/secret.yaml
```

I left a dummy password in that Secret on purpose. This is a public course repo and the database only ever runs on a throwaway course cluster. Don't copy this anywhere real: `kind: Secret` only base64-encodes the value, it doesn't encrypt anything, and once it's committed it stays in the git history.

Worth remembering too: Postgres only picks up the password the first time it sets up its data directory. Changing it in the Secret afterwards does nothing to a database that already exists. You'd have to delete the StatefulSet and its PVC so initdb runs again.

Then build and push the image:

```bash
./gradlew build
docker build -f src/main/docker/Dockerfile.jvm -t lsxol/ping-pong:latest .
docker push lsxol/ping-pong:latest
```

Argo Rollouts has to be in the cluster before the Rollout and the AnalysisTemplate can be applied, and the analysis expects the Prometheus Helm release `prom` in the `monitoring` namespace:

```bash
kubectl create namespace argo-rollouts
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
```

Then apply the rest. The database should be up before the app, but the whole folder at once works fine too:

```bash
kubectl apply -f ping-pong/manifests/
```

It shows up at `http://<gateway ip>/pingpong`, where the IP comes from `kubectl get gateway -n exercises` once the Gateway from log_output is applied. The image tag is always `latest`, so after pushing a new build `kubectl apply` sees no change. Any change to the pod template starts a new canary, for example a new annotation:

```bash
kubectl -n exercises patch rollout ping-pong --type merge -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"redeploy\":\"$(date +%s)\"}}}}}"
kubectl get rollout,analysisrun -n exercises -w
```

See the [log_output README](../log_output/README.md) for enabling the Gateway API on the cluster.

## Development

```bash
./gradlew quarkusDev   # live coding, Dev Services gives you a database
./gradlew test         # also starts a Postgres container
```

Both need Docker running.
