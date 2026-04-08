      *****************************************************************
      **  MEMBER :  CCWM4000                                         **
      **  REMARKS:  MESSAGE INTERFACE FOR CSOM4000                   **
      **            TAMRA HISTORY                                    **
      *****************************************************************
      ** DATE      AUTH.  DESCRIPTION                                **
      **                                                             **
      ** 2026-03-25 TPI   CREATED MESSAGE INTERFACE FOR TPI          **
      *****************************************************************
      TEST COMMIT
      *
       01  MIR-PARM-AREA.
           05  MIR-CONTROL-AREA.                                
               10  MIR-BUS-FCN-ID                         PIC X(004).
               10  MIR-LENGTH                             PIC X(005).
               10  MIR-RETRN-CD                           PIC X(002).
                   88  MIR-RETRN-OK                       VALUE '00'.
                   88  MIR-RETRN-EDIT-ERROR               VALUE '01'.
                   88  MIR-RETRN-RQST-FAILED              VALUE '02'.
                   88  MIR-RETRN-INVALD-RQST              VALUE '99'.
           05  MIR-INPUT-AREA.
               10  FILLER                                 PIC X(01).
           05  MIR-IO-AREA.
               10  MIR-COMMON-FIELDS.
                   15  MIR-TAMRA-EFF-DT                   PIC X(10).
                   15  MIR-DV-OWN-CLI-NM                  PIC X(50).
                   15  MIR-POL-ID.
                       20  MIR-POL-ID-BASE                PIC X(09).
                       20  MIR-POL-ID-SFX                 PIC X(01).
                   15  MIR-TAMRA-ACB-AMT                  PIC X(16).
                   15  MIR-TAMRA-7PAY-ANN-AMT             PIC X(17).
                   15  MIR-TAMRA-CV-AMT                   PIC X(16).
                   15  MIR-TAMRA-7PAY-CUM-AMT             PIC X(19).
                   15  MIR-POL-MATRL-CHNG-DT              PIC X(10).
                   15  MIR-POL-MEC-DT                     PIC X(10).
                   15  MIR-TAMRA-NSP-AMT                  PIC X(16).
                   15  MIR-TAMRA-1035-PD-AMT              PIC X(16).
                   15  MIR-POL-7PAY-ANN-AMT               PIC X(16).
                   15  MIR-POL-7PAY-CUM-AMT               PIC X(18).
                   15  MIR-POL-ISS-EFF-DT                 PIC X(10).
                   15  MIR-TAMRA-REASN-CD                 PIC X(01).
                   15  MIR-TAMRA-STAT-CD                  PIC X(01).
                   15  MIR-TAMRA-SUM-INS-AMT              PIC X(18).
               10  MIR-LIST-FIELDS.
                   15  MIR-TAMRA-ACB-AMT-G.
                       20  MIR-TAMRA-ACB-AMT-T            OCCURS 11
                                                          PIC X(16).
                   15  MIR-TAMRA-7PAY-ANN-AMT-G.
                       20  MIR-TAMRA-7PAY-ANN-AMT-T       OCCURS 11
                                                          PIC X(17).
                   15  MIR-TAMRA-7PAY-CUM-AMT-G.
                       20  MIR-TAMRA-7PAY-CUM-AMT-T       OCCURS 11
                                                          PIC X(19).
                   15  MIR-TAMRA-EFF-DT-G.
                       20  MIR-TAMRA-EFF-DT-T             OCCURS 11
                                                          PIC X(10).
                   15  MIR-TAMRA-REASN-CD-G.
                       20  MIR-TAMRA-REASN-CD-T           OCCURS 11
                                                          PIC X(01).
                   15  MIR-TAMRA-STAT-CD-G.
                       20  MIR-TAMRA-STAT-CD-T            OCCURS 11
                                                          PIC X(01).
           05  MIR-OUTPUT-AREA.
               10  FILLER                                 PIC X(01).
      *****************************************************************
      **                END OF COPYBOOK                              **
      *****************************************************************
