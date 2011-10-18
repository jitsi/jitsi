/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The listener interface for receiving geolocation events. The class that is
 * interested in processing a avatar event implements this interface, and the
 * object created with that class is registered with the avatar operation set,
 * using its <code>addAvatarListener</code> method. When a avatar event occurs,
 * that object's <code>avatarChanged</code> method is invoked.
 * 
 * @see AvatarEvent
 * 
 * @author Damien Roth
 */
public interface AvatarListener
    extends EventListener
{
    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     * 
     * @param event the event containing the new image
     */
    public void avatarChanged(AvatarEvent event);
}
