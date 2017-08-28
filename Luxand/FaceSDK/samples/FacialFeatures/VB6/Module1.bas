Attribute VB_Name = "Module1"
Option Base 0
Option Explicit


Sub Main()
    If (FSDKVB_ActivateLibrary("aCGamccfB6Uj3vlS7eDEryPnDrTbrZQb77ZHouPl3J8Q7o+BG4PcGevchFjppkWrVa038OU6Fghhy/BJfJV1n82InviCSijl8Vbxb11fs+VrcbSEfpESqjKSJQK8OLCqU0qYDy1oRHLRAg/3CHKCBzP/6IHuamy9Y/aY/xd1E7A=") <> FSDKE_OK) Then
        MsgBox "Please run the License Key Wizard (Start - Luxand - FaceSDK - License Key Wizard)", vbOKOnly, "Error activating FaceSDK"
        Exit Sub
    End If
    
    FSDKVB_Initialize ""
 
    Dim frmMain As New Form1
    frmMain.Show
  
  
End Sub
