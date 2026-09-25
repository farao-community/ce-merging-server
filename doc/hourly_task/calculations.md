# Task processing - calculations

This page describes the computation steps following the [inputs preparation](/doc/hourly_task/inputPreparation.md), in
the same order as what is done in the application.

Some of the acronyms used here :

- NP: Net Positions
- VH: Virtual Hubs
- BCI: Base Case Improvement
- IGM/CGM: Individual/Common Grid Model

### Initial net positions computation

The initial calculation of the net positions is done on all the IGMs:
either the artifacts created in the previous steps, or the input IGMs as-is if it's not concerned by these.

The computation is detailed [here](/doc/hourly_task/step_details/initialNp.md) ; its result is saved in a JSON file named _igmsNetPositions.json_.

### Topological merge

This step consists in merging the previously mentioned IGM in one CGM file, of type 2D/country code UX.

This network is then stored as an artifact.

### Recessivity alignment
Since two TSO can provide different statuses for the same X-Node, there is a notion of recessive & reference countries :
In case of conflict, we use the value provided by the reference country.

This step handles such conflicts, and also handles cases where there is some missing information on the provided X-Nodes, trying to fix these using information present in [configuration files](/doc/globalGridConfigurations.md).

It also performs a status check between the Alegro nodes, but no fix.
### Alegro X-Nodes quality check
For Alegro nodes, in addition to the previous status check, we also perform checks on flow direction and flow values.

In this step, we also align the outage statuses (if one node is disconnected, we do the same for the other) and compute the gaps between the target and initial flows for these, and save the result in an artifact file.
### GLSK quality check

Consists of analyzing the GLSK input and comparing it to the available network elements.
This step is decomposed into two parts: GLSK quality report generation and actual GLSK export.

##### Quality report generation

The generated quality report is an XML file that can contain warnings if:
- An explicitly named node cannot be found in the CGM
- An explicitly named node is found but no correct associated resource (Generator or Load) can be found in the CGM
- An explicitly named resource can be found but is not connected to the main synchronous component of the CGM

##### Actual GLSK generation
We then create a new GLSK, filtering blocks like this:
- If the associated GLSK is a country GLSK block, no modification is done.
- If the associated GLSK is an auto GLSK block, invalid nodes are removed
- If the associated GLSK is a manual GLSK block, invalid nodes are removed, and explicit factors are rescaled proportionally to their initial value to have a 100% factors sum.

If multiple blocks were used with a share value different of 100% (e.g. one GSK and one LSK), the share is never modified, except if one of the associated block is empty after previous filtering. In that case, the other associated blocks share value is rescaled proportionally to their initial value to have a 100% share sum.

### Base case improvement
TODO
### Alegro P0 Update
TODO
### Target net positions computation
TODO
### Balances Adjustment
TODO
### Special PST treatment
Some phase shift transformers require special treatments: [Divača / Padriciano](/doc/hourly_task/step_details/divacaPadriciano.md) and [some austrian PSTs](/doc/hourly_task/step_details/austrianPsts.md).
### Slack compensation
Before exporting the CGM, this step changes the setpoints of generators and loads to take into account the slack imbalance distributed by the loadflow.

With the current loadflow parameters, the distribution is done proportionally with generators setpoint. 

We then export the CGM as an UCT file.
### CGM net positions calculation

The final net positions computation is done on the CGM.

It starts by running a loadflow, and then computes the following net positions for each country of the CGM:
- **Global NP with HVDC:** active loads summed over all the XNodes + flows leaving the country merged interconnections
- **Global NP without HVDC:** active loads summed over the XNodes corresponding to an AC interconnection + flows leaving the country merged interconnections
- **CE NP with HVDC:** active loads summed over the XNodes corresponding to a CE interconnection, + flows leaving the country merged interconnections to CE countries
- **CE NP without HVDC:** active loads summed over the XNodes corresponding to a CE and an AC interconnection, + flows leaving the country merged interconnections to CE countries

The mentioned leaving flows are computed as the mean of origin and extremity flows.

If the load flow does not converge in AC or the balances adjustment was performed in DC mode, the final net positions computation is done in DC mode.

