/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.globaldisplaydetails.event;

import java.util.*;

/**
 * The listener interface for receiving global display details events. Notifies
 * all interested parties when a change in the global display name or avatar
 * has occurred.
 *
 * @see GlobalDisplayNameChangeEvent
 *
 * @author Yana Stamcheva
 */
public interface GlobalDisplayDetailsListener
    extends EventListener
{
    /**
     * Indicates a change in the global display name.
     *
     * @param event the event containing the new global display name
     */
    public void globalDisplayNameChanged(GlobalDisplayNameChangeEvent event);

    /**
     * Indicates a change in the global avatar.
     *
     * @param event the event containing the new global avatar
     */
    public void globalDisplayAvatarChanged(GlobalAvatarChangeEvent event);
}
