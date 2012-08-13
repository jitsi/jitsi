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
}
