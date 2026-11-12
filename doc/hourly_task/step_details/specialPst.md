# Special PST procedures
Reminder : PST = **P**hase **S**hift **T**ransformer

## Divača / Padriciano procedures

The merging process supports two mutually exclusive procedures regarding the tap position of the PST Padriciano and the PST Divača.

- **Procedure 1:** Padriciano is set to neutral tap in CGM, while PST Divača remains at the position defined in inputs.
- **Procedure 2:** PSTs taps are set according to a predefined target flow on $SI→IT$ border, available in the ELES D2CF IGM (via **"LDIVAC11 LDIVAC12"** under **##R** section). It will mirror the target flow value on $SI→IT$ border (PST Padriciano + PST Divaca) used in CSE region's D2 process.
  - If both PSTs are connected, the flow for Padriciano us be set to the pre-defined value of 150 MW. That means, flow on tie-line 220 kV Padriciano - Divača is set to 150 MW and the rest of the flow will be delivered through Divača - Redipuglia 400 kV tie-line.
  - If Padriciano is in outage, its target flow is set to 0 and sends the total to Redipuglia. 
  - If Divača is in outage, Redipuglia target flow is set to 0.
- **Procedure 3:** Applies wen both Divača and Padriciano are in outage; no additional computation.

The _pstOutput.json_ file is generated after the execution of one of these two. It contains :
- The number of the procedure used,
- The total target flow,
- The target flow on Divača-Padriciano,
- The target flow on Divača-Redipuglia,
- The tap position of PST Divača in IGM and CGM,
- The tap position of PST Padriciano in IGM and CGM,
- The computed flow on Divača-Padriciano in IGM and CGM,
- The computed flow on Divača-Redipuglia in IGM and CGM.