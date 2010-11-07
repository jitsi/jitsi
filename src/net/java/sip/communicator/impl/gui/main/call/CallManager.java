/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.text.*;
import java.util.*;

import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

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
    private static Hashtable<Call, CallDialog> activeCalls
                                            = new Hashtable<Call, CallDialog>();

    /**
     * A list of the currently missed calls. Only the names of the first
     * participant of each call is stored in the list.
     */
    private static Collection<MissedCall> missedCalls;

    /**
     * Listener notified for changes in missed calls count.
     */
    private static MissedCallsListener missedCallsListener;

    /**
     * Indicates if an outgoing call is a desktop sharing.
     */
    private static boolean isDesktopSharing = false;

    /**
     * The property indicating if the user should be warned when starting a
     * desktop sharing session.
     */
    private static final String desktopSharingWarningProperty
        = "net.java.sip.communicator.impl.gui.main"
            + ".call.SHOW_DESKTOP_SHARING_WARNING";

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
        public void incomingCallReceived(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();

            ReceivedCallDialog receivedCallDialog
                = new ReceivedCallDialog(sourceCall);

            receivedCallDialog.pack();
            receivedCallDialog.setVisible(true);

            final String peerName
                = sourceCall.getCallPeers().next().getDisplayName();
            final Date callDate = new Date();

            NotificationManager.fireNotification(
                NotificationManager.INCOMING_CALL,
                "",
                GuiActivator.getResources()
                    .getI18NString("service.gui.INCOMING_CALL",
                        new String[]{peerName}));

            sourceCall.addCallChangeListener(new CallChangeAdapter()
            {
                @Override
                public void callStateChanged(CallChangeEvent evt)
                {
                    if (evt.getNewValue().equals(CallState.CALL_ENDED)
                        && evt.getOldValue()
                            .equals(CallState.CALL_INITIALIZATION))
                    {
                        // if call was answered elsewhere, don't add it
                        // to missed calls
                        if(evt.getCause() == null
                           || (evt.getCause().getReasonCode() !=
                                CallPeerChangeEvent.NORMAL_CALL_CLEARING))
                            addMissedCall(new MissedCall(peerName, callDate));

                        evt.getSourceCall().removeCallChangeListener(this);
                    }
                }
            });
        }

        /**
         * Implements CallListener.callEnded. Stops sounds that are playing at
         * the moment if there're any. Removes the call panel and disables the
         * hang up button.
         * @param event the <tt>CallEvent</tt>
         */
        public void callEnded(CallEvent event)
        {
            Call sourceCall = event.getSourceCall();

            // Stop all telephony related sounds.
            stopAllSounds();

            // Play the hangup sound.
            NotificationManager.fireNotification(NotificationManager.HANG_UP);

            if (activeCalls.get(sourceCall) != null)
            {
                CallDialog callDialog = activeCalls.get(sourceCall);

                disposeCallDialogWait(callDialog);
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

            CallManager.openCallDialog(sourceCall, isDesktopSharing);

            isDesktopSharing = false;
        }
    }

    /**
     * Removes the given call panel tab.
     *
     * @param callDialog the CallDialog to remove
     */
    public static void disposeCallDialogWait(CallDialog callDialog)
    {
        Timer timer
            = new Timer(5000, new DisposeCallDialogListener(callDialog));

        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Removes the given CallPanel from the main tabbed pane.
     */
    private static class DisposeCallDialogListener
        implements ActionListener
    {
        private final CallDialog callDialog;

        public DisposeCallDialogListener(CallDialog callDialog)
        {
            this.callDialog = callDialog;
        }

        public void actionPerformed(ActionEvent e)
        {
            callDialog.dispose();

            Call call = callDialog.getCall();

            if(call != null)
                activeCalls.remove(call);
        }
    }

    /**
     * Answers the given call.
     *
     * @param call the call to answer
     */
    public static void answerCall(final Call call)
    {
        CallManager.openCallDialog(call, false);

        new AnswerCallThread(call).start();
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
        stopAllSounds();

        NotificationManager.fireNotification(NotificationManager.HANG_UP);

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
     * Creates a call to the contact represented by the given string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     */
    public static void createVideoCall( ProtocolProviderService protocolProvider,
                                        String contact)
    {
        new CreateVideoCallThread(protocolProvider, contact).start();
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
        // Use the default media device corresponding to the screen to share
        createDesktopSharing(protocolProvider, contact, null);
    }

    /**
     * Creates a desktop sharing call to the contact represented by the given
     * string.
     *
     * @param protocolProvider the protocol provider to which this call belongs.
     * @param contact the contact to call to
     * @param mediaDevice the media device corresponding to the screen to share
     */
    public static void createDesktopSharing(
                                    ProtocolProviderService protocolProvider,
                                    String contact,
                                    MediaDevice mediaDevice)
    {
        if (showDesktopSharingWarning())
        {
            // Indicate to the outgoing call event which will be received later
            // that this is a desktop sharing call.
            isDesktopSharing = true;

            new CreateDesktopSharingThread( protocolProvider,
                                            contact,
                                            mediaDevice).start();
        }
    }

    /**
     * Enables the desktop sharing in an existing <tt>call</tt>.
     *
     * @param call the call for which desktop sharing should be enabled
     * @param isEnable indicates if the desktop sharing should be enabled or
     * disabled
     */
    public static void enableDesktopSharing(Call call, boolean isEnable)
    {
        enableDesktopSharing(call, null, isEnable);
    }

    /**
     * Enables the desktop sharing in an existing <tt>call</tt>.
     *
     * @param call the call for which desktop sharing should be enabled
     * @param mediaDevice the media device corresponding to the screen to share
     * @param isEnable indicates if the desktop sharing should be enabled or
     * disabled
     */
    public static void enableDesktopSharing(Call call,
                                            MediaDevice mediaDevice,
                                            boolean isEnable)
    {
        OperationSetDesktopSharingServer desktopOpSet
            = call.getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingServer.class);

        // This shouldn't happen at this stage, because we disable the button
        // if the operation set isn't available.
        if (desktopOpSet == null)
            return;

        if (!isEnable || showDesktopSharingWarning())
        {
            try
            {
                boolean isDesktopSharing
                    = desktopOpSet.isLocalVideoAllowed(call);

                CallDialog callDialog = activeCalls.get(call);
                callDialog.setDesktopSharing(!isDesktopSharing);

                if (mediaDevice != null)
                    desktopOpSet.setLocalVideoAllowed(
                        call,
                        mediaDevice,
                        !isDesktopSharing);
                else
                    desktopOpSet.setLocalVideoAllowed(
                        call,
                        !isDesktopSharing);
            }
            catch (OperationFailedException ex)
            {
                logger.error(
                    "Failed to toggle the streaming of local video.", ex);
            }
        }
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
     * Creates a call to the contact represented by the given string through the
     * default (most connected) protocol provider. If none of the providers is
     * registered or online does nothing.
     *
     * @param contact the contact to call to
     */
    public static void createCall(String contact)
    {
        ProtocolProviderService telProvider = null;
        int status = 0;

        List<ProtocolProviderService> telProviders = getTelephonyProviders();

        for (ProtocolProviderService provider : telProviders)
        {
            if (!provider.isRegistered())
                continue;

            OperationSetPresence presence
                = provider.getOperationSet(OperationSetPresence.class);

            int presenceStatus
                = (presence == null)
                    ? PresenceStatus.AVAILABLE_THRESHOLD
                    : presence.getPresenceStatus().getStatus();

            if (status < presenceStatus)
            {
                status = presenceStatus;
                telProvider = provider;
            }
        }

        if (status >= PresenceStatus.ONLINE_THRESHOLD)
            new CreateCallThread(telProvider, contact).start();
        else
        {
            logger.error("There's no online telephony"
                        + " provider to create this call.");

            new ErrorDialog(
                    null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.WARNING"),
                    GuiActivator.getResources()
                        .getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"),
                    ErrorDialog.WARNING)
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
     * Opens a call dialog.
     *
     * @param call the call object to pass to the call dialog
     * @param isDesktopSharing indicates if the dialog to open is for desktop
     * sharing
     *
     * @return the opened call dialog
     */
    public static CallDialog openCallDialog(Call call, boolean isDesktopSharing)
    {
        CallDialog callDialog = new CallDialog(call, isDesktopSharing);

        activeCalls.put(call, callDialog);

        callDialog.setVisible(true, true);

        return callDialog;
    }

    /**
     * Returns a list of all currently registered telephony providers.
     * @return a list of all currently registered telephony providers
     */
    public static List<ProtocolProviderService> getTelephonyProviders()
    {
        List<ProtocolProviderService> telephonyProviders
            = new LinkedList<ProtocolProviderService>();

        for (ProtocolProviderFactory providerFactory : GuiActivator
            .getProtocolProviderFactories().values())
        {
            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider
                    = (ProtocolProviderService) GuiActivator.bundleContext
                        .getService(serRef);

                if (protocolProvider.getOperationSet(
                        OperationSetBasicTelephony.class) != null
                    && protocolProvider.isRegistered())
                {
                    telephonyProviders.add(protocolProvider);
                }
            }
        }
        return telephonyProviders;
    }

    /**
     * Returns a list of all currently registered telephony providers for the
     * given protocol name.
     * @param protocolName the protocol name
     * @param operationSetClass the operation set class for which we're looking
     * for providers
     * @return a list of all currently registered providers for the given
     * <tt>protocolName</tt> and supporting the given <tt>operationSetClass</tt>
     */
    public static List<ProtocolProviderService> getRegisteredProviders(
        String protocolName, Class<? extends OperationSet> operationSetClass)
    {
        List<ProtocolProviderService> telephonyProviders
            = new LinkedList<ProtocolProviderService>();

        ProtocolProviderFactory providerFactory
            = GuiActivator.getProtocolProviderFactory(protocolName);

        if (providerFactory != null)
        {
            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider
                    = (ProtocolProviderService) GuiActivator.bundleContext
                        .getService(serRef);

                if (protocolProvider.getOperationSet(operationSetClass) != null
                    && protocolProvider.isRegistered())
                {
                    telephonyProviders.add(protocolProvider);
                }
            }
        }
        return telephonyProviders;
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all currently active calls.
     * @return an <tt>Iterator</tt> over a list of all currently active calls
     */
    public static Iterator<Call> getActiveCalls()
    {
        return activeCalls.keySet().iterator();
    }

    /**
     * Sets the given <tt>MissedCallsListener</tt> that would be notified on
     * any changes in missed calls count.
     * @param l the listener to set
     */
    public static void setMissedCallsListener(MissedCallsListener l)
    {
        missedCallsListener = l;
    }

    /**
     * Adds a missed call.
     * @param missedCall the missed call to add to the list of missed calls
     */
    private static void addMissedCall(MissedCall missedCall)
    {
        if (missedCalls == null)
        {
            missedCalls = new LinkedList<MissedCall>();
        }

        missedCalls.add(missedCall);
        fireMissedCallCountChangeEvent(missedCalls);
    }

    /**
     * Clears the count of missed calls. Sets it to 0.
     */
    public static void clearMissedCalls()
    {
        missedCalls = null;
    }

    /**
     * Notifies interested <tt>MissedCallListener</tt> that the count has
     * changed.
     * @param missedCalls the new missed calls
     */
    private static void fireMissedCallCountChangeEvent(
        Collection<MissedCall> missedCalls)
    {
        if (missedCallsListener != null)
            missedCallsListener.missedCallCountChanged(missedCalls);
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
            OperationSetBasicTelephony telephonyOpSet
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

            Throwable exception = null;

            try
            {
                if (contact != null)
                    telephonyOpSet.createCall(contact);
                else if (stringContact != null)
                    telephonyOpSet.createCall(stringContact);
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

        public AnswerCallThread(Call call)
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
            ProtocolProviderService pps = call.getProtocolProvider();
            Iterator<? extends CallPeer> peers = call.getCallPeers();

            while (peers.hasNext())
            {
                CallPeer peer = peers.next();
                OperationSetBasicTelephony telephony
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
            OperationSetBasicTelephony telephony
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
            OperationSetBasicTelephony telephony
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
     * Stops all telephony related sounds.
     */
    private static void stopAllSounds()
    {
        NotificationManager.stopSound(NotificationManager.DIALING);
        NotificationManager.stopSound(NotificationManager.BUSY_CALL);
        NotificationManager.stopSound(NotificationManager.INCOMING_CALL);
        NotificationManager.stopSound(NotificationManager.OUTGOING_CALL);
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
                case 0:
                    return true;
                case 1:
                    return false;
                case 2:
                    GuiActivator.getConfigurationService()
                        .setProperty(desktopSharingWarningProperty, false);
                    return true;
            }
        }

        return true;
    }
}
