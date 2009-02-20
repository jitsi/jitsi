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
 * Represents an UI means to mute the audio stream sent to an associated
 * <tt>CallPariticant</tt>.
 * 
 * @author Lubomir Marinov
 */
public class MuteButton
    extends SIPCommToggleButton
{
    private static final long serialVersionUID = 0L;

	/**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>CallParticipant</tt>.
     * 
     * @param call the <tt>Call</tt> to be associated with
     *            the new instance and to have the audio stream sent to muted
     */
    public MuteButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>CallParticipant</tt>.
     * 
     * @param call  the <tt>Call</tt> to be associated with
     *              the new instance and to be put on/off hold upon performing
     *              its action.
     * @param isFullScreenMode indicates if this button will be used in a normal
     *              or full screen mode.
     * @param isSelected indicates the initial state of this toggle button -
     *              selected or not.
     */
    public MuteButton(Call call, boolean isFullScreenMode, boolean isSelected)
    {
        super();

        if (isFullScreenMode)
        {
            this.setBgImage(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG));
            this.setBgRolloverImage(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG));
            this.setIconImage(
                ImageLoader.getImage(ImageLoader.MUTE_BUTTON));
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
                ImageLoader.getImage(ImageLoader.MUTE_BUTTON));
            this.setPressedImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_PRESSED_BG));
        }

        setModel(new MuteButtonModel(call));
        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.MUTE_BUTTON_TOOL_TIP"));
        setSelected(isSelected);
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
        private final Call call;

        /**
         * Initializes a new <tt>MuteButtonModel</tt> instance to represent the
         * state of a specific <tt>CallParticipant</tt> as a toggle button.
         * 
         * @param call the <tt>Call</tt> whose state is to
         *            be represented as a toggle button
         */
        public MuteButtonModel(Call call)
        {
            this.call = call;

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
            if (call != null)
            {
                Iterator<CallParticipant> participants
                    = call.getCallParticipants();

                while (participants.hasNext())
                {
                    CallParticipant callParticipant = participants.next();

                    OperationSetBasicTelephony telephony
                        = (OperationSetBasicTelephony) call.getProtocolProvider()
                            .getOperationSet(OperationSetBasicTelephony.class);

                    telephony.setMute(  callParticipant,
                                        !callParticipant.isMute());

                    fireItemStateChanged(
                        new ItemEvent(this,
                        ItemEvent.ITEM_STATE_CHANGED, this,
                        isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));

                    fireStateChanged();
                }
            }
        }
    }
}
