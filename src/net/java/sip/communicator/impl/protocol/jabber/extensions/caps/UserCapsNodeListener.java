/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

/**
 * Represents a listener of events notifying about changes in the list of user
 * caps nodes of <tt>EntityCapsManager</tt>.
 *
 * @author Lubomir Marinov
 */
public interface UserCapsNodeListener
{
    /**
     * Notifies this listener that an <tt>EntityCapsManager</tt> has added a
     * record for a specific user about the caps node the user has.
     *
     * @param user the user (full JID)
     * @param node the entity caps node#ver
     * @param online indicates if the user for which we're notified is online
     */
    public void userCapsNodeAdded(String user, String node, boolean online);

    /**
     * Notifies this listener that an <tt>EntityCapsManager</tt> has removed a
     * record for a specific user about the caps node the user has.
     *
     * @param user the user (full JID)
     * @param node the entity caps node#ver
     * @param online indicates if the user for which we're notified is online
     */
    public void userCapsNodeRemoved(String user, String node, boolean online);
}
