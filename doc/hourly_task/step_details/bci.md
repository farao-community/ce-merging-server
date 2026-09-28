# Base Case Improvement
This page describes how the shifts to apply to active bidding zones are calculated,
in case the target net position of inactive bidding zones is outside the feasibility ranges.

What inactive means is that, for this zone, the initial CE target net position was outside the feasibility ranges. Therefore, its CE net position is fixed to the min or max value defined in its feasibility range.

In contrast, active bidding zones participate in the compensation, because their initial CE target net position was inside the feasibility range.

Given :
- **NPx**: Initial CE net position,
- **NeededShift**: The difference between the target net position and the feasibility ranges max/min (whichever is closer to NP),
- **CBA/CBI**: Active/Inactive bidding zone,

The new target CE net positions calculation depends on the sum of NeededShift for inactive zones :
- if it is negative,
![bci formula for negative needed shift](bci_negative_shift.png)
- if it is positive :
![bci formula for positive needed shift](bci_positive_shift.png)

For the inactive bidding zones, the net positions will be shifted to the max/min of the feasibility range using the real merged GLSK.

The remaining difference between target net position and reference net position is being distributed over the active CE bidding zones, to their newly calculated CE net position using the GLSK.
