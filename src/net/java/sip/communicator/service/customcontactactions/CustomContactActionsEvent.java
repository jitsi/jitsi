/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.customcontactactions;

import java.util.*;

/**
 * An event from custom actions.
 * @author Damian Minkov
 */
public class CustomContactActionsEvent
    extends EventObject
{
    /**
     * Creates the event with source.
     * @param source the source of the event.
     */
    public CustomContactActionsEvent(Object source)
    {
        super(source);
    }
}
