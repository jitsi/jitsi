/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.protocol.*;

public class OtrContactManager
{
    
    private static final Map<Contact, List<OtrContact>> contactsMap =
        new ConcurrentHashMap<Contact, List<OtrContact>>();
    
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

    public static OtrContact getOtrContact(
        Contact contact, ContactResource resource)
    {
        if (contact == null)
            return null;

        if (!contactsMap.containsKey(contact))
        {
            List<OtrContact> otrContactsList = new ArrayList<OtrContact>();
            contactsMap.put(contact, otrContactsList);
        }

        List<OtrContact> otrContactList = contactsMap.get(contact);
        for (OtrContact otrContact : otrContactList)
        {
            if (otrContact.resource.equals(resource))
                return otrContact;
        }
        OtrContact otrContact = new OtrContact(contact, resource);
        otrContactList.add(otrContact);
        return otrContact;
    }
}
