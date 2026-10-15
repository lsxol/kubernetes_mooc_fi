# broadcaster

A Quarkus app that tells the outside world what happens to the todos. It listens to NATS and forwards every message to a webhook, using the "Generic" format from the exercise:

```json
{
  "user": "bot",
  "message": "A todo was created: Learn Kubernetes"
}
```

## Where the messages come from

[todo-backend-app](../todo-backend-app) publishes a JSON event on the NATS subject `todos` every time a todo is created or updated, for example `{"action": "updated", "id": 3, "value": "Learn Kubernetes", "done": true}`. The broadcaster turns it into one of:

- `A todo was created: <todo>`
- `A todo was marked done: <todo>`
- `A todo was marked not done: <todo>`

## Six replicas, one message

A plain NATS subscription gets every message on its subject, so six replicas subscribed to `todos` would send each message six times. The broadcaster subscribes in a **queue group** instead (`subscribe("todos", "broadcaster")`). NATS delivers each message to only one member of a queue group, so the replicas share the work and every message goes out once, whatever the replica count.

Core NATS without JetStream delivers at most once: a message published while no broadcaster is connected is simply lost. The exercise allows that ("a randomly missing message is not an issue but a duplicate is"), so nothing is stored or retried.

## Config

- `NATS_URL`, `nats://my-nats.nats.svc.cluster.local:4222` in the cluster, `nats://localhost:4222` by default
- `WEBHOOK_URL`, where the messages are POSTed. It comes from the ConfigMap `broadcaster-config`.

The app connects to NATS in the background, so it starts even when NATS is down and keeps reconnecting. `GET /healthz/ready` is `200` only while it is connected, so `kubectl rollout status` waits for NATS too. `GET /healthz/live` doesn't look at NATS: restarting the pod wouldn't bring NATS back.

## Manifests

- `manifests/deployment.yaml`, six replicas. No Service, nothing calls the broadcaster.
- `manifests/configmap.yaml` holds `WEBHOOK_URL`. For testing it points to [webhook.site](https://webhook.site), which shows every request it gets.

## Running it

NATS is installed once per cluster with Helm, in its own namespace, and every namespace uses the same server:

```bash
helm repo add nats https://nats-io.github.io/k8s/helm/charts/
helm repo update
helm upgrade --install my-nats nats/nats --namespace nats --create-namespace --set promExporter.enabled=true
```

The broadcaster is deployed by the pipeline together with the rest of the project, see the [todo-app README](../todo-app/todo-app/README.md). To check that there are no duplicates, add a todo and look at the logs of all replicas at once; exactly one of them should have sent it:

```bash
kubectl logs -n project -l app=broadcaster --prefix | grep Sent
```

## Development

```bash
docker run -d --name nats -p 4222:4222 nats:2
./gradlew quarkusDev
./gradlew test
```
