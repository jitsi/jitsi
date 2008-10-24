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
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>CallParticipantPanel</tt> is the panel containing data for a call
 * participant in a given call. It contains information like call participant
 * name, photo, call duration, etc.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class CallParticipantPanel
    extends JPanel
{
    private final JLabel stateLabel = new JLabel("Unknown", JLabel.CENTER);

    private final JLabel timeLabel = new JLabel("00:00:00", JLabel.CENTER);

    /**
     * This date is meant to be used in the GuiCallParticipantRecord, which is
     * added to the CallList after a call.
     */
    private final Date callStartTime = new Date(System.currentTimeMillis());

    private Date conversationStartTime;

    private Date callDuration;

    private Timer timer;

    private String callType;

    private final String participantName;

    private final CallParticipant callParticipant;

    /**
     * Creates a <tt>CallParticipantPanel</tt> for the given call participant.
     * 
     * @param callManager the <tt>CallManager</tt> that manages the call
     * @param callParticipant a call participant
     */
    public CallParticipantPanel(CallParticipant callParticipant)
    {
        this.callParticipant = callParticipant;
        this.participantName = callParticipant.getAddress();

        // Initialize the date to 0
        // Need to use Calendar because new Date(0) returns a date where the
        // hour is initialized to 1.
        Calendar c = Calendar.getInstance();
        c.set(0, 0, 0, 0, 0, 0);
        this.callDuration = c.getTime();

        /* Create the main Components of the UI. */
        Component center = createCenter();
        Component buttonBar = createButtonBar();
        Component statusBar = createStatusBar();

        /* Lay out the main Components of the UI. */
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        if (center != null)
        {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;

            layout.setConstraints(center, constraints);
            add(center);
        }
        if (buttonBar != null)
        {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;

            layout.setConstraints(buttonBar, constraints);
            add(buttonBar);
        }
        if (statusBar != null)
        {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;

            layout.setConstraints(statusBar, constraints);
            add(statusBar);
        }

        setPreferredSize(new Dimension(130, 150));

        this.timer = new Timer(1000, new CallTimerListener());
        this.timer.setRepeats(true);
    }

    /**
     * Creates the <code>Component</code> hierarchy of the bar of buttons such
     * as Hold, Mute, Transfer, Secure.
     * 
     * @return the root of the <code>Component</code> hierarchy of the bar of
     *         buttons such as Hold, Mute, Transfer, Secure
     */
    private Component createButtonBar()
    {
        Component[] buttons =
            new Component[]
            { new HoldButton(this.callParticipant),
                new MuteButton(this.callParticipant),
                createTransferCallButton(), createSecureCallButton() };

        Container buttonBar = new JPanel(new GridLayout(1, 0));
        for (int buttonIndex = 0; buttonIndex < buttons.length; buttonIndex++)
        {
            Component button = buttons[buttonIndex];

            if (button != null)
            {
                button.setPreferredSize(new Dimension(24, 24));
                buttonBar.add(button);
            }
        }
        return buttonBar;
    }

    /**
     * Creates the <code>Component</code> hierarchy of the central area of this
     * <code>CallParticipantPanel</code> which displays the photo of the
     * <code>CallParticipant</code> or the video if any.
     * 
     * @return the root of the <code>Component</code> hierarchy of the central
     *         area of this <code>CallParticipantPanel</code> which displays the
     *         photo of the <code>CallParticipant</code> or the video if any
     */
    private Component createCenter()
    {
        JLabel photoLabel =
            new JLabel(new ImageIcon(ImageLoader
                .getImage(ImageLoader.DEFAULT_USER_PHOTO)));
        photoLabel.setPreferredSize(new Dimension(90, 90));

        JPanel center = new JPanel(new FitLayout());
        center.add(photoLabel);

        addVideoListener(center, photoLabel);

        return center;
    }

    /**
     * Creates the <code>Component</code> hierarchy of the area of
     * status-related information such as <code>CallParticipant</code> display
     * name, call duration, security status.
     * 
     * @return the root of the <code>Component</code> hierarchy of the area of
     *         status-related information such as <code>CallParticipant</code>
     *         display name, call duration, security status
     */
    private Component createStatusBar()
    {
        // nameLabel
        JLabel nameLabel = new JLabel("", JLabel.CENTER);
        nameLabel.setText(participantName);

        // stateLabel
        stateLabel.setText(callParticipant.getState().getStateString());

        // secureLabel
        Component secureLabel = createSecureCallLabel();

        Container namePanel = new JPanel(new GridLayout(0, 1));
        namePanel.add(nameLabel);
        namePanel.add(stateLabel);
        namePanel.add(timeLabel);
        if (secureLabel != null)
            namePanel.add(secureLabel);
        return namePanel;
    }

    /**
     * Creates a new <code>Component</code> representing a UI means to transfer
     * the <code>Call</code> of the associated <code>callParticipant</code> or
     * <tt>null</tt> if call-transfer is unsupported.
     * 
     * @return a new <code>Component</code> representing the UI means to
     *         transfer the <code>Call</code> of <code>callParticipant</code> or
     *         <tt>null</tt> if call-transfer is unsupported
     */
    private Component createTransferCallButton()
    {
        Call call = callParticipant.getCall();

        if (call != null)
        {
            OperationSetAdvancedTelephony telephony =
                (OperationSetAdvancedTelephony) call.getProtocolProvider()
                    .getOperationSet(OperationSetAdvancedTelephony.class);

            if (telephony != null)
                return new TransferCallButton(callParticipant);
        }
        return null;
    }

    /**
     * Creates a new <code>Component</code> representing a UI means to secure
     * the <code>Call</code> of the associated <code>callParticipant</code> or
     * <tt>null</tt> if secure call is unsupported.
     * 
     * @return a new <code>Component</code> representing the UI means to secure
     *         the <code>Call</code> of <code>callParticipant</code> or
     *         <tt>null</tt> if secure call is unsupported
     */
    private Component createSecureCallButton()
    {
        Call call = callParticipant.getCall();

        if (call != null)
        {
            OperationSetSecureTelephony secure =
                (OperationSetSecureTelephony) call.getProtocolProvider()
                    .getOperationSet(OperationSetSecureTelephony.class);

            if (secure != null)
            {
                SecureButton secureButton = new SecureButton(callParticipant);

                secureButton.setActionCommand("startSecureMode");
                secureButton.setName("secureButton");
                secureButton.setToolTipText(Messages.getI18NString(
                    "toggleOnSecurity").getText());

                call
                    .addSecureGUIComponent(secureButton.getName(), secureButton);
                return secureButton;
            }
        }
        return null;
    }

    private Component createSecureCallLabel()
    {
        Call call = callParticipant.getCall();

        if (call != null)
        {
            OperationSetSecureTelephony secure =
                (OperationSetSecureTelephony) call.getProtocolProvider()
                    .getOperationSet(OperationSetSecureTelephony.class);

            if (secure != null)
            {
                JLabel secureLabel = new JLabel("Not in call", JLabel.CENTER);

                secureLabel.setBorder(BorderFactory
                    .createLineBorder(Color.BLUE));
                secureLabel.setName("secureLabel");
                secureLabel.setPreferredSize(new Dimension(110, 50));
                secureLabel.setToolTipText(Messages.getI18NString(
                    "defaultSASMessage").getText());

                call.addSecureGUIComponent(secureLabel.getName(), secureLabel);
                return secureLabel;
            }
        }
        return null;
    }

    /**
     * Sets up listening to notifications about adding or removing video for the
     * <code>CallParticipant</code> this panel depicts and displays the video in
     * question in a specific visual <code>Container</code> (currently, the
     * central UI area) as soon as it arrives. If the video is removed at a
     * later point, the method reverts to showing a specific default visual
     * <code>Component</code> (currently, the photo of the
     * <code>CallParticipant</code>).
     * 
     * @param videoContainer the visual <code>Container</code> the display area
     *            of which will display video when it's available
     * @param noVideoComponent the default visual <code>Component</code> to be
     *            displayed in <code>videoContainer</code> when previously
     *            displayed video is no longer available
     */
    private void addVideoListener(final Container videoContainer,
        final Component noVideoComponent)
    {
        final Call call = callParticipant.getCall();
        if (call == null)
            return;

        final OperationSetVideoTelephony telephony =
            (OperationSetVideoTelephony) call.getProtocolProvider()
                .getOperationSet(OperationSetVideoTelephony.class);
        if (telephony == null)
            return;

        final VideoListener videoListener = new VideoListener()
        {
            public void videoAdded(VideoEvent event)
            {
                handleVideoEvent(telephony, videoContainer, noVideoComponent);
            }

            public void videoRemoved(VideoEvent event)
            {
                handleVideoEvent(telephony, videoContainer, noVideoComponent);
            }
        };

        /*
         * The video is only available while the #callParticipant is in a Call
         * and that call is in progress so only listen to VideoEvents during
         * that time.
         */
        CallChangeListener callListener = new CallChangeListener()
        {
            private boolean videoListenerIsAdded;

            private void addVideoListener()
            {
                telephony.addVideoListener(callParticipant, videoListener);
                videoListenerIsAdded = true;

                handleVideoEvent(telephony, videoContainer, noVideoComponent);
            }

            /*
             * When the #callParticipant of this CallParticipantPanel gets added
             * to the Call, starts listening for changes in the video in order
             * to display it.
             */
            public synchronized void callParticipantAdded(
                CallParticipantEvent event)
            {
                if (callParticipant.equals(event.getSourceCallParticipant())
                    && !videoListenerIsAdded)
                {
                    Call call = callParticipant.getCall();

                    if ((call != null)
                        && CallState.CALL_IN_PROGRESS.equals(call
                            .getCallState()))
                        addVideoListener();
                }
            }

            /*
             * When the #callParticipant of this CallParticipantPanel leaves the
             * Call, stops listening for changes in the video because it should
             * no longer be updated anyway.
             */
            public synchronized void callParticipantRemoved(
                CallParticipantEvent event)
            {
                if (callParticipant.equals(event.getSourceCallParticipant())
                    && videoListenerIsAdded)
                {
                    Call call = callParticipant.getCall();

                    if (call != null)
                        removeVideoListener();
                }
            }

            /*
             * When the Call of #callParticipant ends, stops tracking the
             * updates in the video because there should no longer be any video
             * anyway. When the Call in question starts, starts tracking any
             * changes to the video because it's negotiated and it should be
             * displayed in this CallParticipantPanel.
             */
            public synchronized void callStateChanged(CallChangeEvent event)
            {
                CallState newCallState = (CallState) event.getNewValue();

                if (CallState.CALL_ENDED.equals(newCallState))
                {
                    if (videoListenerIsAdded)
                        removeVideoListener();
                    call.removeCallChangeListener(this);
                }
                else if (CallState.CALL_IN_PROGRESS.equals(newCallState))
                {
                    if (!videoListenerIsAdded)
                        addVideoListener();
                }
            }

            private void removeVideoListener()
            {
                telephony.removeVideoListener(callParticipant, videoListener);
                videoListenerIsAdded = false;
            }
        };
        call.addCallChangeListener(callListener);
        callListener.callStateChanged(new CallChangeEvent(call,
            CallChangeEvent.CALL_STATE_CHANGE, null, call.getCallState()));
    }

    /**
     * When a video is added or removed for the <code>callParticipant</code>,
     * makes sure to display it or hide it respectively.
     * 
     * @param telephony the <code>OperationSetVideoTelephony</code> of
     *            <code>callParticipant</code> which gives access to the video
     * @param videoContainer the visual <code>Container</code> in which the
     *            video is to be displayed (currently, the central UI area
     *            displaying the photo when there is no video)
     * @param noVideoComponent the default visual <code>Component</code> to be
     *            displayed in <code>videoContainer</code> when the previously
     *            added video is no longer received from the
     *            <code>callParticipant</code> (currently, the photo)
     */
    private synchronized void handleVideoEvent(
        OperationSetVideoTelephony telephony, Container videoContainer,
        Component noVideoComponent)
    {
        Component[] videos = telephony.getVisualComponents(callParticipant);
        Component video =
            ((videos == null) || (videos.length < 1)) ? null : videos[0];

        videoContainer.removeAll();
        videoContainer.add((video == null) ? noVideoComponent : video);
        videoContainer.validate();
    }

    /**
     * Sets the state of the contained call participant.
     * 
     * @param state the state of the contained call participant
     */
    public void setState(String state)
    {
        this.stateLabel.setText(state);
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
        this.conversationStartTime = new Date(System.currentTimeMillis());
        this.timer.start();
    }

    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer()
    {
        this.timer.stop();
    }

    /**
     * Each second refreshes the time label to show to the user the exact
     * duration of the call.
     */
    private class CallTimerListener
        implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            Date time =
                GuiUtils.substractDates(new Date(System.currentTimeMillis()),
                    conversationStartTime);

            callDuration.setTime(time.getTime());

            timeLabel.setText(GuiUtils.formatTime(time));
        }
    }

    /**
     * Returns the start time of the conversation. If no conversation was made
     * will return null.
     * 
     * @return the start time of the conversation
     */
    public Date getConversationStartTime()
    {
        return conversationStartTime;
    }

    /**
     * Returns the start time of the contained participant call. Note that the
     * start time of the call is different from the conversation start time. For
     * example if we receive a call, the call start time is when the call is
     * received and the conversation start time would be when we accept the
     * call.
     * 
     * @return the start time of the contained participant call
     */
    public Date getCallStartTime()
    {
        return callStartTime;
    }

    /**
     * Returns the duration of the contained participant call.
     * 
     * @return the duration of the contained participant call
     */
    public Date getCallDuration()
    {
        return callDuration;
    }

    /**
     * Returns this call type - GuiCallParticipantRecord: INCOMING_CALL or
     * OUTGOING_CALL
     * 
     * @return Returns this call type : INCOMING_CALL or OUTGOING_CALL
     */
    public String getCallType()
    {
        if (callDuration != null)
            return callType;
        else
            return GuiCallParticipantRecord.INCOMING_CALL;
    }

    /**
     * Sets the type of the call. Call type could be
     * <tt>GuiCallParticipantRecord.INCOMING_CALL</tt> or
     * <tt>GuiCallParticipantRecord.INCOMING_CALL</tt>.
     * 
     * @param callType the type of call to set
     */
    public void setCallType(String callType)
    {
        this.callType = callType;
    }

    /**
     * Returns the name of the participant, contained in this panel.
     * 
     * @return the name of the participant, contained in this panel
     */
    public String getParticipantName()
    {
        return participantName;
    }
}
