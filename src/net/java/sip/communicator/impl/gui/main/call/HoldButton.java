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
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Represents an UI means to put an associated <tt>CallPariticant</tt> on/off
 * hold.
 * 
 * @author Lubomir Marinov
 */
public class HoldButton
    extends SIPCommToggleButton
{
    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallParticipant</tt> on/off hold.
     * 
     * @param callParticipant the <tt>CallParticipant</tt> to be associated with
     *            the new instance and to be put on/off hold upon performing its
     *            action
     */
    public HoldButton(Call call)
    {
        super(
            ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
            ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
            ImageLoader.getImage(ImageLoader.HOLD_BUTTON),
            ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_PRESSED_BG));

        setModel(new HoldButtonModel(call));
        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.HOLD_BUTTON_TOOL_TIP"));
    }

    public HoldButton(Call call, boolean isFullScreenMode)
    {
        this(call);

        if (isFullScreenMode)
        {
            this.setBgImage(
                ImageLoader.getImage(ImageLoader.HOLD_BUTTON_FULL_SCREEN));
            this.setBgRolloverImage(
                ImageLoader.getImage(ImageLoader.HOLD_BUTTON_FULL_SCREEN));
            this.setPressedImage(
                ImageLoader.getImage(ImageLoader.HOLD_BUTTON_FULL_SCREEN));
            this.setIconImage(null);
        }
    }

    /**
     * Represents the model of a toggle button that puts an associated
     * <tt>CallParticipant</tt> on/off hold.
     */
    private static class HoldButtonModel
        extends ToggleButtonModel
    {

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

            InternalListener listener = new InternalListener();

            addActionListener(listener);
        }

        /**
         * Handles actions performed on this model on behalf of a specific
         * <tt>ActionListener</tt>.
         * 
         * @param listener the <tt>ActionListener</tt> notified about the
         *            performing of the action
         * @param evt the <tt>ActionEvent</tt> containing the data associated
         *            with the action and the act of its performing
         */
        private void actionPerformed(ActionListener listener, ActionEvent evt)
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

        /**
         * Represents the set of <tt>EventListener</tt>s this instance uses
         * to track the changes in its <tt>CallParticipant</tt> model.
         */
        private class InternalListener
            implements ActionListener
        {
            /**
             * Invoked when an action occurs.
             * 
             * @param evt the <tt>ActionEvent</tt> instance containing the data
             *            associated with the action and the act of its
             *            performing
             */
            public void actionPerformed(ActionEvent evt)
            {
                HoldButtonModel.this.actionPerformed(this, evt);
            }
        }
    }
}
