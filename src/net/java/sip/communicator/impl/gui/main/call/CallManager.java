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
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.transparent.*;

/**
 * The <tt>CallManager</tt> is the one that handles calls. It contains also
 * the "Call" and "Hang up" buttons panel. Here are handles incoming and
 * outgoing calls from and to the call operation set.
 *
 * @author Yana Stamcheva
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
     * The property indicating if the user should be warned when starting a
     * desktop sharing session.
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
    public static class GuiCallListener implements CallListener
    {
        /**
         * Implements CallListener.incomingCallReceived. When a call is received
         * creates a <tt>ReceivedCallDialog</tt> and plays the
         * ring phone sound to the user.
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
            final ReceivedCallDialog receivedCallDialog
                = new ReceivedCallDialog(sourceCall, event.isVideoCall(),
                    (CallManager.getActiveCalls().size() > 0));

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

                    // Ensure that the CallDialog is created, because for now
                    // it is the one that listens for CallPeers.
                    Call call = evt.getSourceCall();
                    if ((evt.getNewValue()
                            .equals(CallState.CALL_INITIALIZATION)
                        || evt.getNewValue()
                            .equals(CallState.CALL_IN_PROGRESS))
                        && activeCalls.get(call) == null &&
                        call.getCallGroup() == null)
                    {
                        openCallContainer(call);
                    }

                    if (evt.getNewValue().equals(CallState.CALL_ENDED))
                    {
                        if (evt.getOldValue()
                                .equals(CallState.CALL_INITIALIZATION))
                        {
                            // if call was answered elsewhere, don't add it
                            // to missed calls
                            if(evt.getCause() == null
                               || (evt.getCause().getReasonCode() !=
                                    CallPeerChangeEvent.NORMAL_CALL_CLEARING))
                            {
                                addMissedCallNotification(peerName, callDate);
                            }

                            evt.getSourceCall().removeCallChangeListener(this);
                        }

                        // If we're currently in the call history view refresh
                        // the view.
                        TreeContactList contactList
                            = GuiActivator.getContactList();

                        if (contactList.getCurrentFilter()
                                .equals(TreeContactList.historyFilter))
                        {
                            contactList.applyFilter(
                                TreeContactList.historyFilter);
                        }
                    }
                }
            });

            List<CallPanel> calls = new ArrayList<CallPanel>(
                activeCalls.values());

            for(CallPanel cp : calls)
            {
                if(cp != null)
                    cp.incomingCallReceived(event);
            }
        }

        /**
         * Implements CallListener.callEnded. Stops sounds that are playing at
         * the moment if there're any. Removes the call panel and disables the
         * hang up button.
         *
         * @param event the <tt>CallEvent</tt>
         */
        public void callEnded(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();

            if (activeCalls.get(sourceCall) != null)
            {
                CallPanel callContainer = activeCalls.get(sourceCall);

                CallGroup group = sourceCall.getCallGroup();

                if(group != null)
                {
                    for (Call c : group.getCalls())
                    {
                        if (c == sourceCall)
                            continue;
                        CallPanel container = activeCalls.get(c);
                        if (container != null)
                        {
                            activeCalls.remove(c);
                            container.getCallWindow().close(container);
                        }
                    }
                }

                activeCalls.remove(sourceCall);

                callContainer.getCallWindow().closeWait(callContainer);
                List<CallPanel> calls = new ArrayList<CallPanel>(
                    activeCalls.values());

                for(CallPanel cp : calls)
                {
                    if(cp != null)
                        cp.incomingCallReceived(event);
                }
            }
        }

        /**
         * Creates and opens a call dialog. Implements
         * CallListener.outGoingCallCreated.
         * @param event the <tt>CallEvent</tt>
         */
        public void outgoingCallCreated(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();

            CallManager.openCallContainer(sourceCall);

            List<CallPanel> calls = new ArrayList<CallPanel>(
                activeCalls.values());

            for(CallPanel cp : calls)
            {
                if(cp != null)
                    cp.incomingCallReceived(event);
            }
        }
    }

    /**
     * Answers the given call.
     *
     * @param call the call to answer
     */
    public static void answerCall(final Call call)
    {
        CallManager.openCallContainer(call);

        new AnswerCallThread(call).start();
    }

    /**
     * Answers the given call in an existing call. It will end up with a
     * conference call.
     *
     * @param call the call to answer
     */
    public static void answerCallInFirstExistingCall(final Call call)
    {
        Call existingCall = null;

        // pick up the first available call
        for(Call c : activeCalls.keySet())
        {
            existingCall = c;
            break;
        }

        if(existingCall == null)
        {
            answerCall(call);
        }
        else
        {
            new AnswerCallThread(call, existingCall).start();
        }
    }

    /**
     * Merge two existing <tt>Call</tt>s into a single conference call.
     *
     * @param first first call
     * @param calls list of calls
     */
    public static void mergeExistingCall(final Call first,
        final Collection<Call> calls)
    {
        new MergeExistingCalls(first, calls).start();
    }

    /**
     * Answers the given call with video.
     *
     * @param call the call to answer
     */
    public static void answerVideoCall(final Call call)
    {
        CallManager.openCallContainer(call);

        new AnswerVideoCallThread(call).start();
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
        new CreateCallThread(protocolProvider, contact).start();
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
        new CreateCallThread(protocolProvider, contact).start();
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
        new CreateVideoCallThread(protocolProvider, contact).start();
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
        new CreateVideoCallThread(protocolProvider, contact).start();
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

        if (deviceNumber == 1)
        {
            createDesktopSharing(protocolProvider, contact,
                mediaService.getMediaDeviceForPartialDesktopStreaming(
                    width, height, x, y));
        }
        else if (deviceNumber > 1)
        {
            createDesktopSharing(protocolProvider, contact,
                mediaService.getMediaDeviceForPartialDesktopStreaming(
                    width, height, x, y));
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

        if (deviceNumber == 1)
        {
            enableDesktopSharing(call,
                mediaService.getMediaDeviceForPartialDesktopStreaming(
                    width, height, x, y), true);
        }
        else if (deviceNumber > 1)
        {
            enableDesktopSharing(call,
                mediaService.getMediaDeviceForPartialDesktopStreaming(
                    width, height, x, y), true);
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
            // First make sure to disable the local video if it's currently
            // enabled.
            if (enable && isLocalVideoEnabled(call))
                getActiveCallContainer(call).setVideoButtonSelected(false);

            try
            {
                if (mediaDevice != null)
                    desktopOpSet.setLocalVideoAllowed(
                        call,
                        mediaDevice,
                        enable);
                else
                    desktopOpSet.setLocalVideoAllowed(
                        call,
                        enable);

                enableSucceeded = true;
            }
            catch (OperationFailedException ex)
            {
                logger.error(
                    "Failed to toggle the streaming of local video.", ex);
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
        {
            String normalizedContact[] = new String[1];
            normalizedContact[0] = callString;

            normalizePhoneNumbers(normalizedContact);
            callString = normalizedContact[0];
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
            new ErrorDialog(
                    null,
                    GuiActivator.getResources().getI18NString(
                            "service.gui.WARNING"),
                    GuiActivator.getResources().getI18NString(
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
        new CreateConferenceCallThread(callees, protocolProvider).start();
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
        new InviteToConferenceCallThread(callees, call).start();
    }

    /**
     * Invites the given list of <tt>callees</tt> to the given conference
     * <tt>call</tt>.
     *
     * @param callees the list of contacts to invite
     * @param call existing call
     */
    public static void inviteToCrossProtocolConferenceCall(
        Map<ProtocolProviderService, List<String>> callees,
        Call call)
    {
        new InviteToCrossProtocolConferenceCallThread(callees, call).
            start();
    }

    /**
     * Create a call to the given list of contacts.
     *
     * @param callees the list of contacts to invite
     */
    public static void createCrossProtocolConferenceCall(
        Map<ProtocolProviderService, List<String>> callees)
    {
        new CreateCrossProtocolConferenceCallThread(callees).start();
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
     * Opens a call container for the given call.
     *
     * @param call the call object to pass to the call container
     *
     * @return the created and opened call container
     */
    private static CallPanel openCallContainer(Call call)
    {
        // If we're in a single window mode we just return the single window
        // call container.
        CallContainer callContainer
            = GuiActivator.getUIService().getSingleWindowContainer();

        if (callContainer == null)
            // If we're in a multi-window mode we create the CallDialog.
            callContainer = new CallDialog();

        CallPanel callPanel = new CallPanel(call, callContainer);

        activeCalls.put(call, callPanel);

        callContainer.addCallPanel(callPanel);

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
            dialog.setModal(true);
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
                    catch(OperationFailedException e)
                    {
                        logger.info("Unable to change video quality.", e);

                        new ErrorDialog(
                                null,
                                GuiActivator.getResources()
                                    .getI18NString("service.gui.WARNING"),
                                GuiActivator.getResources().getI18NString(
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
     * Creates a call from a given Contact or a given String.
     */
    private static class CreateCallThread
        extends Thread
    {
        private final String stringContact;

        private final Contact contact;

        private final ProtocolProviderService protocolProvider;

        public CreateCallThread(ProtocolProviderService protocolProvider,
                                String contact)
        {
            this.protocolProvider = protocolProvider;
            this.stringContact = contact;
            this.contact = null;
        }

        public CreateCallThread(ProtocolProviderService protocolProvider,
                                Contact contact)
        {
            this.protocolProvider = protocolProvider;
            this.contact = contact;
            this.stringContact = null;
        }

        @Override
        public void run()
        {
            OperationSetBasicTelephony<?> telephonyOpSet
                = protocolProvider.getOperationSet(
                        OperationSetBasicTelephony.class);

            /*
             * XXX If we are here and we just discover that
             * OperationSetBasicTelephony is not supported, then we're already
             * in trouble. At the very least, we've already started a whole new
             * thread just to check that a reference is null.
             */
            if (telephonyOpSet == null)
                return;

            String callString = null;

            if(contact != null)
                callString = contact.getAddress();
            else if(stringContact != null)
                callString = stringContact;

            if(ConfigurationManager.isNormalizePhoneNumber())
            {
                String normalizedContact[] = new String[1];

                normalizedContact[0] = callString;
                normalizePhoneNumbers(normalizedContact);
                callString = normalizedContact[0];
            }

            try
            {
                telephonyOpSet.createCall(callString);
            }
            catch (Throwable exception)
            {
                logger.error("The call could not be created: ", exception);

                new ErrorDialog(
                        null,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        exception.getMessage(),
                        exception)
                    .showDialog();
            }
        }
    }

    /**
     * Creates a video call from a given Contact or a given String.
     */
    private static class CreateVideoCallThread
        extends Thread
    {
        private final String stringContact;

        private final Contact contact;

        private final ProtocolProviderService protocolProvider;

        public CreateVideoCallThread(ProtocolProviderService protocolProvider,
                                    String contact)
        {
            this.protocolProvider = protocolProvider;
            this.stringContact = contact;
            this.contact = null;
        }

        public CreateVideoCallThread(ProtocolProviderService protocolProvider,
                                    Contact contact)
        {
            this.protocolProvider = protocolProvider;
            this.contact = contact;
            this.stringContact = null;
        }

        @Override
        public void run()
        {
            OperationSetVideoTelephony videoTelOpSet
                = protocolProvider.getOperationSet(
                        OperationSetVideoTelephony.class);

            /*
             * XXX If we are here and we just discover that
             * OperationSetVideoTelephony is not supported, then we're already
             * in trouble. At the very least, we've already started a whole new
             * thread just to check that a reference is null.
             */
            if (videoTelOpSet == null)
                return;

            Throwable exception = null;

            try
            {
                if (contact != null)
                    videoTelOpSet.createVideoCall(contact);
                else if (stringContact != null)
                    videoTelOpSet.createVideoCall(stringContact);
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
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        exception.getMessage(),
                        ErrorDialog.ERROR)
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
             * already in trouble. At the very least, we've already started a
             * whole new thread just to check that a reference is null.
             */
            if (desktopSharingOpSet == null)
                return;

            Throwable exception = null;

            try
            {
                if (mediaDevice != null)
                    desktopSharingOpSet
                        .createVideoCall(stringContact, mediaDevice);
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
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        exception.getMessage(),
                        ErrorDialog.ERROR)
                    .showDialog();
            }
        }
    }

    /**
     * Answers all call peers in the given call.
     */
    private static class AnswerCallThread
        extends Thread
    {
        private final Call call;

        private final Call existingCall;

        public AnswerCallThread(Call call)
        {
            this.call = call;
            this.existingCall = null;
        }

        public AnswerCallThread(Call call, Call existingCall)
        {
            this.call = call;
            this.existingCall = existingCall;
        }

        @Override
        public void run()
        {
            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<? extends CallPeer> peers = call.getCallPeers();
            CallGroup group = null;

            if(existingCall != null)
            {
                if(existingCall.getCallGroup() == null)
                {
                    group = new CallGroup();
                    group.addCall(existingCall);
                }
                else
                {
                    group = existingCall.getCallGroup();
                }

                if(call.getCallGroup() == null && group != null)
                    group.addCall(call);
            }

            while (peers.hasNext())
            {
                CallPeer peer = peers.next();
                OperationSetBasicTelephony<?> telephony =
                    pps.getOperationSet(OperationSetBasicTelephony.class);

                try
                {
                    telephony.answerCallPeer(peer);
                }
                catch (OperationFailedException e)
                {
                    logger.error("Could not answer to : " + peer
                        + " caused by the following exception: " + e);
                }
            }
        }
    }

    /**
     * Answers all call peers in the given call with video.
     */
    private static class AnswerVideoCallThread
        extends Thread
    {
        private final Call call;

        /**
         * Constructor.
         *
         * @param call source call
         */
        public AnswerVideoCallThread(Call call)
        {
            this.call = call;
        }

        @Override
        public void run()
        {
            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<? extends CallPeer> peers = call.getCallPeers();

            while (peers.hasNext())
            {
                CallPeer peer = peers.next();
                OperationSetVideoTelephony telephony =
                    pps.getOperationSet(OperationSetVideoTelephony.class);

                try
                {
                    telephony.answerVideoCallPeer(peer);
                }
                catch (OperationFailedException e)
                {
                    logger.error("Could not video answer to : " + peer
                        + " caused by the following exception: " + e);
                }
            }
        }
    }

    /**
     * Creates a conference call from a given list of contact addresses
     */
    private static class CreateConferenceCallThread
        extends Thread
    {
        private final String[] callees;

        private final ProtocolProviderService protocolProvider;

        public CreateConferenceCallThread(
                String[] callees,
                ProtocolProviderService protocolProvider)
        {
            this.callees = callees;
            this.protocolProvider = protocolProvider;
        }

        @Override
        public void run()
        {
            OperationSetTelephonyConferencing confOpSet
                = protocolProvider.getOperationSet(
                    OperationSetTelephonyConferencing.class);

            /*
             * XXX If we are here and we just discover that
             * OperationSetTelephonyConferencing is not supported, then we're
             * already in trouble. At the very least, we've already started a
             * whole new thread just to check that a reference is null.
             */
            if (confOpSet == null)
                return;

            Throwable exception = null;

            if (ConfigurationManager.isNormalizePhoneNumber())
            {
                normalizePhoneNumbers(callees);
            }

            try
            {
                confOpSet.createConfCall(callees);
            }
            catch (OperationFailedException ofe)
            {
                exception = ofe;
            }
            catch (OperationNotSupportedException onse)
            {
                exception = onse;
            }
            catch (IllegalArgumentException iae)
            {
                exception = iae;
            }
            if (exception != null)
            {
                logger.error("Failed to create conference call. " + exception);

                new ErrorDialog(
                        null,
                        GuiActivator
                            .getResources().getI18NString("service.gui.ERROR"),
                        exception.getMessage(),
                        ErrorDialog.ERROR)
                    .showDialog();
            }
        }
    }

    /**
     * Invites a list of callees to a conference call.
     */
    private static class InviteToConferenceCallThread
        extends Thread
    {
        private final String[] callees;

        private final Call call;

        public InviteToConferenceCallThread(String[] callees, Call call)
        {
            this.callees = callees;
            this.call = call;
        }

        @Override
        public void run()
        {
            OperationSetTelephonyConferencing confOpSet
                = call.getProtocolProvider()
                    .getOperationSet(
                            OperationSetTelephonyConferencing.class);

            /*
             * XXX If we are here and we just discover that
             * OperationSetTelephonyConferencing is not supported, then we're
             * already in trouble. At the very least, we've already started a
             * whole new thread just to check that a reference is null.
             */
            if (confOpSet == null)
                return;

            if (ConfigurationManager.isNormalizePhoneNumber())
            {
                normalizePhoneNumbers(callees);
            }

            for (String callee : callees)
            {
                Throwable exception = null;

                try
                {
                    confOpSet.inviteCalleeToCall(callee, call);
                }
                catch (OperationFailedException ofe)
                {
                    exception = ofe;
                }
                catch (OperationNotSupportedException onse)
                {
                    exception = onse;
                }
                catch (IllegalArgumentException iae)
                {
                    exception = iae;
                }
                if (exception != null)
                {
                    logger
                        .error("Failed to invite callee: " + callee, exception);

                    new ErrorDialog(
                            null,
                            GuiActivator
                                .getResources()
                                    .getI18NString("service.gui.ERROR"),
                            exception.getMessage(),
                            ErrorDialog.ERROR)
                        .showDialog();
                }
            }
        }
    }

    /**
     * Invites a list of callees to a conference call.
     */
    private static class InviteToCrossProtocolConferenceCallThread
        extends Thread
    {
        private final Map<ProtocolProviderService, List<String>>
            callees;

        private final Call call;

        public InviteToCrossProtocolConferenceCallThread(
            Map<ProtocolProviderService, List<String>> callees,
            Call call)
        {
            this.callees = callees;
            this.call = call;
        }

        @Override
        public void run()
        {
            for(Map.Entry<ProtocolProviderService, List<String>> entry :
                callees.entrySet())
            {
                ProtocolProviderService provider = entry.getKey();
                List<String> contactList = entry.getValue();

                OperationSetBasicTelephony<?> opSetTelephony
                    = provider.getOperationSet(
                            OperationSetBasicTelephony.class);

                if(opSetTelephony != null)
                {
                    OperationSetTelephonyConferencing opSetConf
                        = provider.getOperationSet(
                                OperationSetTelephonyConferencing.class);

                    String[] contactArray
                        = contactList.toArray(new String[contactList.size()]);

                    if (ConfigurationManager.isNormalizePhoneNumber())
                        normalizePhoneNumbers(contactArray);

                    /* Try to have a single Call per ProtocolProviderService. */
                    Call providerCall = null;
                    CallGroup group = null;

                    if (provider.equals(call.getProtocolProvider()))
                        providerCall = call;
                    else
                    {
                        group = call.getCallGroup();
                        if (group == null)
                        {
                            group = new CallGroup();
                            group.addCall(call);
                            call.setCallGroup(group);
                        }
                        else
                        {
                            for (Call groupCall : group.getCalls())
                            {
                                if (provider.equals(
                                        groupCall.getProtocolProvider()))
                                {
                                    providerCall = groupCall;
                                    break;
                                }
                            }
                        }
                    }

                    try
                    {
                        if (providerCall == null)
                        {
                            opSetConf.createConfCall(
                                    contactArray,
                                    group);
                        }
                        else
                        {
                            for (String contact : contactArray)
                                opSetConf.inviteCalleeToCall(
                                        contact,
                                        providerCall);
                        }
                    }
                    catch(Exception exception)
                    {
                        logger
                            .error("Failed to invite callees",
                                exception);

                        new ErrorDialog(
                                null,
                                GuiActivator
                                    .getResources()
                                        .getI18NString("service.gui.ERROR"),
                                exception.getMessage(),
                                ErrorDialog.ERROR)
                            .showDialog();
                    }
                }
            }
        }
    }

    /**
     * Invites a list of callees to a conference call.
     */
    private static class CreateCrossProtocolConferenceCallThread
        extends Thread
    {
        private final Map<ProtocolProviderService, List<String>>
            callees;

        public CreateCrossProtocolConferenceCallThread(
            Map<ProtocolProviderService, List<String>> callees)
        {
            this.callees = callees;
        }

        @Override
        public void run()
        {
            CallGroup group = new CallGroup();

            for(Map.Entry<ProtocolProviderService, List<String>> entry :
                callees.entrySet())
            {
                ProtocolProviderService provider = entry.getKey();
                List<String> contacts = entry.getValue();

                OperationSetBasicTelephony<?> opSetTelephony =
                    provider.getOperationSet(OperationSetBasicTelephony.class);

                if(opSetTelephony != null)
                {
                    OperationSetTelephonyConferencing opSetConf =
                        provider.getOperationSet(
                            OperationSetTelephonyConferencing.class);

                    String[] contactAddressStrings =
                        new String[contacts.size()];
                    contacts.toArray(contactAddressStrings);

                    if (ConfigurationManager.isNormalizePhoneNumber())
                    {
                        normalizePhoneNumbers(contactAddressStrings);
                    }

                    try
                    {
                        opSetConf.createConfCall(contactAddressStrings, group);
                    }
                    catch(Exception exception)
                    {
                        logger
                            .error("Failed to invite callees",
                                exception);

                        new ErrorDialog(
                                null,
                                GuiActivator
                                    .getResources()
                                        .getI18NString("service.gui.ERROR"),
                                exception.getMessage(),
                                ErrorDialog.ERROR)
                            .showDialog();
                    }
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
            Iterator<? extends CallPeer> peers = null;

            if(call.getCallGroup() != null)
            {
                List<Call> calls = call.getCallGroup().getCalls();

                for(Call c : calls)
                {
                    peers = c.getCallPeers();

                    while(peers.hasNext())
                    {
                        CallPeer peer = peers.next();
                        OperationSetBasicTelephony<?> telephony
                            = peer.getCall().getProtocolProvider().
                            getOperationSet(OperationSetBasicTelephony.class);

                        try
                        {
                            telephony.hangupCallPeer(peer);
                        }
                        catch (OperationFailedException e)
                        {
                            logger.error("Could not hang up : " + peer
                                + " caused by the following exception: " + e);
                        }
                    }
                }
            }
            else
            {
                ProtocolProviderService pps = call.getProtocolProvider();
                peers = call.getCallPeers();

                while (peers.hasNext())
                {
                    CallPeer peer = peers.next();
                    OperationSetBasicTelephony<?> telephony
                        = pps.getOperationSet(OperationSetBasicTelephony.class);

                    try
                    {
                        telephony.hangupCallPeer(peer);
                    }
                    catch (OperationFailedException e)
                    {
                        logger.error("Could not hang up : " + peer
                            + " caused by the following exception: " + e);
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
            // First disable desktop sharing if it's currently enabled.
            if (enable && isDesktopSharingEnabled(call))
            {
                getActiveCallContainer(call).setDesktopSharingButtonSelected(
                        false);

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

        // If the operation didn't succeeded for some reason we make sure
        // to unselect the video button.
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
                String callPeerAddress = callPeer.getAddress();

                if (isOnHold)
                    logger.error("Failed to put"
                        + callPeerAddress + " on hold.", ex);
                else
                    logger.error("Failed to put"
                        + callPeerAddress + " off hold.", ex);
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
         * @param calls list of cals
         */
        public MergeExistingCalls(final Call first,
            final Collection<Call> calls)
        {
            this.first = first;
            this.calls = calls;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
        {
            // unselect onhold
            Iterator<? extends CallPeer> peers = first.getCallPeers();
            OperationSetBasicTelephony<?> telephony
                = first.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            while (peers.hasNext())
            {
                CallPeer callPeer = peers.next();
                boolean putOffHold = true;

                if(callPeer instanceof MediaAwareCallPeer)
                {
                    putOffHold = ((MediaAwareCallPeer<?,?,?>)callPeer).
                        getMediaHandler().isLocallyOnHold();
                }

                if(putOffHold)
                {
                    try
                    {
                        telephony.putOffHold(callPeer);
                        Thread.sleep(400);
                    }
                    catch(Exception ofex)
                    {
                        logger.error(
                                "Failed to put off hold.",
                                ofex);
                    }
                }
            }

            for(Call c : calls)
            {
                if(c == first  || (first.getCallGroup() != null &&
                    c.getCallGroup() == first.getCallGroup()))
                    continue;

                peers = c.getCallPeers();
                telephony = c.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);

                while (peers.hasNext())
                {
                    CallPeer callPeer = peers.next();
                    boolean putOffHold = true;

                    if(callPeer instanceof MediaAwareCallPeer)
                    {
                        putOffHold = ((MediaAwareCallPeer<?,?,?>)callPeer).
                            getMediaHandler().isLocallyOnHold();
                    }

                    if(putOffHold)
                    {
                        try
                        {
                            telephony.putOffHold(callPeer);
                            Thread.sleep(400);
                        }
                        catch(Exception ofex)
                        {
                            logger.error(
                                    "Failed to put off hold.",
                                    ofex);
                        }
                    }
                }

                // dispose existing CallPanel
                CallPanel callPanel = CallManager.getActiveCallContainer(c);
                callPanel.getCallWindow().close(callPanel);

                CallGroup group = first.getCallGroup();
                if(group == null)
                {
                    group = new CallGroup();
                    group.addCall(first);
                    first.setCallGroup(group);
                }

                group.addCall(c);
                group.fireCallGroupEvent(c,
                    CallGroupEvent.CALLGROUP_CALL_ADDED);
            }
        }
    }

    /**
     * Shows a warning window to warn the user that she's about to start a
     * desktop sharing session.
     *
     * @return <tt>true</tt> if the user has accepted the desktop sharing
     * session, <tt>false</tt> - otherwise
     */
    private static boolean showDesktopSharingWarning()
    {
        Boolean isWarningEnabled = GuiActivator.getConfigurationService()
            .getBoolean(desktopSharingWarningProperty, true);

        if (isWarningEnabled.booleanValue())
        {
            MessageDialog warningDialog = new MessageDialog(null,
                GuiActivator.getResources().getI18NString(
                    "service.gui.WARNING"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.DESKTOP_SHARING_WARNING"),
                true);

            int result = warningDialog.showDialog();

            switch (result)
            {
                case MessageDialog.OK_RETURN_CODE:
                    return true;
                case MessageDialog.CANCEL_RETURN_CODE:
                    return false;
                case MessageDialog.OK_DONT_ASK_CODE:
                    GuiActivator.getConfigurationService()
                        .setProperty(desktopSharingWarningProperty, false);
                    return true;
            }
        }

        return true;
    }

    /**
     * Normalizes the phone numbers (if any) in a list of <tt>String</tt>s.
     *
     * @param callees list of contact addresses or phone numbers
     */
    private static void normalizePhoneNumbers(String callees[])
    {
        PhoneNumberI18nService phoneNumberService =
            GuiActivator.getPhoneNumberService();

        for(int i = 0 ; i < callees.length ; i++)
        {
            if (!StringUtils.containsLetters(callees[i]) &&
                GuiActivator.getPhoneNumberService().isPhoneNumber(callees[i]))
            {
                String addr = callees[i];
                if(phoneNumberService.isPhoneNumber(addr))
                {
                    addr = phoneNumberService.normalize(addr);
                }
                else
                {
                    addr = StringUtils.concatenateWords(callees[i]);
                }

                callees[i] = addr;
            }
        }
    }
}
