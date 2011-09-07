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
import javax.swing.border.*;
import javax.swing.event.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.border.*;

/**
 * The dialog created for a given call.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class CallPanel
    extends TransparentPanel
    implements ActionListener,
               CallChangeListener,
               CallPeerConferenceListener,
               PluginComponentListener,
               Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger for this class.
     */
    private static final Logger logger = Logger.getLogger(CallDialog.class);

    /**
     * The dial button name.
     */
    private static final String DIAL_BUTTON = "DIAL_BUTTON";

    /**
     * The conference button name.
     */
    private static final String CONFERENCE_BUTTON = "CONFERENCE_BUTTON";

    /**
     * The hang up button name.
     */
    private static final String HANGUP_BUTTON = "HANGUP_BUTTON";

    /**
     * The dial pad dialog opened when the dial button is clicked.
     */
    private DialpadDialog dialpadDialog;

    /**
     * The handler for DTMF tones.
     */
    private DTMFHandler dtmfHandler;

    /**
     * The panel containing call settings.
     */
    private final TransparentPanel settingsPanel = new TransparentPanel();

    /**
     * The panel representing the call. For conference calls this would be an
     * instance of <tt>ConferenceCallPanel</tt> and for one-to-one calls this
     * would be an instance of <tt>OneToOneCallPanel</tt>.
     */
    private JComponent callPanel = null;

    /**
     * The hold button.
     */
    private HoldButton holdButton;

    /**
     * The button which allows starting and stopping the recording of the
     * {@link #call}.
     */
    private RecordButton recordButton;

    /**
     * The video button.
     */
    private LocalVideoButton videoButton;

    /**
     * The button responsible for hiding/showing the local video.
     */
    private ShowHideVideoButton showHideVideoButton;

    /**
     * The video resize button.
     */
    private ResizeVideoButton resizeVideoButton;

    /**
     * The desktop sharing button.
     */
    private DesktopSharingButton desktopSharingButton;

    /**
     * The transfer call button.
     */
    private TransferCallButton transferCallButton;

    /**
     * The full screen button.
     */
    private FullScreenButton fullScreenButton;

    /**
     * The dial button, which opens a keypad dialog.
     */
    private SIPCommButton dialButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
        ImageLoader.getImage(ImageLoader.DIAL_BUTTON));

    /**
     * The conference button.
     */
    private SIPCommButton conferenceButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
        ImageLoader.getImage(ImageLoader.ADD_TO_CALL_BUTTON));

    /**
     * HangUp button.
     */
    private SIPCommButton hangupButton;

    /**
     * The call represented in this dialog.
     */
    private Call call;

    /**
     * Indicates if the last call was a conference call.
     */
    private boolean isLastConference = false;

    /**
     * The start date time of the call.
     */
    private Date callStartDate;

    /**
     * Indicates if the call timer has been started.
     */
    private boolean isCallTimerStarted = false;

    /**
     * A timer to count call duration.
     */
    private Timer callDurationTimer;

    /**
     * Parent window.
     */
    private CallContainer callWindow;

    /**
     * The title of this call container.
     */
    private String title;

    /**
     * Sound local level label.
     */
    private InputVolumeControlButton localLevel;

    /**
     * Sound remote level label.
     */
    private OutputVolumeControlButton remoteLevel;

    /**
     * A collection of listeners, registered for call title change events.
     */
    private Collection<CallTitleListener> titleListeners
        = new Vector<CallTitleListener>();

    /**
     * Creates an empty constructor allowing to extend this panel.
     */
    public CallPanel() {}

    /**
     * Creates a <tt>CallDialog</tt> by specifying the underlying call panel.
     *
     * @param call the <tt>call</tt> that this dialog represents
     * @param callWindow the parent call window, where this container is added
     */
    public CallPanel(Call call, CallContainer callWindow)
    {
        super(new BorderLayout());

        this.call = call;
        this.callWindow = callWindow;

        this.callDurationTimer = new Timer(1000, new CallTimerListener());
        this.callDurationTimer.setRepeats(true);

        // The call duration parameter is not known yet.
        this.setCallTitle(null);

        // Initializes the correct renderer panel depending on whether we're in
        // a single call or a conference call.
        this.isLastConference = isConference();

        if (isLastConference)
        {
            this.callPanel = new ConferenceCallPanel(this, call);
        }
        else
        {
            CallPeer callPeer = null;

            if (call.getCallPeers().hasNext())
                callPeer = call.getCallPeers().next();

            if (callPeer != null)
                this.callPanel = new OneToOneCallPanel(
                    this, call, callPeer);
        }

        // Adds a CallChangeListener that would receive events when a peer is
        // added or removed, or the state of the call has changed.
        call.addCallChangeListener(this);

        // Adds the CallPeerConferenceListener that would listen for changes in
        // the focus state of each call peer.
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();
        while (callPeers.hasNext())
        {
            callPeers.next().addCallPeerConferenceListener(this);
        }

        // Initializes all buttons and common panels.
        init();

        initPluginComponents();
    }

    /**
     * Initializes all buttons and common panels
     */
    private void init()
    {
        hangupButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));

        holdButton = new HoldButton(call);
        recordButton = new RecordButton(call);
        videoButton = new LocalVideoButton(call);
        showHideVideoButton = new ShowHideVideoButton(call);

        showHideVideoButton.setPeerRenderer(((CallRenderer) callPanel)
            .getCallPeerRenderer(call.getCallPeers().next()));

        // When the local video is enabled/disabled we ensure that the show/hide
        // local video button is selected/unselected.
        videoButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                boolean isVideoSelected = videoButton.isSelected();
                if (isVideoSelected)
                    settingsPanel.add(showHideVideoButton,
                        GuiUtils.getComponentIndex(
                            videoButton, settingsPanel) + 1);
                else
                    settingsPanel.remove(showHideVideoButton);

                showHideVideoButton.setEnabled(isVideoSelected);
                showHideVideoButton.setSelected(isVideoSelected);

                settingsPanel.revalidate();
                settingsPanel.repaint();
            }
        });

        desktopSharingButton = new DesktopSharingButton(call);
        transferCallButton = new TransferCallButton(call);
        fullScreenButton = new FullScreenButton(this);

        localLevel = new InputVolumeControlButton(
            call,
            ImageLoader.MICROPHONE,
            ImageLoader.MUTE_BUTTON,
            false, true, false);
        remoteLevel = new OutputVolumeControlButton(
                ImageLoader.VOLUME_CONTROL_BUTTON, false, true);

        dialButton.setName(DIAL_BUTTON);
        dialButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.DIALPAD"));
        dialButton.addActionListener(this);

        conferenceButton.setName(CONFERENCE_BUTTON);
        conferenceButton.setToolTipText(
            GuiActivator.getResources().getI18NString(
                "service.gui.CREATE_CONFERENCE_CALL"));
        conferenceButton.addActionListener(this);

        hangupButton.setName(HANGUP_BUTTON);
        hangupButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.HANG_UP"));
        hangupButton.addActionListener(this);

        /*
         * The buttons will be enabled once the call has entered in a connected
         * state.
         */
        dialButton.setEnabled(false);
        conferenceButton.setEnabled(false);
        holdButton.setEnabled(false);
        recordButton.setEnabled(false);

        settingsPanel.add(dialButton);
        settingsPanel.add(conferenceButton);
        settingsPanel.add(holdButton);
        settingsPanel.add(recordButton);

        if (!isLastConference)
        {
            // Buttons would be enabled once the call has entered in state
            // connected.
            transferCallButton.setEnabled(false);
            desktopSharingButton.setEnabled(false);
            videoButton.setEnabled(false);
            showHideVideoButton.setEnabled(false);
            fullScreenButton.setEnabled(false);

            addOneToOneSpecificComponents();
        }
        else
        {
            // These buttons are only added in the conference call. For the one
            // to one call mute and sound buttons are in the call peer panel.
            localLevel.setEnabled(false);
            remoteLevel.setEnabled(false);

            addConferenceSpecificComponents();
        }

        dtmfHandler = new DTMFHandler(this);

        JComponent bottomBar = createBottomBar();

        add(callPanel, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }

    /**
     * Handles action events.
     * @param evt the <tt>ActionEvent</tt> that was triggered
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (buttonName.equals(HANGUP_BUTTON))
        {
            actionPerformedOnHangupButton(false);
        }
        else if (buttonName.equals(DIAL_BUTTON))
        {
            if (dialpadDialog == null)
            {
                dialpadDialog = this.getDialpadDialog();
            }

            if(!dialpadDialog.isVisible())
            {
                dialpadDialog.pack();

                Point location = new Point( button.getX(),
                                            button.getY() + button.getHeight());
                SwingUtilities.convertPointToScreen(
                    location, button.getParent());

                dialpadDialog.setLocation(
                    (int) location.getX() + 2,
                    (int) location.getY() + 2);

                dialpadDialog.addWindowFocusListener(dialpadDialog);
                dialpadDialog.setVisible(true);
            }
            else
            {
                dialpadDialog.removeWindowFocusListener(dialpadDialog);
                dialpadDialog.setVisible(false);
            }
        }
        else if (buttonName.equals(CONFERENCE_BUTTON))
        {
            ConferenceInviteDialog inviteDialog
                = new ConferenceInviteDialog(call);

            inviteDialog.setVisible(true);
        }
    }

    /**
     * Executes the action associated with the "Hang up" button which may be
     * invoked by clicking the button in question or closing this dialog.
     */
    public void actionPerformedOnHangupButton(boolean isCloseWait)
    {
        Call call = getCall();

        NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);
        NotificationManager.stopSound(NotificationManager.BUSY_CALL);

        if (call != null)
            CallManager.hangupCall(call);

        if (isCloseWait)
            callWindow.closeWait(this);
        else
            callWindow.close(this);
    }

    /**
     * Returns the <tt>Call</tt> corresponding to this CallDialog.
     *
     * @return the <tt>Call</tt> corresponding to this CallDialog.
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * Returns the parent call window.
     *
     * @return the parent call window
     */
    public CallContainer getCallWindow()
    {
        return callWindow;
    }

    /**
     * Returns the <tt>DialpadDialog</tt> corresponding to this CallDialog.
     *
     * @return the <tt>DialpadDialog</tt> corresponding to this CallDialog.
     */
    private DialpadDialog getDialpadDialog()
    {
        return new DialpadDialog(dtmfHandler);
    }

    /**
     * Updates the state of the general hold button. The hold button is selected
     * only if all call peers are locally or mutually on hold at the same time.
     * In all other cases the hold button is unselected.
     */
    public void updateHoldButtonState()
    {
        Iterator<? extends CallPeer> peers = call.getCallPeers();

        boolean isAllLocallyOnHold = true;
        while (peers.hasNext())
        {
            CallPeer peer = peers.next();

            CallPeerState state = peer.getState();

            // If we have clicked the hold button in a full screen mode
            // we need to update the state of the call dialog hold button.
            if (!state.equals(CallPeerState.ON_HOLD_LOCALLY)
                && !state.equals(CallPeerState.ON_HOLD_MUTUALLY))
            {
                isAllLocallyOnHold = false;
                break;
            }
        }

        // If we have clicked the hold button in a full screen mode or selected
        // hold of the peer menu in a conference call we need to update the
        // state of the call dialog hold button.
        this.holdButton.setSelected(isAllLocallyOnHold);
    }

    /**
     * Selects or unselects the video button in this call dialog.
     *
     * @param isSelected indicates if the video button should be selected or not
     */
    public void setVideoButtonSelected(boolean isSelected)
    {
        if (isSelected && !videoButton.isSelected())
            videoButton.setSelected(true);
        else if (!isSelected && videoButton.isSelected())
            videoButton.setSelected(false);
    }

    /**
     * Returns <tt>true</tt> if the video button is currently selected,
     * <tt>false</tt> - otherwise.
     *
     * @return <tt>true</tt> if the video button is currently selected,
     * <tt>false</tt> - otherwise
     */
    public boolean isVideoButtonSelected()
    {
        return videoButton.isSelected();
    }

    /**
     * Selects or unselects the show/hide video button in this call dialog.
     *
     * @param isSelected indicates if the show/hide video button should be
     * selected or not
     */
    public void setShowHideVideoButtonSelected(boolean isSelected)
    {
        if (isSelected && !showHideVideoButton.isSelected())
            showHideVideoButton.setSelected(true);
        else if (!isSelected && showHideVideoButton.isSelected())
            showHideVideoButton.setSelected(false);
    }

    /**
     * Returns <tt>true</tt> if the show/hide video button is currently selected,
     * <tt>false</tt> - otherwise.
     *
     * @return <tt>true</tt> if the show/hide video button is currently selected,
     * <tt>false</tt> - otherwise
     */
    public boolean isShowHideVideoButtonSelected()
    {
        return showHideVideoButton.isSelected();
    }

    /**
     * Selects or unselects the desktop sharing button in this call dialog.
     *
     * @param isSelected indicates if the video button should be selected or not
     */
    public void setDesktopSharingButtonSelected(boolean isSelected)
    {
        if (logger.isTraceEnabled())
            logger.trace("Desktop sharing enabled: " + isSelected);

        if (isSelected && !desktopSharingButton.isSelected())
            desktopSharingButton.setSelected(true);
        else if (!isSelected && desktopSharingButton.isSelected())
            desktopSharingButton.setSelected(false);

        if (callPanel instanceof OneToOneCallPanel)
        {
            if (isSelected
                && call.getProtocolProvider()
                    .getOperationSet(
                        OperationSetDesktopSharingServer.class) != null)
            {
                ((OneToOneCallPanel) callPanel)
                    .addDesktopSharingComponents();
            }
            else
                ((OneToOneCallPanel) callPanel)
                    .removeDesktopSharingComponents();
        }
    }

    /**
     * Enables or disable some setting buttons when we get on/off hold.
     *
     * @param hold true if we are on hold, false otherwise
     */
    public void enableButtonsWhileOnHold(boolean hold)
    {
        dialButton.setEnabled(!hold);

        ProtocolProviderService protocolProvider
        = call.getProtocolProvider();

        OperationSetVideoTelephony videoTelephony
            = protocolProvider.getOperationSet(
                    OperationSetVideoTelephony.class);

        MediaDevice videoDevice = GuiActivator.getMediaService()
            .getDefaultDevice(MediaType.VIDEO, MediaUseCase.CALL);

        // If the video telephony is supported and the default video device
        // isn't null, i.e. there's an available camera to the video we
        // enable the video button.
        if (videoTelephony != null && videoDevice != null)
        {
            videoButton.setEnabled(!hold);

            // If the video was already enabled (for example in the case of
            // direct video call) make sure the video button is selected.
            if (videoTelephony.isLocalVideoAllowed(call)
                    && !videoButton.isSelected())
                setVideoButtonSelected(!hold);
        }
        else if (videoDevice == null)
            videoButton.setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.NO_CAMERA_AVAILABLE"));

        OperationSetDesktopSharingServer desktopSharing
            = protocolProvider.getOperationSet(
                OperationSetDesktopSharingServer.class);

        if (desktopSharing != null)
        {
            desktopSharingButton.setEnabled(!hold);

            // If the video was already enabled (for example in the case of
            // direct desktop sharing call) make sure the video button is
            // selected.
            if (desktopSharing.isLocalVideoAllowed(call)
                && !desktopSharingButton.isSelected())
                setDesktopSharingButtonSelected(!hold);
        }
    }

    /**
     * Enables or disable all setting buttons.
     */
    public void enableButtons(boolean enable)
    {
        // Buttons would be enabled once the call has entered in state
        // connected.
        dialButton.setEnabled(enable);
        conferenceButton.setEnabled(enable);
        holdButton.setEnabled(enable);
        recordButton.setEnabled(enable);
        localLevel.setEnabled(enable);
        remoteLevel.setEnabled(enable);

        if (!isLastConference)
        {
            // Buttsons would be enabled once the call has entered in state
            // connected.
            ProtocolProviderService protocolProvider
                = call.getProtocolProvider();

            if (call.getCallPeers().hasNext())
            {
                CallPeer callPeer = call.getCallPeers().next();
                enableButtonsWhileOnHold(
                    callPeer.getState() == CallPeerState.ON_HOLD_LOCALLY
                    || callPeer.getState() == CallPeerState.ON_HOLD_MUTUALLY
                    || callPeer.getState() == CallPeerState.ON_HOLD_REMOTELY);
            }

            if (protocolProvider.getOperationSet(
                OperationSetAdvancedTelephony.class) != null)
            {
                transferCallButton.setEnabled(enable);
            }

            if (protocolProvider.getOperationSet(
                OperationSetVideoTelephony.class) != null)
            {
                videoButton.setEnabled(enable);
                fullScreenButton.setEnabled(enable);
                desktopSharingButton.setEnabled(enable);
            }
        }
    }

    /**
     * Implements the <tt>CallChangeListener.callPeerAdded</tt> method.
     * Adds the according user interface when a new peer is added to the call.
     * @param evt the <tt>CallPeerEvent</tt> that notifies us for the change
     */
    public void callPeerAdded(final CallPeerEvent evt)
    {
        if (evt.getSourceCall() != call)
            return;

        final CallPeer callPeer = evt.getSourceCallPeer();

        callPeer.addCallPeerConferenceListener(this);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (isLastConference)
                {
                    ((ConferenceCallPanel) callPanel)
                        .addCallPeerPanel(callPeer);
                }
                else
                {
                    isLastConference = isConference();

                    // We've been in one-to-one call and we're now in a
                    // conference.
                    if (isLastConference)
                    {
                        remove(callPanel);
                        updateCurrentCallPanel(
                            new ConferenceCallPanel(CallPanel.this, call));
                        add(callPanel, BorderLayout.CENTER);
                    }
                    // We're still in one-to-one call and we receive the
                    // remote peer.
                    else
                    {
                        CallPeer onlyCallPeer = null;
                        if (call.getCallPeers().hasNext())
                            onlyCallPeer = call.getCallPeers().next();

                        if (onlyCallPeer != null)
                            ((OneToOneCallPanel) callPanel)
                                .addCallPeerPanel(onlyCallPeer);
                    }
                }

                refreshContainer();
            }
        });
    }

    /**
     * Implements the <tt>CallChangeListener.callPeerRemoved</tt> method.
     * Removes all related user interface when a peer is removed from the call.
     * @param evt the <tt>CallPeerEvent</tt> that has been triggered
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {
        if (evt.getSourceCall() != call)
            return;

        CallPeer callPeer = evt.getSourceCallPeer();

        callPeer.removeCallPeerConferenceListener(this);

        Timer timer = new Timer(5000,
            new RemovePeerPanelListener(callPeer));

        timer.setRepeats(false);
        timer.start();

        // The call is finished when that last peer is removed.
        if (call.getCallPeerCount() == 0)
        {
            this.stopCallTimer();
        }
    }

    public void callStateChanged(CallChangeEvent evt)
    {
    }

    /**
     * Updates <tt>CallPeer</tt> related components to fit the new focus state.
     * @param conferenceEvent the event that notified us of the change
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent conferenceEvent)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (!isLastConference)
                {
                    isLastConference = isConference();

                    // We've been in one-to-one call and we're now in a
                    // conference.
                    if (isLastConference)
                    {
                        removeOneToOneSpecificComponents();
                        remove(callPanel);
                        updateCurrentCallPanel(
                            new ConferenceCallPanel(CallPanel.this, call));
                        add(callPanel, BorderLayout.CENTER);
                    }
                }

                refreshContainer();
            }
        });
    }

    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent)
    {}

    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent)
    {}

    /**
     * Checks if the contained call is a conference call.
     *
     * @return <code>true</code> if the contained <tt>Call</tt> is a conference
     * call, otherwise - returns <code>false</code>.
     */
    public boolean isConference()
    {
        // If we're the focus of the conference.
        if (call.isConferenceFocus())
            return true;

        // If one of our peers is a conference focus, we're in a
        // conference call.
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        while (callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();

            if (callPeer.isConferenceFocus())
                return true;
        }

        // the call can have two peers at the same time and there is no one
        // is conference focus. This is situation when some one has made an
        // attended transfer and has transfered us. We have one call with two
        // peers the one we are talking to and the one we have been transfered
        // to. And the first one is been hanguped and so the call passes through
        // conference call fo a moment and than go again to one to one call.
        return call.getCallPeerCount() > 1;
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
        this.callStartDate = new Date();
        this.callDurationTimer.start();
        this.isCallTimerStarted = true;
    }

    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer()
    {
        this.callDurationTimer.stop();
    }

    /**
     * Returns <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>
     */
    public boolean isCallTimerStarted()
    {
        return isCallTimerStarted;
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        dialButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG));

        dialButton.setIconImage(
                ImageLoader.getImage(ImageLoader.DIAL_BUTTON));

        conferenceButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG));
        conferenceButton.setIconImage(
                ImageLoader.getImage(ImageLoader.ADD_TO_CALL_BUTTON));

        if(hangupButton != null)
            hangupButton.setBackgroundImage(
                    ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));
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
                GuiUtils.substractDates(new Date(), callStartDate);

            setCallTitle(time);
        }
    }

    /**
     * Sets the title of this dialog by specifying the call duration.
     * @param callDuration the duration of the call represented as Date object
     */
    private void setCallTitle(Date callDuration)
    {
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        if (callDuration != null)
            title = GuiUtils.formatTime(callDuration) + " | ";
        else
            title = "00:00:00 | ";

        if (callPeers.hasNext()
                && GuiActivator.getUIService()
                    .getSingleWindowContainer() != null)
            title += callPeers.next().getDisplayName();
        else
            title += GuiActivator.getResources()
                        .getI18NString("service.gui.CALL");

        fireTitleChangeEvent();
    }

    /**
     * Returns the initial call title. The call title could be then changed by
     * call setCallTitle.
     *
     * @return the call title
     */
    public String getCallTitle()
    {
        return title;
    }

    /**
     * Removes the given CallPeer panel from this CallPanel.
     */
    private class RemovePeerPanelListener
        implements ActionListener
    {
        private CallPeer peer;

        public RemovePeerPanelListener(CallPeer peer)
        {
            this.peer = peer;
        }

        public void actionPerformed(ActionEvent e)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    if (isLastConference)
                    {
                        if (call.getCallPeerCount() == 1)
                        {
                            isLastConference = false;

                            remove(callPanel);
                            CallPeer singlePeer = call.getCallPeers().next();

                            // if the other party is focus we want to see
                            // his members
                            if (singlePeer != null
                                && !singlePeer.isConferenceFocus())
                                updateCurrentCallPanel(new OneToOneCallPanel(
                                    CallPanel.this, call, singlePeer));
                            else if(singlePeer.isConferenceFocus())
                            {
                                ((ConferenceCallPanel) callPanel)
                                    .removeCallPeerPanel(peer);
                            }

                            add(callPanel, BorderLayout.CENTER);
                        }
                        else if (call.getCallPeerCount() > 1)
                        {
                            ((ConferenceCallPanel) callPanel)
                                .removeCallPeerPanel(peer);
                        }
                        else
                        {
                            // when in conference and the focus closes the
                            // conference we receive event for peer remove and
                            // there are no other peers in the call, so dispose
                            // the window
                            callWindow.close(CallPanel.this);
                        }

                        refreshContainer();
                    }
                    else
                    {
                        // Dispose the window if there are no peers
                        if (call.getCallPeerCount() < 1)
                            callWindow.close(CallPanel.this);
                    }
                }
            });
        }
    }

    /**
     * Refreshes the content of this dialog.
     */
    public void refreshContainer()
    {
        if (!isVisible())
            return;

        validate();

        // Calling pack would resize the window to fit the new content. We'd
        // like to use the whole possible space before showing the scroll bar.
        // Needed a workaround for the following problem:
        // When window reaches its maximum size (and the scroll bar is visible?)
        // calling pack() results in an incorrect repainting and makes the
        // whole window to freeze.
        // We check also if the vertical scroll bar is visible in order to
        // correctly pack the window when a peer is removed.
        boolean isScrollBarVisible = (callPanel instanceof ConferenceCallPanel)
            && ((ConferenceCallPanel) callPanel).getVerticalScrollBar() != null
            && ((ConferenceCallPanel) callPanel).getVerticalScrollBar()
                .isVisible();

        if (!isScrollBarVisible
            || getHeight()
                < GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds().height)
            callWindow.pack();
        else
            repaint();
    }

    /**
     * Returns the currently used <tt>CallRenderer</tt>.
     * @return the currently used <tt>CallRenderer</tt>
     */
    public CallRenderer getCurrentCallRenderer()
    {
        return (CallRenderer) callPanel;
    }

    /**
     * Adds remote video specific components.
     */
    public void addRemoteVideoSpecificComponents(CallPeer callPeer)
    {
        if(CallManager.isVideoQualityPresetSupported(callPeer))
        {
            if(resizeVideoButton == null)
                resizeVideoButton = new ResizeVideoButton(call);

            if(resizeVideoButton.countAvailableOptions() > 1)
                settingsPanel.add(resizeVideoButton);
        }

        settingsPanel.add(fullScreenButton);
        settingsPanel.revalidate();
        settingsPanel.repaint();
    }

    /**
     * Remove remote video specific components.
     */
    public void removeRemoteVideoSpecificComponents()
    {
        if(resizeVideoButton != null)
            settingsPanel.remove(resizeVideoButton);

        settingsPanel.remove(fullScreenButton);
        settingsPanel.revalidate();
        settingsPanel.repaint();
    }

    /**
     * Replaces the current call panel with the given one.
     * @param callPanel the <tt>JComponent</tt> to replace the current
     * call panel
     */
    private void updateCurrentCallPanel(JComponent callPanel)
    {
        this.callPanel = callPanel;

        if (callPanel instanceof OneToOneCallPanel)
        {
            removeConferenceSpecificComponents();
            addOneToOneSpecificComponents();
        }
        else
        {
            removeOneToOneSpecificComponents();
            addConferenceSpecificComponents();
        }
    }

    /**
     * Removes components specific for the one-to-one call.
     */
    private void removeOneToOneSpecificComponents()
    {
        // Disable video.
        if (videoButton.isSelected())
            videoButton.doClick();

        // Disable desktop sharing.
        if (desktopSharingButton.isSelected())
            desktopSharingButton.doClick();

        // Disable full screen.
        if (fullScreenButton.isSelected())
            fullScreenButton.doClick();

        settingsPanel.remove(videoButton);
        settingsPanel.remove(showHideVideoButton);

        if(resizeVideoButton != null)
            settingsPanel.remove(resizeVideoButton);

        settingsPanel.remove(desktopSharingButton);
        settingsPanel.remove(transferCallButton);
        settingsPanel.remove(fullScreenButton);
    }

    /**
     * Adds components specific for the one-to-one call.
     */
    private void addOneToOneSpecificComponents()
    {
        settingsPanel.add(transferCallButton);
        settingsPanel.add(desktopSharingButton);
        settingsPanel.add(videoButton);

        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        while (callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();

            if (callPeer.getState() == CallPeerState.CONNECTED)
            {
                enableButtons(true);
                return;
            }
        }
        enableButtons(false);
    }

    /**
     * Adds components specific for the conference call.
     */
    private void addConferenceSpecificComponents()
    {
        settingsPanel.add(localLevel);
        settingsPanel.add(remoteLevel);

        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        while (callPeers.hasNext())
        {
            if (callPeers.next().getState() == CallPeerState.CONNECTED)
            {
                enableButtons(true);
                return;
            }
        }
        enableButtons(false);
    }

    /**
     * Removes components specific for the conference call.
     */
    private void removeConferenceSpecificComponents()
    {
        settingsPanel.remove(localLevel);
        settingsPanel.remove(remoteLevel);
    }

    /**
     * Initialize plug-in components already registered for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + net.java.sip.communicator.service.gui.Container.CONTAINER_ID
            + "="+net.java.sip.communicator.service.gui.Container
                                            .CONTAINER_CALL_DIALOG.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);;

                this.add((Component)component.getComponent());
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }


    /**
     * Indicates that a plugin component has been successfully added
     * to the container.
     *
     * @param event the PluginComponentEvent containing the corresponding
     * plugin component
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(
            net.java.sip.communicator.service.gui.Container
                .CONTAINER_CALL_DIALOG)
            && c.getComponent() instanceof Component)
        {
            settingsPanel.add((Component) c.getComponent());

            settingsPanel.revalidate();
            settingsPanel.repaint();
        }
    }

    /**
     * Indicates that a plugin component has been successfully removed
     * from the container.
     *
     * @param event the PluginComponentEvent containing the corresponding
     * plugin component
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(
            net.java.sip.communicator.service.gui.Container
                .CONTAINER_CALL_DIALOG)
            && c.getComponent() instanceof Component)
        {
            settingsPanel.remove((Component) c.getComponent());

            settingsPanel.revalidate();
            settingsPanel.repaint();
        }
    }

    /**
     * Checks whether recording is currently enabled or not, state retrieved
     * from call record button state.
     *
     * @return <tt>true</tt> if the recording is already started, <tt>false</tt>
     * otherwise
     */
    public boolean isRecordingStarted()
    {
        return recordButton.isSelected();
    }

    /**
     * Creates the bottom bar panel for this call dialog, depending on the
     * current operating system.
     *
     * @return the created bottom bar
     */
    private JComponent createBottomBar()
    {
        JComponent bottomBar = new TransparentPanel();

        bottomBar.setBorder(
            new ExtendedEtchedBorder(EtchedBorder.LOWERED, 1, 0, 0, 0));

        if (OSUtils.IS_MAC)
        {
            bottomBar.setOpaque(true);
            bottomBar.setBackground(
                new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));
        }

        bottomBar.setLayout(new BorderLayout());
        bottomBar.add(settingsPanel, BorderLayout.WEST);
        bottomBar.add(hangupButton, BorderLayout.EAST);

        return bottomBar;
    }

    /**
     * Adds the given <tt>CallTitleListener</tt> to the list of listeners,
     * notified for call title changes.
     *
     * @param l the <tt>CallTitleListener</tt> to add
     */
    public void addCallTitleListener(CallTitleListener l)
    {
        synchronized (titleListeners)
        {
            titleListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>CallTitleListener</tt> to the list of listeners,
     * notified for call title changes.
     *
     * @param l the <tt>CallTitleListener</tt> to remove
     */
    public void removeCallTitleListener(CallTitleListener l)
    {
        synchronized (titleListeners)
        {
            titleListeners.remove(l);
        }
    }

    /**
     * Notifies interested listeners of a call title change.
     */
    private void fireTitleChangeEvent()
    {
        Iterator<CallTitleListener> listeners;

        synchronized (titleListeners)
        {
            listeners = new Vector<CallTitleListener>(titleListeners).iterator();
        }

        while (listeners.hasNext())
        {
            listeners.next().callTitleChanged(this);
        }
    }
}