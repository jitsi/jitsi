/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.lang.ref.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.transparent.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * The <tt>CallManager</tt> is the one that handles calls. It contains also
 * the "Call" and "Hang up" buttons panel. Here are handles incoming and
 * outgoing calls from and to the call operation set.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public class CallManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallManager</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(CallManager.class);

    /**
     * The name of the property which indicates whether the user should be
     * warned when starting a desktop sharing session.
     */
    private static final String desktopSharingWarningProperty
        = "net.java.sip.communicator.impl.gui.main"
            + ".call.SHOW_DESKTOP_SHARING_WARNING";

    /**
     * The name of the property which indicates whether the preferred provider
     * will be used when calling UIContact (call history).
     */
    private static final String IGNORE_PREFERRED_PROVIDER_PROP
        = "net.java.sip.communicator.impl.gui.main"
            + ".call.IGNORE_PREFERRED_PROVIDER_PROP";

    /**
     * The <tt>CallPanel</tt>s opened by <tt>CallManager</tt> (because
     * <tt>CallContainer</tt> does not give access to such lists.)
     */
    private static final Map<CallConference, CallPanel> callPanels
        = new HashMap<CallConference, CallPanel>();

    /**
     * A map of active outgoing calls per <tt>UIContactImpl</tt>.
     */
    private static Map<Call, UIContactImpl> uiContactCalls;

    /**
     * The group of notifications dedicated to missed calls.
     */
    private static UINotificationGroup missedCallGroup;

    /**
     * A <tt>CallListener</tt>.
     */
    public static class GuiCallListener
        extends SwingCallListener
    {
        /**
         * Maps for incoming call handlers. The handlers needs to be created
         * in the protocol thread while their method
         * incomingCallReceivedInEventDispatchThread will be called on EDT.
         * On the protocol thread a call state changed listener is added,
         * if this is done on the EDT there is a almost no gap between incoming
         * CallEvent and call state changed when doing auto answer and we
         * end up with call answered and dialog for incoming call.
         */
        private Map<CallEvent,WeakReference<IncomingCallHandler>>
            inCallHandlers = Collections.synchronizedMap(
                new WeakHashMap<CallEvent,
                                WeakReference<IncomingCallHandler>>());

        /**
         * Delivers the <tt>CallEvent</tt> in the protocol thread.
         */
        public void incomingCallReceived(CallEvent ev)
        {
            inCallHandlers.put(
                ev,
                new WeakReference<IncomingCallHandler>(
                        new IncomingCallHandler(ev.getSourceCall())));

            super.incomingCallReceived(ev);
        }

        /**
         * Implements {@link CallListener#incomingCallReceived(CallEvent)}. When
         * a call is received, creates a <tt>ReceivedCallDialog</tt> and plays
         * the ring phone sound to the user.
         *
         * @param ev the <tt>CallEvent</tt>
         */
        @Override
        public void incomingCallReceivedInEventDispatchThread(CallEvent ev)
        {
            WeakReference<IncomingCallHandler> ihRef
                = inCallHandlers.remove(ev);

            if(ihRef != null)
            {
                ihRef.get().incomingCallReceivedInEventDispatchThread(ev);
            }
        }

        /**
         * Implements CallListener.callEnded. Stops sounds that are playing at
         * the moment if there're any. Removes the <tt>CallPanel</tt> and
         * disables the hang-up button.
         *
         * @param ev the <tt>CallEvent</tt> which specifies the <tt>Call</tt>
         * that has ended
         */
        @Override
        public void callEndedInEventDispatchThread(CallEvent ev)
        {
            CallConference callConference = ev.getCallConference();

            closeCallContainerIfNotNecessary(callConference);

            /*
             * Notify the existing CallPanels about the CallEvent (in case
             * they need to update their UI, for example).
             */
            forwardCallEventToCallPanels(ev);

            // If we're currently in the call history view, refresh
            // it.
            TreeContactList contactList
                = GuiActivator.getContactList();

            if (contactList.getCurrentFilter().equals(
                    TreeContactList.historyFilter))
            {
                contactList.applyFilter(
                        TreeContactList.historyFilter);
            }
        }

        /**
         * Creates and opens a call dialog. Implements
         * {@link CallListener#outgoingCallCreated(CallEvent)}.
         *
         * @param ev the <tt>CallEvent</tt>
         */
        @Override
        public void outgoingCallCreatedInEventDispatchThread(CallEvent ev)
        {
            Call sourceCall = ev.getSourceCall();

            openCallContainerIfNecessary(sourceCall);

            /*
             * Notify the existing CallPanels about the CallEvent (in case they
             * need to update their UI, for example).
             */
            forwardCallEventToCallPanels(ev);
        }
    }

    /**
     * Handles incoming calls. Must be created on the protocol thread while the
     * method incomingCallReceivedInEventDispatchThread is executed on the EDT.
     */
    private static class IncomingCallHandler
        extends CallChangeAdapter
    {
        /**
         * The dialog shown
         */
        private ReceivedCallDialog receivedCallDialog;

        /**
         * Peer name.
         */
        private String peerName;

        /**
         * The time of the incoming call.
         */
        private long callTime;

        /**
         * Construct
         * @param sourceCall
         */
        IncomingCallHandler(Call sourceCall)
        {
            Iterator<? extends CallPeer> peerIter = sourceCall.getCallPeers();

            if(!peerIter.hasNext())
            {
                return;
            }

            peerName = peerIter.next().getDisplayName();
            callTime = System.currentTimeMillis();

            sourceCall.addCallChangeListener(this);
        }

        /**
         * State has changed.
         * @param ev
         */
        @Override
        public void callStateChanged(final CallChangeEvent ev)
        {
            if(!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                callStateChanged(ev);
                            }
                        });
                return;
            }
            if (!CallChangeEvent.CALL_STATE_CHANGE
                    .equals(ev.getPropertyName()))
                return;

            // When the call state changes, we ensure here that the
            // received call notification dialog is closed.
            if (receivedCallDialog != null && receivedCallDialog.isVisible())
                receivedCallDialog.setVisible(false);

            // Ensure that the CallDialog is created, because it is the
            // one that listens for CallPeers.
            Object newValue = ev.getNewValue();
            Call call = ev.getSourceCall();

            if (CallState.CALL_INITIALIZATION.equals(newValue)
                    || CallState.CALL_IN_PROGRESS.equals(newValue))
            {
                openCallContainerIfNecessary(call);
            }
            else if (CallState.CALL_ENDED.equals(newValue))
            {
                if (ev.getOldValue().equals(
                        CallState.CALL_INITIALIZATION))
                {
                    // If the call was answered elsewhere, don't mark it
                    // as missed.
                    CallPeerChangeEvent cause = ev.getCause();

                    if ((cause == null)
                            || (cause.getReasonCode()
                                    != CallPeerChangeEvent
                                            .NORMAL_CALL_CLEARING))
                    {
                        addMissedCallNotification(peerName, callTime);
                    }
                }

                call.removeCallChangeListener(this);
            }
        }

        /**
         * Executed on EDT cause will create dialog and will show it.
         * @param ev
         */
        public void incomingCallReceivedInEventDispatchThread(CallEvent ev)
        {
            Call sourceCall = ev.getSourceCall();
            boolean isVideoCall
                = ev.isVideoCall()
                    && ConfigurationUtils.hasEnabledVideoFormat(
                            sourceCall.getProtocolProvider());
            receivedCallDialog = new ReceivedCallDialog(
                sourceCall,
                isVideoCall,
                (CallManager.getInProgressCalls().size() > 0),
                ev.isDesktopStreaming());

            receivedCallDialog.setVisible(true);

            Iterator<? extends CallPeer> peerIter = sourceCall.getCallPeers();

            if(!peerIter.hasNext())
            {
                if (receivedCallDialog.isVisible())
                    receivedCallDialog.setVisible(false);
                return;
            }

            /*
             * Notify the existing CallPanels about the CallEvent (in case they
             * need to update their UI, for example).
             */
            forwardCallEventToCallPanels(ev);
        }
    }

    /**
     * Answers the given call.
     *
     * @param call the call to answer
     */
    public static void answerCall(Call call)
    {
        answerCall(call, null, false /* without video */);
    }

    /**
     * Answers a specific <tt>Call</tt> with or without video and, optionally,
     * does that in a telephony conference with an existing <tt>Call</tt>.
     *
     * @param call
     * @param existingCall
     * @param video
     */
    private static void answerCall(Call call, Call existingCall, boolean video)
    {
        if (existingCall == null)
            openCallContainerIfNecessary(call);

        new AnswerCallThread(call, existingCall, video).start();
    }

    /**
     * Answers the given call in an existing call. It will end up with a
     * conference call.
     *
     * @param call the call to answer
     */
    public static void answerCallInFirstExistingCall(Call call)
    {
        // Find the first existing call.
        Iterator<Call> existingCallIter = getInProgressCalls().iterator();
        Call existingCall
            = existingCallIter.hasNext() ? existingCallIter.next() : null;

        answerCall(call, existingCall, false /* without video */);
    }

    /**
     * Merges specific existing <tt>Call</tt>s into a specific telephony
     * conference.
     *
     * @param conference the conference
     * @param calls list of calls
     */
    public static void mergeExistingCalls(
            CallConference conference,
            Collection<Call> calls)
    {
        new MergeExistingCalls(conference, calls).start();
    }

    /**
     * Answers the given call with video.
     *
     * @param call the call to answer
     */
    public static void answerVideoCall(Call call)
    {
        answerCall(call, null, true /* with video */);
    }

    /**
     * Hang ups the given call.
     *
     * @param call the call to hang up
     */
    public static void hangupCall(Call call)
    {
        new HangupCallThread(call).start();
    }

    /**
     * Hang ups the given <tt>callPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> to hang up
     */
    public static void hangupCallPeer(CallPeer peer)
    {
        new HangupCallThread(peer).start();
    }

    /**
     * Asynchronously hangs up the <tt>Call</tt>s participating in a specific
     * <tt>CallConference</tt>.
     *
     * @param conference the <tt>CallConference</tt> whose participating
     * <tt>Call</tt>s are to be hanged up
     */
    public static void hangupCalls(CallConference conference)
    {
        new HangupCallThread(conference).start();
    }

    /**
     * Creates a call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     */
    public static void createCall(  ProtocolProviderService protocolProvider,
                                    String contact)
    {
        new CreateCallThread(protocolProvider, contact, false /* audio-only */)
            .start();
    }

    /**
     * Creates a call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     * @param uiContact the meta contact we're calling
     */
    public static void createCall(  ProtocolProviderService protocolProvider,
                                    String contact,
                                    UIContactImpl uiContact)
    {
        new CreateCallThread(protocolProvider, null, null, uiContact,
            contact, null, null, false /* audio-only */).start();
    }

    /**
     * Creates a video call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     */
    public static void createVideoCall(ProtocolProviderService protocolProvider,
                                        String contact)
    {
        new CreateCallThread(protocolProvider, contact, true /* video */)
            .start();
    }

    /**
     * Creates a video call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     * @param uiContact the <tt>UIContactImpl</tt> we're calling
     */
    public static void createVideoCall( ProtocolProviderService protocolProvider,
                                        String contact,
                                        UIContactImpl uiContact)
    {
        new CreateCallThread(protocolProvider, null, null, uiContact,
            contact, null, null, true /* video */).start();
    }

    /**
     * Enables/disables local video for a specific <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> to enable/disable to local video for
     * @param enable <tt>true</tt> to enable the local video; otherwise,
     * <tt>false</tt>
     */
    public static void enableLocalVideo(Call call, boolean enable)
    {
        new EnableLocalVideoThread(call, enable).start();
    }

    /**
     * Indicates if the desktop sharing is currently enabled for the given
     * <tt>call</tt>.
     *
     * @param call the <tt>Call</tt>, for which we would to check if the desktop
     * sharing is currently enabled
     * @return <tt>true</tt> if the desktop sharing is currently enabled for the
     * given <tt>call</tt>, <tt>false</tt> otherwise
     */
    public static boolean isLocalVideoEnabled(Call call)
    {
        OperationSetVideoTelephony telephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetVideoTelephony.class);

        return (telephony != null) && telephony.isLocalVideoAllowed(call);
    }

    /**
     * Creates a desktop sharing call to the contact represented by the given
     * string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     * @param uiContact the <tt>UIContactImpl</tt> we're calling
     */
    private static void createDesktopSharing(
            ProtocolProviderService protocolProvider,
            String contact,
            UIContactImpl uiContact)
    {
        // If the user presses cancel on the desktop sharing warning then we
        // have nothing more to do here.
        if (!showDesktopSharingWarning())
            return;

        MediaService mediaService = GuiActivator.getMediaService();
        List<MediaDevice> desktopDevices
            = mediaService.getDevices(MediaType.VIDEO, MediaUseCase.DESKTOP);
        int deviceNumber = desktopDevices.size();

        if (deviceNumber == 1)
        {
            createDesktopSharing(
                    protocolProvider,
                    contact,
                    uiContact,
                    desktopDevices.get(0),
                    true);
        }
        else if (deviceNumber > 1)
        {
            SelectScreenDialog selectDialog
                = new SelectScreenDialog(desktopDevices);

            selectDialog.setVisible(true);
            if (selectDialog.getSelectedDevice() != null)
                createDesktopSharing(
                        protocolProvider,
                        contact,
                        uiContact,
                        selectDialog.getSelectedDevice(),
                        true);
        }
    }

    /**
     * Creates a region desktop sharing through the given
     * <tt>protocolProvider</tt> with the given <tt>contact</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, through
     * which the sharing session will be established
     * @param contact the address of the contact recipient
     * @param uiContact the <tt>UIContactImpl</tt> we're calling
     */
    private static void createRegionDesktopSharing(
                                    ProtocolProviderService protocolProvider,
                                    String contact,
                                    UIContactImpl uiContact)
    {
        if (showDesktopSharingWarning())
        {
            TransparentFrame frame = DesktopSharingFrame
                .createTransparentFrame(
                    protocolProvider, contact, uiContact, true);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }

    /**
     * Creates a desktop sharing call to the contact represented by the given
     * string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     * @param uiContact the <tt>MetaContact</tt> we're calling
     * @param x the x coordinate of the shared region
     * @param y the y coordinated of the shared region
     * @param width the width of the shared region
     * @param height the height of the shared region
     */
    public static void createRegionDesktopSharing(
                                    ProtocolProviderService protocolProvider,
                                    String contact,
                                    UIContactImpl uiContact,
                                    int x,
                                    int y,
                                    int width,
                                    int height)
    {
        MediaService mediaService = GuiActivator.getMediaService();

        List<MediaDevice> desktopDevices = mediaService.getDevices(
            MediaType.VIDEO, MediaUseCase.DESKTOP);

        int deviceNumber = desktopDevices.size();

        if (deviceNumber > 0)
        {
            createDesktopSharing(
                    protocolProvider,
                    contact,
                    uiContact,
                    mediaService.getMediaDeviceForPartialDesktopStreaming(
                        width,
                        height,
                        x,
                        y),
                    false);
        }
    }

    /**
     * Creates a desktop sharing call to the contact represented by the given
     * string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     * @param uiContact the <tt>UIContactImpl</tt> we're calling
     * @param mediaDevice the media device corresponding to the screen to share
     * @param fullscreen whether we are sharing the fullscreen
     */
    private static void createDesktopSharing(
                                    ProtocolProviderService protocolProvider,
                                    String contact,
                                    UIContactImpl uiContact,
                                    MediaDevice mediaDevice,
                                    boolean fullscreen)
    {
        new CreateDesktopSharingThread( protocolProvider,
                                        contact,
                                        uiContact,
                                        mediaDevice,
                                        fullscreen).start();
    }

    /**
     * Enables the desktop sharing in an existing <tt>call</tt>.
     *
     * @param call the call for which desktop sharing should be enabled
     * @param enable indicates if the desktop sharing should be enabled or
     * disabled
     */
    public static void enableDesktopSharing(Call call, boolean enable)
    {
        if (!enable)
            enableDesktopSharing(call, null, enable);
        else if (showDesktopSharingWarning())
        {
            MediaService mediaService = GuiActivator.getMediaService();
            List<MediaDevice> desktopDevices
                = mediaService.getDevices(MediaType.VIDEO, MediaUseCase.DESKTOP);
            int deviceNumber = desktopDevices.size();

            new FullScreenShareIndicator(call);

            if (deviceNumber == 1)
                enableDesktopSharing(call, null, enable);
            else if (deviceNumber > 1)
            {
                SelectScreenDialog selectDialog
                    = new SelectScreenDialog(desktopDevices);

                selectDialog.setVisible(true);

                if (selectDialog.getSelectedDevice() != null)
                    enableDesktopSharing(
                        call, selectDialog.getSelectedDevice(), enable);
            }
        }

        // in case we switch to video, disable remote control if it was
        // enabled
        enableDesktopRemoteControl(call.getCallPeers().next(), false);
    }

    /**
     * Enables the region desktop sharing for the given call.
     *
     * @param call the call, for which the region desktop sharing should be
     * enabled
     * @param enable indicates if the desktop sharing should be enabled or
     * disabled
     */
    public static void enableRegionDesktopSharing(Call call, boolean enable)
    {
        if (!enable)
            enableDesktopSharing(call, null, enable);
        else if (showDesktopSharingWarning())
        {
            TransparentFrame frame
                = DesktopSharingFrame.createTransparentFrame(call, true);

            frame.setVisible(true);
        }
    }

    /**
     * Creates a desktop sharing call to the contact represented by the given
     * string.
     *
     * @param call the call for which desktop sharing should be enabled
     * @param x the x coordinate of the shared region
     * @param y the y coordinated of the shared region
     * @param width the width of the shared region
     * @param height the height of the shared region
     */
    public static void enableRegionDesktopSharing(
                                    Call call,
                                    int x,
                                    int y,
                                    int width,
                                    int height)
    {
        // Use the default media device corresponding to the screen to share
        MediaService mediaService = GuiActivator.getMediaService();

        List<MediaDevice> desktopDevices = mediaService.getDevices(
            MediaType.VIDEO, MediaUseCase.DESKTOP);

        int deviceNumber = desktopDevices.size();

        if (deviceNumber > 0)
        {
            boolean succeed = enableDesktopSharing(
                    call,
                    mediaService.getMediaDeviceForPartialDesktopStreaming(
                        width,
                        height,
                        x,
                        y),
                    true);
            // If the region sharing succeed, then display the frame of the
            // current region shared.
            if(succeed)
            {
                TransparentFrame frame
                    = DesktopSharingFrame.createTransparentFrame(call, false);

                frame.setVisible(true);
            }
        }

        // in case we switch to video, disable remote control if it was
        // enabled
        enableDesktopRemoteControl(call.getCallPeers().next(), false);
    }

    /**
     * Enables the desktop sharing in an existing <tt>call</tt>.
     *
     * @param call the call for which desktop sharing should be enabled
     * @param mediaDevice the media device corresponding to the screen to share
     * @param enable indicates if the desktop sharing should be enabled or
     * disabled
     *
     * @return True if the desktop sharing succeed (we are currently sharing the
     * whole or a part of the desktop). False, otherwise.
     */
    private static boolean enableDesktopSharing(Call call,
                                            MediaDevice mediaDevice,
                                            boolean enable)
    {
        OperationSetDesktopStreaming desktopOpSet
            = call.getProtocolProvider().getOperationSet(
                OperationSetDesktopStreaming.class);
        boolean enableSucceeded = false;

        // This shouldn't happen at this stage, because we disable the button
        // if the operation set isn't available.
        if (desktopOpSet != null)
        {
            // First make sure the local video button is disabled.
            if (enable && isLocalVideoEnabled(call))
                getActiveCallContainer(call).setVideoButtonSelected(false);

            try
            {
                if (mediaDevice != null)
                {
                    desktopOpSet.setLocalVideoAllowed(
                            call,
                            mediaDevice,
                            enable);
                }
                else
                    desktopOpSet.setLocalVideoAllowed(call, enable);

                enableSucceeded = true;
            }
            catch (OperationFailedException ex)
            {
                logger.error(
                        "Failed to toggle the streaming of local video.",
                        ex);
            }
        }

        return (enable && enableSucceeded);
    }

    /**
     * Indicates if the desktop sharing is currently enabled for the given
     * <tt>call</tt>.
     *
     * @param call the <tt>Call</tt>, for which we would to check if the desktop
     * sharing is currently enabled
     * @return <tt>true</tt> if the desktop sharing is currently enabled for the
     * given <tt>call</tt>, <tt>false</tt> otherwise
     */
    public static boolean isDesktopSharingEnabled(Call call)
    {
        OperationSetDesktopStreaming desktopOpSet
            = call.getProtocolProvider().getOperationSet(
                OperationSetDesktopStreaming.class);

        if (desktopOpSet != null
            && desktopOpSet.isLocalVideoAllowed(call))
            return true;

        return false;
    }

    /**
     * Indicates if the desktop sharing is currently enabled for the given
     * <tt>call</tt>.
     *
     * @param call the <tt>Call</tt>, for which we would to check if the desktop
     * sharing is currently enabled
     * @return <tt>true</tt> if the desktop sharing is currently enabled for the
     * given <tt>call</tt>, <tt>false</tt> otherwise
     */
    public static boolean isRegionDesktopSharingEnabled(Call call)
    {
        OperationSetDesktopStreaming desktopOpSet
            = call.getProtocolProvider().getOperationSet(
                OperationSetDesktopStreaming.class);

        if (desktopOpSet != null
            && desktopOpSet.isPartialStreaming(call))
            return true;

        return false;
    }

    /**
     * Enables/disables remote control when in a desktop sharing session with
     * the given <tt>callPeer</tt>.
     *
     * @param callPeer the call peer for which we enable/disable remote control
     * @param isEnable indicates if the remote control should be enabled
     */
    public static void enableDesktopRemoteControl(  CallPeer callPeer,
                                                    boolean isEnable)
    {
        OperationSetDesktopSharingServer sharingOpSet
            = callPeer.getProtocolProvider().getOperationSet(
                OperationSetDesktopSharingServer.class);

        if (sharingOpSet == null)
            return;

        if (isEnable)
            sharingOpSet.enableRemoteControl(callPeer);
        else
            sharingOpSet.disableRemoteControl(callPeer);
    }

    /**
     * Creates a call to the given call string. The given component indicates
     * where should be shown the "call via" menu if needed.
     *
     * @param callString the string to call
     * @param c the component, which indicates where should be shown the "call
     * via" menu if needed
     */
    public static void createCall(  String callString,
                                    JComponent c)
    {
        createCall(callString, c, null);
    }

    /**
     * Creates a call to the given call string. The given component indicates
     * where should be shown the "call via" menu if needed.
     *
     * @param callString the string to call
     * @param c the component, which indicates where should be shown the "call
     * via" menu if needed
     * @param l listener that is notified when the call interface has been
     * started after call was created
     */
    public static void createCall(  String callString,
                                    JComponent c,
                                    CallInterfaceListener l)
    {
        callString = callString.trim();

        // Removes special characters from phone numbers.
        if (ConfigurationUtils.isNormalizePhoneNumber()
            && !NetworkUtils.isValidIPAddress(callString))
        {
            callString = GuiActivator.getPhoneNumberI18nService()
                .normalize(callString);
        }

        List<ProtocolProviderService> telephonyProviders
            = CallManager.getTelephonyProviders();

        if (telephonyProviders.size() == 1)
        {
            CallManager.createCall(
                telephonyProviders.get(0), callString);

            if (l != null)
                l.callInterfaceStarted();
        }
        else if (telephonyProviders.size() > 1)
        {
            /*
             * Allow plugins which do not have a (Jitsi) UI to create calls by
             * automagically picking up a telephony provider.
             */
            if (c == null)
            {
                ProtocolProviderService preferredTelephonyProvider = null;

                for (ProtocolProviderService telephonyProvider
                        : telephonyProviders)
                {
                    try
                    {
                        OperationSetPresence presenceOpSet
                            = telephonyProvider.getOperationSet(
                                    OperationSetPresence.class);

                        if ((presenceOpSet != null)
                                && (presenceOpSet.findContactByID(callString)
                                        != null))
                        {
                            preferredTelephonyProvider = telephonyProvider;
                            break;
                        }
                    }
                    catch (Throwable t)
                    {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
                if (preferredTelephonyProvider == null)
                    preferredTelephonyProvider = telephonyProviders.get(0);

                CallManager.createCall(preferredTelephonyProvider, callString);
                if (l != null)
                    l.callInterfaceStarted();
            }
            else
            {
                ChooseCallAccountPopupMenu chooseAccountDialog
                    = new ChooseCallAccountPopupMenu(
                            c,
                            callString,
                            telephonyProviders,
                            l);

                chooseAccountDialog.setLocation(c.getLocation());
                chooseAccountDialog.showPopupMenu();
            }
        }
        else
        {
            ResourceManagementService resources = GuiActivator.getResources();

            new ErrorDialog(
                    null,
                    resources.getI18NString("service.gui.WARNING"),
                    resources.getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                .showDialog();
        }
    }

    /**
     * Creates a call to the given list of contacts.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param callees the list of contacts to call to
     */
    public static void createConferenceCall(
            String[] callees,
            ProtocolProviderService protocolProvider)
    {
        Map<ProtocolProviderService, List<String>> crossProtocolCallees
            = new HashMap<ProtocolProviderService, List<String>>();

        crossProtocolCallees.put(protocolProvider, Arrays.asList(callees));
        createConferenceCall(crossProtocolCallees);
    }

    /**
     * Invites the given list of <tt>callees</tt> to the given conference
     * <tt>call</tt>.
     *
     * @param callees the list of contacts to invite
     * @param call the protocol provider to which this call belongs
     */
    public static void inviteToConferenceCall(String[] callees, Call call)
    {
        Map<ProtocolProviderService, List<String>> crossProtocolCallees
            = new HashMap<ProtocolProviderService, List<String>>();

        crossProtocolCallees.put(
                call.getProtocolProvider(),
                Arrays.asList(callees));
        inviteToConferenceCall(crossProtocolCallees, call);
    }

    /**
     * Invites the given list of <tt>callees</tt> to the given conference
     * <tt>call</tt>.
     *
     * @param callees the list of contacts to invite
     * @param call existing call
     */
    public static void inviteToConferenceCall(
            Map<ProtocolProviderService, List<String>> callees,
            Call call)
    {
        new InviteToConferenceCallThread(callees, call).start();
    }

    /**
     * Invites specific <tt>callees</tt> to a specific telephony conference.
     *
     * @param callees the list of contacts to invite
     * @param conference the telephony conference to invite the specified
     * <tt>callees</tt> into
     */
    public static void inviteToConferenceCall(
            Map<ProtocolProviderService, List<String>> callees,
            CallConference conference)
    {
        /*
         * InviteToConferenceCallThread takes a specific Call but actually
         * invites to the telephony conference associated with the specified
         * Call (if any). In order to not change the signature of its
         * constructor at this time, just pick up a Call participating in the
         * specified telephony conference (if any).
         */
        Call call = null;

        if (conference != null)
        {
            List<Call> calls = conference.getCalls();

            if (!calls.isEmpty())
                call = calls.get(0);
        }

        new InviteToConferenceCallThread(callees, call).start();
    }

    /**
     * Asynchronously creates a new conference <tt>Call</tt> with a specific
     * list of participants/callees.
     *
     * @param callees the list of participants/callees to invite to a
     * newly-created conference <tt>Call</tt>
     */
    public static void createConferenceCall(
        Map<ProtocolProviderService, List<String>> callees)
    {
        new InviteToConferenceCallThread(callees, null).start();
    }

    /**
     * Asynchronously creates a new video bridge conference <tt>Call</tt> with
     * a specific list of participants/callees.
     *
     * @param callProvider the <tt>ProtocolProviderService</tt> to use for
     * creating the call
     * @param callees the list of participants/callees to invite to the
     * newly-created video bridge conference <tt>Call</tt>
     */
    public static void createJitsiVideobridgeConfCall(
                                        ProtocolProviderService callProvider,
                                        String[] callees)
    {
        new InviteToConferenceBridgeThread(callProvider, callees, null).start();
    }

    /**
     * Invites the given list of <tt>callees</tt> to the given conference
     * <tt>call</tt>.
     *
     * @param callees the list of contacts to invite
     * @param call the protocol provider to which this call belongs
     */
    public static void inviteToJitsiVideobridgeConfCall(String[] callees, Call call)
    {
        new InviteToConferenceBridgeThread( call.getProtocolProvider(),
                                            callees,
                                            call).start();
    }

    /**
     * Puts on or off hold the given <tt>callPeer</tt>.
     * @param callPeer the peer to put on/off hold
     * @param isOnHold indicates the action (on hold or off hold)
     */
    public static void putOnHold(CallPeer callPeer, boolean isOnHold)
    {
        new PutOnHoldCallPeerThread(callPeer, isOnHold).start();
    }

    /**
     * Transfers the given <tt>peer</tt> to the given <tt>target</tt>.
     * @param peer the <tt>CallPeer</tt> to transfer
     * @param target the <tt>CallPeer</tt> target to transfer to
     */
    public static void transferCall(CallPeer peer, CallPeer target)
    {
        OperationSetAdvancedTelephony<?> telephony
            = peer.getCall().getProtocolProvider()
                .getOperationSet(OperationSetAdvancedTelephony.class);

        if (telephony != null)
        {
            try
            {
                telephony.transfer(peer, target);
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to transfer " + peer.getAddress()
                    + " to " + target, ex);
            }
        }
    }

    /**
     * Transfers the given <tt>peer</tt> to the given <tt>target</tt>.
     * @param peer the <tt>CallPeer</tt> to transfer
     * @param target the target of the transfer
     */
    public static void transferCall(CallPeer peer, String target)
    {
        OperationSetAdvancedTelephony<?> telephony
            = peer.getCall().getProtocolProvider()
                .getOperationSet(OperationSetAdvancedTelephony.class);

        if (telephony != null)
        {
            try
            {
                telephony.transfer(peer, target);
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to transfer " + peer.getAddress()
                    + " to " + target, ex);
            }
        }
    }

    /**
     * Closes the <tt>CallPanel</tt> of a specific <tt>Call</tt> if it is no
     * longer necessary (i.e. is not used by other <tt>Call</tt>s participating
     * in the same telephony conference as the specified <tt>Call</tt>.)
     *
     * @param callConference The <tt>CallConference</tt> which is to have its
     * associated <tt>CallPanel</tt>, if any
     */
    private static void closeCallContainerIfNotNecessary(
            final CallConference callConference)
    {
        CallPanel callPanel = callPanels.get(callConference);

        if (callPanel != null)
            closeCallContainerIfNotNecessary(
                callConference, callPanel.isCloseWaitAfterHangup());
    }

    /**
     * Closes the <tt>CallPanel</tt> of a specific <tt>Call</tt> if it is no
     * longer necessary (i.e. is not used by other <tt>Call</tt>s participating
     * in the same telephony conference as the specified <tt>Call</tt>.)
     *
     * @param callConference The <tt>CallConference</tt> which is to have its
     * associated <tt>CallPanel</tt>, if any, closed
     * @param wait <tt>true</tt> to set <tt>delay</tt> param of
     * {@link CallContainer#close(CallPanel, boolean)} (CallPanel)}
     */
    private static void closeCallContainerIfNotNecessary(
            final CallConference callConference,
            final boolean wait)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            closeCallContainerIfNotNecessary(
                                callConference,
                                wait);
                        }
                    });
            return;
        }

        /*
         * XXX The integrity of the execution of the method may be compromised
         * if it is not invoked on the AWT event dispatching thread because
         * findCallPanel and callPanels.remove must be atomically executed. The
         * uninterrupted execution (with respect to the synchronization) is
         * guaranteed by requiring all modifications to callPanels to be made on
         * the AWT event dispatching thread.
         */

        for (Iterator<Map.Entry<CallConference, CallPanel>> entryIter
                    = callPanels.entrySet().iterator();
                entryIter.hasNext();)
        {
            Map.Entry<CallConference, CallPanel> entry = entryIter.next();
            CallConference aConference = entry.getKey();
            boolean notNecessary = aConference.isEnded();

            if (notNecessary)
            {
                CallPanel aCallPanel = entry.getValue();
                CallContainer window = aCallPanel.getCallWindow();

                try
                {
                    window.close(
                            aCallPanel,
                            wait && (aConference == callConference));
                }
                finally
                {
                    /*
                     * We allow non-modifications i.e. reads of callPanels on
                     * threads other than the AWT event dispatching thread so we
                     * have to make sure that we will not cause
                     * ConcurrentModificationException.
                     */
                    synchronized (callPanels)
                    {
                        entryIter.remove();
                    }

                    aCallPanel.dispose();
                }
            }
        }
    }

    /**
     * Opens a <tt>CallPanel</tt> for a specific <tt>Call</tt> if there is none.
     * <p>
     * <b>Note</b>: The method can be called only on the AWT event dispatching
     * thread.
     * </p>
     *
     * @param call the <tt>Call</tt> to open a <tt>CallPanel</tt> for
     * @return the <tt>CallPanel</tt> associated with the <tt>Call</tt>
     * @throws RuntimeException if the method is not called on the AWT event
     * dispatching thread
     */
    private static CallPanel openCallContainerIfNecessary(Call call)
    {
        /*
         * XXX The integrity of the execution of the method may be compromised
         * if it is not invoked on the AWT event dispatching thread because
         * findCallPanel and callPanels.put must be atomically executed. The
         * uninterrupted execution (with respect to the synchronization) is
         * guaranteed by requiring all modifications to callPanels to be made on
         * the AWT event dispatching thread.
         */
        assertIsEventDispatchingThread();

        /*
         * CallPanel displays a CallConference (which may contain multiple
         * Calls.)
         */
        CallConference conference = call.getConference();
        CallPanel callPanel = findCallPanel(conference);

        if (callPanel == null)
        {
            // If we're in single-window mode, the single window is the
            // CallContainer.
            CallContainer callContainer
                = GuiActivator.getUIService().getSingleWindowContainer();

            // If we're in multi-window mode, we create the CallDialog.
            if (callContainer == null)
                callContainer = new CallDialog();

            callPanel = new CallPanel(conference, callContainer);
            callContainer.addCallPanel(callPanel);

            synchronized (callPanels)
            {
                callPanels.put(conference, callPanel);
            }
        }

        return callPanel;
    }

    /**
     * Returns a list of all currently registered telephony providers.
     * @return a list of all currently registered telephony providers
     */
    public static List<ProtocolProviderService> getTelephonyProviders()
    {
        return AccountUtils
            .getRegisteredProviders(OperationSetBasicTelephony.class);
    }

    /**
     * Returns a list of all currently registered telephony providers supporting
     * conferencing.
     *
     * @return a list of all currently registered telephony providers supporting
     * conferencing
     */
    public static List<ProtocolProviderService>
                                            getTelephonyConferencingProviders()
    {
        return AccountUtils
            .getRegisteredProviders(OperationSetTelephonyConferencing.class);
    }

    /**
     * Returns a list of all currently active calls.
     *
     * @return a list of all currently active calls
     */
    private static List<Call> getActiveCalls()
    {
        CallConference[] conferences;

        synchronized (callPanels)
        {
            Set<CallConference> keySet = callPanels.keySet();

            conferences = keySet.toArray(new CallConference[keySet.size()]);
        }

        List<Call> calls = new ArrayList<Call>();

        for (CallConference conference : conferences)
        {
            for (Call call : conference.getCalls())
            {
                if (call.getCallState() == CallState.CALL_IN_PROGRESS)
                    calls.add(call);
            }
        }
        return calls;
    }

    /**
     * Returns a collection of all currently in progress calls. A call is active
     * if it is in progress so the method merely delegates to
     *
     * @return a collection of all currently in progress calls.
     */
    public static Collection<Call> getInProgressCalls()
    {
        return getActiveCalls();
    }

    /**
     * Returns the <tt>CallContainer</tt> corresponding to the given
     * <tt>call</tt>. If the call has been finished and no active
     * <tt>CallContainer</tt> could be found it returns null.
     *
     * @param call the <tt>Call</tt>, which dialog we're looking for
     * @return the <tt>CallContainer</tt> corresponding to the given
     * <tt>call</tt>
     */
    public static CallPanel getActiveCallContainer(Call call)
    {
        return findCallPanel(call.getConference());
    }

    /**
     * A informative text to show for the peer. If display name is missing
     * return the address.
     * @param peer the peer.
     * @param listener the listener to fire change events for later resolutions
     * of display name and image, if exist.
     * @return the text contain display name.
     */
    public static String getPeerDisplayName(CallPeer peer,
                                            DetailsResolveListener listener)
    {
        String displayName = null;

        // We try to find the <tt>UIContact</tt>, to which the call was
        // created if this was an outgoing call.
        UIContactImpl uiContact
            = CallManager.getCallUIContact(peer.getCall());

        if(uiContact != null)
        {
            if(uiContact.getDescriptor() instanceof SourceContact)
            {
                // if it is source contact (history record)
                // search for cusax contact match
                Contact contact = getPeerCusaxContact(peer,
                    (SourceContact)uiContact.getDescriptor());
                if(contact != null)
                    displayName = contact.getDisplayName();
            }

            if(StringUtils.isNullOrEmpty(displayName, true))
                displayName = uiContact.getDisplayName();
        }

        // We search for a contact corresponding to this call peer and
        // try to get its display name.
        if (StringUtils.isNullOrEmpty(displayName, true)
            && peer.getContact() != null)
        {
            displayName = peer.getContact().getDisplayName();
        }

        // We try to find the an alternative peer address.
        if (StringUtils.isNullOrEmpty(displayName, true))
        {
            String imppAddress = peer.getAlternativeIMPPAddress();

            if (!StringUtils.isNullOrEmpty(imppAddress))
            {
                int protocolPartIndex = imppAddress.indexOf(":");

                imppAddress = (protocolPartIndex >= 0)
                        ? imppAddress.substring(protocolPartIndex + 1)
                        : imppAddress;

                        Collection<ProtocolProviderService> cusaxProviders
                        = AccountUtils.getRegisteredProviders(
                            OperationSetCusaxUtils.class);

                if (cusaxProviders != null && cusaxProviders.size() > 0)
                {
                    Iterator<ProtocolProviderService> iter
                        = cusaxProviders.iterator();
                    while(iter.hasNext())
                    {
                        Contact contact = getPeerContact(
                            peer,
                            iter.next(),
                            imppAddress);

                        displayName = (contact != null)
                            ? contact.getDisplayName() : null;

                        if(!StringUtils.isNullOrEmpty(displayName, true))
                            break;
                    }
                }
                else
                {
                    MetaContact metaContact
                        = getPeerMetaContact(peer, imppAddress);

                    displayName = (metaContact != null)
                                    ? metaContact.getDisplayName() : null;
                }
            }
        }

        if (StringUtils.isNullOrEmpty(displayName, true))
        {
            displayName = (!StringUtils.isNullOrEmpty
                            (peer.getDisplayName(), true))
                            ? peer.getDisplayName()
                            : peer.getAddress();

            // Try to resolve the display name
            String resolvedName = queryContactSource(displayName, listener);
            if(resolvedName != null)
            {
                displayName = resolvedName;
            }
        }

        return displayName;
    }

    /**
     * Returns the image corresponding to the given <tt>peer</tt>.
     *
     * @param peer the call peer, for which we're returning an image
     * @return the peer image
     */
    public static byte[] getPeerImage(CallPeer peer)
    {
        byte[] image = null;
        // We search for a contact corresponding to this call peer and
        // try to get its image.
        if (peer.getContact() != null)
        {
            image = getContactImage(peer.getContact());
        }

        // We try to find the <tt>UIContact</tt>, to which the call was
        // created if this was an outgoing call.
        if (image == null || image.length == 0)
        {
            UIContactImpl uiContact
                = CallManager.getCallUIContact(peer.getCall());

            if (uiContact != null)
            {
                if(uiContact.getDescriptor() instanceof SourceContact
                    && ((SourceContact)uiContact.getDescriptor())
                        .isDefaultImage())
                {
                    // if it is source contact (history record)
                    // search for cusax contact match
                    Contact contact = getPeerCusaxContact(peer,
                        (SourceContact)uiContact.getDescriptor());

                    if(contact != null)
                        image = contact.getImage();
                }
                else
                    image = uiContact.getAvatar();
            }
        }

        // We try to find the an alternative peer address.
        if (image == null || image.length == 0)
        {
            String imppAddress = peer.getAlternativeIMPPAddress();

            if (!StringUtils.isNullOrEmpty(imppAddress))
            {
                int protocolPartIndex = imppAddress.indexOf(":");

                imppAddress = (protocolPartIndex >= 0)
                        ? imppAddress.substring(protocolPartIndex + 1)
                        : imppAddress;

                Collection<ProtocolProviderService> cusaxProviders
                    = AccountUtils.getRegisteredProviders(
                        OperationSetCusaxUtils.class);

                if (cusaxProviders != null && cusaxProviders.size() > 0)
                {
                    Iterator<ProtocolProviderService> iter
                        = cusaxProviders.iterator();
                    while(iter.hasNext())
                    {
                        Contact contact = getPeerContact(
                            peer,
                            iter.next(),
                            imppAddress);

                        image = (contact != null) ?
                            getContactImage(contact) : null;

                        if(image != null)
                            break;
                    }
                }
                else
                {
                    MetaContact metaContact
                        = getPeerMetaContact(peer, imppAddress);

                    image = (metaContact != null)
                                ? metaContact.getAvatar() : null;
                }
            }
        }

        // If the icon is still null we try to get an image from the call
        // peer.
        if ((image == null || image.length == 0)
                && peer.getImage() != null)
            image = peer.getImage();

        return image;
    }

    /**
     * Searches the cusax enabled providers for a contact with
     * the detail (address) of the call peer if found and the contact
     * is provided by a provider which is IM capable, return the contact.
     * @param peer the peer we are calling.
     * @return the im capable contact corresponding the <tt>peer</tt>.
     */
    public static Contact getIMCapableCusaxContact(CallPeer peer)
    {
        // We try to find the <tt>UIContact</tt>, to which the call was
        // created if this was an outgoing call.
        UIContactImpl uiContact
            = CallManager.getCallUIContact(peer.getCall());

        if (uiContact != null)
        {
            if(uiContact.getDescriptor() instanceof MetaContact)
            {
                MetaContact metaContact =
                    (MetaContact)uiContact.getDescriptor();
                Iterator<Contact> iter = metaContact.getContacts();
                while(iter.hasNext())
                {
                    Contact contact = iter.next();
                    if(contact.getProtocolProvider()
                        .getOperationSet(
                            OperationSetBasicInstantMessaging.class) != null)
                        return contact;
                }
            }
            else if(uiContact.getDescriptor() instanceof SourceContact)
            {
                // if it is source contact (history record)
                // search for cusax contact match
                Contact contact = getPeerCusaxContact(peer,
                            (SourceContact)uiContact.getDescriptor());
                if(contact != null
                    && contact.getProtocolProvider().getOperationSet(
                            OperationSetBasicInstantMessaging.class) != null)
                    return contact;
            }
        }

        // We try to find the an alternative peer address.
        String imppAddress = peer.getAlternativeIMPPAddress();

        if (!StringUtils.isNullOrEmpty(imppAddress))
        {
            int protocolPartIndex = imppAddress.indexOf(":");

            imppAddress = (protocolPartIndex >= 0)
                    ? imppAddress.substring(protocolPartIndex + 1)
                    : imppAddress;

            Collection<ProtocolProviderService> cusaxProviders
                = AccountUtils.getRegisteredProviders(
                    OperationSetCusaxUtils.class);

            if (cusaxProviders != null && cusaxProviders.size() > 0)
            {
                Iterator<ProtocolProviderService> iter
                    = cusaxProviders.iterator();
                while(iter.hasNext())
                {
                    ProtocolProviderService cusaxProvider = iter.next();

                    Contact contact = getPeerContact(
                        peer,
                        cusaxProvider,
                        imppAddress);

                    if(contact != null
                        && cusaxProvider.getOperationSet(
                        OperationSetBasicInstantMessaging.class) != null)
                    {
                        return contact;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find is there a linked cusax protocol provider for this source contact,
     * if it exist we try to resolve current peer to one of its contacts
     * or details of a contact (numbers).
     * @param peer the peer to check
     * @param sourceContact the currently selected source contact.
     * @return matching cusax contact.
     */
    private static Contact getPeerCusaxContact(
        CallPeer peer, SourceContact sourceContact)
    {
        ProtocolProviderService linkedCusaxProvider = null;
        for(ContactDetail detail : sourceContact.getContactDetails())
        {
            ProtocolProviderService pps
                = detail.getPreferredProtocolProvider(
                OperationSetBasicTelephony.class);

            if(pps != null)
            {
                OperationSetCusaxUtils cusaxOpSet =
                    pps.getOperationSet(OperationSetCusaxUtils.class);

                if(cusaxOpSet != null)
                {
                    linkedCusaxProvider
                        = cusaxOpSet.getLinkedCusaxProvider();
                    break;
                }
            }
        }

        // if we do not have preferred protocol, lets check the one
        // used to dial the peer
        if(linkedCusaxProvider == null)
        {
            ProtocolProviderService pps = peer.getProtocolProvider();

            OperationSetCusaxUtils cusaxOpSet =
                pps.getOperationSet(OperationSetCusaxUtils.class);

            if(cusaxOpSet != null)
            {
                linkedCusaxProvider
                    = cusaxOpSet.getLinkedCusaxProvider();
            }
        }

        if(linkedCusaxProvider != null)
        {
            OperationSetPersistentPresence opSetPersistentPresence
                = linkedCusaxProvider.getOperationSet(
                        OperationSetPersistentPresence.class);

            if(opSetPersistentPresence != null)
            {
                String peerAddress = peer.getAddress();

                // will strip the @server-address part, as the regular expression
                // will match it
                int index = peerAddress.indexOf("@");
                String peerUserID =
                    (index > -1) ? peerAddress.substring(0, index) : peerAddress;

                // searches for the whole number/username or with the @serverpart
                String peerUserIDQ = Pattern.quote(peerUserID);

                Pattern pattern = Pattern.compile(
                    "^(" + peerUserIDQ + "|" + peerUserIDQ + "@.*)$");

                return findContactByPeer(
                    peerUserID,
                    pattern,
                    opSetPersistentPresence.getServerStoredContactListRoot(),
                    linkedCusaxProvider.getOperationSet(
                        OperationSetCusaxUtils.class));
            }
        }

        return null;
    }

    /**
     * Finds a matching cusax contact.
     * @param peerUserID the userID of the call peer to search for
     * @param searchPattern the pattern (userID | userID@...)
     * @param parent the parent group of the groups and contacts to search in
     * @param cusaxOpSet the opset of the provider which will be used to match
     *                   contact's details to peer userID (stored numbers).
     * @return a cusax matching contac
     */
    private static Contact findContactByPeer(
        String peerUserID,
        Pattern searchPattern,
        ContactGroup parent,
        OperationSetCusaxUtils cusaxOpSet)
    {
        Iterator<Contact> contactIterator = parent.contacts();
        while(contactIterator.hasNext())
        {
            Contact contact = contactIterator.next();

            if(searchPattern.matcher(contact.getAddress()).find()
                || cusaxOpSet.doesDetailBelong(contact, peerUserID))
            {
                return contact;
            }
        }

        Iterator<ContactGroup> groupsIterator = parent.subgroups();
        while(groupsIterator.hasNext())
        {
            ContactGroup gr = groupsIterator.next();
            Contact contact = findContactByPeer(
                peerUserID, searchPattern, gr, cusaxOpSet);
            if(contact != null)
                return contact;
        }

        return null;
    }

    /**
     * Returns the image for the given contact.
     *
     * @param contact the <tt>Contact</tt>, which image we're looking for
     * @return the array of bytes representing the image for the given contact
     * or null if such image doesn't exist
     */
    private static byte[] getContactImage(Contact contact)
    {
        MetaContact metaContact = GuiActivator.getContactListService()
            .findMetaContactByContact(contact);

        if (metaContact != null)
            return metaContact.getAvatar();

        return null;
    }

    /**
     * Returns the peer contact for the given <tt>alternativePeerAddress</tt> by
     * checking the if the <tt>callPeer</tt> exists as a detail in the given
     * <tt>cusaxProvider</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> to check in the cusax provider
     * details
     * @param cusaxProvider the linked cusax <tt>ProtocolProviderService</tt>
     * @param alternativePeerAddress the alternative peer address to obtain the
     * image from
     * @return the protocol <tt>Contact</tt> corresponding to the given
     * <tt>alternativePeerAddress</tt>
     */
    private static Contact getPeerContact( CallPeer callPeer,
                                        ProtocolProviderService cusaxProvider,
                                        String alternativePeerAddress)
    {
        OperationSetPresence presenceOpSet
            = cusaxProvider.getOperationSet(OperationSetPresence.class);

        if (presenceOpSet == null)
            return null;

        Contact contact = presenceOpSet.findContactByID(alternativePeerAddress);

        if (contact == null)
            return null;

        OperationSetCusaxUtils cusaxOpSet
            = cusaxProvider.getOperationSet(OperationSetCusaxUtils.class);

        if (cusaxOpSet != null && cusaxOpSet.doesDetailBelong(
                contact, callPeer.getAddress()))
            return contact;

        return null;
    }

    /**
     * Returns the metacontact for the given <tt>CallPeer</tt> by
     * checking the if the <tt>callPeer</tt> contact exists, if not checks the
     * contacts in our contact list that are provided by cusax enabled
     * providers.
     *
     * @param peer the <tt>CallPeer</tt> to check in contact details
     * @return the <tt>MetaContact</tt> corresponding to the given
     * <tt>peer</tt>.
     */
    public static MetaContact getPeerMetaContact(CallPeer peer)
    {
        if(peer == null)
            return null;

        if(peer.getContact() != null)
            return GuiActivator.getContactListService()
                .findMetaContactByContact(peer.getContact());

        // We try to find the <tt>UIContact</tt>, to which the call was
        // created if this was an outgoing call.
        UIContactImpl uiContact
            = CallManager.getCallUIContact(peer.getCall());

        if (uiContact != null)
        {
            if(uiContact.getDescriptor() instanceof MetaContact)
            {
                return (MetaContact)uiContact.getDescriptor();
            }
            else if(uiContact.getDescriptor() instanceof SourceContact)
            {
                // if it is a source contact check for matching cusax contact
                Contact contact = getPeerCusaxContact(peer,
                    (SourceContact)uiContact.getDescriptor());
                if(contact != null)
                    return GuiActivator.getContactListService()
                                .findMetaContactByContact(contact);
            }
        }

        String imppAddress = peer.getAlternativeIMPPAddress();

        if (!StringUtils.isNullOrEmpty(imppAddress))
        {
            int protocolPartIndex = imppAddress.indexOf(":");

            imppAddress = (protocolPartIndex >= 0)
                    ? imppAddress.substring(protocolPartIndex + 1)
                    : imppAddress;

            Collection<ProtocolProviderService> cusaxProviders
                = AccountUtils.getRegisteredProviders(
                    OperationSetCusaxUtils.class);

            if (cusaxProviders != null && cusaxProviders.size() > 0)
            {
                Iterator<ProtocolProviderService> iter
                    = cusaxProviders.iterator();
                while(iter.hasNext())
                {
                    Contact contact = getPeerContact(
                        peer,
                        iter.next(),
                        imppAddress);

                    MetaContact res = GuiActivator.getContactListService()
                        .findMetaContactByContact(contact);

                    if(res != null)
                        return res;
                }
            }
            else
            {
                return getPeerMetaContact(peer, imppAddress);
            }
        }

        return null;
    }

    /**
     * Returns the image for the given <tt>alternativePeerAddress</tt> by
     * checking the if the <tt>callPeer</tt> exists as a detail in one of the
     * contacts in our contact list.
     *
     * @param callPeer the <tt>CallPeer</tt> to check in contact details
     * @param alternativePeerAddress the alternative peer address to obtain the
     * image from
     * @return the <tt>MetaContact</tt> corresponding to the given
     * <tt>alternativePeerAddress</tt>
     */
    private static MetaContact getPeerMetaContact(
                                            CallPeer callPeer,
                                            String alternativePeerAddress)
    {
        Iterator<MetaContact> metaContacts
            = GuiActivator.getContactListService()
                .findAllMetaContactsForAddress(alternativePeerAddress);

        while (metaContacts.hasNext())
        {
            MetaContact metaContact = metaContacts.next();

            UIPhoneUtil phoneUtil
                = UIPhoneUtil.getPhoneUtil(metaContact);

            List<UIContactDetail> additionalNumbers
                = phoneUtil.getAdditionalNumbers();

            if (additionalNumbers == null || additionalNumbers.size() > 0)
                continue;

            Iterator<UIContactDetail> numbersIter
                = additionalNumbers.iterator();
            while (numbersIter.hasNext())
            {
                if (numbersIter.next().getAddress()
                    .equals(callPeer.getAddress()))
                    return metaContact;
            }
        }

        return null;
    }

    /**
     * Opens a call transfer dialog to transfer the given <tt>peer</tt>.
     * @param peer the <tt>CallPeer</tt> to transfer
     */
    public static void openCallTransferDialog(CallPeer peer)
    {
        final TransferCallDialog dialog
            = new TransferCallDialog(peer);

        final Call call = peer.getCall();

        /*
         * Transferring a call works only when the call is in progress
         * so close the dialog (if it's not already closed, of course)
         * once the dialog ends.
         */
        CallChangeListener callChangeListener = new CallChangeAdapter()
        {
            /*
             * Overrides
             * CallChangeAdapter#callStateChanged(CallChangeEvent).
             */
            @Override
            public void callStateChanged(CallChangeEvent evt)
            {
                // we are interested only in CALL_STATE_CHANGEs
                if(!evt.getEventType().equals(
                        CallChangeEvent.CALL_STATE_CHANGE))
                    return;

                if (!CallState.CALL_IN_PROGRESS.equals(call
                    .getCallState()))
                {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }
        };
        call.addCallChangeListener(callChangeListener);
        try
        {
            dialog.pack();
            dialog.setVisible(true);
        }
        finally
        {
            call.removeCallChangeListener(callChangeListener);
        }
    }

    /**
     * Checks whether the <tt>callPeer</tt> supports setting video
     * quality presets. If quality controls is null, its not supported.
     * @param callPeer the peer, which video quality we're checking
     * @return whether call peer supports setting quality preset.
     */
    public static boolean isVideoQualityPresetSupported(CallPeer callPeer)
    {
        ProtocolProviderService provider = callPeer.getProtocolProvider();
        OperationSetVideoTelephony videoOpSet
            = provider.getOperationSet(OperationSetVideoTelephony.class);

        if (videoOpSet == null)
            return false;

        return videoOpSet.getQualityControl(callPeer) != null;
    }

    /**
     * Sets the given quality preset for the video of the given call peer.
     *
     * @param callPeer the peer, which video quality we're setting
     * @param qualityPreset the new quality settings
     */
    public static void setVideoQualityPreset(final CallPeer callPeer,
                                            final QualityPreset qualityPreset)
    {
        ProtocolProviderService provider = callPeer.getProtocolProvider();
        final OperationSetVideoTelephony videoOpSet
            = provider.getOperationSet(OperationSetVideoTelephony.class);

        if (videoOpSet == null)
            return;

        final QualityControl qualityControl =
                    videoOpSet.getQualityControl(callPeer);

        if (qualityControl != null)
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        qualityControl.setPreferredRemoteSendMaxPreset(
                                qualityPreset);
                    }
                    catch(org.jitsi.service.protocol.OperationFailedException e)
                    {
                        logger.info("Unable to change video quality.", e);

                        ResourceManagementService resources
                            = GuiActivator.getResources();

                        new ErrorDialog(
                                null,
                                resources.getI18NString("service.gui.WARNING"),
                                resources.getI18NString(
                                        "service.gui.UNABLE_TO_CHANGE_VIDEO_QUALITY"),
                                e)
                            .showDialog();
                    }
                }
            }).start();
        }
    }

    /**
     * Indicates if we have video streams to show in this interface.
     *
     * @param call the call to check for video streaming
     * @return <tt>true</tt> if we have video streams to show in this interface;
     * otherwise, <tt>false</tt>
     */
    public static boolean isVideoStreaming(Call call)
    {
        return isVideoStreaming(call.getConference());
    }

    /**
     * Indicates if we have video streams to show in this interface.
     *
     * @param conference the conference we check for video streaming
     * @return <tt>true</tt> if we have video streams to show in this interface;
     * otherwise, <tt>false</tt>
     */
    public static boolean isVideoStreaming(CallConference conference)
    {
        for (Call call : conference.getCalls())
        {
            OperationSetVideoTelephony videoTelephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetVideoTelephony.class);

            if (videoTelephony == null)
                continue;

            if (videoTelephony.isLocalVideoStreaming(call))
                return true;

            Iterator<? extends CallPeer> callPeers = call.getCallPeers();

            while (callPeers.hasNext())
            {
                List<Component> remoteVideos
                    = videoTelephony.getVisualComponents(callPeers.next());

                if ((remoteVideos != null) && (remoteVideos.size() > 0))
                    return true;
            }
        }
        return false;
    }

    /**
     * Determines whether two specific addresses refer to one and the same
     * peer/resource/contact.
     * <p>
     * <b>Warning</b>: Use the functionality sparingly because it assumes that
     * an unspecified service is equal to any service.
     * </p>
     *
     * @param a one of the addresses to be compared
     * @param b the other address to be compared to <tt>a</tt>
     * @return <tt>true</tt> if <tt>a</tt> and <tt>b</tt> name one and the same
     * peer/resource/contact; <tt>false</tt>, otherwise
     */
    public static boolean addressesAreEqual(String a, String b)
    {
        if (a.equals(b))
            return true;

        int aProtocolIndex = a.indexOf(':');
        if(aProtocolIndex != -1)
            a = a.substring(aProtocolIndex + 1);

        int bProtocolIndex = b.indexOf(':');
        if(bProtocolIndex != -1)
            b = b.substring(bProtocolIndex + 1);

        if (a.equals(b))
            return true;

        int aServiceBegin = a.indexOf('@', aProtocolIndex);
        String aUserID;
        String aService;

        if (aServiceBegin != -1)
        {
            aUserID = a.substring(0, aServiceBegin);
            ++aServiceBegin;

            int aResourceBegin = a.indexOf('/', aServiceBegin);
            if (aResourceBegin != -1)
                aService = a.substring(aServiceBegin, aResourceBegin);
            else
                aService = a.substring(aServiceBegin);
        }
        else
        {
            aUserID = a;
            aService = null;
        }

        int bServiceBegin = b.indexOf('@', bProtocolIndex);
        String bUserID;
        String bService;

        if (bServiceBegin != -1)
        {
            bUserID = b.substring(0, bServiceBegin);
            ++bServiceBegin;

            int bResourceBegin = b.indexOf('/', bServiceBegin);
            if (bResourceBegin != -1)
                bService = b.substring(bServiceBegin, bResourceBegin);
            else
                bService = b.substring(bServiceBegin);
        }
        else
        {
            bUserID = b;
            bService = null;
        }

        boolean userIDsAreEqual;

        if ((aUserID == null) || (aUserID.length() < 1))
            userIDsAreEqual = ((bUserID == null) || (bUserID.length() < 1));
        else
            userIDsAreEqual = aUserID.equals(bUserID);
        if (!userIDsAreEqual)
            return false;

        boolean servicesAreEqual;

        /*
         * It's probably a veeery long shot but it's assumed here that an
         * unspecified service is equal to any service. Such a case is, for
         * example, RegistrarLess SIP.
         */
        if (((aService == null) || (aService.length() < 1))
                || ((bService == null) || (bService.length() < 1)))
            servicesAreEqual = true;
        else
            servicesAreEqual = aService.equals(bService);

        return servicesAreEqual;
    }

    /**
     * Indicates if the given <tt>ConferenceMember</tt> corresponds to the local
     * user.
     *
     * @param conferenceMember the conference member to check
     * @return <tt>true</tt> if the given <tt>conferenceMember</tt> is the local
     * user, <tt>false</tt> - otherwise
     */
    public static boolean isLocalUser(ConferenceMember conferenceMember)
    {
        String localUserAddress
            = conferenceMember.getConferenceFocusCallPeer()
                .getProtocolProvider().getAccountID().getAccountAddress();

        return CallManager.addressesAreEqual(
            conferenceMember.getAddress(), localUserAddress);
    }

    /**
     * Adds a missed call notification.
     *
     * @param peerName the name of the peer
     * @param callTime the time of the call
     */
    private static void addMissedCallNotification(String peerName, long callTime)
    {
        if (missedCallGroup == null)
        {
            missedCallGroup
                = new UINotificationGroup(
                        "MissedCalls",
                        GuiActivator.getResources().getI18NString(
                                "service.gui.MISSED_CALLS_TOOL_TIP"));
        }

        UINotificationManager.addNotification(
                new UINotification(peerName, callTime, missedCallGroup));
    }

    /**
     * Returns of supported/enabled list of audio formats for a provider.
     * @param device the <tt>MediaDevice</tt>, which audio formats we're
     * looking for
     * @param protocolProvider the provider to check.
     * @return list of supported/enabled auido formats or empty list
     * otherwise.
     */
    private static List<MediaFormat> getAudioFormats(
        MediaDevice device,
        ProtocolProviderService protocolProvider)
    {
        List<MediaFormat> res = new ArrayList<MediaFormat>();

        Map<String, String> accountProperties
           = protocolProvider.getAccountID().getAccountProperties();
        String overrideEncodings
           = accountProperties.get(ProtocolProviderFactory.OVERRIDE_ENCODINGS);

        List<MediaFormat> formats;
        if(Boolean.parseBoolean(overrideEncodings))
        {
           /*
            * The account properties associated with account
             * override the global EncodingConfiguration.
            */
           EncodingConfiguration encodingConfiguration
               = ProtocolMediaActivator.getMediaService()
                       .createEmptyEncodingConfiguration();

           encodingConfiguration.loadProperties(
                   accountProperties,
                   ProtocolProviderFactory.ENCODING_PROP_PREFIX);

            formats = device.getSupportedFormats(
                       null, null, encodingConfiguration);
        }
        else /* The global EncodingConfiguration is in effect. */
        {
            formats = device.getSupportedFormats();
        }

        // skip the special telephony event
        for(MediaFormat format : formats)
        {
            if(!format.getEncoding().equals(Constants.TELEPHONE_EVENT))
                res.add(format);
        }

        return res;
    }

    /**
     * Creates a new (audio-only or video) <tt>Call</tt> to a contact specified
     * as a <tt>Contact</tt> instance or a <tt>String</tt> contact
     * address/identifier.
     */
    private static class CreateCallThread
        extends Thread
    {
        /**
         * The contact to call.
         */
        private final Contact contact;

        /**
         * The specific contact resource to call.
         */
        private final ContactResource contactResource;

        /**
         * The <tt>UIContactImpl</tt> we're calling.
         */
        private final UIContactImpl uiContact;

        /**
         * The protocol provider through which the call goes.
         */
        private final ProtocolProviderService protocolProvider;

        /**
         * The string to call.
         */
        private final String stringContact;

        /**
         * The description of a conference to call, if any.
         */
        private final ConferenceDescription conferenceDescription;

        /**
         * The indicator which determines whether this instance is to create a
         * new video (as opposed to audio-only) <tt>Call</tt>.
         */
        private final boolean video;

        /**
         * The chat room associated with the call.
         */
        private final ChatRoom chatRoom;
        
        /**
         * Creates an instance of <tt>CreateCallThread</tt>.
         *
         * @param protocolProvider the protocol provider through which the call
         * is going.
         * @param contact the contact to call
         * @param contactResource the specific <tt>ContactResource</tt> we're
         * calling
         * @param video indicates if this is a video call
         */
        public CreateCallThread(
                ProtocolProviderService protocolProvider,
                Contact contact,
                ContactResource contactResource,
                boolean video)
        {
            this(protocolProvider, contact, contactResource, null, null, null,
                null, video);
        }

        /**
         * Creates an instance of <tt>CreateCallThread</tt>.
         *
         * @param protocolProvider the protocol provider through which the call
         * is going.
         * @param contact the contact to call
         * @param video indicates if this is a video call
         */
        public CreateCallThread(
                ProtocolProviderService protocolProvider,
                String contact,
                boolean video)
        {
            this(protocolProvider, null, null, null, contact, null, null, video);
        }

        /**
         * Initializes a new <tt>CreateCallThread</tt> instance which is to
         * create a new <tt>Call</tt> to a conference specified via a
         * <tt>ConferenceDescription</tt>.
         * @param protocolProvider the <tt>ProtocolProviderService</tt> which is
         * to perform the establishment of the new <tt>Call</tt>.
         * @param conferenceDescription the description of the conference to
         * call.
         * @param chatRoom the chat room associated with the call.
         */
        public CreateCallThread(
                ProtocolProviderService protocolProvider,
                ConferenceDescription conferenceDescription,
                ChatRoom chatRoom)
        {
            this(protocolProvider, null, null, null, null,
                    conferenceDescription, chatRoom,
                    false /* video */);
        }

        /**
         * Initializes a new <tt>CreateCallThread</tt> instance which is to
         * create a new <tt>Call</tt> to a contact specified either as a
         * <tt>Contact</tt> instance or as a <tt>String</tt> contact
         * address/identifier.
         * <p>
         * The constructor is private because it relies on its arguments being
         * validated prior to its invocation.
         * </p>
         *
         * @param protocolProvider the <tt>ProtocolProviderService</tt> which is
         * to perform the establishment of the new <tt>Call</tt>
         * @param contact the contact to call
         * @param contactResource the specific contact resource to call
         * @param uiContact the ui contact we're calling
         * @param stringContact the string to call
         * @param video <tt>true</tt> if this instance is to create a new video
         * (as opposed to audio-only) <tt>Call</tt>
         * @param conferenceDescription the description of a conference to call
         * @param chatRoom the chat room associated with the call.
         */
        public CreateCallThread(
                ProtocolProviderService protocolProvider,
                Contact contact,
                ContactResource contactResource,
                UIContactImpl uiContact,
                String stringContact,
                ConferenceDescription conferenceDescription,
                ChatRoom chatRoom,
                boolean video)
        {
            this.protocolProvider = protocolProvider;
            this.contact = contact;
            this.contactResource = contactResource;
            this.uiContact = uiContact;
            this.stringContact = stringContact;
            this.video = video;
            this.conferenceDescription = conferenceDescription;
            this.chatRoom = chatRoom;
        }

        @Override
        public void run()
        {
            if(!video)
            {
                // if it is not video let's check for available audio codecs
                // and available audio devices
                MediaService mediaService = GuiActivator.getMediaService();
                MediaDevice dev = mediaService.getDefaultDevice(
                   MediaType.AUDIO, MediaUseCase.CALL);

                List<MediaFormat> formats
                    = getAudioFormats(dev, protocolProvider);

                String errorMsg = null;

                if(!dev.getDirection().allowsSending())
                    errorMsg = GuiActivator.getResources().getI18NString(
                        "service.gui.CALL_NO_AUDIO_DEVICE");
                else if(formats.isEmpty())
                {
                    errorMsg = GuiActivator.getResources().getI18NString(
                        "service.gui.CALL_NO_AUDIO_CODEC");
                }

                if(errorMsg != null)
                {
                    if(GuiActivator.getUIService()
                        .getPopupDialog().showConfirmPopupDialog(
                            errorMsg + " " +
                                GuiActivator.getResources().getI18NString(
                                    "service.gui.CALL_NO_DEVICE_CODECS_Q"),
                            GuiActivator.getResources().getI18NString(
                                    "service.gui.CALL"),
                                PopupDialog.YES_NO_OPTION,
                                PopupDialog.QUESTION_MESSAGE)
                        == PopupDialog.NO_OPTION)
                    {
                        return;
                    }
                }
            }

            Contact contact = this.contact;
            String stringContact = this.stringContact;

            if (ConfigurationUtils.isNormalizePhoneNumber()
                && !NetworkUtils.isValidIPAddress(stringContact))
            {
                if (contact != null)
                {
                    stringContact = contact.getAddress();
                    contact = null;
                }

                if (stringContact != null)
                {
                    stringContact = GuiActivator.getPhoneNumberI18nService()
                        .normalize(stringContact);
                }
            }

            try
            {
                if (conferenceDescription != null)
                {
                    internalCall(  protocolProvider,
                                   conferenceDescription,
                                   chatRoom);
                }
                else
                {
                    if (video)
                    {
                        internalCallVideo(  protocolProvider,
                                            contact,
                                            uiContact,
                                            stringContact);
                    }
                    else
                    {
                        internalCall(   protocolProvider,
                                        contact,
                                        stringContact,
                                        contactResource,
                                        uiContact);
                    }
                }
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;

                logger.error("The call could not be created: ", t);

                String message = GuiActivator.getResources()
                    .getI18NString("servoce.gui.CREATE_CALL_FAILED");

                if (t.getMessage() != null)
                    message += " " +  t.getMessage();

                new ErrorDialog(
                        null,
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ERROR"),
                        message,
                        t)
                    .showDialog();
            }
        }
    }

    /**
     * Creates a video call through the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> through
     * which to make the call
     * @param contact the <tt>Contact</tt> to call
     * @param uiContact the <tt>UIContactImpl</tt> we're calling
     * @param stringContact the contact string to call
     *
     * @throws OperationFailedException thrown if the call operation fails
     * @throws ParseException thrown if the contact string is malformated
     */
    private static void internalCallVideo(
                                    ProtocolProviderService protocolProvider,
                                    Contact contact,
                                    UIContactImpl uiContact,
                                    String stringContact)
        throws  OperationFailedException,
                ParseException
    {
        OperationSetVideoTelephony telephony
            = protocolProvider.getOperationSet(
                    OperationSetVideoTelephony.class);

        Call createdCall = null;
        if (telephony != null)
        {
            if (contact != null)
            {
                createdCall = telephony.createVideoCall(contact);
            }
            else if (stringContact != null)
                createdCall = telephony.createVideoCall(stringContact);
        }

        if (uiContact != null && createdCall != null)
            addUIContactCall(uiContact, createdCall);
    }

    /**
     * Creates a call through the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> through
     * which to make the call
     * @param contact the <tt>Contact</tt> to call
     * @param stringContact the contact string to call
     * @param contactResource the specific <tt>ContactResource</tt> to call
     * @param uiContact the <tt>UIContactImpl</tt> we're calling
     *
     * @throws OperationFailedException thrown if the call operation fails
     * @throws ParseException thrown if the contact string is malformated
     */
    private static void internalCall(
                                ProtocolProviderService protocolProvider,
                                Contact contact,
                                String stringContact,
                                ContactResource contactResource,
                                UIContactImpl uiContact)
        throws  OperationFailedException,
                ParseException
    {
        OperationSetBasicTelephony<?> telephony
            = protocolProvider.getOperationSet(
                    OperationSetBasicTelephony.class);

        OperationSetResourceAwareTelephony resourceTelephony
            = protocolProvider.getOperationSet(
                    OperationSetResourceAwareTelephony.class);

        Call createdCall = null;

        if (resourceTelephony != null && contactResource != null)
        {
            if (contact != null)
                createdCall
                    = resourceTelephony.createCall(contact, contactResource);
            else if (!StringUtils.isNullOrEmpty(stringContact))
                createdCall = resourceTelephony.createCall(
                    stringContact, contactResource.getResourceName());
        }
        else if (telephony != null)
        {
            if (contact != null)
            {
                createdCall = telephony.createCall(contact);
            }
            else if (!StringUtils.isNullOrEmpty(stringContact))
                createdCall = telephony.createCall(stringContact);
        }

        if (uiContact != null && createdCall != null)
            addUIContactCall(uiContact, createdCall);
    }

    /**
     * Creates a call through the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> through
     * which to make the call
     * @param conferenceDescription the description of the conference to call
     * @param chatRoom the chat room associated with the call.
     */
    private static void internalCall(ProtocolProviderService protocolProvider,
                                     ConferenceDescription conferenceDescription,
                                     ChatRoom chatRoom)
            throws OperationFailedException
    {
        OperationSetBasicTelephony<?> telephony
                = protocolProvider.getOperationSet(
                OperationSetBasicTelephony.class);

        if (telephony != null)
        {
            telephony.createCall(conferenceDescription, chatRoom);
        }
    }

    /**
     * Returns the <tt>MetaContact</tt>, to which the given <tt>Call</tt>
     * was initially created.
     *
     * @param call the <tt>Call</tt>, which corresponding <tt>MetaContact</tt>
     * we're looking for
     * @return the <tt>UIContactImpl</tt>, to which the given <tt>Call</tt>
     * was initially created
     */
    public static UIContactImpl getCallUIContact(Call call)
    {
        if (uiContactCalls != null)
            return uiContactCalls.get(call);
        return null;
    }

    /**
     * Adds a call for a <tt>metaContact</tt>.
     *
     * @param uiContact the <tt>UIContact</tt> corresponding to the call
     * @param call the <tt>Call</tt> corresponding to the <tt>MetaContact</tt>
     */
    private static void addUIContactCall( UIContactImpl uiContact,
                                          Call call)
    {
        if (uiContactCalls == null)
            uiContactCalls = new WeakHashMap<Call, UIContactImpl>();

        uiContactCalls.put(call, uiContact);
    }

    /**
     * Creates a desktop sharing session with the given Contact or a given
     * String.
     */
    private static class CreateDesktopSharingThread
        extends Thread
    {
        /**
         * The string contact to share the desktop with.
         */
        private final String stringContact;

        /**
         * The protocol provider through which we share our desktop.
         */
        private final ProtocolProviderService protocolProvider;

        /**
         * The media device corresponding to the screen we would like to share.
         */
        private final MediaDevice mediaDevice;

        /**
         * The <tt>UIContactImpl</tt> we're calling.
         */
        private final UIContactImpl uiContact;

        /**
         * Whether user has selected sharing full screen or region.
         */
        private boolean fullscreen = false;

        /**
         * Creates a desktop sharing session thread.
         *
         * @param protocolProvider protocol provider through which we share our
         * desktop
         * @param contact the contact to share the desktop with
         * @param uiContact the <tt>UIContact</tt>, which initiated the desktop
         * sharing session
         * @param mediaDevice the media device corresponding to the screen we
         * would like to share
         */
        public CreateDesktopSharingThread(
                                    ProtocolProviderService protocolProvider,
                                    String contact,
                                    UIContactImpl uiContact,
                                    MediaDevice mediaDevice,
                                    boolean fullscreen)
        {
            this.protocolProvider = protocolProvider;
            this.stringContact = contact;
            this.uiContact = uiContact;
            this.mediaDevice = mediaDevice;
            this.fullscreen = fullscreen;
        }

        @Override
        public void run()
        {
            OperationSetDesktopStreaming desktopSharingOpSet
                = protocolProvider.getOperationSet(
                    OperationSetDesktopStreaming.class);

            /*
             * XXX If we are here and we just discover that
             * OperationSetDesktopStreaming is not supported, then we're
             * already in trouble - we've already started a whole new thread
             * just to check that a reference is null.
             */
            if (desktopSharingOpSet == null)
                return;

            Throwable exception = null;

            Call createdCall = null;
            try
            {
                if (mediaDevice != null)
                {
                    createdCall = desktopSharingOpSet.createVideoCall(
                            stringContact,
                            mediaDevice);
                }
                else
                    createdCall
                        = desktopSharingOpSet.createVideoCall(stringContact);
            }
            catch (OperationFailedException e)
            {
                exception = e;
            }
            catch (ParseException e)
            {
                exception = e;
            }
            if (exception != null)
            {
                logger.error("The call could not be created: ", exception);

                new ErrorDialog(
                        null,
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ERROR"),
                        exception.getMessage(),
                        ErrorDialog.ERROR)
                    .showDialog();
            }

            if (uiContact != null && createdCall != null)
                addUIContactCall(uiContact, createdCall);


            if(createdCall != null && fullscreen)
            {
                new FullScreenShareIndicator(createdCall);
            }
        }
    }

    /**
     * Answers to all <tt>CallPeer</tt>s associated with a specific
     * <tt>Call</tt> and, optionally, does that in a telephony conference with
     * an existing <tt>Call</tt>.
     */
    private static class AnswerCallThread
        extends Thread
    {
        /**
         * The <tt>Call</tt> which is to be answered.
         */
        private final Call call;

        /**
         * The existing <tt>Call</tt>, if any, which represents a telephony
         * conference in which {@link #call} is to be answered.
         */
        private final Call existingCall;

        /**
         * The indicator which determines whether this instance is to answer
         * {@link #call} with video.
         */
        private final boolean video;

        public AnswerCallThread(Call call, Call existingCall, boolean video)
        {
            this.call = call;
            this.existingCall = existingCall;
            this.video = video;
        }

        @Override
        public void run()
        {
            if (existingCall != null)
                call.setConference(existingCall.getConference());

            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<? extends CallPeer> peers = call.getCallPeers();

            while (peers.hasNext())
            {
                CallPeer peer = peers.next();

                if (video)
                {
                    OperationSetVideoTelephony telephony
                        = pps.getOperationSet(OperationSetVideoTelephony.class);

                    try
                    {
                        telephony.answerVideoCallPeer(peer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error(
                                "Could not answer " + peer + " with video"
                                    + " because of the following exception: "
                                    + ofe);
                    }
                }
                else
                {
                    OperationSetBasicTelephony<?> telephony
                        = pps.getOperationSet(OperationSetBasicTelephony.class);

                    try
                    {
                        telephony.answerCallPeer(peer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error(
                                "Could not answer " + peer
                                    + " because of the following exception: ",
                                ofe);
                    }
                }
            }
        }
    }

    /**
     * Invites a list of callees to a conference <tt>Call</tt>. If the specified
     * <tt>Call</tt> is <tt>null</tt>, creates a brand new telephony conference.
     */
    private static class InviteToConferenceCallThread
        extends Thread
    {
        /**
         * The addresses of the callees to be invited into the telephony
         * conference to be organized by this instance. For further details,
         * refer to the documentation on the <tt>callees</tt> parameter of the
         * respective <tt>InviteToConferenceCallThread</tt> constructor.
         */
        private final Map<ProtocolProviderService, List<String>>
            callees;

        /**
         * The <tt>Call</tt>, if any, into the telephony conference of which
         * {@link #callees} are to be invited. If non-<tt>null</tt>, its
         * <tt>CallConference</tt> state will be shared with all <tt>Call</tt>s
         * established by this instance for the purposes of having the
         * <tt>callees</tt> into the same telephony conference.
         */
        private final Call call;

        /**
         * Initializes a new <tt>InviteToConferenceCallThread</tt> instance
         * which is to invite a list of callees to a conference <tt>Call</tt>.
         * If the specified <tt>call</tt> is <tt>null</tt>, creates a brand new
         * telephony conference.
         *
         * @param callees the addresses of the callees to be invited into a
         * telephony conference. The addresses are provided in multiple
         * <tt>List&lt;String&gt;</tt>s. Each such list of addresses is mapped
         * by the <tt>ProtocolProviderService</tt> through which they are to be
         * invited into the telephony conference. If there are multiple
         * <tt>ProtocolProviderService</tt>s in the specified <tt>Map</tt>, the
         * resulting telephony conference is known by the name
         * &quot;cross-protocol&quot;. It is also allowed to have a list of
         * addresses mapped to <tt>null</tt> which means that the new instance
         * will automatically choose a <tt>ProtocolProviderService</tt> to
         * invite the respective callees into the telephony conference.
         * @param call the <tt>Call</tt> to invite the specified
         * <tt>callees</tt> into. If <tt>null</tt>, this instance will create a
         * brand new telephony conference. Technically, a <tt>Call</tt> instance
         * is protocol/account-specific and it is possible to have
         * cross-protocol/account telephony conferences. That's why the
         * specified <tt>callees</tt> are invited into one and the same
         * <tt>CallConference</tt>: the one in which the specified <tt>call</tt>
         * is participating or a new one if <tt>call</tt> is <tt>null</tt>. Of
         * course, an attempt is made to have all callees from one and the same
         * protocol/account into one <tt>Call</tt> instance.
         */
        public InviteToConferenceCallThread(
                Map<ProtocolProviderService, List<String>> callees,
                Call call)
        {
            this.callees = callees;
            this.call = call;
        }

        /**
         * Invites {@link #callees} into a telephony conference which is
         * optionally specified by {@link #call}.
         */
        @Override
        public void run()
        {
            CallConference conference
                = (call == null) ? null : call.getConference();

            for(Map.Entry<ProtocolProviderService, List<String>> entry
                    : callees.entrySet())
            {
                ProtocolProviderService pps = entry.getKey();

                /*
                 * We'd like to allow specifying callees without specifying an
                 * associated ProtocolProviderService.
                 */
                if (pps != null)
                {
                    OperationSetBasicTelephony<?> basicTelephony
                        = pps.getOperationSet(OperationSetBasicTelephony.class);

                    if(basicTelephony == null)
                        continue;
                }

                List<String> contactList = entry.getValue();
                String[] contactArray
                    = contactList.toArray(new String[contactList.size()]);

                if (ConfigurationUtils.isNormalizePhoneNumber())
                    normalizePhoneNumbers(contactArray);

                /* Try to have a single Call per ProtocolProviderService. */
                Call ppsCall;

                if ((call != null) && call.getProtocolProvider().equals(pps))
                    ppsCall = call;
                else
                {
                    ppsCall = null;
                    if (conference != null)
                    {
                        List<Call> conferenceCalls = conference.getCalls();

                        if (pps == null)
                        {
                            /*
                             * We'd like to allow specifying callees without
                             * specifying an associated ProtocolProviderService.
                             * The simplest approach is to just choose the first
                             * ProtocolProviderService involved in the telephony
                             * conference.
                             */
                            if (call == null)
                            {
                                if (!conferenceCalls.isEmpty())
                                {
                                    ppsCall = conferenceCalls.get(0);
                                    pps = ppsCall.getProtocolProvider();
                                }
                            }
                            else
                            {
                                ppsCall = call;
                                pps = ppsCall.getProtocolProvider();
                            }
                        }
                        else
                        {
                            for (Call conferenceCall : conferenceCalls)
                            {
                                if (pps.equals(
                                        conferenceCall.getProtocolProvider()))
                                {
                                    ppsCall = conferenceCall;
                                    break;
                                }
                            }
                        }
                    }
                }

                OperationSetTelephonyConferencing telephonyConferencing
                    = pps.getOperationSet(
                            OperationSetTelephonyConferencing.class);

                try
                {
                    if (ppsCall == null)
                    {
                        ppsCall
                            = telephonyConferencing.createConfCall(
                                    contactArray,
                                    conference);
                        if (conference == null)
                            conference = ppsCall.getConference();
                    }
                    else
                    {
                        for (String contact : contactArray)
                        {
                            telephonyConferencing.inviteCalleeToCall(
                                    contact,
                                    ppsCall);
                        }
                    }
                }
                catch(Exception e)
                {
                    logger.error(
                            "Failed to invite callees: "
                                + Arrays.toString(contactArray),
                            e);
                    new ErrorDialog(
                            null,
                            GuiActivator.getResources().getI18NString(
                                    "service.gui.ERROR"),
                            e.getMessage(),
                            ErrorDialog.ERROR)
                        .showDialog();
                }
            }
        }
    }

    /**
     * Invites a list of callees to a specific conference <tt>Call</tt>. If the
     * specified <tt>Call</tt> is <tt>null</tt>, creates a brand new telephony
     * conference.
     */
    private static class InviteToConferenceBridgeThread
        extends Thread
    {
        private final ProtocolProviderService callProvider;

        private final String[] callees;

        private final Call call;

        public InviteToConferenceBridgeThread(
                                        ProtocolProviderService callProvider,
                                        String[] callees,
                                        Call call)
        {
            this.callProvider = callProvider;
            this.callees = callees;
            this.call = call;
        }

        @Override
        public void run()
        {
            OperationSetVideoBridge opSetVideoBridge
                = callProvider.getOperationSet(
                    OperationSetVideoBridge.class);

            // Normally if this method is called then this should not happen
            // but we check in order to be sure to be able to proceed.
            if (opSetVideoBridge == null || !opSetVideoBridge.isActive())
                return;

            if (ConfigurationUtils.isNormalizePhoneNumber())
                normalizePhoneNumbers(callees);

            try
            {
                if (call == null)
                {
                    opSetVideoBridge.createConfCall(callees);
                }
                else
                {
                    for (String contact : callees)
                        opSetVideoBridge.inviteCalleeToCall(contact, call);
                }
            }
            catch(Exception e)
            {
                logger.error(
                        "Failed to invite callees: "
                            + Arrays.toString(callees),
                        e);
                new ErrorDialog(
                        null,
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ERROR"),
                        e.getMessage(),
                        e)
                    .showDialog();
            }
        }
    }

    /**
     * Hangs up a specific <tt>Call</tt> (i.e. all <tt>CallPeer</tt>s associated
     * with a <tt>Call</tt>), <tt>CallConference</tt> (i.e. all <tt>Call</tt>s
     * participating in a <tt>CallConference</tt>), or <tt>CallPeer</tt>.
     */
    private static class HangupCallThread
        extends Thread
    {
        private final Call call;

        private final CallConference conference;

        private final CallPeer peer;

        /**
         * Initializes a new <tt>HangupCallThread</tt> instance which is to hang
         * up a specific <tt>Call</tt> i.e. all <tt>CallPeer</tt>s associated
         * with the <tt>Call</tt>.
         *
         * @param call the <tt>Call</tt> whose associated <tt>CallPeer</tt>s are
         * to be hanged up
         */
        public HangupCallThread(Call call)
        {
            this(call, null, null);
        }

        /**
         * Initializes a new <tt>HangupCallThread</tt> instance which is to hang
         * up a specific <tt>CallConference</tt> i.e. all <tt>Call</tt>s
         * participating in the <tt>CallConference</tt>.
         *
         * @param conference the <tt>CallConference</tt> whose participating
         * <tt>Call</tt>s re to be hanged up
         */
        public HangupCallThread(CallConference conference)
        {
            this(null, conference, null);
        }

        /**
         * Initializes a new <tt>HangupCallThread</tt> instance which is to hang
         * up a specific <tt>CallPeer</tt>.
         *
         * @param peer the <tt>CallPeer</tt> to hang up
         */
        public HangupCallThread(CallPeer peer)
        {
            this(null, null, peer);
        }

        /**
         * Initializes a new <tt>HangupCallThread</tt> instance which is to hang
         * up a specific <tt>Call</tt>, <tt>CallConference</tt>, or
         * <tt>CallPeer</tt>.
         *
         * @param call the <tt>Call</tt> whose associated <tt>CallPeer</tt>s are
         * to be hanged up
         * @param conference the <tt>CallConference</tt> whose participating
         * <tt>Call</tt>s re to be hanged up
         * @param peer the <tt>CallPeer</tt> to hang up
         */
        private HangupCallThread(
                Call call,
                CallConference conference,
                CallPeer peer)
        {
            this.call = call;
            this.conference = conference;
            this.peer = peer;
        }

        @Override
        public void run()
        {
            /*
             * There is only an OperationSet which hangs up a CallPeer at a time
             * so prepare a list of all CallPeers to be hanged up.
             */
            Set<CallPeer> peers = new HashSet<CallPeer>();

            if (call != null)
            {
                Iterator<? extends CallPeer> peerIter = call.getCallPeers();

                while (peerIter.hasNext())
                    peers.add(peerIter.next());
            }

            if (conference != null)
                peers.addAll(conference.getCallPeers());

            if (peer != null)
                peers.add(peer);

            for (CallPeer peer : peers)
            {
                OperationSetBasicTelephony<?> basicTelephony
                    = peer.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);

                try
                {
                    basicTelephony.hangupCallPeer(peer);
                }
                catch (OperationFailedException ofe)
                {
                    logger.error("Could not hang up: " + peer, ofe);
                }
            }
        }
    }

    /**
     * Creates the enable local video call thread.
     */
    private static class EnableLocalVideoThread
        extends Thread
    {
        private final Call call;

        private final boolean enable;

        /**
         * Creates the enable local video call thread.
         *
         * @param call the call, for which to enable/disable
         * @param enable
         */
        public EnableLocalVideoThread(Call call, boolean enable)
        {
            this.call = call;
            this.enable = enable;
        }

        @Override
        public void run()
        {
            OperationSetVideoTelephony videoTelephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetVideoTelephony.class);
            boolean enableSucceeded = false;

            if (videoTelephony != null)
            {
                // First make sure the desktop sharing is disabled.
                if (enable && isDesktopSharingEnabled(call))
                {
                    JFrame frame = DesktopSharingFrame.getFrameForCall(call);

                    if (frame != null)
                        frame.dispose();
                }

                try
                {
                    videoTelephony.setLocalVideoAllowed(call, enable);
                    enableSucceeded = true;
                }
                catch (OperationFailedException ex)
                {
                    logger.error(
                            "Failed to toggle the streaming of local video.",
                            ex);

                    ResourceManagementService r = GuiActivator.getResources();
                    String title
                        = r.getI18NString(
                                "service.gui.LOCAL_VIDEO_ERROR_TITLE");
                    String message
                        = r.getI18NString(
                                "service.gui.LOCAL_VIDEO_ERROR_MESSAGE");

                    GuiActivator.getAlertUIService().showAlertPopup(
                            title,
                            message,
                            ex);
                }
            }

            // If the operation didn't succeeded for some reason, make sure the
            // video button doesn't remain selected.
            if (enable && !enableSucceeded)
                getActiveCallContainer(call).setVideoButtonSelected(false);
        }
    }

    /**
     * Puts on hold the given <tt>CallPeer</tt>.
     */
    private static class PutOnHoldCallPeerThread
        extends Thread
    {
        private final CallPeer callPeer;

        private final boolean isOnHold;

        public PutOnHoldCallPeerThread(CallPeer callPeer, boolean isOnHold)
        {
            this.callPeer = callPeer;
            this.isOnHold = isOnHold;
        }

        @Override
        public void run()
        {
            OperationSetBasicTelephony<?> telephony
                = callPeer.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            try
            {
                if (isOnHold)
                    telephony.putOnHold(callPeer);
                else
                    telephony.putOffHold(callPeer);
            }
            catch (OperationFailedException ex)
            {
                logger.error(
                        "Failed to put"
                            + callPeer.getAddress()
                            + (isOnHold ? " on hold." : " off hold."),
                        ex);
            }
        }
    }

    /**
     * Merges specific existing <tt>Call</tt>s into a specific telephony
     * conference.
     */
    private static class MergeExistingCalls
        extends Thread
    {
        /**
         * The telephony conference in which {@link #calls} are to be merged.
         */
        private final CallConference conference;

        /**
         * Second call.
         */
        private final Collection<Call> calls;

        /**
         * Initializes a new <tt>MergeExistingCalls</tt> instance which is to
         * merge specific existing <tt>Call</tt>s into a specific telephony
         * conference.
         *
         * @param conference the telephony conference in which the specified
         * <tt>Call</tt>s are to be merged
         * @param calls the <tt>Call</tt>s to be merged into the specified
         * telephony conference
         */
        public MergeExistingCalls(
                CallConference conference,
                Collection<Call> calls)
        {
            this.conference = conference;
            this.calls = calls;
        }

        /**
         * Puts off hold the <tt>CallPeer</tt>s of a specific <tt>Call</tt>
         * which are locally on hold.
         *
         * @param call the <tt>Call</tt> which is to have its <tt>CallPeer</tt>s
         * put off hold
         */
        private void putOffHold(Call call)
        {
            Iterator<? extends CallPeer> peers = call.getCallPeers();
            OperationSetBasicTelephony<?> telephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            while (peers.hasNext())
            {
                CallPeer callPeer = peers.next();
                boolean putOffHold = true;

                if(callPeer instanceof MediaAwareCallPeer)
                {
                    putOffHold
                        = ((MediaAwareCallPeer<?,?,?>) callPeer)
                            .getMediaHandler()
                                .isLocallyOnHold();
                }
                if(putOffHold)
                {
                    try
                    {
                        telephony.putOffHold(callPeer);
                        Thread.sleep(400);
                    }
                    catch(Exception ofe)
                    {
                        logger.error("Failed to put off hold.", ofe);
                    }
                }
            }
        }

        @Override
        public void run()
        {
            // conference
            for (Call call : conference.getCalls())
                putOffHold(call);

            // calls
            if (!calls.isEmpty())
            {
                for(Call call : calls)
                {
                    if (conference.containsCall(call))
                        continue;

                    putOffHold(call);

                    /*
                     * Dispose of the CallPanel associated with the Call which
                     * is to be merged.
                     */
                    closeCallContainerIfNotNecessary(conference, false);

                    call.setConference(conference);
                }
            }
        }
    }

    /**
     * Shows a warning window to warn the user that she's about to start a
     * desktop sharing session.
     *
     * @return <tt>true</tt> if the user has accepted the desktop sharing
     * session; <tt>false</tt>, otherwise
     */
    private static boolean showDesktopSharingWarning()
    {
        Boolean isWarningEnabled
            = GuiActivator.getConfigurationService().getBoolean(
                    desktopSharingWarningProperty,
                    true);

        if (isWarningEnabled.booleanValue())
        {
            ResourceManagementService resources = GuiActivator.getResources();
            MessageDialog warningDialog
                = new MessageDialog(
                        null,
                        resources.getI18NString("service.gui.WARNING"),
                        resources.getI18NString(
                                "service.gui.DESKTOP_SHARING_WARNING"),
                        true);

            switch (warningDialog.showDialog())
            {
                case MessageDialog.OK_RETURN_CODE:
                    return true;
                case MessageDialog.CANCEL_RETURN_CODE:
                    return false;
                case MessageDialog.OK_DONT_ASK_CODE:
                    GuiActivator.getConfigurationService().setProperty(
                            desktopSharingWarningProperty,
                            false);
                    return true;
            }
        }

        return true;
    }

    /**
     * Normalizes the phone numbers (if any) in a list of <tt>String</tt>
     * contact addresses or phone numbers.
     *
     * @param callees the list of contact addresses or phone numbers to be
     * normalized
     */
    private static void normalizePhoneNumbers(String callees[])
    {
        for (int i = 0 ; i < callees.length ; i++)
            callees[i] = GuiActivator.getPhoneNumberI18nService()
                .normalize(callees[i]);
    }

    /**
     * Throws a <tt>RuntimeException</tt> if the current thread is not the AWT
     * event dispatching thread.
     */
    public static void assertIsEventDispatchingThread()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            throw new RuntimeException(
                    "The methon can be called only on the AWT event dispatching"
                        + " thread.");
        }
    }

    /**
     * Finds the <tt>CallPanel</tt>, if any, which depicts a specific
     * <tt>CallConference</tt>.
     * <p>
     * <b>Note</b>: The method can be called only on the AWT event dispatching
     * thread.
     * </p>
     *
     * @param conference the <tt>CallConference</tt> to find the depicting
     * <tt>CallPanel</tt> of
     * @return the <tt>CallPanel</tt> which depicts the specified
     * <tt>CallConference</tt> if such a <tt>CallPanel</tt> exists; otherwise,
     * <tt>null</tt>
     * @throws RuntimeException if the method is not called on the AWT event
     * dispatching thread
     */
    private static CallPanel findCallPanel(CallConference conference)
    {
        synchronized (callPanels)
        {
            return callPanels.get(conference);
        }
    }

    /**
     * Notifies {@link #callPanels} about a specific <tt>CallEvent</tt> received
     * by <tt>CallManager</tt> (because they may need to update their UI, for
     * example).
     * <p>
     * <b>Note</b>: The method can be called only on the AWT event dispatching
     * thread.
     * </p>
     *
     * @param ev the <tt>CallEvent</tt> received by <tt>CallManager</tt> which
     * is to be forwarded to <tt>callPanels</tt> for further
     * <tt>CallPanel</tt>-specific handling
     * @throws RuntimeException if the method is not called on the AWT event
     * dispatching thread
     */
    private static void forwardCallEventToCallPanels(CallEvent ev)
    {
        assertIsEventDispatchingThread();

        CallPanel[] callPanels;

        synchronized (CallManager.callPanels)
        {
            Collection<CallPanel> values = CallManager.callPanels.values();

            callPanels = values.toArray(new CallPanel[values.size()]);
        }

        for (CallPanel callPanel : callPanels)
        {
            try
            {
                callPanel.onCallEvent(ev);
            }
            catch (Exception ex)
            {
                /*
                 * There is no practical reason while the failure of a CallPanel
                 * to handle the CallEvent should cause the other CallPanels to
                 * be left out-of-date.
                 */
                logger.error("A CallPanel failed to handle a CallEvent", ex);
            }
        }
    }

    /**
     * Creates a call for the supplied operation set.
     * @param opSetClass the operation set to use to create call.
     * @param protocolProviderService the protocol provider
     * @param contact the contact address to call
     */
    static void createCall(Class<? extends OperationSet> opSetClass,
                    ProtocolProviderService protocolProviderService,
                    String contact)
    {
        createCall(opSetClass, protocolProviderService, contact, null);
    }

    /**
     * Creates a call for the supplied operation set.
     * @param opSetClass the operation set to use to create call.
     * @param protocolProviderService the protocol provider
     * @param contact the contact address to call
     *  @param uiContact the <tt>MetaContact</tt> we're calling
     */
    static void createCall(
                    Class<? extends OperationSet> opSetClass,
                    ProtocolProviderService protocolProviderService,
                    String contact,
                    UIContactImpl uiContact)
    {
        if (opSetClass.equals(OperationSetBasicTelephony.class))
        {
            createCall(protocolProviderService, contact, uiContact);
        }
        else if (opSetClass.equals(OperationSetVideoTelephony.class))
        {
            createVideoCall(protocolProviderService, contact, uiContact);
        }
        else if (opSetClass.equals(OperationSetDesktopStreaming.class))
        {
            createDesktopSharing(
                protocolProviderService, contact, uiContact);
        }
    }

    /**
     * Creates a call for the default contact of the metacontact
     *
     * @param metaContact the metacontact that will be called.
     * @param isVideo if <tt>true</tt> will create video call.
     * @param isDesktop if <tt>true</tt> will share the desktop.
     * @param shareRegion if <tt>true</tt> will share a region of the desktop.
     */
    public static void call(MetaContact metaContact,
                            boolean isVideo,
                            boolean isDesktop,
                            boolean shareRegion)
    {
        Contact contact = metaContact
            .getDefaultContact(getOperationSetForCall(isVideo, isDesktop));

        call(contact, isVideo, isDesktop, shareRegion);
    }

    /**
     * A particular contact has been selected no options to select
     * will just call it.
     * @param contact the contact to call
     * @param contactResource the specific contact resource
     * @param isVideo is video enabled
     * @param isDesktop is desktop sharing enabled
     * @param shareRegion is sharing the whole desktop or just a region.
     */
    public static void call(Contact contact,
                            ContactResource contactResource,
                            boolean isVideo,
                            boolean isDesktop,
                            boolean shareRegion)
    {
        if(isDesktop)
        {
            if(shareRegion)
            {
                createRegionDesktopSharing(
                    contact.getProtocolProvider(),
                    contact.getAddress(),
                    null);
            }
            else
                createDesktopSharing(contact.getProtocolProvider(),
                                    contact.getAddress(),
                                    null);
        }
        else
        {
            new CreateCallThread(
                    contact.getProtocolProvider(),
                    contact,
                    contactResource,
                    isVideo).start();
        }
    }

    /**
     * Creates a call to the conference described in
     * <tt>conferenceDescription</tt> through <tt>protocolProvider</tt>
     *
     * @param protocolProvider the protocol provider through which to create
     * the call
     * @param conferenceDescription the description of the conference to call
     * @param chatRoom the chat room associated with the call.
     */
    public static void call(ProtocolProviderService protocolProvider,
                            ConferenceDescription conferenceDescription,
                            ChatRoom chatRoom)
    {
        new CreateCallThread(protocolProvider, conferenceDescription, chatRoom)
            .start();
    }

    /**
     * A particular contact has been selected no options to select
     * will just call it.
     * @param contact the contact to call
     * @param isVideo is video enabled
     * @param isDesktop is desktop sharing enabled
     * @param shareRegion is sharing the whole desktop or just a region.
     */
    public static void call(Contact contact,
                            boolean isVideo,
                            boolean isDesktop,
                            boolean shareRegion)
    {
        if(isDesktop)
        {
            if(shareRegion)
            {
                createRegionDesktopSharing(
                    contact.getProtocolProvider(),
                    contact.getAddress(),
                    null);
            }
            else
                createDesktopSharing(contact.getProtocolProvider(),
                                    contact.getAddress(),
                                    null);
        }
        else
        {
            new CreateCallThread(   contact.getProtocolProvider(),
                                    contact,
                                    null,
                                    isVideo).start();
        }
    }

    /**
     * Calls a phone showing a dialog to choose a provider.
     * @param phone phone to call
     * @param isVideo if <tt>true</tt> will create video call.
     * @param isDesktop if <tt>true</tt> will share the desktop.
     * @param shareRegion if <tt>true</tt> will share a region of the desktop.
     */
    public static void call(final String phone,
                            boolean isVideo,
                            boolean isDesktop,
                            final boolean shareRegion)
    {
        call(phone, null, isVideo, isDesktop, shareRegion);
    }

    /**
     * Calls a phone showing a dialog to choose a provider.
     * @param phone phone to call
     * @param uiContact the <tt>UIContactImpl</tt> we're calling
     * @param isVideo if <tt>true</tt> will create video call.
     * @param isDesktop if <tt>true</tt> will share the desktop.
     * @param shareRegion if <tt>true</tt> will share a region of the desktop.
     */
    public static void call(final String phone,
                            final UIContactImpl uiContact,
                            boolean isVideo,
                            boolean isDesktop,
                            final boolean shareRegion)
    {
        List<ProtocolProviderService> providers =
            CallManager.getTelephonyProviders();

        if(providers.size() > 1)
        {
            ChooseCallAccountDialog chooseAccount =
                new ChooseCallAccountDialog(
                    phone,
                    getOperationSetForCall(isVideo, isDesktop),
                    providers)
            {
                    @Override
                    public void callButtonPressed()
                    {
                        if(shareRegion)
                        {
                            createRegionDesktopSharing(
                                getSelectedProvider(), phone, uiContact);
                        }
                        else
                            super.callButtonPressed();
                    }
            };
            chooseAccount.setUIContact(uiContact);
            chooseAccount.setVisible(true);
        }
        else
        {
            createCall(providers.get(0), phone, uiContact);
        }
    }

    /**
     * Obtain operation set checking the params.
     * @param isVideo if <tt>true</tt> use OperationSetVideoTelephony.
     * @param isDesktop if <tt>true</tt> use OperationSetDesktopStreaming.
     * @return the operation set, default is OperationSetBasicTelephony.
     */
    private static Class<? extends OperationSet> getOperationSetForCall(
        boolean isVideo, boolean isDesktop)
    {
        if(isVideo)
        {
            if(isDesktop)
                return OperationSetDesktopStreaming.class;
            else
                return OperationSetVideoTelephony.class;
        }
        else
            return OperationSetBasicTelephony.class;
    }

    /**
     * Call any of the supplied details.
     *
     * @param uiContactDetailList the list with details to choose for calling
     * @param uiContact the <tt>UIContactImpl</tt> to check what is enabled,
     * available.
     * @param isVideo if <tt>true</tt> will create video call.
     * @param isDesktop if <tt>true</tt> will share the desktop.
     * @param invoker the invoker component
     * @param location the location where this was invoked.
     */
    public static void call(List<UIContactDetail> uiContactDetailList,
                            UIContactImpl uiContact,
                            boolean isVideo,
                            boolean isDesktop,
                            JComponent invoker,
                            Point location)
    {
        call(uiContactDetailList,
            uiContact,
            isVideo,
            isDesktop,
            invoker,
            location,
            true);
    }

    /**
     * Call any of the supplied details.
     *
     * @param uiContactDetailList the list with details to choose for calling
     * @param uiContact the <tt>UIContactImpl</tt> to check what is enabled,
     * available.
     * @param isVideo if <tt>true</tt> will create video call.
     * @param isDesktop if <tt>true</tt> will share the desktop.
     * @param invoker the invoker component
     * @param location the location where this was invoked.
     * @param usePreferredProvider whether to use the <tt>uiContact</tt>
     * preferredProvider if provided.
     */
    private static void call(List<UIContactDetail> uiContactDetailList,
                            UIContactImpl uiContact,
                            boolean isVideo,
                            boolean isDesktop,
                            JComponent invoker,
                            Point location,
                            boolean usePreferredProvider)
    {
        ChooseCallAccountPopupMenu chooseCallAccountPopupMenu = null;

        Class<? extends OperationSet> opsetClass =
            getOperationSetForCall(isVideo, isDesktop);

        UIPhoneUtil contactPhoneUtil = null;
        if (uiContact != null
            && uiContact.getDescriptor() instanceof MetaContact)
            contactPhoneUtil = UIPhoneUtil
                .getPhoneUtil((MetaContact) uiContact.getDescriptor());

        if(contactPhoneUtil != null)
        {
            boolean addAdditionalNumbers = false;
            if(!isVideo
                || ConfigurationUtils
                        .isRouteVideoAndDesktopUsingPhoneNumberEnabled())
            {
                addAdditionalNumbers = true;
            }
            else
            {
                if(isVideo && contactPhoneUtil != null)
                {
                    // lets check is video enabled in additional numbers
                    addAdditionalNumbers =
                        contactPhoneUtil.isVideoCallEnabled() ?
                            isDesktop ?
                                contactPhoneUtil.isDesktopSharingEnabled()
                                : true
                            : false;
                }
            }

            if(addAdditionalNumbers)
            {
                uiContactDetailList.addAll(
                    contactPhoneUtil.getAdditionalNumbers());
            }
        }

        if (uiContactDetailList.size() == 1)
        {
            UIContactDetail detail = uiContactDetailList.get(0);

            ProtocolProviderService preferredProvider = null;

            if(usePreferredProvider)
                preferredProvider =
                    detail.getPreferredProtocolProvider(opsetClass);

            List<ProtocolProviderService> providers = null;
            String protocolName = null;

            if (preferredProvider != null)
            {
                if (preferredProvider.isRegistered())
                {
                    createCall(opsetClass,
                               preferredProvider,
                               detail.getAddress(),
                               uiContact);
                }

                // If we have a provider, but it's not registered we try to
                // obtain all registered providers for the same protocol as the
                // given preferred provider.
                else
                {
                    protocolName = preferredProvider.getProtocolName();
                    providers = AccountUtils.getRegisteredProviders(protocolName,
                        opsetClass);
                }
            }
            // If we don't have a preferred provider we try to obtain a
            // preferred protocol name and all registered providers for it.
            else
            {
                protocolName = detail.getPreferredProtocol(opsetClass);

                if (protocolName != null)
                    providers
                        = AccountUtils.getRegisteredProviders(protocolName,
                            opsetClass);
                else
                    providers
                        = AccountUtils.getRegisteredProviders(opsetClass);
            }

            // If our call didn't succeed, try to call through one of the other
            // protocol providers obtained above.
            if (providers != null)
            {
                int providersCount = providers.size();

                if (providersCount <= 0)
                {
                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CALL_FAILED"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                    .showDialog();
                }
                else if (providersCount == 1)
                {
                    createCall(
                        opsetClass,
                        providers.get(0),
                        detail.getAddress(),
                        uiContact);
                }
                else if (providersCount > 1)
                {
                    chooseCallAccountPopupMenu =
                        new ChooseCallAccountPopupMenu(
                            invoker, detail.getAddress(), providers,
                            opsetClass);
                }
            }
        }
        else if (uiContactDetailList.size() > 1)
        {
            chooseCallAccountPopupMenu
                = new ChooseCallAccountPopupMenu(invoker, uiContactDetailList,
                        opsetClass);
        }

        // If the choose dialog is created we're going to show it.
        if (chooseCallAccountPopupMenu != null)
        {
            if (uiContact != null)
                chooseCallAccountPopupMenu.setUIContact(uiContact);

            chooseCallAccountPopupMenu.showPopupMenu(location.x, location.y);
        }
    }


    /**
     * Call the ui contact.
     *
     * @param uiContact the contact to call.
     * @param isVideo if <tt>true</tt> will create video call.
     * @param isDesktop if <tt>true</tt> will share the desktop.
     * @param invoker the invoker component
     * @param location the location where this was invoked.
     */
    public static void call(UIContact uiContact,
                            boolean isVideo,
                            boolean isDesktop,
                            JComponent invoker,
                            Point location)
    {
        UIContactImpl uiContactImpl = null;
        if(uiContact instanceof UIContactImpl)
        {
            uiContactImpl = (UIContactImpl) uiContact;
        }

        List<UIContactDetail> telephonyContacts
            = uiContact.getContactDetailsForOperationSet(
                getOperationSetForCall(isVideo, isDesktop));

        boolean ignorePreferredProvider =
            GuiActivator.getConfigurationService().getBoolean(
                IGNORE_PREFERRED_PROVIDER_PROP, false);

        call(   telephonyContacts,
                uiContactImpl,
                isVideo,
                isDesktop,
                invoker,
                location,
                !ignorePreferredProvider);
    }

    /**
     * Tries to resolves a peer address into a display name, by reqesting the
     * <tt>ContactSourceService</tt>s. This function returns only the
     * first match.
     *
     * @param peerAddress The peer address.
     * @param listener the listener to fire change events for later resolutions
     * of display name and image, if exist.
     * @return The corresponding display name, if there is a match. Null
     * otherwise.
     */
    private static String queryContactSource(
        String peerAddress,
        DetailsResolveListener listener)
    {
        String displayName = null;

        if(!StringUtils.isNullOrEmpty(peerAddress))
        {
            ContactSourceSearcher searcher
                = new ContactSourceSearcher(peerAddress, listener);

            if(listener == null)
            {
                searcher.run();
                displayName = searcher.displayName;
            }
            else
                new Thread(searcher, searcher.getClass().getName()).start();
        }

        return displayName;
    }

    /**
     * Runnable that will search for a source contact and when found will
     * fire events to inform that display name or contact image is found.
     */
    private static class ContactSourceSearcher
        implements Runnable
    {
        private final DetailsResolveListener listener;

        private final String peerAddress;

        private String displayName;
        private byte[] displayImage;

        private ContactSourceSearcher(
            String peerAddress,
            DetailsResolveListener listener)
        {
            this.peerAddress = peerAddress;
            this.listener = listener;
        }

        @Override
        public void run()
        {
            Vector<ResolveAddressToDisplayNameContactQueryListener> resolvers
                = new Vector<ResolveAddressToDisplayNameContactQueryListener>
                    (1, 1);

            // will strip the @server-address part, as the regular expression
            // will match it
            int index = peerAddress.indexOf("@");
            String peerUserID =
                (index > -1) ? peerAddress.substring(0, index) : peerAddress;

            // searches for the whole number/username or with the @serverpart
            String quotedPeerUserID = Pattern.quote(peerUserID);
            Pattern pattern = Pattern.compile(
                "^(" + quotedPeerUserID + "|" + quotedPeerUserID + "@.*)$");

            // Queries all available resolvers
            for(ContactSourceService css : GuiActivator.getContactSources())
            {
                if(css.getType() != ContactSourceService.SEARCH_TYPE)
                    continue;

                ContactQuery query;
                if(css instanceof ExtendedContactSourceService)
                {
                    // use the pattern method of (ExtendedContactSourceService)
                    query = ((ExtendedContactSourceService)css)
                        .createContactQuery(pattern);
                }
                else
                {
                    query = css.createContactQuery(peerUserID);
                }

                if(query == null)
                    continue;

                resolvers.add(
                    new ResolveAddressToDisplayNameContactQueryListener(query));
                query.start();
            }

            long startTime = System.currentTimeMillis();

            // The detault timeout is set to 500ms.
            long timeout = listener == null ? 500 : -1;
            boolean hasRunningResolver = true;
            // Loops until we found a valid display name and image,
            // or waits for timeout if any.
            while((displayName == null || displayImage == null)
                    && hasRunningResolver
                    && (listener == null || listener.isInterested()))
            {
                hasRunningResolver = false;

                for(int i = 0; i < resolvers.size()
                    && (displayName == null || displayImage == null)
                    && (listener == null || listener.isInterested()); ++i)
                {
                    ResolveAddressToDisplayNameContactQueryListener resolver
                        = resolvers.get(i);
                    if(!resolver.isRunning())
                    {
                        if(displayName == null
                            && resolver.isFoundName())
                        {
                            displayName = resolver.getResolvedName();
                            // If this is the same result as the peer address,
                            // then that is not what we are looking for. Then,
                            // continue the search.
                            if(displayName.equals(peerAddress))
                            {
                                displayName = null;
                            }

                            if(listener != null && displayName != null)
                            {
                                // fire
                                listener.displayNameUpdated(displayName);
                            }
                        }

                        if(displayImage == null
                            && resolver.isFoundImage())
                        {
                            displayImage = resolver.getResolvedImage();

                            String name = resolver.getResolvedName();
                            // If this is the same result as the peer address,
                            // then that is not what we are looking for. Then,
                            // continue the search.
                            if(name != null && name.equals(peerAddress))
                            {
                                displayImage = null;
                            }
                            else if(listener != null && displayImage != null)
                            {
                                // fire
                                listener.imageUpdated(displayImage);
                            }
                        }
                    }
                    else
                        hasRunningResolver = true;
                }
                Thread.yield();

                if( timeout > 0 &&
                    System.currentTimeMillis() - startTime >= timeout)
                    break;
            }

            // Free lasting resolvers.
            for(int i = 0; i < resolvers.size(); ++i)
            {
                ResolveAddressToDisplayNameContactQueryListener resolver
                    = resolvers.get(i);
                if(resolver.isRunning())
                {
                    resolver.stop();
                }
            }
        }
    }

    /**
     * A listener that will be notified for found source contacts details.
     */
    public static interface DetailsResolveListener
    {
        /**
         * When a display name is found.
         * @param displayName the name that was found.
         */
        public void displayNameUpdated(String displayName);

        /**
         * The image that was found.
         * @param image the image that was found.
         */
        public void imageUpdated(byte[] image);

        /**
         * Whether the listener is still interested in the events.
         * When the window/panel using this resolver listener is closed
         * will return false;
         * @return whether the listener is still interested in the events.
         */
        public boolean isInterested();
    }
}
