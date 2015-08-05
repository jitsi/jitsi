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
package net.java.sip.communicator.impl.msghistory;

import java.beans.*;
import java.io.*;
import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

/**
 * The source contact service. The will show most recent messages.
 *
 * @author Damian Minkov
 */
public class MessageSourceService
    extends MetaContactListAdapter
    implements ContactSourceService,
               ContactPresenceStatusListener,
               ContactCapabilitiesListener,
               ProviderPresenceStatusListener,
               SubscriptionListener,
               LocalUserChatRoomPresenceListener,
               MessageListener,
               ChatRoomMessageListener,
               AdHocChatRoomMessageListener
{
    /**
     * The logger for this class.
     */
    private static Logger logger = Logger.getLogger(MessageSourceService.class);

    /**
     * The display name of this contact source.
     */
    private final String MESSAGE_HISTORY_NAME;

    /**
     * The type of the source service, the place to be shown in the ui.
     */
    private int sourceServiceType = CONTACT_LIST_TYPE;

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
    private final List<ComparableEvtObj> recentMessages
        = new LinkedList<ComparableEvtObj>();

    /**
     * Date of the oldest shown message.
     */
    private Date oldestRecentMessage = null;

    /**
     * The last query created.
     */
    private MessageSourceContactQuery recentQuery = null;

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
            (MessageSourceContactQuery)createContactQuery(
                queryString, numberOfMessages);

        return recentQuery;
    }

    /**
     * Updates the contact sources in the recent query if any.
     * Done here in order to sync with recentMessages instance, and to
     * check for already existing instances of contact sources.
     * Normally called from the query.
     */
    public void updateRecentMessages()
    {
        if(recentQuery == null)
            return;

        synchronized(recentMessages)
        {
            List<SourceContact> currentContactsInQuery
                = recentQuery.getQueryResults();

            for(ComparableEvtObj evtObj : recentMessages)
            {
                // the contains will use the correct equals method of
                // the object evtObj
                if(!currentContactsInQuery.contains(evtObj))
                {
                    MessageSourceContact newSourceContact =
                        new MessageSourceContact(
                            evtObj.getEventObject(),
                            MessageSourceService.this);
                    newSourceContact.initDetails(evtObj.getEventObject());

                    recentQuery.addQueryResult(newSourceContact);
                }
            }
        }
    }

    /**
     * Searches for entries in cached recent messages in history.
     *
     * @param provider the provider which contact messages we will search
     * @param isStatusChanged is the search because of status changed
     * @return entries in cached recent messages in history.
     */
    private List<ComparableEvtObj> getCachedRecentMessages(
        ProtocolProviderService provider, boolean isStatusChanged)
    {
        String providerID = provider.getAccountID().getAccountUniqueID();
        List<String> recentMessagesContactIDs =
            getRecentContactIDs(providerID,
                recentMessages.size() < numberOfMessages
                    ? null : oldestRecentMessage );

        List<ComparableEvtObj> cachedRecentMessages
            = new ArrayList<ComparableEvtObj>();

        for(String contactID : recentMessagesContactIDs)
        {
            Collection<EventObject> res =
                messageHistoryService.findRecentMessagesPerContact(
                    numberOfMessages,
                    providerID,
                    contactID,
                    isSMSEnabled);

            processEventObjects(res, cachedRecentMessages, isStatusChanged);
        }

        return cachedRecentMessages;
    }

    /**
     * Process list of event objects. Checks whether message source contact
     * already exist for this event object, if yes just update it with the new
     * values (not sure whether we should do this, as it may bring old messages)
     * and if status of provider is changed, init its details, updates its
     * capabilities. It still adds the found messages source contact to
     * the list of the new contacts, as later we will detect this and fire
     * update event.
     * If nothing found a new contact is created.
     *
     * @param res list of event
     * @param cachedRecentMessages list of newly created source contacts
     * or already existed but updated with corresponding event object
     * @param isStatusChanged whether provider status changed
     * and we are processing
     */
    private void processEventObjects(
        Collection<EventObject> res,
        List<ComparableEvtObj> cachedRecentMessages,
        boolean isStatusChanged)
    {
        for(EventObject obj : res)
        {
            ComparableEvtObj oldMsg = findRecentMessage(obj, recentMessages);

            if(oldMsg != null)
            {
                oldMsg.update(obj);// update

                if(isStatusChanged && recentQuery != null)
                    recentQuery.updateCapabilities(oldMsg, obj);

                // we still add it to cachedRecentMessages
                // later we will find it is duplicate and will fire
                // update event
                if(!cachedRecentMessages.contains(oldMsg))
                    cachedRecentMessages.add(oldMsg);

                continue;
            }

            oldMsg = findRecentMessage(obj, cachedRecentMessages);

            if(oldMsg == null)
            {
                oldMsg = new ComparableEvtObj(obj);

                if(isStatusChanged && recentQuery != null)
                    recentQuery.updateCapabilities(oldMsg, obj);

                cachedRecentMessages.add(oldMsg);
            }
        }
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
     * Add the ComparableEvtObj, newly added will fire new,
     * for existing fire update and when trimming the list to desired length
     * fire remove for those that were removed
     * @param contactsToAdd
     */
    private void addNewRecentMessages(
        List<ComparableEvtObj> contactsToAdd)
    {
        // now find object to fire new, and object to fire remove
        // let us find duplicates and fire update
        List<ComparableEvtObj> duplicates = new ArrayList<ComparableEvtObj>();
        for(ComparableEvtObj msgToAdd : contactsToAdd)
        {
            if(recentMessages.contains(msgToAdd))
            {
                duplicates.add(msgToAdd);

                // save update
                updateRecentMessageToHistory(msgToAdd);
            }
        }
        recentMessages.removeAll(duplicates);

        // now contacts to add has no duplicates, add them all
        boolean changed = recentMessages.addAll(contactsToAdd);

        if(changed)
        {
            Collections.sort(recentMessages);

            if(recentQuery != null)
            {
                for(ComparableEvtObj obj : duplicates)
                    recentQuery.updateContact(obj, obj.getEventObject());
            }
        }

        if(!recentMessages.isEmpty())
            oldestRecentMessage
                = recentMessages.get(recentMessages.size() - 1).getTimestamp();

        // trim
        List<ComparableEvtObj> removedItems = null;
        if(recentMessages.size() > numberOfMessages)
        {
            removedItems = new ArrayList<ComparableEvtObj>(
                recentMessages.subList(numberOfMessages, recentMessages.size()));

            recentMessages.removeAll(removedItems);
        }

        if(recentQuery != null)
        {
            // now fire, removed for all that were in the list
            // and now are removed after trim
            if(removedItems != null)
            {
                for(ComparableEvtObj msc : removedItems)
                {
                    if(!contactsToAdd.contains(msc))
                        recentQuery.fireContactRemoved(msc);
                }
            }

            // fire new for all that were added, and not removed after trim
            for(ComparableEvtObj msc : contactsToAdd)
            {
                if((removedItems == null
                        || !removedItems.contains(msc))
                    && !duplicates.contains(msc))
                {
                    MessageSourceContact newSourceContact =
                        new MessageSourceContact(
                                msc.getEventObject(),
                                MessageSourceService.this);
                    newSourceContact.initDetails(msc.getEventObject());

                    recentQuery.addQueryResult(newSourceContact);
                }
            }

            // if recent messages were changed, indexes have change lets
            // fire event for the last element which will reorder the whole
            // group if needed.
            if(changed)
                recentQuery.fireContactChanged(
                    recentMessages.get(recentMessages.size() - 1));
        }
    }

    /**
     * When a provider is added, do not block and start executing in new thread.
     *
     * @param provider ProtocolProviderService
     */
    void handleProviderAdded(final ProtocolProviderService provider,
                             final boolean isStatusChanged)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                handleProviderAddedInSeparateThread(provider, isStatusChanged);
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
        ProtocolProviderService provider, boolean isStatusChanged)
    {
        // lets check if we have cached recent messages for this provider, and
        // fire events if found and are newer

        synchronized(recentMessages)
        {
            List<ComparableEvtObj> cachedRecentMessages
                = getCachedRecentMessages(provider, isStatusChanged);

            if(cachedRecentMessages.isEmpty())
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

                List<ComparableEvtObj> newMsc
                    = new ArrayList<ComparableEvtObj>();

                processEventObjects(res, newMsc, isStatusChanged);

                addNewRecentMessages(newMsc);

                for(ComparableEvtObj msc : newMsc)
                {
                    saveRecentMessageToHistory(msc);
                }
            }
            else
                addNewRecentMessages(cachedRecentMessages);
        }
    }

    /**
     * Tries to match the event object to already existing
     * ComparableEvtObj in the supplied list.
     * @param obj the object that we will try to match.
     * @param list the list we will search in.
     * @return the found ComparableEvtObj
     */
    private static ComparableEvtObj findRecentMessage(
        EventObject obj, List<ComparableEvtObj> list)
    {
        Contact contact = null;
        ChatRoom chatRoom = null;

        if(obj instanceof MessageDeliveredEvent)
        {
            contact = ((MessageDeliveredEvent)obj).getDestinationContact();
        }
        else if(obj instanceof MessageReceivedEvent)
        {
            contact = ((MessageReceivedEvent)obj).getSourceContact();
        }
        else if(obj instanceof ChatRoomMessageDeliveredEvent)
        {
            chatRoom = ((ChatRoomMessageDeliveredEvent)obj).getSourceChatRoom();
        }
        else if(obj instanceof ChatRoomMessageReceivedEvent)
        {
            chatRoom = ((ChatRoomMessageReceivedEvent)obj).getSourceChatRoom();
        }

        for(ComparableEvtObj evt : list)
        {
            if((contact != null
                && contact.equals(evt.getContact()))
                || (chatRoom != null
                && chatRoom.equals(evt.getRoom())))
                return evt;
        }

        return null;
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
                List<ComparableEvtObj> removedItems
                    = new ArrayList<ComparableEvtObj>();
                for(ComparableEvtObj msc : recentMessages)
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
                    for(ComparableEvtObj msc : removedItems)
                    {
                        recentQuery.fireContactRemoved(msc);
                    }
                }
            }

            // handleProviderRemoved can be invoked due to stopped
            // history service, if this is the case we do not want to
            // update messages
            if(!this.messageHistoryService.isHistoryLoggingEnabled())
                return;

            // lets do the same as we enable provider
            // for all registered providers and finally fire events
            List<ComparableEvtObj> contactsToAdd
                = new ArrayList<ComparableEvtObj>();
            for (ProtocolProviderService pps
                    : messageHistoryService.getCurrentlyAvailableProviders())
            {
                contactsToAdd.addAll(getCachedRecentMessages(pps, true));
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
                SimpleDateFormat sdf
                    = new SimpleDateFormat(HistoryService.DATE_FORMAT);

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
            for (int i = 0; i < recentMessages.size(); i++)
                if(recentMessages.get(i).equals(messageSourceContact))
                    return i;

            return -1;
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

        recentQuery = new MessageSourceContactQuery(
            MessageSourceService.this);

        return recentQuery;
    }

    /**
     * Updates contact source contacts with status.
     * @param evt the ContactPresenceStatusChangeEvent describing the status
     */
    @Override
    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent evt)
    {
        if(recentQuery == null)
            return;

        synchronized(recentMessages)
        {
            for(ComparableEvtObj msg : recentMessages)
            {
                if(msg.getContact() != null
                    && msg.getContact().equals(evt.getSourceContact()))
                {
                    recentQuery.updateContactStatus(msg, evt.getNewStatus());
                }
            }
        }
    }

    @Override
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
    {
        if(!evt.getNewStatus().isOnline() || evt.getOldStatus().isOnline())
            return;

        handleProviderAdded(evt.getProvider(), true);
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

        ComparableEvtObj srcContact = null;

        synchronized(recentMessages)
        {
            for(ComparableEvtObj msg : recentMessages)
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
            recentQuery.updateContactStatus(
                srcContact,
                ChatRoomPresenceStatus.CHAT_ROOM_ONLINE);
        }
        else if ((LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_LEFT.equals(eventType)
            || LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_KICKED.equals(eventType)
            || LocalUserChatRoomPresenceChangeEvent
            .LOCAL_USER_DROPPED.equals(eventType))
            )
        {
            recentQuery.updateContactStatus(
                srcContact,
                ChatRoomPresenceStatus.CHAT_ROOM_OFFLINE);
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
            ComparableEvtObj existingMsc = null;
            for(ComparableEvtObj msc : recentMessages)
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
                {
                    recentQuery.updateContact(
                        existingMsc, existingMsc.getEventObject());
                    recentQuery.fireContactChanged(existingMsc);
                }

                return;
            }

            // if missing create source contact
            // and update recent messages, trim and sort
            MessageSourceContact newSourceContact =
                new MessageSourceContact(obj, MessageSourceService.this);
            newSourceContact.initDetails(obj);
            // we have already checked for duplicate
            ComparableEvtObj newMsg = new ComparableEvtObj(obj);
            recentMessages.add(newMsg);

            Collections.sort(recentMessages);
            oldestRecentMessage
                = recentMessages.get(recentMessages.size() - 1).getTimestamp();

            // trim
            List<ComparableEvtObj> removedItems = null;
            if(recentMessages.size() > numberOfMessages)
            {
                removedItems = new ArrayList<ComparableEvtObj>(
                    recentMessages.subList(
                        numberOfMessages, recentMessages.size()));

                recentMessages.removeAll(removedItems);
            }

            // save
            saveRecentMessageToHistory(newMsg);

            // no query nothing to fire
            if(recentQuery == null)
                return;

            // now fire
            if(removedItems != null)
            {
                for(ComparableEvtObj msc : removedItems)
                {
                    recentQuery.fireContactRemoved(msc);
                }
            }

            recentQuery.addQueryResult(newSourceContact);
        }
    }

    /**
     * Adds recent message in history.
     */
    private void saveRecentMessageToHistory(ComparableEvtObj msc)
    {
        synchronized(historyID)
        {
            // and create it
            try
            {
                History history = getHistory();
                HistoryWriter writer = history.getWriter();

                SimpleDateFormat sdf
                    = new SimpleDateFormat(HistoryService.DATE_FORMAT);

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
    private void updateRecentMessageToHistory(final ComparableEvtObj msg)
    {
        synchronized(historyID)
        {
            // and create it
            try
            {
                History history = getHistory();

                HistoryWriter writer = history.getWriter();

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
                            for(int i = 0;
                                i < hr.getPropertyNames().length;
                                i++)
                            {
                                String propName = hr.getPropertyNames()[i];

                                if(propName.equals(STRUCTURE_NAMES[0]))
                                {
                                    if(msg.getProtocolProviderService()
                                        .getAccountID().getAccountUniqueID()
                                        .equals(hr.getPropertyValues()[i]))
                                    {
                                        providerFound = true;
                                    }
                                }
                                else if(propName.equals(STRUCTURE_NAMES[1]))
                                {
                                    if(msg.getContactAddress()
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
                                = new SimpleDateFormat(
                                        HistoryService.DATE_FORMAT);

                            for(int i = 0;
                                i < hr.getPropertyNames().length;
                                i++)
                            {
                                String propName = hr.getPropertyNames()[i];

                                if(propName.equals(STRUCTURE_NAMES[0]))
                                {
                                    map.put(
                                        propName,
                                        msg.getProtocolProviderService()
                                            .getAccountID()
                                            .getAccountUniqueID());
                                }
                                else if(propName.equals(STRUCTURE_NAMES[1]))
                                {
                                    map.put(propName, msg.getContactAddress());
                                }
                                else if(propName.equals(STRUCTURE_NAMES[2]))
                                {
                                    map.put(propName,
                                        sdf.format(msg.getTimestamp()));
                                }
                                else if(propName.equals(STRUCTURE_NAMES[3]))
                                    map.put(propName, RECENT_MSGS_VER);
                            }

                            return map;
                        }
                    });
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

    @Override
    public void subscriptionCreated(SubscriptionEvent evt)
    {}

    @Override
    public void subscriptionFailed(SubscriptionEvent evt)
    {}

    @Override
    public void subscriptionRemoved(SubscriptionEvent evt)
    {}

    @Override
    public void subscriptionMoved(SubscriptionMovedEvent evt)
    {}

    @Override
    public void subscriptionResolved(SubscriptionEvent evt)
    {}

    /**
     * If a contact is renamed update the locally stored message if any.
     *
     * @param evt the <tt>ContactPropertyChangeEvent</tt> containing the source
     */
    @Override
    public void contactModified(ContactPropertyChangeEvent evt)
    {
        if(!evt.getPropertyName()
            .equals(ContactPropertyChangeEvent.PROPERTY_DISPLAY_NAME))
            return;

        Contact contact = evt.getSourceContact();

        if(contact == null)
            return;

        for(ComparableEvtObj msc : recentMessages)
        {
            if(contact.equals(msc.getContact()))
            {
                if(recentQuery != null)
                    recentQuery.updateContactDisplayName(
                        msc,
                        contact.getDisplayName());

                return;
            }
        }
    }

    /**
     * Indicates that a MetaContact has been modified.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        for(ComparableEvtObj msc : recentMessages)
        {
            if(evt.getSourceMetaContact().containsContact(msc.getContact()))
            {
                if(recentQuery != null)
                    recentQuery.updateContactDisplayName(
                        msc,
                        evt.getNewDisplayName());
            }
        }
    }

    @Override
    public void supportedOperationSetsChanged(ContactCapabilitiesEvent event)
    {
        Contact contact = event.getSourceContact();

        if(contact == null)
            return;

        for(ComparableEvtObj msc : recentMessages)
        {
            if(contact.equals(msc.getContact()))
            {
                if(recentQuery != null)
                    recentQuery.updateCapabilities(msc, contact);

                return;
            }
        }
    }

    /**
     * Permanently removes all locally stored message history,
     * remove recent contacts.
     */
    public void eraseLocallyStoredHistory()
        throws IOException
    {
        List<ComparableEvtObj> toRemove = null;
        synchronized(recentMessages)
        {
            toRemove = new ArrayList<ComparableEvtObj>(recentMessages);

            recentMessages.clear();
        }

        if(recentQuery != null)
        {
            for(ComparableEvtObj msc : toRemove)
            {
                recentQuery.fireContactRemoved(msc);
            }
        }
    }

    /**
     * Permanently removes locally stored message history for the metacontact,
     * remove any recent contacts if any.
     */
    public void eraseLocallyStoredHistory(MetaContact contact)
        throws IOException
    {
        List<ComparableEvtObj> toRemove = null;
        synchronized(recentMessages)
        {
            toRemove = new ArrayList<ComparableEvtObj>();
            Iterator<Contact> iter = contact.getContacts();
            while(iter.hasNext())
            {
                Contact item = iter.next();
                String id = item.getAddress();
                ProtocolProviderService provider = item.getProtocolProvider();

                for(ComparableEvtObj msc : recentMessages)
                {
                    if(msc.getProtocolProviderService().equals(provider)
                        && msc.getContactAddress().equals(id))
                    {
                        toRemove.add(msc);
                    }
                }
            }

            recentMessages.removeAll(toRemove);
        }
        if(recentQuery != null)
        {
            for(ComparableEvtObj msc : toRemove)
            {
                recentQuery.fireContactRemoved(msc);
            }
        }
    }

    /**
     * Permanently removes locally stored message history for the chatroom,
     * remove any recent contacts if any.
     */
    public void eraseLocallyStoredHistory(ChatRoom room)
    {
        ComparableEvtObj toRemove = null;
        synchronized(recentMessages)
        {
            for(ComparableEvtObj msg : recentMessages)
            {
                if(msg.getRoom() != null
                    && msg.getRoom().equals(room))
                {
                    toRemove = msg;
                    break;
                }
            }

            if(toRemove == null)
                return;

            recentMessages.remove(toRemove);
        }

        if(recentQuery != null)
            recentQuery.fireContactRemoved(toRemove);
    }

    /**
     * Object used to cache recent messages.
     */
    private class ComparableEvtObj
        implements Comparable<ComparableEvtObj>
    {
        private EventObject eventObject;

        /**
         * The protocol provider.
         */
        private ProtocolProviderService ppService = null;

        /**
         * The address.
         */
        private String address = null;

        /**
         * The timestamp.
         */
        private Date timestamp = null;

        /**
         * The contact instance.
         */
        private Contact contact = null;

        /**
         * The room instance.
         */
        private ChatRoom room = null;

        /**
         * Constructs.
         * @param source used to extract initial values.
         */
        ComparableEvtObj(EventObject source)
        {
            update(source);
        }

        /**
         * Extract values from <tt>EventObject</tt>.
         * @param source
         */
        public void update(EventObject source)
        {
            this.eventObject = source;

            if(source instanceof MessageDeliveredEvent)
            {
                MessageDeliveredEvent e = (MessageDeliveredEvent)source;

                this.contact = e.getDestinationContact();

                this.address = contact.getAddress();
                this.ppService = contact.getProtocolProvider();
                this.timestamp = e.getTimestamp();
            }
            else if(source instanceof MessageReceivedEvent)
            {
                MessageReceivedEvent e = (MessageReceivedEvent)source;

                this.contact = e.getSourceContact();

                this.address = contact.getAddress();
                this.ppService = contact.getProtocolProvider();
                this.timestamp = e.getTimestamp();
            }
            else if(source instanceof ChatRoomMessageDeliveredEvent)
            {
                ChatRoomMessageDeliveredEvent e
                    = (ChatRoomMessageDeliveredEvent)source;

                this.room = e.getSourceChatRoom();

                this.address = room.getIdentifier();
                this.ppService = room.getParentProvider();
                this.timestamp = e.getTimestamp();
            }
            else if(source instanceof ChatRoomMessageReceivedEvent)
            {
                ChatRoomMessageReceivedEvent e
                    = (ChatRoomMessageReceivedEvent)source;

                this.room = e.getSourceChatRoom();

                this.address = room.getIdentifier();
                this.ppService = room.getParentProvider();
                this.timestamp = e.getTimestamp();
            }
        }

        @Override
        public String toString()
        {
            return "ComparableEvtObj{" +
                "address='" + address + '\'' +
                ", ppService=" + ppService +
                '}';
        }

        /**
         * The timestamp of the message.
         * @return the timestamp of the message.
         */
        public Date getTimestamp()
        {
            return timestamp;
        }

        /**
         * The contact.
         * @return the contact.
         */
        public Contact getContact()
        {
            return contact;
        }

        /**
         * The room.
         * @return the room.
         */
        public ChatRoom getRoom()
        {
            return room;
        }

        /**
         * The protocol provider.
         * @return the protocol provider.
         */
        public ProtocolProviderService getProtocolProviderService()
        {
            return ppService;
        }

        /**
         * The address.
         * @return the address.
         */
        public String getContactAddress()
        {
            if(this.address != null)
                return this.address;

            return null;
        }

        /**
         * The event object.
         * @return the event object.
         */
        public EventObject getEventObject()
        {
            return eventObject;
        }

        /**
         * Compares two ComparableEvtObj.
         * @param o the object to compare with
         * @return 0, less than zero, greater than zero, if equals,
         * less or greater.
         */
        @Override
        public int compareTo(ComparableEvtObj o)
        {
            if(o == null
                || o.getTimestamp() == null)
                return 1;

            return o.getTimestamp()
                .compareTo(getTimestamp());
        }

        /**
         * Checks if equals, and if this event object is used to create
         * a MessageSourceContact, if the supplied <tt>Object</tt> is instance
         * of MessageSourceContact.
         * @param o the object to check.
         * @return <tt>true</tt> if equals.
         */
        @Override
        public boolean equals(Object o)
        {
            if(this == o)
                return true;
            if(o == null
                || (!(o instanceof MessageSourceContact)
                        && getClass() != o.getClass()))
                return false;

            if(o instanceof ComparableEvtObj)
            {
                ComparableEvtObj that = (ComparableEvtObj) o;

                if(!address.equals(that.address))
                    return false;
                if(!ppService.equals(that.ppService))
                    return false;
            }
            else if(o instanceof MessageSourceContact)
            {
                MessageSourceContact that = (MessageSourceContact)o;

                if(!address.equals(that.getContactAddress()))
                    return false;
                if(!ppService.equals(that.getProtocolProviderService()))
                    return false;
            }
            else
                return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = address.hashCode();
            result = 31 * result + ppService.hashCode();
            return result;
        }
    }
}
