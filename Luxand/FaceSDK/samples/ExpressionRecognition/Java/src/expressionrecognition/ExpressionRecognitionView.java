/*
 * ExpressionRecognitionView.java
 * 
 * To edit GUI in visual editor of Netbeans 7.2+ you can install Swing Application Framework plugin:
 * http://plugins.netbeans.org/plugin/43853/swing-application-framework-support
 * Do not forget to restart Netbeans after installing the plugin!
 */

package Expressionrecognition;

import org.jdesktop.application.Action;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import Luxand.*;
import Luxand.FSDK.*;
import Luxand.FSDKCam.*;


/**
 * The application's main frame.
 */
public class ExpressionRecognitionView extends FrameView {
    public ExpressionRecognitionView(SingleFrameApplication app) {
        super(app);

        initComponents();
        
        final JPanel mainFrame = this.mainPanel;
        
        try {
            int r = FSDK.ActivateLibrary("aCGamccfB6Uj3vlS7eDEryPnDrTbrZQb77ZHouPl3J8Q7o+BG4PcGevchFjppkWrVa038OU6Fghhy/BJfJV1n82InviCSijl8Vbxb11fs+VrcbSEfpESqjKSJQK8OLCqU0qYDy1oRHLRAg/3CHKCBzP/6IHuamy9Y/aY/xd1E7A=");
            if (r != FSDK.FSDKE_OK){
                JOptionPane.showMessageDialog(mainPanel, "Please run the License Key Wizard (Start - Luxand - FaceSDK - License Key Wizard)", "Error activating FaceSDK", JOptionPane.ERROR_MESSAGE); 
                System.exit(r);
            }
        } 
        catch(java.lang.UnsatisfiedLinkError e) {
            JOptionPane.showMessageDialog(mainPanel, e.toString(), "Link Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }    
            
        FSDK.Initialize();
           
        final HTracker tracker = new HTracker(); // creating a Tracker
        FSDK.CreateTracker(tracker);

        // set realtime face detection parameters
        int err[] = new int[1];
        err[0] = 0;
        FSDK.SetTrackerMultipleParameters(tracker, "RecognizeFaces=false; DetectExpression=true; HandleArbitraryRotations=false; DetermineFaceRotationAngle=false; InternalResizeWidth=100; FaceDetectionThreshold=5;", err);
        
        FSDKCam.InitializeCapturing();
                
        TCameras cameraList = new TCameras();
        int count[] = new int[1];
        FSDKCam.GetCameraList(cameraList, count);
        if (count[0] == 0){
            JOptionPane.showMessageDialog(mainFrame, "Please attach a camera"); 
            System.exit(1);
        }
        
        String cameraName = cameraList.cameras[0];
        
        FSDK_VideoFormats formatList = new FSDK_VideoFormats();
        FSDKCam.GetVideoFormatList(cameraName, formatList, count);
        FSDKCam.SetVideoFormat(cameraName, formatList.formats[0]);
        
        cameraHandle = new HCamera();
        int r = FSDKCam.OpenVideoCamera(cameraName, cameraHandle);
        if (r != FSDK.FSDKE_OK){
            JOptionPane.showMessageDialog(mainFrame, "Error opening camera"); 
            System.exit(r);
        }
        
        
        // Timer to draw and process image from camera
        drawingTimer = new Timer(40, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HImage imageHandle = new HImage();
                if (FSDKCam.GrabFrame(cameraHandle, imageHandle) == FSDK.FSDKE_OK){
                    Image awtImage[] = new Image[1];
                    if (FSDK.SaveImageToAWTImage(imageHandle, awtImage, FSDK_IMAGEMODE.FSDK_IMAGE_COLOR_24BIT) == FSDK.FSDKE_OK){
                        
                        BufferedImage bufImage = null;
                        TFacePosition.ByReference facePosition = new TFacePosition.ByReference();
                        
                        long[] IDs = new long[256]; // maximum of 256 faces detected
                        long[] faceCount = new long[1];
                        
                        FSDK.FeedFrame(tracker, 0, imageHandle, faceCount, IDs); 
                        for (int i=0; i<faceCount[0]; ++i) {
                            FSDK.GetTrackerFacePosition(tracker, 0, IDs[i], facePosition);
                            
                            int left = facePosition.xc - (int)(facePosition.w * 0.6);
                            int top = facePosition.yc - (int)(facePosition.w * 0.5);
                            int w = (int)(facePosition.w * 1.2);
                            
                            bufImage = new BufferedImage(awtImage[0].getWidth(null), awtImage[0].getHeight(null), BufferedImage.TYPE_INT_ARGB);
                            Graphics gr = bufImage.getGraphics(); 
                            gr.drawImage(awtImage[0], 0, 0, null);
                            gr.setColor(Color.green);
                            gr.drawRect(left, top, w, w); // draw face rectangle
                            
                            String [] AttributeValues = new String[1];
                            
    			    int res = FSDK.GetTrackerFacialAttribute(tracker, 0, IDs[i], "Expression", AttributeValues, 1024);
			    if (FSDK.FSDKE_OK == res) { // draw Expression
                                float [] ConfidenceSmile = new float[1];
                                float [] ConfidenceEyesOpen = new float[1];
                                FSDK.GetValueConfidence(AttributeValues[0], "Smile", ConfidenceSmile);
                                FSDK.GetValueConfidence(AttributeValues[0], "EyesOpen", ConfidenceEyesOpen);
                                
                                String str = "Smile: " + Integer.toString((int)(ConfidenceSmile[0]*100))
                                             + "%; Eyes open: " + Integer.toString((int)(ConfidenceEyesOpen[0]*100)) + "%";
                                
                                gr.setFont(new Font("Arial", Font.BOLD, 16));
                                FontMetrics fm = gr.getFontMetrics();
                                java.awt.geom.Rectangle2D textRect = fm.getStringBounds(str, gr);
                                gr.drawString(str, (int)(facePosition.xc - textRect.getWidth()/2), (int)(top + w + textRect.getHeight()));
                            }
                        }
                        
                        // display current frame
                        mainFrame.getRootPane().getGraphics().drawImage((bufImage != null) ? bufImage : awtImage[0], 0, 0, null);
                    }
                    FSDK.FreeImage(imageHandle); // delete the FaceSDK image handle
                }
            }
        });
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        mainPanel.setName("mainPanel"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(Expressionrecognition.ExpressionRecognitionApp.class).getContext().getActionMap(ExpressionRecognitionView.class, this);
        jButton1.setAction(actionMap.get("buttonStart")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(Expressionrecognition.ExpressionRecognitionApp.class).getContext().getResourceMap(ExpressionRecognitionView.class);
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap(327, Short.MAX_VALUE)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(101, 101, 101))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap(314, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap())
        );

        jLabel1.getAccessibleContext().setAccessibleName(resourceMap.getString("jLabel1.AccessibleContext.accessibleName")); // NOI18N

        setComponent(mainPanel);
    }// </editor-fold>//GEN-END:initComponents

    @Action
    public void buttonStart() {
        this.jButton1.setEnabled(false);
        drawingTimer.start();
    }
    
    public void closeCamera(){
        FSDKCam.CloseVideoCamera(cameraHandle);
        FSDKCam.FinalizeCapturing();
        FSDK.Finalize();
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel mainPanel;
    // End of variables declaration//GEN-END:variables
    
    public final Timer drawingTimer;
    private HCamera cameraHandle;
}
