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
package net.java.sip.communicator.impl.protocol.sip;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Queue;

import javax.sip.*;
import javax.sip.Dialog;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
// disambiguation

/**
 * Implements all desktop sharing client-side related functions for SIP
 * protocol.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopSharingClientSipImpl
    extends AbstractOperationSetDesktopSharingClient
                <ProtocolProviderServiceSipImpl>
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetDesktopSharingClientSipImpl.class);

    /**
     * The <tt>CallPeerListener</tt> which listens to modifications in the
     * properties/state of <tt>CallPeer</tt>.
     */
    private final CallPeerListener callPeerListener = new CallPeerAdapter()
    {
        /**
         * Indicates that a change has occurred in the status of the source
         * <tt>CallPeer</tt>.
         *
         * @param evt the <tt>CallPeerChangeEvent</tt> instance containing the
         * source event as well as its previous and its new status
         */
        @Override
        public void peerStateChanged(CallPeerChangeEvent evt)
        {
            CallPeer peer = evt.getSourceCallPeer();
            CallPeerState state = peer.getState();

            if (CallPeerState.DISCONNECTED.equals(state)
                    || CallPeerState.FAILED.equals(state))
            {
                /* if the peer is disconnected or call has failed, remove
                 * corresponding subscription.
                 */
                try
                {
                    notifier.removeSubscription(parentProvider.
                            parseAddressString(peer.getAddress()));
                }
                catch(ParseException ex)
                {
                }
            }
        }
    };

    /**
     * The <tt>EventPackageNotifier</tt> which implements remote-control
     * event-package notifier support on behalf of this
     * <tt>OperationSetDesktopSharingClient</tt> instance.
     */
    private final EventPackageNotifier notifier;

    /**
     * The <tt>Timer</tt> which executes delayed tasks scheduled by
     * {@link #notifier}.
     */
    private final TimerScheduler timer = new TimerScheduler();

    /**
     * List of SIP NOTIFY messages.
     */
    private final Queue<String> inputEvents = new LinkedList<String>();

    /**
     * Synchronization object for {@link #inputEvents} access.
     */
    private final Object inputSync = new Object();

    /**
     * Initializes a new <tt>OperationSetDesktopSharingClientSipImpl</tt>.
     *
     * @param parentProvider the SIP <tt>ProtocolProviderService</tt>
     * implementation which has requested the creation of the new instance and
     * for which the new instance is to provide desktop sharing.
     */
    public OperationSetDesktopSharingClientSipImpl(
            ProtocolProviderServiceSipImpl parentProvider)
    {
        super(parentProvider);

        final boolean desktopControlOutOfDialogEnabled
            = SipActivator.getConfigurationService().getBoolean(
                    DesktopSharingCallSipImpl
                        .ENABLE_OUTOFDIALOG_DESKTOP_CONTROL_PROP,
                    false);

        notifier
            = new EventPackageNotifier(
                    this.parentProvider,
                    DesktopSharingProtocolSipImpl.EVENT_PACKAGE,
                    DesktopSharingProtocolSipImpl.SUBSCRIPTION_DURATION,
                    DesktopSharingProtocolSipImpl.CONTENT_SUB_TYPE,
                    this.timer)
            {
                // the received dssid from the subscription and
                // the one to be used in the notify requests
                private String dssid = null;

                @Override
                protected Subscription createSubscription(
                        Address fromAddress,
                        String eventId)
                {
                    /* new subscription received */
                    RemoteControlNotifierSubscription rcNotifierSubscription
                        = new RemoteControlNotifierSubscription(
                        fromAddress,
                        eventId);

                    if(dssid != null)
                        rcNotifierSubscription.setDSSID(dssid);

                    return rcNotifierSubscription;
                }

                /**
                 * {@inheritedDocs}.
                 */
                @Override
                public boolean processRequest(RequestEvent requestEvent)
                {
                    if(desktopControlOutOfDialogEnabled)
                    {
                        Header dssidHeader = requestEvent.getRequest()
                            .getHeader(DesktopSharingCallSipImpl.DSSID_HEADER);
                        if(dssidHeader != null)
                        {
                            dssid = dssidHeader.toString().replaceAll(
                                dssidHeader.getName() + ":", "").trim();
                        }
                    }

                    boolean ret = super.processRequest(requestEvent);
                    if(requestEvent == null || requestEvent.getDialog() == null
                        || requestEvent.getDialog().getCallId() == null)
                        return ret;

                    String callId = requestEvent.getDialog().
                        getCallId().getCallId();
                    Subscription subs = this.getSubscription(callId);

                    if(subs instanceof RemoteControlNotifierSubscription)
                    {
                        RemoteControlNotifierSubscription rcnSub
                            = (RemoteControlNotifierSubscription)subs;

                        fireRemoteControlGranted(rcnSub.getCallPeer());

                        // if we have dssid set it to notifier
                        if(dssid != null)
                            rcnSub.setDSSID(dssid);
                    }

                    return ret;
                }

                @Override
                protected void removeSubscription(
                    Response response,
                    String eventId,
                    ClientTransaction clientTransaction)
                {
                    CallIdHeader callIdHeader
                        = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
                    String callId = callIdHeader.getCallId();
                    Subscription ret = this.getSubscription(callId);

                    if(ret instanceof RemoteControlNotifierSubscription)
                    {
                        fireRemoteControlRevoked(
                            ((RemoteControlNotifierSubscription)ret).
                                getCallPeer());
                    }

                    super.removeSubscription(
                        response,
                        eventId,
                        clientTransaction);
                }

                /**
                 * Creates a NOTIFY request which is to notify about a
                 * specific subscription state and carry a specific content.
                 * This request MUST be sent using <tt>Dialog#sendRequest()</tt>
                 *
                 * @param dialog the <tt>Dialog</tt> to create the NOTIFY
                 * request in
                 * @param content the content to be carried by the NOTIFY
                 * request to be created
                 * @param subscriptionState the subscription state
                 * @param reason the reason for the specified subscription state
                 * <tt>null</tt> for no reason
                 *
                 * @return a valid <tt>ClientTransaction</tt> ready to send the
                 * request
                 *
                 * @throws OperationFailedException if something goes wrong
                 * during the creation of the request
                 */
                @Override
                protected ClientTransaction createNotify( Dialog dialog,
                                                          byte[] content,
                                                          String subscriptionState,
                                                          String reason)
                    throws OperationFailedException
                {
                    ClientTransaction res = super.createNotify(
                        dialog, content, subscriptionState, reason);

                    if(desktopControlOutOfDialogEnabled)
                    {
                        try
                        {
                            Header dssidHeader =
                                OperationSetDesktopSharingClientSipImpl.this
                                    .parentProvider.getHeaderFactory()
                                    .createHeader(
                                        DesktopSharingCallSipImpl.DSSID_HEADER,
                                        dssid);
                            res.getRequest().setHeader(dssidHeader);
                        }
                        catch(ParseException ex)
                        {
                            logger.error("error ", ex);
                        }
                    }

                    return res;
                }
            };
    }

    /**
     * Notifies all <tt>Subscription</tt>s.
     *
     * @param callPeer the <tt>CallPeer</tt> to notify
     */
    private void notifySubscriptions(final CallPeer callPeer)
    {
        EventPackageNotifier.SubscriptionFilter subscriptionFilter
            = new EventPackageNotifier.SubscriptionFilter()
        {
            public boolean accept(
                    EventPackageNotifier.Subscription subscription)
            {
                return
                    (subscription instanceof RemoteControlNotifierSubscription)
                        && callPeer.getAddress().equals(
                            ((RemoteControlNotifierSubscription)subscription).
                                getCallPeer().getAddress());

            }
        };

        try
        {
            notifier.notifyAll(SubscriptionStateHeader.ACTIVE, null,
                    subscriptionFilter);
        }
        catch (OperationFailedException ofe)
        {
            logger.error("Failed to notify the remote-control subscriptions",
                    ofe);
        }
    }

    /**
     * Send a keyboard notification.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>KeyEvent</tt> received and that will be send to
     * remote peer
     */
    public void sendKeyboardEvent(CallPeer callPeer, KeyEvent event)
    {
        /* build a SIP NOTIFY with the corresponding keyboard event
         * and send it
         */
        int keyChar = event.getKeyChar();
        int keyCode
            = (keyChar == KeyEvent.CHAR_UNDEFINED)
                ? event.getKeyCode()
                : keyChar;

        if (keyCode == 0)
            return;

        String msg;

        switch(event.getID())
        {
        case KeyEvent.KEY_TYPED:
            msg = DesktopSharingProtocolSipImpl.getKeyTypedXML(keyCode);
            break;
        case KeyEvent.KEY_PRESSED:
            msg = DesktopSharingProtocolSipImpl.getKeyPressedXML(keyCode);
            break;
        case KeyEvent.KEY_RELEASED:
            msg = DesktopSharingProtocolSipImpl.getKeyReleasedXML(keyCode);
            break;
        default:
            /* ignore */
            return;
        }

        synchronized(inputSync)
        {
            inputEvents.add(msg);
            notifySubscriptions(callPeer);
        }
    }

    /**
     * Send a mouse notification for specific "moved" <tt>MouseEvent</tt>. As
     * controller computer could have smaller desktop that controlled ones, we
     * should take care to send the percentage of point x and point y.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>MouseEvent</tt> received and that will be send to
     * remote peer
     * @param videoPanelSize size of the panel that contains video
     */
    public void sendMouseEvent(CallPeer callPeer, MouseEvent event,
            Dimension videoPanelSize)
    {
        /* build a SIP NOTIFY with the corresponding mouse event
         * and send it
         */
        String msg = null;

        if(event.getID() != MouseEvent.MOUSE_MOVED
            && event.getID() != MouseEvent.MOUSE_DRAGGED)
        {
            sendMouseEvent(callPeer, event);
            return;
        }

        Point p = event.getPoint();
        double x = (p.getX() / videoPanelSize.width);
        double y = (p.getY() / videoPanelSize.height);

        msg = DesktopSharingProtocolSipImpl.getMouseMovedXML(x, y);

        synchronized(inputSync)
        {
            inputEvents.add(msg);
            notifySubscriptions(callPeer);
        }
    }

    /**
     * Send a mouse notification.
     *
     * @param callPeer <tt>CallPeer</tt> that will be notified
     * @param event <tt>MouseEvent</tt> received and that will be send to
     * remote peer
     */
    public void sendMouseEvent(CallPeer callPeer, MouseEvent event)
    {
        /* build a SIP NOTIFY with the corresponding mouse event
         * and send it
         */
        String msg = null;

        /* note that MOUSE_MOVED and MOUSE_DRAGGED are handled in
         * sendMouseEvent(MouseEvent event, Dimension videoPanelSize)
         */
        switch(event.getID())
        {
        case MouseEvent.MOUSE_PRESSED:
            msg = DesktopSharingProtocolSipImpl.getMousePressedXML(
                    event.getModifiers());
            break;
        case MouseEvent.MOUSE_RELEASED:
            msg = DesktopSharingProtocolSipImpl.getMouseReleasedXML(
                    event.getModifiers());
            break;
        case MouseEvent.MOUSE_WHEEL:
            MouseWheelEvent evt = (MouseWheelEvent)event;
            msg = DesktopSharingProtocolSipImpl.getMouseWheelXML(
                    evt.getWheelRotation());
            break;
        default:
            /* ignore */
            return;
        }

        synchronized(inputSync)
        {
            inputEvents.add(msg);
            notifySubscriptions(callPeer);
        }
    }

    /**
     * Implements <tt>EventPackageNotifier.Subscription</tt> in order to
     * represent a subscription created by a remote <tt>CallPeer</tt>
     * to the remote-control event package of a local <tt>Call</tt>.
     */
    private class RemoteControlNotifierSubscription
        extends EventPackageNotifier.Subscription
    {
        /**
         * The <tt>CallPeer</tt> associated with this notification.
         */
        private CallPeerSipImpl callPeer = null;

        /**
         * The received dssid from the subscription and the one to be used
         * in the notify requests.
         */
        private String dssid = null;

        /**
         * Initializes a new <tt>RemoteControlNotifierSubscription</tt> instance
         * with a specific subscription <tt>Address</tt>/Request URI and a
         * specific id tag of the associated Event headers.
         *
         * @param fromAddress the subscription <tt>Address</tt>/Request URI
         * which is to be the target of the NOTIFY requests associated with the
         * new instance
         * @param eventId the value of the id tag to be placed in the Event
         * headers of the NOTIFY requests created for the new instance and to be
         * present in the received Event headers in order to have the new
         * instance associated with them
         */
        public RemoteControlNotifierSubscription(
                Address fromAddress,
                String eventId)
        {
            super(fromAddress, eventId);
        }

        /**
         * Creates the content of the NOTIFY request to be sent to the target
         * represented by this <tt>Subscription</tt> and having a specific
         * subscription state and a specific reason for that subscription state.
         *
         * @param subscriptionState the subscription state to be notified about
         * in the NOTIFY request which is to carry the returned content
         * @param reason the reason for the subscription state to be notified
         * about in the NOTIFY request which is to carry the returned content
         *
         * @return an array of <tt>byte</tt>s representing the content of the
         * NOTIFY request to be sent to the target represented by this
         * <tt>Subscription</tt>
         * @see EventPackageNotifier.Subscription#createNotifyContent(String,
         * String)
         */
        @Override
        protected byte[] createNotifyContent(
                String subscriptionState,
                String reason)
        {
            CallPeerSipImpl callPeer = getCallPeer();

            if (callPeer == null)
            {
                logger
                    .error(
                        "Failed to find the CallPeer of the remote-control" +
                        "subscription " + this);
                return null;
            }

            String xml = null;
            byte[] notifyContent = null;

            xml = inputEvents.poll();

            if(xml == null)
            {
                xml = new String("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<remote-control />");
            }

            try
            {
                notifyContent = xml.getBytes("UTF-8");
            }
            catch (UnsupportedEncodingException uee)
            {
                logger
                    .warn(
                        "Failed to gets bytes from String for the UTF-8 charset",
                        uee);
                notifyContent = xml.getBytes();
            }

            return notifyContent;
        }

        /**
         * Gets the <tt>CallPeerSipImpl</tt> subscribed to the
         * <tt>EventPackageNotifier</tt> and represented by this
         * <tt>Subscription</tt>.
         *
         * @return the <tt>CallPeerSipImpl</tt> subscribed to the
         * <tt>EventPackageNotifier</tt> and represented by this
         * <tt>Subscription</tt>
         */
        private CallPeerSipImpl getCallPeer()
        {
            if(callPeer == null)
            {
                Dialog dialog = getDialog();

                if (dialog != null)
                {
                    OperationSetBasicTelephony<?> basicTelephony
                        = parentProvider.getOperationSet(
                                OperationSetBasicTelephony.class);

                    if (basicTelephony != null)
                    {
                        ActiveCallsRepositorySipImpl callRepo
                            = ((OperationSetBasicTelephonySipImpl)
                                    basicTelephony).getActiveCallsRepository();

                        callPeer = callRepo.findCallPeer(dialog);

                        // if call peer is still null and we have enabled
                        // working out of dialog desktop sharing, search the
                        // peer based on the dssid we have
                        if(callPeer == null && dssid != null)
                        {
                            callPeer = findCallPeerByDSSID(callRepo);
                        }

                        if (callPeer != null)
                            callPeer.addCallPeerListener(callPeerListener);
                    }
                }
            }
            return callPeer;
        }

        /**
         * Sets dssid value.
         * @param value
         */
        public void setDSSID(String value)
        {
            this.dssid = value;
        }

        /**
         * Finds a call peer by a call with same <tt>dssid</tt> if any.
         * @param callRepo the active call repository to use while
         * searching calls.
         * @return a matching call peer.
         */
        public CallPeerSipImpl findCallPeerByDSSID(
            ActiveCallsRepositorySipImpl callRepo)
        {
            if(dssid == null)
                return null;

            for (Iterator<CallSipImpl> activeCalls = callRepo.getActiveCalls();
                    activeCalls.hasNext();)
            {
                CallSipImpl call = activeCalls.next();

                if(call instanceof DesktopSharingCallSipImpl)
                {
                    DesktopSharingCallSipImpl dsCall
                        = (DesktopSharingCallSipImpl)call;

                    if( dsCall.getDesktopSharingSessionID() != null
                        && dsCall.getDesktopSharingSessionID().equals(dssid))
                    {
                        return dsCall.getCallPeers().next();
                    }
                }
            }

            return null;
        }
    }
}
