# DevOps with Kubernetes 2026

My solutions to the exercises of the University of Helsinki course [DevOps with Kubernetes](https://devopswithkubernetes.com/).

Every exercise number links to its **release**, and the directory column links to the code **as it was in that release**.

## Exercises

| Exercise | Description | Directories in this release |
|---|---|---|
| **[1.1](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.1)** | log_output - first application | [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.1/log_output) |
| **[1.2](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.2)** | The project, step 1 - todo-app created | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.2/todo-app/todo-app) |
| **[1.3](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.3)** | log_output - declarative approach (deployment.yaml) | [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.3/log_output) |
| **[1.4](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.4)** | The project, step 2 - deployment.yaml | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.4/todo-app/todo-app) |
| **[1.5](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.5)** | The project, step 3 - access via port-forward | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.5/todo-app/todo-app) |
| **[1.6](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.6)** | The project, step 4 - NodePort service | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.6/todo-app/todo-app) |
| **[1.7](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.7)** | log_output - Service and Ingress | [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.7/log_output) |
| **[1.8](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.8)** | The project, step 5 - Ingress | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.8/todo-app/todo-app) |
| **[1.9](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.9)** | ping-pong - new application | [`ping-pong`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.9/ping-pong) |
| **[1.10](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.10)** | Even more services | [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.10/log_output) |
| **[1.11](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.11)** | Persisting data | [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.11/log_output)<br>[`ping-pong`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.11/ping-pong)<br>[`shared`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.11/shared) |
| **[1.12](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.12)** | The project, step 6 | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.12/todo-app/todo-app) |
| **[1.13](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/1.13)** | The project, step 7 | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/1.13/todo-app/todo-app) |
| **[2.1](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/2.1)** | Connecting pods | [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.1/log_output)<br>[`ping-pong`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.1/ping-pong) |
| **[2.2](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/2.2)** | The project, step 8 | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.2/todo-app/todo-app)<br>[`todo-backend-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.2/todo-backend-app) |
| **[2.3](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/2.3)** | Keep them separated | [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.3/log_output)<br>[`ping-pong`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.3/ping-pong)<br>[`shared`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.3/shared) |
| **[2.4](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/2.4)** | The project, step 9 | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.4/todo-app/todo-app)<br>[`todo-backend-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.4/todo-backend-app) |
| **[2.5](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/2.5)** | Documentation and ConfigMaps | [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.5/log_output) |
| **[2.6](https://github.com/lsxol/kubernetes_mooc_fi/releases/tag/2.6)** | The project, step 10 | [`todo-app/todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/2.6/todo-app/todo-app) |

## Applications

| Directory | Description |
|---|---|
| [`log_output`](https://github.com/lsxol/kubernetes_mooc_fi/tree/master/log_output) | Log output application — a `writer` and a `reader` container sharing a volume |
| [`ping-pong`](https://github.com/lsxol/kubernetes_mooc_fi/tree/master/ping-pong) | Ping-pong counter application |
| [`todo-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/master/todo-app/todo-app) | The project: todo application frontend |
| [`todo-backend-app`](https://github.com/lsxol/kubernetes_mooc_fi/tree/master/todo-backend-app) | The project: todo application backend |

Startup instructions for each application are in the README.md of its own directory.

[`shared/manifests`](https://github.com/lsxol/kubernetes_mooc_fi/tree/master/shared/manifests) is not an application — it holds the PersistentVolume and PersistentVolumeClaim that `log_output` and `ping-pong` both mount, kept in one place because the claim is shared between them.

## Namespaces

- `exercises` — log_output, ping-pong and their shared volume
- `project` — todo-app and todo-backend-app

Neither namespace has a manifest of its own; create them with `kubectl create namespace <name>`.
