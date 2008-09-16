/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoJabberImpl
    implements OperationSetServerStoredContactInfo
{
    private InfoRetreiver infoRetreiver = null;

    protected OperationSetServerStoredContactInfoJabberImpl(
        InfoRetreiver infoRetreiver)
    {
        this.infoRetreiver = infoRetreiver;
    }
    
    /**
     * returns the user details from the specified class or its descendants
     * the class is one from the
     * net.java.sip.communicator.service.protocol.ServerStoredDetails
     * or implemented one in the operation set for the user info
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator getDetailsAndDescendants(Contact contact, Class detailClass)
    {
        List details = infoRetreiver.getContactDetails(contact.getAddress());
        List result = new LinkedList();

        if(details == null)
            return result.iterator();

        Iterator iter = details.iterator();
        while (iter.hasNext())
        {
            Object item = iter.next();
            if(detailClass.isInstance(item))
                result.add(item);
        }

        return result.iterator();
    }

    /**
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator getDetails(Contact contact, Class detailClass)
    {
        List details = infoRetreiver.getContactDetails(contact.getAddress());
        List result = new LinkedList();

        if(details == null)
            return result.iterator();

        Iterator iter = details.iterator();
        while (iter.hasNext())
        {
            Object item = iter.next();
            if(detailClass.equals(item.getClass()))
                result.add(item);
        }

        return result.iterator();
    }

    /**
     * request the full info for the given uin
     * waits and return this details
     *
     * @param contact Contact
     * @return Iterator
     */
    public Iterator getAllDetailsForContact(Contact contact)
    {
        List details = infoRetreiver.getContactDetails(contact.getAddress());

        if(details == null)
            return new LinkedList().iterator();
        else
            return new LinkedList(details).iterator();
    }
}