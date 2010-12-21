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
 * Implements <tt>ContactSourceService</tt> for the Address Book of Microsoft
 * Outlook.
 *
 * @author Lyubomir Marinov
 */
public class MsOutlookAddressBookContactSourceService
    implements ContactSourceService
{
    private static final long MAPI_INIT_VERSION = 0;

    private static final long MAPI_MULTITHREAD_NOTIFICATIONS = 0x00000001;

    static
    {
        System.loadLibrary("jmsoutlook");
    }

    /**
     * The <tt>List</tt> of <tt>MsOutlookAddressBookContactQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<MsOutlookAddressBookContactQuery> queries
        = new LinkedList<MsOutlookAddressBookContactQuery>();

    /**
     * Initializes a new <tt>MsOutlookAddressBookContactSourceService</tt>
     * instance.
     *
     * @throws MsOutlookMAPIHResultException if anything goes wrong while
     * initializing the new <tt>MsOutlookAddressBookContactSourceService</tt>
     * instance
     */
    public MsOutlookAddressBookContactSourceService()
        throws MsOutlookMAPIHResultException
    {
        MAPIInitialize(MAPI_INIT_VERSION, MAPI_MULTITHREAD_NOTIFICATIONS);
    }

    /**
     * Gets a human-readable <tt>String</tt> which names this
     * <tt>ContactSourceService</tt> implementation.
     *
     * @return a human-readable <tt>String</tt> which names this
     * <tt>ContactSourceService</tt> implementation
     * @see ContactSourceService#getDisplayName()
     */
    public String getDisplayName()
    {
        return "Microsoft Outlook Address Book";
    }

    /**
     * Gets a <tt>String</tt> which uniquely identifies the instances of the
     * <tt>MsOutlookAddressBookContactSourceService</tt> implementation.
     *
     * @return a <tt>String</tt> which uniquely identifies the instances of the
     * <tt>MsOutlookAddressBookContactSourceService</tt> implementation
     * @see ContactSourceService#getIdentifier()
     */
    public String getIdentifier()
    {
        return "MsOutlookAddressBook";
    }

    private static native void MAPIInitialize(long version, long flags)
        throws MsOutlookMAPIHResultException;

    private static native void MAPIUninitialize();

    /**
     * Queries this <tt>ContactSourceService</tt> for <tt>SourceContact</tt>s
     * which match a specific <tt>query</tt> <tt>String</tt>.
     *
     * @param query the <tt>String</tt> which this <tt>ContactSourceService</tt>
     * is being queried for
     * @return a <tt>ContactQuery</tt> which represents the query of this
     * <tt>ContactSourceService</tt> implementation for the specified
     * <tt>String</tt> and via which the matching <tt>SourceContact</tt>s (if
     * any) will be returned
     * @see ContactSourceService#queryContactSource(String)
     */
    public ContactQuery queryContactSource(String query)
    {
        MsOutlookAddressBookContactQuery msoabcq
            = new MsOutlookAddressBookContactQuery(this, query);

        synchronized (queries)
        {
            queries.add(msoabcq);
        }

        boolean msoabcqHasStarted = false;

        try
        {
            msoabcq.start();
            msoabcqHasStarted = true;
        }
        finally
        {
            if (!msoabcqHasStarted)
            {
                synchronized (queries)
                {
                    if (queries.remove(msoabcq))
                        queries.notify();
                }
            }
        }
        return msoabcq;
    }

    /**
     * Stops this <tt>ContactSourceService</tt> implementation and prepares it
     * for garbage collection.
     */
    void stop()
    {
        boolean interrupted = false;

        synchronized (queries)
        {
            while (!queries.isEmpty())
            {
                queries.get(0).cancel();
                try
                {
                    queries.wait();
                }
                catch (InterruptedException iex)
                {
                    interrupted = true;
                }
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();

        MAPIUninitialize();
    }

    /**
     * Notifies this <tt>MsOutlookAddressBookContactSourceService</tt> that a
     * specific <tt>MsOutlookAddressBookContactQuery</tt> has stopped.
     *
     * @param msoabcq the <tt>MsOutlookAddressBookContactQuery</tt> which has
     * stopped
     */
    void stopped(MsOutlookAddressBookContactQuery msoabcq)
    {
        synchronized (queries)
        {
            if (queries.remove(msoabcq))
                queries.notify();
        }
    }
}
