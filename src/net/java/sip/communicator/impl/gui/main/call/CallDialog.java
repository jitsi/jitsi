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
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The dialog created for a given call.
 * 
 * @author Yana Stamcheva
 */
public class CallDialog
    extends SIPCommFrame
    implements ActionListener
{
    private Logger logger = Logger.getLogger(CallDialog.class);

    private static final String DIAL_BUTTON = "DialButton";

    private static final String HANGUP_BUTTON = "HangupButton";

    DialpadDialog dialpadDialog;

    private CallPanel callPanel;

    /**
     * Creates a <tt>CallDialog</tt> by specifying the underlying call panel.
     * 
     * @param callPanel The underlying call panel.
     */
    public CallDialog(CallPanel callPanel)
    {
        this.callPanel = callPanel;

        this.setTitle(GuiActivator.getResources().getI18NString("call"));

        this.setPreferredSize(new Dimension(500, 400));

        JPanel buttonsPanel = new JPanel(new BorderLayout(5, 5));

        SIPCommButton hangupButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.HANGUP_BUTTON_BG), ImageLoader
            .getImage(ImageLoader.HANGUP_ROLLOVER_BUTTON_BG), null, ImageLoader
            .getImage(ImageLoader.HANGUP_BUTTON_PRESSED_BG));

        SIPCommButton dialButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.DIAL_BUTTON),
            ImageLoader.getImage(ImageLoader.DIAL_BUTTON));

        dialButton.setName(DIAL_BUTTON);

        dialButton.setToolTipText(Messages.getI18NString("dialpad").getText());

        dialButton.addActionListener(this);

        this.getContentPane().add(callPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        hangupButton.setName(HANGUP_BUTTON);

        hangupButton.setToolTipText(
            Messages.getI18NString("hangUp").getText());

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
            Call call = callPanel.getCall();

            CallManager.hangupCall(call);

            this.dispose();

//            if (removeCallTimers.containsKey(callPanel))
//            {
//                ((Timer) removeCallTimers.get(callPanel)).stop();
//                removeCallTimers.remove(callPanel);
//            }
//
//            removeCallPanel(callPanel);
        }
        else if (buttonName.equals(DIAL_BUTTON))
        {
            if (dialpadDialog == null)
            {
                Call call = callPanel.getCall();

                if (call != null)
                {
                    dialpadDialog 
                        = new DialpadDialog(
                            callPanel.getCall().getCallParticipants());
                }
                else
                {
                    dialpadDialog = new DialpadDialog(new Vector().iterator());
                }
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
     * Returns the <tt>Call</tt> corresponding to this CallDialog.
     * 
     * @return the <tt>Call</tt> corresponding to this CallDialog.
     */
    public Call getCall()
    {
        return callPanel.getCall();
    }

    @Override
    protected void close(boolean isEscaped)
    {
    }
}
