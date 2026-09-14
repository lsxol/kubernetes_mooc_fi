# Log output

Two small Java programs that together make up the log output exercise app. They run as two containers in one Deployment and talk to each other through a shared volume.

`writer/` becomes the image `lsxol/log-output:latest`. Every 5 seconds it appends a timestamp and a UUID to `/usr/src/app/files/log.txt`.

`reader/` becomes `lsxol/log-reader:latest`. It serves HTTP on port 8080 and shows the newest line from that file.

The naming is a bit confusing and I kept it as it is: the container called `log-output` in the Deployment is actually the writer, and `log-reader` is the reader. The folder names describe the roles better than the image names do.

Both are single-file Java programs the JDK runs directly (`java App.java` on eclipse-temurin:25-jdk-alpine). No build tool, nothing to compile first.

## What the reader shows

A GET on the root path gives you four lines:

```
file content: this text is from file
env variable: MESSAGE=hello world
2026-08-12T10:15:30.123Z: 3f2b1c8e-...
Ping / Pongs: 12
```

The first line is read from `/usr/src/app/config/information.txt`, which is mounted from the ConfigMap. The second is the `MESSAGE` key of the same ConfigMap, passed in as an environment variable. The third is the newest line the writer wrote to the shared file. The last one is fetched from whatever address sits in the `URI_PINGPONG` ConfigMap key, with `/count` added to the end; if ping-pong can't be reached, the reader logs the error and shows `0`.

## Manifests

Everything goes into the `exercises` namespace.

- `manifests/deployment.yaml` has both containers, the shared volume and the ConfigMap mounts
- `manifests/service.yaml` is a ClusterIP Service on port 2345, forwarding to 8080
- `manifests/configmap.yaml` holds `information.txt`, `MESSAGE` and `URI_PINGPONG`
- `manifests/gateway.yaml` is the Gateway, class `gke-l7-global-external-managed`, one HTTP listener on port 80
- `manifests/route.yaml` is the HTTPRoute attached to it: `/log_output` goes here, `/pingpong` goes to the ping-pong app, rewritten to `/` on the way
- `manifests/healthcheckpolicy.yaml` is a GKE HealthCheckPolicy for the ping-pong Service, see below

The shared `log.txt` lives on an `emptyDir` now, so it only has to survive as long as the pod does. The PersistentVolume and PersistentVolumeClaim in [shared/manifests](../shared/manifests) are what it used on the local k3d cluster; they're pinned to a node there and don't apply on GKE.

### Ingress to Gateway

Until exercise 3.2 the two paths were routed by an Ingress. In 3.3 it became a Gateway plus an HTTPRoute. The split is: the Gateway owns the load balancer and its listener, the HTTPRoute owns the path rules and can be changed without touching the Gateway. Both Services are plain ClusterIP, GKE sends traffic straight to the pods through network endpoint groups, so there's no need for NodePort anymore.

The load balancer runs its own health checks, and by default it asks for `/`. The reader answers on `/` with a page, but ping-pong's `/` increments the counter, so `healthcheckpolicy.yaml` points ping-pong's health check at `/count` instead. With the Ingress the path was inferred from the readinessProbe; with the Gateway it has to be said explicitly, in a policy object that references the Service by name in `targetRef`.

### Rewriting the path

Since 3.4 the ping-pong rule carries a `URLRewrite` filter with `ReplacePrefixMatch: /`. A request for `/pingpong` reaches the pod as `/`, and `/pingpong/count` as `/count`. The browser still sees `/pingpong`; the rewrite changes the request on its way to the backend, it isn't a redirect. The ping-pong app can therefore serve the root path and stay unaware of where the cluster publishes it. The reader doesn't need a rewrite: its handler is registered on `/` and Java's `HttpServer` matches every path under it, so `/log_output` works as is.

Two things in this folder talk to ping-pong directly, bypassing the Gateway and its rewrite, and use the app's own paths: the health check (`/count`) and the reader, whose `URI_PINGPONG` in the ConfigMap is now just `http://ping-pong-service:80` with `/count` appended in code.

Changes to the HTTPRoute show up on the cluster immediately but the Google load balancer takes a few minutes to pick them up. During that window the old rules are still served, which is easy to mistake for a broken manifest.

## Running it

Build and push both images:

```bash
docker build -t lsxol/log-output:latest log_output/writer
docker push lsxol/log-output:latest

docker build -t lsxol/log-reader:latest log_output/reader
docker push lsxol/log-reader:latest
```

The cluster needs the Gateway API enabled, GKE doesn't turn it on by default:

```bash
gcloud container clusters update dwk-cluster --zone europe-central2-b --gateway-api=standard
```

Create the namespace, it has no manifest of its own, then the app:

```bash
kubectl create namespace exercises
kubectl apply -f log_output/manifests/
```

The Gateway gets a public IP after a few minutes:

```bash
kubectl get gateway -n exercises
```

The app is then at `http://<that ip>/log_output`. Until the load balancer is fully programmed and the backends are healthy, the address answers `fault filter abort`; that's the load balancer talking, not the app.

## Things that caught me out

The Gateway and HTTPRoute have their own API group, `gateway.networking.k8s.io`. Writing `networking.k8s.io` out of Ingress habit fails with `no matches for kind "Gateway"`, which looks like the Gateway API is missing from the cluster when it isn't. The GKE class is `gke-l7-...` with a letter L, not the digit one.

In an HTTPRoute the path match is `type: PathPrefix`, not `pathType` like in Ingress, and it sits inside `path:`, indented one level deeper. Get the indentation wrong and the API rejects it with `unknown field "spec.rules[0].matches[0].type"`.

The `Ping / Pongs` line stays at `0` until ping-pong is actually running in the same namespace.

`MESSAGE` and `URI_PINGPONG` are environment variables, so editing the ConfigMap isn't enough on its own. The Deployment has to be restarted with `kubectl rollout restart deployment log-output -n exercises`. The mounted `information.txt` does refresh by itself, which makes this easy to misread.
