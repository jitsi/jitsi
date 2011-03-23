/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.googlecontacts;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements <tt>ContactQuery</tt> for Google Contacts.
 *
 * @author Sebastien Vincent
 */
public class GoogleContactsQuery
    extends AsyncContactQuery<GoogleContactsSourceService>
{
    /**
     * Maximum results for Google Contacts query.
     */
    public static final int GOOGLECONTACTS_MAX_RESULTS = 20;

    /**
     * Maximum number of results for this instance.
     */
    private final int count;

    /**
     * The Google query.
     */
    private GoogleQuery gQuery = null;

    /**
     * Initializes a new <tt>GoogleContactsQuery</tt> instance which is to
     * perform a specific <tt>query</tt> on behalf of a specific
     * <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is to
     * perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>Pattern</tt> for which <tt>contactSource</tt> is
     * being queried
     * @param count maximum number of results
     */
    protected GoogleContactsQuery(GoogleContactsSourceService
            contactSource, Pattern query, int count)
    {
        super(contactSource, query);
        this.count = count;
    }

    /**
     * Normalizes a <tt>String</tt> phone number by converting alpha characters
     * to their respective digits on a keypad and then stripping non-digit
     * characters.
     *
     * @param phoneNumber a <tt>String</tt> which represents a phone number to
     * normalize
     * @return a <tt>String</tt> which is a normalized form of the specified
     * <tt>phoneNumber</tt>
     */
    protected String normalizePhoneNumber(String phoneNumber)
    {
        PhoneNumberI18nService phoneNumberI18nService
            = GoogleContactsActivator.getPhoneNumberI18nService();

        return
            (phoneNumberI18nService == null)
                ? phoneNumber
                : phoneNumberI18nService.normalize(phoneNumber);
    }

    /**
     * Determines whether a specific <tt>String</tt> phone number matches the
     * {@link #query} of this <tt>AsyncContactQuery</tt>.
     *
     * @param phoneNumber the <tt>String</tt> which represents the phone number
     * to match to the <tt>query</tt> of this <tt>AsyncContactQuery</tt>
     * @return <tt>true</tt> if the specified <tt>phoneNumber</tt> matches the
     * <tt>query</tt> of this <tt>AsyncContactQuery</tt>; otherwise,
     * <tt>false</tt>
     */
    protected boolean phoneNumberMatches(String phoneNumber)
    {
        /*
         * PhoneNumberI18nService implements functionality to aid the parsing,
         * formatting and validation of international phone numbers so attempt to
         * use it to determine whether the specified phoneNumber matches the
         * query. For example, check whether the normalized phoneNumber matches
         * the query.
         */

        PhoneNumberI18nService phoneNumberI18nService
            = GoogleContactsActivator.getPhoneNumberI18nService();
        boolean phoneNumberMatches = false;

        if (phoneNumberI18nService != null)
        {
            if (query
                    .matcher(phoneNumberI18nService.normalize(phoneNumber))
                        .find())
            {
                phoneNumberMatches = true;
            }
            else
            {
                /*
                 * The fact that the normalized form of the phoneNumber doesn't
                 * match the query doesn't mean that, for example, it doesn't
                 * match the normalized form of the query. The latter, though,
                 * requires the query to look like a phone number as well. In
                 * order to not accidentally start matching all queries to phone
                 * numbers, it seems justified to normalize the query only when
                 * it is a phone number, not whenever it looks like a piece of a
                 * phone number.
                 */

                String phoneNumberQuery = getPhoneNumberQuery();

                if ((phoneNumberQuery != null)
                        && (phoneNumberQuery.length() != 0))
                {
                    try
                    {
                        phoneNumberMatches
                            = phoneNumberI18nService.phoneNumbersMatch(
                                    phoneNumberQuery,
                                    phoneNumber);
                    }
                    catch (IllegalArgumentException iaex)
                    {
                        /*
                         * Ignore it, phoneNumberMatches will remain equal to
                         * false.
                         */
                    }
                }
            }
        }
        return phoneNumberMatches;
    }


    /**
     * Create a <tt>SourceContact</tt> from a <tt>GoogleContactsEntry</tt>.
     *
     * @param entry <tt>GoogleContactsEntry</tt>
     */
    private void onGoogleContactsEntry(GoogleContactsEntry entry)
    {
        String displayName = entry.getFullName();
        if(displayName == null || displayName.length() == 0)
        {
            if((entry.getGivenName() == null ||
                    entry.getGivenName().length() == 0) &&
                    (entry.getFamilyName() == null ||
                            entry.getFamilyName().length() == 0))
            {
                return;
            }

            displayName = entry.getGivenName() + " " +
                entry.getFamilyName();
        }

        List<ContactDetail> contactDetails = getContactDetails(entry);

        if (!contactDetails.isEmpty())
        {
            GenericSourceContact sourceContact
                = new GenericSourceContact(
                        getContactSource(),
                        displayName,
                        contactDetails);

            try
            {
                byte img[] = GoogleContactsServiceImpl.downloadPhoto(
                        ((GoogleContactsEntryImpl)entry).getPhotoLink(),
                        getContactSource().getConnection().
                        getGoogleService());
                sourceContact.setImage(img);
            }
            catch (OutOfMemoryError oome)
            {
                // Ignore it, the image is not vital.
            }

            addQueryResult(sourceContact);
        }
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    @Override
    protected void run()
    {
        GoogleContactsServiceImpl service =
            GoogleContactsActivator.getGoogleContactsService();
        gQuery = new GoogleQuery(query);

        GoogleContactsConnection cnx = getContactSource().getConnection();

        if(cnx == null)
        {
            return;
        }

        service.searchContact(
                cnx,
                gQuery,
                count,
                new GoogleEntryCallback()
                {
                    public void callback(GoogleContactsEntry entry)
                    {
                        onGoogleContactsEntry(entry);
                    }
                });
    }

    /**
     * Gets the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>.
     *
     * @param entry <tt>GoogleContactsEntry</tt>
     * @return the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     */
    private List<ContactDetail> getContactDetails(GoogleContactsEntry entry)
    {
        List<ContactDetail> ret = new LinkedList<ContactDetail>();
        List<String> homeMails = entry.getHomeMails();
        List<String> workMails = entry.getWorkMails();
        List<String> mobilePhones = entry.getMobilePhones();
        List<String> homePhones = entry.getHomePhones();
        List<String> workPhones = entry.getWorkPhones();
        Map<String, GoogleContactsEntry.IMProtocol> ims =
            entry.getIMAddresses();
        ContactDetail detail = null;

        for(String mail : homeMails)
        {
            detail = new ContactDetail(mail, ContactDetail.CATEGORY_EMAIL,
                    new String[]{ContactDetail.LABEL_HOME});
            ret.add(detail);
        }
        for(String mail : workMails)
        {
            detail = new ContactDetail(mail, ContactDetail.CATEGORY_EMAIL,
                    new String[]{ContactDetail.LABEL_WORK});
            ret.add(detail);
        }

        for(String homePhone : homePhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(1);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            homePhone = normalizePhoneNumber(homePhone);
            detail = new ContactDetail(homePhone,
                    ContactDetail.CATEGORY_PHONE,
                    new String[]{ContactDetail.LABEL_HOME});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(String workPhone : workPhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(1);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            workPhone = normalizePhoneNumber(workPhone);
            detail = new ContactDetail(workPhone,
                    ContactDetail.CATEGORY_PHONE,
                    new String[]{ContactDetail.LABEL_WORK});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(String mobilePhone : mobilePhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(1);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            mobilePhone = normalizePhoneNumber(mobilePhone);
            detail = new ContactDetail(mobilePhone,
                    ContactDetail.CATEGORY_PHONE,
                    new String[]{ContactDetail.LABEL_MOBILE});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(Map.Entry<String, GoogleContactsEntry.IMProtocol> im :
            ims.entrySet())
        {
            if(im.getValue() != GoogleContactsEntry.IMProtocol.OTHER)
            {
                detail = new ContactDetail(im.getKey(),
                        ContactDetail.CATEGORY_INSTANT_MESSAGING,
                        new String[]{im.getValue().toString()});

                setIMCapabilities(detail, im.getValue());
                ret.add(detail);
            }
        }

        return ret;
    }

    /**
     * Sets the IM capabilities of a specific <tt>ContactDetail</tt> (e.g.
     * <tt>supportedOpSets</tt>).
     *
     * @param contactDetail the <tt>ContactDetail</tt> to set the capabilities
     * of
     * @param protocol protocol
     * @return <tt>contactDetail</tt>
     */
    private ContactDetail setIMCapabilities(
            ContactDetail contactDetail,
            GoogleContactsEntry.IMProtocol protocol)
    {
        List<Class<? extends OperationSet>> supportedOpSets
            = new LinkedList<Class<? extends OperationSet>>();
        Map<Class<? extends OperationSet>, String> preferredProtocols
            = new HashMap<Class<? extends OperationSet>, String>();

        switch (protocol)
        {
        case GOOGLETALK:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.AIM);
            break;
        case ICQ:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.ICQ);
            break;
        case JABBER:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.JABBER);
            supportedOpSets.add(OperationSetBasicTelephony.class);
            preferredProtocols.put(
                    OperationSetBasicTelephony.class,
                    ProtocolNames.JABBER);
            break;
        case MSN:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.MSN);
            break;
        case YAHOO:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.YAHOO);
            break;
        default:
            break;
        }
        contactDetail.setSupportedOpSets(supportedOpSets);

        if (!preferredProtocols.isEmpty())
            contactDetail.setPreferredProtocols(preferredProtocols);

        return contactDetail;
    }

    /**
     * Notifies this <tt>GoogleContactsQuery</tt> that it has stopped performing
     * in the associated background <tt>Thread</tt>.
     *
     * @param completed <tt>true</tt> if this <tt>ContactQuery</tt> has
     * successfully completed, <tt>false</tt> if an error has been encountered
     * during its execution
     * @see AsyncContactQuery#stopped(boolean)
     */
    @Override
    protected void stopped(boolean completed)
    {
        try
        {
            super.stopped(completed);
        }
        finally
        {
            getContactSource().stopped(this);
        }
    }

    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    @Override
    public void cancel()
    {
        if(gQuery != null)
        {
            gQuery.cancel();
        }
        super.cancel();
    }
}
