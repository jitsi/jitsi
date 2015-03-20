/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

/**
 * Interface of the presence watcher.
 * 
 * This interface defines the requirements of a presence watcher. The watcher is
 * used to update contact presence for a selection of nicks.
 * 
 * @author Danny van Heumen
 */
public interface PresenceWatcher
{
    /**
     * Add a nick to the list.
     *
     * @param nick the nick
     */
    void add(String nick);

    /**
     * Remove a nick from the list.
     *
     * @param nick the nick
     */
    void remove(String nick);
}
