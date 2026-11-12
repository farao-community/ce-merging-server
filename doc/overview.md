# Functional Overview
Here's a list of acronyms used across this documentation: [acronyms](/doc/acronyms.md).

## Nominal process

The nominal merging case is (done either by API call or with Swagger for testing purposes) :
- create an hourly task with all the expected inputs
  - it will produce a response containing a task ID
- run this task, providing the task ID
- repeat for the 24 hours (±1 on DST days)
- create a daily merging task, providing : 
  - a merging request file, containing target calculation date and other information,
  - all the previously mentioned task IDs,
  - a merging version.
- Run it with the provided daily task ID.

Each of these steps corresponds to a REST endpoint; there are several other endpoints providing configurations or a given task's specific inputs, outputs, or intermediate files (called [**Artifacts**](/doc/hourly_task/artifacts.md)).

For more detail on the endpoints, see the **Controller** classes.

## Inputs
It works with these input files :
- Individual [UCTE](https://eepublicdownloads.entsoe.eu/clean-documents/pre2015/publications/ce/otherreports/UCTE-format.pdf) networks by country (called **IGM - Individual Grid Model**),
- **Quality Checks** for those: contains information such as, for example, warnings about some quantity being outside expected boundaries,
- **Net Position Forecast (NPF)**: contains the TSO's daily exchanges predictions, 
- **Generation Load Shift Key (GLSK)**: describes how a shift in power should be distributed among all the grid's nodes,
- **Feasibility Ranges & External Constraints**: define constraints on some quantities for nodes / areas,
- **Italian Bidding Zones**: contains information about Italy's internal bidding zone, in regard to the UCTE nodes,
- **OLF parameters**: there are a few files that specify parameters for the load flow calculation on different occurrences :
  - Base Case Improvement
  - Balances Adjustment
  - AC loadflow
  - DC loadflow

### Global configuration
These are not inputs as they are not linked to a particular task, but one can apply changes on the [configuration files](/doc/globalGridConfigurations.md).

## Merging steps
### Hourly
- [Inputs preparation](/doc/hourly_task/inputPreparation.md)
- [Calculations](/doc/hourly_task/calculations.md)
- [Output Generation](/doc/hourly_task/outputs.md)

### Daily
- Check that all provided task IDs correspond to finished tasks, with coherent dates regarding the daily task
- Get the successful task's hourly outputs and merge them into aggregated files, as described below
- Create some other files like the XLSX summary described below

## Outputs
For every hour of a given day, there is an hourly process launched; then these are subsequently merged into daily results.

At the hourly level, these are :
- **RefProg (Reference Program) :** represents border exchanges 
- **CGM (Common Grid Model) :** aggregation of all the IGMs
- **GLSK :** previously described, adjusted after calculations
- **IGM Quality Checks :** previously described, enriched along the treatment
- **Xnodes Inconsistencies :** tracks the errors in the provided Xnodes, compared to configuration (incorrect countries, missing information, ...) 
- **GLSK Quality Check :** analysis of the input GLSK file, comparison to the available network elements in the CGM
- **Merging logs :** contains information about net positions, generation/load quantity, and global balances

The final daily outputs are :
- **RefProg (F101) :** all hourly refprogs merged in one file
- **Merging logs (F123) :** same for merging logs
- **CGM Zip (F100) :** a zip of all the day's CGMs
  - **CGM recognition :** included in the above zip, contains information about target dates and missing information
- **Xnodes Inconsistencies zip :** same as CGM
- **GLSK quality check (F117) :** same as RefProg (one file)
- **Merging Response (F121) :** makes a link between the merging request and the provided outputs
- **Merging report :** a summary of the merging process in spreadsheet form