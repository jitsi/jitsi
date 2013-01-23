/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>CallRenderer</tt> represents a renderer for a call. All user
 * interfaces representing a call should implement this interface.
 *
 * @author Yana Stamcheva
 */
public interface CallRenderer
{
    /**
     * Releases the resources acquired by this instance which require explicit
     * disposal (e.g. any listeners added to the depicted
     * <tt>CallConference</tt>, the participating <tt>Call</tt>s, and their
     * associated <tt>CallPeer</tt>s). Invoked by <tt>CallPanel</tt> when it
     * determines that this <tt>CallRenderer</tt> is no longer necessary. 
     */
    public void dispose();

    /**
     * Returns the <tt>Call</tt> depicted by this renderer.
     *
     * @return the <tt>Call</tt> depicted by this renderer
     */
    public Call getCall();

    /**
     * Returns the parent, container which created this <tt>CallRenderer</tt>
     * and in which this <tt>CallRenderer</tt> is added
     *
     * @return the parent, container which created this <tt>CallRenderer</tt>
     * and in which this <tt>CallRenderer</tt> is added
     */
    public CallPanel getCallContainer();

    /**
     * Returns the <tt>CallPeerRenderer</tt> corresponding to the given
     * <tt>callPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt>, for which we're looking for a
     * renderer
     * @return the <tt>CallPeerRenderer</tt> corresponding to the given
     * <tt>callPeer</tt>
     */
    public CallPeerRenderer getCallPeerRenderer(CallPeer callPeer);
}
