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
 */
public class HoldButton
    extends SIPCommToggleButton
{
    private static final long serialVersionUID = 0L;

    private static final Logger logger = Logger.getLogger(HoldButton.class);

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

        setModel(new HoldButtonModel(call));
        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.HOLD_BUTTON_TOOL_TIP"));
        setSelected(isSelected);
    }

    /**
     * Represents the model of a toggle button that puts an associated
     * <tt>CallPeer</tt> on/off hold.
     */
    private static class HoldButtonModel
        extends ToggleButtonModel
        implements ActionListener
    {

        /**
         * The <tt>CallPeer</tt> whose state is being adapted for the
         * purposes of depicting as a toggle button.
         */
        private final Call call;

        /**
         * Initializes a new <tt>HoldButtonModel</tt> instance to represent
         * the state of a specific <tt>CallPeer</tt> as a toggle
         * button.
         *
         * @param call
         *            the <tt>Call</tt> whose state is to be
         *            represented as a toggle button
         */
        public HoldButtonModel(Call call)
        {
            this.call = call;

            addActionListener(this);
        }

        public void actionPerformed(ActionEvent evt)
        {
            if (call != null)
            {
                OperationSetBasicTelephony telephony =
                    (OperationSetBasicTelephony) call.getProtocolProvider()
                        .getOperationSet(OperationSetBasicTelephony.class);

                Iterator<? extends CallPeer> peers = call.getCallPeers();

                while (peers.hasNext())
                {
                    CallPeer callPeer = peers.next();

                    try
                    {
                        if (isSelected())
                            telephony.putOnHold(callPeer);
                        else
                            telephony.putOffHold(callPeer);
                    }
                    catch (OperationFailedException ex)
                    {
                        if (isSelected())
                            logger.error("Failed to put on hold.", ex);
                        else
                            logger.error("Failed to put off hold.", ex);
                    }
                }
            }
        }
    }
}
