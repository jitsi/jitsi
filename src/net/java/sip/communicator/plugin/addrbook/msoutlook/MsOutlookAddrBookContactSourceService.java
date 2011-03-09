/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements <tt>ContactSourceService</tt> for the Address Book of Microsoft
 * Outlook.
 *
 * @author Lyubomir Marinov
 */
public class MsOutlookAddrBookContactSourceService
    extends AsyncContactSourceService
{
    private static final long MAPI_INIT_VERSION = 0;

    private static final long MAPI_MULTITHREAD_NOTIFICATIONS = 0x00000001;

    static
    {
        System.loadLibrary("jmsoutlookaddrbook");

        /*
         * We have multiple reports of an "UnsatisfiedLinkError: no
         * jmsoutlookaddrbook in java.library.path" at
         * MsOutlookAddrBookContactSourceService#queryContactSource() which
         * seems strange since getting there means that we have already
         * successfully gone through the System.loadLibrary() above. Try to load
         * MsOutlookAddrBookContactQuery here and see how it goes.
         */
        try
        {
            Class.forName(MsOutlookAddrBookContactQuery.class.getName());
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new RuntimeException(cnfe);
        }
    }

    /**
     * The <tt>List</tt> of <tt>MsOutlookAddrBookContactQuery</tt> instances
     * which have been started and haven't stopped yet.
     */
    private final List<MsOutlookAddrBookContactQuery> queries
        = new LinkedList<MsOutlookAddrBookContactQuery>();

    /**
     * Initializes a new <tt>MsOutlookAddrBookContactSourceService</tt>
     * instance.
     *
     * @throws MsOutlookMAPIHResultException if anything goes wrong while
     * initializing the new <tt>MsOutlookAddrBookContactSourceService</tt>
     * instance
     */
    public MsOutlookAddrBookContactSourceService()
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
        return "Microsoft Outlook";
    }

    /**
     * Gets a <tt>String</tt> which uniquely identifies the instances of the
     * <tt>MsOutlookAddrBookContactSourceService</tt> implementation.
     *
     * @return a <tt>String</tt> which uniquely identifies the instances of the
     * <tt>MsOutlookAddrBookContactSourceService</tt> implementation
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
     * which match a specific <tt>query</tt> <tt>Pattern</tt>.
     *
     * @param query the <tt>Pattern</tt> which this
     * <tt>ContactSourceService</tt> is being queried for
     * @return a <tt>ContactQuery</tt> which represents the query of this
     * <tt>ContactSourceService</tt> implementation for the specified
     * <tt>Pattern</tt> and via which the matching <tt>SourceContact</tt>s (if
     * any) will be returned
     * @see ExtendedContactSourceService#queryContactSource(Pattern)
     */
    public ContactQuery queryContactSource(Pattern query)
    {
        MsOutlookAddrBookContactQuery msoabcq
            = new MsOutlookAddrBookContactQuery(this, query);

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
     *
     * @see AsyncContactSourceService#stop()
     */
    public void stop()
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
     * Notifies this <tt>MsOutlookAddrBookContactSourceService</tt> that a
     * specific <tt>MsOutlookAddrBookContactQuery</tt> has stopped.
     *
     * @param msoabcq the <tt>MsOutlookAddrBookContactQuery</tt> which has
     * stopped
     */
    void stopped(MsOutlookAddrBookContactQuery msoabcq)
    {
        synchronized (queries)
        {
            if (queries.remove(msoabcq))
                queries.notify();
        }
    }
}
