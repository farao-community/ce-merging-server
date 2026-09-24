# Global Grid Configurations

This page describes the configuration files that can be provided to the application. 

Providing these files is not mandatory : there are default configurations that can be found inside the resource folder, under _gridDefaultConfigurations_.

### Region configuration

it contains the following :
- _name_: region name (CE)
- _id_: region EIC 
- _areasIn_: list of countries/EIC inside CE 
- _areasOut_: list of countries/EIC outside CE 
- _germanyZones_: list of German subareas ; each one is described by **tsoName** & **EIC**

### Sharing keys for bilateral exchanges
It represents a matrix of coefficients used for RefProg calculations. 

Each line is a bilateral exchange, and the columns are coefficients modelling the effect of this bilateral exchange on other countries.

### X-Nodes configuration
Describe border nodes (called X-Nodes because their UCTE codes start with X) that will be considered separately from other nodes given their location, and the fact that they can appear in several IGMs.

### Virtual Hubs configuration
Also contains border information, but more specifically about involved TSOs/regions.

### Recessivity configuration

Since two TSO can provide different statuses for a same X-Node, there is a notion of recessive & reference countries :
In case of conflict, we use the value provided by the reference country. 

Which country is reference & which is recessive is specified in the recessivity configuration file.

### HVDC alignment configuration

This is similar to the recessivity configuration file, but with specific data for HVDC links ; it also contains the default [slack node](https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/grid_features/loadflow_validation.html#active-power) for computations.