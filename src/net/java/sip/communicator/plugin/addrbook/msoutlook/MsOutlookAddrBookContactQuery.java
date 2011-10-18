/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements <tt>ContactQuery</tt> for the Address Book of Microsoft Outlook.
 *
 * @author Lyubomir Marinov
 */
public class MsOutlookAddrBookContactQuery
    extends AsyncContactQuery<MsOutlookAddrBookContactSourceService>
{
    private static final int dispidEmail1EmailAddress = 11;

    private static final int dispidEmail2EmailAddress = 12;

    private static final int dispidEmail3EmailAddress = 13;

    /**
     * The object type of a <tt>SourceContact</tt> in the Address Book of
     * Microsoft Outlook.
     */
    private static final long MAPI_MAILUSER = 0x00000006;

    /**
     * The IDs of the properties of <tt>MAPI_MAILUSER</tt> which are to be
     * queried by the <tt>MsOutlookAddrBookContactQuery</tt> instances.
     */
    private static final long[] MAPI_MAILUSER_PROP_IDS
        = new long[]
        {
            0x3001 /* PR_DISPLAY_NAME */,
            0x3003 /* PR_EMAIL_ADDRESS */,
            0x3A06 /* PR_GIVEN_NAME */,
            0x3A44 /* PR_MIDDLE_NAME */,
            0x3A11 /* PR_SURNAME */,
            0x3A08 /* PR_BUSINESS_TELEPHONE_NUMBER */,
            0x3A1B /* PR_BUSINESS2_TELEPHONE_NUMBER */,
            0x3A09 /* PR_HOME_TELEPHONE_NUMBER */,
            0x3A2F /* PR_HOME2_TELEPHONE_NUMBER */,
            0x3A1C /* PR_MOBILE_TELEPHONE_NUMBER */,
            0x0FFE /* PR_OBJECT_TYPE */,
            0x00008083 /* dispidEmail1EmailAddress */,
            0x00008093 /* dispidEmail2EmailAddress */,
            0x000080A3 /* dispidEmail3EmailAddress */
        };

    /**
     * The object type of a <tt>SourceContact</tt> in a Contacts folder of
     * Microsoft Outlook.
     */
    private static final long MAPI_MESSAGE = 0x00000005;

    /**
     * The flag which signals that MAPI strings should be returned in the
     * Unicode character set.
     */
    private static final long MAPI_UNICODE = 0x80000000;

    /**
     * The id of the <tt>PR_ATTACHMENT_CONTACTPHOTO</tt> MAPI property.
     */
    private static final long PR_ATTACHMENT_CONTACTPHOTO = 0x7FFF;

    /**
     * The index of the id of the <tt>PR_BUSINESS_TELEPHONE_NUMBER</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_BUSINESS_TELEPHONE_NUMBER = 5;

    /**
     * The index of the id of the <tt>PR_BUSINESS2_TELEPHONE_NUMBER</tt>
     * property in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_BUSINESS2_TELEPHONE_NUMBER = 6;

    /**
     * The index of the id of the <tt>PR_DISPLAY_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_DISPLAY_NAME = 0;

    /**
     * The index of the id of the <tt>PR_EMAIL_ADDRESS</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_EMAIL_ADDRESS = 1;

    /**
     * The index of the id of the <tt>PR_GIVEN_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_GIVEN_NAME = 2;

    /**
     * The index of the id of the <tt>PR_HOME_TELEPHONE_NUMBER</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_HOME_TELEPHONE_NUMBER = 7;

    /**
     * The index of the id of the <tt>PR_HOME2_TELEPHONE_NUMBER</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_HOME2_TELEPHONE_NUMBER = 8;

    /**
     * The index of the id of the <tt>PR_MIDDLE_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_MIDDLE_NAME = 3;

    /**
     * The index of the id of the <tt>PR_MOBILE_TELEPHONE_NUMBER</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_MOBILE_TELEPHONE_NUMBER = 9;

    /**
     * The index of the id of the <tt>PR_OBJECT_TYPE</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_OBJECT_TYPE = 10;

    /**
     * The index of the id of the <tt>PR_SURNAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_SURNAME = 4;

    /**
     * The indexes in {@link #MAPI_MAILUSER_PROP_IDS} of the property IDs which
     * are to be represented in <tt>SourceContact</tt> as
     * <tt>ContactDetail</tt>s.
     */
    private static final int[] CONTACT_DETAIL_PROP_INDEXES
        = new int[]
        {
            PR_EMAIL_ADDRESS,
            PR_BUSINESS_TELEPHONE_NUMBER,
            PR_BUSINESS2_TELEPHONE_NUMBER,
            PR_HOME_TELEPHONE_NUMBER,
            PR_HOME2_TELEPHONE_NUMBER,
            PR_MOBILE_TELEPHONE_NUMBER,
            dispidEmail1EmailAddress,
            dispidEmail2EmailAddress,
            dispidEmail3EmailAddress
        };

    static
    {
        System.loadLibrary("jmsoutlookaddrbook");
    }

    /**
     * The number of <tt>SourceContact</tt>s matching this <tt>ContactQuery</tt>
     * which have been retrieved from Contacts folders. Since each one of them
     * may appear multiple times in the Address Book as well, no matching in the
     * Address Book will be performed if there is at least one matching
     * <tt>SourceContact</tt> in a Contacts folder.
     */
    private int mapiMessageCount;

    /**
     * Initializes a new <tt>MsOutlookAddrBookContactQuery</tt> instance to
     * be performed by a specific
     * <tt>MsOutlookAddrBookContactSourceService</tt>.
     *
     * @param msoabcss the <tt>MsOutlookAddrBookContactSourceService</tt>
     * which is to perform the new <tt>ContactQuery</tt>
     * @param query the <tt>Pattern</tt> for which <tt>msoabcss</tt> is being
     * queried
     */
    public MsOutlookAddrBookContactQuery(
            MsOutlookAddrBookContactSourceService msoabcss,
            Pattern query)
    {
        super(msoabcss, query);
    }

    /**
     * Calls back to a specific <tt>PtrCallback</tt> for each
     * <tt>MAPI_MAILUSER</tt> found in the Address Book of Microsoft Outlook
     * which matches a specific <tt>String</tt> query.
     *
     * @param query the <tt>String</tt> for which the Address Book of Microsoft
     * Outlook is to be queried. <b>Warning</b>: Ignored at the time of this
     * writing.
     * @param callback the <tt>PtrCallback</tt> to be notified about the
     * matching <tt>MAPI_MAILUSER</tt>s
     */
    private static native void foreachMailUser(
            String query,
            PtrCallback callback);

    /**
     * Gets the set of <tt>ContactDetail</tt> labels to be assigned to a
     * property specified by its index in {@link #MAPI_MAILUSER_PROP_IDS}.
     *
     * @param propIndex the index in <tt>MAPI_MAILUSER_PROP_IDS</tt> of the
     * property to get the <tt>ContactDetail</tt> labels of
     * @return the set of <tt>ContactDetail</tt> labels to be assigned to the
     * property specified by its index in <tt>MAPI_MAILUSER_PROP_IDS</tt>
     */
    private String[] getLabels(int propIndex)
    {
        switch (propIndex)
        {
        case dispidEmail1EmailAddress:
        case dispidEmail2EmailAddress:
        case dispidEmail3EmailAddress:
        case PR_EMAIL_ADDRESS:
            return new String[] { ContactDetail.CATEGORY_EMAIL };
        case PR_BUSINESS2_TELEPHONE_NUMBER:
        case PR_BUSINESS_TELEPHONE_NUMBER:
            return
                new String[]
                        {
                            ContactDetail.CATEGORY_PHONE,
                            ContactDetail.LABEL_WORK
                        };
        case PR_HOME2_TELEPHONE_NUMBER:
        case PR_HOME_TELEPHONE_NUMBER:
            return
                new String[]
                        {
                            ContactDetail.CATEGORY_PHONE,
                            ContactDetail.LABEL_HOME
                        };
        case PR_MOBILE_TELEPHONE_NUMBER:
            return
                new String[]
                        {
                            ContactDetail.CATEGORY_PHONE,
                            ContactDetail.LABEL_MOBILE
                        };
        default:
            return null;
        }
    }

    private static native Object[] IMAPIProp_GetProps(
            long mapiProp,
            long[] propIds, long flags)
        throws MsOutlookMAPIHResultException;

    /**
     * Determines whether a specific index in {@link #MAPI_MAILUSER_PROP_IDS}
     * stands for a property with a phone number value.
     *
     * @param propIndex the index in <tt>MAPI_MAILUSER_PROP_IDS</tt> of the
     * property to check
     * @return <tt>true</tt> if <tt>propIndex</tt> stands for a property with a
     * phone number value; otherwise, <tt>false</tt>
     */
    private boolean isPhoneNumber(int propIndex)
    {
        switch (propIndex)
        {
        case PR_BUSINESS2_TELEPHONE_NUMBER:
        case PR_BUSINESS_TELEPHONE_NUMBER:
        case PR_HOME2_TELEPHONE_NUMBER:
        case PR_HOME_TELEPHONE_NUMBER:
        case PR_MOBILE_TELEPHONE_NUMBER:
            return true;
        default:
            return false;
        }
    }

    /**
     * Determines whether a specific <tt>MAPI_MAILUSER</tt> property with a
     * specific <tt>value</tt> matches the {@link #query} of this
     * <tt>AsyncContactQuery</tt>.
     *
     * @param property the <tt>MAPI_MAILUSER</tt> property to check
     * @param value the value of the <tt>property</tt> to check
     * @return <tt>true</tt> if the specified <tt>value</tt> of the specified
     * <tt>property</tt> matches the <tt>query</tt> of this
     * <tt>AsyncContactQuery</tt>; otherwise, <tt>false</tt>
     */
    private boolean matches(int property, String value)
    {
        return
            query.matcher(value).find()
                || (isPhoneNumber(property) && phoneNumberMatches(value));
    }

    /**
     * Notifies this <tt>MsOutlookAddrBookContactQuery</tt> about a specific
     * <tt>MAPI_MAILUSER</tt>.
     *
     * @param iUnknown a pointer to an <tt>IUnknown</tt> instance for the
     * <tt>MAPI_MAILUSER</tt> to notify about
     * @return <tt>true</tt> if this <tt>MsOutlookAddrBookContactQuery</tt>
     * is to continue being called; otherwise, <tt>false</tt>
     * @throws MsOutlookMAPIHResultException if anything goes wrong while
     * getting the properties of the specified <tt>MAPI_MAILUSER</tt>
     */
    private boolean onMailUser(long iUnknown)
        throws MsOutlookMAPIHResultException
    {
        Object[] props
            = IMAPIProp_GetProps(
                    iUnknown,
                    MAPI_MAILUSER_PROP_IDS,
                    MAPI_UNICODE);
        long objType
            = (props[PR_OBJECT_TYPE] instanceof Long)
                ? ((Long) props[PR_OBJECT_TYPE]).longValue()
                : 0;

        /*
         * If we have results from the Contacts folder(s), don't read from the
         * Address Book because there may be duplicates.
         */
        if ((MAPI_MAILUSER == objType) && (mapiMessageCount != 0))
            return false;

        int propIndex = 0;
        boolean matches = false;

        for (Object prop : props)
        {
            if ((prop instanceof String) && matches(propIndex, (String) prop))
            {
                matches = true;
                break;
            }
            propIndex++;
        }
        if (matches)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(1);

            supportedOpSets.add(OperationSetBasicTelephony.class);

            List<ContactDetail> contactDetails
                = new LinkedList<ContactDetail>();

            for (int i = 0; i < CONTACT_DETAIL_PROP_INDEXES.length; i++)
            {
                propIndex = CONTACT_DETAIL_PROP_INDEXES[i];

                Object prop = props[propIndex];

                if (prop instanceof String)
                {
                    String stringProp = (String) prop;

                    if (stringProp.length() != 0)
                    {
                        if (isPhoneNumber(propIndex))
                            stringProp = normalizePhoneNumber(stringProp);

                        ContactDetail contactDetail
                            = new ContactDetail(
                                    stringProp,
                                    getLabels(propIndex));

                        contactDetail.setSupportedOpSets(supportedOpSets);
                        contactDetails.add(contactDetail);
                    }
                }
            }

            GenericSourceContact sourceContact
                = new GenericSourceContact(
                        getContactSource(),
                        (String) props[PR_DISPLAY_NAME],
                        contactDetails);

            if (MAPI_MESSAGE == objType)
            {
                ++mapiMessageCount;

                try
                {
                    Object[] images
                        = IMAPIProp_GetProps(
                                iUnknown,
                                new long[] { PR_ATTACHMENT_CONTACTPHOTO },
                                0);
                    Object image = images[0];

                    if (image instanceof byte[])
                        sourceContact.setImage((byte[]) image);
                }
                catch (MsOutlookMAPIHResultException ex)
                {
                    // Ignore it, the image isn't as vital as the SourceContact.
                }
            }

            addQueryResult(sourceContact);
        }
        return (getStatus() == QUERY_IN_PROGRESS);
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    protected void run()
    {
        foreachMailUser(
            query.toString(),
            new PtrCallback()
            {
                public boolean callback(long iUnknown)
                {
                    try
                    {
                        return onMailUser(iUnknown);
                    }
                    catch (MsOutlookMAPIHResultException ex)
                    {
                        ex.printStackTrace(System.err);
                        return false;
                    }
                }
            });
    }

    /**
     * Notifies this <tt>AsyncContactQuery</tt> that it has stopped performing
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
            = AddrBookActivator.getPhoneNumberI18nService();

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
            = AddrBookActivator.getPhoneNumberI18nService();
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
}
