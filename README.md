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

## Functionalities
A functional overview of this application is available here : [Functional Overview](/doc/overview.md)