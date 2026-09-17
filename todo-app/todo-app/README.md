# todo-app

The project frontend. A Quarkus app that serves one HTML page with a random picture and a todo list, and passes the todo requests on to [todo-backend-app](../../todo-backend-app).

## Endpoints

`GET /` is the page itself, a static `index.html` from `src/main/resources/META-INF/resources`.

`GET /image` returns the picture. The app downloads one from `PICSUM_URL`, keeps it in a file and serves that file until it is older than `FRESH_TIMER` seconds, then fetches a new one.

`GET /todos` and `POST /todos` are forwarded to the backend at `TODO_BACKEND_URL`. The page only ever talks to this app, never to the backend directly. If the backend can't be reached the answer is `502`.

## Config

All of it comes from the ConfigMap `todo-app-configmap`, loaded with `envFrom`:

- `TODO_BACKEND_URL` is the backend Service inside the cluster, `http://todo-backend-app-service:2346`
- `PICSUM_URL` is where the picture comes from
- `IMAGE_PATH` is the file the picture is cached in, `/usr/src/app/files/image.jpg`
- `FRESH_TIMER` is how many seconds a picture stays fresh
- `PORT` sets the HTTP port, defaults to 8080

## Manifests

Everything goes into the `project` namespace.

- `manifests/namespace.yaml` creates the namespace
- `manifests/configmap.yaml` holds the config above
- `manifests/deployment.yaml` is the app, with an `emptyDir` mounted at `/usr/src/app/files` for the cached picture
- `manifests/service.yaml` is a ClusterIP Service on port 2345, forwarding to 8080
- `manifests/gateway.yaml` is the Gateway, class `gke-l7-global-external-managed`, one HTTP listener on port 80
- `manifests/route.yaml` is the HTTPRoute, a single rule sending `/` to this app
- `manifests/kustomization.yaml` lists the files above

There is only one route rule on purpose. The backend has no rule of its own, so it can't be reached from outside the cluster; the frontend calls it through its Service. Gateway and HTTPRoute are for traffic coming in, pods talk to each other through Services without them.

The load balancer's default health check asks for `/`, which this app answers with the page, so no HealthCheckPolicy is needed here.

Up to exercise 2.10 the picture lived on a local PersistentVolume pinned to a k3d node, and the app was exposed by an Ingress. Both are gone since 3.5: the picture is only a cache, so an `emptyDir` is enough, and the node the volume was pinned to doesn't exist on GKE.

## Running it

The whole project, frontend and backend together, is deployed from the repository root with Kustomize:

```bash
kubectl apply -k .
```

The root [`kustomization.yaml`](../../kustomization.yaml) points at this folder and at `todo-backend-app/manifests`, each with its own `kustomization.yaml`. `kubectl kustomize .` prints what would be applied.

The cluster needs the Gateway API enabled first, GKE leaves it off by default. Without it the apply stops at `no matches for kind "Gateway"`, while everything else is created fine:

```bash
gcloud container clusters update dwk-cluster --zone europe-central2-b --gateway-api=standard
```

The address shows up after a few minutes:

```bash
kubectl get gateway -n project
```

The app is at `http://<that ip>/`. Right after the Gateway reports `Programmed`, the address still answers with a Google 404 page for a few minutes, until the load balancer has picked up the route.

To build and push the image:

```bash
./gradlew build
docker build -f src/main/docker/Dockerfile.jvm -t lsxol/todo-app:latest .
docker push lsxol/todo-app:latest
```

The tag is always `latest`, so after a push use `kubectl rollout restart deployment todo-app -n project` to get the new image running.

## Development

```bash
./gradlew quarkusDev
```

In dev mode the backend is expected at `http://localhost:2346` and the picture is cached in `/tmp/image.jpg`.
