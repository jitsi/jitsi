/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * An event filter that decides whether an instant message event should be 
 * filtered or not. For instance, maybe some type of received messages should not be 
 * shown in the chat windows. In such cases implementations of this filter would block
 * this message before it is delivered to other IM listeners.
 * <p>
 * Note that in order to be able to use this Filter, protocols should implement 
 * <tt>OperationSetMessageFiltering</tt>.
 * <p>
 * @author Keio Kraaner
 */
public interface EventFilter
{
    /**
     * Checks if an event should be filtered out or processed.
     * 
     * @param msg The event that should be checked
     * @return <tt>true</tt> if the event was filtered out, otherwise 
     * <tt>false</tt>.
     */
    public boolean filterEvent(EventObject msg);
}
