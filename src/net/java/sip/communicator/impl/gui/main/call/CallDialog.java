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
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.border.*;

/**
 * The dialog created for a given call.
 * 
 * @author Yana Stamcheva
 */
public class CallDialog
    extends SIPCommFrame
    implements ActionListener,
               MouseListener
{
    private static final String DIAL_BUTTON = "DIAL_BUTTON";

    private static final String HANGUP_BUTTON = "HANGUP_BUTTON";

    private DialpadDialog dialpadDialog;

    private final CallPanel callPanel;

    private final HoldButton holdButton;

    private final MuteButton muteButton;

    private final LocalVideoButton videoButton;

    /**
     * Creates a <tt>CallDialog</tt> by specifying the underlying call panel.
     */
    public CallDialog(Call call, String callType)
    {
        this.setTitle(
            GuiActivator.getResources().getI18NString("service.gui.CALL"));

        this.callPanel
            = new CallPanel(this, call, GuiCallPeerRecord.INCOMING_CALL);

        this.setPreferredSize(new Dimension(500, 400));

        TransparentPanel buttonsPanel
            = new TransparentPanel(new BorderLayout(5, 5));

        TransparentPanel settingsPanel
            = new TransparentPanel();

        SIPCommButton hangupButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));

        SIPCommButton dialButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
            ImageLoader.getImage(ImageLoader.DIAL_BUTTON));

        holdButton = new HoldButton(call);
        muteButton = new MuteButton(call);
        videoButton = new LocalVideoButton(call);

        dialButton.setName(DIAL_BUTTON);
        dialButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.DIALPAD"));
        dialButton.addActionListener(this);
        dialButton.addMouseListener(this);

        Container contentPane = getContentPane();
        contentPane.add(callPanel, BorderLayout.CENTER);
        contentPane.add(buttonsPanel, BorderLayout.SOUTH);

        hangupButton.setName(HANGUP_BUTTON);
        hangupButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.HANG_UP"));
        hangupButton.addActionListener(this);

        settingsPanel.add(dialButton);
        settingsPanel.add(holdButton);
        settingsPanel.add(muteButton);
        settingsPanel.add(videoButton);

        buttonsPanel.add(settingsPanel, BorderLayout.WEST);
        buttonsPanel.add(hangupButton, BorderLayout.EAST);

        buttonsPanel.setBorder(
            new ExtendedEtchedBorder(EtchedBorder.LOWERED, 1, 0, 0, 0));
    }

    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (buttonName.equals(HANGUP_BUTTON))
        {
            actionPerformedOnHangupButton();
        }
        else if (buttonName.equals(DIAL_BUTTON))
        {
            if (dialpadDialog == null)
                dialpadDialog = this.getDialpadDialog();

            if(!dialpadDialog.isVisible())
            {
                dialpadDialog.setSize(
                    this.getWidth() - 20,
                    dialpadDialog.getHeight());

                dialpadDialog.setLocation(
                    this.getX() + 10,
                    getLocationOnScreen().y + getHeight());

                dialpadDialog.setVisible(true);
                dialpadDialog.requestFocus();
            }
            else
            {
                dialpadDialog.setVisible(false);
            }
        }
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
        if (dialpadDialog == null)
            dialpadDialog = this.getDialpadDialog();
        dialpadDialog.removeWindowFocusListener(dialpadDialog);
    }

    public void mouseExited(MouseEvent e)
    {
        if (dialpadDialog == null)
            dialpadDialog = this.getDialpadDialog();
        dialpadDialog.addWindowFocusListener(dialpadDialog);
    }

    /**
     * Executes the action associated with the "Hang up" button which may be
     * invoked by clicking the button in question or closing this dialog.
     */
    private void actionPerformedOnHangupButton()
    {
        Call call = getCall();

        if (call != null)
            CallManager.hangupCall(call);

        this.dispose();
    }

    /**
     * Returns the <tt>Call</tt> corresponding to this CallDialog.
     * 
     * @return the <tt>Call</tt> corresponding to this CallDialog.
     */
    public Call getCall()
    {
        return (callPanel != null) ? callPanel.getCall() : null;
    }

    @Override
    protected void close(boolean isEscaped)
    {
        if (!isEscaped)
        {
            actionPerformedOnHangupButton();
        }
    }
    
    /**
     * Returns the <tt>DialpadDialog</tt> corresponding to this CallDialog.
     * 
     * @return the <tt>DialpadDialog</tt> corresponding to this CallDialog.
     */
    private DialpadDialog getDialpadDialog()
    {
        Call call = callPanel.getCall();
        Iterator<CallPeer> callParticipants =
            (call == null) ? new Vector<CallPeer>().iterator()
                : callPanel.getCall().getCallPeers();

        return new DialpadDialog(callParticipants);
    }

    /**
     * Returns <code>true</code> if the hold button is selected,
     * <code>false</code> - otherwise.
     * 
     * @return  <code>true</code> if the hold button is selected,
     * <code>false</code> - otherwise.
     */
    public boolean isHoldButtonSelected()
    {
        return holdButton.isSelected();
    }

    /**
     * Selects or unselects the hold button in this call dialog.
     * 
     * @param isSelected indicates if the hold button should be selected or not
     */
    public void setHoldButtonSelected(boolean isSelected)
    {
        this.holdButton.setSelected(true);
    }

    /**
     * Returns <code>true</code> if the mute button is selected,
     * <code>false</code> - otherwise.
     * 
     * @return  <code>true</code> if the mute button is selected,
     * <code>false</code> - otherwise.
     */
    public boolean isMuteButtonSelected()
    {
        return muteButton.isSelected();
    }

    /**
     * Selects or unselects the mute button in this call dialog.
     * 
     * @param isSelected indicates if the mute button should be selected or not
     */
    public void setMuteButtonSelected(boolean isSelected)
    {
        this.muteButton.setSelected(true);
    }

    /**
     * Returns <code>true</code> if the video button is selected,
     * <code>false</code> - otherwise.
     * 
     * @return  <code>true</code> if the video button is selected,
     * <code>false</code> - otherwise.
     */
    public boolean isVideoButtonSelected()
    {
        return videoButton.isSelected();
    }

    /**
     * Selects or unselects the video button in this call dialog.
     * 
     * @param isSelected indicates if the video button should be selected or not
     */
    public void setVideoButtonSelected(boolean isSelected)
    {
        this.videoButton.setSelected(true);
    }
}
