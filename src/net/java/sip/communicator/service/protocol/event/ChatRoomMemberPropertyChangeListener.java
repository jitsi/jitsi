package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The <tt>ChatRoomMemberPropertyChangeListener</tt> receives events notifying 
 * interested parties that a property of the corresponding chat room member 
 * (e.g. such as its nickname) has been modified.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface ChatRoomMemberPropertyChangeListener
    extends EventListener
{
    /**
     * Called to indicate that a chat room member property has been modified.
     * 
     * @param event the <tt>ChatRoomMemberPropertyChangeEvent</tt> containing
     * the name of the property that has just changed, as well as its old and
     * new values.
     */
    public void chatRoomPropertyChanged(ChatRoomMemberPropertyChangeEvent event);
}
