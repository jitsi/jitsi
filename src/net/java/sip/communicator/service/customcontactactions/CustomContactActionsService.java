/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.customcontactactions;

import java.util.*;

/**
 * The <tt>CustomContactActionsService</tt> can be used to define a set of
 * custom actions for a contact list entry.
 *
 * @author Damian Minkov
 */
public interface CustomContactActionsService<T>
{
    /**
     * Returns the template class that this service has been initialized with
     *
     * @return the template class
     */
    public Class<T> getContactSourceClass();

    /**
     * Returns all custom actions defined by this service.
     *
     * @return an iterator over a list of <tt>ContactAction</tt>s
     */
    public Iterator<ContactAction<T>> getCustomContactActions();

    /**
     * Registers a CustomContactActionsListener with this service so that it gets
     * notifications of various events.
     *
     * @param listener the <tt>CustomContactActionsListener</tt> to register.
     */
    public void addCustomContactActionsListener(
        CustomContactActionsListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications.
     *
     * @param listener the <tt>CustomContactActionsListener</tt> to unregister.
     */
    public void removeCustomContactActionsListener(
        CustomContactActionsListener listener);
}
