/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import java.util.regex.*;

import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>ContactSourceService</tt> for the Address Book of Microsoft
 * Outlook.
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */
public class MsOutlookAddrBookContactSourceService
    extends AsyncContactSourceService
    implements EditableContactSourceService
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>MsOutlookAddrBookContactSourceService</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MsOutlookAddrBookContactSourceService.class);

    /**
     * The outlook address book prefix.
     */
    public static final String OUTLOOK_ADDR_BOOK_PREFIX
        = "net.java.sip.communicator.plugin.addrbook.OUTLOOK_ADDR_BOOK_PREFIX";

    /**
     * Boolean property that defines whether using this contact source service
     * as result for the search field is authorized.  This is useful when an
     * external plugin looks for result of this contact source service, but want
     * to display the search field result from its own (avoid duplicate
     * results).
     */
    public static final String PNAME_OUTLOOK_ADDR_BOOK_SEARCH_FIELD_DISABLED =
        "net.java.sip.communicator.plugin.addrbook.OUTLOOK_ADDR_BOOK_SEARCH_FIELD_DISABLED";

    private static final long MAPI_INIT_VERSION = 0;

    private static final long MAPI_MULTITHREAD_NOTIFICATIONS = 0x00000001;

    /**
     * The latest query created.
     */
    private MsOutlookAddrBookContactQuery latestQuery = null;

    static
    {
        try
        {
            System.loadLibrary("jmsoutlookaddrbook");
        }
        catch (Throwable ex)
        {
            logger.error("Unable to load outlook native lib", ex);
            throw new RuntimeException(ex);
        }

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
        MAPIInitialize(
                MAPI_INIT_VERSION,
                MAPI_MULTITHREAD_NOTIFICATIONS,
                new NotificationsDelegate());
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
     * @see ContactSourceService#getType()
     */
    public int getType()
    {
        return SEARCH_TYPE;
    }

    private static native void MAPIInitialize(
            long version,
            long flags,
            NotificationsDelegate callback)
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
        if(latestQuery != null)
            latestQuery.clear();

        latestQuery = new MsOutlookAddrBookContactQuery(this, query);

        latestQuery.start();
        return latestQuery;
    }

    /**
     * Stops this <tt>ContactSourceService</tt> implementation and prepares it
     * for garbage collection.
     *
     * @see AsyncContactSourceService#stop()
     */
    @Override
    public void stop()
    {
        if(latestQuery != null)
        {
            latestQuery.clear();
            latestQuery = null;
        }
        MAPIUninitialize();
    }

    /**
     * Returns the global phone number prefix to be used when calling contacts
     * from this contact source.
     *
     * @return the global phone number prefix
     */
    public String getPhoneNumberPrefix()
    {
        return AddrBookActivator.getConfigService()
                .getString(OUTLOOK_ADDR_BOOK_PREFIX);
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    public int getIndex()
    {
        return -1;
    }

    /**
     * Delegate class to be notified for addressbook changes.
     */
    public class NotificationsDelegate
    {
        /**
         * Callback method when receiving notifications for inserted items.
         */
        public void inserted(String id)
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("Inserted: " + id);
            }
            if(latestQuery != null)
                latestQuery.inserted(id);
        }

        /**
         * Callback method when receiving notifications for updated items.
         */
        public void updated(String id)
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("Updated: " + id);
            }
            if(latestQuery != null)
                latestQuery.updated(id);
        }

        /**
         * Callback method when receiving notifications for deleted items.
         */
        public void deleted(String id)
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("Deleted: " + id);
            }
            if(latestQuery != null)
                latestQuery.deleted(id);
        }
    }

    /**
     * Creates a new contact from the database (i.e "contacts" or
     * "msoutlook", etc.).
     *
     * @return The ID of the contact to remove. NULL if failed to create a new
     * contact.
     */
    public String createContact()
    {
        return MsOutlookAddrBookContactQuery.createContact();
    }

    /**
     * Adds a new empty contact, which will be filled in later.
     *
     * @param id The ID of the contact to add.
     */
    public void addEmptyContact(String id)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug("Add empty contact: " + id);
        }
        if(id != null && latestQuery != null)
        {
            latestQuery.addEmptyContact(id);
        }
    }

    /**
     * Removes the given contact from the database (i.e "contacts" or
     * "msoutlook", etc.).
     *
     * @param id The ID of the contact to remove.
     */
    public void deleteContact(String id)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug("Delete contact: " + id);
        }
        if(id != null && MsOutlookAddrBookContactQuery.deleteContact(id))
        {
            if(latestQuery != null)
            {
                latestQuery.deleted(id);
            }
        }
    }

    /**
     * Defines whether using this contact source service can be used as result
     * for the search field. This is useful when an external plugin looks for
     * result of this contact source service, but want to display the search
     * field result from its own (avoid duplicate results).
     *
     * @return True if this contact source service can be used to perform search
     * for contacts. False otherwise.
     */
    @Override
    public boolean canBeUsedToSearchContacts()
    {
        return !AddrBookActivator.getConfigService().getBoolean(
                    PNAME_OUTLOOK_ADDR_BOOK_SEARCH_FIELD_DISABLED, false);
    }
}
