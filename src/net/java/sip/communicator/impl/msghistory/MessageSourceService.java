/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import org.jitsi.util.*;

import java.beans.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

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
     * Number of messages to show.
     */
    private int numberOfMessages = 10;

    /**
     * The structure to save recent messages list.
     */
    private static final String[] STRUCTURE_NAMES
        = new String[] { "provider", "contact"};

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
     * List of recent messages.
     */
    private List<MessageSourceContact> recentMessages = null;

    /**
     * The last query created.
     */
    private MessageHistoryContactQuery recentQuery = null;

    /**
     * Constructs MessageSourceService.
     */
    MessageSourceService()
    {
        if(MessageHistoryActivator.getConfigurationService()
            .getBoolean(IN_HISTORY_PROPERTY , false))
        {
            sourceServiceType = HISTORY_TYPE;
        }

        MESSAGE_HISTORY_NAME = MessageHistoryActivator.getResources()
            .getI18NString("service.gui.RECENT_MESSAGES");

        numberOfMessages = MessageHistoryActivator.getConfigurationService()
            .getInt(NUMBER_OF_RECENT_MSGS_PROP, numberOfMessages);
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

    private List<MessageSourceContact> getRecentMessages()
    {
        if(recentMessages == null)
        {
            // find locally stored list of recent messages
            // time, provider, contact
            List<MessageSourceContact> cachedRecent
                = getRecentMessagesFromHistory();

            if(cachedRecent != null)
            {
                recentMessages = cachedRecent;

                Collections.sort(recentMessages);

                return recentMessages;
            }

            recentMessages = new LinkedList<MessageSourceContact>();

            // If missing search and construct it and save it

            MessageHistoryServiceImpl msgHistoryService =
                MessageHistoryActivator.getMessageHistoryService();
            Collection<EventObject> res = msgHistoryService
                .findRecentMessagesPerContact(numberOfMessages, null, null);

            for(EventObject obj : res)
            {
                recentMessages.add(
                    new MessageSourceContact(obj, MessageSourceService.this));
            }
            Collections.sort(recentMessages);

            // save it
            saveRecentMessagesToHistory();
        }

        return recentMessages;
    }

    /**
     * Loads recent messages if saved in history.
     * @return
     */
    private List<MessageSourceContact> getRecentMessagesFromHistory()
    {
        MessageHistoryServiceImpl msgService
            = MessageHistoryActivator.getMessageHistoryService();
        HistoryService historyService = msgService.getHistoryService();

        // if not existing, return to search for initial load
        if (!historyService.isHistoryCreated(historyID))
            return null;

        List<MessageSourceContact> res
            = new LinkedList<MessageSourceContact>();

        // and load it
        try
        {
            SimpleDateFormat sdf
                = new SimpleDateFormat(HistoryService.DATE_FORMAT);

            History history;
            if (historyService.isHistoryExisting(historyID))
                history = historyService.getHistory(historyID);
            else
                history
                    = historyService.createHistory(historyID, recordStructure);

            Iterator<HistoryRecord> recs
                = history.getReader().findLast(numberOfMessages);
            while(recs.hasNext())
            {
                HistoryRecord hr = recs.next();

                String provider = null;
                String contact = null;

                for (int i = 0; i < hr.getPropertyNames().length; i++)
                {
                    String propName = hr.getPropertyNames()[i];

                    if (propName.equals(STRUCTURE_NAMES[0]))
                        provider = hr.getPropertyValues()[i];
                    else if (propName.equals(STRUCTURE_NAMES[1]))
                        contact = hr.getPropertyValues()[i];
                }

                if(provider == null || contact == null)
                    return res;

                for(EventObject ev : msgService.findRecentMessagesPerContact(
                                        numberOfMessages, provider, contact))
                {
                    res.add(new MessageSourceContact(ev, this));
                }
            }
        }
        catch(IOException ex)
        {
            logger.error("cannot create recent_messages history", ex);
            return null;
        }

        return res;
    }

    /**
     * Saves cached list of recent messages in history.
     */
    private void saveRecentMessagesToHistory()
    {
        HistoryService historyService = MessageHistoryActivator
            .getMessageHistoryService().getHistoryService();

        if (historyService.isHistoryExisting(historyID))
        {
            // delete it
            try
            {
                historyService.purgeLocallyStoredHistory(historyID);
            }
            catch(IOException ex)
            {
                logger.error("Cannot delete recent_messages history", ex);
                return;
            }
        }

        // and create it
        try
        {
            History history = historyService.createHistory(
                historyID, recordStructure);

            HistoryWriter writer = history.getWriter();
            SimpleDateFormat sdf
                = new SimpleDateFormat(HistoryService.DATE_FORMAT);

            List<MessageSourceContact> messages = getRecentMessages();

            synchronized(messages)
            {
                for(MessageSourceContact msc : messages)
                {
                    writer.addRecord(
                        new String[]
                            {
                                msc.getProtocolProviderService()
                                    .getAccountID().getAccountUniqueID(),
                                msc.getContactAddress()
                            });
                }
            }
        }
        catch(IOException ex)
        {
            logger.error("cannot create recent_messages history", ex);
            return;
        }
    }

    /**
     * Returns the index of the source contact, in the list of recent messages.
     * @param messageSourceContact
     * @return
     */
    int getIndex(MessageSourceContact messageSourceContact)
    {
        List<MessageSourceContact> messages = getRecentMessages();

        synchronized(messages)
        {
            return messages.indexOf(messageSourceContact);
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

        List<MessageSourceContact> messages = getRecentMessages();

        synchronized(messages)
        {
            for(MessageSourceContact msgSC : messages)
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
        if(!evt.getNewStatus().isOnline())
            return;

        // now check for chat rooms as we are connected
        MessageHistoryServiceImpl msgHistoryService =
            MessageHistoryActivator.getMessageHistoryService();
        Collection<EventObject> res = msgHistoryService
            .findRecentMessagesPerContact(
                numberOfMessages,
                evt.getProvider().getAccountID().getAccountUniqueID(),
                null);

        List<String> recentMessagesForProvider = new LinkedList<String>();
        List<MessageSourceContact> messages = getRecentMessages();
        synchronized(messages)
        {
            for(MessageSourceContact msc : messages)
            {
                if(msc.getProtocolProviderService().equals(evt.getProvider()))
                    recentMessagesForProvider.add(msc.getContactAddress());
            }

            List<MessageSourceContact> newContactSources
                = new LinkedList<MessageSourceContact>();
            for(EventObject obj : res)
            {
                if(obj instanceof ChatRoomMessageDeliveredEvent
                    || obj instanceof ChatRoomMessageReceivedEvent)
                {
                    MessageSourceContact msc
                        = new MessageSourceContact(obj,
                                                   MessageSourceService.this);

                    if(recentMessagesForProvider
                            .contains(msc.getContactAddress()))
                        continue;

                    messages.add(msc);
                    newContactSources.add(msc);

                }
            }

            // sort
            Collections.sort(messages);

            // and now fire events to update ui
            if(recentQuery != null)
            {
                for(MessageSourceContact msc : newContactSources)
                {
                    recentQuery.addQueryResult(msc);
                }
            }
        }
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

        List<MessageSourceContact> messages = getRecentMessages();

        synchronized(messages)
        {
            for(MessageSourceContact msg : messages)
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
        List<MessageSourceContact> messages = getRecentMessages();
        synchronized(messages)
        {
            for(MessageSourceContact msc : messages)
            {
                if(msc.getProtocolProviderService().equals(provider)
                    && msc.getContactAddress().equals(id))
                {
                    // update
                    msc.update(obj);

                    if(recentQuery != null)
                        recentQuery.fireContactChanged(msc);

                    return;
                }
            }

            // if missing create source contact
            // and update recent messages, trim and sort
            MessageSourceContact newSourceContact =
                new MessageSourceContact(obj, MessageSourceService.this);
            messages.add(newSourceContact);

            Collections.sort(messages);

            // trim
            List<MessageSourceContact> removedItems = null;
            if(messages.size() > numberOfMessages)
            {
                removedItems = new ArrayList<MessageSourceContact>(
                    messages.subList(numberOfMessages, messages.size()));

                messages.removeAll(removedItems);
            }

            // save
            saveRecentMessagesToHistory();

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

    @Override
    public void messageReceived(MessageReceivedEvent evt)
    {
        handle(
            evt,
            evt.getSourceContact().getProtocolProvider(),
            evt.getSourceContact().getAddress());
    }

    @Override
    public void messageDelivered(MessageDeliveredEvent evt)
    {
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
            List<MessageSourceContact> messages = getRecentMessages();
            synchronized(messages)
            {
                for(MessageSourceContact rm : messages)
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
