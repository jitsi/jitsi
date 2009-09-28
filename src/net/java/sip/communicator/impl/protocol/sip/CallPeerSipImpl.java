/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Our SIP implementation of the default CallPeer;
 *
 * @author Emil Ivov
 */
public class CallPeerSipImpl
    extends AbstractCallPeer
    implements SessionCreatorCallback
{
    private static final Logger logger
        = Logger.getLogger(CallPeerSipImpl.class);

    /**
     * The sip address of this peer
     */
    private Address peerAddress = null;

    /**
     * A byte array containing the image/photo representing the call peer.
     */
    private byte[] image;

    /**
     * A string uniquely identifying the peer.
     */
    private String peerID;

    /**
     * The call this peer belongs to.
     */
    private CallSipImpl call;

    /**
     * The JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     */
    private Dialog jainSipDialog = null;

    /**
     * The SDP session description that we have received from this call
     * peer.
     */
    private String sdpDescription = null;

    /**
     * The SIP transaction that established this call. This was previously kept
     * in the jain-sip dialog but got deprected there so we're now keeping it
     * here.
     */
    private Transaction firstTransaction = null;

    /**
     * The jain sip provider instance that is responsible for sending and
     * receiving requests and responses related to this call peer.
     */
    private SipProvider jainSipProvider = null;

    /**
     * The transport address that we are using to address the peer or the
     * first one that we'll try when we next send them a message (could be the
     * address of our sip registrar).
     */
    private InetSocketAddress transportAddress = null;

    /**
     * A URL pointing to a location with call information or a call control
     * web interface related to this peer.
     */
    private URL callControlURL = null;

    /**
     * The <tt>CallPeerSoundLevelListener</tt>-s registered to get
     * <tt>CallPeerSoundLevelEvent</tt>-s
     */
    private final List<CallPeerSoundLevelListener> soundLevelListeners
        = new ArrayList<CallPeerSoundLevelListener>();

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param peerAddress the JAIN SIP <tt>Address</tt> of the new call peer.
     * @param owningCall the call that contains this call peer.
     */
    public CallPeerSipImpl(Address     peerAddress,
                           CallSipImpl owningCall)
    {
        this.peerAddress = peerAddress;
        this.call = owningCall;

        //create the uid
        this.peerID = String.valueOf( System.currentTimeMillis())
                             + String.valueOf(hashCode());
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        return this.peerAddress.getURI().toString();
    }

    /**
     * Specifies the address, phone number, or other protocol specific
     * identifier that represents this call peer. This method is to be
     * used by service users and MUST NOT be called by the implementation.
     *
     * @param address The address of this call peer.
     */
    public void setAddress(Address address)
    {
        String oldAddress = getAddress();

        if(peerAddress.equals(address))
            return;

        this.peerAddress = address;
        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_ADDRESS_CHANGE,
                oldAddress,
                address.toString());
    }

    /**
     * Returns a human readable name representing this peer.
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        String displayName = peerAddress.getDisplayName();
        return (displayName == null)
                    ? peerAddress.getURI().toString()
                    : displayName;
    }

    /**
     * Sets a human readable name representing this peer.
     *
     * @param displayName the peer's display name
     */
    protected void setDisplayName(String displayName)
    {
        String oldName = getDisplayName();
        try
        {
            this.peerAddress.setDisplayName(displayName);
        }
        catch (ParseException ex)
        {
            //couldn't happen
            logger.error(ex.getMessage(), ex);
            throw new IllegalArgumentException(ex.getMessage());
        }

        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_DISPLAY_NAME_CHANGE,
                oldName,
                displayName);
    }

    /**
     * The method returns an image representation of the call peer
     * (e.g.
     *
     * @return byte[] a byte array containing the image or null if no image
     *   is available.
     */
    public byte[] getImage()
    {
        return image;
    }

    /**
     * Sets the byte array containing an image representation (photo or picture)
     * of the call peer.
     *
     * @param image a byte array containing the image
     */
    protected void setImage(byte[] image)
    {
        byte[] oldImage = getImage();
        this.image = image;

        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_IMAGE_CHANGE,
                oldImage,
                image);
    }

    /**
     * Returns a unique identifier representing this peer.
     *
     * @return an identifier representing this call peer.
     */
    public String getPeerID()
    {
        return peerID;
    }

    /**
     * Returns the latest sdp description that this peer sent us.
     * @return the latest sdp description that this peer sent us.
     */
    public String getSdpDescription()
    {
        return sdpDescription;
    }

    /**
     * Sets the String that serves as a unique identifier of this
     * CallPeer.
     * @param peerID the ID of this call peer.
     */
    protected void setPeerID(String peerID)
    {
        this.peerID = peerID;
    }

    /**
     * Returns a reference to the call that this peer belongs to. Calls
     * are created by underlying telephony protocol implementations.
     *
     * @return a reference to the call containing this peer.
     */
    public CallSipImpl getCall()
    {
        return call;
    }

    /**
     * Sets the call containing this peer.
     * @param call the call that this call peer is
     * partdicipating in.
     */
    protected void setCall(CallSipImpl call)
    {
        this.call = call;
    }

    /**
     * Sets the sdp description for this call peer.
     *
     * @param sdpDescription the sdp description for this call peer.
     */
    public void setSdpDescription(String sdpDescription)
    {
        this.sdpDescription = sdpDescription;
    }

    /**
     * Returns the javax.sip Address of this call peer.
     * @return the javax.sip Address of this call peer.
     */
    public Address getJainSipAddress()
    {
        return peerAddress;
    }

    /**
     * Sets the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     * @param dialog the JAIN SIP dialog that has been created by the
     * application for this call.
     */
    public void setDialog(Dialog dialog)
    {
        this.jainSipDialog = dialog;
    }

    /**
     * Returns the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     *
     * @return the JAIN SIP dialog that has been created by the application for
     * communication with this call peer.
     */
    public Dialog getDialog()
    {
        return jainSipDialog;
    }

    /**
     * Sets the transaction instance that contains the INVITE which started
     * this call.
     *
     * @param transaction the Transaction that initiated this call.
     */
    public void setFirstTransaction(Transaction transaction)
    {
        this.firstTransaction = transaction;
    }

    /**
     * Returns the transaction instance that contains the INVITE which started
     * this call.
     *
     * @return the Transaction that initiated this call.
     */
    public Transaction getFirstTransaction()
    {
        return firstTransaction;
    }

    /**
     * Sets the jain sip provider instance that is responsible for sending and
     * receiving requests and responses related to this call peer.
     *
     * @param jainSipProvider the <tt>SipProvider</tt> that serves this call
     * peer.
     */
    public void setJainSipProvider(SipProvider jainSipProvider)
    {
        this.jainSipProvider = jainSipProvider;
    }

    /**
     * Returns the jain sip provider instance that is responsible for sending
     * and receiving requests and responses related to this call peer.
     *
     * @return the jain sip provider instance that is responsible for sending
     * and receiving requests and responses related to this call peer.
     */
    public SipProvider getJainSipProvider()
    {
        return jainSipProvider;
    }

    /**
     * The address that we have used to contact this peer. In cases
     * where no direct connection has been established with the peer,
     * this method will return the address that will be first tried when
     * connection is established (often the one used to connect with the
     * protocol server). The address may change during a session and
     *
     * @param transportAddress The address that we have used to contact this
     * peer.
     */
    public void setTransportAddress(InetSocketAddress transportAddress)
    {
        InetSocketAddress oldTransportAddress = this.transportAddress;
        this.transportAddress = transportAddress;

        this.fireCallPeerChangeEvent(
            CallPeerChangeEvent
                .CALL_PEER_TRANSPORT_ADDRESS_CHANGE,
                oldTransportAddress,
                transportAddress);
    }

    /**
     * Returns the protocol provider that this peer belongs to.
     * @return a reference to the ProtocolProviderService that this peer
     * belongs to.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.getCall().getProtocolProvider();
    }

    /**
     * Returns the contact corresponding to this peer or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this peer or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        ProtocolProviderService pps = call.getProtocolProvider();
        OperationSetPresenceSipImpl opSetPresence
            = (OperationSetPresenceSipImpl) pps
                .getOperationSet(OperationSetPresence.class);

        return opSetPresence.resolveContactID(getAddress());
    }

    /**
     * Returns a URL pointing ta a location with call control information for
     * this peer or <tt>null</tt> if no such URL is available for this
     * call peer.
     *
     * @return a URL link to a location with call information or a call control
     * web interface related to this peer or <tt>null</tt> if no such URL
     * is available.
     */
    public URL getCallInfoURL()
    {
        return this.callControlURL;
    }

    /**
     * Returns a URL pointing ta a location with call control information for
     * this peer.
     *
     * @param callControlURL a URL link to a location with call information or
     * a call control web interface related to this peer.
     */
    public void setCallInfoURL(URL callControlURL)
    {
        this.callControlURL = callControlURL;
    }

    /**
     * Determines whether the audio stream (if any) being sent to this
     * peer is mute.
     *
     * @return <tt>true</tt> if an audio stream is being sent to this
     *         peer and it is currently mute; <tt>false</tt>, otherwise
     */
    public boolean isMute()
    {
        CallSipImpl call = this.call;

        if (call != null)
        {
            CallSession callSession = call.getMediaCallSession();

            if (callSession != null)
                return callSession.isMute();
        }
        return false;
    }

    /**
     * Sets the security status to ON for this call peer.
     *
     * @param sessionType the type of the call session - audio or video.
     * @param cipher the cipher
     * @param securityString the SAS
     * @param isVerified indicates if the SAS has been verified
     */
    public void securityOn(  int sessionType,
                                String cipher,
                                String securityString,
                                boolean isVerified)
    {
        fireCallPeerSecurityOnEvent( sessionType,
                                     cipher,
                                     securityString,
                                     isVerified);
    }

    /**
     * Sets the security status to OFF for this call peer.
     *
     * @param sessionType the type of the call session - audio or video.
     */
    public void securityOff(int sessionType)
    {
        fireCallPeerSecurityOffEvent(sessionType);
    }

    /**
     * Sets the security message associated with a failure/warning or
     * information coming from the encryption protocol.
     *
     * @param messageType the type of the message.
     * @param i18nMessage the message
     * @param severity severity level
     */
    public void securityMessage( String messageType,
                                    String i18nMessage,
                                    int severity)
    {
        fireCallPeerSecurityMessageEvent(messageType,
                                         i18nMessage,
                                         severity);
    }

    /**
     * Adds a specific <tt>CallPeerSoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in sound level related
     * information.
     *
     * @param listener the <tt>CallPeerSoundLevelListener</tt> to add
     */
    public void addCallPeerSoundLevelListener(
        CallPeerSoundLevelListener listener)
    {
        synchronized (soundLevelListeners)
        {
            soundLevelListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>CallPeerSoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in sound level related
     * information.
     *
     * @param listener the <tt>CallPeerSoundLevelListener</tt> to remove
     */
    public void removeCallPeerSoundLevelListener(
        CallPeerSoundLevelListener listener)
    {
        synchronized (soundLevelListeners)
        {
            soundLevelListeners.remove(listener);
        }
    }

    /**
     * Fires a <tt>CallPeerSoundLevelEvent</tt> and notifies all registered
     * listeners.
     *
     * @param level The new sound level
     */
    public void fireCallPeerSoundLevelEvent(int level)
    {
        CallPeerSoundLevelEvent event
            = new CallPeerSoundLevelEvent(this, level);

        CallPeerSoundLevelListener[] listeners;

        synchronized(soundLevelListeners)
        {
            listeners =
                soundLevelListeners.toArray(
                    new CallPeerSoundLevelListener[soundLevelListeners.size()]);
        }

        for (CallPeerSoundLevelListener listener : listeners)
        {
            listener.peerSoundLevelChanged(event);
        }
    }
}
