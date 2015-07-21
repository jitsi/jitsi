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
import java.util.List;

import javax.sip.*;
import javax.sip.Dialog;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.xml.parsers.*;

import net.java.sip.communicator.service.hid.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.util.xml.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Implements all desktop sharing server-side related functions for SIP
 * protocol.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopSharingServerSipImpl
    extends OperationSetDesktopStreamingSipImpl
    implements OperationSetDesktopSharingServer,
               MethodProcessorListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger
            .getLogger(OperationSetDesktopSharingServerSipImpl.class);

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

            if (remoteControlEnabled && state != null &&
                    (state.equals(CallPeerState.DISCONNECTED) ||
                            state.equals(CallPeerState.FAILED)))
            {
                /* if the peer is disconnected or call has failed the SIP
                 * dialog is terminated and sending a SUBSCRIBE (with 0 as
                 * lifetime) will throw exception
                 */
                remoteControlEnabled = false;

                try
                {
                    subscriber.removeSubscription(parentProvider.
                            parseAddressString(peer.getAddress()));
                }
                catch(ParseException ex)
                {
                }
            }
        }
    };

    /**
     * If the remote control is authorized and thus enabled.
     */
    private boolean remoteControlEnabled = false;

    /**
     * The <tt>EventPackageNotifier</tt> which implements remote-control
     * event-package subscriber support on behalf of this
     * <tt>OperationSetDesktopSharingServer</tt> instance.
     */
    private final EventPackageSubscriber subscriber;

    /**
     * The SIP <tt>ProtocolProviderService</tt> implementation which created
     * this instance and for which telephony conferencing services are being
     * provided by this instance.
     */
    private final ProtocolProviderServiceSipImpl parentProvider;

    /**
     * The <tt>Timer</tt> which executes delayed tasks scheduled by
     * {@link #subscriber}.
     */
    private final TimerScheduler timer = new TimerScheduler();

    /**
     * HID service that will regenerates keyboard and mouse events received in
     * SIP NOTIFY.
     */
    private HIDService hidService = null;

    /**
     * Initializes a new <tt>OperationSetDesktopSharingSipImpl</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephonySipImpl</tt>.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephonySipImpl</tt>
     * the new extension should build upon
     */
    public OperationSetDesktopSharingServerSipImpl(
            OperationSetBasicTelephonySipImpl basicTelephony)
    {
        super(basicTelephony);
        parentProvider = basicTelephony.getProtocolProvider();

        hidService = SipActivator.getHIDService();

        subscriber = new EventPackageSubscriber(
                this.parentProvider,
                DesktopSharingProtocolSipImpl.EVENT_PACKAGE,
                DesktopSharingProtocolSipImpl.SUBSCRIPTION_DURATION,
                DesktopSharingProtocolSipImpl.CONTENT_SUB_TYPE,
                this.timer,
                DesktopSharingProtocolSipImpl.REFRESH_MARGIN)
        {
            /**
             * Populates a specific <tt>Request</tt> instance with the headers
             * common to dialog-creating <tt>Request</tt>s and ones sent inside
             * existing dialogs and specific to the general event package
             * subscription functionality that this instance and a specific
             * <tt>Subscription</tt> represent.
             *
             * @param req the <tt>Request</tt> instance to be populated with
             * common headers and ones specific to the event package of a
             * specific <tt>Subscription</tt>
             * @param subscription the <tt>Subscription</tt> which is to be
             * described in the specified <tt>Request</tt> i.e. its properties
             * are to be used to populate the specified <tt>Request</tt>
             * @param expires the subscription duration to be set into the
             * Expires header of the specified SUBSCRIBE <tt>Request</tt>
             * @throws OperationFailedException if we fail parsing or populating
             * the subscription request.
             */
            @Override
            protected void populateSubscribeRequest(
                Request req,
                Subscription subscription,
                int expires)
                throws
                OperationFailedException
            {
                super.populateSubscribeRequest(req, subscription, expires);

                RemoteControlSubscriberSubscription
                    rControlSubs = (RemoteControlSubscriberSubscription)subscription;

                // add DSSID for subscribe if needed, if out of dialog
                // desktop control is enabled
                if(rControlSubs.callPeer.getCall()
                        instanceof DesktopSharingCallSipImpl)
                {
                    try
                    {
                        Header dssidHeader = parentProvider.getHeaderFactory()
                            .createHeader(
                                DesktopSharingCallSipImpl.DSSID_HEADER,
                                ((DesktopSharingCallSipImpl)rControlSubs
                                    .callPeer.getCall())
                                    .getDesktopSharingSessionID());
                        req.setHeader(dssidHeader);
                    }
                    catch(ParseException ex)
                    {
                        logger.error("error ", ex);
                    }
                }
            }
        };
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @param device <tt>MediaDevice</tt> to use for this call
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    @Override
    public Call createVideoCall(String uri, MediaDevice device)
        throws OperationFailedException, ParseException
    {
        CallSipImpl call = (CallSipImpl)super.createVideoCall(uri, device);
        CallPeerSipImpl callPeer = call.getCallPeers().next();
        callPeer.addMethodProcessorListener(this);
        callPeer.addCallPeerListener(callPeerListener);

        size = (((VideoMediaFormat)call.getDefaultDevice(MediaType.VIDEO).
                getFormat()).getSize());
        origin = getOriginForMediaDevice(device);
        return call;
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @param device <tt>MediaDevice</tt> to use for this call
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    @Override
    public Call createVideoCall(Contact callee, MediaDevice device)
        throws OperationFailedException
    {
        CallSipImpl call = (CallSipImpl)super.createVideoCall(callee, device);
        CallPeerSipImpl callPeer = call.getCallPeers().next();

        callPeer.addMethodProcessorListener(this);
        callPeer.addCallPeerListener(callPeerListener);

        size
            = (((VideoMediaFormat)
                    call.getDefaultDevice(MediaType.VIDEO).getFormat())
                .getSize());
        origin = getOriginForMediaDevice(device);
        return call;
    }

    /**
     * Enable desktop remote control. Local desktop can now regenerates keyboard
     * and mouse events received from peer.
     *
     * @param callPeer call peer that will take control on local computer
     */
    public void enableRemoteControl(CallPeer callPeer)
    {
        RemoteControlSubscriberSubscription subscription
            = new RemoteControlSubscriberSubscription(
                    (CallPeerSipImpl) callPeer);

        try
        {
            subscriber.subscribe(subscription);
        }
        catch (OperationFailedException ofe)
        {
            logger.error(
                    "Failed to create or send a remote-control subscription",
                    ofe);
        }
    }

    /**
     * Disable desktop remote control. Local desktop stop regenerates keyboard
     * and mouse events received from peer.
     *
     * @param callPeer call peer that will stop controlling on local computer
     */
    public void disableRemoteControl(CallPeer callPeer)
    {
        /* unsubscribe */
        try
        {
            Address addr
                = parentProvider.parseAddressString(
                        callPeer.getAddress());

            subscriber.unsubscribe(addr, false);
         }
        catch(ParseException ex)
        {
            logger.error("Failed to parse address", ex);
        }
        catch (OperationFailedException ofe)
        {
            logger.error(
                    "Failed to create or send a remote-control unsubscription",
                    ofe);
            return;
        }

        remoteControlEnabled = false;
    }

    /**
     * Notifies this <tt>MethodProcessorListener</tt> that a specific
     * <tt>CallPeer</tt> has processed a specific SIP <tt>Request</tt> and has
     * replied to it with a specific SIP <tt>Response</tt>.
     *
     * @param sourceCallPeer the <tt>CallPeer</tt> which has processed the
     * specified SIP <tt>Request</tt>
     * @param request the SIP <tt>Request</tt> which has been processed by
     * <tt>sourceCallPeer</tt>
     * @param response the SIP <tt>Response</tt> sent by <tt>sourceCallPeer</tt>
     * as a reply to the specified SIP <tt>request</tt>
     * @see MethodProcessorListener#requestProcessed(CallPeerSipImpl, Request,
     * Response)
     */
    public void requestProcessed(
            CallPeerSipImpl sourceCallPeer,
            Request request,
            Response response)
    {
    }

    /**
     * Notifies this <tt>MethodProcessorListener</tt> that a specific
     * <tt>CallPeer</tt> has processed a specific SIP <tt>Response</tt> and has
     * replied to it with a specific SIP <tt>Request</tt>.
     *
     * @param sourceCallPeer the <tt>CallPeer</tt> which has processed the
     * specified SIP <tt>Response</tt>
     * @param response the SIP <tt>Response</tt> which has been processed by
     * <tt>sourceCallPeer</tt>
     * @param request the SIP <tt>Request</tt> sent by <tt>sourceCallPeer</tt>
     * as a reply to the specified SIP <tt>response</tt>
     * @see MethodProcessorListener#responseProcessed(CallPeerSipImpl, Response,
     * Request)
     */
    public void responseProcessed(
            CallPeerSipImpl sourceCallPeer,
            Response response,
            Request request)
    {
        if (Response.OK == response.getStatusCode())
        {
            CSeqHeader cseqHeader
                = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

            if ((cseqHeader != null)
                    && Request.INVITE.equalsIgnoreCase(cseqHeader.getMethod()))
            {
                /* if we have successfully established a SIP session, launch
                 * remote control
                 */

                /* we never launch directly sharing (future sharer is force to
                 * toggle remote control.
                 */
                //enableRemoteControl(sourceCallPeer);
            }
        }
    }

    /**
     * Process keyboard notification received from remote peer.
     *
     * @param event <tt>KeyboardEvent</tt> that will be regenerated on
     * local computer
     */
    public void processKeyboardEvent(KeyEvent event)
    {
        /* ignore command if remote control is not enabled otherwise regenerates
         * event on the computer
         */
        if (remoteControlEnabled && hidService != null)
        {
            int keycode = 0;

            /* process immediately a "key-typed" event via press/release */
            if(event.getKeyChar() != 0 && event.getID() == KeyEvent.KEY_TYPED)
            {
                hidService.keyPress(event.getKeyChar());
                hidService.keyRelease(event.getKeyChar());
                return;
            }

            keycode = event.getKeyCode();

            if(keycode == 0)
            {
                return;
            }

            switch(event.getID())
            {
            case KeyEvent.KEY_PRESSED:
                hidService.keyPress(keycode);
                break;
            case KeyEvent.KEY_RELEASED:
                hidService.keyRelease(keycode);
                break;
            default:
                break;
            }
        }
    }

    /**
     * Process mouse notification received from remote peer.
     *
     * @param event <tt>MouseEvent</tt> that will be regenerated on local
     * computer
     */
    public void processMouseEvent(MouseEvent event)
    {
        /* ignore command if remote control is not enabled otherwise regenerates
         * event on the computer
         */
        if (remoteControlEnabled && hidService != null)
        {
            switch(event.getID())
            {
            case MouseEvent.MOUSE_PRESSED:
                hidService.mousePress(event.getModifiers());
                break;
            case MouseEvent.MOUSE_RELEASED:
                hidService.mouseRelease(event.getModifiers());
                break;
            case MouseEvent.MOUSE_MOVED:
                hidService.mouseMove(event.getX(), event.getY());
                break;
            case MouseEvent.MOUSE_WHEEL:
                MouseWheelEvent evt = (MouseWheelEvent)event;
                hidService.mouseWheel(evt.getWheelRotation());
                break;
            default:
                break;
            }
        }
    }

    /**
     * Implements <tt>EventPackageSubscriber.Subscription</tt> in order to
     * represent the subscription of the local peer to the remote-control event
     * package of a specific remote <tt>CallPeer</tt> acting as a desktop
     * sharing server.
     */
    private class RemoteControlSubscriberSubscription
        extends EventPackageSubscriber.Subscription
    {
        /**
         * The <tt>CallPeer</tt> which is acting as a remote-control focus in
         * its <tt>Call</tt> with the local peer.
         */
        private final CallPeerSipImpl callPeer;

        /**
         * Initializes a new <tt>RemoteControlSubscriberSubscription</tt>
         * instance which is to represent the subscription of the local peer to
         * the remote-control event package of a specific <tt>CallPeer</tt>
         * acting as a desktop sharing server.
         *
         * @param callPeer
         *            the <tt>CallPeer</tt> acting as a desktop sharing server
         *            which the new instance is to subscribe to
         */
        public RemoteControlSubscriberSubscription(CallPeerSipImpl callPeer)
        {
            super(callPeer.getPeerAddress());

            this.callPeer = callPeer;
        }

        /**
         * Gets the <tt>Dialog</tt> which was created by the SUBSCRIBE request
         * associated with this <tt>Subscription</tt> or which was used to send
         * that request in.
         *
         * @return the <tt>Dialog</tt> which was created by the SUBSCRIBE
         *         request associated with this <tt>Subscription</tt> or which
         *         was used to send that request in; <tt>null</tt> if the
         *         success of the SUBSCRIBE request has not been confirmed yet
         *         or this <tt>Subscription</tt> was removed from the list of
         *         the <tt>EventPackageSupport</tt> it used to be in
         * @see EventPackageSubscriber.Subscription#getDialog()
         */
        @Override
        protected Dialog getDialog()
        {
            Dialog dialog = super.getDialog();

            if ((dialog == null)
                    || DialogState.TERMINATED.equals(dialog.getState()))
                dialog = callPeer.getDialog();
            return dialog;
        }

        /**
         * Notifies this <tt>Subscription</tt> that an active NOTIFY
         * <tt>Request</tt> has been received and it may process the specified
         * raw content carried in it.
         *
         * @param requestEvent
         *            the <tt>RequestEvent</tt> carrying the full details of the
         *            received NOTIFY <tt>Request</tt> including the raw content
         *            which may be processed by this <tt>Subscription</tt>
         * @param rawContent
         *            an array of bytes which represents the raw content carried
         *            in the body of the received NOTIFY <tt>Request</tt> and
         *            extracted from the specified <tt>RequestEvent</tt> for the
         *            convenience of the implementers
         * @see EventPackageSubscriber.Subscription#processActiveRequest(
         * RequestEvent, byte[])
         */
        @Override
        protected void processActiveRequest(RequestEvent requestEvent,
                byte[] rawContent)
        {
            if(requestEvent.getDialog() != callPeer.getDialog())
            {
                if(callPeer.getCall() instanceof DesktopSharingCallSipImpl)
                {
                    Header dssidHeader = requestEvent.getRequest()
                        .getHeader(DesktopSharingCallSipImpl.DSSID_HEADER);
                    if(dssidHeader != null)
                    {
                        String dssid = dssidHeader.toString().replaceAll(
                            dssidHeader.getName() + ":", "").trim();
                        if(!dssid.equals(
                                ((DesktopSharingCallSipImpl)callPeer.getCall())
                                    .getDesktopSharingSessionID()))
                            return;
                    }
                    else
                        return;
                }
                else
                    return;
            }

            if (rawContent != null)
            {
                /* parse rawContent */
                Document document = null;
                Throwable exception = null;

                try
                {
                    DocumentBuilderFactory factory
                        = XMLUtils.newDocumentBuilderFactory();
                    document
                        = factory.newDocumentBuilder()
                                .parse(new ByteArrayInputStream(rawContent));
                }
                catch (IOException ioe)
                {
                    exception = ioe;
                }
                catch (ParserConfigurationException pce)
                {
                    exception = pce;
                }
                catch (SAXException saxe)
                {
                    exception = saxe;
                }

                if (exception != null)
                {
                    logger.error("Failed to parse remote-info XML", exception);
                }
                else
                {
                    Element root = document.getDocumentElement();
                    List<ComponentEvent> events = null;
                    Point p = getOrigin();

                    if(size == null)
                    {
                        size = (((VideoMediaFormat)
                            callPeer.getCall()
                            .getDefaultDevice(MediaType.VIDEO).getFormat())
                            .getSize());
                    }
                    events = DesktopSharingProtocolSipImpl.parse(root, size, p);

                    for(ComponentEvent evt : events)
                    {
                        if(evt instanceof MouseEvent)
                            processMouseEvent((MouseEvent)evt);
                        else if(evt instanceof KeyEvent)
                            processKeyboardEvent((KeyEvent)evt);
                    }
                }
            }
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt> to a
         * previous SUBSCRIBE <tt>Request</tt> has been received with a status
         * code in the failure range and it may process the status code carried
         * in it.
         *
         * @param responseEvent
         *            the <tt>ResponseEvent</tt> carrying the full details of
         *            the received <tt>Response</tt> including the status code
         *            which may be processed by this <tt>Subscription</tt>
         * @param statusCode
         *            the status code carried in the <tt>Response</tt> and
         *            extracted from the specified <tt>ResponseEvent</tt> for
         *            the convenience of the implementers
         * @see EventPackageSubscriber.Subscription#processFailureResponse(
         * ResponseEvent, int)
         */
        @Override
        protected void processFailureResponse(ResponseEvent responseEvent,
                int statusCode)
        {
            /* we have not managed to subscribe to remote peer so it is better
             * to disable remote control feature
             */
            remoteControlEnabled = false;
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt> to a
         * previous SUBSCRIBE <tt>Request</tt> has been received with a status
         * code in the success range and it may process the status code carried
         * in it.
         *
         * @param responseEvent
         *            the <tt>ResponseEvent</tt> carrying the full details of
         *            the received <tt>Response</tt> including the status code
         *            which may be processed by this <tt>Subscription</tt>
         * @param statusCode
         *            the status code carried in the <tt>Response</tt> and
         *            extracted from the specified <tt>ResponseEvent</tt> for
         *            the convenience of the implementers
         * @see EventPackageSubscriber.Subscription#processSuccessResponse(
         * ResponseEvent, int)
         */
        @Override
        protected void processSuccessResponse(ResponseEvent responseEvent,
                int statusCode)
        {
            switch (statusCode)
            {
            case Response.OK:
            case Response.ACCEPTED:
                /* we have succeeded to subscribe to remote peer */
                remoteControlEnabled = true;
                break;
            }
        }

        /**
         * Notifies this <tt>Subscription</tt> that a terminating NOTIFY
         * <tt>Request</tt> has been received and it may process the reason code
         * carried in it.
         *
         * @param requestEvent
         *            the <tt>RequestEvent</tt> carrying the full details of the
         *            received NOTIFY <tt>Request</tt> including the reason code
         *            which may be processed by this <tt>Subscription</tt>
         * @param reasonCode
         *            the code of the reason for the termination carried in the
         *            NOTIFY <tt>Request</tt> and extracted from the specified
         *            <tt>RequestEvent</tt> for the convenience of the
         *            implementers
         * @see EventPackageSubscriber.Subscription#processTerminatedRequest(
         * RequestEvent, String)
         */
        @Override
        protected void processTerminatedRequest(RequestEvent requestEvent,
                String reasonCode)
        {
            if (SubscriptionStateHeader.DEACTIVATED.equals(reasonCode))
            {
                try
                {
                    subscriber.poll(this);
                }
                catch (OperationFailedException ofe)
                {
                    logger.error(
                            "Failed to renew the remote-control subscription "
                            + this, ofe);
                }
            }
        }
    }

    /**
     * Tells if the peer provided can be remotely controlled by this peer:
     * - The server is able to grant/revoke remote access to its desktop.
     * - The client (the call peer) is able to send mouse and keyboard events.
     *
     * @param callPeer The call peer which may remotely control the shared
     * desktop.
     *
     * @return True if the server and the client are able to respectively grant
     *  remote access and send mouse/keyboard events. False, if one of the call
     *  participant (server or client) is not able to deal with remote controls.
     */
    public boolean isRemoteControlAvailable(CallPeer callPeer)
    {
        // There is no mean to discover if the remote peer can sends mouse and
        // keyboard events for SIP. Thus, let define it always to true.
        return true;
    }
}
