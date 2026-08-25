# Log output

Two small plain-Java applications that together form the `log-output` exercise app. They run as **two containers in a single Deployment** and communicate through a shared volume.

## Structure

| Directory | Image | Role |
|---|---|---|
| `writer/` | `lsxol/log-output:latest` | Every 5 seconds appends `<timestamp>: <uuid>` to `/usr/src/app/files/log.txt` |
| `reader/` | `lsxol/log-reader:latest` | HTTP server on port 8080 that reports the newest log line |

> Note the naming: the container called `log-output` in the Deployment is the **writer**, and the `log-reader` container is the reader. The directory names (`writer/`, `reader/`) describe the roles more accurately than the image names do.

Both are single-file Java programs run directly by the JDK (`java App.java`, `eclipse-temurin:25-jdk-alpine`) — there is no build tool and nothing to compile beforehand.

## What the reader returns

A `GET` to the root path produces four lines:

```
file content: this text is from file
env variable: MESSAGE=hello world
2026-08-12T10:15:30.123Z: 3f2b1c8e-...
Ping / Pongs: 12
```

- **file content** — read from `/usr/src/app/config/information.txt`, mounted from the ConfigMap
- **env variable** — the `MESSAGE` key of the same ConfigMap, injected as an environment variable
- **third line** — the newest entry written by the writer through the shared volume
- **Ping / Pongs** — fetched over the cluster network from `http://ping-pong-service:2345/pingpong/count`; falls back to `0` when ping-pong is unreachable

## Manifests

All resources live in the `exercises` namespace.

| File | Resource |
|---|---|
| `manifests/deployment.yaml` | Deployment `log-output` — both containers, shared volume, ConfigMap mounts |
| `manifests/service.yaml` | ClusterIP Service `log-output-service`, port 2345 to container port 8080 |
| `manifests/ingress.yaml` | Ingress routing `/log_output` here **and** `/pingpong` to the ping-pong app |
| `manifests/configmap.yaml` | ConfigMap `log-output-configmap` — `information.txt` and `MESSAGE` |

The PersistentVolume and PersistentVolumeClaim used for the shared `log.txt` are not here — they are kept in [`shared/manifests`](../shared/manifests) because ping-pong mounts the same claim.

## Starting it up

Build and push both images:

```bash
docker build -t lsxol/log-output:latest log_output/writer
docker push lsxol/log-output:latest

docker build -t lsxol/log-reader:latest log_output/reader
docker push lsxol/log-reader:latest
```

Create the namespace and the storage (the namespace has no manifest of its own):

```bash
kubectl create namespace exercises
kubectl apply -f shared/manifests/persistentvolume.yaml
kubectl apply -f shared/manifests/persistentvolumeclaim.yaml
```

Deploy the application:

```bash
kubectl apply -f log_output/manifests/
```

Open it at <http://localhost:8001/log_output> (local k3d cluster with the ingress port mapped to 8001).

## Notes

- The PersistentVolume is a `local` volume bound to `/mnt/data` on node `k3d-k3s-default-agent-0` via node affinity, so it only works on that k3d cluster.
- The `Ping / Pongs` line stays at `0` until the ping-pong application is running in the same namespace.
- After editing the ConfigMap, restart the Deployment so the `MESSAGE` environment variable is re-read: `kubectl rollout restart deployment log-output -n exercises`.
