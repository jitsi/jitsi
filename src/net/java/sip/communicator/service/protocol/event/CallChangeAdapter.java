/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * An abstract adapter class for receiving call-state (change) events. This
 * class exists only as a convenience for creating listener objects.
 * <p>
 * Extend this class to create a <code>CallChangeEvent</code> listener and
 * override the methods for the events of interest. (If you implement the
 * <code>CallChangeListener</code> interface, you have to define all of the
 * methods in it. This abstract class defines null methods for them all, so you
 * only have to define methods for events you care about.)
 * </p>
 *
 * @see CallChangeEvent
 * @see CallChangeListener
 *
 * @author Lubomir Marinov
 */
public abstract class CallChangeAdapter
    implements CallChangeListener
{

    /**
     * A dummy implementation of this listener's callPeerAdded() method.
     *
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerAdded(CallPeerEvent evt)
    {
    }


    /**
     * A dummy implementation of this listener's callPeerRemoved() method.
     *
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {
    }


    /**
     * A dummy implementation of this listener's callStateChanged() method.
     *
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     * calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt)
    {
    }
}
