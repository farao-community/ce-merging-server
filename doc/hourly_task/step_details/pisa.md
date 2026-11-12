# Piemonte-Savoie X-Nodes displacement
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