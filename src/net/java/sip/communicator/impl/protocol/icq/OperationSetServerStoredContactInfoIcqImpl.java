/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoIcqImpl
    implements OperationSetServerStoredContactInfo
{
    private InfoRetreiver infoRetreiver;
    
    /**
     * The icq provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    protected OperationSetServerStoredContactInfoIcqImpl
        (InfoRetreiver infoRetreiver, ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.infoRetreiver = infoRetreiver;
        this.icqProvider = icqProvider;
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
    public Iterator<GenericDetail> getDetailsAndDescendants(
        Contact contact,
        Class<? extends GenericDetail> detailClass)
    {
        assertConnected();
        
        if(detailClass.equals(ServerStoredDetails.ImageDetail.class) && 
            contact.getImage() != null)
        {
            List<GenericDetail> res = new Vector<GenericDetail>();
            res.add(
                new ServerStoredDetails.ImageDetail(
                        "Image",
                        contact.getImage()));
            return res.iterator();    
        }
        return
            infoRetreiver
                .getDetailsAndDescendants(contact.getAddress(), detailClass);
    }

    /**
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator<GenericDetail> getDetails(
        Contact contact,
        Class<? extends GenericDetail> detailClass)
    {
        assertConnected();
        
        if(detailClass.equals(ServerStoredDetails.ImageDetail.class) && 
            contact.getImage() != null)
        {
            List<GenericDetail> res = new Vector<GenericDetail>();
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
    public Iterator<GenericDetail> getAllDetailsForContact(Contact contact)
    {
        assertConnected();
        
        List<GenericDetail> res
            = infoRetreiver.getContactDetails(contact.getAddress());
        
        if(contact.getImage() != null)
        {
            res.add(new ServerStoredDetails.ImageDetail(
                "Image", contact.getImage()));
        }

        return res.iterator();
    }
    
    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (icqProvider == null)
            throw new IllegalStateException(
                "The icq provider must be non-null and signed on the ICQ "
                +"service before being able to communicate.");
        if (!icqProvider.isRegistered())
            throw new IllegalStateException(
                "The icq provider must be signed on the ICQ service before "
                +"being able to communicate.");
    }
}
