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
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>ContactQuery</tt> for the Address Book of Microsoft Outlook.
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */
public class MsOutlookAddrBookContactQuery
    extends AbstractAddrBookContactQuery<MsOutlookAddrBookContactSourceService>
{
    /**
     * The <tt>Logger</tt> used by the <tt>MsOutlookAddrBookContactQuery</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MsOutlookAddrBookContactQuery.class);

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
            0x000080A3 /* dispidEmail3EmailAddress */,
            0x3A16 /* PR_COMPANY_NAME */,
            0x0FFF /* PR_ORIGINAL_ENTRYID */,
            0x3A24 /* dispidFax1EmailAddress */,
            0x3A25 /* dispidFax2EmailAddress */,
            0x3A23 /* dispidFax3EmailAddress */,
            0x3A4F /* PR_NICKNAME */,
            0x3A45 /* PR_DISPLAY_NAME_PREFIX */,
            0x3A50 /* PR_PERSONAL_HOME_PAGE */,
            0x3A51 /* PR_BUSINESS_HOME_PAGE */
            //0x00008062 /* dispidInstMsg */, // EDITION NOT WORKING
            //0x0000801A /* dispidHomeAddress */, // EDITION NOT WORKING
            //0x0000801B /* dispidWorkAddress */, // EDITION NOT WORKING
            //0x0000801C /* dispidOtherAddress */ // EDITION ONT WORKING
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

    private static final int PR_COMPANY_NAME = 14;

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
     * The index of the id of the <tt>PR_ORIGINAL_ENTRYID</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_ORIGINAL_ENTRYID = 15;

    /**
     * The index of the 1st fax telephone number (business fax).
     */
    private static final int dispidFax1EmailAddress = 16;

    /**
     * The index of the 2nd fax telephone number (home fax).
     */
    private static final int dispidFax2EmailAddress = 17;

    /**
     * The index of the 3rd fax telephone number (other fax).
     */
    private static final int dispidFax3EmailAddress = 18;

    /**
     * The index of the nickname.
     */
    private static final int PR_NICKNAME = 19;

    /**
     * The index of the name prefix.
     */
    private static final int PR_DISPLAY_NAME_PREFIX = 20;

    /**
     * The index of the personnal home page
     */
    private static final int PR_PERSONAL_HOME_PAGE = 21;

    /**
     * The index of the business home page
     */
    private static final int PR_BUSINESS_HOME_PAGE = 22;
    
    /**
     * The index of the instant messaging address.
     */
    //private static final int dispidInstMsg = 23;

    /**
     * The index of the home address
     */
    //private static final int dispidHomeAddress = 24;

    /**
     * The index of the work address
     */
    //private static final int dispidWorkAddress = 25;

    /**
     * The index of the other address
     */
    //private static final int dispidOtherAddress = 26;

    /**
     * The indexes in {@link #MAPI_MAILUSER_PROP_IDS} of the property IDs which
     * are to be represented in <tt>SourceContact</tt> as
     * <tt>ContactDetail</tt>s.
     */
    private static final int[] CONTACT_DETAIL_PROP_INDEXES
        = new int[]
        {
            PR_EMAIL_ADDRESS,
            PR_GIVEN_NAME,
            PR_MIDDLE_NAME,
            PR_SURNAME,
            PR_BUSINESS_TELEPHONE_NUMBER,
            PR_BUSINESS2_TELEPHONE_NUMBER,
            PR_HOME_TELEPHONE_NUMBER,
            PR_HOME2_TELEPHONE_NUMBER,
            PR_MOBILE_TELEPHONE_NUMBER,
            dispidEmail1EmailAddress,
            dispidEmail2EmailAddress,
            dispidEmail3EmailAddress,
            PR_COMPANY_NAME,
            dispidFax1EmailAddress,
            dispidFax2EmailAddress,
            dispidFax3EmailAddress,
            PR_NICKNAME,
            PR_DISPLAY_NAME_PREFIX,
            PR_PERSONAL_HOME_PAGE,
            PR_BUSINESS_HOME_PAGE
            //dispidInstMsg,
            //dispidHomeAddress,
            //dispidWorkAddress,
            //dispidOtherAddress
        };

    /**
     * The indexes in {@link #MAPI_MAILUSER_PROP_IDS} of the property IDs which
     * represent an identifier which can be used for telephony or persistent
     * presence.
     */
    private static final int[] CONTACT_OPERATION_SET_ABLE_PROP_INDEXES
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
            dispidEmail3EmailAddress,
            dispidFax1EmailAddress,
            dispidFax2EmailAddress,
            dispidFax3EmailAddress
            //dispidInstMsg
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
    public static native void foreachMailUser(
            String query,
            PtrCallback callback);

    private ContactDetail.Category getCategory(int propIndex)
    {
        switch (propIndex)
        {
        case PR_GIVEN_NAME:
        case PR_MIDDLE_NAME:
        case PR_SURNAME:
        case PR_NICKNAME:
        case PR_DISPLAY_NAME_PREFIX:
        case PR_PERSONAL_HOME_PAGE:
            return ContactDetail.Category.Personal;
        case PR_COMPANY_NAME:
        case PR_BUSINESS_HOME_PAGE:
            return ContactDetail.Category.Organization;
        case dispidEmail1EmailAddress:
        case dispidEmail2EmailAddress:
        case dispidEmail3EmailAddress:
        case PR_EMAIL_ADDRESS:
            return ContactDetail.Category.Email;
        case PR_BUSINESS2_TELEPHONE_NUMBER:
        case PR_BUSINESS_TELEPHONE_NUMBER:
        case PR_HOME2_TELEPHONE_NUMBER:
        case PR_HOME_TELEPHONE_NUMBER:
        case PR_MOBILE_TELEPHONE_NUMBER:
        case dispidFax1EmailAddress:
        case dispidFax2EmailAddress:
        case dispidFax3EmailAddress:
            return ContactDetail.Category.Phone;
        //case dispidInstMsg:
        //    return ContactDetail.Category.InstantMessaging;
        //case dispidHomeAddress:
        //case dispidWorkAddress:
        //case dispidOtherAddress:
        //    return ContactDetail.Category.Address;
        default:
            return null;
        }
    }

    /**
     * Gets the set of <tt>ContactDetail</tt> labels to be assigned to a
     * property specified by its index in {@link #MAPI_MAILUSER_PROP_IDS}.
     *
     * @param propIndex the index in <tt>MAPI_MAILUSER_PROP_IDS</tt> of the
     * property to get the <tt>ContactDetail</tt> labels of
     * @return the set of <tt>ContactDetail</tt> labels to be assigned to the
     * property specified by its index in <tt>MAPI_MAILUSER_PROP_IDS</tt>
     */
    private ContactDetail.SubCategory[] getSubCategories(int propIndex)
    {
        switch (propIndex)
        {
        case PR_GIVEN_NAME:
        case PR_MIDDLE_NAME:
        case PR_COMPANY_NAME:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Name
                        };
        case PR_SURNAME:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.LastName
                        };
        case PR_NICKNAME:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Nickname
                        };
        case PR_BUSINESS2_TELEPHONE_NUMBER:
        case PR_BUSINESS_TELEPHONE_NUMBER:
        case dispidEmail1EmailAddress:
        case PR_EMAIL_ADDRESS:
        //case dispidWorkAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Work
                        };
        case PR_HOME2_TELEPHONE_NUMBER:
        case PR_HOME_TELEPHONE_NUMBER:
        case dispidEmail2EmailAddress:
        //case dispidHomeAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Home
                        };
        case PR_MOBILE_TELEPHONE_NUMBER:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Mobile
                        };
        case dispidFax1EmailAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Fax,
                            ContactDetail.SubCategory.Work
                        };
        case dispidFax2EmailAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Fax,
                            ContactDetail.SubCategory.Home
                        };
        case dispidFax3EmailAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Fax,
                            ContactDetail.SubCategory.Other
                        };
        //case dispidOtherAddress:
        //    return
        //        new ContactDetail.SubCategory[]
        //                {
        //                    ContactDetail.SubCategory.Other
        //                };
        case dispidEmail3EmailAddress:
            return
                new ContactDetail.SubCategory[]
                        {
                            ContactDetail.SubCategory.Other
                        };
        default:
            return null;
        }
    }

    /**
     * Find the outlook property tag from category and subcategories.
     *
     * @param category The category.
     * @param subCategories The subcategories.
     *
     * @return The outlook property tag corresponding to the given category and
     * subcategories.
     */
    public static long getProperty(
        ContactDetail.Category category,
        Collection<ContactDetail.SubCategory> subCategories)
    {
        switch(category)
        {
        case Personal:
            if(subCategories.contains(ContactDetail.SubCategory.Name))
                return MAPI_MAILUSER_PROP_IDS[PR_GIVEN_NAME];
                // PR_MIDDLE_NAME:
            else if(subCategories.contains(
                        ContactDetail.SubCategory.LastName))
                return MAPI_MAILUSER_PROP_IDS[PR_SURNAME];
            else if(subCategories.contains(
                        ContactDetail.SubCategory.Nickname))
                return MAPI_MAILUSER_PROP_IDS[PR_NICKNAME];
            else if(subCategories.contains(
                        ContactDetail.SubCategory.HomePage))
                return MAPI_MAILUSER_PROP_IDS[PR_PERSONAL_HOME_PAGE];
            else
                return MAPI_MAILUSER_PROP_IDS[PR_DISPLAY_NAME_PREFIX];
        case Organization:
            if(subCategories.contains(ContactDetail.SubCategory.Name))
                return MAPI_MAILUSER_PROP_IDS[PR_COMPANY_NAME];
            else
                return MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_HOME_PAGE];
        case Email:
            if(subCategories.contains(ContactDetail.SubCategory.Work))
                return MAPI_MAILUSER_PROP_IDS[dispidEmail1EmailAddress];
                // PR_EMAIL_ADDRESS:
            else if(subCategories.contains(
                        ContactDetail.SubCategory.Home))
                return MAPI_MAILUSER_PROP_IDS[dispidEmail2EmailAddress];
            else if(subCategories.contains(
                        ContactDetail.SubCategory.Other))
                return MAPI_MAILUSER_PROP_IDS[dispidEmail3EmailAddress];
            break;
        case Phone:
            if(subCategories.contains(ContactDetail.SubCategory.Fax))
            {
                if(subCategories.contains(ContactDetail.SubCategory.Work))
                    return MAPI_MAILUSER_PROP_IDS[dispidFax1EmailAddress];
                else if(subCategories.contains(
                            ContactDetail.SubCategory.Home))
                    return MAPI_MAILUSER_PROP_IDS[dispidFax2EmailAddress];
                else if(subCategories.contains(
                            ContactDetail.SubCategory.Other))
                    return MAPI_MAILUSER_PROP_IDS[dispidFax3EmailAddress];
            }
            else if(subCategories.contains(ContactDetail.SubCategory.Work))
                return MAPI_MAILUSER_PROP_IDS[PR_BUSINESS_TELEPHONE_NUMBER];
                // PR_BUSINESS2_TELEPHONE_NUMBER:
            else if(subCategories.contains(ContactDetail.SubCategory.Home))
                return MAPI_MAILUSER_PROP_IDS[PR_HOME_TELEPHONE_NUMBER];
                // PR_HOME2_TELEPHONE_NUMBER:
            else if(subCategories.contains(
                        ContactDetail.SubCategory.Mobile))
                return MAPI_MAILUSER_PROP_IDS[PR_MOBILE_TELEPHONE_NUMBER];
            break;
//        case InstantMessaging:
//            return MAPI_MAILUSER_PROP_IDS[dispidInstMsg];
//        case Address:
//            if(subCategories.contains(ContactDetail.SubCategory.Work))
//                return MAPI_MAILUSER_PROP_IDS[dispidWorkAddress];
//            else if(subCategories.contains(
//                        ContactDetail.SubCategory.Home))
//                return MAPI_MAILUSER_PROP_IDS[dispidHomeAddress];
//            else if(subCategories.contains(
//                        ContactDetail.SubCategory.Other))
//                return MAPI_MAILUSER_PROP_IDS[dispidOtherAddress];
//            break;
        default:
            // Silence the compiler.
            break;
        }
        return -1;
    }

    private static native Object[] IMAPIProp_GetProps(
            long mapiProp,
            long[] propIds, long flags)
        throws MsOutlookMAPIHResultException;

    public static native boolean IMAPIProp_SetPropString(
            long propId,
            String value,
            String entryId);

    public static native boolean IMAPIProp_DeleteProp(
            long propId,
            String entryId);

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
            List<ContactDetail> contactDetails = getContactDetails(props);

            /*
             * What's the point of showing a contact who has no contact details?
             */
            if (!contactDetails.isEmpty())
            {
                String displayName = (String) props[PR_DISPLAY_NAME];

                if ((displayName == null) || (displayName.length() == 0))
                    displayName = (String) props[PR_COMPANY_NAME];

                MsOutlookAddrBookSourceContact sourceContact
                    = new MsOutlookAddrBookSourceContact(
                            getContactSource(),
                            (String) props[PR_ORIGINAL_ENTRYID],
                            displayName,
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
                        /*
                         * Ignore it, the image isn't as vital as the
                         * SourceContact.
                         */
                    }
                }

                addQueryResult(sourceContact);
            }
        }
        return (getStatus() == QUERY_IN_PROGRESS);
    }

    /**
     * Gets the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     * which is to represent an <tt>ABPerson</tt> specified by the values of its
     * {@link #ABPERSON_PROPERTIES}.
     *
     * @param values the values of the <tt>ABPERSON_PROPERTIES</tt> which
     * represent the <tt>ABPerson</tt> to get the <tt>contactDetails</tt> of
     * @return the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     * which is to represent the <tt>ABPerson</tt> specified by <tt>values</tt>
     */
    private List<ContactDetail> getContactDetails(Object[] values)
    {
        List<Class<? extends OperationSet>> supportedOpSets
            = new ArrayList<Class<? extends OperationSet>>(2);
        supportedOpSets.add(OperationSetBasicTelephony.class);
        // can be added as contacts
        supportedOpSets.add(OperationSetPersistentPresence.class);

        List<ContactDetail> contactDetails = new LinkedList<ContactDetail>();

        for (int i = 0; i < CONTACT_DETAIL_PROP_INDEXES.length; i++)
        {
            int property = CONTACT_DETAIL_PROP_INDEXES[i];
            Object value = values[property];

            if (value instanceof String)
            {
                String stringValue = (String) value;

                if (stringValue.length() != 0)
                {
                    if(isPhoneNumber(property))
                        stringValue
                            = PhoneNumberI18nService.normalize(stringValue);

                    MsOutlookAddrBookContactDetail contactDetail
                        = new MsOutlookAddrBookContactDetail(
                                stringValue,
                                getCategory(property),
                                getSubCategories(property),
                                MAPI_MAILUSER_PROP_IDS[property]);

                    // Check if this contact detail support the telephony and
                    // the persistent presence operation set.
                    for(int j = 0;
                            j < CONTACT_OPERATION_SET_ABLE_PROP_INDEXES.length;
                            ++j)
                    {
                        if(property
                                == CONTACT_OPERATION_SET_ABLE_PROP_INDEXES[j])
                        {
                            contactDetail.setSupportedOpSets(supportedOpSets);
                            // Found, then break the loop.
                            j = CONTACT_OPERATION_SET_ABLE_PROP_INDEXES.length;
                        }
                    }
                    contactDetails.add(contactDetail);
                }
            }
        }
        
        return contactDetails;
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    protected void run()
    {
        synchronized (MsOutlookAddrBookContactQuery.class)
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
                        catch (MsOutlookMAPIHResultException e)
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug(
                                    MsOutlookAddrBookContactQuery.class
                                            .getSimpleName()
                                        + "#onMailUser(long)",
                                    e);
                            }
                            return false;
                        }
                    }
                });
        }
    }

    /**
     * Callback method when receiving notifications for inserted items.
     *
     * @param person The pointer to the outlook contact object.
     */
    public void inserted(long person)
    {
        try
        {
            onMailUser(person);
        }
        catch (MsOutlookMAPIHResultException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        MsOutlookAddrBookContactQuery.class.getSimpleName()
                        + "#onMailUser(long)",
                        e);
            }
        }
    }

    /**
     * Callback method when receiving notifications for updated items.
     *
     * @param person The pointer to the outlook contact object.
     */
    public void updated(long person)
    {
        Object[] props = null;
        try
        {
            props = IMAPIProp_GetProps(
                    person,
                    MAPI_MAILUSER_PROP_IDS,
                    MAPI_UNICODE);
        }
        catch (MsOutlookMAPIHResultException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        MsOutlookAddrBookContactQuery.class.getSimpleName()
                        + "#IMAPIProp_GetProps(long, long[], long)",
                        e);
            }
        }

        if(props[PR_ORIGINAL_ENTRYID] != null)
        {
            SourceContact sourceContact
                = findSourceContactByID((String) props[PR_ORIGINAL_ENTRYID]);

            if(sourceContact != null
                    && sourceContact instanceof MsOutlookAddrBookSourceContact)
            {
                // let's update the the details
                MsOutlookAddrBookSourceContact editableSourceContact
                    = (MsOutlookAddrBookSourceContact) sourceContact;

                List<ContactDetail> contactDetails = getContactDetails(props);
                editableSourceContact.setDetails(contactDetails);

                fireContactChanged(sourceContact);
            }
        }
    }

    /**
     * Callback method when receiving notifications for deleted items.
     *
     * @param id The outlook contact identifier.
     */
    public void deleted(String id)
    {
        if(id != null)
        {
            SourceContact sourceContact = findSourceContactByID(id);

            if(sourceContact != null)
            {
                fireContactRemoved(sourceContact);
            }
        }
    }
}
