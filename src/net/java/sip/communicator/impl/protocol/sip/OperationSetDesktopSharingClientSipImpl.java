/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
import java.text.*;
import java.util.List; // disambiguation
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.sip.Dialog; // disambiguation

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implements all desktop sharing client-side related functions for SIP
 * protocol.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopSharingClientSipImpl
    implements OperationSetDesktopSharingClient
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger
            .getLogger(OperationSetDesktopSharingClientSipImpl.class);

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

            if(state != null && (state.equals(CallPeerState.DISCONNECTED) ||
                            state.equals(CallPeerState.FAILED)))
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
     * List of listeners to be notified when a change occurred in remote control
     * access.
     */
    private List<RemoteControlListener> listeners =
        new ArrayList<RemoteControlListener>();

    /**
     * The <tt>EventPackageNotifier</tt> which implements remote-control
     * event-package notifier support on behalf of this
     * <tt>OperationSetDesktopSharingClient</tt> instance.
     */
    private final EventPackageNotifier notifier;

    /**
     * The SIP <tt>ProtocolProviderService</tt> implementation which created
     * this instance and for which telephony conferencing services are being
     * provided by this instance.
     */
    private final ProtocolProviderServiceSipImpl parentProvider;

    /**
     * The <tt>Timer</tt> which executes delayed tasks scheduled by
     * {@link #notifier}.
     */
    private final TimerScheduler timer = new TimerScheduler();

    /**
     * List of SIP NOTIFY messages.
     */
    private Queue<String> inputEvents = new LinkedList<String>();

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
        this.parentProvider = parentProvider;

        this.notifier = new EventPackageNotifier(
                this.parentProvider,
                DesktopSharingProtocolSipImpl.EVENT_PACKAGE,
                DesktopSharingProtocolSipImpl.SUBSCRIPTION_DURATION,
                DesktopSharingProtocolSipImpl.CONTENT_SUB_TYPE,
                this.timer)
        {
            protected Subscription createSubscription(
                    Address fromAddress,
                    String eventId)
            {
                /* new subscription received */
                fireRemoteControlGranted();

                return
                    new RemoteControlNotifierSubscription(
                            fromAddress,
                            eventId);
            }

            protected void removeSubscription(
                Response response,
                String eventId,
                ClientTransaction clientTransaction)
            {
                super.removeSubscription(response, eventId, clientTransaction);

                fireRemoteControlRevoked();
            }
        };
    }

    /**
     * Notifies all <tt>Subscription</tt>s.
     */
    private void notifySubscriptions()
    {
        EventPackageNotifier.SubscriptionFilter subscriptionFilter
            = new EventPackageNotifier.SubscriptionFilter()
        {
            public boolean accept(
                    EventPackageNotifier.Subscription subscription)
            {
                return
                    (subscription instanceof RemoteControlNotifierSubscription);
                    /*
                        && call
                                .equals(
                                    ((RemoteControlNotifierSubscription)
                                            subscription)
                                        .getCall());
                    */
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
        String msg = null;
        int keycode = event.getKeyCode();
        int key = event.getKeyChar();

        if(key != KeyEvent.CHAR_UNDEFINED)
        {
            keycode = event.getKeyChar();
        }
        else
        {
            keycode = event.getKeyCode();
        }

        if(keycode == 0)
        {
            return;
        }

        switch(event.getID())
        {
        case KeyEvent.KEY_TYPED:
            msg = DesktopSharingProtocolSipImpl.getKeyTypedXML(keycode);
            break;
        case KeyEvent.KEY_PRESSED:
            msg = DesktopSharingProtocolSipImpl.getKeyPressedXML(keycode);
            break;
        case KeyEvent.KEY_RELEASED:
            msg = DesktopSharingProtocolSipImpl.getKeyReleasedXML(keycode);
            break;
        default:
            /* ignore */
            return;
        }

        synchronized(inputSync)
        {
            inputEvents.add(msg);
            notifySubscriptions();
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
            notifySubscriptions();
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
            notifySubscriptions();
        }
    }

    /**
     * Fire a <tt>RemoteControlGrantedEvent</tt> to all registered listeners.
     */
    public void fireRemoteControlGranted()
    {
        RemoteControlGrantedEvent event = new RemoteControlGrantedEvent(this);
        for(RemoteControlListener l : listeners)
        {
            l.remoteControlGranted(event);
        }
    }

    /**
     * Fire a <tt>RemoteControlGrantedEvent</tt> to all registered listeners.
     */
    public void fireRemoteControlRevoked()
    {
        RemoteControlRevokedEvent event = new RemoteControlRevokedEvent(this);
        for(RemoteControlListener l : listeners)
        {
            l.remoteControlRevoked(event);
        }
    }

    /**
     * Add a <tt>RemoteControlListener</tt> to be notified when remote peer
     * accept to give us full control.
     *
     * @param listener <tt>RemoteControlListener</tt> to add
     */
    public void addRemoteControlListener(RemoteControlListener listener)
    {
        if(!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    /**
     * Remove a <tt>RemoteControlListener</tt> to be notified when remote peer
     * accept/revoke to give us full control.
     *
     * @param listener <tt>RemoteControlListener</tt> to remove
     */
    public void removeRemoteControlListener(RemoteControlListener listener)
    {
        if(listeners.contains(listener))
        {
            listeners.remove(listener);
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
         * Gets the <tt>Call</tt> of the <tt>CallPeerSipImpl</tt> subscribed to
         * the <tt>EventPackageNotifier</tt> and represented by this
         * <tt>Subscription</tt>.
         *
         * @return the <tt>Call</tt> of the <tt>CallPeerSipImpl</tt> subscribed
         * to the <tt>EventPackageNotifier</tt> and represented by this
         * <tt>Subscription</tt>
         */
        public CallSipImpl getCall()
        {
            CallPeerSipImpl callPeer = getCallPeer();

            return (callPeer == null) ? null : callPeer.getCall();
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
                        callPeer =
                            ((OperationSetBasicTelephonySipImpl)basicTelephony)
                               .getActiveCallsRepository().findCallPeer(dialog);

                        if (callPeer != null)
                            callPeer.addCallPeerListener(callPeerListener);
                    }
                }
            }
            return callPeer;
        }
    }
}
