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

	/**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallParticipant</tt> on/off hold.
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
     * <tt>CallParticipant</tt> on/off hold.
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
     * <tt>CallParticipant</tt> on/off hold.
     */
    private static class HoldButtonModel
        extends ToggleButtonModel
        implements ActionListener
    {

        private static final long serialVersionUID = 1L;
        /**
         * The <tt>CallParticipant</tt> whose state is being adapted for the
         * purposes of depicting as a toggle button.
         */
        private final Call call;

        /**
         * Initializes a new <tt>HoldButtonModel</tt> instance to represent
         * the state of a specific <tt>CallParticipant</tt> as a toggle
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

                Iterator<CallParticipant> participants
                    = call.getCallParticipants();

                while (participants.hasNext())
                {
                    CallParticipant callParticipant = participants.next();

                    try
                    {
                        if (isSelected())
                            telephony.putOnHold(callParticipant);
                        else
                            telephony.putOffHold(callParticipant);
                    }
                    catch (OperationFailedException ex)
                    {
                        // TODO Auto-generated method stub
                    }
                }
            }
        }
    }
}
