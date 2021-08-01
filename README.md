# 8430ft_exporter

This repository contains metrics exporter to [Prometheus](https://prometheus.io) for 8430FT modem. 8430FT is a 3G/4G modem widely used by MTS. It can act as a normal USB modem or can be WiFi access point.

![8430FT](docs/8430FT.jpg)

By default 8430FT exports all monitoring information via its own WEB UI. 

![monitoring](docs/monitoring.png)

This is OK for small deployments, but getting harder to control and monitor on a larger scale. 8430ft_exporter project allows export such information into [Prometheus](https://prometheus.io) where it can be aggregated from hundreds of other modems.


## Build

In order to build the project simply execute:

```
mvn clean package
```

## Run

FIXME

## Run as a service

FIXME

## Prometheus configuration

FIXME
