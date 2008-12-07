/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.service.gui.PopupDialog;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.swing.*;

public class ZrtpPanel extends TransparentPanel {
    /**
     * Default.
     */
    private static final long serialVersionUID = 1L;
    
    private CallParticipant participant;
    
    private JButton secButton;
    private JLabel sasLabel;
    private JLabel secMethod;
    private JLabel sessionType;

    private JButton secButtonV;
    private JLabel sasLabelV;
    private JLabel secMethodV;
    private JLabel sessionTypeV;
    
    private ImageIcon iconEncr;
    private ImageIcon iconEncrVerified;
    private ImageIcon iconEncrDisabled;

    private boolean sasVerified = false;
    
    GridLayout simpleLayout = new GridLayout(0, 4);

    public ZrtpPanel() {
        iconEncrVerified = new ImageIcon(
                ImageLoader.getImage(ImageLoader.ENCR_VERIFIED));
        iconEncr = new ImageIcon(ImageLoader.getImage(ImageLoader.ENCR));
        iconEncrDisabled = new ImageIcon(
                ImageLoader.getImage(ImageLoader.ENCR_DISABLED));
        setLayout(simpleLayout);
        
        simpleLayout.setHgap(5);
        simpleLayout.setVgap(2);

        setLayout(simpleLayout);
        
        simpleLayout.setHgap(10);
        simpleLayout.setVgap(3);
        
        secButton = new JButton("");
        sasLabel = new JLabel("", JLabel.CENTER);
        secMethod = new JLabel("None", JLabel.CENTER);
        sessionType = new JLabel("", JLabel.CENTER);

        secButtonV = new JButton(iconEncrDisabled);
        sasLabelV = new JLabel("", JLabel.CENTER);
        secMethodV = new JLabel("None", JLabel.CENTER);
        sessionTypeV = new JLabel("", JLabel.CENTER);

        setPreferredSize(new Dimension(350, 80));
    }

    public void addComponentsToPane() {

        add(new JLabel("Session", JLabel.CENTER));
        add(new JLabel("SAS", JLabel.CENTER));
        add(new JLabel("Status", JLabel.CENTER));
        add(new JLabel("Method", JLabel.CENTER));

        add(sessionType);
        add(sasLabel);
        add(secButton);
        secButton.setEnabled(false);
        add(secMethod);

        add(sessionTypeV);
        add(sasLabelV);
        add(secButtonV);
        add(secMethodV);

        // Action to trigger SAS verification
        secButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                boolean sucess = false;
                Call call = participant.getCall();

                if (call != null) {
                    OperationSetSecureTelephony secure = (OperationSetSecureTelephony) call
                            .getProtocolProvider().getOperationSet(
                                    OperationSetSecureTelephony.class);
                    if (secure != null) {
                        sucess = secure.setSasVerified(participant,
                                !sasVerified);
                    }

                    if (sucess) {
                        if (sasVerified) {
                            sasVerified = false;
                            secButton.setIcon(iconEncr);
                            // secButton.setText("Sec");
                        } else {
                            sasVerified = true;
                            secButton.setIcon(iconEncrVerified);
                            // secButton.setText("Ver");
                        }
                    }
                }

            }
        });
        revalidate();
        setVisible(true);
    }


    public void refreshStates(SecurityGUIEventZrtp securityEvent) {
        HashMap<String, Object> state = securityEvent.getStates();

        if (SecurityGUIEventZrtp.AUDIO.equals((String)state.get(SecurityGUIEventZrtp.SESSION_TYPE))) {
            refreshStatesAudio(securityEvent);

        }
        if (SecurityGUIEventZrtp.VIDEO.equals((String)state.get(SecurityGUIEventZrtp.SESSION_TYPE))) {
            refreshStatesVideo(securityEvent);
        }
        if (SecurityGUIEventZrtp.MSG_WARN.equals((String)state.get(SecurityGUIEventZrtp.SESSION_TYPE))) {
            String text = (String)state.get(SecurityGUIEventZrtp.MSG_TEXT);
            DisplayPopupMessage popup = new DisplayPopupMessage("ZRTP Security Warning", text);
            popup.start();
        }
        revalidate();
    }

    private void refreshStatesAudio(SecurityGUIEventZrtp securityEvent) {
        HashMap<String, Object> state = securityEvent.getStates();

        sessionType.setText("Audio");
        String sas = (String)state.get(SecurityGUIEventZrtp.SAS);
        if (sas != null) {
            // if SAS is provided then this event was sent by the ZRTP master (DH mode)
            // store this participant for action calls via SecureTelephony operations
            participant = (CallParticipant)securityEvent.getSource();
            sasLabel.setText(sas);
            Boolean verified = (Boolean)state.get(SecurityGUIEventZrtp.SAS_VERIFY);
            if (verified.booleanValue()) {
                secButton.setIcon(iconEncrVerified);
                secButton.setEnabled(true);
                sasVerified = true;
            }
            else {
                sasVerified = false;
            }
        }
        
        Boolean secure = (Boolean)state.get(SecurityGUIEventZrtp.SECURITY_CHANGE);
        if (secure != null) {
            if (secure.booleanValue()) {
                secButton.setIcon(iconEncr);
                secButton.setEnabled(true);
                secMethod.setText((String)state.get(SecurityGUIEventZrtp.CIPHER));
            }
            else {
                secButton.setIcon(iconEncrDisabled);
                secButton.setEnabled(false);
                secMethod.setText("");
                sasLabel.setText("");

            }
        }
    }

    private void refreshStatesVideo(SecurityGUIEventZrtp securityEvent) {
        HashMap<String, Object> state = securityEvent.getStates();

        sessionTypeV.setText("Video");
        String sas = (String)state.get(SecurityGUIEventZrtp.SAS);
        if (sas != null) {
            // if SAS is provided then this event was sent by the ZRTP master (DH mode)
            // store this participant for action calls via SecureTelephony operations
            participant = (CallParticipant)securityEvent.getSource();
            sasLabelV.setText(sas);
            Boolean verified = (Boolean)state.get(SecurityGUIEventZrtp.SAS_VERIFY);
            if (verified.booleanValue()) {
                secButtonV.setIcon(iconEncrVerified);
            }
        }
        
        Boolean secure = (Boolean)state.get(SecurityGUIEventZrtp.SECURITY_CHANGE);
        if (secure != null) {
            if (secure.booleanValue()) {
                secButtonV.setIcon(iconEncr);
                secMethodV.setText((String)state.get(SecurityGUIEventZrtp.CIPHER));
            }
            else {
                secButtonV.setIcon(iconEncrDisabled);
                secMethodV.setText("");
                sasLabelV.setText("");

            }
        }
    }
    
    /**
     * This small thread display messages that are relevant to the end user.
     * Use an own thread not to block ZRTP processing. 
     * 
     * @author Werner Dittmann <Werner.Dittmann@t-online.de>
     *
     */
    private class DisplayPopupMessage extends Thread {

        private final PopupDialog popupDialog;
        private final String message;
        private final String title;
        
        DisplayPopupMessage(String title, String message) {
            
            this.title = title;
            this.message = message;
            
            UIService uiService = GuiActivator.getUIService();

            // Obtain the current UI implementation PopupDialog
            popupDialog = uiService.getPopupDialog();
        }

        public void run() {
            popupDialog.showMessagePopupDialog(message, title, PopupDialog.INFORMATION_MESSAGE);
        }
   }
}
