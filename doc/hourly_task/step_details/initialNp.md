# Initial net positions calculation details

For each IGM, it starts by running a loadflow (in AC with fallback DC) and then computes the following net positions:

- **Global NP with VH:** active loads summed over all the X-Nodes/Nodes present on the virtual hubs
  config file
- **Global NP without VH:** active powers summed over AC interconnections
- **CE NP with VH:** active loads summed over the CE X-Nodes/Nodes present on the virtual hubs
  config file
- **CE NP without VH:** active powers summed over CE AC interconnections

During the initial calculation of the net positions, for X-Nodes with area=DE, to determine the country on the other side, we look at subarea : 

- subarea = "D1" → DK
- subarea ∈ { D2, D4, D6, D7, D8 } → DE

- subarea ∉ { D1, D2, D4, D6, D7, D8 } →  Warning because the area is DE without a valid subarea

Here's how the X-Nodes are counted depending on their presence in config files :

| X-node is present in  …              | X-node & VH config                                                      | VH Config                         | X-Node config                                                 | neither*           | 
|--------------------------------------|-------------------------------------------------------------------------|-----------------------------------|---------------------------------------------------------------|--------------------|
| examples                             | Alegro, GR-IT, Cobra and HVDC cables going outside the synchronous area | XCEPR120 (created during process) | X-nodes modeled as AC links (including FR-ES and FR-IT HVDCs) | XHR_HR12, XMO_HO11 |
| load counted in Global NP with VH    | ✅                                                                      | ✅                                | ✅                                                            | ✅                 | 
| load counted in Global NP without VH | ❌                                                                      | ❌                                | ✅                                                            | ✅                 |
| load counted in outBciNetPosition    | ✅                                                                      | ✅                                | ❌                                                            | ✅                 |

\* No such X-node should be encountered in real conditions (merging not allowed by supervisor)

### Monita post-treatment

For the MONITA HVDC, post-treatment is necessary:
Its nodes are all present in the Italian IGM, but we need to have the XKOTR120 and XKOTR220 nodes to Montenegro, to
apply the BCI step correctly on Montenegro.

Indeed, as we can see in the reference program file provided in outputs, we have :

NP_IT = (IT-FR + IT-AT + IT-SI + IT-CH) + (XAR_GA1I + XCEPR220 + XCEPR120)

NP_ME = (ME-RS + ME-AL + ME-XK + ME-BA) + (XKOTR120 + XKOTR220)

And here's how the balance adjustment targets are calculated :

Target NP = NP from BCI (without HVDC) + out flows from BCI

It is therefore necessary that the nodes XCEPR120 and XCEPR220 be counted in the outBciNetPosition for Italy and that
the nodes XKOTR120 and XKTRO220 be counted in the outBciNetPosition for Montenegro to compute the correct balance
adjustment targets.