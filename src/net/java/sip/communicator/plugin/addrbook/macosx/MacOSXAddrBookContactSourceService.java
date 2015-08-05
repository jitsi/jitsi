/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.addrbook.macosx;

import java.util.regex.*;

import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements <tt>ContactSourceService</tt> for the Address Book of Mac OS X.
 *
 * @author Lyubomir Marinov
 */
public class MacOSXAddrBookContactSourceService
    extends AsyncContactSourceService
    implements EditableContactSourceService, PrefixedContactSourceService
{
    /**
     * the Mac OS X address book prefix.
     */
    public static final String MACOSX_ADDR_BOOK_PREFIX
        = "net.java.sip.communicator.plugin.addrbook.MACOSX_ADDR_BOOK_PREFIX";

    /**
     * Boolean property that defines whether using this contact source service
     * as result for the search field is authorized.  This is useful when an
     * external plugin looks for result of this contact source service, but want
     * to display the search field result from its own (avoid duplicate
     * results).
     */
    public static final String PNAME_MACOSX_ADDR_BOOK_SEARCH_FIELD_DISABLED =
        "net.java.sip.communicator.plugin.addrbook.MACOSX_ADDR_BOOK_SEARCH_FIELD_DISABLED";


    static
    {
        System.loadLibrary("jmacosxaddrbook");
    }

    /**
     * The pointer to the native counterpart of this
     * <tt>MacOSXAddrBookContactSourceService</tt>.
     */
    private long ptr;

    /**
     * The latest query created.
     */
    private MacOSXAddrBookContactQuery latestQuery;

    /**
     * Initializes a new <tt>MacOSXAddrBookContactSourceService</tt> instance.
     */
    public MacOSXAddrBookContactSourceService()
    {
        ptr = start();
        if (0 == ptr)
            throw new IllegalStateException("ptr");
        setDelegate(ptr, new NotificationsDelegate());
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
        return "Address Book";
    }

    /**
     * Gets a <tt>String</tt> which uniquely identifies the instances of the
     * <tt>MacOSXAddrBookContactSourceService</tt> implementation.
     *
     * @return a <tt>String</tt> which uniquely identifies the instances of the
     * <tt>MacOSXAddrBookContactSourceService</tt> implementation
     * @see ContactSourceService#getType()
     */
    public int getType()
    {
        return SEARCH_TYPE;
    }

    /**
     * Creates query that searches for <tt>SourceContact</tt>s
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
    public ContactQuery createContactQuery(Pattern query)
    {
        if(latestQuery != null)
            latestQuery.clear();

        latestQuery = new MacOSXAddrBookContactQuery(this, query);

        return latestQuery;
    }

    /**
     * Starts a new native <tt>MacOSXAddrBookContactSourceService</tt> instance.
     *
     * @return a pointer to the newly-started native
     * <tt>MacOSXAddrBookContactSourceService</tt> instance
     */
    private static native long start();

    /**
     * Stops this <tt>ContactSourceService</tt> implementation and prepares it
     * for garbage collection.
     *
     * @see AsyncContactSourceService#stop()
     */
    @Override
    public synchronized void stop()
    {
        if (0 != ptr)
        {
            if(latestQuery != null)
            {
                latestQuery.clear();
                latestQuery = null;
            }

            stop(ptr);
            ptr = 0;
        }
    }

    /**
     * Returns the global phone number prefix to be used when calling contacts
     * from this contact source.
     *
     * @return the global phone number prefix
     */
    @Override
    public String getPhoneNumberPrefix()
    {
        return AddrBookActivator.getConfigService()
                .getString(MACOSX_ADDR_BOOK_PREFIX);
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
     * Stops a native <tt>MacOSXAddrBookContactSourceService</tt>.
     *
     * @param ptr the pointer to the native
     * <tt>MacOSXAddrBookContactSourceService</tt> to stop
     */
    private static native void stop(long ptr);

    /**
     * Sets notifier delegate.
     * @param ptr
     * @param delegate
     */
    public static native void setDelegate(long ptr, NotificationsDelegate delegate);

    /**
     * Delegate class to be notified for addressbook changes.
     */
    public class NotificationsDelegate
    {
        /**
         * Callback method when receiving notifications for inserted items.
         */
        public void inserted(long person)
        {
            if(latestQuery != null)
                latestQuery.inserted(person);
        }

        /**
         * Callback method when receiving notifications for updated items.
         */
        public void updated(long person)
        {
            if(latestQuery != null)
                latestQuery.updated(person);
        }

        /**
         * Callback method when receiving notifications for deleted items.
         */
        public void deleted(String id)
        {
            if(latestQuery != null)
                latestQuery.deleted(id);
        }
    }

    /**
     * Returns the latest query created.
     *
     * @return the latest query created.
     */
    public MacOSXAddrBookContactQuery getLatestQuery()
    {
        return this.latestQuery;
    }

    /**
     * Creates a new contact from the database (i.e "contacts" or
     * "msoutlook", etc.).
     *
     * @return The ID of the contact to created. NULL if failed to create a new
     * contact.
     */
    public String createContact()
    {
        return MacOSXAddrBookContactQuery.createContact();
    }

    /**
     * Adds a new empty contact, which will be filled in later.
     *
     * @param id The ID of the contact to add.
     */
    public void addEmptyContact(String id)
    {
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
        if(id != null && MacOSXAddrBookContactQuery.deleteContact(id))
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
                    PNAME_MACOSX_ADDR_BOOK_SEARCH_FIELD_DISABLED, false);
    }

    /**
     * Returns the bitness of this contact source service.
     *
     * @return The bitness of this contact source service.
     */
    public int getBitness()
    {
        return -1;
    }

    /**
     * Returns the version of this contact source service.
     *
     * @return The version of this contact source service.
     */
    public int getVersion()
    {
        return -1;
    }

    /**
     * Returns the number of contact notifications to deal with.
     *
     * @return The number of contact notifications to deal with.
     */
    public int getNbRemainingNotifications()
    {
        return 0;
    }
}
