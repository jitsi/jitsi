/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

/**
 * Listeners are notified of events occurred to the {@link FacebookSession}.
 * 
 * @author Edgar Poce
 */
public interface FacebookSessionListener
{

    /**
     * Callback method which notifies when the connection is lost
     */
    public void onFacebookConnectionLost();

    /**
     * Callback method which notifies when the buddy list is updated
     */
    public void onBuddyListUpdated();

    /**
     * Callback method which notifies that a new {@link FacebookMessage} arrived
     * 
     * @param message
     *            the new {@link FacebookMessage}
     */
    public void onIncomingChatMessage(FacebookMessage message);

    /**
     * Callback method which notifies that a new typing notification arrived
     * 
     * @param buddyUid
     *            the buddy UID
     * @param state
     *            the typing state
     */
    public void onIncomingTypingNotification(String buddyUid, int state);
}
