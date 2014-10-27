/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

/**
 * IRC connection listener interface for events on connection interruptions.
 *
 * @author Danny van Heumen
 */
public interface IrcConnectionListener
{
    /**
     * Event for any kind of connection interruption.
     *
     * IRC recognizes the ERROR message that signals a fatal or serious error in
     * the IRC connection. Upon receiving such an error, IRC assumes unreliable
     * connection thus disconnects its components and issues this event to
     * signal the event to the listener.
     *
     * Some IRC servers will also issue an ERROR message as a reply upon
     * receiving the QUIT command from the client, i.e. the local user.
     *
     * @param connection the connection that gets interrupted
     */
    void connectionInterrupted(IrcConnection connection);
}
