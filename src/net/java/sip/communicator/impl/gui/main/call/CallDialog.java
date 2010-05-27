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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.border.*;

/**
 * The dialog created for a given call.
 *
 * @author Yana Stamcheva
 */
public class CallDialog
    extends SIPCommFrame
    implements ActionListener,
               CallChangeListener,
               CallPeerConferenceListener
{
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
     * The content pane of this dialog.
     */
    private final Container contentPane = getContentPane();

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
     * The mute button.
     */
    private MuteButton muteButton;

    /**
     * The video button.
     */
    private LocalVideoButton videoButton;

    /**
     * The transfer call button.
     */
    private TransferCallButton transferCallButton;

    /**
     * The full screen button.
     */
    private FullScreenButton fullScreenButton;

    /**
     * The call represented in this dialog.
     */
    private final Call call;

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
     * Creates a <tt>CallDialog</tt> by specifying the underlying call panel.
     * @param call the <tt>call</tt> that this dialog represents
     */
    public CallDialog(Call call)
    {
        super(false);

        this.call = call;

        this.callDurationTimer = new Timer(1000, new CallTimerListener());
        this.callDurationTimer.setRepeats(true);

        // The call duration parameter is not known yet.
        this.setCallTitle(null);

        // Initializes the correct renderer panel depending on whether we're in
        // a single call or a conference call.
        this.isLastConference = isConference();

        if (isLastConference)
        {
            this.callPanel
                = new ConferenceCallPanel(this, call);
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
    }

    /**
     * Initializes all buttons and common panels
     */
    private void init()
    {
        TransparentPanel buttonsPanel
            = new TransparentPanel(new BorderLayout(5, 5));

        SIPCommButton hangupButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));

        SIPCommButton dialButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
            ImageLoader.getImage(ImageLoader.DIAL_BUTTON));

        SIPCommButton conferenceButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
            ImageLoader.getImage(ImageLoader.ADD_TO_CALL_BUTTON));

        holdButton = new HoldButton(call);
        muteButton = new MuteButton(call);
        videoButton = new LocalVideoButton(call);
        transferCallButton = new TransferCallButton(call);
        fullScreenButton = new FullScreenButton(this);

        dialButton.setName(DIAL_BUTTON);
        dialButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.DIALPAD"));
        dialButton.addActionListener(this);

        conferenceButton.setName(CONFERENCE_BUTTON);
        conferenceButton.setToolTipText(
            GuiActivator.getResources().getI18NString(
                "service.gui.CREATE_CONFERENCE_CALL"));
        conferenceButton.addActionListener(this);

        contentPane.add(callPanel, BorderLayout.CENTER);
        contentPane.add(buttonsPanel, BorderLayout.SOUTH);

        hangupButton.setName(HANGUP_BUTTON);
        hangupButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.HANG_UP"));
        hangupButton.addActionListener(this);

        settingsPanel.add(dialButton);
        settingsPanel.add(conferenceButton);
        settingsPanel.add(holdButton);
        settingsPanel.add(muteButton);

        if (!isLastConference)
        {
            settingsPanel.add(videoButton);
            settingsPanel.add(transferCallButton);
            settingsPanel.add(fullScreenButton);
        }

        buttonsPanel.add(settingsPanel, BorderLayout.WEST);
        buttonsPanel.add(hangupButton, BorderLayout.EAST);

        buttonsPanel.setBorder(
            new ExtendedEtchedBorder(EtchedBorder.LOWERED, 1, 0, 0, 0));

        dtmfHandler = new DTMFHandler(this);
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
            actionPerformedOnHangupButton();
        }
        else if (buttonName.equals(DIAL_BUTTON))
        {
            if (dialpadDialog == null)
            {
                dialpadDialog = this.getDialpadDialog();
            }

            if(!dialpadDialog.isVisible())
            {
                dialpadDialog.setSize(
                    this.getWidth() - 20,
                    dialpadDialog.getHeight());

                dialpadDialog.setLocation(
                    this.getX() + 10,
                    getLocationOnScreen().y + getHeight());

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
    private void actionPerformedOnHangupButton()
    {
        Call call = getCall();

        NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);
        NotificationManager.stopSound(NotificationManager.BUSY_CALL);

        if (call != null)
            CallManager.hangupCall(call);

        this.dispose();
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
     * Hang ups the current call on close.
     * @param isEscaped indicates if the window was close by pressing the escape
     * button
     */
    protected void close(boolean isEscaped)
    {
        if (!isEscaped)
        {
            actionPerformedOnHangupButton();
        }
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
     * Updates the state of the general mute button. The mute buttons is
     * selected only if all call peers are muted at the same time. In all other
     * cases the mute button is unselected.
     */
    public void updateMuteButtonState()
    {
        // Check if all the call peers are muted and change the state of
        // the button.
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        boolean isAllMute = true;
        while(callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();
            if (!callPeer.isMute())
            {
                isAllMute = false;
                break;
            }
        }

        // If we have clicked the mute button in a full screen mode or selected
        // mute of the peer menu in a conference call we need to update the
        // state of the call dialog hold button.
        muteButton.setSelected(isAllMute);
    }

    /**
     * Returns <code>true</code> if the video button is selected,
     * <code>false</code> - otherwise.
     *
     * @return  <code>true</code> if the video button is selected,
     * <code>false</code> - otherwise.
     */
    public boolean isVideoButtonSelected()
    {
        return videoButton.isSelected();
    }

    /**
     * Selects or unselects the video button in this call dialog.
     *
     * @param isSelected indicates if the video button should be selected or not
     */
    public void setVideoButtonSelected(boolean isSelected)
    {
        this.videoButton.setSelected(true);
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
                        contentPane.remove(callPanel);
                        updateCurrentCallPanel(
                            new ConferenceCallPanel(CallDialog.this, call));
                        contentPane.add(callPanel, BorderLayout.CENTER);
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

                refreshWindow();
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

    public void callStateChanged(CallChangeEvent evt) {}

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
                        settingsPanel.remove(videoButton);
                        contentPane.remove(callPanel);
                        updateCurrentCallPanel(
                            new ConferenceCallPanel(CallDialog.this, call));
                        contentPane.add(callPanel, BorderLayout.CENTER);
                    }
                }

                refreshWindow();
            }
        });
    }

    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent) {}

    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent) {}

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
        return false;
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
        String titleString
            = GuiActivator.getResources().getI18NString("service.gui.CALL")
                + " | ";

        if (callDuration != null)
            this.setTitle(titleString + GuiUtils.formatTime(callDuration));
        else
            this.setTitle(titleString + "00:00:00");
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
                            contentPane.remove(callPanel);
                            CallPeer singlePeer = call.getCallPeers().next();

                            // if the other party is focus we want to see
                            // his members
                            if (singlePeer != null
                                && !singlePeer.isConferenceFocus())
                                updateCurrentCallPanel(new OneToOneCallPanel(
                                    CallDialog.this, call, singlePeer));
                            else if(singlePeer.isConferenceFocus())
                            {
                                ((ConferenceCallPanel) callPanel)
                                    .removeCallPeerPanel(peer);
                            }

                            contentPane.add(callPanel, BorderLayout.CENTER);

                            isLastConference = false;
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
                            dispose();
                        }

                        refreshWindow();
                    }
                    else
                    {
                        // Dispose the window
                        dispose();
                    }
                }
            });
        }
    }

    /**
     * Refreshes the content of this dialog.
     */
    public void refreshWindow()
    {
        if (!contentPane.isVisible())
            return;

        contentPane.validate();

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
            pack();
        else
            contentPane.repaint();
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
     * Replaces the current call panel with the given one.
     * @param callPanel the <tt>JComponent</tt> to replace the current
     * call panel
     */
    private void updateCurrentCallPanel(JComponent callPanel)
    {
        this.callPanel = callPanel;

        if (callPanel instanceof OneToOneCallPanel)
        {
            settingsPanel.add(videoButton);
            settingsPanel.add(transferCallButton);
            settingsPanel.add(fullScreenButton);
        }
        else
        {
            settingsPanel.remove(videoButton);
            settingsPanel.remove(transferCallButton);
            settingsPanel.remove(fullScreenButton);
        }
    }
}
