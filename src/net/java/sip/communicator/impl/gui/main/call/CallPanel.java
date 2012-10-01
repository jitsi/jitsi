/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.Container;
import java.awt.event.*;
import java.beans.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * The dialog created for a given call.
 *
 * Ordered buttons we are adding/removing, numbers are the index we have set.
 * And the order that will be kept.
 * 0 dialButton
 * 1 conferenceButton
 * 2 holdButton
 * 3 recordButton
 * 4 mergeButton
 * 5 transferCallButton
 * 6 localLevel
 * 7 remoteLevel
 * 8 desktopSharingButton
 * 9 resizeVideoButton
 * 10 fullScreenButton
 * 11 videoButton
 * 12 showHideVideoButton
 * 19 chatButton
 * 20 infoButton
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
     * Property to disable the info button.
     */
    private static final String SHOW_CALL_INFO_BUTON_PROP =
        "net.java.sip.communicator.impl.gui.main.call.SHOW_CALL_INFO_BUTTON";

    /**
     * The dial button name.
     */
    private static final String DIAL_BUTTON = "DIAL_BUTTON";

    /**
     * The conference button name.
     */
    private static final String CONFERENCE_BUTTON = "CONFERENCE_BUTTON";

    /**
     * The chat button name.
     */
    private static final String CHAT_BUTTON = "CHAT_BUTTON";

    /**
     * The info button name.
     */
    private static final String INFO_BUTTON = "INFO_BUTTON";

    /**
     * The hang up button name.
     */
    private static final String MERGE_BUTTON = "MERGE_BUTTON";

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
    private JComponent settingsPanel;

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
     * The button which allows starting and stopping the recording of
     * {@link #callConference}.
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
    private CallToolBarButton dialButton = new CallToolBarButton(
        ImageLoader.getImage(ImageLoader.DIAL_BUTTON),
        DIAL_BUTTON,
        GuiActivator.getResources().getI18NString("service.gui.DIALPAD"));

    /**
     * The conference button.
     */
    private CallToolBarButton conferenceButton
        = new CallToolBarButton(
                ImageLoader.getImage(ImageLoader.ADD_TO_CALL_BUTTON),
                CONFERENCE_BUTTON,
                GuiActivator.getResources().getI18NString(
                    "service.gui.CREATE_CONFERENCE_CALL"));

    /**
     * Chat button.
     */
    private SIPCommButton chatButton;

    /**
     * Info button.
     */
    private SIPCommButton infoButton;

    /**
     * The Frame used to display this call information statistics.
     */
    private CallInfoFrame callInfoFrame;

    /**
     * HangUp button.
     */
    private SIPCommButton hangupButton;

    /**
     * Merge button.
     */
    private CallToolBarButton mergeButton =
        new CallToolBarButton(
            ImageLoader.getImage(ImageLoader.MERGE_CALL_BUTTON),
            MERGE_BUTTON,
            GuiActivator.getResources().getI18NString(
                "service.gui.MERGE_TO_CALL"));

    /**
     * The {@link CallConference} instance depicted by this <tt>CallPanel</tt>.
     */
    private final CallConference callConference;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to changes in the
     * values of {@link #callConference}'s properties.
     */
    private final PropertyChangeListener callConferencePropertyChangeListener
        = new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent ev)
            {
                callConferencePropertyChange(ev);
            }
        };

    /**
     * Indicates if the last call was a conference call.
     */
    private boolean isLastConference = false;

    /**
     * The time in milliseconds at which the telephony call/conference depicted
     * by this <tt>CallPanel</tt> (i.e. {@link #callConference}) has started.
     */
    private long callConferenceStartTime;

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
    private final CallContainer callWindow;

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
    private Component remoteLevel;

    /**
     * A collection of listeners, registered for call title change events.
     */
    private Collection<CallTitleListener> titleListeners
        = new Vector<CallTitleListener>();

    /**
     * Initializes a new <tt>CallPanel</tt> which is to depict a specific
     * <tt>CallConference</tt>.
     *
     * @param callConference the <tt>CallConference</tt> to be depicted by the
     * new instance
     * @param callWindow the parent window in which the new instance will be
     * added
     */
    public CallPanel(CallConference callConference, CallContainer callWindow)
    {
        super(new BorderLayout());

        this.callConference = callConference;
        this.callWindow = callWindow;

        settingsPanel = CallPeerRendererUtils.createButtonBar(false, null);

        /*
         * TODO CallPanel depicts a whole CallConference which may have multiple
         * Calls, new Calls may be added to the CallConference and existing
         * Calls may be removed from the CallConference. For example, the
         * buttons which accept a Call as an argument should be changed to take
         * into account the whole CallConference.
         */
        Call call = this.callConference.getCalls().get(0);

        holdButton = new HoldButton(call);
        recordButton = new RecordButton(call);
        videoButton = new LocalVideoButton(call);
        showHideVideoButton = new ShowHideVideoButton(call);
        desktopSharingButton = new DesktopSharingButton(call);
        transferCallButton = new TransferCallButton(call);
        fullScreenButton = new FullScreenButton(this, false);
        chatButton = new CallToolBarButton(
            ImageLoader.getImage(ImageLoader.CHAT_BUTTON_SMALL_WHITE),
            CHAT_BUTTON,
            GuiActivator.getResources().getI18NString("service.gui.CHAT"));

        localLevel
            = new InputVolumeControlButton(
                    call, ImageLoader.MICROPHONE, ImageLoader.MUTE_BUTTON,
                    false, true, false);
        remoteLevel
            = new OutputVolumeControlButton(
                    ImageLoader.VOLUME_CONTROL_BUTTON, false, true)
                .getComponent();

        this.callDurationTimer = new Timer(1000, new CallTimerListener());
        this.callDurationTimer.setRepeats(true);

        // The call duration parameter is not known yet.
        this.setCallTitle(0);

        // Initializes the correct renderer panel depending on whether we're in
        // a single call or a conference call.
        this.isLastConference = isConference();

        if (isLastConference)
        {
            enableConferenceInterface(
                    CallManager.isVideoStreaming(callConference));
        }
        else
        {
            List<CallPeer> callPeers = callConference.getCallPeers();
            CallPeer callPeer = null;

            if (!callPeers.isEmpty())
            {
                callPeer = callPeers.get(0);
                this.callPanel = new OneToOneCallPanel(this, callPeer);
            }
        }

        // Adds a CallChangeListener that would receive events when a peer is
        // added or removed, or the state of the call has changed.
        callConference.addCallChangeListener(this);
        callConference.addPropertyChangeListener(
                callConferencePropertyChangeListener);
        // Adds the CallPeerConferenceListener that would listen for changes in
        // the focus state of each call peer.
        for (CallPeer callPeer : callConference.getCallPeers())
            callPeer.addCallPeerConferenceListener(this);

        // Initializes all buttons and common panels.
        initToolBar();

        initPluginComponents();
    }

    /**
     * Initializes all buttons and common panels
     */
    private void initToolBar()
    {
        hangupButton = new HangupButton(this);

        // Initializes the order of buttons in the call tool bar.
        initButtonIndexes();

        showHideVideoButton.setPeerRenderer(
                ((CallRenderer) callPanel).getCallPeerRenderer(
                        callConference.getCallPeers().get(0)));

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

        chatButton.addActionListener(this);
        dialButton.addActionListener(this);
        conferenceButton.addActionListener(this);
        mergeButton.addActionListener(this);

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
        settingsPanel.add(mergeButton);
        mergeButton.setVisible(false);

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

            // Enables the button only if there is 1 and only 1 peer which is
            // basicIM capable.
            if(getIMCapableCallPeers().size() == 1)
            {
                settingsPanel.add(chatButton);
            }
        }
        else
        {
            // These buttons are only added in the conference call. For the one
            // to one call mute and sound buttons are in the call peer panel.
            localLevel.setEnabled(false);
            remoteLevel.setEnabled(false);

            addConferenceSpecificComponents();
        }

        if(GuiActivator.getConfigurationService()
                .getBoolean(SHOW_CALL_INFO_BUTON_PROP, true))
        {
            infoButton = new CallToolBarButton(
                    ImageLoader.getImage(ImageLoader.CALL_INFO),
                    INFO_BUTTON,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.PRESS_FOR_CALL_INFO"));

            infoButton.addActionListener(this);
            settingsPanel.add(infoButton);
        }

        settingsPanel.add(hangupButton);

        dtmfHandler = new DTMFHandler(this);

        add(callPanel, BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);
    }

    /**
     * Initializes buttons order in the call tool bar.
     */
    private void initButtonIndexes()
    {
        dialButton.setIndex(0);
        conferenceButton.setIndex(1);
        holdButton.setIndex(2);
        recordButton.setIndex(3);
        mergeButton.setIndex(4);
        transferCallButton.setIndex(5);
        localLevel.setIndex(6);

        if (remoteLevel instanceof OrderedComponent)
            ((OrderedComponent) remoteLevel).setIndex(7);

        desktopSharingButton.setIndex(8);
        fullScreenButton.setIndex(10);
        videoButton.setIndex(11);
        showHideVideoButton.setIndex(12);
        chatButton.setIndex(19);

        if (infoButton != null)
            infoButton.setIndex(20);

        hangupButton.setIndex(100);
    }

    /**
     * Handles action events.
     * @param evt the <tt>ActionEvent</tt> that was triggered
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (buttonName.equals(MERGE_BUTTON))
        {
            CallManager.mergeExistingCalls(
                    callConference,
                    CallManager.getInProgressCalls());
        }
        else if (buttonName.equals(DIAL_BUTTON))
        {
            if (dialpadDialog == null)
                dialpadDialog = this.getDialpadDialog();

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
            new ConferenceInviteDialog(callConference).setVisible(true);
        }
        else if (buttonName.equals(CHAT_BUTTON))
        {
            Collection<Contact> collectionIMCapableContacts
                = getIMCapableCallPeers();

            // If a single peer is basic instant messaging capable, then we
            // create a chat with this account.
            if(collectionIMCapableContacts.size() == 1)
            {
                Contact contact = collectionIMCapableContacts.iterator().next();
                MetaContact metaContact =
                    GuiActivator.getContactListService()
                    .findMetaContactByContact(contact);
                GuiActivator.getUIService().getChatWindowManager()
                    .startChat(metaContact);
            }
        }
        else if (buttonName.equals(INFO_BUTTON))
        {
            if (callInfoFrame == null)
            {
                this.callInfoFrame = new CallInfoFrame(callConference);
                this.addCallTitleListener(callInfoFrame);
            }

            callInfoFrame.setVisible(!callInfoFrame.isVisible());
        }
    }

    /**
     * Executes the action associated with the "Hang up" button which may be
     * invoked by clicking the button in question or by closing this dialog.
     *
     * @param closeWait <tt>true</tt> to close this instance with a few seconds
     * of delay or <tt>false</tt> to close it immediately
     */
    public void actionPerformedOnHangupButton(boolean closeWait)
    {
        this.disposeCallInfoFrame();

        CallManager.hangupCalls(callConference);

        /*
         * XXX It is the responsibility of CallManager to close this CallPanel
         * when a Call is ended.
         */
    }

    /**
     * Returns the <tt>CallConference</tt> depicted by this <tt>CallPanel</tt>
     *
     * @return the <tt>CallConference</tt> depicted by this
     * <tt>CallConference</tt>
     */
    public CallConference getCallConference()
    {
        return callConference;
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
        boolean areAllPeersLocallyOnHold = true;

        for (CallPeer peer : callConference.getCallPeers())
        {
            CallPeerState state = peer.getState();

            // If we have clicked the hold button in a full screen mode
            // we need to update the state of the call dialog hold button.
            if (!state.equals(CallPeerState.ON_HOLD_LOCALLY)
                && !state.equals(CallPeerState.ON_HOLD_MUTUALLY))
            {
                areAllPeersLocallyOnHold = false;
                break;
            }
        }

        // If we have clicked the hold button in a full screen mode or selected
        // hold of the peer menu in a conference call we need to update the
        // state of the call dialog hold button.
        this.holdButton.setSelected(areAllPeersLocallyOnHold);
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
            OneToOneCallPanel oneToOneCallPanel = (OneToOneCallPanel) callPanel;

            if (isSelected
                && oneToOneCallPanel.getCall().getProtocolProvider()
                    .getOperationSet(
                        OperationSetDesktopSharingServer.class) != null)
            {
                oneToOneCallPanel.addDesktopSharingComponents();
            }
            else
                oneToOneCallPanel.removeDesktopSharingComponents();
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
        videoButton.setEnabled(!hold);

        boolean videoTelephonyAllowsLocalVideo = false;
        boolean desktopSharingServerAllowsLocalVideo = false;

        for (Call call : callConference.getCalls())
        {
            ProtocolProviderService protocolProvider
                = call.getProtocolProvider();

            if (!videoTelephonyAllowsLocalVideo)
            {
                OperationSetVideoTelephony videoTelephony
                    = protocolProvider.getOperationSet(
                            OperationSetVideoTelephony.class);

                if ((videoTelephony != null)
                        && videoTelephony.isLocalVideoAllowed(call))
                {
                    videoTelephonyAllowsLocalVideo = true;
                }
            }
            if (!desktopSharingServerAllowsLocalVideo)
            {
                OperationSetDesktopSharingServer desktopSharingServer
                    = protocolProvider.getOperationSet(
                            OperationSetDesktopSharingServer.class);

                if ((desktopSharingServer != null)
                        && desktopSharingServer.isLocalVideoAllowed(call))
                {
                    desktopSharingServerAllowsLocalVideo = true;
                }
            }

            if (videoTelephonyAllowsLocalVideo
                    && desktopSharingServerAllowsLocalVideo)
            {
                break;
            }
        }

        // If the video was already enabled (for example in the case of
        // direct video call) make sure the video button is selected.
        if (videoTelephonyAllowsLocalVideo && !videoButton.isSelected())
            setVideoButtonSelected(!hold);

        desktopSharingButton.setEnabled(!hold);
        // If the video was already enabled (for example in the case of
        // direct desktop sharing call) make sure the video button is
        // selected.
        if (desktopSharingServerAllowsLocalVideo
                && !desktopSharingButton.isSelected())
        {
            setDesktopSharingButtonSelected(!hold);
        }
    }

    /**
     * Enables or disable all setting buttons.
     *
     * @param enable true to enable all setting buttons, false to disable them
     */
    public void enableButtons(boolean enable)
    {
        // Buttons would be enabled once the call has entered in state
        // connected.
        dialButton.setEnabled(enable);
        holdButton.setEnabled(enable);
        recordButton.setEnabled(enable);
        localLevel.setEnabled(enable);
        remoteLevel.setEnabled(enable);
        mergeButton.setEnabled(enable);

        // Buttons would be enabled once the call has entered in the connected
        // state.
        List<Call> calls = callConference.getCalls();
        boolean enableConferenceButton = false;
        boolean enableTransferCallButton = !calls.isEmpty();
        boolean enableVideoButtons = false;

        for (Call call : calls)
        {
            ProtocolProviderService protocolProvider
                = call.getProtocolProvider();

            if (!enableConferenceButton
                    && (protocolProvider.getOperationSet(
                            OperationSetTelephonyConferencing.class)
                        != null))
            {
                enableConferenceButton = true;
            }

            if (enableTransferCallButton
                    && (protocolProvider.getOperationSet(
                            OperationSetAdvancedTelephony.class)
                        == null))
            {
                enableTransferCallButton = false;
            }

            if (!enableVideoButtons
                    && (protocolProvider.getOperationSet(
                            OperationSetVideoTelephony.class)
                        != null))
            {
                enableVideoButtons = true;
            }
        }

        if (enableConferenceButton && (conferenceButton != null))
            conferenceButton.setEnabled(enable);

        if (videoButton != null)
            videoButton.setEnabled(enable);

        if (!isLastConference)
        {
            List<CallPeer> callPeers = callConference.getCallPeers();

            if (callPeers.size() > 0)
            {
                CallPeer callPeer = callPeers.get(0);
                CallPeerState callPeerState = callPeer.getState();

                enableButtonsWhileOnHold(
                        callPeerState == CallPeerState.ON_HOLD_LOCALLY
                            || callPeerState == CallPeerState.ON_HOLD_MUTUALLY
                            || callPeerState == CallPeerState.ON_HOLD_REMOTELY);
            }

            if (enableTransferCallButton && (transferCallButton != null))
                transferCallButton.setEnabled(enable);

            if (enableVideoButtons)
            {
                if (fullScreenButton != null)
                    fullScreenButton.setEnabled(enable);
                if (desktopSharingButton != null)
                    desktopSharingButton.setEnabled(enable);
            }
        }
    }

    /**
     * Implements {@link CallChangeListener#callPeerAdded(CallPeerEvent)}. Adds
     * the appropriate user interface when a new <tt>CallPeer</tt> is added to
     * a <tt>Call</tt> participating in {@link #callConference}.
     *
     * @param evt the <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> that got added and the <tt>Call</tt> to which it was
     * added
     */
    public void callPeerAdded(final CallPeerEvent evt)
    {
        if (!callConference.containsCall(evt.getSourceCall()))
            return;

        final CallPeer callPeer = evt.getSourceCallPeer();

        callPeer.addCallPeerConferenceListener(this);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (isLastConference)
                {
                    if (CallManager.isVideoStreaming(callConference))
                    {
                        if (!(callPanel instanceof VideoConferenceCallPanel))
                            enableConferenceInterface(true);
                        else
                        {
                            ((VideoConferenceCallPanel) callPanel)
                                .addCallPeerPanel(callPeer);
                        }
                    }
                    else
                    {
                        if(callPanel instanceof VideoConferenceCallPanel)
                            enableConferenceInterface(false);
                        else
                        {
                            ((ConferenceCallPanel) callPanel)
                                .addCallPeerPanel(callPeer);
                        }
                    }
                }
                else
                {
                    isLastConference = isConference();

                    // We've been in one-to-one call and we're now in a
                    // conference.
                    if (isLastConference)
                    {
                        enableConferenceInterface(
                            CallManager.isVideoStreaming(callConference));
                    }
                    // We're still in a one-to-one call and we receive the
                    // remote peer.
                    else
                    {
                        List<CallPeer> callPeers
                            = callConference.getCallPeers();

                        if (!callPeers.isEmpty())
                        {
                            CallPeer onlyCallPeer = callPeers.get(0);

                            ((OneToOneCallPanel) callPanel)
                                .addCallPeerPanel(onlyCallPeer);
                        }
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
        /*
         * Technically, we do not have to remove this as a
         * CallPeerConferenceListener from a CallPeer which was removed from a
         * Call not participating in callConference. Anyway, it shouldn't hurt
         * (much).
         */
        CallPeer peer = evt.getSourceCallPeer();

        peer.removeCallPeerConferenceListener(this);

        Call call = evt.getSourceCall();

        if (callConference.containsCall(call))
        {
            /*
             * We could argue that the logic applied to
             * removeCallPeerConferenceListener above allies to
             * RemovePeerPanelListener. But we're creating a Timer here so it
             * hurts (more).
             */
            Timer timer = new Timer(5000, new RemovePeerPanelListener(peer));

            timer.setRepeats(false);
            timer.start();

            /*
             * The call/telephony conference is finished when that last CallPeer
             * is removed.
             */
            if (callConference.getCallPeerCount() == 0)
                stopCallTimer();
        }
    }

    /**
     * Implements {@link CallChangeListener#callStateChanged(CallChangeEvent)}.
     */
    public void callStateChanged(CallChangeEvent evt)
    {
        updateMergeButtonState();
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
//                if (!isLastConference)
//                {
//                    isLastConference = isConference();
//
//                    // We've been in one-to-one call and we're now in a
//                    // conference.
//                    if (isLastConference)
//                    {
//                        removeOneToOneSpecificComponents();
//                        enableConferenceInterface(
//                            CallManager.isVideoStreaming(call));
//                    }
//                }

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
        if (callConference.isConferenceFocus())
            return true;

        // If one of our peers is a conference focus, we're in a
        // conference call.
        List<CallPeer> callPeers = callConference.getCallPeers();

        for (CallPeer callPeer : callPeers)
        {
            if (callPeer.isConferenceFocus())
                return true;
        }

        // the call can have two peers at the same time and there is no one
        // is conference focus. This is situation when someone has made an
        // attended transfer and has transfered us. We have one call with two
        // peers the one we are talking to and the one we have been transfered
        // to. And the first one is been hanged up and so the call passes through
        // conference call focus a moment and than go again to one to one call.
        return callPeers.size() > 1;
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
        callConferenceStartTime = System.currentTimeMillis();
        callDurationTimer.start();
        isCallTimerStarted = true;
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
            setCallTitle(callConferenceStartTime);
        }
    }

    /**
     * Sets the title of this dialog in accord with a specific time of start of
     * the telephony call/conference depicted by this <tt>CallPanel</tt>.
     *
     * @param startTime the time in milliseconds at which the telephony
     * call/conference depicted by this <tt>CallPanel</tt> is considered to have
     * started
     */
    private void setCallTitle(long startTime)
    {
        StringBuilder title = new StringBuilder();

        if (startTime != 0)
        {
            title.append(
                    GuiUtils.formatTime(
                            startTime,
                            System.currentTimeMillis()));
            title.append(" | ");
        }
        else
            title.append("00:00:00 | ");

        List<CallPeer> callPeers = callConference.getCallPeers();

        if ((callPeers.size() > 0)
                && (GuiActivator.getUIService().getSingleWindowContainer()
                        != null))
        {
            title.append(callPeers.get(0).getDisplayName());
        }
        else
        {
            title.append(
                    GuiActivator.getResources().getI18NString(
                            "service.gui.CALL"));
        }

        this.title = title.toString();

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
        private final CallPeer peer;

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
                    int callPeerCount = callConference.getCallPeerCount();

                    if (isLastConference)
                    {
                        if (callPeerCount == 1)
                        {
                            isLastConference = false;

                            remove(callPanel);

                            CallPeer singlePeer
                                = callConference.getCallPeers().get(0);

                            // if the other party is focus we want to see
                            // his members
                            if (!singlePeer.isConferenceFocus())
                            {
                                CallRenderer callRenderer
                                    = (CallRenderer) callPanel;
                                UIVideoHandler videoHandler
                                    = callRenderer.getVideoHandler();

                                // If we have already a video handler, try to
                                // initiate the new UI with the current video
                                // handler!
                                JComponent newPanel
                                    = new OneToOneCallPanel(
                                            CallPanel.this,
                                            singlePeer,
                                            videoHandler);

                                updateCurrentCallPanel(newPanel);
                            }
                            else if(singlePeer.isConferenceFocus())
                            {
                                ((ConferenceCallPanel) callPanel)
                                    .removeCallPeerPanel(peer);
                            }

                            add(callPanel, BorderLayout.CENTER);
                        }
                        else if (callPeerCount > 1)
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
                        if (callPeerCount < 1)
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
        boolean isScrollBarVisible;

        if (callPanel instanceof ConferenceCallPanel)
        {
            Component scrollBar
                = ((ConferenceCallPanel) callPanel).getVerticalScrollBar();

            isScrollBarVisible = ((scrollBar != null) && scrollBar.isVisible());
        }
        else
            isScrollBarVisible = false;

        /*
         * Repacking should be done only when the callWindow is not high enough
         * to not have a vertical scroll bar and there is still room left to
         * expand its height without going out of the screen.
         */
        if (isScrollBarVisible
                && (getHeight()
                        < GraphicsEnvironment
                            .getLocalGraphicsEnvironment()
                                .getMaximumWindowBounds()
                                    .height))
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
     *
     * @param callPeer the <tt>CallPeer</tt>
     */
    public void addRemoteVideoSpecificComponents(CallPeer callPeer)
    {
        if(CallManager.isVideoQualityPresetSupported(callPeer))
        {
            if(resizeVideoButton == null)
            {
                resizeVideoButton = new ResizeVideoButton(callPeer.getCall());
                resizeVideoButton.setIndex(9);
            }

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
     * Enables the video or non-video conference interface.
     *
     * @param isVideo <tt>true</tt> to enable the video conference interface or
     * <tt>false</tt> to enable the non-video conference interface
     */
    public void enableConferenceInterface(final boolean isVideo)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        enableConferenceInterface(isVideo);
                    }
                });
            }
            catch (InterruptedException e)
            {
                logger.error("Failed to enable conference interface", e);
            }
            catch (InvocationTargetException e)
            {
                logger.error("Failed to enable conference interface", e);
            }
            return;
        }

        UIVideoHandler videoHandler = null;

        if (callPanel != null)
        {
            videoHandler = ((CallRenderer) callPanel).getVideoHandler();

            remove(callPanel);
        }

        // We've been in a one-to-one call and we're now in a conference.
        ConferenceCallPanel newCallPanel;
        Call call = callConference.getCalls().get(0);

        if (isVideo)
        {
            newCallPanel
                = new VideoConferenceCallPanel(this, call, videoHandler);
        }
        else
        {
            newCallPanel
                = new ConferenceCallPanel(this, call, videoHandler, false);
        }
        updateCurrentCallPanel(newCallPanel);

        add(callPanel, BorderLayout.CENTER);

        refreshContainer();
    }

    /**
     * Removes components specific for the one-to-one call.
     */
    private void removeOneToOneSpecificComponents()
    {
        // Disable desktop sharing.
        if (desktopSharingButton != null && desktopSharingButton.isSelected())
            desktopSharingButton.doClick();

        // Disable full screen.
        if (fullScreenButton != null && fullScreenButton.isSelected())
            fullScreenButton.doClick();

        if (videoButton != null)
            settingsPanel.remove(videoButton);

        if (showHideVideoButton != null)
            settingsPanel.remove(showHideVideoButton);

        if(resizeVideoButton != null)
            settingsPanel.remove(resizeVideoButton);

        if (desktopSharingButton != null)
        {
            settingsPanel.remove(desktopSharingButton);
            settingsPanel.remove(transferCallButton);
            settingsPanel.remove(fullScreenButton);
        }
    }

    /**
     * Adds components specific for the one-to-one call.
     */
    private void addOneToOneSpecificComponents()
    {
        for (CallPeer callPeer : callConference.getCallPeers())
        {
            settingsPanel.add(transferCallButton);

            Contact peerContact = callPeer.getContact();

            ProtocolProviderService callProvider
                = callPeer.getCall().getProtocolProvider();

            OperationSetContactCapabilities capOpSet
                = callProvider.getOperationSet(
                    OperationSetContactCapabilities.class);

            if (peerContact != null
                && capOpSet != null)
            {
                if (capOpSet.getOperationSet(peerContact,
                    OperationSetDesktopSharingServer.class) != null)
                    settingsPanel.add(desktopSharingButton);

                if (capOpSet.getOperationSet(peerContact,
                    OperationSetVideoTelephony.class) != null)
                    settingsPanel.add(videoButton);
            }
            else
            {
                if (callProvider.getOperationSet(
                    OperationSetDesktopSharingServer.class) != null)
                    settingsPanel.add(desktopSharingButton);
                if (callProvider.getOperationSet(
                    OperationSetVideoTelephony.class) != null)
                    settingsPanel.add(videoButton);
            }

            if (callPeer.getState() == CallPeerState.CONNECTED)
            {
                if(!isCallTimerStarted())
                    startCallTimer();

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
        settingsPanel.add(videoButton);

        for (CallPeer callPeer : callConference.getCallPeers())
        {
            if (callPeer.getState() == CallPeerState.CONNECTED)
            {
                if(!isCallTimerStarted())
                    startCallTimer();

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
        JComponent bottomBar
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        bottomBar.setBorder(BorderFactory.createEmptyBorder(0, 30, 2, 30));

        if (OSUtils.IS_MAC)
        {
            bottomBar.setOpaque(true);
            bottomBar.setBackground(
                new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));
        }

        bottomBar.add(settingsPanel);

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

    /**
     * Returns the list of call peers which are capable to manage basic instant
     * messaging operations.
     *
     * @return The collection of contacts for this call which are IM capable.
     */
    private Collection<Contact> getIMCapableCallPeers()
    {
        List<CallPeer> callPeers = callConference.getCallPeers();
        List<Contact> contacts = new ArrayList<Contact>(callPeers.size());

        /*
         * Choose the CallPeers (or rather their associated Contacts) which are
         * capable of basic instant messaging.
         */
        for (CallPeer callPeer : callPeers)
        {
            if (callPeer.getProtocolProvider().getOperationSet(
                        OperationSetBasicInstantMessaging.class)
                    != null)
            {
                /*
                 * CallPeer#getContact) is more expensive in terms of execution
                 * than ProtocolProviderService#getOperationSet(Class).
                 */
                Contact contact = callPeer.getContact();

                if (contact != null)
                    contacts.add(contact);
            }
        }
        return contacts;
    }

    /**
     * Disposes the call info frame if it exists.
     */
    public void disposeCallInfoFrame()
    {
        if (callInfoFrame != null)
            callInfoFrame.dispose();
    }

    /**
     * Notifies this <tt>CallPanel</tt> about a specific <tt>CallEvent</tt>
     * (received by <tt>CallManager</tt>). The source <tt>Call</tt> may or may
     * not be participating in the telephony conference depicted by this
     * instance but allows it to update any state which may depend on the
     * <tt>Call</tt>s which are established application-wide.
     *
     * @param ev a <tt>CallEvent</tt> which specifies the <tt>Call</tt> which
     * caused this instance to be notified and the exact type of the
     * notification event
     */
    void onCallEvent(CallEvent ev)
    {
        updateMergeButtonState();
    }

    /**
     * Updates the <tt>visible</tt> state/property of {@link #mergeButton}.
     */
    private void updateMergeButtonState()
    {
        List<CallConference> conferences = new ArrayList<CallConference>();
        int cpt = 0;

        for (Call call : CallManager.getInProgressCalls())
        {
            CallConference conference = call.getConference();

            if (conference == null)
                cpt++;
            else if (!conferences.contains(conference))
            {
                conferences.add(conference);
                cpt++;
            }
            else
                continue;

            if (cpt > 1)
                break;
        }

        mergeButton.setVisible(cpt > 1);
    }

    /**
     * Returns the minimum width needed to show buttons.
     * Used to calculate the minimum size of the call dialog.
     * @return the minimum width for the buttons.
     */
    public int getMinimumButtonWidth()
    {
        int numberOfButtons = countButtons(settingsPanel.getComponents());

        if (numberOfButtons > 0)
        {
            // +1 cause we had and a hangup button
            // *32, a button is 28 pixels width and give some border
            return (numberOfButtons + 1) * 32;
        }
        else
            return -1;
    }

    /**
     * Count the number of the buttons in the supplied components.
     * @param cs the components to search for buttons.
     * @return number of buttons.
     */
    private int countButtons(Component[] cs)
    {
        int count = 0;

        for(Component c : cs)
        {
            if(c instanceof SIPCommButton || c instanceof SIPCommToggleButton)
                count++;
            if(c instanceof Container)
                count += countButtons(((Container)c).getComponents());
        }

        return count;
    }

    /**
     * Notifies this instance that the value of a property of
     * {@link #callConference} has changed.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which specifies the name of the
     * property which had its value changed and the old and new values of that
     * property
     */
    private void callConferencePropertyChange(PropertyChangeEvent ev)
    {
        if (CallConference.CALLS.equals(ev.getPropertyName()))
        {
            /*
             * When a Call is removed from the callConference, remove any
             * listeners from it and its associated CallPeers.
             */
            Object oldValue = ev.getOldValue();

            if (oldValue instanceof Call)
            {
                Iterator<? extends CallPeer> oldPeerIter
                    = ((Call) oldValue).getCallPeers();

                while (oldPeerIter.hasNext())
                    oldPeerIter.next().removeCallPeerConferenceListener(this);
            }

            /*
             * When a Call is added to the callConference, add any (necessary)
             * listeners to it and its associated CallPeers.
             */
            Object newValue = ev.getNewValue();

            if (newValue instanceof Call)
            {
                Iterator<? extends CallPeer> newPeerIter
                    = ((Call) newValue).getCallPeers();

                while (newPeerIter.hasNext())
                    newPeerIter.next().addCallPeerConferenceListener(this);
            }
        }
    }

    /**
     * Releases the resources acquired by this instance which require explicit
     * disposal (e.g. any listeners added to the depicted
     * <tt>CallConference</tt>, the participating <tt>Call</tt>s, and their
     * associated <tt>CallPeer</tt>s). Invoked by <tt>CallManager</tt> when it
     * determines that this <tt>CallPanel</tt> is no longer necessary. 
     */
    void dispose()
    {
        callConference.removeCallChangeListener(this);
        callConference.removePropertyChangeListener(
                callConferencePropertyChangeListener);
        for (CallPeer callPeer : callConference.getCallPeers())
            callPeer.removeCallPeerConferenceListener(this);
    }
}
