/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * An abstract base implementation of the {@link Contact} interface which is to
 * aid implementers.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractContact
    implements Contact
{
    /**
     * The list of <tt>ContactResourceListener</tt>-s registered in this
     * contact.
     */
    private Collection<ContactResourceListener> resourceListeners;

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        else if (obj == this)
            return true;
        else if (!obj.getClass().equals(getClass()))
            return false;
        else
        {
            Contact contact = (Contact) obj;
            ProtocolProviderService protocolProvider
                = contact.getProtocolProvider();
            ProtocolProviderService thisProtocolProvider
                = getProtocolProvider();

            if ((protocolProvider == null)
                    ? (thisProtocolProvider == null)
                    : protocolProvider.equals(thisProtocolProvider))
            {
                String address = contact.getAddress();
                String thisAddress = getAddress();

                return
                    (address == null)
                        ? (thisAddress == null)
                        : address.equals(thisAddress);
            }
            else
                return false;
        }
    }

    @Override
    public int hashCode()
    {
        int hashCode = 0;

        ProtocolProviderService protocolProvider = getProtocolProvider();

        if (protocolProvider != null)
            hashCode += protocolProvider.hashCode();

        String address = getAddress();

        if (address != null)
            hashCode += address.hashCode();

        return hashCode;
    }

    /**
     * Indicates if this contact supports resources.
     * <p>
     * This default implementation indicates no support for contact resources.
     *
     * @return <tt>true</tt> if this contact supports resources, <tt>false</tt>
     * otherwise
     */
    public boolean supportResources()
    {
        return false;
    }

    /**
     * Returns a collection of resources supported by this contact or null
     * if it doesn't support resources.
     * <p>
     * This default implementation indicates no support for contact resources.
     *
     * @return a collection of resources supported by this contact or null
     * if it doesn't support resources
     */
    public Collection<ContactResource> getResources()
    {
        return null;
    }

    /**
     * Adds the given <tt>ContactResourceListener</tt> to listen for events
     * related to contact resources changes.
     *
     * @param l the <tt>ContactResourceListener</tt> to add
     */
    public void addResourceListener(ContactResourceListener l)
    {
        if (resourceListeners == null)
            resourceListeners = new ArrayList<ContactResourceListener>();

        synchronized (resourceListeners)
        {
            resourceListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>ContactResourceListener</tt> listening for events
     * related to contact resources changes.
     *
     * @param l the <tt>ContactResourceListener</tt> to remove
     */
    public void removeResourceListener(ContactResourceListener l)
    {
        if (resourceListeners == null)
            return;

        synchronized (resourceListeners)
        {
            resourceListeners.remove(l);
        }
    }

    /**
     * Notifies all registered <tt>ContactResourceListener</tt>s that an event
     * has occurred.
     *
     * @param event the <tt>ContactResourceEvent</tt> to fire notification for
     */
    protected void fireContactResourceEvent(ContactResourceEvent event)
    {
        if (resourceListeners == null)
            return;

        Collection<ContactResourceListener> listeners;
        synchronized (resourceListeners)
        {
            listeners
                = new ArrayList<ContactResourceListener>(resourceListeners);
        }

        Iterator<ContactResourceListener> listenersIter = listeners.iterator();
        while (listenersIter.hasNext())
        {
            if (event.getEventType() == ContactResourceEvent.RESOURCE_ADDED)
                listenersIter.next().contactResourceAdded(event);
            else if (event.getEventType()
                        == ContactResourceEvent.RESOURCE_REMOVED)
                listenersIter.next().contactResourceRemoved(event);
            else if (event.getEventType()
                        == ContactResourceEvent.RESOURCE_MODIFIED)
                listenersIter.next().contactResourceModified(event);
        }
    }
    
    /**
     * Returns the same as <tt>getAddress</tt> function.
     * 
     * @return the address of the contact.
     */
    public String getPersistableAddress()
    {
        return getAddress();
    }

    /**
     * Whether contact is mobile one. Logged in only from mobile device.
     * @return whether contact is mobile one.
     */
    public boolean isMobile()
    {
        return false;
    }
}
