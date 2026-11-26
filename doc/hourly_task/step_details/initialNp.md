# Initial net positions calculation details
This calculation is part of the [hourly calculations process](/doc/hourly_task/calculations.md#initial-net-positions-computation).

For each IGM, it starts by running a loadflow in AC, and if it does not converge, another in DC. It then computes the following net positions:

- **Global with VH**: active loads summed over all the X-Nodes/Nodes present on the virtual hubs
  [config file](/doc/globalGridConfigurations.md),
- **Global without VH**: active powers summed over AC interconnections,
- **CE with VH**: active loads summed over the CE X-Nodes/Nodes present on the virtual hubs
  config file,
- **CE without VH**: active powers summed over CE AC interconnections.

During the initial calculation of the net positions, for X-Nodes with $area = DE$, to determine the country on the other side, we look at its subarea : 

- $subarea = D1 → DK$
- $subarea ∈ { D2, D4, D6, D7, D8 } → DE$
- $subarea ∉ { D1, D2, D4, D6, D7, D8 } →$ Warning because the area is DE without a valid subarea

Here's how the X-Nodes are counted depending on their presence in config files :

| X-Node is present in  …              | X-Node & VH config                                                      | VH Config                         | X-Node config                                                 | neither*           | 
|--------------------------------------|-------------------------------------------------------------------------|-----------------------------------|---------------------------------------------------------------|--------------------|
| examples                             | Alegro, GR-IT, Cobra and HVDC cables going outside the synchronous area | XCEPR120 and PiSa nodes (created during process) | X-Nodes modeled as AC links (including FR-ES and FR-IT HVDCs) | XHR_HR12, XMO_HO11 |
| load counted in Global NP with VH    | ✅                                                                      | ✅                                | ✅                                                            | ✅                 | 
| load counted in Global NP without VH | ❌                                                                      | ❌                                | ✅                                                            | ✅                 |
| load counted in outBciNetPosition    | ✅                                                                      | ✅                                | ❌                                                            | ✅                 |

_\* No such X-Node should be encountered in real conditions (merging not allowed by supervisor)_

## Post-process adjustment
For some HVDCs, post-treatment is necessary because some X-Nodes are to be displaced, which impacts the net position result.
### PiSa HVDC
Process detailed in the [PiSa section](/doc/hourly_task/step_details/pisa.md).

### MonIta HVDC
For the MonIta HVDC, it is because
Its nodes are all present in the Italian IGM, but we need to have the $XKOTR120$ and $XKOTR220$ nodes in Montenegro's IGM to
be able to apply the [BCI step](/doc/hourly_task/step_details/bci.md) correctly.

Indeed, as we can see in the reference program file provided in outputs, we have :

$NP(IT) = NP(IT→FR) + NP(IT→AT) + NP(IT→SI) + NP(IT→CH) + (XAR \\_ GA1I + XCEPR220 + XCEPR120)$

$NP(ME) = NP(ME→RS) + NP(ME→AL) + NP(ME→XK) + NP(ME→BA) + (XKOTR120 + XKOTR220)$

And here's how the balance adjustment targets are calculated :

$NP_{TARGET} = NP_{BCI w/oHVDC}+ outBciFlows$

It is therefore necessary that the nodes $XCEPR120$ and $XCEPR220$ be counted in the _outBciNetPosition_ for Italy and that
the nodes $XKOTR120$ and $XKOTR220$ be counted in the _outBciNetPosition_ for Montenegro to compute the correct [balance
adjustment](/doc/hourly_task/step_details/balancesAdjustment.md) targets.