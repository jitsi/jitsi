/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Represents an UI means to put an associated <tt>CallPariticant</tt> on/off
 * hold.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class HoldButton
    extends SIPCommToggleButton
    implements ActionListener
{
    private static final long serialVersionUID = 0L;

    private static final Logger logger = Logger.getLogger(HoldButton.class);

    private final Call call;

    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallPeer</tt> on/off hold.
     *
     * @param call the <tt>Call</tt> to be associated with
     *            the new instance and to be put on/off hold upon performing its
     *            action
     */
    public HoldButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallPeer</tt> on/off hold.
     *
     * @param call  the <tt>Call</tt> to be associated with
     *              the new instance and to be put on/off hold upon performing
     *              its action.
     * @param isFullScreenMode indicates if this button will be used in a normal
     *              or full screen mode.
     * @param isSelected indicates the initial state of this toggle button -
     *              selected or not.
     */
    public HoldButton(  Call call,
                        boolean isFullScreenMode,
                        boolean isSelected)
    {
        this.call = call;

        if (isFullScreenMode)
        {
            this.setBgImage(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG));
            this.setBgRolloverImage(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG));
            this.setIconImage(
                ImageLoader.getImage(ImageLoader.HOLD_BUTTON));
            this.setPressedImage(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG_PRESSED));
        }
        else
        {
            this.setBgImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG));
            this.setBgRolloverImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG));
            this.setIconImage(
                ImageLoader.getImage(ImageLoader.HOLD_BUTTON));
            this.setPressedImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_PRESSED_BG));
        }

        this.addActionListener(this);
        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.HOLD_BUTTON_TOOL_TIP"));
        setSelected(isSelected);
    }

    /**
     * Holds on or off call peers when the hold button is clicked.
     * @param evt the <tt>ActionEvent</tt> that notified us of the action
     */
    public void actionPerformed(ActionEvent evt)
    {
        if (call != null)
        {
            OperationSetBasicTelephony telephony =
                call.getProtocolProvider()
                    .getOperationSet(OperationSetBasicTelephony.class);

            Iterator<? extends CallPeer> peers = call.getCallPeers();

            // Obtain the isSelected property before invoking putOnHold,
            // because the property could change after putting on/off hold.
            boolean isHoldSelected = isSelected();

            while (peers.hasNext())
            {
                CallPeer callPeer = peers.next();

                try
                {
                    if (isHoldSelected)
                        telephony.putOnHold(callPeer);
                    else
                        telephony.putOffHold(callPeer);
                }
                catch (OperationFailedException ex)
                {
                    if (isHoldSelected)
                        logger.error("Failed to put on hold.", ex);
                    else
                        logger.error("Failed to put off hold.", ex);
                }
            }
        }
    }
}
