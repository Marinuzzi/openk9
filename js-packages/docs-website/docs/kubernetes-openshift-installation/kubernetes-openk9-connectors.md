---
id: kubernetes-openk9-connectors
title: Openk9 Connectors
---

In this section is described how to install Openk9 parser components. To install components helm charts are used.

### Preparing the installation

OpenK9 uses parsers to ingest data. You can use some core parsers developed for Openk9.
Currently installing through [Helm Charts](https://helm.sh/docs/topics/charts/) is the best choice.

Inside the [openk9-helm-charts repository](https://github.com/smclab/openk9-helm-charts) there is the
`kubernetes/02-parsers` folder where, for each parser, there are configuration files for the different installation scenarios.

So clone this repository before start to install.

## Web Parser

Web Parser is a Crawler connector for Openk9. See more on plugins documentation <mark>TODO</mark>

Install using the local chart, which is already set up to use the latest stable version of the component, and the
configuration that *adapts* it to the chosen scenario.

```bash
helm install web-parser 02-parsers/openk9-web-parser \
  -n openk9 \
  -f 02-parsers/openk9-web-parser/scenarios/local-runtime.yaml
```

### Verify installation

Check the pod startup logs for the absence of serious errors

```bash
kubectl -n openk9 logs $(kubectl -n openk9 get pod --selector="app.kubernetes.io/name=openk9-web-parser" -o name)
```

Activate a port-forward to be able to access the administration interface

```
kubectl -n openk9 port-forward \
 $(kubectl -n openk9 get pod --selector="app.kubernetes.io/name=openk9-web-parser" -o name) \
 6800:6800
```


## Email Parser

Web Parser is a connector for Openk9 to get data from Imap server. See more on plugins documentation <mark>TODO</mark>

Install using the local chart, which is already set up to use the latest stable version of the component, and the
configuration that *adapts* it to the chosen scenario.

```bash
helm install email-parser 02-parsers/openk9-email-parser \
  -n openk9 \
  -f 02-parsers/openk9-email-parser/scenarios/local-runtime.yaml
```

### Verify installation

Check the pod startup logs for the absence of serious errors

```bash
kubectl -n openk9 logs $(kubectl -n openk9 get pod --selector="app.kubernetes.io/name=openk9-email-parser" -o name)
```

Activate a port-forward to be able to access the administration interface

```
kubectl -n openk9 port-forward \
 $(kubectl -n openk9 get pod --selector="app.kubernetes.io/name=openk9-email-parser" -o name) \
 6800:6800
```

