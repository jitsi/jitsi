/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

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
     * Returns the call represented by this call renderer.
     *
     * @return the call represented by this call renderer
     */
    public Call getCall();

    /**
     * Enters in full screen mode.
     */
    public void enterFullScreen();

    /**
     * Exits the full screen mode.
     */
    public void exitFullScreen();

    /**
     * Ensures the size of the window.
     *
     * @param component the component, which size should be considered
     * @param width the desired width
     * @param height the desired height
     */
    public void ensureSize(Component component, int width, int height);

    /**
     * Returns the parent call container, where this renderer is contained.
     *
     * @return the parent call container, where this renderer is contained
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

    /**
     * Indicates that the given conference member has been added to the given
     * peer.
     *
     * @param callPeer the parent call peer
     * @param conferenceMember the member that was added
     */
    public void conferenceMemberAdded(  CallPeer callPeer,
                                        ConferenceMember conferenceMember);

    /**
     * Indicates that the given conference member has been removed from the
     * given peer.
     *
     * @param callPeer the parent call peer
     * @param conferenceMember the member that was removed
     */
    public void conferenceMemberRemoved(CallPeer callPeer,
                                        ConferenceMember conferenceMember);
}
