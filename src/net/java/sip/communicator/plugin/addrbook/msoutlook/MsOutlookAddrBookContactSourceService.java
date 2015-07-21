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
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import java.util.*;
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
    implements EditableContactSourceService, PrefixedContactSourceService
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

    /**
     * Boolean property that defines whether the warning for the default mail
     * client should be shown or not.
     */
    public static final String PNAME_OUTLOOK_ADDR_BOOK_SHOW_DEFAULTMAILCLIENT_WARNING =
        "net.java.sip.communicator.plugin.addrbook.SHOW_DEFAULTMAILCLIENT_WARNING";

    private static final long MAPI_INIT_VERSION = 0;

    private static final long MAPI_MULTITHREAD_NOTIFICATIONS = 0x00000001;
    
    private static final int NATIVE_LOGGER_LEVEL_INFO = 0;

    private static final int NATIVE_LOGGER_LEVEL_TRACE = 1;

    /**
     * The thread used to collect the notifications.
     */
    private NotificationThread notificationThread = null;

    /**
     * The mutex used to synchronized the notification thread.
     */
    private Object notificationThreadMutex = new Object();

    /**
     * The latest query created.
     */
    private MsOutlookAddrBookContactQuery latestQuery = null;

    /**
     * Indicates whether MAPI is initialized or not.
     */
    private static boolean isMAPIInitialized = false;

    static
    {
        String lib = "jmsoutlookaddrbook";

        try
        {
            System.loadLibrary(lib);
        }
        catch (Throwable t)
        {
            logger.error(
                    "Failed to load native library " + lib + ": "
                        + t.getMessage());
            throw new RuntimeException(t);
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

        int bitness = getOutlookBitnessVersion();
        int version = getOutlookVersion();
        if(bitness != -1 && version != -1)
        {
            logger.info(
                    "Outlook " + version + "-x" + bitness + " is installed.");
        }
    }

    /**
     * Initializes a new <tt>MsOutlookAddrBookContactSourceService</tt>
     * instance.
     * @param notificationDelegate the object to be notified for addressbook 
     * changes
     * @throws MsOutlookMAPIHResultException if anything goes wrong while
     * initializing the new <tt>MsOutlookAddrBookContactSourceService</tt>
     * instance
     */
    public static void initMAPI(NotificationsDelegate notificationDelegate)
        throws MsOutlookMAPIHResultException
    {
        if(!isMAPIInitialized)
        {
            boolean isOutlookDefaultMailClient = isOutlookDefaultMailClient();
            boolean showWarning 
                = AddrBookActivator.getConfigService().getBoolean(
                    PNAME_OUTLOOK_ADDR_BOOK_SHOW_DEFAULTMAILCLIENT_WARNING, 
                    true);
            if(!isOutlookDefaultMailClient && showWarning)
            {
                DefaultMailClientMessageDialog dialog 
                    = new DefaultMailClientMessageDialog();
                int result = dialog.showDialog();
                if((result & DefaultMailClientMessageDialog
                    .DONT_ASK_SELECTED_MASK) != 0)
                {
                    AddrBookActivator.getConfigService().setProperty(
                        PNAME_OUTLOOK_ADDR_BOOK_SHOW_DEFAULTMAILCLIENT_WARNING, 
                        false);
                }
                
                if((result & DefaultMailClientMessageDialog
                        .DEFAULT_MAIL_CLIENT_SELECTED_MASK) != 0)
                {
                    RegistryHandler.setOutlookAsDefaultMailClient();
                }
            }
            
            if(isOutlookDefaultMailClient && !showWarning)
            {
                AddrBookActivator.getConfigService().setProperty(
                    PNAME_OUTLOOK_ADDR_BOOK_SHOW_DEFAULTMAILCLIENT_WARNING, 
                    true);
            }
            
            String logFileName = "";
            String homeLocation = System.getProperty(
                "net.java.sip.communicator.SC_LOG_DIR_LOCATION");
            String dirName = System.getProperty(
                "net.java.sip.communicator.SC_HOME_DIR_NAME");

            if(homeLocation != null && dirName != null)
            {
                logFileName = homeLocation + "\\" + dirName + "\\log\\";
            }

            int logLevel = NATIVE_LOGGER_LEVEL_INFO;
            if(logger.isTraceEnabled())
            {
                logLevel = NATIVE_LOGGER_LEVEL_TRACE;
            }

            logger.info("Init mapi with log level " + logLevel + " and log file"
                + " path " + logFileName);

            MAPIInitialize(
                    MAPI_INIT_VERSION,
                    MAPI_MULTITHREAD_NOTIFICATIONS,
                    notificationDelegate,
                    logFileName,
                    logLevel);
            isMAPIInitialized = true;
        }
    }

    /**
     * Creates new <tt>NotificationsDelegate</tt> instance.
     * @return the <tt>NotificationsDelegate</tt> instance
     */
    public NotificationsDelegate createNotificationDelegate()
    {
        return new NotificationsDelegate();
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
            NotificationsDelegate callback,
            String logFileName,
            int logLevel)
        throws MsOutlookMAPIHResultException;
    
    /**
     * Uninitializes MAPI.
     */
    public static void UninitializeMAPI()
    {
        if(isMAPIInitialized)
        {
            MAPIUninitialize();
            isMAPIInitialized = false;
        }
    }

    private static native void MAPIUninitialize();

    public static native int getOutlookBitnessVersion();

    public static native int getOutlookVersion();
    
    private static native boolean isOutlookDefaultMailClient();

    /**
     *  Creates query that searches for <tt>SourceContact</tt>s
     * which match a specific <tt>query</tt> <tt>Pattern</tt>.
     *
     * @param query the <tt>Pattern</tt> which this
     * <tt>ContactSourceService</tt> is being queried for
     * @return a <tt>ContactQuery</tt> which represents the query of this
     * <tt>ContactSourceService</tt> implementation for the specified
     * <tt>Pattern</tt> and via which the matching <tt>SourceContact</tt>s (if
     * any) will be returned
     * @see ExtendedContactSourceService#createContactQuery(Pattern)
     */
    public ContactQuery createContactQuery(Pattern query)
    {
        if(latestQuery != null)
            latestQuery.clear();

        latestQuery = new MsOutlookAddrBookContactQuery(this, query);

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
        UninitializeMAPI();
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
            if(latestQuery != null)
                addNotification(id, 'i');
        }

        /**
         * Callback method when receiving notifications for updated items.
         */
        public void updated(String id)
        {
            if(latestQuery != null)
                addNotification(id, 'u');
        }

        /**
         * Callback method when receiving notifications for deleted items.
         */
        public void deleted(String id)
        {
            if(latestQuery != null)
                addNotification(id, 'd');
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

    /**
     * Collects a new notification and adds it to the notification thread.
     *
     * @param id The contact id.
     * @param function The kind of notification: 'd' for deleted, 'u' for
     * updated and 'i' for inserted.
     */
    public void addNotification(String id, char function)
    {
        synchronized(notificationThreadMutex)
        {
            if(notificationThread == null
                    || !notificationThread.isAlive())
            {
                notificationThread = new NotificationThread();
                notificationThread.start();
            }
            notificationThread.add(id, function);
        }
    }

    /**
     * Thread used to collect the notification.
     */
    private class NotificationThread
        extends Thread
    {
        /**
         * The list of notification collected.
         */
        private Vector<NotificationIdFunction> contactIds
            = new Vector<NotificationIdFunction>();

        /**
         * Initializes a new notification thread.
         */
        public NotificationThread()
        {
            super("MsOutlookAddrbookContactSourceService notification thread");
        }

        /**
         * Dispatchs the collected notifications.
         */
        public void run()
        {
            boolean hasMore;
            NotificationIdFunction idFunction = null;
            String id;
            char function;

            synchronized(notificationThreadMutex)
            {
                hasMore = (contactIds.size() > 0);
                if(hasMore)
                {
                    idFunction = contactIds.get(0);
                }
            }
            while(hasMore)
            {
                if(latestQuery != null)
                {
                    id = idFunction.getId();
                    function = idFunction.getFunction();
                    if(function == 'd')
                    {
                        latestQuery.deleted(id);
                    }
                    else if(function == 'u')
                    {
                        latestQuery.updated(id);
                    }
                    else if(function == 'i')
                    {
                        latestQuery.inserted(id);
                    }
                }
                synchronized(notificationThreadMutex)
                {
                    contactIds.remove(0);
                    hasMore = (contactIds.size() > 0);
                    if(hasMore)
                    {
                        idFunction = contactIds.get(0);
                    }
                }
            }
        }

        /**
         * Adds a new notification. Avoids previous notification for the given
         * contact.
         *
         * @param id The contact id.
         * @param function The kind of notification: 'd' for deleted, 'u' for
         * updated and 'i' for inserted.
         */
        public void add(String id, char function)
        {
            NotificationIdFunction idFunction
                = new NotificationIdFunction(id, function);

            synchronized(notificationThreadMutex)
            {
                contactIds.remove(idFunction);
                contactIds.add(idFunction);
            }
        }

        /**
         * Returns the number of contact notifications to deal with.
         *
         * @return The number of contact notifications to deal with.
         */
        public int getNbRemainingNotifications()
        {
            return contactIds.size();
        }

        /**
         * Clear the current results.
         */
        public void clear()
        {
            synchronized(notificationThreadMutex)
            {
                contactIds.clear();
            }
        }
    }

    /**
     * Defines a notification: a combination of a contact identifier and a
     * function.
     */
    private class NotificationIdFunction
    {
        /**
         * The contact identifier.
         */
        private String id;

        /**
         * The kind of notification: 'd' for deleted, 'u' for updated and 'i'
         * for inserted.
         */
        private char function;

        /**
         * Creates a new notification.
         *
         * @param id The contact id.
         * @param function The kind of notification: 'd' for deleted, 'u' for
         * updated and 'i' for inserted.
         */
        public NotificationIdFunction(String id, char function)
        {
            this.id = id;
            this.function = function;
        }

        /**
         * Returns the contact identifier.
         *
         * @return The contact identifier.
         */
        public String getId()
        {
            return this.id;
        }

        /**
         * Returns the kind of notification.
         *
         * @return 'd' for deleted, 'u' for updated and 'i' for inserted.
         */
        public char getFunction()
        {
            return this.function;
        }

        /**
         * Returns if this notification is about the same contact has the one
         * given in parameter.
         *
         * @param obj An NotificationIdFunction to compare with.
         *
         * @return True if this notification is about the same contact has the
         * one given in parameter. False otherwise.
         */
        public boolean equals(Object obj)
        {
            return (this.id == null && obj == null
                || obj instanceof String && this.id.equals((String) obj));
        }

        /**
         * Returns the hash code corresponding to the contact identifier.
         *
         * @return The hash code corresponding to the contact identifier.
         */
        public int hashCode()
        {
            return this.id.hashCode();
        }
    }

    /**
     * Returns the bitness of this contact source service.
     *
     * @return The bitness of this contact source service.
     */
    public int getBitness()
    {
        return getOutlookBitnessVersion();
    }

    /**
     * Returns the version of this contact source service.
     *
     * @return The version of this contact source service.
     */
    public int getVersion()
    {
        return getOutlookVersion();
    }

    /**
     * Returns the number of contact notifications to deal with.
     *
     * @return The number of contact notifications to deal with.
     */
    public int getNbRemainingNotifications()
    {
        int nbNotifications = 0;

        synchronized(notificationThreadMutex)
        {
            if(notificationThread != null)
            {
                nbNotifications
                    = notificationThread.getNbRemainingNotifications();
            }
        }

        return nbNotifications;
    }

    /**
     * Cancels the contact notifications.
     */
    public void clearRemainingNotifications()
    {
        if(notificationThread != null)
        {
            notificationThread.clear();
        }
    }
}
