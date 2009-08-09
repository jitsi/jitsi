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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CallParticipantPanel</tt> is the panel containing data for a call
 * participant in a given call. It contains information like call participant
 * name, photo, call duration, etc.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class CallParticipantPanel
    extends TransparentPanel
{
    private static final Logger logger =
        Logger.getLogger(CallParticipantPanel.class);

    private final JLabel callStatusLabel = new JLabel();

    private final SecurityStatusLabel securityStatusLabel;

    private final JLabel muteStatusLabel = new JLabel();

    private final JLabel timeLabel = new JLabel("00:00:00", JLabel.CENTER);

    /**
     * This date is meant to be used in the GuiCallParticipantRecord, which is
     * added to the CallList after a call.
     */
    private final Date callStartTime = new Date(System.currentTimeMillis());

    private Date callDuration;

    private Timer timer;

    private String callType;

    private final String participantName;

    private final CallPeer callParticipant;

    private final java.util.List<Container> videoContainers =
        new ArrayList<Container>();

    private OperationSetVideoTelephony videoTelephony;

    private Component localVideo;

    private boolean isAudioSecurityOn = false;

    private boolean isVideoSecurityOn = false;

    private String encryptionCipher;

    /**
     * The current <code>Window</code> being displayed in full-screen. Because
     * the AWT API with respect to the full-screen support doesn't seem
     * sophisticated enough, the field is used sparingly i.e. when there are no
     * other means (such as a local variable) of acquiring the instance.
     */
    private Window fullScreenWindow;

    private SecurityPanel securityPanel = null;

    private final CallDialog callDialog;

    /**
     * Creates a <tt>CallParticipantPanel</tt> for the given call participant.
     *
     * @param callParticipant a call participant
     */
    public CallParticipantPanel(CallDialog callDialog,
                                CallPeer callParticipant)
    {
        this.callDialog = callDialog;
        this.callParticipant = callParticipant;
        this.participantName = callParticipant.getDisplayName();

        this.securityStatusLabel = new SecurityStatusLabel(
            this,
            new ImageIcon(ImageLoader.getImage(ImageLoader.SECURE_BUTTON_OFF)),
            JLabel.CENTER);

        // Initialize the date to 0
        // Need to use Calendar because new Date(0) returns a date where the
        // hour is initialized to 1.
        Calendar c = Calendar.getInstance();
        c.set(0, 0, 0, 0, 0, 0);
        this.callDuration = c.getTime();

        /* Create the main Components of the UI. */
        Component nameBar = createNameBar();
        Component center = createCenter();
        Component statusBar = createStatusBar();

        /* Lay out the main Components of the UI. */
        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        if (nameBar != null)
        {
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0;

            add(nameBar, constraints);
        }
        if (center != null)
        {
            /*
             * Don't let the center dictate the preferred size because it may
             * display large videos. Otherwise, the large video will make this
             * panel expand and then the panel's container will show scroll
             * bars.
             */
            center.setPreferredSize(new Dimension(1, 1));

            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;

            add(center, constraints);
        }
        if (statusBar != null)
        {
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);

            add(statusBar, constraints);
        }

        this.timer = new Timer(1000, new CallTimerListener());
        this.timer.setRepeats(true);

        addVideoListener();
    }

    private Component createButtonBar(  boolean heavyweight,
                                        Component[] buttons)
    {
        Container buttonBar
            = heavyweight ? new Container() : new TransparentPanel();

        buttonBar.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));

        for (Component button : buttons)
        {
            if (button != null)
                buttonBar.add(button);
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
        final JLabel photoLabel =
            new JLabel(new ImageIcon(ImageLoader
                .getImage(ImageLoader.DEFAULT_USER_PHOTO)));

        photoLabel.setPreferredSize(new Dimension(90, 90));

        final Container videoContainer = createVideoContainer(photoLabel);
        videoContainer.addHierarchyListener(new HierarchyListener()
        {
            public void hierarchyChanged(HierarchyEvent event)
            {
                int changeFlags = HierarchyEvent.DISPLAYABILITY_CHANGED;

                if ((event.getChangeFlags() & changeFlags) == changeFlags)
                {
                    synchronized (videoContainers)
                    {
                        boolean changed = false;

                        if (videoContainer.isDisplayable())
                        {
                            if (!videoContainers.contains(videoContainer))
                                changed = videoContainers.add(videoContainer);
                        }
                        else
                            changed = videoContainers.remove(videoContainer);
                        if (changed)
                            handleVideoEvent(null);
                    }
                }
            }
        });
        return videoContainer;
    }

    /**
     * Creates a new AWT <code>Container</code> which can display a single
     * <code>Component</code> at a time (supposedly, one which represents video)
     * and, in the absence of such a <code>Component</code>, displays a
     * predefined default <code>Component</code> (in accord with the previous
     * supposition, one which is the default when there is no video). The
     * returned <code>Container</code> will track the <code>Components</code>s
     * added to and removed from it in order to make sure that
     * <code>noVideoContainer</code> is displayed as described.
     *
     * @param noVideoComponent the predefined default <code>Component</code> to
     *            be displayed in the returned <code>Container</code> when there
     *            is no other <code>Component</code> in it
     * @return a new <code>Container</code> which can display a single
     *         <code>Component</code> at a time and, in the absence of such a
     *         <code>Component</code>, displays <code>noVideoComponent</code>
     */
    private Container createVideoContainer(Component noVideoComponent)
    {
        return new VideoContainer(noVideoComponent);
    }

    private Component createNameBar()
    {
        // nameLabel
        JLabel nameLabel = new JLabel("", JLabel.CENTER);
        nameLabel.setText(participantName);
        nameLabel.setAlignmentX(JLabel.CENTER);

        return nameLabel;
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
        // stateLabel
        callStatusLabel.setForeground(Color.WHITE);
        callStatusLabel.setText(callParticipant.getState().getStateString());

        ParticipantStatusPanel statusPanel
            = new ParticipantStatusPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 0));

        TransparentPanel statusIconsPanel
            = new TransparentPanel(
                new FlowLayout(FlowLayout.CENTER, 5, 0));

        timeLabel.setForeground(Color.WHITE);

        statusIconsPanel.add(securityStatusLabel);
        statusIconsPanel.add(muteStatusLabel);
        statusIconsPanel.add(callStatusLabel);

        statusPanel.add(timeLabel);
        statusPanel.add(statusIconsPanel);

        Component[] buttons =
            new Component[]
            {
                createTransferCallButton(),
                createEnterFullScreenButton()
            };

        Component buttonBar = createButtonBar(false, buttons);

        statusPanel.add(buttonBar);

        return statusPanel;
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

    public void createSecurityPanel(
        CallPeerSecurityOnEvent event)
    {
        Call call = callParticipant.getCall();

        if (call != null)
        {
            OperationSetSecureTelephony secure
                = (OperationSetSecureTelephony) call
                    .getProtocolProvider().getOperationSet(
                            OperationSetSecureTelephony.class);

            if (secure != null)
            {
                if (securityPanel == null)
                {
                    securityPanel = new SecurityPanel(callParticipant);

                    GridBagConstraints constraints = new GridBagConstraints();

                    constraints.fill = GridBagConstraints.NONE;
                    constraints.gridx = 0;
                    constraints.gridy = 2;
                    constraints.weightx = 0;
                    constraints.weighty = 0;
                    constraints.insets = new Insets(5, 0, 0, 0);

                    this.add(securityPanel, constraints);
                }

                securityPanel.refreshStates(event);

                this.revalidate();
            }
        }
    }

    private class VideoTelephonyListener
        implements PropertyChangeListener,
                   VideoListener
    {
        public void propertyChange(PropertyChangeEvent event)
        {
            if (OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING.equals(
                    event.getPropertyName()))
            {
                handleLocalVideoStreamingChange(
                        this);
            }
        }

        public void videoAdded(VideoEvent event)
        {
            handleVideoEvent(event);
        }

        public void videoRemoved(VideoEvent event)
        {
            handleVideoEvent(event);
        }
    }

    /**
     * Sets up listening to notifications about adding or removing video for the
     * <code>CallParticipant</code> this panel depicts and displays the video in
     * question in the last-known of {@link #videoContainers} (because the video
     * is represented by a <code>Component</code> and it cannot be displayed in
     * multiple <code>Container</code>s at one and the same time) as soon as it
     * arrives.
     */
    private OperationSetVideoTelephony addVideoListener()
    {
        final Call call = callParticipant.getCall();
        if (call == null)
            return null;

        final OperationSetVideoTelephony telephony =
            (OperationSetVideoTelephony) call.getProtocolProvider()
                .getOperationSet(OperationSetVideoTelephony.class);
        if (telephony == null)
            return null;

        final VideoTelephonyListener videoTelephonyListener
            = new VideoTelephonyListener();

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
                telephony.addVideoListener(
                        callParticipant, videoTelephonyListener);
                telephony.addPropertyChangeListener(
                        call, videoTelephonyListener);
                videoListenerIsAdded = true;

                synchronized (videoContainers)
                {
                    videoTelephony = telephony;

                    handleVideoEvent(null);

                    handleLocalVideoStreamingChange(
                            videoTelephonyListener);
                }
            }

            /*
             * When the #callParticipant of this CallParticipantPanel gets added
             * to the Call, starts listening for changes in the video in order
             * to display it.
             */
            public synchronized void callPeerAdded(
                CallPeerEvent event)
            {
                if (callParticipant.equals(event.getSourceCallPeer())
                        && !videoListenerIsAdded)
                {
                    Call call = callParticipant.getCall();

                    if ((call != null)
                            && CallState.CALL_IN_PROGRESS.equals(
                                    call.getCallState()))
                        addVideoListener();
                }
            }

            /*
             * When the #callParticipant of this CallParticipantPanel leaves the
             * Call, stops listening for changes in the video because it should
             * no longer be updated anyway.
             */
            public synchronized void callPeerRemoved(
                CallPeerEvent event)
            {
                if (callParticipant.equals(event.getSourceCallPeer())
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
                telephony.removeVideoListener(
                        callParticipant, videoTelephonyListener);
                telephony.removePropertyChangeListener(
                        call, videoTelephonyListener);
                videoListenerIsAdded = false;

                if (localVideo != null)
                {
                    telephony.disposeLocalVisualComponent(
                            callParticipant, localVideo);
                    localVideo = null;
                }

                synchronized (videoContainers)
                {
                    if (telephony.equals(videoTelephony))
                        videoTelephony = null;
                }

                exitFullScreen(fullScreenWindow);
            }
        };
        call.addCallChangeListener(callListener);
        callListener.callStateChanged(new CallChangeEvent(call,
            CallChangeEvent.CALL_STATE_CHANGE, null, call.getCallState()));

        return telephony;
    }

    /**
     * When a video is added or removed for the <code>callParticipant</code>,
     * makes sure to display or hide it respectively.
     *
     * @param event a <code>VideoEvent</code> describing the added visual
     *            <code>Component</code> representing video and the provider it
     *            was added into or <code>null</code> if such information is not
     *            available
     */
    private void handleVideoEvent(final VideoEvent event)
    {
        synchronized (videoContainers)
        {
            if ((event != null) && !event.isConsumed()
                && (event.getOrigin() == VideoEvent.LOCAL))
            {
                Component localVideo = event.getVisualComponent();

                switch (event.getType())
                {
                case VideoEvent.VIDEO_ADDED:
                    this.localVideo = localVideo;

                    /*
                     * Let the creator of the local visual Component know it
                     * shouldn't be disposed of because we're going to use it.
                     */
                    event.consume();
                    break;

                case VideoEvent.VIDEO_REMOVED:
                    if (this.localVideo == localVideo)
                    {
                        this.localVideo = null;
                    }
                    break;
                }
            }
        }

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    handleVideoEvent(event);
                }
            });
            return;
        }

        synchronized (videoContainers)
        {
            int videoContainerCount;

            if ((videoTelephony != null)
                && ((videoContainerCount = videoContainers.size()) > 0))
            {
                Container videoContainer =
                    videoContainers.get(videoContainerCount - 1);
                int zOrder = 0;

                videoContainer.removeAll();

                // LOCAL
                if (localVideo != null)
                {
                    videoContainer.add(localVideo, VideoLayout.LOCAL, zOrder++);

                    // If the local video is turned on, we ensure that the
                    // button is selected.
                    if (!callDialog.isVideoButtonSelected())
                        callDialog.setVideoButtonSelected(true);
                }

                // REMOTE
                Component[] videos =
                    videoTelephony.getVisualComponents(callParticipant);

                Component video =
                    ((videos == null) || (videos.length < 1)) ? null
                        : videos[0];

                if (video != null)
                    videoContainer
                        .add(video, VideoLayout.CENTER_REMOTE, zOrder++);

                videoContainer.validate();

                /*
                 * Without explicit repainting, the remote visual Component will
                 * not stay small after entering fullscreen, the Component shown
                 * when there's no video will show be shown beneath the video
                 * Component though the former has already been removed...
                 */
                videoContainer.repaint();
            }
        }
    }

    private void handleLocalVideoStreamingChange(
            VideoTelephonyListener listener)
    {
        synchronized (videoContainers)
        {
            if (videoTelephony == null)
                return;

            if (videoTelephony.isLocalVideoStreaming(callParticipant.getCall()))
            {
                try
                {
                    videoTelephony.createLocalVisualComponent(
                            callParticipant, listener);
                }
                catch (OperationFailedException ex)
                {
                    logger.error(
                            "Failed to create local video/visual Component.",
                            ex);
                }
            }
            else if (localVideo != null)
            {
                videoTelephony.disposeLocalVisualComponent(
                        callParticipant, localVideo);
                localVideo = null;
            }
        }
    }

    /**
     * Sets the state of the contained call participant by specifying the
     * state name and icon.
     *
     * @param state the state of the contained call participant
     * @param icon the icon of the state
     */
    public void setState(String state, Icon icon)
    {
        this.callStatusLabel.setText(state);
        this.callStatusLabel.setIcon(icon);
    }

    /**
     * Sets the secured status icon to the status panel.
     *
     * @param isSecured indicates if the call with this participant is
     * secured
     */
    public void setSecured(boolean isSecured)
    {
        if (isSecured)
            securityStatusLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.SECURE_BUTTON_ON)));
        else
            securityStatusLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.SECURE_BUTTON_OFF)));
    }

    /**
     * Sets the mute status icon to the status panel.
     *
     * @param isMute indicates if the call with this participant is
     * muted
     */
    public void setMute(boolean isMute)
    {
        if(isMute)
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));
        else
            muteStatusLabel.setIcon(null);
    }

    /**
     * Sets the audio security on or off.
     * 
     * @param isAudioSecurityOn indicates if the audio security is turned on or
     * off.
     */
    public void setAudioSecurityOn(boolean isAudioSecurityOn)
    {
        this.isAudioSecurityOn = isAudioSecurityOn;
    }

    /**
     * Sets the video security on or off.
     * 
     * @param isVideoSecurityOn indicates if the video security is turned on or
     * off.
     */
    public void setVideoSecurityOn(boolean isVideoSecurityOn)
    {
        this.isVideoSecurityOn = isVideoSecurityOn;
    }

    /**
     * Indicates if the audio security is turned on or off.
     * 
     * @return <code>true</code> if the audio security is on, otherwise -
     * <code>false</code>.
     */
    public boolean isAudioSecurityOn()
    {
        return isAudioSecurityOn;
    }

    /**
     * Indicates if the video security is turned on or off.
     * 
     * @return <code>true</code> if the video security is on, otherwise -
     * <code>false</code>.
     */
    public boolean isVideoSecurityOn()
    {
        return isVideoSecurityOn;
    }

    /**
     * Returns the cipher used for the encryption of the current call.
     * 
     * @return the cipher used for the encryption of the current call.
     */
    public String getEncryptionCipher()
    {
        return encryptionCipher;
    }

    /**
     * Sets the cipher used for the encryption of the current call.
     * 
     * @param encryptionCipher the cipher used for the encryption of the
     * current call.
     */
    public void setEncryptionCipher(String encryptionCipher)
    {
        this.encryptionCipher = encryptionCipher;
    }


    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
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
                    new Date(callParticipant.getCallDurationStartTime()));

            callDuration.setTime(time.getTime());

            timeLabel.setText(GuiUtils.formatTime(time));
        }
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
            return GuiCallPeerRecord.INCOMING_CALL;
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

    private Component createEnterFullScreenButton()
    {
        SIPCommButton button =
            new SIPCommButton(ImageLoader
                .getImage(ImageLoader.ENTER_FULL_SCREEN_BUTTON));

        button.setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.ENTER_FULL_SCREEN_TOOL_TIP"));
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                enterFullScreen();
            }
        });
        return button;
    }

    private Component createExitFullScreenButton()
    {
        JButton button =
            new SIPCommButton(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG),
                ImageLoader.getImage(ImageLoader.EXIT_FULL_SCREEN_BUTTON));

        button.setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.EXIT_FULL_SCREEN_TOOL_TIP"));
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                Object source = event.getSource();
                Frame fullScreenFrame =
                    (source instanceof Component) ? TransferCallButton
                        .getFrame((Component) source) : null;

                exitFullScreen(fullScreenFrame);
            }
        });
        return button;
    }

    private Component createFullScreenButtonBar()
    {
        CallPeerState participantState
            = callParticipant.getState();

        Component[] buttons =
            new Component[]
            {   new HoldButton( callParticipant.getCall(),
                                true,
                                CallPeerState.isOnHold(participantState)),
                new MuteButton( callParticipant.getCall(),
                                true,
                                callParticipant.isMute()),
                createExitFullScreenButton() };

        Component fullScreenButtonBar = createButtonBar(true, buttons);

        return fullScreenButtonBar;
    }

    private void enterFullScreen()
    {
        // Create the main Components of the UI.
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setTitle(getParticipantName());
        frame.setUndecorated(true);

        Component center = createCenter();
        final Component buttonBar = createFullScreenButtonBar();

        // Lay out the main Components of the UI.
        final Container contentPane = frame.getContentPane();
        contentPane.setLayout(new FullScreenLayout(false));
        if (buttonBar != null)
            contentPane.add(buttonBar, FullScreenLayout.SOUTH);
        if (center != null)
            contentPane.add(center, FullScreenLayout.CENTER);

        // Full-screen windows usually have black backgrounds.
        Color background = Color.black;
        contentPane.setBackground(background);
        setBackground(center, background);

        class FullScreenListener
            implements ContainerListener, KeyListener, WindowStateListener
        {
            public void componentAdded(ContainerEvent event)
            {
                Component child = event.getChild();

                child.addKeyListener(this);
            }

            public void componentRemoved(ContainerEvent event)
            {
                Component child = event.getChild();

                child.removeKeyListener(this);
            }

            public void keyPressed(KeyEvent event)
            {
                if (!event.isConsumed()
                    && (event.getKeyCode() == KeyEvent.VK_ESCAPE))
                {
                    event.consume();
                    exitFullScreen(frame);
                }
            }

            public void keyReleased(KeyEvent event)
            {
            }

            public void keyTyped(KeyEvent event)
            {
            }

            public void windowStateChanged(WindowEvent event)
            {
                switch (event.getID())
                {
                case WindowEvent.WINDOW_CLOSED:
                case WindowEvent.WINDOW_DEACTIVATED:
                case WindowEvent.WINDOW_ICONIFIED:
                case WindowEvent.WINDOW_LOST_FOCUS:
                    exitFullScreen(frame);
                    break;
                }
            }
        }
        FullScreenListener listener = new FullScreenListener();

        // Exit on Escape.
        addKeyListener(frame, listener);
        // Activate the above features for the local and remote videos.
        if (center instanceof Container)
            ((Container) center).addContainerListener(listener);
        // Exit when the "full screen" looses its focus.
        frame.addWindowStateListener(listener);

        getGraphicsConfiguration().getDevice().setFullScreenWindow(frame);
        this.fullScreenWindow = frame;
    }

    private void exitFullScreen(Window fullScreenWindow)
    {
        GraphicsConfiguration graphicsConfig = getGraphicsConfiguration();
        if (graphicsConfig != null)
            graphicsConfig.getDevice().setFullScreenWindow(null);

        if (fullScreenWindow != null)
        {
            if (fullScreenWindow.isVisible())
                fullScreenWindow.setVisible(false);
            fullScreenWindow.dispose();

            if (this.fullScreenWindow == fullScreenWindow)
                this.fullScreenWindow = null;
        }
    }

    private void setBackground(Component component, Color background)
    {
        component.setBackground(background);
        if (component instanceof Container)
        {
            Component[] components = ((Container) component).getComponents();
            for (Component c : components)
                setBackground(c, background);
        }
    }

    private void addKeyListener(Component component, KeyListener l)
    {
        component.addKeyListener(l);
        if (component instanceof Container)
        {
            Component[] components = ((Container) component).getComponents();
            for (Component c : components)
                addKeyListener(c, l);
        }
    }

    private static class ParticipantStatusPanel
        extends TransparentPanel
    {

        /*
         * Silence the serial warning. Though there isn't a plan to serialize
         * the instances of the class, there're no fields so the default
         * serialization routine will work.
         */
        private static final long serialVersionUID = 0L;

        public ParticipantStatusPanel(LayoutManager layout)
        {
            super(layout);
            this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        }

        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();

            try
            {
                AntialiasingManager.activateAntialiasing(g);

                g.setColor(Color.DARK_GRAY);
                g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);
            }
            finally
            {
                g.dispose();
            }
        }
    }
}
