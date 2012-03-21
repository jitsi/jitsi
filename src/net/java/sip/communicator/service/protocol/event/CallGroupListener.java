/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Instances of this class are used for listening for notifications coming out
 * of a <tt>CallGroup</tt>.
 *
 * @author Sebastien Vincent
 */
public interface CallGroupListener
    extends EventListener
{
    /**
     * Notifies this instance that a specific <tt>Call</tt> has been added to a
     * <tt>CallGroup</tt>.
     *
     * @param evt a <tt>CallGroupEvent</tt> which specifies the <tt>Call</tt>
     * which has been added to a <tt>CallGroup</tt>
     */
    public void callAdded(CallGroupEvent evt);

    /**
     * Notified when a call are removed from a <tt>CallGroup</tt>.
     *
     * @param evt event
     */
    public void callRemoved(CallGroupEvent evt);
}
