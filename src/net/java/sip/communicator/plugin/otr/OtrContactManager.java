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
package net.java.sip.communicator.plugin.otr;

import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The OtrContactManager is used for accessing <tt>OtrContact</tt>s in a static
 * way.
 * 
 * The <tt>OtrContact</tt> class is just a wrapper of [Contact, ContactResource]
 * pairs. Its purpose is for the otr plugin to be able to create different
 * <tt>Session</tt>s for every ContactResource that a Contact has.
 * 
 * Currently, only the Jabber protocol supports ContactResources.
 * 
 * @author Marin Dzhigarov
 *
 */
public class OtrContactManager implements ServiceListener
{

    /**
     * The logger
     */
    private final Logger logger = Logger.getLogger(OtrContactManager.class);

    /**
     * A map that caches OtrContacts to minimize memory usage.
     */
    private static final Map<Contact, List<OtrContact>> contactsMap =
        new ConcurrentHashMap<Contact, List<OtrContact>>();

    /**
     * The <tt>OtrContact</tt> class is just a wrapper of
     * [Contact, ContactResource] pairs. Its purpose is for the otr plugin to be
     * able to create different <tt>Session</tt>s for every ContactResource that
     * a Contact has.
     * 
     * @author Marin Dzhigarov
     *
     */
    public static class OtrContact
    {
        public final Contact contact;

        public final ContactResource resource;

        private OtrContact(Contact contact, ContactResource resource)
        {
            this.contact = contact;
            this.resource = resource;
        }

        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;

            if (!(obj instanceof OtrContact))
                return false;

            OtrContact other = (OtrContact) obj;

            if (this.contact != null && this.contact.equals(other.contact))
            {
                if (this.resource != null && resource.equals(other.resource))
                    return true;
                if (this.resource == null && other.resource == null)
                    return true;
                return false;
            }
            return false;
        }

        public int hashCode()
        {
            int result = 17;

            result = 31 * result + (contact == null ? 0 : contact.hashCode());
            result = 31 * result + (resource == null ? 0 : resource.hashCode());

            return result;
        }
    }

    /**
     * Gets the <tt>OtrContact</tt> that represents this
     * [Contact, ContactResource] pair from the cache. If such pair does not
     * still exist it is then created and cached for further usage.
     * 
     * @param contact the <tt>Contact</tt> that the returned OtrContact
     *                  represents.
     * @param resource the <tt>ContactResource</tt> that the returned OtrContact
     *                  represents.
     * @return The <tt>OtrContact</tt> that represents this
     *                  [Contact, ContactResource] pair.
     */
    public static OtrContact getOtrContact(
        Contact contact, ContactResource resource)
    {
        if (contact == null)
            return null;

        List<OtrContact> otrContactsList = contactsMap.get(contact);
        if (otrContactsList != null)
        {
            for (OtrContact otrContact : otrContactsList)
            {
                if (resource != null && resource.equals(otrContact.resource))
                    return otrContact;
            }
            OtrContact otrContact = new OtrContact(contact, resource);
            synchronized (otrContactsList)
            {
                while (!otrContactsList.contains(otrContact))
                    otrContactsList.add(otrContact);
            }
            return otrContact;
        }
        else
        {
            synchronized (contactsMap)
            {
                while (!contactsMap.containsKey(contact))
                {
                    otrContactsList = new ArrayList<OtrContact>();
                    contactsMap.put(contact, otrContactsList);
                }
            }
            return getOtrContact(contact, resource);
        }
    }

    /**
     * Cleans up unused cached up Contacts.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object service
            = OtrActivator.bundleContext.getService(event.getServiceReference());

        if (!(service instanceof ProtocolProviderService))
            return;
    
        if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Unregistering a ProtocolProviderService, cleaning"
                            + " OTR's Contact to OtrContact map");
            }

            ProtocolProviderService provider
                = (ProtocolProviderService) service;
    
            synchronized(contactsMap)
            {
                Iterator<Contact> i = contactsMap.keySet().iterator();
    
                while (i.hasNext())
                {
                    if (provider.equals(i.next().getProtocolProvider()))
                        i.remove();
                }
            }
        }
    }
}
