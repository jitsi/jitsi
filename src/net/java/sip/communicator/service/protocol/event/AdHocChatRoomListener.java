/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * A listener which dispatches events notifying ad-hoc chat rooms who have been
 * created, joined and destroyed.
 * 
 * @author Valentin Martinet
 */
public interface AdHocChatRoomListener
{
    /**
     * Called when we receive an <tt>AdHocChatRoomCreatedEvent</tt>.
     * 
     * @param evt the <tt>AdHocChatRoomCreatedEvent</tt>
     */
    public void adHocChatRoomCreated(AdHocChatRoomCreatedEvent evt);

    /**
     * Called when we receive an <tt>AdHocChatRoomDestroyedEvent</tt>.
     * 
     * @param evt the <tt>AdHocChatRoomDestroyedEvent</tt>
     */
    public void adHocChatRoomDestroyed(AdHocChatRoomDestroyedEvent evt);
}
