/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.ServerStoredDetails;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.packet.VCard;

/**
 * Handles and retrieves all info of our contacts or our account info
 *
 * @author Damian Minkov
 */
public class InfoRetreiver
{
    private static final Logger logger =
        Logger.getLogger(InfoRetreiver.class);

    /**
     * A callback to the Jabber provider that created us.
     */
    private ProtocolProviderServiceJabberImpl jabberProvider = null;

    // here is kept all the details retrieved so far
    private final Map<String, List<GenericDetail>> retreivedDetails
        = new Hashtable<String, List<GenericDetail>>();

    private static final String TAG_FN_OPEN = "<FN>";
    private static final String TAG_FN_CLOSE = "</FN>";

    // the uin of the account using us,
    // used when sending commands for user info to the server
    private final String ownerUin;

    protected InfoRetreiver
        (ProtocolProviderServiceJabberImpl jabberProvider, String ownerUin)
    {
        this.jabberProvider = jabberProvider;
        this.ownerUin = ownerUin;
    }

    /**
     * returns the user details from the specified class or its descendants
     * the class is one from the
     * net.java.sip.communicator.service.protocol.ServerStoredDetails
     * or implemented one in the operation set for the user info
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    Iterator<GenericDetail> getDetailsAndDescendants(
        String uin,
        Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = getContactDetails(uin);
        List<GenericDetail> result = new LinkedList<GenericDetail>();

        for (GenericDetail item : details)
            if(detailClass.isInstance(item))
                result.add(item);

        return result.iterator();
    }

    /**
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    Iterator<GenericDetail> getDetails(
        String uin,
        Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = getContactDetails(uin);
        List<GenericDetail> result = new LinkedList<GenericDetail>();

        for (GenericDetail item : details)
            if(detailClass.equals(item.getClass()))
                result.add(item);

        return result.iterator();
    }

    /**
     * request the full info for the given contactAddress
     * waits and return this details
     *
     * @param contactAddress String
     * @return Vector the details
     */
    List<GenericDetail> getContactDetails(String contactAddress)
    {
        List<GenericDetail> result = retreivedDetails.get(contactAddress);

        if(result == null)
        {
            result = new LinkedList<GenericDetail>();
            try
            {
                XMPPConnection connection = jabberProvider.getConnection();

                if(connection == null || !connection.isAuthenticated())
                    return null;

                VCard card = new VCard();
                card.load(connection, contactAddress);

                String tmp = null;

                tmp = checkForFullName(card);
                if(tmp != null)
                    result.add(new ServerStoredDetails.DisplayNameDetail(tmp));

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

                // Home Details
                // addrField one of
                // POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
                // LOCALITY, REGION, PCODE, CTRY
                tmp = card.getAddressFieldHome("STREET");
                if(tmp != null)
                    result.add(new ServerStoredDetails.AddressDetail(tmp));

                tmp = card.getAddressFieldHome("LOCALITY");
                if(tmp != null)
                    result.add(new ServerStoredDetails.CityDetail(tmp));

                tmp = card.getAddressFieldHome("REGION");
                if(tmp != null)
                    result.add(new ServerStoredDetails.ProvinceDetail(tmp));

                tmp = card.getAddressFieldHome("PCODE");
                if(tmp != null)
                    result.add(new ServerStoredDetails.PostalCodeDetail(tmp));

//                tmp = card.getAddressFieldHome("CTRY");
//                if(tmp != null)
//                    result.add(new ServerStoredDetails.CountryDetail(tmp);

                // phoneType one of
                //VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF

                tmp = card.getPhoneHome("VOICE");
                if(tmp != null)
                    result.add(new ServerStoredDetails.PhoneNumberDetail(tmp));

                tmp = card.getPhoneHome("FAX");
                if(tmp != null)
                    result.add(new ServerStoredDetails.FaxDetail(tmp));

                tmp = card.getPhoneHome("PAGER");
                if(tmp != null)
                    result.add(new ServerStoredDetails.PagerDetail(tmp));

                tmp = card.getPhoneHome("CELL");
                if(tmp != null)
                    result.add(new ServerStoredDetails.MobilePhoneDetail(tmp));

                tmp = card.getEmailHome();
                if(tmp != null)
                    result.add(new ServerStoredDetails.EmailAddressDetail(tmp));

                // Work Details
                // addrField one of
                // POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
                // LOCALITY, REGION, PCODE, CTRY
                tmp = card.getAddressFieldWork("STREET");
                if(tmp != null)
                    result.add(new ServerStoredDetails.WorkAddressDetail(tmp));

                tmp = card.getAddressFieldWork("LOCALITY");
                if(tmp != null)
                    result.add(new ServerStoredDetails.WorkCityDetail(tmp));

                tmp = card.getAddressFieldWork("REGION");
                if(tmp != null)
                    result.add(new ServerStoredDetails.WorkProvinceDetail(tmp));

                tmp = card.getAddressFieldWork("PCODE");
                if(tmp != null)
                    result.add(new ServerStoredDetails.WorkPostalCodeDetail(tmp));

//                tmp = card.getAddressFieldWork("CTRY");
//                if(tmp != null)
//                    result.add(new ServerStoredDetails.WorkCountryDetail(tmp);

                // phoneType one of
                //VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF

                tmp = card.getPhoneWork("VOICE");
                if(tmp != null)
                    result.add(new ServerStoredDetails.WorkPhoneDetail(tmp));

                tmp = card.getPhoneWork("FAX");
                if(tmp != null)
                    result.add(new WorkFaxDetail(tmp));

                tmp = card.getPhoneWork("PAGER");
                if(tmp != null)
                    result.add(new WorkPagerDetail(tmp));

                tmp = card.getPhoneWork("CELL");
                if(tmp != null)
                    result.add(new ServerStoredDetails.WorkMobilePhoneDetail(tmp));


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

        return new LinkedList<GenericDetail>(result);
    }

    private String checkForFullName(VCard card)
    {
        String vcardXml = card.toXML();

        int indexOpen = vcardXml.indexOf(TAG_FN_OPEN);

        if(indexOpen == -1)
            return null;

        int indexClose = vcardXml.indexOf(TAG_FN_CLOSE, indexOpen);

        // something is wrong!
        if(indexClose == -1)
            return null;

        return vcardXml.substring(indexOpen + TAG_FN_OPEN.length(), indexClose);
    }

    /**
     * Work department
     */
    public static class WorkDepartmentNameDetail
        extends ServerStoredDetails.NameDetail
    {
        /**
         * Constructor.
         *
         * @param workDepartmentName name of the work department
         */
        public WorkDepartmentNameDetail(String workDepartmentName)
        {
            super("Work Department Name", workDepartmentName);
        }
    }

    /**
     * Fax at work
     */
    public static class WorkFaxDetail
        extends ServerStoredDetails.FaxDetail
    {
        /**
         * Constructor.
         *
         * @param number work fax number
         */
        public WorkFaxDetail(String number)
        {
            super(number);
            super.detailDisplayName = "WorkFax";
        }
    }

    /**
     * Pager at work
     */
    public static class WorkPagerDetail
        extends ServerStoredDetails.PhoneNumberDetail
    {
        /**
         * Constructor.
         *
         * @param number work pager number
         */
        public WorkPagerDetail(String number)
        {
            super(number);
            super.detailDisplayName = "WorkPager";
        }
    }
}
