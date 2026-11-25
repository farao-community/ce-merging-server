# ce-merging-server
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=farao-community_ce-merging-server&metric=coverage)](https://sonarcloud.io/component_measures?id=farao-community_ce-merging-server&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=farao-community_ce-merging-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=farao-community_ce-merging-server)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

The purpose of this application is the creation of aggregated files for the [Central Europe (CE) zone](https://www.entsoe.eu/bites/ccr-ce/about/). To this end, most flow calculations are made using [PowSyBl](https://powsybl.readthedocs.io/en/latest/) libraries.

## Requirements
To be able to run this application on your local machine, you need :
- Java JDK >= 21
- A maven installation.

When launched, the application is callable through a [Swagger](https://swagger.io/) interface or by API. Its default address is *localhost:8080*.

## Running with Docker

The Docker image is built from the jar Maven builds (`mvn package`, then `docker build .`). The server runs as an unprivileged user (uid 10001, group 0) and keeps its data below `/home/ce`:
- `/home/ce/ce-merging`: the H2 database, with the tasks and their execution logs
- `/home/ce/ce-merging-server`: the input, output and artifact files of the tasks

Mount volumes on both directories to keep the data across containers. The container may also run under another uid, provided it uses group 0 and the volumes are writable for it.

Important: The UID can be chosen freely, but must remain the same when reusing existing volumes across container restarts. Changing the UID after data has been created may result in permission errors. To use a different UID, remove and recreate the volumes first.

The H2 web console is off by default, since it gives full access to the database. Set SPRING_H2_CONSOLE_ENABLED=true to switch it on, where the server is not exposed.

## Functionalities
A functional overview of this application is available here : [Functional Overview](/doc/overview.md)