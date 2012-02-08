/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * Provides implementations for some of the methods in the <tt>Call</tt>
 * abstract class to facilitate implementations.
 *
 * @param <T> the peer extension class like for example <tt>CallPeerSipImpl</tt>
 * or <tt>CallPeerJabberImpl</tt>
 * @param <U> the provider extension class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt> or
 * <tt>ProtocolProviderServiceJabberImpl</tt>
 *
 * @author Emil Ivov
 */
public abstract class AbstractCall<T extends CallPeer,
                                   U extends ProtocolProviderService>
    extends Call
{
    /**
     * A list containing all <tt>CallPeer</tt>s of this call.
     */
    private Vector<T> callPeers = new Vector<T>();

    /**
     * List of <tt>CallPeer</tt> from other protocol (cross-protocol conference
     * call).
     */
    private Vector<CallPeer> crossProtocolCallPeers = new Vector<CallPeer>();

    /**
     * The <tt>CallGroup</tt> of this <tt>Call</tt>.
     */
    protected CallGroup callGroup = null;

    /**
     * Creates a new Call instance.
     *
     * @param sourceProvider the proto provider that created us.
     */
    protected AbstractCall(U sourceProvider)
    {
        super(sourceProvider);
    }

    /**
     * Returns an iterator over all call peers.
     *
     * @return an Iterator over all peers currently involved in the call.
     */
    public Iterator<T> getCallPeers()
    {
        return new LinkedList<T>(getCallPeersVector()).iterator();
    }

    /**
     * Returns the number of peers currently associated with this call.
     *
     * @return an <tt>int</tt> indicating the number of peers currently
     *         associated with this call.
     */
    public int getCallPeerCount()
    {
        return callPeers.size();
    }

    /**
     * Returns the {@link Vector} containing {@link CallPeer}s currently
     * part of this call. This method should eventually be removed and code
     * that is using it in the descendants should be brought here.
     *
     * @return  the {@link Vector} containing {@link CallPeer}s currently
     * participating in this call.
     */
    protected Vector<T> getCallPeersVector()
    {
        return callPeers;
    }

    /**
     * Returns an iterator over all cross-protocol call peers.
     *
     * @return an Iterator over all cross-protocol peers currently involved in
     * the call.
     */
    public Iterator<CallPeer> getCrossProtocolCallPeers()
    {
        return new LinkedList<CallPeer>(getCrossProtocolCallPeersVector()).
            iterator();
    }

    /**
     * Returns the number of cross-protocol peers currently associated with this
     * call.
     *
     * @return an <tt>int</tt> indicating the number of cross-protocol peers
     * currently associated with this call.
     */
    public int getCrossProtocolCallPeerCount()
    {
        return crossProtocolCallPeers.size();
    }

    /**
     * Returns the {@link Vector} containing cross-protocol {@link CallPeer}s
     * currently part of this call. This method should eventually be removed and
     * code that is using it in the descendants should be brought here.
     *
     * @return  the {@link Vector} containing cross-protocol {@link CallPeer}s
     * currently participating in this call.
     */
    protected Vector<CallPeer> getCrossProtocolCallPeersVector()
    {
        return crossProtocolCallPeers;
    }

    /**
     * Returns a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this call.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance that
     * created this call.
     */
    @SuppressWarnings("unchecked")
    public U getProtocolProvider()
    {
        return (U) super.getProtocolProvider();
    }

    /**
     * Returns the <tt>CallGroup</tt> from which this <tt>Call</tt> belongs.
     *
     * @return <tt>CallGroup</tt> or null if the <tt>Call</tt> does not belongs
     * to a <tt>CallGroup</tt>
     */
    public CallGroup getCallGroup()
    {
        return callGroup;
    }

    /**
     * Sets the <tt>CallGroup</tt> of this <tt>Call</tt>.
     *
     * @param callGroup <tt>CallGroup</tt> to set
     */
    public void setCallGroup(CallGroup callGroup)
    {
        this.callGroup = callGroup;
    }
}
