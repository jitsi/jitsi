/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.packet.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoJabberImpl
    implements OperationSetServerStoredContactInfo
{
    private static final Logger logger =
        Logger.getLogger(OperationSetServerStoredContactInfoJabberImpl.class);
    
    /**
     * A callback to the Jabber provider that created us.
     */
    private ProtocolProviderServiceJabberImpl jabberProvider = null;
    
    // here is kept all the details retreived so far
    private Hashtable retreivedDetails = new Hashtable();

    protected OperationSetServerStoredContactInfoJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.jabberProvider = provider;
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
        List details = getContactDetails(contact.getAddress());
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
        List details = getContactDetails(contact.getAddress());
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
        List details = getContactDetails(contact.getAddress());
        
        if(details == null)
            return new LinkedList().iterator();
        else
            return new LinkedList(details).iterator();
    }
    
    /**
     * request the full info for the given contactAddress
     * waits and return this details
     *
     * @param contactAddress String
     * @return Vector the details
     */
    private List getContactDetails(String contactAddress)
    {
        List result = (List)retreivedDetails.get(contactAddress);

        if(result == null)
        {
            result = new LinkedList();
            try
            {
                XMPPConnection connection = jabberProvider.getConnection();

                if(connection == null || !connection.isAuthenticated())
                    return null;

                VCard card = new VCard();
                card.load(connection, contactAddress);

                String tmp = null;
                
                tmp = card.getFirstName();
                if(tmp != null)
                    result.add(new ServerStoredDetails.FirstNameDetail(tmp));
                
                tmp = card.getMiddleName();
                if(tmp != null)
                    result.add(new ServerStoredDetails.MiddleNameDetail(tmp));
                
                tmp = card.getLastName();
                if(tmp != null)
                    result.add(new ServerStoredDetails.LastNameDetail(tmp));
                
                tmp = card.getNickName();
                if(tmp != null)
                    result.add(new ServerStoredDetails.NicknameDetail(tmp));
                
//                addrField one of 
//                POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, 
//                EXTADR, STREET, LOCALITY, REGION, PCODE, CTRY
//                String[] types = new String[]{"POSTAL", "PARCEL", "DOM", "INTL", 
//                                                "PREF", "POBOX", "EXTADR", 
//                                                "STREET", "LOCALITY", "REGION", 
//                                                "PCODE", "CTRY"};
//                for (String t : types)
//                {
//                    System.out.println("getAddressFieldHome(" + t + ") - " + card.getAddressFieldHome(t));
//                }
                
                //phoneType one of 
//                VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
//                types = new String[]{"VOICE", "FAX", "PAGER", "MSG", "CELL", 
//                                "VIDEO", "BBS", "MODEM", "ISDN", "PCS", "PREF"};
//                for (String t : types)
//                {
//                    System.out.println("getPhoneHome(" + t + ") - " + card.getPhoneHome(t));
//                }
                
                tmp = card.getEmailHome();
                if(tmp != null)
                    result.add(new ServerStoredDetails.EmailAddressDetail(tmp));
                
                    // addrField one of 
//                    POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
//                    LOCALITY, REGION, PCODE, CTRY
//                types = new String[]{"POSTAL", "PARCEL", "DOM", "INTL", "PREF", 
//                "POBOX", "EXTADR", "STREET", "LOCALITY", "REGION", "PCODE", "CTRY"};
//                for (String t : types)
//                {
//                    System.out.println("getAddressFieldWork(" + t + ") - " + card.getAddressFieldWork(t));
//                }
                                
                // phoneType one of 
//                VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
//                types = new String[]{"VOICE", "FAX", "PAGER", "MSG", "CELL", 
//                        "VIDEO", "BBS", "MODEM", "ISDN", "PCS", "PREF"};
//                for (String t : types)
//                {
//                    System.out.println("getPhoneWork(" + t + ") - " + card.getPhoneWork(t));
//                }

                tmp = card.getEmailWork();
                if(tmp != null)
                    result.add(new ServerStoredDetails.EmailAddressDetail(tmp));
                
                tmp = card.getOrganization();
                if(tmp != null)
                    result.add(new ServerStoredDetails.WorkOrganizationNameDetail(tmp));
                
                tmp = card.getOrganizationUnit();
                if(tmp != null)
                    result.add(new WorkDepartmentNameDetail(tmp));
                
                byte[] imageBytes = card.getAvatar();
                if(imageBytes != null && imageBytes.length > 0)
                    result.add(new ServerStoredDetails.ImageDetail(
                        "Image", imageBytes));
            }
            catch (Exception exc)
            {
                logger.error("Cannot load details for contact "
                    + this + " : " + exc.getMessage()
                    , exc);
            }
        }

        retreivedDetails.put(contactAddress, result);
        
        return new LinkedList(result);
    }
    
    /**
     * Work department
     */
    public static class WorkDepartmentNameDetail
        extends ServerStoredDetails.NameDetail
    {
        public WorkDepartmentNameDetail(String workDepartmentName)
        {
            super("Work Department Name", workDepartmentName);
        }
    }
}