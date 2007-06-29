/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoIcqImpl
    implements OperationSetServerStoredContactInfo
{
    private InfoRetreiver infoRetreiver;

    protected OperationSetServerStoredContactInfoIcqImpl
        (InfoRetreiver infoRetreiver)
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
        if(detailClass.equals(ServerStoredDetails.ImageDetail.class) && 
            contact.getImage() != null)
        {
            Vector res = new Vector();
            res.add(new ServerStoredDetails.ImageDetail(
                "Image", contact.getImage()));
            return res.iterator();    
        }
        return infoRetreiver.getDetailsAndDescendants(contact.getAddress(), detailClass);
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
        if(detailClass.equals(ServerStoredDetails.ImageDetail.class) && 
            contact.getImage() != null)
        {
            Vector res = new Vector();
            res.add(new ServerStoredDetails.ImageDetail(
                "Image", contact.getImage()));
            return res.iterator();    
        }
        return infoRetreiver.getDetails(contact.getAddress(), detailClass);
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
        List res = infoRetreiver.getContactDetails(contact.getAddress());
        
        if(contact.getImage() != null)
        {
            res.add(new ServerStoredDetails.ImageDetail(
                "Image", contact.getImage()));
        }

        return res.iterator();
    }
}