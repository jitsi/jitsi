/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * An event listener that should be implemented by parties interested in
 * remote control feature (i.e desktop sharing).
 *
 * @author Sebastien Vincent
 */
public interface RemoteControlListener
{
    /**
     * This method is called when remote control has been granted.
     *
     * @param event <tt>RemoteControlGrantedEvent</tt>
     */
    public void remoteControlGranted(RemoteControlGrantedEvent event);

    /**
     * This method is called when remote control has been revoked.
     *
     * @param event <tt>RemoteControlRevokedEvent</tt>
     */
    public void remoteControlRevoked(RemoteControlRevokedEvent event);
}
