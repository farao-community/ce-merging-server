# GLSK Quality check: detailled process
The computations described here are done on the input [GLSK](https://powsybl.readthedocs.io/projects/entsoe/en/latest/glsk/glsk.html). 
## Quality report generation
The generated quality report is an XML file that can contain warnings if:
- An explicitly named node cannot be found in the CGM,
- An explicitly named node is found, but no correct associated resource (Generator or Load) can be found in the CGM,
- An explicitly named resource is found, but is not connected to the main synchronous component of the CGM.

## Actual GLSK generation
We then create a new GLSK, filtering blocks like this:
- If the associated GLSK is a country GLSK block, no modification is done,
- If the associated GLSK is an auto GLSK block, invalid nodes are removed,
- If the associated GLSK is a manual GLSK block, invalid nodes are removed, and explicit factors are rescaled proportionally to their initial value to have a 100% factors sum.

If multiple blocks were used with a share value different of 100% (e.g., one GSK and one LSK), the share is never modified, except if one of the associated blocks is empty after previous filtering.
In that case, the other associated blocks share value is rescaled proportionally to their initial value to have a 100% share sum.

### Special case : Italian bidding zones
In the case of Italy, we may receive a GLSK with data for each [italian bidding zone](https://eepublicdownloads.entsoe.eu/clean-documents/cep/implementation/BZ/A4_BZRM_CSI_SQ.pdf). To be able to use it for the following computations, we have to transform it to a single Italian block.

Depending on the inputs :
- If any bidding zone is defined with a *\<CountryGSK\>* block, we create an Italian block with a country block within.
- Else, we get all Italian bidding zone's nodes and group them in a single Italian block with an *\<AutoNode\>* list. If we encounter manual nodes, we convert them to auto nodes, stripping their factor.