/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import java.beans.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

import static net.java.sip.communicator.service.history.HistoryService.DATE_FORMAT;

/**
 * The source contact service. The will show most recent messages.
 *
 * @author Damian Minkov
 */
public class MessageSourceService
    implements ContactSourceService,
               ContactPresenceStatusListener,
               ProviderPresenceStatusListener,
               LocalUserChatRoomPresenceListener,
               MessageListener,
               ChatRoomMessageListener,
               AdHocChatRoomMessageListener
{
    /**
     * The logger for this class.
     */
    private static Logger logger = Logger
        .getLogger(MessageSourceService.class);

    /**
     * The display name of this contact source.
     */
    private final String MESSAGE_HISTORY_NAME;

    /**
     * The type of the source service, the place to be shown in the ui.
     */
    private int sourceServiceType = RECENT_MESSAGES_TYPE;

    /**
     * Whether to show recent messages in history or in contactlist.
     * By default we show it in contactlist.
     */
    private static final String IN_HISTORY_PROPERTY
        = "net.java.sip.communicator.impl.msghistory.contactsrc.IN_HISTORY";

    /**
     * Property to control number of recent messages.
     */
    private static final String NUMBER_OF_RECENT_MSGS_PROP
        = "net.java.sip.communicator.impl.msghistory.contactsrc.MSG_NUMBER";

    /**
     * Property to control version of recent messages.
     */
    private static final String VER_OF_RECENT_MSGS_PROP
        = "net.java.sip.communicator.impl.msghistory.contactsrc.MSG_VER";

    /**
     * Property to control messages type. Can query for message sub type.
     */
    private static final String IS_MESSAGE_SUBTYPE_SMS_PROP
        = "net.java.sip.communicator.impl.msghistory.contactsrc.IS_SMS_ENABLED";

    /**
     * The number of recent messages to store in the history, but will retrieve
     * just <tt>numberOfMessages</tt>
     */
    private static final int NUMBER_OF_MSGS_IN_HISTORY = 100;

    /**
     * Number of messages to show.
     */
    private int numberOfMessages = 10;

    /**
     * The structure to save recent messages list.
     */
    private static final String[] STRUCTURE_NAMES
        = new String[] { "provider", "contact", "timestamp", "ver"};

    /**
     * The current version of recent messages. When changed the recent messages
     * are recreated.
     */
    private static String RECENT_MSGS_VER = "2";

    /**
     * The structure.
     */
    private static final HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(STRUCTURE_NAMES);

    /**
     * Recent messages history ID.
     */
    private static final HistoryID historyID = HistoryID.createFromRawID(
        new String[] {  "recent_messages"});

    /**
     * The cache for recent messages.
     */
    private History history = null;

    /**
     * List of recent messages.
     */
    private List<MessageSourceContact> recentMessages
        = new LinkedList<MessageSourceContact>();

    /**
     * Date of the oldest shown message.
     */
    private Date oldestRecentMessage = null;

    /**
     * The last query created.
     */
    private MessageHistoryContactQuery recentQuery = null;

    /**
     * The message subtype if any.
     */
    private boolean isSMSEnabled = false;

    /**
     * Message history service that has created us.
     */
    private MessageHistoryServiceImpl messageHistoryService;

    /**
     * Constructs MessageSourceService.
     */
    MessageSourceService(MessageHistoryServiceImpl messageHistoryService)
    {
        this.messageHistoryService = messageHistoryService;

        ConfigurationService conf
            = MessageHistoryActivator.getConfigurationService();

        if(conf.getBoolean(IN_HISTORY_PROPERTY , false))
        {
            sourceServiceType = HISTORY_TYPE;
        }

        MESSAGE_HISTORY_NAME = MessageHistoryActivator.getResources()
            .getI18NString("service.gui.RECENT_MESSAGES");

        numberOfMessages
            = conf.getInt(NUMBER_OF_RECENT_MSGS_PROP, numberOfMessages);

        isSMSEnabled
            = conf.getBoolean(IS_MESSAGE_SUBTYPE_SMS_PROP, isSMSEnabled);

        RECENT_MSGS_VER
            = conf.getString(VER_OF_RECENT_MSGS_PROP, RECENT_MSGS_VER);

        MessageSourceContactPresenceStatus.MSG_SRC_CONTACT_ONLINE
            .setStatusIcon(MessageHistoryActivator.getResources()
                .getImageInBytes("service.gui.icons.SMS_STATUS_ICON"));
    }

    /**
     * Returns the display name of this contact source.
     * @return the display name of this contact source
     */
    @Override
    public String getDisplayName()
    {
        return MESSAGE_HISTORY_NAME;
    }

    /**
     * Returns default type to indicate that this contact source can be queried
     * by default filters.
     *
     * @return the type of this contact source
     */
    @Override
    public int getType()
    {
        return sourceServiceType;
    }

    /**
     * Returns the index of the contact source in the result list.
     *
     * @return the index of the contact source in the result list
     */
    @Override
    public int getIndex()
    {
        return 0;
    }

    /**
     * Creates query for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @return the created query
     */
    @Override
    public ContactQuery createContactQuery(String queryString)
    {
        recentQuery =
            (MessageHistoryContactQuery)createContactQuery(
                queryString, numberOfMessages);

        return recentQuery;
    }

    /**
     * Searches for entries in cached recent messages in history.
     *
     * @param provider
     * @return
     */
    private List<MessageSourceContact> getSourceContacts(
        ProtocolProviderService provider)
    {
        String providerID = provider.getAccountID().getAccountUniqueID();
        List<String> recentMessagesContactIDs =
            getRecentContactIDs(providerID,
                recentMessages.size() < numberOfMessages
                    ? null : oldestRecentMessage );

        List<MessageSourceContact> sourceContactsToAdd
            = new ArrayList<MessageSourceContact>();

        for(String contactID : recentMessagesContactIDs)
        {
            Collection<EventObject> res =
                messageHistoryService.findRecentMessagesPerContact(
                    numberOfMessages,
                    providerID,
                    contactID,
                    isSMSEnabled);

            for(EventObject obj : res)
            {
                MessageSourceContact msc = new MessageSourceContact(
                    obj, MessageSourceService.this);
                if(!recentMessages.contains(msc)
                    && !sourceContactsToAdd.contains(msc))
                    sourceContactsToAdd.add(msc);
            }
        }

        return sourceContactsToAdd;
    }

    /**
     * Access for source contacts impl.
     * @return
     */
    boolean isSMSEnabled()
    {
        return isSMSEnabled;
    }

    /**
     * Add the source contacts, newly added will fire new,
     * for existing fire update and when trimming the list to desired length
     * fire remove for those that were removed
     * @param contactsToAdd
     */
    private void addNewRecentMessages(List<MessageSourceContact> contactsToAdd)
    {
        // now find object to fire new, and object to fire remove
        // let us find duplicates and fire update
        List<MessageSourceContact> duplicates
            = new ArrayList<MessageSourceContact>();
        for(MessageSourceContact msc : recentMessages)
        {
            for(MessageSourceContact mscToAdd : contactsToAdd)
            {
                if(mscToAdd.equals(msc))
                {
                    duplicates.add(msc);

                    // update currently used instance
                    msc.update(mscToAdd);

                    // save update
                    updateRecentMessageToHistory(msc);
                }
            }
        }

        if(!duplicates.isEmpty())
        {
            contactsToAdd.removeAll(duplicates);

            Collections.sort(recentMessages);

            if(recentQuery != null)
            {
                for(MessageSourceContact msc : duplicates)
                    recentQuery.fireContactChanged(msc);
            }

            return;
        }

        // now contacts to add has no duplicates, add them all
        recentMessages.addAll(contactsToAdd);

        Collections.sort(recentMessages);

        if(!recentMessages.isEmpty())
            oldestRecentMessage
                = recentMessages.get(recentMessages.size() - 1).getTimestamp();

        // trim
        List<MessageSourceContact> removedItems = null;
        if(recentMessages.size() > numberOfMessages)
        {
            removedItems = new ArrayList<MessageSourceContact>(
                recentMessages.subList(numberOfMessages, recentMessages.size()));

            recentMessages.removeAll(removedItems);
        }

        if(recentQuery != null)
        {
            // now fire, removed for all that were in the list
            // and now are removed after trim
            if(removedItems != null)
            {
                for(MessageSourceContact msc : removedItems)
                {
                    if(!contactsToAdd.contains(msc))
                        recentQuery.fireContactRemoved(msc);
                }
            }

            // fire new for all that were added, and not removed after trim
            for(MessageSourceContact msc : contactsToAdd)
            {
                if(removedItems == null
                        || !removedItems.contains(msc))
                    recentQuery.fireContactReceived(msc);
            }
        }
    }

    /**
     * When a provider is added, do not block and start executing in new thread.
     *
     * @param provider ProtocolProviderService
     */
    void handleProviderAdded(final ProtocolProviderService provider)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                handleProviderAddedInSeparateThread(provider);
            }
        }).start();
    }

    /**
     * When a provider is added. As searching can be slow especially
     * when handling special type of messages (with subType) this need to be
     * run in new Thread.
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAddedInSeparateThread(
        ProtocolProviderService provider)
    {
        // lets check if we have cached recent messages for this provider, and
        // fire events if found and are newer

        synchronized(recentMessages)
        {
            List<MessageSourceContact> sourceContactsToAdd
                = getSourceContacts(provider);

            if(sourceContactsToAdd.isEmpty())
            {
                // maybe there is no cached history for this
                // let's check
                // load it not from cache, but do a local search
                Collection<EventObject> res = messageHistoryService
                    .findRecentMessagesPerContact(
                        numberOfMessages,
                        provider.getAccountID().getAccountUniqueID(),
                        null,
                        isSMSEnabled);

                List<MessageSourceContact> newMsc
                    = new ArrayList<MessageSourceContact>();
                for(EventObject obj : res)
                {
                    MessageSourceContact msc = new MessageSourceContact(
                        obj, MessageSourceService.this);
                    if(!recentMessages.contains(msc)
                        && !newMsc.contains(msc))
                        newMsc.add(msc);
                }

                addNewRecentMessages(newMsc);

                for(MessageSourceContact msc : newMsc)
                {
                    saveRecentMessageToHistory(msc);
                }

            }
            else
                addNewRecentMessages(sourceContactsToAdd);
        }
    }

    /**
     * A provider has been removed.
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    void handleProviderRemoved(ProtocolProviderService provider)
    {
        // lets remove the recent messages for this provider, and update
        // with recent messages for the available providers
        synchronized(recentMessages)
        {
            if(provider != null)
            {
                List<MessageSourceContact> removedItems
                    = new ArrayList<MessageSourceContact>();
                for(MessageSourceContact msc : recentMessages)
                {
                    if(msc.getProtocolProviderService().equals(provider))
                        removedItems.add(msc);
                }

                recentMessages.removeAll(removedItems);
                if(!recentMessages.isEmpty())
                    oldestRecentMessage
                        = recentMessages.get(recentMessages.size() - 1)
                            .getTimestamp();
                else
                    oldestRecentMessage = null;

                if(recentQuery != null)
                {
                    for(MessageSourceContact msc : removedItems)
                    {
                        recentQuery.fireContactRemoved(msc);
                    }
                }
            }

            // lets do the same as we enable provider
            // for all registered providers and finally fire events
            List<MessageSourceContact> contactsToAdd
                = new ArrayList<MessageSourceContact>();
            for (ProtocolProviderService pps
                    : messageHistoryService.getCurrentlyAvailableProviders())
            {
                contactsToAdd.addAll(getSourceContacts(pps));
            }

            addNewRecentMessages(contactsToAdd);
        }
    }

    /**
     * Searches for contact ids in history of recent messages.
     * @param provider
     * @param after
     * @return
     */
    List<String> getRecentContactIDs(String provider, Date after)
    {
        List<String> res = new ArrayList<String>();

        try
        {
            History history = getHistory();

            if(history != null)
            {
                Iterator<HistoryRecord> recs
                    = history.getReader().findLast(NUMBER_OF_MSGS_IN_HISTORY);
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                while(recs.hasNext())
                {
                    HistoryRecord hr = recs.next();

                    String contact = null;
                    String recordProvider = null;
                    Date timestamp = null;

                    for (int i = 0; i < hr.getPropertyNames().length; i++)
                    {
                        String propName = hr.getPropertyNames()[i];

                        if (propName.equals(STRUCTURE_NAMES[0]))
                            recordProvider = hr.getPropertyValues()[i];
                        else if (propName.equals(STRUCTURE_NAMES[1]))
                            contact = hr.getPropertyValues()[i];
                        else if (propName.equals(STRUCTURE_NAMES[2]))
                        {
                            try
                            {
                                timestamp
                                    = sdf.parse(hr.getPropertyValues()[i]);
                            }
                            catch (ParseException e)
                            {
                                timestamp =
                                    new Date(Long.parseLong(
                                            hr.getPropertyValues()[i]));
                            }
                        }
                    }

                    if(recordProvider == null || contact == null)
                        continue;

                    if(after != null
                        && timestamp != null
                        && timestamp.before(after))
                        continue;

                    if(recordProvider.equals(provider))
                        res.add(contact);
                }
            }
        }
        catch(IOException ex)
        {
            logger.error("cannot create recent_messages history", ex);
        }

        return res;
    }

    /**
     * Returns the cached recent messages history.
     * @return
     * @throws IOException
     */
    private History getHistory()
        throws IOException
    {
        synchronized(historyID)
        {
            HistoryService historyService =
                MessageHistoryActivator.getMessageHistoryService()
                    .getHistoryService();

            if(history == null)
            {
                if (historyService.isHistoryExisting(historyID))
                    history = historyService.getHistory(historyID);
                else
                    history = historyService.createHistory(
                        historyID, recordStructure);

                // lets check the version if not our version, re-create
                // history (delete it)
                HistoryReader reader = history.getReader();
                boolean delete = false;
                QueryResultSet<HistoryRecord> res = reader.findLast(1);
                if(res != null && res.hasNext())
                {
                    HistoryRecord hr = res.next();
                    if(hr.getPropertyValues().length >= 4)
                    {
                        if(!hr.getPropertyValues()[3].equals(RECENT_MSGS_VER))
                            delete = true;
                    }
                    else
                        delete = true;
                }

                if(delete)
                {
                    // delete it
                    try
                    {
                        historyService.purgeLocallyStoredHistory(historyID);

                        history = historyService.createHistory(
                            historyID, recordStructure);
                    }
                    catch(IOException ex)
                    {
                        logger.error(
                            "Cannot delete recent_messages history", ex);
                    }
                }
            }

            return history;
        }
    }

    /**
     * Returns the index of the source contact, in the list of recent messages.
     * @param messageSourceContact
     * @return
     */
    int getIndex(MessageSourceContact messageSourceContact)
    {
        synchronized(recentMessages)
        {
            return recentMessages.indexOf(messageSourceContact);
        }
    }

    /**
     * Creates query for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @param contactCount the maximum count of result contacts
     * @return the created query
     */
    @Override
    public ContactQuery createContactQuery(String queryString, int contactCount)
    {
        if(!StringUtils.isNullOrEmpty(queryString))
            return null;

        recentQuery = new MessageHistoryContactQuery(numberOfMessages);

        return recentQuery;
    }

    /**
     * Updates contact source contacts with status.
     * @param evt the ContactPresenceStatusChangeEvent describing the status
     */
    @Override
    public void contactPresenceStatusChanged(ContactPresenceStatusChangeEvent evt)
    {
        if(recentQuery == null)
            return;

        synchronized(recentMessages)
        {
            for(MessageSourceContact msgSC : recentMessages)
            {
                if(msgSC.getContact() != null
                    && msgSC.getContact().equals(evt.getSourceContact()))
                {
                    msgSC.setStatus(evt.getNewStatus());
                    recentQuery.fireContactChanged(msgSC);
                }
            }
        }
    }

    @Override
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
    {
        if(!evt.getNewStatus().isOnline() || evt.getOldStatus().isOnline())
            return;

        handleProviderAdded(evt.getProvider());
    }

    @Override
    public void providerStatusMessageChanged(PropertyChangeEvent evt)
    {}

    @Override
    public void localUserPresenceChanged(
        LocalUserChatRoomPresenceChangeEvent evt)
    {
        if(recentQuery == null)
            return;

        MessageSourceContact srcContact = null;

        synchronized(recentMessages)
        {
            for(MessageSourceContact msg : recentMessages)
            {
                if(msg.getRoom() != null
                    && msg.getRoom().equals(evt.getChatRoom()))
                {
                    srcContact = msg;
                    break;
                }
            }
        }

        if(srcContact == null)
            return;

        String eventType = evt.getEventType();

        if (LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_JOINED.equals(eventType))
        {
            srcContact.setStatus(ChatRoomPresenceStatus.CHAT_ROOM_ONLINE);
            recentQuery.fireContactChanged(srcContact);
        }
        else if ((LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_LEFT.equals(eventType)
            || LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_KICKED.equals(eventType)
            || LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_DROPPED.equals(eventType))
            )
        {
            srcContact.setStatus(ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE);
            recentQuery.fireContactChanged(srcContact);
        }
    }

    /**
     * Handles new events.
     *
     * @param obj the event object
     * @param provider the provider
     * @param id the id of the source of the event
     */
    private void handle(EventObject obj,
                        ProtocolProviderService provider,
                        String id)
    {
        // check if provider - contact exist update message content
        synchronized(recentMessages)
        {
            MessageSourceContact existingMsc = null;
            for(MessageSourceContact msc : recentMessages)
            {
                if(msc.getProtocolProviderService().equals(provider)
                    && msc.getContactAddress().equals(id))
                {
                    // update
                    msc.update(obj);
                    updateRecentMessageToHistory(msc);

                    existingMsc = msc;
                }
            }

            if(existingMsc != null)
            {
                Collections.sort(recentMessages);
                oldestRecentMessage = recentMessages
                    .get(recentMessages.size() - 1).getTimestamp();

                if(recentQuery != null)
                    recentQuery.fireContactChanged(existingMsc);

                return;
            }

            // if missing create source contact
            // and update recent messages, trim and sort
            MessageSourceContact newSourceContact =
                new MessageSourceContact(obj, MessageSourceService.this);
            // we have already checked for duplicate
            recentMessages.add(newSourceContact);

            Collections.sort(recentMessages);
            oldestRecentMessage
                = recentMessages.get(recentMessages.size() - 1).getTimestamp();

            // trim
            List<MessageSourceContact> removedItems = null;
            if(recentMessages.size() > numberOfMessages)
            {
                removedItems = new ArrayList<MessageSourceContact>(
                    recentMessages.subList(
                        numberOfMessages, recentMessages.size()));

                recentMessages.removeAll(removedItems);
            }

            // save
            saveRecentMessageToHistory(newSourceContact);

            // no query nothing to fire
            if(recentQuery == null)
                return;

            // now fire
            if(removedItems != null)
            {
                for(MessageSourceContact msc : removedItems)
                {
                    recentQuery.fireContactRemoved(msc);
                }
            }

            recentQuery.fireContactReceived(newSourceContact);
        }
    }

    /**
     * Adds recent message in history.
     */
    private void saveRecentMessageToHistory(MessageSourceContact msc)
    {
        synchronized(historyID)
        {
            // and create it
            try
            {
                History history = getHistory();

                HistoryWriter writer = history.getWriter();

                synchronized(recentMessages)
                {
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                    writer.addRecord(
                        new String[]
                            {
                                msc.getProtocolProviderService()
                                    .getAccountID().getAccountUniqueID(),
                                msc.getContactAddress(),
                                sdf.format(msc.getTimestamp()),
                                RECENT_MSGS_VER
                            },
                        NUMBER_OF_MSGS_IN_HISTORY);
                }
            }
            catch(IOException ex)
            {
                logger.error("cannot create recent_messages history", ex);
                return;
            }
        }
    }

    /**
     * Updates recent message in history.
     */
    private void updateRecentMessageToHistory(final MessageSourceContact msc)
    {
        synchronized(historyID)
        {
            // and create it
            try
            {
                History history = getHistory();

                HistoryWriter writer = history.getWriter();

                synchronized(recentMessages)
                {
                    writer.updateRecord(
                        new HistoryWriter.HistoryRecordUpdater()
                        {
                            HistoryRecord hr;

                            @Override
                            public void setHistoryRecord(
                                HistoryRecord historyRecord)
                            {
                                this.hr = historyRecord;
                            }

                            @Override
                            public boolean isMatching()
                            {
                                boolean providerFound = false;
                                boolean contactFound = false;
                                for(int i = 0; i < hr.getPropertyNames().length; i++)
                                {
                                    String propName = hr.getPropertyNames()[i];

                                    if(propName.equals(STRUCTURE_NAMES[0]))
                                    {
                                        if(msc.getProtocolProviderService()
                                            .getAccountID().getAccountUniqueID()
                                            .equals(hr.getPropertyValues()[i]))
                                        {
                                            providerFound = true;
                                        }
                                    }
                                    else if(propName.equals(STRUCTURE_NAMES[1]))
                                    {
                                        if(msc.getContactAddress()
                                            .equals(hr.getPropertyValues()[i]))
                                        {
                                            contactFound = true;
                                        }
                                    }
                                }


                                return contactFound && providerFound;
                            }

                            @Override
                            public Map<String, String> getUpdateChanges()
                            {
                                HashMap<String, String> map
                                    = new HashMap<String, String>();
                                SimpleDateFormat sdf
                                    = new SimpleDateFormat(DATE_FORMAT);
                                for(int i = 0;
                                    i < hr.getPropertyNames().length;
                                    i++)
                                {
                                    String propName = hr.getPropertyNames()[i];

                                    if(propName.equals(STRUCTURE_NAMES[0]))
                                    {
                                        map.put(
                                            propName,
                                            msc.getProtocolProviderService()
                                                .getAccountID()
                                                .getAccountUniqueID());
                                    }
                                    else if(propName.equals(STRUCTURE_NAMES[1]))
                                    {
                                        map.put(propName, msc.getContactAddress());
                                    }
                                    else if(propName.equals(STRUCTURE_NAMES[2]))
                                    {
                                        map.put(propName,
                                            sdf.format(msc.getTimestamp()));
                                    }
                                    else if(propName.equals(STRUCTURE_NAMES[3]))
                                        map.put(propName, RECENT_MSGS_VER);
                                }

                                return map;
                            }
                        });
                }
            }
            catch(IOException ex)
            {
                logger.error("cannot create recent_messages history", ex);
                return;
            }
        }
    }

    @Override
    public void messageReceived(MessageReceivedEvent evt)
    {
        if(isSMSEnabled
            && evt.getEventType() != MessageReceivedEvent.SMS_MESSAGE_RECEIVED)
        {
            return;
        }

        handle(
            evt,
            evt.getSourceContact().getProtocolProvider(),
            evt.getSourceContact().getAddress());
    }

    @Override
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        if(isSMSEnabled && !evt.isSmsMessage())
            return;

        handle(
            evt,
            evt.getDestinationContact().getProtocolProvider(),
            evt.getDestinationContact().getAddress());
    }

    /**
     * Not used.
     * @param evt the <tt>MessageFailedEvent</tt> containing the ID of the
     */
    @Override
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {}

    @Override
    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        if(isSMSEnabled)
            return;

        // ignore non conversation messages
        if(evt.getEventType() !=
            ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED)
            return;

        handle(
            evt,
            evt.getSourceChatRoom().getParentProvider(),
            evt.getSourceChatRoom().getIdentifier());
    }

    @Override
    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        if(isSMSEnabled)
            return;

        handle(
            evt,
            evt.getSourceChatRoom().getParentProvider(),
            evt.getSourceChatRoom().getIdentifier());
    }

    /**
     * Not used.
     * @param evt the <tt>ChatroomMessageDeliveryFailedEvent</tt> containing
     */
    @Override
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {}

    @Override
    public void messageReceived(AdHocChatRoomMessageReceivedEvent evt)
    {
        // TODO
    }

    @Override
    public void messageDelivered(AdHocChatRoomMessageDeliveredEvent evt)
    {
        // TODO
    }

    /**
     * Not used.
     * @param evt the <tt>AdHocChatroomMessageDeliveryFailedEvent</tt>
     */
    @Override
    public void messageDeliveryFailed(AdHocChatRoomMessageDeliveryFailedEvent evt)
    {}

    /**
     * The contact query implementation.
     */
    private class MessageHistoryContactQuery
        extends AsyncContactQuery<MessageSourceService>
    {
        MessageHistoryContactQuery(int contactCount)
        {
            super(MessageSourceService.this,
                Pattern.compile("",
                    Pattern.CASE_INSENSITIVE | Pattern.LITERAL),
                false);
        }

        @Override
        public void run()
        {
            synchronized(recentMessages)
            {
                for(MessageSourceContact rm : recentMessages)
                {
                    addQueryResult(rm);
                }
            }
        }

        /**
         * Notifies the <tt>ContactQueryListener</tt>s registered with this
         * <tt>ContactQuery</tt> that a new <tt>SourceContact</tt> has been
         * received.
         *
         * @param contact the <tt>SourceContact</tt> which has been received and
         * which the registered <tt>ContactQueryListener</tt>s are to be notified
         * about
         */
        public void fireContactReceived(SourceContact contact)
        {
            fireContactReceived(contact, false);
        }

        /**
         * Notifies the <tt>ContactQueryListener</tt>s registered with this
         * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
         * changed.
         *
         * @param contact the <tt>SourceContact</tt> which has been changed and
         * which the registered <tt>ContactQueryListener</tt>s are to be notified
         * about
         */
        public void fireContactChanged(SourceContact contact)
        {
            super.fireContactChanged(contact);
        }

        /**
         * Notifies the <tt>ContactQueryListener</tt>s registered with this
         * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
         * removed.
         *
         * @param contact the <tt>SourceContact</tt> which has been removed and
         * which the registered <tt>ContactQueryListener</tt>s are to be notified
         * about
         */
        public void fireContactRemoved(SourceContact contact)
        {
            super.fireContactRemoved(contact);
        }

        /**
         * Adds a specific <tt>SourceContact</tt> to the list of
         * <tt>SourceContact</tt>s to be returned by this <tt>ContactQuery</tt> in
         * response to {@link #getQueryResults()}.
         *
         * @param sourceContact the <tt>SourceContact</tt> to be added to the
         * <tt>queryResults</tt> of this <tt>ContactQuery</tt>
         * @return <tt>true</tt> if the <tt>queryResults</tt> of this
         * <tt>ContactQuery</tt> has changed in response to the call
         */
        public boolean addQueryResult(SourceContact sourceContact)
        {
            return super.addQueryResult(sourceContact);
        }
    }
}
