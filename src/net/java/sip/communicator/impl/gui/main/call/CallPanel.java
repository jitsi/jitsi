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
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.call.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;
import org.jitsi.util.event.*;
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
 * @author Lyubomir Marinov
 * @author Hristo Terezov
 */
public class CallPanel
    extends TransparentPanel
    implements ActionListener,
               PluginComponentListener,
               Skinnable,
               ConferencePeerViewListener
{
    /**
     * The chat button name.
     */
    private static final String CHAT_BUTTON = "CHAT_BUTTON";

    /**
     * The conference button name.
     */
    private static final String CONFERENCE_BUTTON = "CONFERENCE_BUTTON";

    /**
     * The dial button name.
     */
    private static final String DIAL_BUTTON = "DIAL_BUTTON";

    /**
     * The info button name.
     */
    private static final String INFO_BUTTON = "INFO_BUTTON";

    /**
     * The logger for this class.
     */
    private static final Logger logger = Logger.getLogger(CallDialog.class);

    /**
     * The hang up button name.
     */
    private static final String MERGE_BUTTON = "MERGE_BUTTON";

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Property to disable the info button.
     */
    private static final String HIDE_CALL_INFO_BUTON_PROP
        = "net.java.sip.communicator.impl.gui.main.call.HIDE_CALL_INFO_BUTTON";

    /**
     * Property to disable the record button.
     */
    private static final String HIDE_CALL_RECORD_BUTON_PROP
        = "net.java.sip.communicator.impl.gui.main.call.HIDE_CALL_RECORD_BUTTON";

    /**
     * The <tt>Component</tt> which is at the bottom of this view and contains
     * {@link #settingsPanel}. It overrides the Swing-defined background on OS
     * X so it needs explicit updating upon switching between full-screen and
     * windowed mode in order to respect any background-related settings of the
     * ancestors such as black background in full-screen mode.
     */
    private JComponent bottomBar;

    /**
     * The {@link CallConference} instance depicted by this <tt>CallPanel</tt>.
     */
    private final CallConference callConference;

    /**
     * The listener which listens to events fired by the <tt>CallConference</tt>
     * depicted by this instance, the <tt>Call</tt>s participating in that
     * telephony conference, the <tt>CallPeer</tt>s associated with those
     * <tt>Call</tt>s and the <tt>ConferenceMember</tt>s participating in the
     * telephony conferences organized by those <tt>CallPeer</tt>s. Updates this
     * view i.e. CallPanel so that it depicts the current state of its model
     * i.e. {@link #callConference}.
     */
    private final CallConferenceListener callConferenceListener
        = new CallConferenceListener();

    /**
     * The time in milliseconds at which the telephony call/conference depicted
     * by this <tt>CallPanel</tt> (i.e. {@link #callConference}) has started.
     */
    private long callConferenceStartTime;

    /**
     * A timer to count call duration.
     */
    private Timer callDurationTimer;

    /**
     * The Frame used to display this call information statistics.
     */
    private CallInfoFrame callInfoFrame;

    /**
     * The panel representing the call. For conference calls this would be an
     * instance of <tt>ConferenceCallPanel</tt> and for one-to-one calls this
     * would be an instance of <tt>OneToOneCallPanel</tt>.
     */
    private JComponent callPanel;

    /**
     * Parent window.
     */
    private final CallContainer callWindow;

    /**
     * Chat button.
     */
    private SIPCommButton chatButton;

    /**
     * The conference button.
     */
    private CallToolBarButton conferenceButton;

    /**
     * The desktop sharing button.
     */
    private DesktopSharingButton desktopSharingButton;

    /**
     * The dial button, which opens a keypad dialog.
     */
    private CallToolBarButton dialButton;

    /**
     * The dial pad dialog opened when the dial button is clicked.
     */
    private DialpadDialog dialpadDialog;

    /**
     * The indicator which determines whether {@link #dispose()} has already
     * been invoked on this instance. If <tt>true</tt>, this instance is
     * considered non-functional and is to be left to the garbage collector.
     */
    private boolean disposed = false;

    /**
     * The handler for DTMF tones.
     */
    private DTMFHandler dtmfHandler;

    /**
     * The full screen button.
     */
    private FullScreenButton fullScreenButton;

    /**
     * HangUp button.
     */
    private SIPCommButton hangupButton;

    /**
     * The hold button.
     */
    private HoldButton holdButton;

    /**
     * Info button.
     */
    private SIPCommButton infoButton;

    /**
     * Indicates if the call timer has been started.
     */
    private boolean isCallTimerStarted = false;

    /**
     * Sound local level label.
     */
    private InputVolumeControlButton localLevel;

    /**
     * Merge button.
     */
    private CallToolBarButton mergeButton;

    /**
     * The button which allows starting and stopping the recording of
     * {@link #callConference}.
     */
    private RecordButton recordButton;

    /**
     * Sound remote level label.
     */
    private Component remoteLevel;

    /**
     * The video resize button.
     */
    private ResizeVideoButton resizeVideoButton;

    /**
     * The panel containing call settings.
     */
    private CallToolBar settingsPanel;

    /**
     * The button responsible for hiding/showing the local video.
     */
    private ShowHideVideoButton showHideVideoButton;

    /**
     * The title of this call container.
     */
    private String title;

    /**
     * A collection of listeners, registered for call title change events.
     */
    private Collection<CallTitleListener> titleListeners
        = new Vector<CallTitleListener>();

    /**
     * The transfer call button.
     */
    private TransferCallButton transferCallButton;

    /**
     * The facility which aids this instance in the dealing with the
     * video-related information.
     */
    private final UIVideoHandler2 uiVideoHandler;

    /**
     * Indicates if this call panel should be closed immediately after hang up
     * or should wait some time so that the user can be notified of the last
     * state. By default we wait, so that the user can be notified of the
     * current state of the call.
     */
    private boolean isCloseWaitAfterHangup = true;

    /**
     * The <tt>Observer</tt> which listens to {@link #uiVideoHandler} about
     * changes in the video-related information.
     */
    private final Observer uiVideoHandlerObserver
        = new Observer()
        {
            public void update(Observable o, Object arg)
            {
                uiVideoHandlerUpdate(arg);
            }
        };

    /**
     * The <tt>Runnable</tt> which is scheduled by
     * {@link #updateViewFromModel()} for execution in the AWT event dispatching
     * thread in order to invoke
     * {@link #updateViewFromModelInEventDispatchThread()}.
     */
    private final Runnable updateViewFromModelInEventDispatchThread
        = new Runnable()
        {
            public void run()
            {
                /*
                 * We receive events/notifications from various threads and we
                 * respond to them in the AWT event dispatching thread. It is
                 * possible to first schedule an event to be brought to the AWT
                 * event dispatching thread, then to have #dispose() invoked on
                 * this instance and, finally, to receive the scheduled event in
                 * the AWT event dispatching thread. In such a case, this
                 * disposed instance should not respond to the event.
                 */
                if (!disposed)
                    updateViewFromModelInEventDispatchThread();
            }
        };

    /**
     * The video button.
     */
    private LocalVideoButton videoButton;

    /**
     * Initializes a new <tt>CallPanel</tt> which is to depict a specific
     * <tt>CallConference</tt>.
     *
     * @param callConference the <tt>CallConference</tt> to be depicted by the
     * new instance
     * @param callWindow the parent window in which the new instance will be
     * added
     */
    public CallPanel(   CallConference callConference,
                        CallContainer callWindow)
    {
        super(new BorderLayout());

        this.callConference = callConference;
        this.callWindow = callWindow;

        uiVideoHandler = new UIVideoHandler2(this.callConference);

        callDurationTimer
            = new Timer(
                    1000,
                    new ActionListener()
                            {
                                public void actionPerformed(ActionEvent e)
                                {
                                    setCallTitle(callConferenceStartTime);
                                }
                            });
        callDurationTimer.setRepeats(true);

        // The call duration parameter is not known yet.
        setCallTitle(0);

        initializeUserInterfaceHierarchy();

        dtmfHandler = new DTMFHandler(this);

        /*
         * Adds the listeners which will observe the model and will trigger the
         * updates of this view from it.
         */
        this.callConference.addCallChangeListener(callConferenceListener);
        this.callConference.addCallPeerConferenceListener(
                callConferenceListener);
        this.callConference.addPropertyChangeListener(callConferenceListener);
        uiVideoHandler.addObserver(uiVideoHandlerObserver);

        callWindow.getFrame().addPropertyChangeListener(
                CallContainer.PROP_FULL_SCREEN,
                callConferenceListener);

        updateViewFromModel();

        initPluginComponents();
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
            ConferenceInviteDialog inviteDialog;

            if (callConference.isJitsiVideoBridge())
            {
                inviteDialog
                    = new ConferenceInviteDialog(
                            callConference,
                            callConference.getCalls().get(0)
                                    .getProtocolProvider(),
                            true);
            }
            else
                inviteDialog = new ConferenceInviteDialog(callConference);

            inviteDialog.setVisible(true);
        }
        else if (buttonName.equals(CHAT_BUTTON))
        {
            /*
             * If there is exactly 1 CallPeer capable of instant messaging, then
             * we'll try to start a chat with her.
             */
            /*
             * TODO The following is very likely to block the user interface in
             * a noticeable way sooner or later.
             */
            List<Contact> imCapableCallPeers = getIMCapableCallPeers(1);

            if (imCapableCallPeers.size() == 1)
            {
                Contact contact = imCapableCallPeers.get(0);
                MetaContact metaContact
                    = GuiActivator.getContactListService()
                            .findMetaContactByContact(contact);
                GuiActivator.getUIService().getChatWindowManager().startChat(
                        metaContact);
            }
        }
        else if (buttonName.equals(INFO_BUTTON))
        {
            if (callInfoFrame == null)
            {
                callInfoFrame = new CallInfoFrame(callConference);
                addCallTitleListener(callInfoFrame);
            }
            callInfoFrame.setVisible(
                    callInfoFrame.hasCallInfo() && !callInfoFrame.isVisible());
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
        isCloseWaitAfterHangup = closeWait;

        this.disposeCallInfoFrame();

        /*
         * It is the responsibility of CallManager to close this CallPanel
         * when a Call is ended.
         */
        if (callConference.getCallCount() > 0)
            CallManager.hangupCalls(callConference);
        /*
         * If however there are no more calls related to this panel we will
         * close the window directly. This could happen in the case, where
         * the other side has already hanged up the call, the call window shows
         * the state disconnected and we press the hang up button. In this
         * case the contained call is already null and we should only close the
         * call window.
         */
        else
            callWindow.close(this, false);
    }

    /**
     * Indicates if this call panel should be closed immediately after hang up
     * or should wait some time so that the user can be notified of the last
     * state.
     *
     * @return <tt>true</tt> to indicate that when hanged up this call panel
     * should not be closed immediately, <tt>false</tt> - otherwise
     */
    public boolean isCloseWaitAfterHangup()
    {
        return isCloseWaitAfterHangup;
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
            {
                settingsPanel.add(resizeVideoButton);
                settingsPanel.revalidate();
                settingsPanel.repaint();
            }
        }
    }

    /**
     * Notifies this instance about a <tt>PropertyChangeEvent</tt> fired by
     * {@link #callWindow}.
     *
     * @param ev the <tt>PropertyChangeEvent</tt> fired by <tt>callWindow</tt>
     * to notify this instance about
     */
    private void callWindowPropertyChange(PropertyChangeEvent ev)
    {
        /*
         * We are registered for CallContainer#PROP_FULL_SCREEN only. This
         * instance will fire the notification as its own to allow listeners to
         * register with a source which is more similar to them with respect to
         * life span.
         */
        try
        {
            if (OSUtils.IS_MAC && (bottomBar != null))
                bottomBar.setOpaque(!isFullScreen());
        }
        finally
        {
            firePropertyChange(
                    ev.getPropertyName(),
                    ev.getOldValue(), ev.getNewValue());
        }
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
     * Creates the bottom bar panel for this <tt>CallPanel</tt>.
     *
     * @return a new bottom bar panel for this <tt>CallPanel</tt>
     */
    private JComponent createBottomBar()
    {
        bottomBar
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomBar.setBorder(BorderFactory.createEmptyBorder(0, 30, 2, 30));

        /*
         * The bottomBar on OS X overrides the Swing-defined background.
         * However, full-screen display usually uses a black background. The
         * black background will be set elsewhere on an ancestor but we have to
         * make sure that bottomBar's background does not interfere with the
         * setting.
         */
        if (OSUtils.IS_MAC)
        {
            bottomBar.setOpaque(!isFullScreen());
            bottomBar.setBackground(
                    new Color(GuiActivator.getResources().getColor(
                            "service.gui.MAC_PANEL_BACKGROUND")));
        }

        bottomBar.add(settingsPanel);

        return bottomBar;
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
        disposed = true;

        callConference.removeCallChangeListener(callConferenceListener);
        callConference.removeCallPeerConferenceListener(callConferenceListener);
        callConference.removePropertyChangeListener(callConferenceListener);

        uiVideoHandler.deleteObserver(uiVideoHandlerObserver);
        uiVideoHandler.dispose();

        callWindow.getFrame().removePropertyChangeListener(
                CallContainer.PROP_FULL_SCREEN,
                callConferenceListener);

        if (callPanel != null)
        {
            if(callPanel instanceof BasicConferenceCallPanel)
            {
                ((BasicConferenceCallPanel) callPanel)
                    .removePeerViewListener(this);
            }
            ((CallRenderer) callPanel).dispose();
        }

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
     * Updates {@link #settingsPanel} from the model of this view. The update is
     * performed in the AWT event dispatching thread.
     * <p>
     * The center of this view is occupied by {@link #callPanel}, the bottom of
     * this view is dedicated to <tt>settingsPanel</tt>. The method
     * {@link #updateViewFromModelInEventDispatchThread()} updates
     * <tt>callPanel</tt> from the model of this view and then invokes the
     * method <tt>updateSettingsPanelInEventDispatchThread()</tt>. Thus this
     * whole view is updated so that it depicts the current state of its model.
     * </p>
     *
     * @param callConferenceIsEnded <tt>true</tt> if the method
     * <tt>updateViewFromModelInEventDispatchThread()</tt> considers the
     * {@link #callConference} ended; otherwise, <tt>false</tt>. When the
     * <tt>callConference</tt> is considered ended, the <tt>callPanel</tt>
     * instance will not be switched to a specific type (one-to-one, audio-only,
     * or audio/video) because, otherwise, the switch will leave it
     * <tt>null</tt> and this view will remain blank. In such a case,
     * <tt>settingsPanel</tt> may wish to do pretty much the same but disable
     * and/or hide the buttons it contains.
     */
    private void doUpdateSettingsPanelInEventDispatchThread(
            boolean callConferenceIsEnded)
    {
        settingsPanel.setFullScreen(isFullScreen());

        boolean isConference = (callPanel instanceof BasicConferenceCallPanel);

        /*
         * For whatever reason, we're treating the localLevel and the
         * remoteLevel buttons differently and we're adding and removing them in
         * accord with the conference state of the user interface.
         */
        if (isConference)
        {
            settingsPanel.add(localLevel);
            settingsPanel.add(remoteLevel);
        }
        else
        {
            settingsPanel.remove(localLevel);
            settingsPanel.remove(remoteLevel);
        }

        /*
         * We do not support chat conferencing with the participants in a
         * telephony conference at this time so we do not want the chatButton
         * visible in such a scenario.
         */
        chatButton.setVisible(
                !isConference && (getIMCapableCallPeers(1).size() == 1));

        updateHoldButtonState();
        updateMergeButtonState();

        List<Call> calls = callConference.getCalls();
        /*
         * OperationSetAdvancedTelephony implements call transfer. The feature
         * is not supported if the local user/peer is a conference focus.
         * Instead of disabling the transferCallButton in this case though, we
         * want it hidden.
         */
        boolean advancedTelephony = !calls.isEmpty();
        boolean telephonyConferencing = false;
        boolean videoTelephony = false;
        boolean videoTelephonyIsLocalVideoAllowed = false;
        boolean videoTelephonyIsLocalVideoStreaming = false;
        boolean desktopSharing = false;
        boolean desktopSharingIsStreamed = false;
        boolean allCallsConnected = true;

        for (Call call : calls)
        {
            ProtocolProviderService pps = call.getProtocolProvider();

            /*
             * The transferCallButton requires OperationSetAdvancedTelephony
             * for all Calls.
             */
            if (advancedTelephony)
            {
                OperationSetAdvancedTelephony<?> osat
                    = pps.getOperationSet(OperationSetAdvancedTelephony.class);

                if (osat == null)
                    advancedTelephony = false;
            }

            /*
             * The conferenceButton needs at least one Call with
             * OperationSetTelephonyConferencing,
             */
            if (!telephonyConferencing)
            {
                OperationSetTelephonyConferencing ostc
                    = pps.getOperationSet(
                            OperationSetTelephonyConferencing.class);

                if (ostc != null)
                    telephonyConferencing = true;
            }

            if (!videoTelephony
                    || !videoTelephonyIsLocalVideoAllowed
                    || !videoTelephonyIsLocalVideoStreaming)
            {
                OperationSetVideoTelephony osvt
                    = pps.getOperationSet(OperationSetVideoTelephony.class);

                if (osvt != null)
                {
                    if (!videoTelephony)
                        videoTelephony = true;
                    if (!videoTelephonyIsLocalVideoAllowed
                            && osvt.isLocalVideoAllowed(call))
                        videoTelephonyIsLocalVideoAllowed = true;
                    if (!videoTelephonyIsLocalVideoStreaming
                            && osvt.isLocalVideoStreaming(call))
                        videoTelephonyIsLocalVideoStreaming = true;
                }
            }

            if(!desktopSharing)
            {
                OperationSetDesktopStreaming osds
                    = pps.getOperationSet(
                            OperationSetDesktopStreaming.class);
                if(osds != null)
                {
                    desktopSharing = true;

                    if(videoTelephonyIsLocalVideoStreaming
                            && call instanceof MediaAwareCall
                            && ((MediaAwareCall<?,?,?>) call).getMediaUseCase()
                                == MediaUseCase.DESKTOP)
                    {
                        desktopSharingIsStreamed = true;
                    }
                }
            }

            if (CallState.CALL_IN_PROGRESS != call.getCallState())
            {
                allCallsConnected = false;
            }
        }

        conferenceButton.setEnabled(telephonyConferencing);
        transferCallButton.setEnabled(advancedTelephony);
        transferCallButton.setVisible(!callConference.isConferenceFocus());

        /*
         * The videoButton is a beast of its own kind because it depends not
         * only on the state of the depicted telephony conference but also on
         * the global application state.
         */
        videoButton.setEnabled(allCallsConnected && videoTelephony);
        videoButton.setSelected(videoTelephonyIsLocalVideoAllowed);

        /*
         * Consequently, the showHideVideoButton which depends on videoButton
         * has to be updated depending on the state of the videoButton as well.
         */
        showHideVideoButton.setEnabled(
                videoButton.isEnabled()
                    && videoTelephonyIsLocalVideoAllowed);
        showHideVideoButton.setSelected(
                showHideVideoButton.isEnabled()
                    && uiVideoHandler.isLocalVideoVisible());
        showHideVideoButton.setVisible(showHideVideoButton.isEnabled());

        // The desktop sharing button depends on the operation set desktop
        // sharing server.
        desktopSharingButton.setEnabled(desktopSharing);
        desktopSharingButton.setSelected(desktopSharingIsStreamed);
        if (callPanel instanceof OneToOneCallPanel)
        {
            OneToOneCallPanel oneToOneCallPanel = (OneToOneCallPanel) callPanel;
            if(desktopSharingIsStreamed)
                oneToOneCallPanel.addDesktopSharingComponents();
            else
                oneToOneCallPanel.removeDesktopSharingComponents();
        }
    }

    /**
     * Updates this view i.e. <tt>CallPanel</tt> so that it depicts the current
     * state of its model i.e. <tt>callConference</tt>. The update is performed
     * in the AWT event dispatching thread.
     */
    private void doUpdateViewFromModelInEventDispatchThread()
    {
        /*
         * If the telephony conference depicted by this instance has ended, do
         * not update the user interface because it will be left blank. It is
         * CallManager's responsibility to dispose of this CallPanel after its
         * telephony conference has ended. Additionally, the various types of
         * callPanel will usually require at least one CallPeer in order to not
         * be blank. The absence of CallPeers usually indicates that a Call and,
         * respectively, a telephony conference has ended. So it makes some
         * sense to skip the update in such cases in order to try to not have
         * the user interface blank.
         */
        if (callConference.isEnded()
                || (callConference.getCallPeerCount() == 0))
        {
            /*
             * However, the settingsPanel contains buttons which may still need
             * to be disabled and/or hidden.
             */
            updateSettingsPanelInEventDispatchThread(true);
            return;
        }

        boolean isConference = isConference();
        boolean isVideo = CallManager.isVideoStreaming(callConference);
        CallPeer callPeer = null;
        boolean validateAndRepaint = false;

        if (callPanel != null)
        {
            boolean removeCallPanel;

            if (isConference)
            {
                if (callPanel instanceof BasicConferenceCallPanel)
                {
                    if (isVideo)
                    {
                        removeCallPanel
                            = !(callPanel instanceof VideoConferenceCallPanel);
                    }
                    else
                    {
                        removeCallPanel
                            = (callPanel instanceof VideoConferenceCallPanel);
                    }
                }
                else
                {
                    removeCallPanel = true;
                }
            }
            else
            {
                if (callPanel instanceof OneToOneCallPanel)
                {
                    if (callPeer == null)
                    {
                        List<CallPeer> callPeers
                            = callConference.getCallPeers();

                        if (!callPeers.isEmpty())
                            callPeer = callPeers.get(0);
                    }
                    removeCallPanel
                        = !((OneToOneCallPanel) callPanel).getCallPeer().equals(
                                callPeer);
                }
                else
                {
                    if( (callPanel instanceof BasicConferenceCallPanel) &&
                        ((BasicConferenceCallPanel) callPanel)
                            .hasDelayedCallPeers())
                    {
                        removeCallPanel = false;
                    }
                    else
                    {
                        removeCallPanel = true;
                    }

                }
            }
            if (removeCallPanel)
            {
                remove(callPanel);
                validateAndRepaint = true;
                try
                {
                    ((CallRenderer) callPanel).dispose();
                }
                finally
                {
                    callPanel = null;
                }
            }
        }
        if (callPanel == null)
        {
            if (isConference)
            {
                if (isVideo)
                {
                    callPanel
                        = new VideoConferenceCallPanel(
                                this,
                                callConference,
                                uiVideoHandler);
                }
                else
                {
                    callPanel
                        = new AudioConferenceCallPanel(this, callConference);
                }

                ((BasicConferenceCallPanel) callPanel)
                    .addPeerViewlListener(this);
            }
            else
            {
                if (callPeer == null)
                {
                    List<CallPeer> callPeers = callConference.getCallPeers();

                    if (!callPeers.isEmpty())
                        callPeer = callPeers.get(0);
                }
                if (callPeer != null)
                {
                    callPanel
                        = new OneToOneCallPanel(this, callPeer, uiVideoHandler);
                }
            }
            if (callPanel != null)
            {
                add(callPanel, BorderLayout.CENTER);
                validateAndRepaint = true;
            }
        }

        try
        {
            /*
             * The center of this view is occupied by callPanel and we have just
             * updated it. The bottom of this view is dedicated to settingsPanel
             * so we have to update it as well.
             */
            updateSettingsPanelInEventDispatchThread(false);
        }
        finally
        {
            /*
             * It seems that AWT/Swing does not validate and/or repaint this
             * Container (enough) and, consequently, its display may not update
             * itself with an up-to-date drawing of the current callPanel.
             */
            if (validateAndRepaint)
            {
                if (isDisplayable())
                {
                    validate();
                    repaint();
                }
                else
                    doLayout();
            }
        }
    }

    /**
     * Attempts to give a specific <tt>Component</tt> a visible rectangle with a
     * specific width  and a specific height if possible and sane by resizing
     * the <tt>Window</tt> which contains this instance.
     *
     * @param component the <tt>Component</tt> which requests a visible
     * rectangle with the specified <tt>width</tt> and <tt>height</tt>
     * @param width the width of the visible rectangle requested by the
     * specified <tt>component</tt>
     * @param height the height of the visible rectangle requested by the
     * specified <tt>component</tt>
     */
    private void ensureSize(Component component, int width, int height)
    {
        CallContainer callContainer = getCallWindow();

        if (callContainer != null)
            callContainer.ensureSize(component, width, height);
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
     * Returns the parent call window.
     *
     * @return the parent call window
     */
    public CallContainer getCallWindow()
    {
        return callWindow;
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
     * Returns the <tt>DialpadDialog</tt> corresponding to this CallDialog.
     *
     * @return the <tt>DialpadDialog</tt> corresponding to this CallDialog.
     */
    private DialpadDialog getDialpadDialog()
    {
        return new DialpadDialog(dtmfHandler);
    }

    /**
     * Finds the <tt>Contact</tt>s which are participating in the telephony
     * conference depicted by this instance and which are capable of instant
     * messaging i.e. support {@link OperationSetBasicTelephony}.
     *
     * @param limit the maximum number of <tt>Contact</tt>s to be found. Since
     * it is expensive in terms of execution time (at very least) to find a
     * <tt>Contact</tt> which stands for a <tt>CallPeer</tt> (and to query it
     * whether it supports instant messaging), it is advised to limit the search
     * as much as possible. For example, the <tt>chatButton</tt> is enabled
     * and/or shown only when there is exactly one such <tt>Contact</tt> so it
     * makes perfect sense to specify <tt>1</tt> as the <tt>limit</tt> in the
     * case.
     * @return a <tt>List</tt> of the <tt>Contact</tt>s which are participating
     * in the telephony conference depicted by this instance and which are
     * capable of instant messaging i.e. support
     * <tt>OperationSetBasicTelephony</tt>
     */
    private List<Contact> getIMCapableCallPeers(int limit)
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
            else
            {
                Contact contact = CallManager.getIMCapableCusaxContact(callPeer);
                if (contact != null)
                {
                    contacts.add(contact);
                }
            }

            if (contacts.size() >= limit)
                break;
        }
        return contacts;
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
     * Initializes buttons order in the call tool bar.
     */
    private void initButtonIndexes()
    {
        dialButton.setIndex(0);
        conferenceButton.setIndex(1);
        holdButton.setIndex(2);
        if (recordButton != null)
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
     * Initialize plug-in components already registered for this container.
     */
    private void initPluginComponents()
    {
        // Search for plug-in components registered through the OSGI
        // BundleContext.

        String osgiFilter
            = "("
                + net.java.sip.communicator.service.gui.Container.CONTAINER_ID
                + "="
                + net.java.sip.communicator.service.gui.Container
                    .CONTAINER_CALL_DIALOG.getID()
                + ")";
        ServiceReference[] serRefs = null;

        try
        {
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        PluginComponent.class.getName(),
                        osgiFilter);
        }
        catch (InvalidSyntaxException ise)
        {
            logger.error("Could not obtain plugin reference.", ise);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                PluginComponent component
                    = (PluginComponent)
                        GuiActivator.bundleContext.getService(serRef);

                component.setCurrentContact(
                    CallManager.getPeerMetaContact(
                        callConference.getCallPeers().get(0)));

                settingsPanel.add((Component) component.getComponent());
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Initializes the user interface hierarchy of this <tt>CallPanel</tt> i.e.
     * the AWT <tt>Component</tt>s which constitute the user interface to be
     * displayed by this <tt>Component</tt>. Their state does not have to depict
     * the current state of the model of this view because
     * {@link #updateViewFromModel()} will be invoked before this view becomes
     * visible. At the center of the user interface of this view is
     * {@link #callPanel} but it is dynamically added and removed multiple times
     * as part of the execution of the <tt>updateViewFromModel</tt> method so
     * it is not dealt with here.
     */
    private void initializeUserInterfaceHierarchy()
    {
        /*
         * The settingsPanel will contain the buttons. It is initialized before
         * the buttons in case any of the buttons need it (which is hard to
         * determine at the time of this writing).
         */
        settingsPanel = new CallToolBar(isFullScreen(), false);

        /*
         * TODO CallPanel depicts a whole CallConference which may have multiple
         * Calls, new Calls may be added to the CallConference and existing
         * Calls may be removed from the CallConference. For example, the
         * buttons which accept a Call as an argument should be changed to take
         * into account the whole CallConference.
         */
        Call aCall = callConference.getCalls().get(0);

        chatButton
            = new CallToolBarButton(
                    ImageLoader.getImage(ImageLoader.CHAT_BUTTON_SMALL_WHITE),
                    CHAT_BUTTON,
                    GuiActivator.getResources().getI18NString(
                            "service.gui.CHAT"));
        conferenceButton
            = new CallToolBarButton(
                    ImageLoader.getImage(ImageLoader.ADD_TO_CALL_BUTTON),
                    CONFERENCE_BUTTON,
                    GuiActivator.getResources().getI18NString(
                            "service.gui.CREATE_CONFERENCE_CALL"));
        desktopSharingButton = new DesktopSharingButton(aCall);
        dialButton
            = new CallToolBarButton(
                    ImageLoader.getImage(ImageLoader.DIAL_BUTTON),
                    DIAL_BUTTON,
                    GuiActivator.getResources().getI18NString(
                            "service.gui.DIALPAD"));
        fullScreenButton = new FullScreenButton(this);
        hangupButton = new HangupButton(this);
        holdButton = new HoldButton(aCall);
        if(!GuiActivator.getConfigurationService().getBoolean(
                HIDE_CALL_INFO_BUTON_PROP,
                false))
        {
            infoButton
                = new CallToolBarButton(
                        ImageLoader.getImage(ImageLoader.CALL_INFO),
                        INFO_BUTTON,
                        GuiActivator.getResources().getI18NString(
                                "service.gui.PRESS_FOR_CALL_INFO"));
        }
        mergeButton
            = new CallToolBarButton(
                    ImageLoader.getImage(ImageLoader.MERGE_CALL_BUTTON),
                    MERGE_BUTTON,
                    GuiActivator.getResources().getI18NString(
                            "service.gui.MERGE_TO_CALL"));

        if(!GuiActivator.getConfigurationService().getBoolean(
            HIDE_CALL_RECORD_BUTON_PROP,
            false))
        {
            recordButton = new RecordButton(aCall);
        }
        showHideVideoButton = new ShowHideVideoButton(uiVideoHandler);
        transferCallButton = new TransferCallButton(aCall);
        videoButton = new LocalVideoButton(aCall);

        localLevel
            = new InputVolumeControlButton(
                    aCall,
                    ImageLoader.MICROPHONE,
                    ImageLoader.MUTE_BUTTON,
                    true,
                    false);
        remoteLevel
            = new OutputVolumeControlButton(
                    callConference,
                    ImageLoader.VOLUME_CONTROL_BUTTON,
                    false,
                    true)
                .getComponent();

        /*
         * Now that the buttons have been initialized, set their order indexes
         * so that they get added in the correct order later on.
         */
        initButtonIndexes();

        chatButton.addActionListener(this);
        conferenceButton.addActionListener(this);
        dialButton.addActionListener(this);
        if (infoButton != null)
            infoButton.addActionListener(this);
        mergeButton.addActionListener(this);

        settingsPanel.add(chatButton);
        settingsPanel.add(conferenceButton);
        settingsPanel.add(desktopSharingButton);
        settingsPanel.add(dialButton);
        settingsPanel.add(fullScreenButton);
        settingsPanel.add(hangupButton);
        settingsPanel.add(holdButton);
        if (infoButton != null)
            settingsPanel.add(infoButton);
        settingsPanel.add(mergeButton);
        if (recordButton != null)
            settingsPanel.add(recordButton);
        settingsPanel.add(showHideVideoButton);
        settingsPanel.add(transferCallButton);
        settingsPanel.add(videoButton);

        // The bottom bar will contain the settingsPanel.
        add(createBottomBar(), BorderLayout.SOUTH);
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
     * Checks if the contained call is a conference call.
     *
     * @return <code>true</code> if the contained <tt>Call</tt> is a conference
     * call, otherwise - returns <code>false</code>.
     */
    boolean isConference()
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
     * Determines whether this view is displayed in full-screen or windowed
     * mode.
     *
     * @return <tt>true</tt> if this view is displayed in full-screen mode or
     * <tt>false</tt> for windowed mode
     */
    boolean isFullScreen()
    {
        return callWindow.isFullScreen();
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
        if (recordButton == null)
            return false;

        return recordButton.isSelected();
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

        if (hangupButton != null)
            hangupButton.setBackgroundImage(
                    ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));
    }

    /**
     * Notifies this instance about a specific <tt>VideoEvent</tt> which may
     * warrant {@link #ensureSize(Component, int, int)} to be invoked in order
     * to try to have the associated visual <tt>Component</tt> displaying video
     * shown without scaling. The method will execute on the AWT event
     * dispatching thread because it will be making its judgments based on the
     * properties of AWT <tt>Component</tt>s.
     *
     * @param ev a <tt>VideoEvent</tt> which represents the cause of the
     * notification and specifies the visual <tt>Component</tt> displaying video
     * which may need an adjustment of a Frame's size in order to be displayed
     * without scaling
     */
    private void maybeEnsureSize(final VideoEvent ev)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            maybeEnsureSize(ev);
                        }
                    });
            return;
        }

        if (ev instanceof SizeChangeVideoEvent)
        {
            /*
             * If a visual Component depicting video (streaming between the
             * local peer/user and the remote peers) changes its size, try to
             * adjust the size of the Frame which displays it so that it appears
             * without scaling.
             */
            SizeChangeVideoEvent scev = (SizeChangeVideoEvent) ev;

            ensureSize(
                    scev.getVisualComponent(),
                    scev.getWidth(), scev.getHeight());
        }
        else if (ev.getType() == VideoEvent.VIDEO_ADDED)
        {
            Component video = ev.getVisualComponent();

            if ((video != null)
                    && UIVideoHandler2.isAncestor(this, video)
                    && video.isPreferredSizeSet())
            {
                Dimension prefSize = video.getPreferredSize();

                if ((prefSize.height > 0) && (prefSize.width > 0))
                {
                    Dimension size = video.getSize();

                    if ((prefSize.height > size.height)
                            || (prefSize.width > size.width))
                    {
                        ensureSize(
                                video,
                                prefSize.width, prefSize.height);
                    }
                }
            }
        }
    }

    /**
     * Invoked by {@link #callConferenceListener} to notify this instance about
     * an <tt>EventObject</tt> related to the <tt>CallConference</tt> depicted
     * by this <tt>CallPanel</tt>, the <tt>Call</tt>s participating in it,
     * the <tt>CallPeer</tt>s associated with them, the
     * <tt>ConferenceMember</tt>s participating in any telephony conferences
     * organized by them, etc. In other words, notifies this instance about
     * any change which may cause an update to be required so that this view
     * i.e. <tt>CallPanel</tt> depicts the current state of its model i.e.
     * {@link #callConference}.
     *
     * @param ev the <tt>EventObject</tt> this instance is being notified
     * about.
     */
    private void onCallConferenceEventObject(EventObject ev)
    {
        /*
         * The main task is to invoke updateViewFromModel() in order to make
         * sure that this view depicts the current state of its model.
         */

        try
        {
            /*
             * However, we seem to be keeping track of the duration of the call
             * (i.e. the telephony conference) in the user interface. Stop the
             * Timer which ticks the duration of the call as soon as the
             * telephony conference depicted by this instance appears to have
             * ended. The situation will very likely occur when a Call is
             * removed from the telephony conference or a CallPeer is removed
             * from a Call.
             */
            boolean tryStopCallTimer = false;

            if (ev instanceof CallPeerEvent)
            {
                tryStopCallTimer
                    = (CallPeerEvent.CALL_PEER_REMOVED
                            == ((CallPeerEvent) ev).getEventID());
            }
            else if (ev instanceof PropertyChangeEvent)
            {
                PropertyChangeEvent pcev = (PropertyChangeEvent) ev;

                tryStopCallTimer
                    = (CallConference.CALLS.equals(pcev.getPropertyName())
                            && (pcev.getOldValue() instanceof Call)
                            && (pcev.getNewValue() == null));
            }
            if (tryStopCallTimer
                    && (callConference.isEnded()
                            || callConference.getCallPeerCount() == 0))
            {
                stopCallTimer();
            }
        }
        finally
        {
            updateViewFromModel();
        }
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
     * Adds/removes the <tt>Component</tt> of the <tt>PluginComponent</tt>
     * specified by a <tt>PluginComponentEvent</tt> to/from
     * {@link #settingsPanel} (if it is appropriate for this
     * <tt>Container</tt>).
     *
     * @param ev a <tt>PluginComponentEvent</tt> which specifies the
     * <tt>PluginComponent</tt> whose <tt>Component</tt> is to be added/removed
     * to/from {@link #settingsPanel}
     */
    protected void onPluginComponentEvent(PluginComponentEvent ev)
    {
        PluginComponent pc = ev.getPluginComponent();
        Object c;

        if (pc.getContainer().equals(
                    net.java.sip.communicator.service.gui.Container
                            .CONTAINER_CALL_DIALOG)
                && ((c = pc.getComponent()) instanceof Component))
        {
            pc.setCurrentContact(
                CallManager.getPeerMetaContact(
                    callConference.getCallPeers().get(0)));

            switch (ev.getEventID())
            {
            case PluginComponentEvent.PLUGIN_COMPONENT_ADDED:
                settingsPanel.add((Component) c);
                break;
            case PluginComponentEvent.PLUGIN_COMPONENT_REMOVED:
                settingsPanel.remove((Component) c);
                break;
            }

            settingsPanel.revalidate();
            settingsPanel.repaint();
        }
    }

    /**
     * Indicates that the peer panel was added.
     *
     * @param ev the event.
     */
    public void peerViewAdded(ConferencePeerViewEvent ev) {}

    /**
     * Indicates that the peer panel was removed.
     *
     * @param ev the event.
     */
    public void peerViewRemoved(ConferencePeerViewEvent ev)
    {
        updateViewFromModel();
    }

    /**
     * {@inheritDoc}
     *
     * Adds the <tt>Component</tt> of the <tt>PluginComponent</tt> specified by
     * the <tt>PluginComponentEvent</tt> to {@link #settingsPanel} (if it is
     * appropriate for this <tt>Container</tt>).
     */
    public void pluginComponentAdded(PluginComponentEvent ev)
    {
        onPluginComponentEvent(ev);
    }

    /**
     * {@inheritDoc}
     *
     * Removes the <tt>Component</tt> of the <tt>PluginComponent</tt> specified
     * by the <tt>PluginComponentEvent</tt> from {@link #settingsPanel} (if it
     * is appropriate for this <tt>Container</tt>).
     */
    public void pluginComponentRemoved(PluginComponentEvent ev)
    {
        onPluginComponentEvent(ev);
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
     * Remove remote video specific components.
     */
    public void removeRemoteVideoSpecificComponents()
    {
        if(resizeVideoButton != null)
            settingsPanel.remove(resizeVideoButton);

        settingsPanel.revalidate();
        settingsPanel.repaint();
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
     * Sets the display of this view to full-screen or windowed mode.
     *
     * @param fullScreen <tt>true</tt> to display this view in full-screen mode
     * or <tt>false</tt> for windowed mode
     */
    void setFullScreen(boolean fullScreen)
    {
        callWindow.setFullScreen(fullScreen);
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
     * Notifies this instance that {@link #uiVideoHandler} has reported a change
     * in the video-related information which may warrant an update of this view
     * from its model.
     *
     * @param arg an <tt>Object</tt>, if any, which represents the cause that
     * triggered the notification
     */
    private void uiVideoHandlerUpdate(Object arg)
    {
        /* The most important task is to update this view from its model. */

        /*
         * If a visual Component displaying video is reported to have been
         * added/prepared/received, we may have to adjust the size of the Frame
         * displaying this user interface so that the video appears without
         * scaling.
         */
        /*
         * XXX The following may be making judgments about the user interface
         * out of the AWT event dispatching thread which is a prerequisite for
         * unexpected behavior. Anyway, that's the only idea at the time of this
         * writing.
         */
        VideoEvent maybeEnsureSize = null;

        if (arg instanceof VideoEvent)
        {
            try
            {
                VideoEvent vev = (VideoEvent) arg;
                int vevType = vev.getType();

                if (vevType == VideoEvent.VIDEO_ADDED)
                {
                    Component video = vev.getVisualComponent();

                    if ((video != null)
                            && !UIVideoHandler2.isAncestor(this, video))
                    {
                        maybeEnsureSize = vev;
                    }
                }
                else if (vevType == SizeChangeVideoEvent.VIDEO_SIZE_CHANGE)
                {
                    /*
                     * If a visual Component depicting video (streaming between
                     * the local peer/user and the remote peers) changes its
                     * size, try to adjust the size of the Frame which displays
                     * it so that it appears without scaling.
                     */
                    maybeEnsureSize = vev;
                }
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
                else if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "Failed to determine whether it is necessary to"
                                + " adjust a Frame's size in response to a"
                                + " VideoEvent.",
                            t);
                }
            }
        }

        updateViewFromModel();

        if (maybeEnsureSize != null)
        {
            /*
             * The method maybeEnsureSize will execute on the AWT event
             * dispatching thread.
             */
            try
            {
                maybeEnsureSize(maybeEnsureSize);
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
                else
                {
                    logger.error(
                            "Failed to adjust a Frame's size"
                                + " in response to a VideoEvent.",
                            t);
                }
            }
        }
    }

    /**
     * Updates the state of the general hold button. The hold button is selected
     * only if all call peers are locally or mutually on hold at the same time.
     * In all other cases the hold button is unselected.
     */
    public void updateHoldButtonState()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateHoldButtonState();
                }
            });

            return;
        }

        List<CallPeer> peers = callConference.getCallPeers();
        boolean areAllPeersLocallyOnHold;

        if (peers.isEmpty())
        {
            /*
             * It feels natural to not have the holdButton selected when there
             * are no peers.
             */
            areAllPeersLocallyOnHold = false;
        }
        else
        {
            areAllPeersLocallyOnHold = true;
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
        }

        // If we have clicked the hold button in a full screen mode or selected
        // hold of the peer menu in a conference call we need to update the
        // state of the call dialog hold button.
        holdButton.setSelected(areAllPeersLocallyOnHold);
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
     * Updates {@link #settingsPanel} from the model of this view. The update is
     * performed in the AWT event dispatching thread.
     * <p>
     * The center of this view is occupied by {@link #callPanel}, the bottom of
     * this view is dedicated to <tt>settingsPanel</tt>. The method
     * {@link #updateViewFromModelInEventDispatchThread()} updates
     * <tt>callPanel</tt> from the model of this view and then invokes the
     * method <tt>updateSettingsPanelInEventDispatchThread()</tt>. Thus this
     * whole view is updated so that it depicts the current state of its model.
     * </p>
     *
     * @param callConferenceIsEnded <tt>true</tt> if the method
     * <tt>updateViewFromModelInEventDispatchThread()</tt> considers the
     * {@link #callConference} ended; otherwise, <tt>false</tt>. When the
     * <tt>callConference</tt> is considered ended, the <tt>callPanel</tt>
     * instance will not be switched to a specific type (one-to-one, audio-only,
     * or audio/video) because, otherwise, the switch will leave it
     * <tt>null</tt> and this view will remain blank. In such a case,
     * <tt>settingsPanel</tt> may wish to do pretty much the same but disable
     * and/or hide the buttons it contains.
     */
    private void updateSettingsPanelInEventDispatchThread(
            boolean callConferenceIsEnded)
    {
        /*
         * XXX The method directly delegates to the method
         * doUpdateSettingsPanelInEventDispatchThread at the time of this
         * writing which may be considered a waste. But in the fashion of the
         * method updateViewFromModelInEventDispatchThread we have made it easy
         * to add code before and/or after the invocation of the delegate.
         */
        doUpdateSettingsPanelInEventDispatchThread(callConferenceIsEnded);
    }

    /**
     * Updates this view i.e. <tt>CallPanel</tt> so that it depicts the current
     * state of its model i.e. <tt>callConference</tt>.
     */
    private void updateViewFromModel()
    {
        /*
         * We receive events/notifications from various threads and we respond
         * to them in the AWT event dispatching thread. It is possible to first
         * schedule an event to be brought to the AWT event dispatching thread,
         * then to have #dispose() invoked on this instance and, finally, to
         * receive the scheduled event in the AWT event dispatching thread. In
         * such a case, this disposed instance should not respond to the event.
         */
        if (!disposed)
        {
            if (SwingUtilities.isEventDispatchThread())
                updateViewFromModelInEventDispatchThread();
            else
            {
                SwingUtilities.invokeLater(
                        updateViewFromModelInEventDispatchThread);
            }
        }
    }

    /**
     * Updates this view i.e. <tt>CallPanel</tt> so that it depicts the current
     * state of its model i.e. <tt>callConference</tt>. The update is performed
     * in the AWT event dispatching thread.
     */
    private void updateViewFromModelInEventDispatchThread()
    {
        /*
         * We receive events/notifications from various threads and we respond
         * to them in the AWT event dispatching thread. It is possible to first
         * schedule an event to be brought to the AWT event dispatching thread,
         * then to have #dispose() invoked on this instance and, finally, to
         * receive the scheduled event in the AWT event dispatching thread. In
         * such a case, this disposed instance should not respond to the event.
         */
        if (disposed)
            return;

        /*
         * We may add, remove, show, and hide various Components of the user
         * interface hierarchy of this instance bellow. Consequently, this view
         * may become larger in width and/or height than its current Frame has
         * dedicated to it. Try to detect such cases and attempt to adjust the
         * Frame's size accordingly.
         */
        Dimension oldPrefSize = getPreferredSize();

        doUpdateViewFromModelInEventDispatchThread();

        /*
         * We may have added, removed, shown, and hidden various Components of
         * the user interface hierarchy of this instance above. Consequently,
         * this view may have become larger in width and/or height than its
         * current Frame has dedicated to it. Try to detect such cases and
         * attempt to adjust the Frame's size accordingly.
         */
        Dimension newPrefSize = getPreferredSize();

        if ((newPrefSize != null)
                && ((newPrefSize.height > getHeight())
                        || (newPrefSize.width > getWidth())))
        {
            int oldPrefHeight, oldPrefWidth;

            if (oldPrefSize == null)
            {
                oldPrefHeight = 0;
                oldPrefWidth = 0;
            }
            else
            {
                oldPrefHeight = oldPrefSize.height;
                oldPrefWidth = oldPrefSize.width;
            }
            if ((newPrefSize.height != oldPrefHeight)
                    || (newPrefSize.width != oldPrefWidth))
            {
                ensureSize(
                        this,
                        newPrefSize.width, newPrefSize.height);
            }
        }
    }

    /**
     * Implements the listener which listens to events fired by the
     * <tt>CallConference</tt> depicted by this instance, the <tt>Call</tt>s
     * participating in that telephony conference, the <tt>CallPeer</tt>s
     * associated with those <tt>Call</tt>s and the <tt>ConferenceMember</tt>s
     * participating in the telephony conferences organized by those
     * <tt>CallPeer</tt>s. Updates this view i.e. CallPanel so that it depicts
     * the current state of its model i.e. {@link #callConference}.
     */
    private class CallConferenceListener
        extends CallPeerConferenceAdapter
        implements CallChangeListener,
                   PropertyChangeListener
    {
        /**
         * {@inheritDoc}
         *
         * Invokes {@link #onCallPeerEvent(CallPeerEvent)} because the
         * <tt>CallPeerEvent</tt> allows distinguishing whether a
         * <tt>CallPeer</tt> was added or removed by examining its
         * <tt>eventID</tt>.
         */
        public void callPeerAdded(CallPeerEvent ev)
        {
            onCallPeerEvent(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Invokes {@link #onCallPeerEvent(CallPeerEvent)} because the
         * <tt>CallPeerEvent</tt> allows distinguishing whether a
         * <tt>CallPeer</tt> was added or removed by examining its
         * <tt>eventID</tt>.
         */
        public void callPeerRemoved(CallPeerEvent ev)
        {
            onCallPeerEvent(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Invokes {@link #onEventObject(EventObject)}.
         */
        public void callStateChanged(CallChangeEvent ev)
        {
            onEventObject(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Invokes {@link #onEventObject(EventObject)}.
         */
        @Override
        protected void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
        {
            onEventObject(ev);
        }

        /**
         * Notifies this <tt>CallChangeListener</tt> about a specific
         * <tt>CallPeerEvent</tt> i.e. that a <tt>CallPeer</tt> was added to or
         * removed from a <tt>Call</tt>. Invokes
         * {@link #onEventObject(EventObject)}.
         *
         * @param ev the <tt>CallPeerEvent</tt> to notify this
         * <tt>CallChangeListener</tt> about i.e. which specifies the
         * <tt>CallPeer</tt> which was added/removed and the <tt>Call</tt>
         * to/from which it was added/removed
         */
        private void onCallPeerEvent(CallPeerEvent ev)
        {
            onEventObject(ev);
        }

        /**
         * Invoked by the various listener method implementations provided by
         * this <tt>CallConferenceListener</tt> to notify this instance about an
         * <tt>EventObject</tt> related to the <tt>CallConference</tt> depicted
         * by this <tt>CallPanel</tt>, the <tt>Call</tt>s participating in it,
         * the <tt>CallPeer</tt>s associated with them, the
         * <tt>ConferenceMember</tt>s participating in any telephony conferences
         * organized by them, etc. In other words, notifies this instance about
         * any change which may cause an update to be required so that this view
         * i.e. <tt>CallPanel</tt> depicts the current state of its model i.e.
         * {@link CallPanel#callConference}.
         *
         * @param ev the <tt>EventObject</tt> this instance is being notified
         * about.
         */
        private void onEventObject(EventObject ev)
        {
            onCallConferenceEventObject(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Invokes {@link #onEventObject(EventObject)}.
         */
        public void propertyChange(PropertyChangeEvent ev)
        {
            String propertyName = ev.getPropertyName();

            /*
             * If a Call is added to or removed from the CallConference depicted
             * by this CallPanel, an update of the view from its model will most
             * likely be required.
             */
            if (propertyName.equals(CallConference.CALLS))
            {
                onEventObject(ev);
            }
            else if (propertyName.equals(CallContainer.PROP_FULL_SCREEN))
            {
                if (ev.getSource().equals(callWindow.getFrame()))
                {
                    try
                    {
                        /*
                         * We'll turn the switching between full-screen and
                         * windowed mode into a model state because a
                         * significant part of this view changes upon such a
                         * switch.
                         */
                        onEventObject(ev);
                    }
                    finally
                    {
                        callWindowPropertyChange(ev);
                    }
                }
            }
        }
    }
}
