/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.customcontactactions;

/**
 * Notifies for events coming from custom actions.
 * @author Damian Minkov
 */
public interface CustomContactActionsListener
{
    /**
     * Notifies that object has been updated.
     * @param event the event containing the source which was updated.
     */
    public void updated(CustomContactActionsEvent event);
}
