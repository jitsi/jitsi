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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The dialog created for a given call.
 * 
 * @author Yana Stamcheva
 */
public class CallDialog
    extends SIPCommFrame
    implements ActionListener
{
    private static final String DIAL_BUTTON = "DialButton";

    private static final String HANGUP_BUTTON = "HangupButton";

    private DialpadDialog dialpadDialog;

    private final CallPanel callPanel;

    /**
     * Creates a <tt>CallDialog</tt> by specifying the underlying call panel.
     * 
     * @param callPanel The underlying call panel.
     */
    public CallDialog(CallPanel callPanel)
    {
        this.callPanel = callPanel;

        this.setTitle(
            GuiActivator.getResources().getI18NString("service.gui.CALL"));

        this.setPreferredSize(new Dimension(500, 400));

        TransparentPanel buttonsPanel
            = new TransparentPanel(new BorderLayout(5, 5));

        SIPCommButton hangupButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));

        SIPCommButton dialButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.DIAL_BUTTON));

        dialButton.setName(DIAL_BUTTON);

        dialButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.DIALPAD"));

        dialButton.addActionListener(this);

        Container contentPane = getContentPane();
        contentPane.add(callPanel, BorderLayout.CENTER);
        contentPane.add(buttonsPanel, BorderLayout.SOUTH);

        hangupButton.setName(HANGUP_BUTTON);

        hangupButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.HANG_UP"));

        hangupButton.addActionListener(this);

        buttonsPanel.add(dialButton, BorderLayout.WEST);
        buttonsPanel.add(hangupButton, BorderLayout.EAST);

        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
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
            {
                Call call = callPanel.getCall();
                Iterator<CallParticipant> callParticipants =
                    (call == null) ? new Vector<CallParticipant>().iterator()
                        : callPanel.getCall().getCallParticipants();

                dialpadDialog = new DialpadDialog(callParticipants);
            }

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
}
