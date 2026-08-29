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
- `manifests/ingress.yaml` routes `/log_output` here and `/pingpong` to the ping-pong app
- `manifests/configmap.yaml` holds `information.txt`, `MESSAGE` and `URI_PINGPONG`

The PersistentVolume and PersistentVolumeClaim for the shared `log.txt` aren't here, they're in [shared/manifests](../shared/manifests). Ping-pong used to keep its counter file on the same claim, before that counter moved into PostgreSQL.

## Running it

Build and push both images:

```bash
docker build -t lsxol/log-output:latest log_output/writer
docker push lsxol/log-output:latest

docker build -t lsxol/log-reader:latest log_output/reader
docker push lsxol/log-reader:latest
```

Create the namespace and the storage. The namespace has no manifest of its own:

```bash
kubectl create namespace exercises
kubectl apply -f shared/manifests/persistentvolume.yaml
kubectl apply -f shared/manifests/persistentvolumeclaim.yaml
```

Then the app:

```bash
kubectl apply -f log_output/manifests/
```

It shows up at <http://localhost:8001/log_output>. My k3d cluster maps the ingress to port 8001.

## Things that caught me out

The PersistentVolume is a local volume pinned to `/mnt/data` on node `k3d-k3s-default-agent-0` through node affinity, so it only works on that k3d cluster.

The `Ping / Pongs` line stays at `0` until ping-pong is actually running in the same namespace.

`MESSAGE` and `URI_PINGPONG` are environment variables, so editing the ConfigMap isn't enough on its own. The Deployment has to be restarted with `kubectl rollout restart deployment log-output -n exercises`. The mounted `information.txt` does refresh by itself, which makes this easy to misread.
