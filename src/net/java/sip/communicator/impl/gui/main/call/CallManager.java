/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.transparent.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * The <tt>CallManager</tt> is the one that handles calls. It contains also
 * the "Call" and "Hang up" buttons panel. Here are handles incoming and
 * outgoing calls from and to the call operation set.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class CallManager
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(CallManager.class);

    /**
     * A table mapping protocol <tt>Call</tt> objects to the GUI dialogs
     * that are currently used to display them.
     */
    private static Hashtable<Call, CallPanel> activeCalls
        = new Hashtable<Call, CallPanel>();

    /**
     * The name of the property which indicates whether the user should be
     * warned when starting a desktop sharing session.
     */
    private static final String desktopSharingWarningProperty
        = "net.java.sip.communicator.impl.gui.main"
            + ".call.SHOW_DESKTOP_SHARING_WARNING";

    /**
     * The group of notifications dedicated to missed calls.
     */
    private static UINotificationGroup missedCallGroup;

    /**
     * A call listener.
     */
    public static class GuiCallListener
        implements CallListener
    {
        /**
         * Implements {@link CallListener#incomingCallReceived(CallEvent)}. When
         * a call is received, creates a <tt>ReceivedCallDialog</tt> and plays
         * the ring phone sound to the user.
         *
         * @param event the <tt>CallEvent</tt>
         */
        public void incomingCallReceived(final CallEvent event)
        {
            if(!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        incomingCallReceived(event);
                    }
                });
                return;
            }

            Call sourceCall = event.getSourceCall();
            boolean isVideoCall = event.isVideoCall() && ConfigurationManager
                    .hasEnabledVideoFormat(sourceCall.getProtocolProvider());
            final ReceivedCallDialog receivedCallDialog
                = new ReceivedCallDialog(sourceCall, isVideoCall,
                    (CallManager.getInProgressCalls().size() > 0));

            receivedCallDialog.setVisible(true);

            Iterator<? extends CallPeer> peerIterator =
                sourceCall.getCallPeers();

            if(!peerIterator.hasNext())
            {
                if (receivedCallDialog.isVisible())
                    receivedCallDialog.setVisible(false);
                return;
            }

            final String peerName = peerIterator.next().getDisplayName();
            final Date callDate = new Date();

            sourceCall.addCallChangeListener(new CallChangeAdapter()
            {
                @Override
                public void callStateChanged(CallChangeEvent evt)
                {
                    // When the call state changes, we ensure here that the
                    // received call notification dialog is closed.
                    if (receivedCallDialog.isVisible())
                        receivedCallDialog.setVisible(false);

                    // Ensure that the CallDialog is created, because it is the
                    // one that listens for CallPeers.
                    Object newValue = evt.getNewValue();
                    Call call = evt.getSourceCall();

                    if (CallState.CALL_INITIALIZATION.equals(newValue)
                            || CallState.CALL_IN_PROGRESS.equals(newValue))
                    {
                        openCallContainerIfNecessary(call);
                    }
                    else if (CallState.CALL_ENDED.equals(newValue))
                    {
                        if (evt.getOldValue().equals(
                                CallState.CALL_INITIALIZATION))
                        {
                            // if call was answered elsewhere, don't add it
                            // to missed calls
                            CallPeerChangeEvent cause = evt.getCause();

                            if((cause == null)
                                || (cause.getReasonCode()
                                    != CallPeerChangeEvent.NORMAL_CALL_CLEARING))
                            {
                                addMissedCallNotification(peerName, callDate);
                            }

                            call.removeCallChangeListener(this);
                        }

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
                }
            });

            /*
             * Notify the existing CallPanels about the CallEvent (in case they
             * need to update their UI, for example). 
             */
            List<CallPanel> callPanels
                = new ArrayList<CallPanel>(activeCalls.values());

            for (CallPanel callPanel : callPanels)
            {
                if (callPanel != null)
                    callPanel.incomingCallReceived(event);
            }
        }

        /**
         * Implements CallListener.callEnded. Stops sounds that are playing at
         * the moment if there're any. Removes the <tt>CallPanel</tt> and
         * disables the hang-up button.
         *
         * @param event the <tt>CallEvent</tt> which specifies the <tt>Call</tt>
         * that has ended
         */
        public void callEnded(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();
            CallPanel callContainer = getActiveCallContainer(sourceCall);

            if (callContainer != null)
            {
                closeCallContainerIfNotNecessary(sourceCall, true);

                /*
                 * Notify the existing CallPanels about the CallEvent (in case
                 * they need to update their UI, for example). 
                 */
                List<CallPanel> callPanels
                    = new ArrayList<CallPanel>(activeCalls.values());

                for (CallPanel callPanel : callPanels)
                {
                    if (callPanel != null)
                        callPanel.callEnded(event);
                }
            }
        }

        /**
         * Creates and opens a call dialog. Implements
         * {@link CallListener#outgoingCallCreated(CallEvent)}.
         *
         * @param event the <tt>CallEvent</tt>
         */
        public void outgoingCallCreated(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();

            openCallContainerIfNecessary(sourceCall);

            /*
             * Notify the existing CallPanels about the CallEvent (in case they
             * need to update their UI, for example). 
             */
            List<CallPanel> callPanels
                = new ArrayList<CallPanel>(activeCalls.values());

            for (CallPanel callPanel : callPanels)
            {
                if (callPanel != null)
                    callPanel.outgoingCallCreated(event);
            }
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
     * Merge multiple existing <tt>Call</tt>s into a single conference
     * <tt>Call</tt>.
     *
     * @param first first call
     * @param calls list of calls
     */
    public static void mergeExistingCall(Call first, Collection<Call> calls)
    {
        new MergeExistingCalls(first, calls).start();
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
    public static void hangupCall(final Call call)
    {
        new HangupCallThread(call).start();
    }

    /**
     * Hang ups the given <tt>callPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> to hang up
     */
    public static void hangupCallPeer(final CallPeer callPeer)
    {
        new HangupCallPeerThread(callPeer).start();
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
     */
    public static void createCall(  ProtocolProviderService protocolProvider,
                                    Contact contact)
    {
        new CreateCallThread(protocolProvider, contact, false /* audio-only */)
            .start();
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
     */
    public static void createVideoCall(ProtocolProviderService protocolProvider,
                                        Contact contact)
    {
        new CreateCallThread(protocolProvider, contact, true /* video */)
            .start();
    }

    /**
     * Enables/disables local video for the given call.
     *
     * @param enable indicates whether to enable or disable the local video
     * @param call the call for which the local video should be enabled/disabled
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
     */
    public static void createDesktopSharing(
            ProtocolProviderService protocolProvider,
            String contact)
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
                    desktopDevices.get(0));
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
                        selectDialog.getSelectedDevice());
        }
    }

    /**
     * Creates a region desktop sharing through the given
     * <tt>protocolProvider</tt> with the given <tt>contact</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, through
     * which the sharing session will be established
     * @param contact the address of the contact recipient
     */
    public static void createRegionDesktopSharing(
                                    ProtocolProviderService protocolProvider,
                                    String contact)
    {
        if (showDesktopSharingWarning())
        {
            TransparentFrame frame = DesktopSharingFrame.createTransparentFrame(
                    protocolProvider, contact, true);

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
     * @param x the x coordinate of the shared region
     * @param y the y coordinated of the shared region
     * @param width the width of the shared region
     * @param height the height of the shared region
     */
    public static void createRegionDesktopSharing(
                                    ProtocolProviderService protocolProvider,
                                    String contact,
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
                    mediaService.getMediaDeviceForPartialDesktopStreaming(
                        width,
                        height,
                        x,
                        y));
        }
    }

    /**
     * Creates a desktop sharing call to the contact represented by the given
     * string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     * @param mediaDevice the media device corresponding to the screen to share
     */
    private static void createDesktopSharing(
                                    ProtocolProviderService protocolProvider,
                                    String contact,
                                    MediaDevice mediaDevice)
    {
        new CreateDesktopSharingThread( protocolProvider,
                                        contact,
                                        mediaDevice).start();
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
            enableDesktopSharing(
                    call,
                    mediaService.getMediaDeviceForPartialDesktopStreaming(
                        width,
                        height,
                        x,
                        y),
                    true);
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
     */
    private static void enableDesktopSharing(Call call,
                                            MediaDevice mediaDevice,
                                            boolean enable)
    {
        OperationSetDesktopSharingServer desktopOpSet
            = call.getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingServer.class);
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

        if (enable && !enableSucceeded)
            getActiveCallContainer(call).setDesktopSharingButtonSelected(false);
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
        OperationSetDesktopSharingServer desktopOpSet
            = call.getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingServer.class);

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
        OperationSetDesktopSharingServer desktopOpSet
            = call.getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingServer.class);

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
        if (ConfigurationManager.isNormalizePhoneNumber())
            callString = PhoneNumberI18nService.normalize(callString);

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
    public static void inviteToConferenceCall(  String[] callees,
                                                Call call)
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
     * @param call the <tt>Call</tt> which is to have its associated
     * <tt>CallPanel</tt>, if any, closed
     * @param wait <tt>true</tt> to use
     * {@link CallContainer#closeWait(CallPanel)} or <tt>false</tt> to use
     * {@link CallContainer#close(CallPanel)}
     */
    private static void closeCallContainerIfNotNecessary(
            Call call,
            boolean wait)
    {
        CallPanel callPanel = activeCalls.remove(call);

        if ((callPanel != null) && !activeCalls.containsValue(callPanel))
        {
            CallContainer callContainer = callPanel.getCallWindow();

            if (wait)
                callContainer.closeWait(callPanel);
            else
                callContainer.close(callPanel);
        }
    }

    /**
     * Opens a <tt>CallPanel</tt> for a specific <tt>Call</tt> if there is none.
     *
     * @param call the <tt>Call</tt> to open a <tt>CallPanel</tt> for
     * @return the <tt>CallPanel</tt> associated with the <tt>Call</tt>
     */
    private static CallPanel openCallContainerIfNecessary(Call call)
    {
        CallPanel callPanel = activeCalls.get(call);

        if (callPanel == null)
        {
            /*
             * Technically, CallPanel displays a whole CallConference, not a
             * single Call. Try to locate the CallPanel for the specified Call
             * by looking at the CallPanels of the other Calls.
             */
            CallConference conference = call.getConference();

            if (conference != null)
            {
                for (Call conferenceCall : conference.getCalls())
                {
                    CallPanel conferenceCallPanel
                        = activeCalls.get(conferenceCall);

                    if (conferenceCallPanel != null)
                    {
                        if (callPanel == null)
                            callPanel = conferenceCallPanel;
                        else if (logger.isDebugEnabled()
                                && (conferenceCallPanel != callPanel))
                        {
                            logger.debug(
                                    "Calls in the same CallConference"
                                        + " have different CallPanels.");
                        }
                    }
                }
            }

            if (callPanel == null)
            {
                // If we're in single-window mode, the single window is the
                // CallContainer.
                CallContainer callContainer
                    = GuiActivator.getUIService().getSingleWindowContainer();

                // If we're in multi-window mode, we create the CallDialog.
                if (callContainer == null)
                    callContainer = new CallDialog();

                callPanel = new CallPanel(call, callContainer);
                callContainer.addCallPanel(callPanel);
            }

            activeCalls.put(call, callPanel);
        }

        return callPanel;
    }

    /**
     * Returns a list of all currently registered telephony providers.
     * @return a list of all currently registered telephony providers
     */
    public static List<ProtocolProviderService> getTelephonyProviders()
    {
        return GuiActivator
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
        return GuiActivator
            .getRegisteredProviders(OperationSetTelephonyConferencing.class);
    }

    /**
     * Returns a collection of all currently active calls.
     *
     * @return a collection of all currently active calls
     */
    public static Collection<Call> getActiveCalls()
    {
        return activeCalls.keySet();
    }

    /**
     * Returns a collection of all currently in progress calls.
     *
     * @return a collection of all currently in progress calls.
     */
    public static Collection<Call> getInProgressCalls()
    {
        Set<Call> calls = activeCalls.keySet();
        ArrayList<Call> inProgressCalls = new ArrayList<Call>(calls.size()); 
        Iterator<Call> it = calls.iterator();
        Call tmpCall;

        while(it.hasNext())
        {
            tmpCall = it.next();
            if(tmpCall.getCallState() == CallState.CALL_IN_PROGRESS)
            {
                inProgressCalls.add(tmpCall);
            }
        }

        //return calls;
        return inProgressCalls;
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
        return activeCalls.get(call);
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
            MetaContact metaContact = GuiActivator.getContactListService()
                .findMetaContactByContact(peer.getContact());

            image = metaContact.getAvatar();
        }

        // If the icon is still null we try to get an image from the call
        // peer.
        if ((image == null || image.length == 0)
                && peer.getImage() != null)
            image = peer.getImage();

        return image;
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
     * @return <tt>true</tt> if we have video streams to show in this interface,
     * otherwise we return <tt>false</tt>.
     */
    public static boolean isVideoStreaming(Call call)
    {
        OperationSetVideoTelephony videoOpSet
            = call.getProtocolProvider()
                .getOperationSet(OperationSetVideoTelephony.class);

        if (videoOpSet == null)
            return false;

        if (videoOpSet.isLocalVideoStreaming(call))
            return true;

        Iterator<? extends CallPeer> callPeers = call.getCallPeers();
        while (callPeers.hasNext())
        {
            List<Component> remoteVideos
                = videoOpSet.getVisualComponents(callPeers.next());

            if (remoteVideos != null && remoteVideos.size() > 0)
                return true;
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
        if(aProtocolIndex > -1)
            a = a.substring(aProtocolIndex + 1);

        int bProtocolIndex = b.indexOf(':');
        if(bProtocolIndex > -1)
            b = b.substring(bProtocolIndex + 1);

        if (a.equals(b))
            return true;

        int aServiceBegin = a.indexOf('@');
        String aUserID;
        String aService;

        if (aServiceBegin > -1)
        {
            aUserID = a.substring(0, aServiceBegin);

            int slashIndex = a.indexOf("/");
            if (slashIndex > 0)
                aService = a.substring(aServiceBegin + 1, slashIndex);
            else
                aService = a.substring(aServiceBegin + 1);
        }
        else
        {
            aUserID = a;
            aService = null;
        }

        int bServiceBegin = b.indexOf('@');
        String bUserID;
        String bService;

        if (bServiceBegin > -1)
        {
            bUserID = b.substring(0, bServiceBegin);
            int slashIndex = b.indexOf("/");

            if (slashIndex > 0)
                bService = b.substring(bServiceBegin + 1, slashIndex);
            else
                bService = b.substring(bServiceBegin + 1);
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
     * Searches for additional phone numbers found in contact information
     * @return additional phone numbers found in contact information;
     */
    public static List<UIContactDetail> getAdditionalNumbers(
                                                        MetaContact metaContact)
    {
        List<UIContactDetail> telephonyContacts
            = new ArrayList<UIContactDetail>();

        Iterator<Contact> contacts = metaContact.getContacts();

        while(contacts.hasNext())
        {
            Contact contact = contacts.next();
            OperationSetServerStoredContactInfo infoOpSet =
                contact.getProtocolProvider().getOperationSet(
                    OperationSetServerStoredContactInfo.class);
            Iterator<GenericDetail> details;
            ArrayList<String> phones = new ArrayList<String>();

            if(infoOpSet != null)
            {
                details = infoOpSet.getAllDetailsForContact(contact);

                while(details.hasNext())
                {
                    GenericDetail d = details.next();
                    if(d instanceof PhoneNumberDetail &&
                        !(d instanceof PagerDetail) &&
                        !(d instanceof FaxDetail))
                    {
                        PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                        if(pnd.getNumber() != null &&
                            pnd.getNumber().length() > 0)
                        {
                            String localizedType = null;

                            if(d instanceof WorkPhoneDetail)
                            {
                                localizedType =
                                    GuiActivator.getResources().
                                        getI18NString(
                                            "service.gui.WORK_PHONE");
                            }
                            else if(d instanceof MobilePhoneDetail)
                            {
                                localizedType =
                                    GuiActivator.getResources().
                                        getI18NString(
                                            "service.gui.MOBILE_PHONE");
                            }
                            else
                            {
                                localizedType =
                                    GuiActivator.getResources().
                                        getI18NString(
                                            "service.gui.PHONE");
                            }

                            phones.add(pnd.getNumber());

                            UIContactDetail cd =
                                new UIContactDetailImpl(
                                    pnd.getNumber(),
                                    pnd.getNumber() +
                                    " (" + localizedType + ")",
                                    null,
                                    new ArrayList<String>(),
                                    null,
                                    null,
                                    null,
                                    pnd)
                            {
                                public PresenceStatus getPresenceStatus()
                                {
                                    return null;
                                }
                            };
                            telephonyContacts.add(cd);
                        }
                    }
                }
            }
        }

        return telephonyContacts;
    }

    /**
     * Adds a missed call notification.
     *
     * @param peerName the name of the peer
     * @param callDate the date of the call
     */
    private static void addMissedCallNotification(  String peerName,
                                                    Date callDate)
    {
        if (missedCallGroup == null)
            missedCallGroup
                = new UINotificationGroup("MissedCalls",
                    GuiActivator.getResources().getI18NString(
                        "service.gui.MISSED_CALLS_TOOL_TIP"));

        UINotificationManager.addNotification(new UINotification(
                                                    peerName,
                                                    callDate,
                                                    missedCallGroup));
    }

    /**
     * Creates a new (audio-only or video) <tt>Call</tt> to a contact specified
     * as a <tt>Contact</tt> instance or a <tt>String</tt> contact
     * address/identifier.
     */
    private static class CreateCallThread
        extends Thread
    {
        private final Contact contact;

        private final ProtocolProviderService protocolProvider;

        private final String stringContact;

        /**
         * The indicator which determines whether this instance is to create a
         * new video (as opposed to audio-only) <tt>Call</tt>.
         */
        private final boolean video;

        public CreateCallThread(
                ProtocolProviderService protocolProvider,
                Contact contact,
                boolean video)
        {
            this(protocolProvider, contact, null, video);
        }

        public CreateCallThread(
                ProtocolProviderService protocolProvider,
                String contact,
                boolean video)
        {
            this(protocolProvider, null, contact, video);
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
         * @param contact
         * @param stringContact
         * @param video <tt>true</tt> if this instance is to create a new video
         * (as opposed to audio-only) <tt>Call</tt>
         */
        private CreateCallThread(
                ProtocolProviderService protocolProvider,
                Contact contact,
                String stringContact,
                boolean video)
        {
            this.protocolProvider = protocolProvider;
            this.contact = contact;
            this.stringContact = stringContact;
            this.video = video;
        }

        @Override
        public void run()
        {
            Contact contact = this.contact;
            String stringContact = this.stringContact;

            if (ConfigurationManager.isNormalizePhoneNumber())
            {
                if (contact != null)
                {
                    stringContact = contact.getAddress();
                    contact = null;
                }

                stringContact = PhoneNumberI18nService.normalize(stringContact);
            }

            try
            {
                if (video)
                {
                    OperationSetVideoTelephony telephony
                        = protocolProvider.getOperationSet(
                                OperationSetVideoTelephony.class);

                    if (telephony != null)
                    {
                        if (contact != null)
                            telephony.createVideoCall(contact);
                        else if (stringContact != null)
                            telephony.createVideoCall(stringContact);
                    }
                }
                else
                {
                    OperationSetBasicTelephony<?> telephony
                        = protocolProvider.getOperationSet(
                                OperationSetBasicTelephony.class);

                    if (telephony != null)
                    {
                        if (contact != null)
                            telephony.createCall(contact);
                        else if (stringContact != null)
                            telephony.createCall(stringContact);
                    }
                }
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;

                logger.error("The call could not be created: ", t);
                new ErrorDialog(
                        null,
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ERROR"),
                        t.getMessage(),
                        t)
                    .showDialog();
            }
        }
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
         * Creates a desktop sharing session thread.
         *
         * @param protocolProvider protocol provider through which we share our
         * desktop
         * @param contact the contact to share the desktop with
         * @param mediaDevice the media device corresponding to the screen we
         * would like to share
         */
        public CreateDesktopSharingThread(
                                    ProtocolProviderService protocolProvider,
                                    String contact,
                                    MediaDevice mediaDevice)
        {
            this.protocolProvider = protocolProvider;
            this.stringContact = contact;
            this.mediaDevice = mediaDevice;
        }

        @Override
        public void run()
        {
            OperationSetDesktopSharingServer desktopSharingOpSet
                = protocolProvider.getOperationSet(
                        OperationSetDesktopSharingServer.class);

            /*
             * XXX If we are here and we just discover that
             * OperationSetDesktopSharingServer is not supported, then we're
             * already in trouble - we've already started a whole new thread
             * just to check that a reference is null.
             */
            if (desktopSharingOpSet == null)
                return;

            Throwable exception = null;

            try
            {
                if (mediaDevice != null)
                {
                    desktopSharingOpSet.createVideoCall(
                            stringContact,
                            mediaDevice);
                }
                else
                    desktopSharingOpSet.createVideoCall(stringContact);
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
                                "Could not answer "
                                    + peer
                                    + " with video"
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
                                "Could not answer "
                                    + peer
                                    + " because of the following exception: ",
                                ofe);
                    }
                }
            }
        }
    }

    /**
     * Invites a list of callees to a specific conference <tt>Call</tt>. If the
     * specified <tt>Call</tt> is <tt>null</tt>, creates a brand new telephony
     * conference.
     */
    private static class InviteToConferenceCallThread
        extends Thread
    {
        private final Map<ProtocolProviderService, List<String>>
            callees;

        private final Call call;

        public InviteToConferenceCallThread(
                Map<ProtocolProviderService, List<String>> callees,
                Call call)
        {
            this.callees = callees;
            this.call = call;
        }

        @Override
        public void run()
        {
            CallConference conference
                = (call == null) ? null : call.getConference();

            for(Map.Entry<ProtocolProviderService, List<String>> entry
                    : callees.entrySet())
            {
                ProtocolProviderService pps = entry.getKey();

                OperationSetBasicTelephony<?> basicTelephony
                    = pps.getOperationSet(OperationSetBasicTelephony.class);

                if(basicTelephony == null)
                    continue;

                List<String> contactList = entry.getValue();
                String[] contactArray
                    = contactList.toArray(new String[contactList.size()]);

                if (ConfigurationManager.isNormalizePhoneNumber())
                    normalizePhoneNumbers(contactArray);

                /* Try to have a single Call per ProtocolProviderService. */
                Call ppsCall;

                if ((call != null) && pps.equals(call.getProtocolProvider()))
                    ppsCall = call;
                else
                {
                    ppsCall = null;
                    if (conference != null)
                    {
                        for (Call conferenceCall : conference.getCalls())
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
     * Hang-ups all call peers in the given call.
     */
    private static class HangupCallThread
        extends Thread
    {
        private final Call call;

        public HangupCallThread(Call call)
        {
            this.call = call;
        }

        @Override
        public void run()
        {
            for (Call conferenceCall : CallConference.getCalls(call))
            {
                Iterator<? extends CallPeer> peerIter
                    = conferenceCall.getCallPeers();
                OperationSetBasicTelephony<?> basicTelephony
                    = conferenceCall.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);

                while (peerIter.hasNext())
                {
                    CallPeer peer = peerIter.next();

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
    }

    /**
     * Hang-ups the given <tt>CallPeer</tt>.
     */
    private static class HangupCallPeerThread
        extends Thread
    {
        private final CallPeer callPeer;

        public HangupCallPeerThread(CallPeer callPeer)
        {
            this.callPeer = callPeer;
        }

        @Override
        public void run()
        {
            ProtocolProviderService pps = callPeer.getProtocolProvider();
            OperationSetBasicTelephony<?> telephony
                = pps.getOperationSet(OperationSetBasicTelephony.class);

            try
            {
                telephony.hangupCallPeer(callPeer);
            }
            catch (OperationFailedException e)
            {
                logger.error("Could not hang up : " + callPeer
                    + " caused by the following exception: " + e);
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
            OperationSetVideoTelephony telephony
                = call.getProtocolProvider()
                    .getOperationSet(OperationSetVideoTelephony.class);
            boolean enableSucceeded = false;

            if (telephony != null)
            {
                // First make sure the desktop sharing is disabled.
                if (enable && isDesktopSharingEnabled(call))
                {
                    getActiveCallContainer(call)
                        .setDesktopSharingButtonSelected(false);

                    JFrame frame = DesktopSharingFrame.getFrameForCall(call);

                    if(frame != null)
                        frame.dispose();
                }

                try
                {
                    telephony.setLocalVideoAllowed(call, enable);
                    enableSucceeded = true;
                }
                catch (OperationFailedException ex)
                {
                    logger.error(
                        "Failed to toggle the streaming of local video.",
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
     * Merge existing calls thread.
     */
    private static class MergeExistingCalls
        extends Thread
    {
        /**
         * First call.
         */
        private final Call first;

        /**
         * Second call.
         */
        private final Collection<Call> calls;

        /**
         * Constructor.
         *
         * @param first first call
         * @param calls list of calls
         */
        public MergeExistingCalls(Call first, Collection<Call> calls)
        {
            this.first = first;
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
            // first
            putOffHold(first);

            // calls
            if (!calls.isEmpty())
            {
                CallConference conference = first.getConference();

                for(Call call : calls)
                {
                    if (call == first)
                        continue;

                    putOffHold(call);

                    /*
                     * Dispose of the CallPanel associated with the Call which
                     * is to be merged.
                     */
                    closeCallContainerIfNotNecessary(call, false);

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
            callees[i] = PhoneNumberI18nService.normalize(callees[i]);
    }
}
