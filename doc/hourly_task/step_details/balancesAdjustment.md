# Balances Adjustment

To execute this step, we have to convert the current CGM to XIIDM format, then back to UCTE for the output.

## Balances area definition
Balances adjustment algorithm is based on the definition of balance areas. Each area is defined by three important elements:

- Area definition: the way to compute its net position on a network instance (e.g., area based on a country),
- Target net position: the expected value, at the end of the algorithm, of the area net position as calculated using the previous description,
- GLSK: the way in which a modification in injection changes the net position of the area.

For this particular process, an area is created for each UCTE country. 

## Balancing algorithm
The net position of the country is calculated as the sum of:
- Active loads of XNodes connected to a [substation](https://javadoc.io/doc/com.powsybl/powsybl-core/latest/com/powsybl/iidm/network/Substation.html) in the country,
- Leaving flows of lines connected :
  - in the country on one side
  - outside the country on the other side

The flow leaving side 1 is calculated as $\frac{(P_1 - P_2)}{2}$ with $P_x$ being the flow of side $x$, calculated by a load flow.

With this definition of net position, the target value for each area is the global net position including HVDCs for each country.

If a country does not provide GLSK (as it is the case for countries outside the CE region), 
a country LSK is created, dispatching the flows proportionally on all loads. 
The balances adjustment ignores the $P_{min}$ / $P_{max}$ limitation of generators.

The algorithm is iterative: at each iteration, each area's net position mismatch is redispatched via the area's GLSK. 

It continues until the sum of the square mismatches is lower than a certain threshold, or until it reaches the maximum number of iterations. 
This threshold and the max number of iterations are both configurable in the app.