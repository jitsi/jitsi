/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import java.io.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.event.ProgressEvent;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.msghistory.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The Message History Service stores messages exchanged through the various
 * protocols
 * Logs messages for all protocol providers that support basic instant messaging
 * (i.e. those that implement OperationSetBasicInstantMessaging).
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 * @author Lubomir Marinov
 * @author Valentin Martinet
 */
public class MessageHistoryServiceImpl
    implements  MessageHistoryService,
                MessageListener,
                ChatRoomMessageListener,
                AdHocChatRoomMessageListener,
                ServiceListener,
                LocalUserChatRoomPresenceListener,
                LocalUserAdHocChatRoomPresenceListener
{
    /**
     * The logger for this class.
     */
    private static Logger logger = Logger
            .getLogger(MessageHistoryServiceImpl.class);

    private static String[] STRUCTURE_NAMES
        = new String[] { "dir", "msg_CDATA", "msgTyp", "enc", "uid", "sub",
            "receivedTimestamp" };

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(STRUCTURE_NAMES);

    // the field used to search by keywords
    private static final String SEARCH_FIELD = "msg";

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    private HistoryService historyService = null;

    private Object syncRoot_HistoryService = new Object();

    private Hashtable<MessageHistorySearchProgressListener,
        HistorySearchProgressListener> progressListeners = 
            new Hashtable<MessageHistorySearchProgressListener,
            HistorySearchProgressListener>();

    private ConfigurationService configService;

    private MessageHistoryPropertyChangeListener msgHistoryPropListener;

    private static ResourceManagementService resourcesService;

    /**
     * Returns the history service.
     * @return the history service
     */
    public HistoryService getHistoryService()
    {
        return historyService;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact after the given date
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByStartDate( MetaContact contact,
                                                    Date startDate)
        throws RuntimeException
    {
        TreeSet<EventObject> result = 
            new TreeSet<EventObject>(new MessageEventComparator<EventObject>());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        int recordsCount = countRecords(readers);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact item = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, recordsCount);
            Iterator<HistoryRecord> recs = reader.findByStartDate(startDate);
            while (recs.hasNext())
            {
                result.add(
                    convertHistoryRecordToMessageEvent(recs.next(), item));
            }
        }

        // now remove this listeners
        removeHistorySearchProgressListeners(readers);

        return result;
    }

    private void removeHistorySearchProgressListeners(
        Map<?, HistoryReader> readers)
    {
        for (HistoryReader item : readers.values())
            removeHistorySearchProgressListeners(item);
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact before the given date
     *
     * @param contact MetaContact
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByEndDate(   MetaContact contact,
                                                    Date endDate)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new MessageEventComparator<EventObject>());

        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        int recordsCount = countRecords(readers);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact item = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, recordsCount);
            Iterator<HistoryRecord> recs = reader.findByEndDate(endDate);
            while (recs.hasNext())
            {
                result
                    .add(convertHistoryRecordToMessageEvent(recs.next(), item));
            }
        }

        // now remove this listeners
        removeHistorySearchProgressListeners(readers);

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact between the given dates
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByPeriod(MetaContact contact,
                                                Date startDate,
                                                Date endDate)
        throws RuntimeException
    {
        TreeSet<EventObject> result
            = new TreeSet<EventObject>(new MessageEventComparator<EventObject>());

        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        int recordsCount = countRecords(readers);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact item = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, recordsCount);

            Iterator<HistoryRecord> recs
                = reader.findByPeriod(startDate, endDate);
            while (recs.hasNext())
            {
                result
                    .add(convertHistoryRecordToMessageEvent(recs.next(), item));
            }
        }

        // now remove this listeners
        removeHistorySearchProgressListeners(readers);

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact between the given dates and having the given
     * keywords
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @param keywords array of keywords
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByPeriod(MetaContact contact,
                                       Date startDate, Date endDate,
                                       String[] keywords)
        throws RuntimeException
    {
        return findByPeriod(contact, startDate, endDate, keywords, false);
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keyword
     *
     * @param contact MetaContact
     * @param keyword keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeyword(   MetaContact contact,
                                                    String keyword)
        throws RuntimeException
    {
        return findByKeyword(contact, keyword, false);
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keywords
     *
     * @param contact MetaContact
     * @param keywords keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeywords(  MetaContact contact,
                                                    String[] keywords)
        throws RuntimeException
    {
        return findByKeywords(contact, keywords, false);
    }

    /**
     * Returns the supplied number of recent messages exchanged by all the
     * contacts in the supplied metacontact
     *
     * @param contact MetaContact
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findLast(MetaContact contact, int count)
        throws RuntimeException
    {
        TreeSet<EventObject> result
            = new TreeSet<EventObject>(
                new MessageEventComparator<EventObject>());

        Iterator<Contact> iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();

            try
            {
                History history = this.getHistory(null, item);

                HistoryReader reader = history.getReader();
                Iterator<HistoryRecord> recs = reader.findLast(count);
                while (recs.hasNext())
                {
                    result.add(
                        convertHistoryRecordToMessageEvent(recs.next(),
                            item));

                }
            } catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }

        LinkedList<EventObject> resultAsList
            = new LinkedList<EventObject>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * Returns the supplied number of recent messages after the given date
     * exchanged by all the contacts in the supplied metacontact
     *
     * @param contact MetaContact
     * @param date messages after date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findFirstMessagesAfter(  MetaContact contact,
                                                            Date date,
                                                            int count)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new MessageEventComparator<EventObject>());

        Iterator<Contact> iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();

            try
            {
                History history = this.getHistory(null, item);

                HistoryReader reader = history.getReader();
                // date param of method is the one saved in receivedTimestamp
                // the method findFirstRecordsAfter compares to the
                // attribute timestamp. Most of the times there is 1 or 2 mills
                // difference between the two dates. So we will request more
                // records from the reader and than will get the needed count
                // according to the correct field comparsion (receivedTimestamp)
                Iterator<HistoryRecord> recs
                    = reader.findFirstRecordsAfter(date, count + 4);
                while (recs.hasNext())
                {
                    result.add(
                        convertHistoryRecordToMessageEvent(recs.next(),
                            item));

                }
            } catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }

        // check the dates and skip the starting records which are not ok
        int startIx = 0;
        Iterator<EventObject> i = result.iterator();
        boolean isRecordOK = false;
        while (i.hasNext() && !isRecordOK)
        {
            Object object = i.next();
            if(object instanceof MessageDeliveredEvent)
            {
                isRecordOK =
                    (((MessageDeliveredEvent)object).getTimestamp()
                            > date.getTime());
            }
            else if(object instanceof MessageReceivedEvent)
            {
                isRecordOK =
                    (((MessageReceivedEvent)object).getTimestamp()
                            > date.getTime());
            }

            if(!isRecordOK)
                startIx++;
        }

        LinkedList<EventObject> resultAsList
            = new LinkedList<EventObject>(result);

        int toIndex = startIx + count;
        if(toIndex > resultAsList.size())
            toIndex = resultAsList.size();

        return resultAsList.subList(startIx, toIndex);
    }

    /**
     * Returns the supplied number of recent messages before the given date
     * exchanged by all the contacts in the supplied metacontact
     *
     * @param contact MetaContact
     * @param date messages before date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findLastMessagesBefore(  MetaContact contact,
                                                            Date date,
                                                            int count)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new MessageEventComparator<EventObject>());

        Iterator<Contact> iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();

            try
            {
                History history = this.getHistory(null, item);

                HistoryReader reader = history.getReader();
                Iterator<HistoryRecord> recs
                    = reader.findLastRecordsBefore(date, count);
                while (recs.hasNext())
                {
                    result.add(
                        convertHistoryRecordToMessageEvent(recs.next(),
                            item));

                }
            } catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }

        LinkedList<EventObject> resultAsList
            = new LinkedList<EventObject>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * Returns the history by specified local and remote contact
     * if one of them is null the default is used
     *
     * @param localContact Contact
     * @param remoteContact Contact
     * @return History
     * @throws IOException
     */
    private History getHistory(Contact localContact, Contact remoteContact)
            throws IOException {
        History retVal = null;

        String localId = localContact == null ? "default" : localContact
                .getAddress();
        String remoteId = remoteContact == null ? "default" : remoteContact
                .getAddress();

        String account = "unkown";
        if (remoteContact != null)
            account = remoteContact.getProtocolProvider()
                .getAccountID().getAccountUniqueID();

        HistoryID historyId = HistoryID.createFromRawID(
            new String[] {  "messages",
                            localId,
                            account,
                            remoteId });

        // if this history doesn't exists check to see if old one still exists
        // old one is not storing history per account
        // if old one exists its converted/moved to the new one
        // the new one is in format messages/profile_name/account_uid/contact
        // the old one was messages/profile_name/contact
        if(!this.historyService.isHistoryCreated(historyId))
        {
            HistoryID historyId_old = HistoryID.createFromRawID(
            new String[] {  "messages",
                            localId,
                            remoteId });

            if(this.historyService.isHistoryCreated(historyId_old))
            {
                try
                {
                    this.historyService.moveHistory(historyId_old, historyId);
                }
                catch (IOException iOException)
                {
                    // something is wrong just use the old one
                    historyId = historyId_old;
                }
            }
        }

        if (this.historyService.isHistoryExisting(historyId))
        {
            retVal = this.historyService.getHistory(historyId);
        }
        else
        {
            retVal = this.historyService.createHistory(historyId,
                recordStructure);
        }

        return retVal;
    }

    /**
     * Returns the history by specified local contact
     * (if is null the default is used)
     * and by the chat room
     *
     * @param room The chat room
     * @return History the history - created if not existing
     * @throws IOException
     */
    private History getHistoryForMultiChat(
                        ChatRoom room)
        throws IOException
    {
        AccountID account = room.getParentProvider().getAccountID();

        return this.getHistoryForMultiChat(
            null,
            account.getAccountUniqueID(),
            account.getService(),
            room.getName());
    }
    
    /**
     * Returns the history by specified local contact
     * (if is null the default is used)
     * and by the ad-hoc chat room
     *
     * @param room The ad-hoc chat room
     * @return History the history - created if not existing
     * @throws IOException
     */
    private History getHistoryForAdHocMultiChat(
                        AdHocChatRoom room)
        throws IOException
    {
        AccountID account = room.getParentProvider().getAccountID();

        return this.getHistoryForMultiChat(
            null,
            account.getAccountUniqueID(),
            account.getService(),
            room.getName());
    }

    /**
     * Returns the history by specified local contact
     * (if is null the default is used)
     * and by accountUniqueID, channel and server
     * used by the multichat account.
     *
     * @param localContact Contact
     * @param account The account UniqueID
     * @param server the server used by the account
     * @param channel the channel history we are searching for
     * @return History the history - created if not existing
     * @throws IOException
     */
    private History getHistoryForMultiChat(
                        Contact localContact,
                        String account,
                        String server,
                        String channel)
        throws IOException
    {
        History retVal = null;

        String localId = localContact == null ? "default" : localContact
                .getAddress();

        HistoryID historyId = HistoryID.createFromRawID(
            new String[] { "messages",
                            localId,
                            account,
                            channel + "@" + server });

        if (this.historyService.isHistoryExisting(historyId))
        {
            retVal = this.historyService.getHistory(historyId);
        } else {
            retVal = this.historyService.createHistory(historyId,
                    recordStructure);
        }

        return retVal;
    }

    /**
     * Used to convert HistoryRecord in MessageDeliveredEvent or
     * MessageReceivedEvent which are returned by the finder methods
     *
     * @param hr HistoryRecord
     * @param contact Contact
     * @return Object
     */
    private EventObject convertHistoryRecordToMessageEvent( HistoryRecord hr,
                                                            Contact contact)
    {
        MessageImpl msg = createMessageFromHistoryRecord(hr);
        long timestamp;

        // if there is value for date of receiving the message
        // this is the event timestamp (this is the date that had came
        // from protocol)
        // the HistoryRecord timestamp is the timestamp when the record
        // was written
        long messageReceivedDate = msg.getMessageReceivedDate();
        long hrTimestamp = hr.getTimeInMillis();
        if (messageReceivedDate != 0)
        {
            if(messageReceivedDate - hrTimestamp > 86400000) // 24*60*60*1000
                timestamp = hrTimestamp;
            else
                timestamp = msg.getMessageReceivedDate();
        }
        else
            timestamp = hrTimestamp;

        if(msg.isOutgoing)
        {
            return new MessageDeliveredEvent(
                    msg,
                    contact,
                    timestamp);
        }
        else
            return new MessageReceivedEvent(
                        msg,
                        contact,
                        timestamp);
    }

    /**
     * Used to convert HistoryRecord in ChatRoomMessageDeliveredEvent or
     * ChatRoomMessageReceivedEvent
     * which are returned by the finder methods
     *
     * @param hr HistoryRecord
     * @param room the chat room
     * @return Object
     */
    private EventObject convertHistoryRecordToMessageEvent(
        HistoryRecord hr, ChatRoom room)
    {
        MessageImpl msg = createMessageFromHistoryRecord(hr);
        long timestamp;

        // if there is value for date of receiving the message
        // this is the event timestamp (this is the date that had came
        // from protocol)
        // the HistoryRecord timestamp is the timestamp when the record
        // was written
        long messageReceivedDate = msg.getMessageReceivedDate();
        long hrTimestamp = hr.getTimeInMillis();
        if(messageReceivedDate != 0)
        {
            if(messageReceivedDate - hrTimestamp > 86400000) // 24*60*60*1000
                timestamp = hrTimestamp;
            else
                timestamp = msg.getMessageReceivedDate();
        }
        else
            timestamp = hrTimestamp;

        // 5 is the index of the subject in the structure
        String fromStr = hr.getPropertyValues()[5];

        ChatRoomMember from = new ChatRoomMemberImpl(fromStr, room, null);

        if(msg.isOutgoing)
        {
            return new ChatRoomMessageDeliveredEvent(
                    room,
                    timestamp,
                    msg,
                    ChatRoomMessageDeliveredEvent
                        .CONVERSATION_MESSAGE_DELIVERED);
        }
        else
            return new ChatRoomMessageReceivedEvent(
                        room,
                        from,
                        timestamp,
                        msg,
                        ChatRoomMessageReceivedEvent
                            .CONVERSATION_MESSAGE_RECEIVED);
    }

    private MessageImpl createMessageFromHistoryRecord(HistoryRecord hr)
    {
        // History structure
        // 0 - dir
        // 1 - msg_CDATA
        // 2 - msgTyp
        // 3 - enc
        // 4- uid
        // 5 - sub
        // 6 - receivedTimestamp
        String textContent = null;
        String contentType = null;
        String contentEncoding = null;
        String messageUID = null;
        String subject = null;
        boolean isOutgoing = false;
        long messageReceivedDate = 0;
        for (int i = 0; i < hr.getPropertyNames().length; i++)
        {
            String propName = hr.getPropertyNames()[i];

            if (propName.equals("msg") || propName.equals(STRUCTURE_NAMES[1]))
                textContent = hr.getPropertyValues()[i];
            else if (propName.equals(STRUCTURE_NAMES[2]))
                contentType = hr.getPropertyValues()[i];
            else if (propName.equals(STRUCTURE_NAMES[3]))
                contentEncoding = hr.getPropertyValues()[i];
            else if (propName.equals(STRUCTURE_NAMES[4]))
                messageUID = hr.getPropertyValues()[i];
            else if (propName.equals(STRUCTURE_NAMES[5]))
                subject = hr.getPropertyValues()[i];
            else if (propName.equals(STRUCTURE_NAMES[0]))
            {
                if (hr.getPropertyValues()[i].equals("in"))
                    isOutgoing = false;
                else if (hr.getPropertyValues()[i].equals("out"))
                    isOutgoing = true;
            }
            else if (propName.equals(STRUCTURE_NAMES[6]))
            {
                messageReceivedDate =
                    Long.parseLong(hr.getPropertyValues()[i]);
            }
        }
        return new MessageImpl(textContent, contentType, contentEncoding,
            subject, messageUID, isOutgoing, messageReceivedDate);
    }

    /**
     * Starts the service. Check the current registered protocol providers
     * which supports BasicIM and adds message listener to them
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        this.bundleContext = bc;

        ServiceReference refConfig = bundleContext.getServiceReference(
            ConfigurationService.class.getName());

        configService = (ConfigurationService)
            bundleContext.getService(refConfig);

        // Check if the message history is enabled in the configuration
        // service, and if not do not register the service.
        String isMessageHistoryEnabledPropertyString =
            "impl.msghistory.IS_MESSAGE_HISTORY_ENABLED";

        String isMessageHistoryEnabledString = configService.getString(
            isMessageHistoryEnabledPropertyString);

        if(isMessageHistoryEnabledString == null)
            isMessageHistoryEnabledString =
                getResources().
                getSettingsString(isMessageHistoryEnabledPropertyString);

        // If the property doesn't exist we stop here.
        if (isMessageHistoryEnabledString == null
            || isMessageHistoryEnabledString.length() == 0)
            return;

        boolean isMessageHistoryEnabled
             = new Boolean(isMessageHistoryEnabledString).booleanValue();

        // If the message history is not enabled we stop here.
        if (!isMessageHistoryEnabled)
            return;

        // We're adding a property change listener in order to
        // listen for modifications of the isMessageHistoryEnabled property.
        msgHistoryPropListener = new MessageHistoryPropertyChangeListener();

        configService.addPropertyChangeListener(
            "impl.msghistory.IS_MESSAGE_HISTORY_ENABLED",
            msgHistoryPropListener);

        logger.debug("Starting the msg history implementation.");

        this.loadMessageHistoryService();
    }

    /**
     * Stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        if (configService != null)
            configService.removePropertyChangeListener(msgHistoryPropListener);

        stopMessageHistoryService();
    }

    // //////////////////////////////////////////////////////////////////////////
    // MessageListener implementation methods

    public void messageReceived(MessageReceivedEvent evt)
    {
        this.writeMessage("in", null, evt.getSourceContact(), evt
                .getSourceMessage(), evt.getTimestamp());
    }

    public void messageDelivered(MessageDeliveredEvent evt)
    {
        this.writeMessage("out", null, evt.getDestinationContact(), evt
                .getSourceMessage(), evt.getTimestamp());
    }

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
    }

    // //////////////////////////////////////////////////////////////////////////
    // ChatRoomMessageListener implementation methods

    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        try
        {
            // ignore non conversation messages
            if(evt.getEventType() !=
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED)
                return;

            History history = this.getHistoryForMultiChat(
                evt.getSourceChatRoom());

            writeMessage(history, "in", evt.getSourceChatRoomMember(),
                evt.getMessage(), evt.getTimestamp());
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        try
        {
            History history = this.
                getHistoryForMultiChat(
                evt.getSourceChatRoom());

            writeMessage(history, "out", evt.getMessage(), evt.getTimestamp());
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {
    }

    /**
     * Writes message to the history
     * @param direction String direction of the message
     * @param source The source Contact
     * @param destination The destiantion Contact
     * @param message Message message to be written
     * @param messageTimestamp Date this is the timestamp when was message received
     *                          that came from the protocol provider
     */
    private void writeMessage(String direction, Contact source,
            Contact destination, Message message, long messageTimestamp)
    {
        try {
            History history = this.getHistory(source, destination);

            writeMessage(history, direction, message, messageTimestamp);
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    /**
     * Writes message to the history
     * @param history The history to which will write the message
     * @param message Message
     * @param messageTimestamp Date this is the timestamp when was message received
     *                          that came from the protocol provider
     */
    private void writeMessage(History history, String direction,
            Message message, long messageTimestamp)
    {
        try {
            HistoryWriter historyWriter = history.getWriter();
            historyWriter.addRecord(new String[] { direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    message.getSubject(), String.valueOf(messageTimestamp) },
                    new Date()); // this date is when the history record is written
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    /**
     * Writes message to the history
     * @param history The history to which will write the message
     * @param message Message
     * @param messageTimestamp Date this is the timestamp when was message received
     *                          that came from the protocol provider
     */
    private void writeMessage(History history, String direction,
            ChatRoomMember from,
            Message message, long messageTimestamp)
    {
        try {
            // mising from, strange messages, most probably a history
            // coming from server and probably already written
            if(from == null)
                return;

            HistoryWriter historyWriter = history.getWriter();
            historyWriter.addRecord(new String[] { direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    from.getContactAddress(),
                    String.valueOf(messageTimestamp) },
                    new Date()); // this date is when the history record is written
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }
    
    /**
     * Writes a message to the history.
     * @param history The history to which will write the message
     * @param message Message
     * @param messageTimestamp Date this is the timestamp when was message received
     *                          that came from the protocol provider
     */
    private void writeMessage(History history, String direction,
            Contact from,
            Message message, long messageTimestamp)
    {
        try
        {
            HistoryWriter historyWriter = history.getWriter();
            historyWriter.addRecord(new String[] { direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    from.getAddress(),
                    String.valueOf(messageTimestamp) },
                    new Date()); // this date is when the history record is written
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    // //////////////////////////////////////////////////////////////////////////

    /**
     * Set the configuration service.
     *
     * @param historyService HistoryService
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void setHistoryService(HistoryService historyService)
            throws IllegalArgumentException, IOException {
        synchronized (this.syncRoot_HistoryService)
        {
            this.historyService = historyService;

            logger.debug("New history service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param historyService HistoryService
     */
    public void unsetHistoryService(HistoryService historyService)
    {
        synchronized (this.syncRoot_HistoryService)
        {
            if (this.historyService == historyService)
            {
                this.historyService = null;

                logger.debug("History service unregistered.");
            }
        }
    }

    /**
     * When new protocol provider is registered we check
     * does it supports BasicIM and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService = bundleContext.getService(serviceEvent.getServiceReference());

        logger.trace("Received a service event for: " + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            logger.debug("Handling registration of a new Protocol Provider.");

            this.handleProviderAdded((ProtocolProviderService)sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved( (ProtocolProviderService) sService);
        }

    }

    /**
     * Used to attach the Message History Service to existing or
     * just registered protocol provider. Checks if the provider has implementation
     * of OperationSetBasicInstantMessaging
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        logger.debug("Adding protocol provider "
                + provider.getProtocolDisplayName());

        // check whether the provider has a basic im operation set
        OperationSetBasicInstantMessaging opSetIm =
            provider.getOperationSet(OperationSetBasicInstantMessaging.class);

        if (opSetIm != null)
        {
            opSetIm.addMessageListener(this);
        }
        else
        {
            logger.trace("Service did not have a im op. set.");
        }

        OperationSetMultiUserChat opSetMultiUChat =
            provider.getOperationSet(OperationSetMultiUserChat.class);

        if (opSetMultiUChat != null)
        {
            Iterator<ChatRoom> iter =
                opSetMultiUChat.getCurrentlyJoinedChatRooms().iterator();

            while(iter.hasNext())
            {
                ChatRoom room = iter.next();
                room.addMessageListener(this);
            }

            opSetMultiUChat.addPresenceListener(this);
        }
        else
        {
            logger.trace("Service did not have a multi im op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the messages exchanged by it
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm =
            provider.getOperationSet(OperationSetBasicInstantMessaging.class);

        if (opSetIm != null)
        {
            opSetIm.removeMessageListener(this);
        }

        OperationSetMultiUserChat opSetMultiUChat =
            provider.getOperationSet(OperationSetMultiUserChat.class);

        if (opSetMultiUChat != null)
        {
            Iterator<ChatRoom> iter =
                opSetMultiUChat.getCurrentlyJoinedChatRooms().iterator();

            while(iter.hasNext())
            {
                ChatRoom room = iter.next();
                room.removeMessageListener(this);
            }
        }
    }

    /**
     * Called to notify interested parties that a change in our presence in
     * a chat room has occured. Changes may include us being kicked, join,
     * left.
     * @param evt the <tt>LocalUserChatRoomPresenceChangeEvent</tt> instance
     * containing the chat room and the type, and reason of the change
     */
    public void localUserPresenceChanged(LocalUserChatRoomPresenceChangeEvent evt)
    {
        if(evt.getEventType() ==
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED)
        {
            if (!evt.getChatRoom().isSystem())
                evt.getChatRoom().addMessageListener(this);
        }
        else
        {
            evt.getChatRoom().removeMessageListener(this);
        }
    }

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
    public void addSearchProgressListener(MessageHistorySearchProgressListener
                                          listener)
    {
        synchronized(progressListeners){
            HistorySearchProgressListener wrapperListener =
                new SearchProgressWrapper(listener);
            progressListeners.put(listener, wrapperListener);
        }
    }

    /**
     * Removing progress listener
     *
     * @param listener HistorySearchProgressListener
     */
    public void removeSearchProgressListener(
        MessageHistorySearchProgressListener listener)
    {
        synchronized(progressListeners){
            progressListeners.remove(listener);
        }
    }

    /**
     * Add the registered MessageHistorySearchProgressListeners
     * to the given HistoryReader
     *
     * @param reader HistoryReader
     * @param countRecords number of records will search
     */
    private void addHistorySearchProgressListeners(
        HistoryReader reader, int countRecords)
    {
        synchronized(progressListeners)
        {
            Iterator<HistorySearchProgressListener> iter = progressListeners.values().iterator();
            while (iter.hasNext())
            {
                SearchProgressWrapper l =
                    (SearchProgressWrapper) iter.next();
                l.setCurrentValues(reader, countRecords);
                reader.addSearchProgressListener(l);
            }
        }
    }

    /**
     * Removes the registered MessageHistorySearchProgressListeners
     * from the given HistoryReader
     *
     * @param reader HistoryReader
     */
    private void removeHistorySearchProgressListeners(HistoryReader reader)
    {
        synchronized(progressListeners)
        {
            Iterator<HistorySearchProgressListener> iter = progressListeners.values().iterator();
            while (iter.hasNext())
            {
                SearchProgressWrapper l =
                    (SearchProgressWrapper) iter.next();
                l.clear();
                reader.removeSearchProgressListener(l);
            }
        }
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact between the given dates and having the given
     * keywords
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @param keywords array of keywords
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByPeriod(MetaContact contact, Date startDate,
                                   Date endDate, String[] keywords,
                                   boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(new MessageEventComparator<EventObject>());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        int recordsCount = countRecords(readers);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact item = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, recordsCount);
            Iterator<HistoryRecord> recs = reader.findByPeriod(startDate, endDate, keywords,
                                                SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent(recs.next(), item));
            }
        }

        // now remove this listeners
        removeHistorySearchProgressListeners(readers);

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keyword
     *
     * @param contact MetaContact
     * @param keyword keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeyword(MetaContact contact, String keyword,
                                    boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(new MessageEventComparator<EventObject>());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        int recordsCount = countRecords(readers);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact item = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, recordsCount);
            Iterator<HistoryRecord> recs = reader.
                findByKeyword(keyword, SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent(recs.next(), item));
            }
        }

        // now remove this listeners
        removeHistorySearchProgressListeners(readers);

        return result;
    }

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keywords
     *
     * @param contact MetaContact
     * @param keywords keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeywords(MetaContact contact, String[] keywords,
                                     boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(new MessageEventComparator<EventObject>());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        int recordsCount = countRecords(readers);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact item = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, recordsCount);
            Iterator<HistoryRecord> recs = reader.
                findByKeywords(keywords, SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent(recs.next(), item));
            }
        }

        // now remove this listeners
        removeHistorySearchProgressListeners(readers);

        return result;
    }

    /**
     * Gets all the history readers for the contacts in the given MetaContact
     * @param contact MetaContact
     * @return Hashtable
     */
    private Map<Contact, HistoryReader> getHistoryReaders(MetaContact contact)
    {
        Map<Contact, HistoryReader> readers = new Hashtable<Contact, HistoryReader>();
        Iterator<Contact> iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();

            try
            {
                History history = this.getHistory(null, item);
                readers.put(item, history.getReader());
            }
            catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }
        return readers;
    }

    /**
     * Total count of records for supplied history readers will read through
     *
     * @param readers hashtable with pairs contact <-> history reader
     * @return the number of searched messages
     * @throws UnsupportedOperationException
     *              Thrown if an exception occurs during the execution of the
     *              query, such as internal IO error.
     */
    public int countRecords(Map<?, HistoryReader> readers)
    {
        int result = 0;

        for (HistoryReader r : readers.values())
            result += r.countRecords();
        return result;
    }

    /**
     * Returns all the messages exchanged in the supplied
     * chat room after the given date
     *
     * @param room The chat room
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByStartDate(ChatRoom room, Date startDate)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(new ChatRoomMessageEventComparator<EventObject>());
        try
        {
            // get the readers for this room
            HistoryReader reader =
                this.getHistoryForMultiChat(room).getReader();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, 1);

            Iterator<HistoryRecord> recs = reader.findByStartDate(startDate);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent(recs.next(), room));
            }

            removeHistorySearchProgressListeners(reader);
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged
     * in the supplied chat room before the given date
     *
     * @param room The chat room
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByEndDate(ChatRoom room, Date endDate)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(new ChatRoomMessageEventComparator<EventObject>());
        try
        {
            // get the readers for this room
            HistoryReader reader =
                this.getHistoryForMultiChat(room).getReader();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, 1);

            Iterator<HistoryRecord> recs = reader.findByEndDate(endDate);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent(recs.next(), room));
            }

            removeHistorySearchProgressListeners(reader);
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged
     * in the supplied chat room between the given dates
     *
     * @param room The chat room
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByPeriod(ChatRoom room, Date startDate, Date endDate)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(new ChatRoomMessageEventComparator<EventObject>());
        try
        {
            // get the readers for this room
            HistoryReader reader =
                this.getHistoryForMultiChat(room).getReader();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, 1);

            Iterator<HistoryRecord> recs = reader.findByPeriod(startDate, endDate);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent(recs.next(), room));
            }

            removeHistorySearchProgressListeners(reader);
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged
     * in the supplied chat room between the given dates and having the given
     * keywords
     *
     * @param room The chat room
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @param keywords array of keywords
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByPeriod(ChatRoom room,
            Date startDate, Date endDate, String[] keywords)
        throws RuntimeException
    {
        return findByPeriod(room, startDate, endDate, keywords, false);
    }

    /**
     * Returns all the messages exchanged
     * in the supplied chat room between the given dates and having the given
     * keywords
     *
     * @param room The chat room
     * @param startDate Date the start date of the conversations
     * @param endDate Date the end date of the conversations
     * @param keywords array of keywords
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByPeriod(ChatRoom room, Date startDate,
            Date endDate, String[] keywords, boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new ChatRoomMessageEventComparator<EventObject>());
        try
        {
            // get the readers for this room
            HistoryReader reader =
                this.getHistoryForMultiChat(room).getReader();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, 1);

            Iterator<HistoryRecord> recs
                = reader.findByPeriod(startDate, endDate, keywords,
                                        SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(convertHistoryRecordToMessageEvent(recs.next(), room));
            }

            removeHistorySearchProgressListeners(reader);
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged
     * in the supplied room having the given keyword
     *
     * @param room The Chat room
     * @param keyword keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeyword(ChatRoom room, String keyword)
        throws RuntimeException
    {
        return findByKeyword(room, keyword, false);
    }

    /**
     * Returns all the messages exchanged
     * in the supplied chat room having the given keyword
     *
     * @param room The chat room
     * @param keyword keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeyword(ChatRoom room, String keyword,
            boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new ChatRoomMessageEventComparator<EventObject>());
        try
        {
            // get the readers for this room
            HistoryReader reader =
                this.getHistoryForMultiChat(room).getReader();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, 1);

            Iterator<HistoryRecord> recs = reader.
                findByKeyword(keyword, SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result
                    .add(convertHistoryRecordToMessageEvent(recs.next(), room));
            }

            removeHistorySearchProgressListeners(reader);
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        return result;
    }

    /**
     * Returns all the messages exchanged
     * in the supplied chat room having the given keywords
     *
     * @param room The chat room
     * @param keywords keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeywords(  ChatRoom room,
                                                    String[] keywords)
        throws RuntimeException
    {
        return findByKeywords(room, keywords, false);
    }

    /**
     * Returns all the messages exchanged
     * in the supplied chat room having the given keywords
     *
     * @param room The chat room
     * @param keywords keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeywords(  ChatRoom room,
                                                    String[] keywords,
                                                    boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new ChatRoomMessageEventComparator<EventObject>());
        try
        {
            // get the readers for this room
            HistoryReader reader =
                this.getHistoryForMultiChat(room).getReader();

            // add the progress listeners
            addHistorySearchProgressListeners(reader, 1);

            Iterator<HistoryRecord> recs = reader.
                findByKeywords(keywords, SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result
                    .add(convertHistoryRecordToMessageEvent(recs.next(), room));
            }

            removeHistorySearchProgressListeners(reader);
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        return result;
    }

    /**
     * Returns the supplied number of recent messages exchanged
     * in the supplied chat room
     *
     * @param room The chat room
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findLast(ChatRoom room, int count)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new ChatRoomMessageEventComparator<EventObject>());

        try
        {
            // get the readers for this room
            HistoryReader reader =
                this.getHistoryForMultiChat(room).getReader();
            Iterator<HistoryRecord> recs = reader.findLast(count);
            while (recs.hasNext())
            {
                result.add(
                    convertHistoryRecordToMessageEvent(recs.next(),
                        room));

            }
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        LinkedList<EventObject> resultAsList
        = new LinkedList<EventObject>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * Returns the supplied number of recent messages after the given date
     * exchanged in the supplied chat room
     *
     * @param room The chat room
     * @param date messages after date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findFirstMessagesAfter(  ChatRoom room,
                                                            Date date,
                                                            int count)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new ChatRoomMessageEventComparator<EventObject>());

        try
        {
            HistoryReader reader
                = this.getHistoryForMultiChat(room).getReader();
            Iterator<HistoryRecord> recs
                = reader.findFirstRecordsAfter(date, count);
            while (recs.hasNext())
            {
                result.add(
                    convertHistoryRecordToMessageEvent(recs.next(),
                        room));

            }
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        LinkedList<EventObject> resultAsList
            = new LinkedList<EventObject>(result);

        int toIndex = count;
        if(toIndex > resultAsList.size())
            toIndex = resultAsList.size();

        return resultAsList.subList(0, toIndex);
    }

    /**
     * Returns the supplied number of recent messages before the given date
     * exchanged in the supplied chat room
     *
     * @param room The chat room
     * @param date messages before date
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findLastMessagesBefore(  ChatRoom room,
                                                            Date date,
                                                            int count)
        throws RuntimeException
    {
        TreeSet<EventObject> result = new TreeSet<EventObject>(
            new ChatRoomMessageEventComparator<EventObject>());

        try
        {
            HistoryReader reader =
                this.getHistoryForMultiChat(room).getReader();
            Iterator<HistoryRecord> recs
                = reader.findLastRecordsBefore(date, count);
            while (recs.hasNext())
            {
                result.add(
                    convertHistoryRecordToMessageEvent(recs.next(),
                        room));

            }
        } catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        LinkedList<EventObject> resultAsList
            = new LinkedList<EventObject>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference
                = MessageHistoryActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService)
                MessageHistoryActivator.bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }

    /**
     * A wrapper around HistorySearchProgressListener
     * that fires events for MessageHistorySearchProgressListener
     */
    private class SearchProgressWrapper
        implements HistorySearchProgressListener
    {
        private MessageHistorySearchProgressListener listener = null;
        int allRecords = 0;
        HistoryReader reader = null;
        double currentReaderProgressRatio = 0;
        double accumulatedRatio = 0;
        double currentProgress = 0;
        double lastHistoryProgress = 0;

        // used for more precise calculations with double values
        int raiser = 1000;

        SearchProgressWrapper(MessageHistorySearchProgressListener listener)
        {
            this.listener = listener;
        }

        private void setCurrentValues(  HistoryReader currentReader,
                                        int allRecords)
        {
            this.allRecords = allRecords;
            this.reader = currentReader;
            currentReaderProgressRatio =
                    (double)currentReader.countRecords()/allRecords * raiser;
            accumulatedRatio += currentReaderProgressRatio;
        }

        public void progressChanged(ProgressEvent evt)
        {
            int progress = getProgressMapping(evt);
            currentProgress = progress;

            listener.progressChanged(
                new net.java.sip.communicator.service.msghistory.event.
                    ProgressEvent(MessageHistoryServiceImpl.this,
                    evt, progress/raiser));
        }

        /**
         * Calculates the progress according the count of the records
         * we will search
         * @param historyProgress int
         * @return int
         */
        private int getProgressMapping(ProgressEvent evt)
        {
            double tmpHistoryProgress =
                    currentReaderProgressRatio * evt.getProgress();

            currentProgress += tmpHistoryProgress - lastHistoryProgress;

            if(evt.getProgress()
                    == HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE)
            {
                lastHistoryProgress = 0;

                // this is the last one and the last event fire the max
                // there will be looses in currentProgress due to the devision
                if((int)accumulatedRatio == raiser)
                    currentProgress = raiser *
                        MessageHistorySearchProgressListener
                            .PROGRESS_MAXIMUM_VALUE;
            }
            else
                lastHistoryProgress = tmpHistoryProgress;

            return (int)currentProgress;
        }

        /**
         * clear the values
         */
        void clear()
        {
            allRecords = 0;
            currentProgress = 0;
            lastHistoryProgress = 0;
        }
    }

    /**
     * Simple message implementation.
     */
    private static class MessageImpl
        extends AbstractMessage
    {
        private final boolean isOutgoing;

        private final long messageReceivedDate;

        MessageImpl(String content, String contentType, String encoding,
            String subject, String messageUID, boolean isOutgoing,
            long messageReceivedDate)
        {
            super(content, contentType, encoding, subject, messageUID);

            this.isOutgoing = isOutgoing;
            this.messageReceivedDate = messageReceivedDate;
        }

        public long getMessageReceivedDate()
        {
            return messageReceivedDate;
        }
    }

    /**
     * Used to compare MessageDeliveredEvent or MessageReceivedEvent
     * and to be ordered in TreeSet according their timestamp
     */
    private static class MessageEventComparator<T>
        implements Comparator<T>
    {
        public int compare(T o1, T o2)
        {
            long date1;
            long date2;

            if(o1 instanceof MessageDeliveredEvent)
                date1 = ((MessageDeliveredEvent)o1).getTimestamp();
            else if(o1 instanceof MessageReceivedEvent)
                date1 = ((MessageReceivedEvent)o1).getTimestamp();
            else
                return 0;

            if(o2 instanceof MessageDeliveredEvent)
                date2 = ((MessageDeliveredEvent)o2).getTimestamp();
            else if(o2 instanceof MessageReceivedEvent)
                date2 = ((MessageReceivedEvent)o2).getTimestamp();
            else
                return 0;

            return (date1 < date2) ? -1 : ((date1 == date2) ? 0 : 1);
        }
    }

    /**
     * Used to compare ChatRoomMessageDeliveredEvent
     * or ChatRoomMessageReceivedEvent
     * and to be ordered in TreeSet according their timestamp
     */
    private static class ChatRoomMessageEventComparator<T>
        implements Comparator<T>
    {
        public int compare(T o1, T o2)
        {
            long date1;
            long date2;

            if(o1 instanceof ChatRoomMessageDeliveredEvent)
                date1 = ((ChatRoomMessageDeliveredEvent)o1).getTimestamp();
            else if(o1 instanceof ChatRoomMessageReceivedEvent)
                date1 = ((ChatRoomMessageReceivedEvent)o1).getTimestamp();
            else
                return 0;

            if(o2 instanceof ChatRoomMessageDeliveredEvent)
                date2 = ((ChatRoomMessageDeliveredEvent)o2).getTimestamp();
            else if(o2 instanceof ChatRoomMessageReceivedEvent)
                date2 = ((ChatRoomMessageReceivedEvent)o2).getTimestamp();
            else
                return 0;

            return (date1 < date2) ? -1 : ((date1 == date2) ? 0 : 1);
        }
    }

    /**
     * Simple ChatRoomMember implementation.
     */
    static class ChatRoomMemberImpl
        implements ChatRoomMember
    {
        private final ChatRoom chatRoom;
        private final String name;
        private ChatRoomMemberRole role;

        public ChatRoomMemberImpl(String name, ChatRoom chatRoom,
            ChatRoomMemberRole role)
        {
            this.chatRoom = chatRoom;
            this.name =  name;
            this.role = role;
        }

        public ChatRoom getChatRoom()
        {
            return chatRoom;
        }

        public ProtocolProviderService getProtocolProvider()
        {
            return chatRoom.getParentProvider();
        }

        public String getContactAddress()
        {
            return name;
        }

        public String getName()
        {
            return name;
        }

        public ChatRoomMemberRole getRole()
        {
            return role;
        }

        public byte[] getAvatar()
        {
            return null;
        }

        public void setRole(ChatRoomMemberRole newRole)
        {
            this.role = newRole;
        }

        public Contact getContact()
        {
            return null;
        }
    }

    /**
     * Handles <tt>PropertyChangeEvent</tt> triggered from the modification of
     * the isMessageHistoryEnabled property.
     */
    private class MessageHistoryPropertyChangeListener
        implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt)
        {
            String newPropertyValue = (String) evt.getNewValue();

            boolean isMessageHistoryEnabled
                = new Boolean(newPropertyValue).booleanValue();

            // If the message history is not enabled we stop here.
            if (isMessageHistoryEnabled)
                loadMessageHistoryService();
            else
                stop(bundleContext);
        }
    }


    /**
     * Loads the History and MessageHistoryService. Registers the service in the
     * bundle context.
     */
    private void loadMessageHistoryService()
    {
        // start listening for newly register or removed protocol providers
        bundleContext.addServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bundleContext.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            logger.debug("Found "
                         + protocolProviderRefs.length
                         + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) bundleContext
                        .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * Stops the MessageHistoryService.
     */
    private void stopMessageHistoryService()
    {
        // start listening for newly register or removed protocol providers
        bundleContext.removeServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bundleContext.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) bundleContext
                        .getService(protocolProviderRefs[i]);

                this.handleProviderRemoved(provider);
            }
        }
    }

    public void messageDelivered(AdHocChatRoomMessageDeliveredEvent evt) 
    {
        try
        {
            History history = this.
            getHistoryForAdHocMultiChat(
                    evt.getSourceAdHocChatRoom());

            writeMessage(history, "out", evt.getMessage(), evt.getTimestamp());
        }
        catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    public void messageDeliveryFailed(
            AdHocChatRoomMessageDeliveryFailedEvent evt)
    {
        // TODO Auto-generated method stub
    }

    public void messageReceived(AdHocChatRoomMessageReceivedEvent evt)
    {
         try
            {
                History history = this.getHistoryForAdHocMultiChat(
                    evt.getSourceChatRoom());

                writeMessage(history, "in", evt.getSourceChatRoomParticipant(),
                    evt.getMessage(), evt.getTimestamp());
            } catch (IOException e)
            {
                logger.error("Could not add message to history", e);
            }
    }

    /**
     * Called to notify interested parties that a change in our presence in
     * an ad-hoc chat room has occurred. Changes may include us being join,
     * left.
     * @param evt the <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt> 
     * instance containing the ad-hoc chat room and the type, and reason of the 
     * change
     */
    public void localUserAdHocPresenceChanged(
            LocalUserAdHocChatRoomPresenceChangeEvent evt)
    {
        if(evt.getEventType()
            == LocalUserAdHocChatRoomPresenceChangeEvent.LOCAL_USER_JOINED)
        {
            evt.getAdHocChatRoom().addMessageListener(this);
        }
        else
        {
            evt.getAdHocChatRoom().removeMessageListener(this);
        }
    }
}
