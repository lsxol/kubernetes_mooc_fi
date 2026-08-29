# Ping-pong

A Quarkus app that counts how many times you've asked it to play. The count is kept in PostgreSQL, so it survives pod restarts.

## Endpoints

`GET /pingpong` answers `pong 0`, then `pong 1`, and so on. It returns the current number and then bumps it by one.

`GET /pingpong/count` just returns the number without changing anything. The log_output reader calls this one over the cluster network, so both apps have to be in the same namespace.

One thing worth knowing: `/pingpong` returns the number from *before* the increment. So after 5 requests the last answer was `pong 4`, but `/count` says `5`.

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
- `manifests/service.yaml` is a ClusterIP Service on port 2345, forwarding to 8080

The Ingress rule for `/pingpong` isn't here. It lives in [log_output/manifests/ingress.yaml](../log_output/manifests/ingress.yaml), together with the rule for the log output app.

## Running it

Namespace and password first. Without the Secret both pods get stuck in `CreateContainerConfigError`:

```bash
kubectl create namespace exercises
kubectl apply -f ping-pong/manifests/secret.yaml
```

I left a dummy password in that Secret on purpose. This is a public course repo and the database only ever runs on my local k3d cluster. Don't copy this anywhere real: `kind: Secret` only base64-encodes the value, it doesn't encrypt anything, and once it's committed it stays in the git history.

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

It shows up at <http://localhost:8001/pingpong>. My k3d cluster maps the ingress to port 8001.

## Development

```bash
./gradlew quarkusDev   # live coding, Dev Services gives you a database
./gradlew test         # also starts a Postgres container
```

Both need Docker running.
