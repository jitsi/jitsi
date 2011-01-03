/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msoutlook;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements <tt>ContactQuery</tt> for the Address Book of Microsoft Outlook.
 *
 * @author Lyubomir Marinov
 */
public class MsOutlookAddressBookContactQuery
    extends AbstractContactQuery<MsOutlookAddressBookContactSourceService>
{

    /**
     * The IDs of the properties of <tt>MAPI_MAILUSER</tt> which are to be
     * queried by the <tt>MsOutlookAddressBookContactQuery</tt> instances.
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
            0x3A1C /* PR_MOBILE_TELEPHONE_NUMBER */
        };

    /**
     * The flag which signals that MAPI strings should be returned in the
     * Unicode character set.
     */
    private static final long MAPI_UNICODE = 0x80000000;

    /**
     * The index of the id of the <tt>PR_BUSINESS_TELEPHONE_NUMBER</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_BUSINESS_TELEPHONE_NUMBER_INDEX = 5;

    /**
     * The index of the id of the <tt>PR_BUSINESS2_TELEPHONE_NUMBER</tt>
     * property in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_BUSINESS2_TELEPHONE_NUMBER_INDEX = 6;

    /**
     * The index of the id of the <tt>PR_DISPLAY_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_DISPLAY_NAME_INDEX = 0;

    /**
     * The index of the id of the <tt>PR_EMAIL_ADDRESS</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_EMAIL_ADDRESS_INDEX = 1;

    /**
     * The index of the id of the <tt>PR_GIVEN_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_GIVEN_NAME_INDEX = 2;

    /**
     * The index of the id of the <tt>PR_HOME_TELEPHONE_NUMBER</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_HOME_TELEPHONE_NUMBER_INDEX = 7;

    /**
     * The index of the id of the <tt>PR_HOME2_TELEPHONE_NUMBER</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_HOME2_TELEPHONE_NUMBER_INDEX = 8;

    /**
     * The index of the id of the <tt>PR_MIDDLE_NAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_MIDDLE_NAME_INDEX = 3;

    /**
     * The index of the id of the <tt>PR_MOBILE_TELEPHONE_NUMBER</tt> property
     * in {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_MOBILE_TELEPHONE_NUMBER_INDEX = 9;

    /**
     * The index of the id of the <tt>PR_SURNAME</tt> property in
     * {@link #MAPI_MAILUSER_PROP_IDS}.
     */
    private static final int PR_SURNAME_INDEX = 4;

    /**
     * The indexes in {@link #MAPI_MAILUSER_PROP_IDS} of the property IDs which
     * are to be represented in <tt>SourceContact</tt> as
     * <tt>ContactDetail</tt>s.
     */
    private static final int[] CONTACT_DETAIL_PROP_INDEXES
        = new int[]
        {
            PR_EMAIL_ADDRESS_INDEX,
            PR_BUSINESS_TELEPHONE_NUMBER_INDEX,
            PR_BUSINESS2_TELEPHONE_NUMBER_INDEX,
            PR_HOME_TELEPHONE_NUMBER_INDEX,
            PR_HOME2_TELEPHONE_NUMBER_INDEX,
            PR_MOBILE_TELEPHONE_NUMBER_INDEX
        };

    static
    {
        System.loadLibrary("jmsoutlook");
    }

    /**
     * The <tt>String</tt> for which the associated
     * <tt>MsOutlookAddressBookContactSourceService</tt> is being queried.
     */
    private String query;

    /**
     * The <tt>SourceContact</tt>s which match {@link #query}.
     */
    private final List<SourceContact> queryResults
        = new LinkedList<SourceContact>();

    /**
     * The <tt>Thread</tt> in which this
     * <tt>MsOutlookAddressBookContactQuery</tt> is performing {@link #query}.
     */
    private Thread thread;

    /**
     * Initializes a new <tt>MsOutlookAddressBookContactQuery</tt> instance to
     * be performed by a specific
     * <tt>MsOutlookAddressBookContactSourceService</tt>.
     *
     * @param msoabcss the <tt>MsOutlookAddressBookContactSourceService</tt>
     * which is to perform the new <tt>ContactQuery</tt>
     * @param query the <tt>String</tt> for which <tt>msoabcss</tt> is being
     * queried
     */
    public MsOutlookAddressBookContactQuery(
            MsOutlookAddressBookContactSourceService msoabcss,
            String query)
    {
        super(msoabcss);

        this.query = (query == null) ? null : query.toLowerCase();
    }

    /**
     * Calls back to a specific <tt>MsOutlookAddressBookCallback</tt> for each
     * <tt>MAPI_MAILUSER</tt> found in the Address Book of Microsoft Outlook
     * which matches a specific <tt>String</tt>.
     *
     * @param query the <tt>String</tt> for which the Address Book of Microsoft
     * Outlook is to be queried. <bb>Warning</bb>: Ignored at the time of this
     * writing.
     * @param callback the <tt>MsOutlookAddressBookCallback</tt> to be notified
     * about the matching <tt>MAPI_MAILUSER</tt>s
     */
    private static native void foreachMailUser(
            String query,
            MsOutlookAddressBookCallback callback);

    /**
     * Gets the <tt>List</tt> of <tt>SourceContact</tt>s which match this
     * <tt>ContactQuery</tt>.
     *
     * @return the <tt>List</tt> of <tt>SourceContact</tt>s which match this
     * <tt>ContactQuery</tt>
     * @see ContactQuery#getQueryResults()
     */
    public List<SourceContact> getQueryResults()
    {
        List<SourceContact> qr;

        synchronized (queryResults)
        {
            qr = new ArrayList<SourceContact>(queryResults.size());
            qr.addAll(queryResults);
        }
        return qr;
    }

    private static native Object[] IMAPIProp_GetProps(
            long mapiProp,
            long[] propIds, long flags)
        throws MsOutlookMAPIHResultException;

    /**
     * Notifies this <tt>MsOutlookAddressBookContactQuery</tt> about a specific
     * <tt>MAPI_MAILUSER</tt>.
     *
     * @param iUnknown a pointer to an <tt>IUnknown</tt> instance for the
     * <tt>MAPI_MAILUSER</tt> to notify about
     * @return <tt>true</tt> if this <tt>MsOutlookAddressBookContactQuery</tt>
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
        boolean matches = false;

        for (Object prop : props)
        {
            if ((prop instanceof String)
                    && ((String) prop).toLowerCase().contains(query))
            {
                matches = true;
                break;
            }
        }
        if (matches)
        {
            List<ContactDetail> contactDetails
                = new LinkedList<ContactDetail>();

            for (int i = 0; i < CONTACT_DETAIL_PROP_INDEXES.length; i++)
            {
                Object prop = props[CONTACT_DETAIL_PROP_INDEXES[i]];

                if (prop instanceof String)
                {
                    String stringProp = (String) prop;

                    if (stringProp.length() != 0)
                        contactDetails.add(new ContactDetail(stringProp));
                }
            }

            SourceContact sourceContact
                = new MsOutlookMailUserSourceContact(
                        getContactSource(),
                        (String) props[PR_DISPLAY_NAME_INDEX],
                        contactDetails);

            queryResults.add(sourceContact);
            fireContactReceived(sourceContact);
        }
        return (getStatus() == QUERY_IN_PROGRESS);
    }

    /**
     * Starts this <tt>MsOutlookAddressBookContactQuery</tt>.
     */
    synchronized void start()
    {
        if (thread == null)
        {
            thread
                = new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            foreachMailUser(
                                query,
                                new MsOutlookAddressBookCallback()
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
                        finally
                        {
                            synchronized (MsOutlookAddressBookContactQuery.this)
                            {
                                if (thread == Thread.currentThread())
                                {
                                    getContactSource().stopped(
                                            MsOutlookAddressBookContactQuery.this);
                                }
                            }
                        }
                    }
                };
            thread.start();
        }
        else
            throw new IllegalStateException("thread");
    }
}
