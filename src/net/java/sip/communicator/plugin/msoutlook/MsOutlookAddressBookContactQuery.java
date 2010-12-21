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

        this.query = query;
    }

    /**
     * Calls back to a specific <tt>MsOutlookAddressBookCallback</tt> for each
     * <tt>MAPI_MAILUSER</tt> found in the Address Book of Microsoft Outlook
     * which matches a specific <tt>String</tt>.
     *
     * @param query the <tt>String</tt> for which the Address Book of Microsoft
     * Outlook is to be queried
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

    /**
     * Notifies this <tt>MsOutlookAddressBookContactQuery</tt> about a specific
     * <tt>MAPI_MAILUSER</tt>.
     *
     * @param iUnknown a pointer to an <tt>IUnknown</tt> instance for the
     * <tt>MAPI_MAILUSER</tt> to notify about
     * @return <tt>true</tt> if this <tt>MsOutlookAddressBookContactQuery</tt>
     * is to continue being called; otherwise, <tt>false</tt>
     */
    private boolean onMailUser(long iUnknown)
    {
        // TODO Auto-generated method stub
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
                                        return onMailUser(iUnknown);
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
