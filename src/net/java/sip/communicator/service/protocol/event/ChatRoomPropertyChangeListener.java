package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The ChatRoomChangeListener is receives events notifying interested parties
 * that a property of the corresponding chat room (e.g. such as its subject or
 * type) has been modified.
 *
 * @author Emil Ivov
 */
public interface ChatRoomPropertyChangeListener
    extends EventListener
{
    /**
     * Called to indicate that a property of the corresponding chat room (e.g.
     * its subject or type) have just been modified.
     * @param event the ChatRoomChangeEvent containing the name of the property
     * that has just changed as well as its old and new values.
     */
    public void chatRoomChanged(ChatRoomPropertyChangeEvent event);
}
