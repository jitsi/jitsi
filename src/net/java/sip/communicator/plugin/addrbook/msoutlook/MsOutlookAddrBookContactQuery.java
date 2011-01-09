/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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

    private static native Object[] IMAPIProp_GetProps(
            long mapiProp,
            long[] propIds, long flags)
        throws MsOutlookMAPIHResultException;

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

        boolean matches = false;

        for (Object prop : props)
        {
            if ((prop instanceof String) && query.matcher((String) prop).find())
            {
                matches = true;
                break;
            }
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
                Object prop = props[CONTACT_DETAIL_PROP_INDEXES[i]];

                if (prop instanceof String)
                {
                    String stringProp = (String) prop;

                    if (stringProp.length() != 0)
                    {
                        ContactDetail contactDetail
                            = new ContactDetail(stringProp);

                        contactDetail.setSupportedOpSets(supportedOpSets);
                        contactDetails.add(contactDetail);
                    }
                }
            }

            AddrBookSourceContact sourceContact
                = new AddrBookSourceContact(
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
     * @see AsyncContactQuery#stopped()
     */
    @Override
    protected void stopped()
    {
        getContactSource().stopped(this);
    }
}
