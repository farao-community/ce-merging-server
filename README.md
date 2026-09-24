# ce-merging-server
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=farao-community_ce-merging-server&metric=coverage)](https://sonarcloud.io/component_measures?id=farao-community_ce-merging-server&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=farao-community_ce-merging-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=farao-community_ce-merging-server)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Functional Overview
The purpose of this application is the creation of aggregated files for the [CE zone]([https://www.entsoe.eu/bites/ccr-ce/about/](https://www.entsoe.eu/bites/ccr-ce/about/)).

### Process

The nominal merging case is (either automated, or with Swagger for testing/developping purposes) :
- create an hourly task with all the expected inputs
  - it will produce a response containing a task id
- run this task, providing the task id
- repeat for the 24 hours (±1 on DST days)
- create a daily merging task, providing : 
  - a merging request file, containing target calculation date & other informations,
  - all the previous task ids.
- Run it

Each of these steps correspond to a REST endpoint ; there are several other providing a given task's specific inputs, outputs, or intermediate files (called Artifacts).

For more detail, see the Controller classes.

### Inputs
It works with these inputs :
- individual [UCTE](https://eepublicdownloads.entsoe.eu/clean-documents/pre2015/publications/ce/otherreports/UCTE-format.pdf) network files by country (called **IGM - Integrated Grid Model**),
- **quality checks** files for those : contains information such as, for example, warnings about a given node having attributes outside of expected boundaries,
- **Net Position Forecast (NPF)** file : contains the TSO's daily predictions for power generation / consumption 
- the merged **Generation Load Shift Key file (GLSK)** : describes how a shift in power should be distributed among all the grid's nodes
- **feasibility ranges & external constraints** : define constraints on some quantities for nodes / areas
- OLF parameters files : there are a few files that specify parameters for the load flow calculation at different occurences :
  - base case improvement
  - balances adjustment
  - AC loadflow
  - DC loadflow

### Outputs
For every hour of a given day, there is an hourly process launched, then these are subsequently merged into daily results.

At the hourly level, these are :
- **RefProg (Reference Program) :** represents border exchanges 
- **CGM (Common Grid Model) :** aggregation of all the IGMs
- **GLSK :** previously described, adjusted after calculations
- **IGM Quality Checks :** previously described, enriched along the treatment
- **Xnodes Inconsistencies :** tracks the errors in the provided Xnodes, compared to configuration (incorrect countries, misssing information, ...) 
- **GLSK Quality Check :** analysis of the input GLSK file, comparison to the available network elements in the CGM
- **Merging logs :** contains information about net positions, generation quantity , load quantity and global balances

The final daily outputs are :
- **RefProg (F101) :** all of the hourly refprogs merged in one file
- **Merging logs (F123) :** same for merging logs
- **CGM Zip (F100) :** a zip of all the day's CGMs
  - **CGM recognition :** included in the above zip, contains information about target dates & missing information
- **Xnodes Inconsistencies zip :** same as CGM
- **GLSK quality check (F117) :** same as RefProg (1 file)
- **Merging Response (F121) :** makes a link between the merging request and the provided outputs
- **Merging report :** a summary of the merging process in spreadsheet form

