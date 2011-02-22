/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;
import java.text.*;

import java.awt.event.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt.*;
import net.java.sip.communicator.service.hid.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implements all desktop sharing server-side related functions for Jabber
 * protocol.
 *
 * @author Sebastien Vincent
 */
public class OperationSetDesktopSharingServerJabberImpl
    extends OperationSetDesktopStreamingJabberImpl
    implements OperationSetDesktopSharingServer,
               RegistrationStateChangeListener,
               PacketListener,
               PacketFilter
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger
            .getLogger(OperationSetDesktopSharingServerJabberImpl.class);

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
                disableRemoteControl(evt.getSourceCallPeer());
            }
        }
    };

    /**
     * If the remote control is authorized and thus enabled.
     */
    private boolean remoteControlEnabled = false;

    /**
     * HID service that will regenerates keyboard and mouse events received in
     * Jabber messages.
     */
    private HIDService hidService = null;

    /**
     * List of callPeers for the desktop sharing session.
     */
    private List<String> callPeers = new ArrayList<String>();

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
        CallJabberImpl call = (CallJabberImpl)super.createVideoCall(uri,
                device);
        CallPeerJabberImpl callPeer = call.getCallPeers().next();
        callPeer.addCallPeerListener(callPeerListener);

        size = (((VideoMediaFormat)call.getDefaultDevice(
                MediaType.VIDEO).getFormat()).getSize());
        origin = null;
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
        CallJabberImpl call = (CallJabberImpl)super.createVideoCall(callee,
                device);
        CallPeerJabberImpl callPeer = call.getCallPeers().next();
        callPeer.addCallPeerListener(callPeerListener);

        size = (((VideoMediaFormat)call.getDefaultDevice(
                MediaType.VIDEO).getFormat()).getSize());
        origin = null;
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
    protected Call createOutgoingVideoCall(String calleeAddress, MediaDevice
            videoDevice)
        throws OperationFailedException
    {
        boolean supported = false;
        String fullCalleeURI = null;

        if (calleeAddress.indexOf('/') > 0)
        {
            fullCalleeURI = calleeAddress;
        }
        else
        {
            fullCalleeURI = parentProvider.getConnection()
                .getRoster().getPresence(calleeAddress).getFrom();
        }

        if (logger.isInfoEnabled())
            logger.info("creating outgoing desktop sharing call...");

        DiscoverInfo di = null;
        try
        {
            // check if the remote client supports inputevt (remote control)
            di = parentProvider.getDiscoveryManager()
                    .discoverInfo(fullCalleeURI);

            if (di.containsFeature(InputEvtIQ.NAMESPACE))
            {
                if (logger.isInfoEnabled())
                    logger.info(fullCalleeURI + ": remote-control supported");

                supported = true;
            }
            else
            {
                if (logger.isInfoEnabled())
                    logger.info(fullCalleeURI +
                            ": remote-control not supported!");

                /* XXX fail or not ? */
                /*
                throw new OperationFailedException(
                        "Failed to create a true desktop sharing.\n"
                            + fullCalleeURI + " does not support inputevt",
                        OperationFailedException.INTERNAL_ERROR);
                */
            }
        }
        catch (XMPPException ex)
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

        if(videoDevice != null)
        {
            call.setVideoDevice(videoDevice);
        }

        /* enable video */
        call.setLocalVideoAllowed(true, getMediaUseCase());
        /* enable remote-control */
        call.setLocalInputEvtAware(supported);



        basicTelephony.createOutgoingCall(call, calleeAddress);
        return call;
    }

    /**
     * Implements OperationSetVideoTelephony#setLocalVideoAllowed(Call,
     * boolean). Modifies the local media setup to reflect the requested setting
     * for the streaming of the local video and then re-invites all
     * CallPeers to re-negotiate the modified media setup.
     *
     * @param call the call where we'd like to allow sending local video.
     * @param allowed <tt>true</tt> if local video transmission is allowed and
     * <tt>false</tt> otherwise.
     *
     *  @throws OperationFailedException if video initialization fails.
     */
    @Override
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {
        ((CallJabberImpl)call).setLocalInputEvtAware(allowed);
        super.setLocalVideoAllowed(call, allowed);
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
    public void setLocalVideoAllowed(Call call,
                                     MediaDevice mediaDevice,
                                     boolean allowed)
        throws OperationFailedException
    {
        ((CallJabberImpl)call).setLocalInputEvtAware(allowed);
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
        if(logger.isInfoEnabled())
            logger.info("Enable remote control");

        CallJabberImpl call = (CallJabberImpl)callPeer.getCall();

        if(call.getLocalInputEvtAware())
        {
            remoteControlEnabled = true;

            if(!callPeers.contains(callPeer.getAddress()))
            {
                callPeers.add(callPeer.getAddress());
            }
        }
    }

    /**
     * Disable desktop remote control. Local desktop stops regenerate keyboard
     * and mouse events received from peer.
     *
     * @param callPeer call peer that will stop controlling on local computer
     */
    public void disableRemoteControl(CallPeer callPeer)
    {
        if(logger.isInfoEnabled())
            logger.info("Disable remote control");

        remoteControlEnabled = false;

        if(callPeers.contains(callPeer.getAddress()))
        {
           callPeers.remove(callPeer.getAddress());
        }
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
        if ((evt.getNewState() == RegistrationState.REGISTERING))
        {
            /* listen to specific inputevt IQ */
            parentProvider.getConnection().addPacketListener(this, this);
        }
    }

    /**
     * Handles incoming inputevt packets and passes them to the corresponding
     * method based on their action.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        //this is not supposed to happen because of the filter ... but still
        if (!(packet instanceof InputEvtIQ))
            return;

        InputEvtIQ inputIQ = (InputEvtIQ)packet;

        if(inputIQ.getAction() != InputEvtAction.NOTIFY)
        {
            return;
        }

        /* do not waste time to parse packet if remote control is not enabled */
        if(!remoteControlEnabled)
        {
            return;
        }

        //first ack all "set" requests.
        if(inputIQ.getType() == IQ.Type.SET)
        {
            IQ ack = IQ.createResultIQ(inputIQ);
            parentProvider.getConnection().sendPacket(ack);
        }

        if(!callPeers.contains(inputIQ.getFrom()))
        {
            return;
        }

        for(RemoteControlExtension p : inputIQ.getRemoteControls())
        {
            ComponentEvent evt = p.getEvent();
            processComponentEvent(evt);
        }
    }

    /**
     * Tests whether or not the specified packet should be handled by this
     * operation set. This method is called by smack prior to packet delivery
     * and it would only accept <tt>InputEvtIQ</tt>s.
     *
     * @param packet the packet to test.
     * @return true if and only if <tt>packet</tt> passes the filter.
     */
    public boolean accept(Packet packet)
    {
        //we only handle InputEvtIQ-s
        if(!(packet instanceof InputEvtIQ))
            return false;

        return true;
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
        if (hidService != null)
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
}
