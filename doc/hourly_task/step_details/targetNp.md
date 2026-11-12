# Target Net Positions calculation
To calculate target net position $NP$, we have :


$NP = NP_{BCIw/oHVDC} + outBciFlows$

with :
- $NP_{BCIw/oHVDC}$: The net position of the BCI output without the HVDCs,
- $outBciFlows$: HVDC Flows + Flows out to areas not configured in the region configuration file.

There are a few special cases:
### Alegro countries
To compensate for the fact that Alegro initial flows are already taken into account in the BCI target NP,
we add to the previous formula the gap between target and initial flow for AL_BE for Belgium (respectively AL_DE for germany) :

### German virtual hubs
Some German virtual hubs have codes not beginning with X (**D2HWKR1D**, **D8BWW_25**...).
To keep coherence between the target net position and the net position computed with loadflow, we subtract the flow of these virtual hubs from the target NP.

In the end, given:
- the Alegro mismatch $M_{AL}(country) =$ Alegro target flow – Alegro initial flow,
- $FLOW_{VHw/oX}$ the sum of flows over german VH not starting with X,

we have:

$NP_{BE} = NP_{BCIw/oHVDC}(BE) + outBciFlows + M_{AL}(BE)$

$NP_{DE} = NP_{BCIw/oHVDC}(DE) + outBciFlows + M_{AL}(DE) - FLOW_{VHw/oX}$