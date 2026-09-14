# Ping-pong

A Quarkus app that counts how many times you've asked it to play. The count is kept in PostgreSQL, so it survives pod restarts.

## Endpoints

`GET /` answers `pong 0`, then `pong 1`, and so on. It returns the current number and then bumps it by one.

`GET /count` just returns the number without changing anything. The log_output reader calls this one over the cluster network, so both apps have to be in the same namespace.

One thing worth knowing: `/` returns the number from *before* the increment. So after 5 requests the last answer was `pong 4`, but `/count` says `5`.

Since exercise 3.4 the app doesn't know it's published under `/pingpong`. It serves the root path, and the HTTPRoute in log_output rewrites `/pingpong` to `/` before the request reaches the pod. Up to 3.3 the prefix was hard-coded in `@Path("/pingpong")`, which meant the app, the test, the health check and log_output's ConfigMap all had to agree on the cluster URL. Now only the route does.

## Where the count lives

It's a single row in a `counter` table. Liquibase creates the table and inserts that row when the app starts, using `src/main/resources/db/changeLog.sql`. There's nothing to set up in the database by hand.

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
- `manifests/deployment.yaml` is the app
- `manifests/service.yaml` is a ClusterIP Service on port 80, forwarding to 8080

The routing for `/pingpong` isn't here. It lives in [log_output/manifests/route.yaml](../log_output/manifests/route.yaml), an HTTPRoute attached to the Gateway in the same folder, together with the rule for the log output app. The [HealthCheckPolicy](../log_output/manifests/healthcheckpolicy.yaml) that makes the load balancer check `/count` instead of `/` is there too, next to the Gateway it belongs to. The health check talks to the pod directly, so the rewrite doesn't apply to it, and it has to use the app's own paths. It must not use `/`: that endpoint increments the counter, and the load balancer would bump it every 15 seconds on its own.

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

Then apply the rest. The database should be up before the app, but the whole folder at once works fine too:

```bash
kubectl apply -f ping-pong/manifests/
```

It shows up at `http://<gateway ip>/pingpong`, where the IP comes from `kubectl get gateway -n exercises` once the Gateway from log_output is applied. The image tag is always `latest`, so after pushing a new build `kubectl apply` sees no change; use `kubectl rollout restart deployment ping-pong -n exercises` to get the new image running. See the [log_output README](../log_output/README.md) for enabling the Gateway API on the cluster.

## Development

```bash
./gradlew quarkusDev   # live coding, Dev Services gives you a database
./gradlew test         # also starts a Postgres container
```

Both need Docker running.
