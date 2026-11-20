# Piemonte-Savoie X-Nodes special treatments
## Displacement
In the case of Piemonte-Savoie HVDC, we have to "move" the border to different terminals. In the network model, that means having some X-Nodes transformed into normal nodes, and inversely : 
- **FFG.IL/11/12/13/14** become X-Nodes (**XFG**...)
- **XPI_GI11/12** become normal Italian nodes (**IPI**...)

To achieve that, because we are changing two kinds of nodes at the same time, we must proceed in order :
- Check if any defined line would become a line between X-Nodes. If such a case arise :
  - if the line is disconnected, remove it from the network
  - if not, stop there and raise an exception
- Depending on the appearance/disappearance of boundary lines, there could be [tie lines](https://javadoc.io/doc/com.powsybl/powsybl-core/latest/com/powsybl/iidm/network/TieLine.html) that would no longer exist: we delete them early on to be able to create the desired network elements.
- Handle the change of line type : 
  - A [boundary line](https://javadoc.io/doc/com.powsybl/powsybl-core/latest/com/powsybl/iidm/network/BoundaryLine.html) could become a [simple line](https://javadoc.io/doc/com.powsybl/powsybl-core/2.6.1/com/powsybl/iidm/network/Line.html), or the other way around,
  - There could also be a boundary line that we have to "invert": in addition to the changes in names, we have to feed the terminal data from what will become the boundary line's terminal.
- Then, all the nodes and network elements referring to them are renamed, using a custom [PowSyBl naming strategy](https://powsybl.readthedocs.io/projects/powsybl-core/en/latest/grid_features/network_modifications/topology_modifications.html#naming-strategy).
## Net position adjustment
To take this displacement into account in the [initial net positions calculation](/doc/hourly_task/step_details/initialNp.md), we have to do the following :

Since $XPI \_ GI1X$ nodes will no longer be X-Nodes, we remove from Italian net positions : 

$(IPIOTA11→IPI \_ GI11) + (IPIOTA11→IPI \_ GI12)$

$(IPIOTA11→XPI \_ GI11) + (IPIOTA11→XPI \_ GI12)$ 

$FFG.IL1X$ nodes will become X-Nodes, so we add to Italian net positions :

$(IPI \_ GI11→XFG.IL11) + (IPI \_ GI12→XFG.IL13)$

$= -(XFG.IL11→IPI \_ GI11) - (XFG.IL13→IPI \_ GI12)$ 

$= -(XFG.IL11→IPI \_ GI11) - (XFG.IL13→IPI \_ GI12)$

$= (FFG.IL12→XPI \_ GI11) + (FFG.IL14→XPI \_ GI12)$

because as the border will be between $XFG.IL11/13$ and $XFG.IL12/14$, these have opposite signs.


For France, following the same logic, we would have to remove $FFG.IL11/13→XPI \_ GI11/12$  from the net positions and add :

$FG.ILE12/14→XFG.IL12/14$

$= -IPI \_ GI11/12 →XFG.IL11/13$ (opposite side of border)

$= FFG.IL11/13→XPI \_ GI11/12$

⇒ it would remain unchanged

