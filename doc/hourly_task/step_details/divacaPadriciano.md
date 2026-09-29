# Divača / Padriciano procedures

The merging process supports two mutually exclusive procedures regarding the tap position of the PST Padriciano and the PST Divača.

- **Procedure 1:** Padriciano is set to neutral tap in the Italian IGM, while PST Divača remains at the position defined in inputs.
- **Procedure 2:** PSTs taps are set according to a predefined target flow on SI-IT border, available in the ELES D2CF IGM (via **"LDIVAC11 LDIVAC12"** under **##R** section)
    - It will mirror the target flow value on SI->IT border (PST Padriciano + PST Divaca) used in D-2 process in the CSE region.
    - The flow for PST Padriciano should be set to the fixed value of 150 MW. That means, flow on tie-line 220 kV Padriciano - Divača should be fixed to 150 MW and the rest of the flow should be delivered through tie-line 400 kV Divača - Redipuglia.

The pstOutput.json file is generated after the execution of one of these two. It contains :
- The number of the procedure used
- The total target flow
- The target flow on Divača-Padriciano
- The target flow on Divača-Redipuglia
- The tap position of PST Divača in IGM and CGM
- The tap position of PST Padriciano in IGM and CGM
- The computed flow on Divača-Padriciano in IGM and CGM
- The computed flow on Divača-Redipuglia in IGM and CGM