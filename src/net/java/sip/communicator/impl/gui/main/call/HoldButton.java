/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.swing.*;

/**
 * Represents an UI means to put an associated <tt>CallPariticant</tt> on/off
 * hold.
 * 
 * @author Lubomir Marinov
 */
public class HoldButton
    extends JToggleButton
{

    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallParticipant</tt> on/off hold.
     * 
     * @param callParticipant the <tt>CallParticipant</tt> to be associated with
     *            the new instance and to be put on/off hold upon performing its
     *            action
     */
    public HoldButton(CallParticipant callParticipant)
    {
        super(new ImageIcon(ImageLoader.getImage(ImageLoader.HOLD_BUTTON)));

        setModel(new HoldButtonModel(callParticipant));
        setToolTipText(GuiActivator.getResources().getI18NString(
            "HoldButton_toolTipText"));
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
        private final CallParticipant callParticipant;

        /**
         * Initializes a new <tt>HoldButtonModel</tt> instance to represent
         * the state of a specific <tt>CallParticipant</tt> as a toggle
         * button.
         * 
         * @param callParticipant
         *            the <tt>CallParticipant</tt> whose state is to be
         *            represented as a toggle button
         */
        public HoldButtonModel(CallParticipant callParticipant)
        {
            this.callParticipant = callParticipant;

            InternalListener listener = new InternalListener();
            this.callParticipant.addCallParticipantListener(listener);
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
            Call call = callParticipant.getCall();

            if (call != null)
            {
                OperationSetBasicTelephony telephony =
                    (OperationSetBasicTelephony) call.getProtocolProvider()
                        .getOperationSet(OperationSetBasicTelephony.class);

                try
                {
                    if (isSelected())
                        telephony.putOffHold(callParticipant);
                    else
                        telephony.putOnHold(callParticipant);
                }
                catch (OperationFailedException ex)
                {
                    // TODO Auto-generated method stub
                }
            }
        }

        /**
         * Determines whether this model represents a state which should be
         * visualized by the currently depicting toggle button as selected.
         */
        public boolean isSelected()
        {
            CallParticipantState state = callParticipant.getState();
            return CallParticipantState.ON_HOLD_LOCALLY.equals(state)
                    || CallParticipantState.ON_HOLD_MUTUALLY.equals(state);
        }

        /**
         * Handles changes in the state of the source <tt>CallParticipant</tt>
         * on behalf of a specific <tt>CallParticipantListener</tt>.
         * 
         * @param listener the <tt>CallParticipantListener</tt> notified about
         *            the state change
         * @param evt the <tt>CallParticipantChangeEvent</tt> containing the
         *            source event as well as its previous and its new state
         */
        private void participantStateChanged(CallParticipantListener listener,
            CallParticipantChangeEvent evt)
        {
            CallParticipantState newState =
                (CallParticipantState) evt.getNewValue();
            CallParticipant callParticipant = evt.getSourceCallParticipant();

            fireItemStateChanged(new ItemEvent(this,
                ItemEvent.ITEM_STATE_CHANGED, this,
                isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
            fireStateChanged();

            /*
             * For the sake of completeness, try to not leave a listener on the
             * CallParticipant after it's no longer of interest.
             */
            if (CallParticipantState.DISCONNECTED.equals(newState))
            {
                callParticipant.removeCallParticipantListener(listener);
            }
        }

        /**
         * Represents the set of <tt>EventListener</tt>s this instance uses
         * to track the changes in its <tt>CallParticipant</tt> model.
         */
        private class InternalListener
            extends CallParticipantAdapter
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

            /**
             * Indicates that a change has occurred in the state of the source
             * CallParticipant.
             * 
             * @param evt The <tt>CallParticipantChangeEvent</tt> instance
             *            containing the source event as well as its previous
             *            and its new status.
             */
            public void participantStateChanged(CallParticipantChangeEvent evt)
            {
                HoldButtonModel.this.participantStateChanged(this, evt);
            }
        }
    }
}
