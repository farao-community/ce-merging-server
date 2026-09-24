# Task processing - input preparation
Before merging the provided inputs, we have to apply some transformations and do some checks first on the individual inputs, that we'll describe here.

As of writing, one paragraph below <=> one service called in the **MergingService** class.

### X-Node IGM status
For every X-Node found in IGMs, we check if there are inconsistencies :
- Is there a X-Node appearing only in one IGM ?
- Is there a difference in statuses between two IGMs ?
- Is there an X-Node undefined in the X-Node configuration file ?

If so, these informations are saved in an X-Node inconsistencies file.
### NPF import
The net position file is an XML that comes from user input; it contains a time period interval that we compare with our task's date to check its validity. 

It also contains a list of time series of power throughout the day, that we filter by [reason code](https://eepublicdownloads.entsoe.eu/clean-documents/EDI/Library/Core/entso-e-code-list-v36r0.pdf) : if it is A95, we don't use it for the merging.

We then check date validity against the time series periods, and we compute the flow for this period depending on the [specified curve type](https://eepublicdownloads.entsoe.eu/clean-documents/EDI/Library/cim_based/Introduction_of_different_Timeseries_possibilities__curvetypes__with_ENTSO-E_electronic_document_v1.4.pdf) :

If it is not **A01** or **A03**, we throw an exception because it is not an acceptable input,
if it's one of these, it defines how we model the intervals of time :

**A01 :** end of interval = start of interval + resolution

**A03 :** end of interval = start of next interval
### German pre-merge
Since there are several German TSOs, we receive 5 different IGMs, with the "country" codes being D2, D4, D6, D7 and D8.

In this step, we merge these into a single German UCTE file, with the country code DE.
The XNodes merged during the process are then converted into German standard nodes, since these are not representing a border node anymore.
Then we run a first load flow on this merged German network.

Finally, we calculate the german internal mismatch by summing all internal net positions, and distribute this mismatch on boundary lines proportionally :

updated flow = initial flow + mismatch * | initial flow / sumExternalNP |



### Denmark renaming
The IGM files that we receive from Danish TSO have nodes defined in the **##ZD1** zone and have names starting with D1.

In order to not clash with german country code, any node which name starts with D1 will be moved in the **##ZDK** zone and renamed to start with K1.

An explicit list of specific nodes (HVDC ones) that does not start with D1, but are Danish nodes,
will be moved in **##ZDK** zone and renamed to start with X. 

Any element (line, transformer...) that reference these nodes are also renamed.
### HVDC alignment
For each couple defined in the HVDC alignment configuration, we check if one of the nodes was not found in virtual hub configuration.

If the two nodes have been recovered correctly, we find the corresponding countries using the _RelatedMa_ field, and then the corresponding IGMs.

If we cannot find any one of these, we emit an error. Then, if we are unable to retrieve the boundary line associated with the reference or recessive node in the network, we will emit a warning.


If the two dangling lines have been recovered correctly, we apply the HVDC alignment :

- Boundary line and generator P corresponding to the recessive X-Node are set to the opposite of that of reference node,
- We also align the outage status.

We then save the modified IGMs in the artifacts with the same name.
### MONITA renaming
The nodes describing the MONITA (**MON**tenegro - **ITA**ly) HVDC doesn't start with an X. In order to include them correctly in the following calculations, they have to, so we rename these :
- ICEPR → XCEPR
- IKOTR → XKOTR.

This renaming will allow us to ensure that the MONITA nodes (XCEPR120, XCEPR220, XKOTR120, XKOTR220) 
will be counted in the calculation of outBciNetPosition which allows us to have a correct calculation of the balance adjustment targets for Italy and Montenegro.

