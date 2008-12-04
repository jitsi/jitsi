/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.swing.*;

/**
 * Represents an UI means to mute the audio stream sent to an associated
 * <tt>CallPariticant</tt>.
 * 
 * @author Lubomir Marinov
 */
public class MuteButton
    extends JToggleButton
{

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>CallParticipant</tt>.
     * 
     * @param callParticipant the <tt>CallParticipant</tt> to be associated with
     *            the new instance and to have the audio stream sent to muted
     */
    public MuteButton(CallParticipant callParticipant)
    {
        super(new ImageIcon(ImageLoader.getImage(ImageLoader.MUTE_BUTTON)));

        setModel(new MuteButtonModel(callParticipant));
    }

    /**
     * Represents the model of a toggle button that mutes the audio stream sent
     * to a specific <tt>CallParticipant</tt>.
     */
    private static class MuteButtonModel
        extends ToggleButtonModel
    {

        /**
         * The <tt>CallParticipant</tt> whose state is being adapted for the
         * purposes of depicting as a toggle button.
         */
        private final CallParticipant callParticipant;

        /**
         * Initializes a new <tt>MuteButtonModel</tt> instance to represent the
         * state of a specific <tt>CallParticipant</tt> as a toggle button.
         * 
         * @param callParticipant the <tt>CallParticipant</tt> whose state is to
         *            be represented as a toggle button
         */
        public MuteButtonModel(CallParticipant callParticipant)
        {
            this.callParticipant = callParticipant;

            addActionListener(new ActionListener()
            {

                /**
                 * Invoked when an action occurs.
                 * 
                 * @param evt the <tt>ActionEvent</tt> instance containing the
                 *            data associated with the action and the act of its
                 *            performing
                 */
                public void actionPerformed(ActionEvent evt)
                {
                    MuteButtonModel.this.actionPerformed(this, evt);
                }
            });
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

                telephony.setMute(callParticipant, !callParticipant.isMute());

                fireItemStateChanged(new ItemEvent(this,
                    ItemEvent.ITEM_STATE_CHANGED, this,
                    isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
                fireStateChanged();
            }
        }

        /**
         * Determines whether this model represents a state which should be
         * visualized by the currently depicting toggle button as selected.
         */
        public boolean isSelected()
        {
            return callParticipant.isMute();
        }
    }
}
