# Task processing - calculations
This page describes the computation steps following the [inputs preparation](/doc/hourly_task/inputPreparation.md), in
the same order as in the process.

## Initial net positions computation
The initial calculation of the net positions is done on all the IGMs:
either the artifacts created in the previous steps or the input IGMs as-is if it's not concerned by these.

The computation is detailed [here](/doc/hourly_task/step_details/initialNp.md); its result is saved in an artifact file.

## Topological merge
This step consists in merging the previously mentioned IGM in one CGM file, of type 2D / country code UX; this network is then stored as an artifact.

## PiSa displacement
Some nodes have to be displaced to be able to represent the PiSa HVDC as desired : [details here](/doc/hourly_task/step_details/pisa.md).

## Recessivity alignment
Since two TSOs can provide different statuses for the same X-Node, there is a notion of recessive and reference countries:
In case of conflict, we use the value provided by the reference country.

This step handles such conflicts and cases where there's missing information on the provided X-Nodes, trying to fix these using information present in [configuration files](/doc/globalGridConfigurations.md).

It also performs a status check between the Alegro nodes, but no fix.

## Alegro X-Nodes quality check
For Alegro nodes, in addition to the previous status check, we also perform checks on flow direction and flow values.

In addition, we align the outage statuses (if one node is disconnected, we do the same for the other) and compute the gaps between the target and initial node flows. We then save the result in an artifact file.

## GLSK quality check
Consists of analyzing the GLSK input and comparing it to the available network elements.
This step produces two artifacts: GLSK quality report generation and corrected GLSK export. Both are detailed [here](/doc/hourly_task/step_details/glsk.md).

## Base case improvement
This step is a first calculation on net positions, to bring the ones outside the feasibility ranges to acceptable values. More details in the [Base Case Improvement page](/doc/hourly_task/step_details/bci.md).


## Target net positions computation
The aim of this step is the computation of the target net positions, i.e., the net positions to which we'll shift the CGM in the next step, as described [here](/doc/hourly_task/step_details/targetNp.md).

## Balances adjustment
Balances adjustment consists in shifting the CGM to the global target net positions. it is detailed in [this page](/doc/hourly_task/step_details/balancesAdjustment.md).

The result of this step is a balanced CGM.

## Special PST treatment
Some PST (Phase Shift Transformers) require special treatments Divača / Padriciano described in [this page](/doc/hourly_task/step_details/specialPst.md).

## Slack compensation
Before exporting the CGM, this step changes the setpoints of generators and loads to take into account the slack imbalance distributed by the loadflow.

With the current loadflow parameters, the distribution is done proportionally with generators setpoints. 

We then export the CGM as an UCT file.

## CGM net positions calculation

The final net positions computation is done on the CGM.

It starts by running a load flow, and then computes the following net positions for each country of the CGM:
- **Global with HVDC:** active loads summed over X-Nodes + flows leaving the country's interconnections
- **Global without HVDC:** active loads summed over XNodes corresponding to an AC interconnection + flows leaving the country's interconnections
- **CE with HVDC:** active loads summed over X-Nodes corresponding to a CE interconnection, + flows leaving the country's interconnections to CE countries
- **CE without HVDC:** active loads summed over the X-Nodes corresponding to a CE **and** AC interconnection, + flows leaving the country's interconnections to CE countries

The mentioned leaving flows are computed as the mean of origin and extremity flows.

If the load flow does not converge in AC or the balances adjustment was performed in DC mode, the final net positions computation is done in DC mode.

