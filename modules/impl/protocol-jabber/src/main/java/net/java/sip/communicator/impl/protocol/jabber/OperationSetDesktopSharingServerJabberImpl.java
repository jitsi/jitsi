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
package net.java.sip.communicator.impl.protocol.jabber;

import java.awt.event.*;
import java.text.*;
import java.util.*;

import org.jitsi.xmpp.extensions.inputevt.*;
import net.java.sip.communicator.service.hid.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.utils.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.iqrequest.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.roster.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.stringprep.*;

/**
 * Implements all desktop sharing server-side related functions for Jabber
 * protocol.
 *
 * @author Sebastien Vincent
 * @author Vincent Lucas
 */
public class OperationSetDesktopSharingServerJabberImpl
    extends OperationSetDesktopStreamingJabberImpl
    implements OperationSetDesktopSharingServer,
               RegistrationStateChangeListener,
               IQRequestHandler
{
    /**
     * Our class logger.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetDesktopSharingServerJabberImpl.class);

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

            if (state != null
                    && (state.equals(CallPeerState.DISCONNECTED)
                        || state.equals(CallPeerState.FAILED)))
            {
                disableRemoteControl(peer);
            }
        }
    };

    /**
     * HID service that will regenerates keyboard and mouse events received in
     * Jabber messages.
     */
    private final HIDService hidService;

    /**
     * List of callPeers for the desktop sharing session with remote control
     * granted.
     */
    private final List<Jid> callPeers = new ArrayList<>();

    /**
     * Initializes a new <tt>OperationSetDesktopSharingJabberImpl</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephonyJabberImpl</tt>.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephonyJabberImpl</tt>
     * the new extension should build upon
     */
    public OperationSetDesktopSharingServerJabberImpl(
            OperationSetBasicTelephonyJabberImpl basicTelephony)
    {
        super(basicTelephony);

        parentProvider.addRegistrationStateChangeListener(this);
        hidService = JabberActivator.getHIDService();
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @param device video device that will be used to stream desktop.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    @Override
    public Call createVideoCall(String uri, MediaDevice device)
        throws OperationFailedException, ParseException
    {
        MediaAwareCall<?,?,?> call
            = (MediaAwareCall<?,?,?>) super.createVideoCall(uri, device);

        size
            = (((VideoMediaFormat)
                        call.getDefaultDevice(MediaType.VIDEO).getFormat())
                    .getSize());
        origin = getOriginForMediaDevice(device);
        return call;
    }

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @param device video device that will be used to stream desktop.
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
        MediaAwareCall<?,?,?> call
            = (MediaAwareCall<?,?,?>) super.createVideoCall(callee, device);

        size
            = ((VideoMediaFormat)
                    call.getDefaultDevice(MediaType.VIDEO).getFormat())
                .getSize();
        origin = getOriginForMediaDevice(device);
        return call;
    }

    /**
     * Check if the remote part supports Jingle video.
     *
     * @param calleeAddress Contact address
     * @return true if contact support Jingle video, false otherwise
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    @Override
    protected Call createOutgoingVideoCall(String calleeAddress)
        throws OperationFailedException
    {
        return createOutgoingVideoCall(calleeAddress, null);
    }

    /**
     * Check if the remote part supports Jingle video.
     *
     * @param calleeAddress Contact address
     * @param videoDevice specific video device to use (null to use default
     * device)
     * @return true if contact support Jingle video, false otherwise
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    @Override
    protected Call createOutgoingVideoCall(
            String calleeAddress,
            MediaDevice videoDevice)
        throws OperationFailedException
    {
        boolean remoteControlSupported = false;

        Jid calleeJid;
        try
        {
            calleeJid = JidCreate.from(calleeAddress);
        }
        catch (XmppStringprepException e)
        {
            throw new OperationFailedException(
                calleeAddress + "is not a valid JID",
                OperationFailedException.GENERAL_ERROR,
                e
            );
        }

        FullJid fullCalleeURI;
        if (calleeJid.hasResource())
        {
            fullCalleeURI = calleeJid.asFullJidOrThrow();
        }
        else
        {
            Roster r = Roster.getInstanceFor(parentProvider.getConnection());
            fullCalleeURI = r.getPresence(calleeJid.asBareJid())
                .getFrom()
                .asFullJidOrThrow();
        }

        if (logger.isInfoEnabled())
            logger.info("creating outgoing desktop sharing call...");

        DiscoverInfo di;
        try
        {
            // check if the remote client supports inputevt (remote control)
            di = parentProvider.getDiscoveryManager()
                    .discoverInfo(fullCalleeURI);

            if (di.containsFeature(InputEvtIQ.NAMESPACE_CLIENT))
            {
                if (logger.isInfoEnabled())
                    logger.info(fullCalleeURI + ": remote-control supported");

                remoteControlSupported = true;
            }
            else
            {
                if (logger.isInfoEnabled())
                {
                    logger.info(
                            fullCalleeURI + ": remote-control not supported!");
                }

                // TODO fail or not?
                /*
                throw new OperationFailedException(
                        "Failed to create a true desktop sharing.\n"
                            + fullCalleeURI + " does not support inputevt",
                        OperationFailedException.INTERNAL_ERROR);
                */
            }
        }
        catch (XMPPException
            | InterruptedException
            | NotConnectedException
            | NoResponseException ex)
        {
            logger.warn("could not retrieve info for " + fullCalleeURI, ex);
        }

        if (parentProvider.getConnection() == null)
        {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession.\n"
                    + "we don't have a valid XMPPConnection."
                    , OperationFailedException.INTERNAL_ERROR);
        }

        CallJabberImpl call = new CallJabberImpl(basicTelephony);
        MediaUseCase useCase = getMediaUseCase();

        if (videoDevice != null)
        {
            call.setVideoDevice(videoDevice, useCase);

            // enable video
            call.setLocalVideoAllowed(true, useCase);

            // enable remote-control
            call.setLocalInputEvtAware(remoteControlSupported);
            size = (((VideoMediaFormat)videoDevice.getFormat()).getSize());
        }

        basicTelephony.createOutgoingCall(call, calleeAddress);
        new CallPeerJabberImpl(calleeJid, call);
        return call;
    }

    /**
     * Sets the indicator which determines whether the streaming of local video
     * in a specific <tt>Call</tt> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> to allow/disallow the streaming of local
     * video for
     * @param mediaDevice the media device to use for the desktop streaming
     * @param allowed <tt>true</tt> to allow the streaming of local video for
     * the specified <tt>Call</tt>; <tt>false</tt> to disallow it
     *
     * @throws OperationFailedException if initializing local video fails.
     */
    @Override
    public void setLocalVideoAllowed(Call call,
                                     MediaDevice mediaDevice,
                                     boolean allowed)
        throws OperationFailedException
    {
        ((CallJabberImpl) call).setLocalInputEvtAware(allowed);
        super.setLocalVideoAllowed(call, mediaDevice, allowed);
    }

    /**
     * Enable desktop remote control. Local desktop can now regenerates keyboard
     * and mouse events received from peer.
     *
     * @param callPeer call peer that will take control on local computer
     */
    public void enableRemoteControl(CallPeer callPeer)
    {
        callPeer.addCallPeerListener(callPeerListener);

        size = (((VideoMediaFormat)((CallJabberImpl)callPeer.getCall())
            .getDefaultDevice(MediaType.VIDEO).getFormat()).getSize());

        this.modifyRemoteControl(callPeer, true);
    }

    /**
     * Disable desktop remote control. Local desktop stops regenerate keyboard
     * and mouse events received from peer.
     *
     * @param callPeer call peer that will stop controlling on local computer
     */
    public void disableRemoteControl(CallPeer callPeer)
    {
        this.modifyRemoteControl(callPeer, false);
        callPeer.removeCallPeerListener(callPeerListener);
        origin = null;
    }

    /**
     * Implementation of method <tt>registrationStateChange</tt> from
     * interface RegistrationStateChangeListener for setting up (or down)
     * our <tt>InputEvtManager</tt> when an <tt>XMPPConnection</tt> is available
     *
     * @param evt the event received
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        OperationSetDesktopSharingServerJabberImpl.registrationStateChanged(
                    evt,
                    this,
                    this.parentProvider.getConnection());
    }

    /**
     * Implementation of method <tt>registrationStateChange</tt> from
     * interface RegistrationStateChangeListener for setting up (or down)
     * our <tt>InputEvtManager</tt> when an <tt>XMPPConnection</tt> is available
     *
     * @param evt the event received
     * @param packetListener the packet listener for InputEvtIQ of this
     * connection.  (OperationSetDesktopSharingServerJabberImpl or
     * OperationSetDesktopSharingClientJabberImpl).
     * @param connection The XMPP connection.
     */
    public static void registrationStateChanged(
            RegistrationStateChangeEvent evt,
            IQRequestHandler packetListener,
            XMPPConnection connection)
    {
        if(connection == null)
            return;

        if ((evt.getNewState() == RegistrationState.REGISTERING))
        {
            /* listen to specific inputevt IQ */
            connection.registerIQRequestHandler(packetListener);
        }
        else if ((evt.getNewState() == RegistrationState.UNREGISTERING))
        {
            /* listen to specific inputevt IQ */
            connection.unregisterIQRequestHandler(packetListener);
        }
    }

    /**
     * Process an <tt>ComponentEvent</tt> received from remote peer.
     *
     * @param event <tt>ComponentEvent</tt> that will be regenerated on local
     * computer
     */
    public void processComponentEvent(ComponentEvent event)
    {
        if(event == null)
        {
            return;
        }

        if(event instanceof KeyEvent)
        {
            processKeyboardEvent((KeyEvent)event);
        }
        else if(event instanceof MouseEvent)
        {
            processMouseEvent((MouseEvent)event);
        }
    }

    /**
     * Process keyboard notification received from remote peer.
     *
     * @param event <tt>KeyboardEvent</tt> that will be regenerated on local
     * computer
     */
    public void processKeyboardEvent(KeyEvent event)
    {
        /* ignore command if remote control is not enabled otherwise regenerates
         * event on the computer
         */
        if (hidService != null)
        {
            int keycode;

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
        if (hidService != null)
        {
            switch(event.getID())
            {
            case MouseEvent.MOUSE_PRESSED:
                hidService.mousePress(event.getModifiersEx());
                break;
            case MouseEvent.MOUSE_RELEASED:
                hidService.mouseRelease(event.getModifiersEx());
                break;
            case MouseEvent.MOUSE_MOVED:
                int originX = origin != null ? origin.x : 0;
                int originY = origin != null ? origin.y : 0;

                /* x and y position are sent in percentage but we multiply
                 * by 1000 in depacketizer because we cannot passed the size
                 * to the Provider
                 */
                int x = originX + ((event.getX() * size.width) / 1000);
                int y = originY + ((event.getY() * size.height) / 1000);
                hidService.mouseMove(x, y);
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
     * Sends IQ InputEvent START or STOP in order to enable/disable the remote
     * peer to remotely control our PC.
     *
     * @param cp call peer that will stop controlling on local computer.
     * @param enables True to enable remote peer to gain remote control on our
     * PC. False Otherwise.
     */
    public void modifyRemoteControl(CallPeer cp, boolean enables)
    {
        CallPeerJabberImpl callPeer = (CallPeerJabberImpl) cp;
        synchronized(callPeers)
        {
            if(callPeers.contains(callPeer.getAddressAsJid()) != enables)
            {
                if(isRemoteControlAvailable(callPeer))
                {
                    if(logger.isInfoEnabled())
                        logger.info("Enables remote control: " + enables);

                    InputEvtIQ inputIQ = new InputEvtIQ();
                    if(enables)
                    {
                        inputIQ.setAction(InputEvtAction.START);
                    }
                    else
                    {
                        inputIQ.setAction(InputEvtAction.STOP);
                    }
                    inputIQ.setType(IQ.Type.set);
                    inputIQ.setFrom(parentProvider.getOurJID());
                    inputIQ.setTo(callPeer.getAddressAsJid());

                    Stanza p = null;
                    try
                    {
                        StanzaCollector collector = parentProvider
                            .getConnection()
                            .createStanzaCollectorAndSend(inputIQ);
                        try
                        {
                            p = collector.nextResult();
                        }
                        finally
                        {
                            collector.cancel();
                        }
                    }
                    catch (InterruptedException | NotConnectedException e)
                    {
                        logger.error("Could not set remote control status", e);
                    }

                    if(enables)
                    {
                        receivedResponseToIqStart(callPeer, p);
                    }
                    else
                    {
                        receivedResponseToIqStop(callPeer, p);
                    }
                }
            }
        }
    }

    /**
     * Manages response to IQ InputEvent START.
     *
     * @param callPeer call peer that will stop controlling on local computer.
     * @param p The response to our IQ InputEvent START sent.
     */
    private void receivedResponseToIqStart(CallPeerJabberImpl callPeer, Stanza p)
    {
        // If the IQ has been correctly acknowledged, save the callPeer
        // has a peer with remote control granted.
        if(p != null && ((IQ) p).getType() == IQ.Type.result)
        {
            callPeers.add(callPeer.getAddressAsJid());
        }
        else
        {
            String packetString = (p == null)
                ? "\n\tPacket is null (IQ request timeout)."
                : "\n\tPacket: " + p.toXML();
            logger.info(
                    "Remote peer has not received/accepted the START "
                    + "action to grant the remote desktop control."
                    + packetString);
        }
    }

    /**
     * Manages response to IQ InputEvent STOP.
     *
     * @param callPeer call peer that will stop controlling on local computer.
     * @param p The response to our IQ InputEvent STOP sent.
     */
    private void receivedResponseToIqStop(CallPeerJabberImpl callPeer, Stanza p)
    {
        if(p == null || ((IQ) p).getType() == IQ.Type.error)
        {
            String packetString = (p == null)
                ? "\n\tPacket is null (IQ request timeout)."
                : "\n\tPacket: " + p.toXML();
            logger.info(
                    "Remote peer has not received/accepted the STOP "
                    + "action to grant the remote desktop control."
                    + packetString);
        }
        // Even if the IQ has not been correctly acknowledged, save the
        // callPeer has a peer with remote control revoked.
        callPeers.remove(callPeer.getAddressAsJid());
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
        DiscoverInfo discoverInfo
            = ((CallPeerJabberImpl) callPeer)
                .getDiscoveryInfo();

        return
            parentProvider.getDiscoveryManager().includesFeature(
                    InputEvtIQ.NAMESPACE_SERVER)
                && (discoverInfo != null)
                && discoverInfo.containsFeature(InputEvtIQ.NAMESPACE_CLIENT);
    }

    /**
     * Handles incoming inputevt packets and passes them to the corresponding
     * method based on their action.
     *
     * @param iqRequest the packet to process.
     * @return IQ response
     */
    @Override
    public IQ handleIQRequest(IQ iqRequest)
    {
        InputEvtIQ inputIQ = (InputEvtIQ)iqRequest;

        if(inputIQ.getAction() == InputEvtAction.NOTIFY)
        {
            // Apply NOTIFY action only to peers which have remote control
            // granted.
            if(callPeers.contains(inputIQ.getFrom()))
            {
                for(RemoteControlExtension p : inputIQ.getRemoteControls())
                {
                    ComponentEvent evt = p.getEvent();
                    processComponentEvent(evt);
                }
            }
        }

        return IQ.createResultIQ(inputIQ);
    }

    @Override
    public Mode getMode()
    {
        return Mode.sync;
    }

    @Override
    public Type getType()
    {
        return Type.set;
    }

    @Override
    public String getElement()
    {
        return InputEvtIQ.ELEMENT;
    }

    @Override
    public String getNamespace()
    {
        return InputEvtIQ.NAMESPACE;
    }
}
