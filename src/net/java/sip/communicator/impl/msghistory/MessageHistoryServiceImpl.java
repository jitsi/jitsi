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

import static net.java.sip.communicator.service.history.HistoryService.*;

import java.beans.*;
import java.io.*;
import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.event.ProgressEvent;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.msghistory.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

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
                MessageHistoryAdvancedService,
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

    static String[] STRUCTURE_NAMES
        = new String[] { "dir", "msg_CDATA", "msgTyp", "enc", "uid", "sub",
            "receivedTimestamp", "msgSubTyp" };

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(STRUCTURE_NAMES);

    /**
     * the field used to search by keywords
     */
    private static final String SEARCH_FIELD = "msg";

    /**
     * Subtype sms to mark sms messages.
     */
    static final String MSG_SUBTYPE_SMS = "sms";

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

    /**
     * Indicates if history logging is enabled.
     */
    private static boolean isHistoryLoggingEnabled;

    /**
     * The message source service, can be null if not enabled.
     */
    private MessageSourceService messageSourceService;

    /**
     * The message source service registration.
     */
    private ServiceRegistration messageSourceServiceReg = null;

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
        HashSet<EventObject> result = new HashSet<EventObject>();

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
        HashSet<EventObject> result = new HashSet<EventObject>();

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
        HashSet<EventObject> result = new HashSet<EventObject>();

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
        LinkedList<EventObject> result = new LinkedList<EventObject>();

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
                        convertHistoryRecordToMessageEvent(recs.next(), item));

                }
            }
            catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }

        Collections.sort(result, new MessageEventComparator<EventObject>());
        int startIndex = result.size() - count;
        if(startIndex < 0)
            startIndex = 0;

        return result.subList(startIndex, result.size());
    }

    /**
     * Checks whether this historyID contains messages of certain type.
     * @param historyID
     * @param keywords
     * @param field
     * @param caseSensitive
     * @return
     * @throws IOException
     */
    private boolean hasMessages(HistoryID historyID,
                                String[] keywords,
                                String field,
                                boolean caseSensitive)
        throws IOException
    {
        if(!this.historyService.isHistoryCreated(historyID))
            return false;

        History history = this.historyService.createHistory(historyID,
                recordStructure);

        return history.getReader().findLast(
            1, keywords, field, caseSensitive).hasNext();
    }

    /**
     * Returns the messages for the recently contacted <tt>count</tt> contacts.
     *
     * @param count contacts count
     * @param providerToFilter can be filtered by provider, or <tt>null</tt> to
     * search for all providers
     * @param contactToFilter can be filtered by contac, or <tt>null</tt> to
     * search for all contacts
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    Collection<EventObject> findRecentMessagesPerContact(
            int count, String providerToFilter, String contactToFilter,
            boolean isSMSEnabled)
        throws RuntimeException
    {
        HashSet<EventObject> result = new HashSet<EventObject>();

        List<HistoryID> historyIDs=
            this.historyService.getExistingHistories(
                new String[]{"messages", "default"});

        // make the filter provider string to reflect those that were
        // used when creating folders
        String providerFilterStr = null;
        if(providerToFilter != null)
            providerFilterStr = HistoryID.readableHash(providerToFilter);

        for(HistoryID id : historyIDs)
        {
            if(result.size() >= count)
                break;

            try
            {
                // this history id is: "messages", localId, account, remoteId
                if(id.getID().length != 4)
                    continue;

                // filter by protocol provider
                if(providerFilterStr != null
                    && !id.getID()[2].startsWith(providerFilterStr))
                {
                    continue;
                }

                if(contactToFilter != null
                    && !id.getID()[3].startsWith(contactToFilter))
                {
                    continue;
                }

                // find contact or chatroom for historyID
                Object descriptor = getContactOrRoomByID(
                    providerToFilter,
                    id.getID()[3],
                    id,
                    isSMSEnabled);

                // skip not found contacts, disabled accounts and hidden one
                if(descriptor == null)
                    continue;

                History history = this.historyService.createHistory(id,
                        recordStructure);

                HistoryReader reader = history.getReader();

                // find last by type
                Iterator<HistoryRecord> recs;

                if(isSMSEnabled)
                {
                    recs = reader.findLast(
                        1,
                        new String[]{MessageHistoryServiceImpl.MSG_SUBTYPE_SMS},
                        MessageHistoryServiceImpl.STRUCTURE_NAMES[7],
                        true);
                }
                else
                {
                    recs = reader.findLast(1);
                }

                while (recs.hasNext())
                {
                    if(descriptor instanceof Contact)
                    {
                        EventObject o = convertHistoryRecordToMessageEvent(
                            recs.next(), (Contact) descriptor);

                        result.add(o);
                    }
                    if(descriptor instanceof ChatRoom)
                    {
                        EventObject o = convertHistoryRecordToMessageEvent(
                            recs.next(), (ChatRoom) descriptor);

                        result.add(o);
                    }
                    break;
                }
            }
            catch(IOException ex)
            {
                logger.error("Could not read history", ex);
            }
        }

        return result;
    }

    /**
     * Founds the contact or chat room corresponding this HistoryID. Checks the
     * account and then searches for the contact or chat room.
     * Will skip hidden and disabled accounts.
     *
     * @param accountID the account id.
     * @param id the contact or room id.
     * @return contact or chat room.
     */
    private Object getContactOrRoomByID(String accountID,
                                        String id,
                                        HistoryID historyID,
                                        boolean isSMSEnabled)
        throws IOException
    {
        AccountID account = null;
        for(AccountID acc : AccountUtils.getStoredAccounts())
        {
            if( !acc.isHidden()
                && acc.isEnabled()
                && accountID.startsWith(acc.getAccountUniqueID()))
            {
                account = acc;
                break;
            }
        }

        if(account == null)
            return null;

        ProtocolProviderService pps =
            AccountUtils.getRegisteredProviderForAccount(account);

        if(pps == null)
            return null;

        OperationSetPersistentPresence opSetPresence =
            pps.getOperationSet(OperationSetPersistentPresence.class);

        if(opSetPresence == null)
            return null;

        Contact contact = opSetPresence.findContactByID(id);

        if(isSMSEnabled)
        {
            //lets check if we have a contact and it has sms messages return it
            if(contact != null
                && hasMessages(
                        historyID,
                        new String[]{MessageHistoryServiceImpl.MSG_SUBTYPE_SMS},
                        MessageHistoryServiceImpl.STRUCTURE_NAMES[7],
                        true))
            {
                return contact;
            }

            // we will check only for sms contacts
            OperationSetSmsMessaging opSetSMS =
                pps.getOperationSet(OperationSetSmsMessaging.class);

            // return the contact only if it has stored sms messages
            if(opSetSMS == null
                || !hasMessages(
                        historyID,
                        new String[]{MessageHistoryServiceImpl.MSG_SUBTYPE_SMS},
                        MessageHistoryServiceImpl.STRUCTURE_NAMES[7],
                        true))
            {
                return null;
            }

            return opSetSMS.getContact(id);
        }

        if(contact != null)
            return contact;

        OperationSetMultiUserChat opSetMuc =
            pps.getOperationSet(OperationSetMultiUserChat.class);

        if(opSetMuc == null)
            return null;

        try
        {
            // will remove the server part
            id = id.substring(0, id.lastIndexOf('@'));

            return opSetMuc.findRoom(id);
        }
        catch(Exception e)
        {
            //logger.error("Cannot find room", e);
            return null;
        }
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
        LinkedList<EventObject> result = new LinkedList<EventObject>();

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
            }
            catch (IOException e)
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
                    (((MessageDeliveredEvent)object).getTimestamp().getTime()
                            > date.getTime());
            }
            else if(object instanceof MessageReceivedEvent)
            {
                isRecordOK =
                    (((MessageReceivedEvent)object).getTimestamp().getTime()
                            > date.getTime());
            }

            if(!isRecordOK)
                startIx++;
        }

        Collections.sort(result, new MessageEventComparator<EventObject>());
        int toIndex = startIx + count;
        if(toIndex > result.size())
            toIndex = result.size();

        return result.subList(startIx, toIndex);
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
        LinkedList<EventObject> result = new LinkedList<EventObject>();

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
            }
            catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }

        Collections.sort(result, new MessageEventComparator<EventObject>());
        int startIndex = result.size() - count;
        if(startIndex < 0)
            startIndex = 0;

        return result.subList(startIndex, result.size());
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

        return this.historyService.createHistory(historyId, recordStructure);
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
        String localId = localContact == null ? "default" : localContact
                .getAddress();

        HistoryID historyId = HistoryID.createFromRawID(
            new String[] { "messages",
                            localId,
                            account,
                            channel + "@" + server });

        return this.historyService.createHistory(historyId, recordStructure);
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
        Date timestamp;

        // if there is value for date of receiving the message
        // this is the event timestamp (this is the date that had came
        // from protocol)
        // the HistoryRecord timestamp is the timestamp when the record
        // was written
        Date messageReceivedDate = msg.getMessageReceivedDate();
        Date hrTimestamp = hr.getTimestamp();
        if (messageReceivedDate.getTime() != 0)
        {
            // 24*60*60*1000
            if(messageReceivedDate.getTime() - hrTimestamp.getTime() > 86400000)
                timestamp = hrTimestamp;
            else
                timestamp = msg.getMessageReceivedDate();
        }
        else
            timestamp = hrTimestamp;

        if(msg.isOutgoing)
        {
            MessageDeliveredEvent evt
                = new MessageDeliveredEvent(
                        msg,
                        contact,
                        timestamp);

            if(msg.getMsgSubType() != null
                && msg.getMsgSubType().equals(MSG_SUBTYPE_SMS))
            {
                evt.setSmsMessage(true);
            }

            return evt;
        }
        else
        {
            int eventType = MessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED;

            if(msg.getMsgSubType() != null
                && msg.getMsgSubType().equals(MSG_SUBTYPE_SMS))
            {
                eventType = MessageReceivedEvent.SMS_MESSAGE_RECEIVED;
            }

            return new MessageReceivedEvent(
                        msg,
                        contact,
                        timestamp,
                        eventType);
        }
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
        Date timestamp;

        // if there is value for date of receiving the message
        // this is the event timestamp (this is the date that had came
        // from protocol)
        // the HistoryRecord timestamp is the timestamp when the record
        // was written
        Date messageReceivedDate = msg.getMessageReceivedDate();
        Date hrTimestamp = hr.getTimestamp();
        if(messageReceivedDate.getTime() != 0)
        {
            // 24*60*60*1000
            if(messageReceivedDate.getTime() - hrTimestamp.getTime() > 86400000)
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
        // 7 - msgSubType
        String textContent = null;
        String contentType = null;
        String contentEncoding = null;
        String messageUID = null;
        String subject = null;
        boolean isOutgoing = false;
        Date messageReceivedDate = new Date(0);
        String msgSubType = null;
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
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
                try
                {
                    messageReceivedDate = sdf.parse(hr.getPropertyValues()[i]);
                }
                catch (ParseException e)
                {
                    messageReceivedDate =
                        new Date(Long.parseLong(hr.getPropertyValues()[i]));
                }
            }
            else if (propName.equals(STRUCTURE_NAMES[7]))
            {
                msgSubType = hr.getPropertyValues()[i];
            }
        }
        return new MessageImpl(textContent, contentType, contentEncoding,
            subject, messageUID, isOutgoing, messageReceivedDate, msgSubType);
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
        boolean isMessageHistoryEnabled = configService.getBoolean(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED,
            Boolean.parseBoolean(
                MessageHistoryActivator.getResources().getSettingsString(
                MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED))
            );

        // We're adding a property change listener in order to
        // listen for modifications of the isMessageHistoryEnabled property.
        msgHistoryPropListener = new MessageHistoryPropertyChangeListener();
        
        // Load the "IS_MESSAGE_HISTORY_ENABLED" property.
        isHistoryLoggingEnabled = configService.getBoolean(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED,
            Boolean.parseBoolean(UtilActivator
                .getResources().getSettingsString(
                    MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED))
            );

        configService.addPropertyChangeListener(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED,
            msgHistoryPropListener);

        if (isMessageHistoryEnabled)
        {
            if (logger.isDebugEnabled())
                logger.debug("Starting the msg history implementation.");

            this.loadMessageHistoryService();
        }
    }

    /**
     * Loads and registers the contact source service.
     */
    private void loadRecentMessages()
    {
        this.messageSourceService = new MessageSourceService(this);
        messageSourceServiceReg = bundleContext.registerService(
            ContactSourceService.class.getName(),
            messageSourceService, null);
        MessageHistoryActivator.getContactListService()
            .addMetaContactListListener(this.messageSourceService);
    }

    /**
     * Unloads the contact source service.
     */
    private void stopRecentMessages()
    {
        if(messageSourceServiceReg != null)
        {
            MessageHistoryActivator.getContactListService()
                .removeMetaContactListListener(this.messageSourceService);

            messageSourceServiceReg.unregister();
            messageSourceServiceReg = null;

            this.messageSourceService = null;
        }
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
        this.writeMessage(
            "in",
            null,
            evt.getSourceContact(),
            evt.getSourceMessage(),
            evt.getTimestamp(),
            evt.getEventType() == MessageReceivedEvent.SMS_MESSAGE_RECEIVED);
    }

    public void messageDelivered(MessageDeliveredEvent evt)
    {
        this.writeMessage(
            "out",
            null,
            evt.getDestinationContact(),
            evt.getSourceMessage(),
            evt.getTimestamp(),
            evt.isSmsMessage());
    }

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
    }

    // //////////////////////////////////////////////////////////////////////////
    // ChatRoomMessageListener implementation methods

    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        if(!isHistoryLoggingEnabled(
                evt.getSourceChatRoom().getIdentifier()))
        {
            // logging is switched off for this particular chat room
            return;
        }

        try
        {
            // ignore non conversation messages
            if(evt.getEventType() !=
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED)
                return;

            History history = this.getHistoryForMultiChat(
                evt.getSourceChatRoom());

            // if this is chat room message history on every room enter
            // we can receive the same latest history messages and this
            // will just fill the history on every join
            if(evt.isHistoryMessage())
            {
                Collection<EventObject> c =
                    findFirstMessagesAfter(evt.getSourceChatRoom(),
                        new Date(evt.getTimestamp().getTime() - 10000),
                        20);

                Iterator<EventObject> iter = c.iterator();
                boolean isPresent = false;
                while(iter.hasNext())
                {
                    EventObject e = iter.next();

                    if(e instanceof ChatRoomMessageReceivedEvent)
                    {
                        ChatRoomMessageReceivedEvent cev =
                            (ChatRoomMessageReceivedEvent)e;

                        if( evt.getSourceChatRoomMember().getContactAddress()
                                != null
                            && evt.getSourceChatRoomMember().getContactAddress()
                                .equals(cev.getSourceChatRoomMember()
                                                .getContactAddress())
                            && evt.getTimestamp() != null
                            && evt.getTimestamp().equals(cev.getTimestamp()))
                        {
                            isPresent = true;
                            break;
                        }

                        // also check and message content
                        Message m1 = cev.getMessage();
                        Message m2 = evt.getMessage();

                        if(m1 != null && m2 != null
                           && m1.getContent().equals(m2.getContent()))
                        {
                            isPresent = true;
                            break;
                        }
                    }
                }

                if (isPresent)
                    return;
            }

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
            if(!isHistoryLoggingEnabled(
                    evt.getSourceChatRoom().getIdentifier()))
            {
                // logging is switched off for this particular chat room
                return;
            }

            History history = this.getHistoryForMultiChat(
                evt.getSourceChatRoom());

            // if this is chat room message history on every room enter
            // we can receive the same latest history messages and this
            // will just fill the history on every join
            if(evt.isHistoryMessage())
            {
                Collection<EventObject> c =
                    findFirstMessagesAfter(evt.getSourceChatRoom(),
                        new Date(evt.getTimestamp().getTime() - 10000),
                        20);

                Iterator<EventObject> iter = c.iterator();
                boolean isPresent = false;
                while(iter.hasNext())
                {
                    EventObject e = iter.next();
                    if(e instanceof  ChatRoomMessageDeliveredEvent)
                    {
                        ChatRoomMessageDeliveredEvent cev =
                            (ChatRoomMessageDeliveredEvent)e;

                        if(evt.getTimestamp() != null
                            && evt.getTimestamp().equals(cev.getTimestamp()))
                        {
                            isPresent = true;
                            break;
                        }

                        // also check and message content
                        Message m1 = cev.getMessage();
                        Message m2 = evt.getMessage();

                        if(m1 != null && m2 != null
                            && m1.getContent().equals(m2.getContent()))
                        {
                            isPresent = true;
                            break;
                        }
                    }
                }

                if (isPresent)
                    return;
            }

            writeMessage(
                history, "out", evt.getMessage(), evt.getTimestamp(), false);
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
     * @param destination The destination Contact
     * @param message Message message to be written
     * @param messageTimestamp Date this is the timestamp when was message
     * received that came from the protocol provider
     * @param isSmsSubtype whether message to write is an sms
     */
    private void writeMessage(
        String direction,
        Contact source,
        Contact destination,
        Message message,
        Date messageTimestamp,
        boolean isSmsSubtype)
    {
        try
        {
            MetaContact metaContact = MessageHistoryActivator
                .getContactListService().findMetaContactByContact(destination);
            if(metaContact != null
                && !isHistoryLoggingEnabled(
                        metaContact.getMetaUID()))
            {
                // logging is switched off for this particular contact
                return;
            }

            History history = this.getHistory(source, destination);

            writeMessage(
                history, direction, message, messageTimestamp, isSmsSubtype);
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    /**
     * Writes message to the history
     * @param history The history to which will write the message
     * @param direction coming from
     * @param message Message
     * @param messageTimestamp Date this is the timestamp when was message
     * received that came from the protocol provider
     */
    private void writeMessage(History history, String direction,
            Message message, Date messageTimestamp, boolean isSmsSubtype)
    {
        try {
            HistoryWriter historyWriter = history.getWriter();
            SimpleDateFormat sdf
                = new SimpleDateFormat(HistoryService.DATE_FORMAT);
            historyWriter.addRecord(new String[] { direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    message.getSubject(), sdf.format(messageTimestamp),
                    isSmsSubtype ? MSG_SUBTYPE_SMS : null},
                    new Date()); // this date is when the history record is written
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    /**
     * Writes message to the history
     * @param history The history to which will write the message
     * @param direction the direction of the message.
     * @param from coming from
     * @param message Message
     * @param messageTimestamp Date this is the timestamp when was message received
     *                          that came from the protocol provider
     */
    private void writeMessage(History history, String direction,
            ChatRoomMember from,
            Message message, Date messageTimestamp)
    {
        try {
            // mising from, strange messages, most probably a history
            // coming from server and probably already written
            if(from == null)
                return;

            HistoryWriter historyWriter = history.getWriter();
            SimpleDateFormat sdf
                = new SimpleDateFormat(HistoryService.DATE_FORMAT);
            historyWriter.addRecord(new String[] { direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    from.getContactAddress(),
                    sdf.format(messageTimestamp),
                    null},
                    new Date()); // this date is when the history record is written
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    /**
     * Writes a message to the history.
     * @param history The history to which will write the message
     * @param direction the direction of the message.
     * @param from coming from
     * @param message Message
     * @param messageTimestamp Date this is the timestamp when was message received
     *                          that came from the protocol provider
     */
    private void writeMessage(History history, String direction,
            Contact from,
            Message message, Date messageTimestamp)
    {
        try
        {
            HistoryWriter historyWriter = history.getWriter();
            SimpleDateFormat sdf
                = new SimpleDateFormat(HistoryService.DATE_FORMAT);
            historyWriter.addRecord(new String[] { direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    from.getAddress(),
                    sdf.format(messageTimestamp),
                    null},
                    new Date()); // this date is when the history record is written
        } catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    /**
     * Inserts message to the history. Allows to update the laready saved
     * history.
     * @param direction String direction of the message in or out.
     * @param source The source Contact
     * @param destination The destination Contact
     * @param message Message message to be written
     * @param messageTimestamp Date this is the timestamp when was message
     * received that came from the protocol provider
     * @param isSmsSubtype whether message to write is an sms
     */
    public void insertMessage(
        String direction,
        Contact source,
        Contact destination,
        Message message,
        Date messageTimestamp,
        boolean isSmsSubtype)
    {
        try
        {
            MetaContact metaContact = MessageHistoryActivator
                .getContactListService().findMetaContactByContact(destination);
            if(metaContact != null
                && !isHistoryLoggingEnabled(
                metaContact.getMetaUID()))
            {
                // logging is switched off for this particular contact
                return;
            }

            History history = this.getHistory(source, destination);

            HistoryWriter historyWriter = history.getWriter();
            SimpleDateFormat sdf
                = new SimpleDateFormat(HistoryService.DATE_FORMAT);
            historyWriter.insertRecord(new String[]{direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    message.getSubject(), sdf.format(messageTimestamp),
                    isSmsSubtype ? MSG_SUBTYPE_SMS : null},
                messageTimestamp,
                STRUCTURE_NAMES[6]);
                // this date is when the history record to be written
                // as we are inserting

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

            if (logger.isDebugEnabled())
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

                if (logger.isDebugEnabled())
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

        if (logger.isTraceEnabled())
            logger.trace("Received a service event for: " + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        if (logger.isDebugEnabled())
            logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            if (logger.isDebugEnabled())
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
        if (logger.isDebugEnabled())
            logger.debug("Adding protocol provider "
                + provider.getProtocolDisplayName());

        // check whether the provider has a basic im operation set
        OperationSetBasicInstantMessaging opSetIm =
            provider.getOperationSet(OperationSetBasicInstantMessaging.class);

        if (opSetIm != null)
        {
            opSetIm.addMessageListener(this);

            if(this.messageSourceService != null)
                opSetIm.addMessageListener(messageSourceService);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a im op. set.");
        }

        OperationSetSmsMessaging opSetSMS =
            provider.getOperationSet(OperationSetSmsMessaging.class);

        if (opSetSMS != null)
        {
            opSetSMS.addMessageListener(this);

            if(this.messageSourceService != null)
                opSetSMS.addMessageListener(messageSourceService);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a sms op. set.");
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

            if(messageSourceService != null)
                opSetMultiUChat.addPresenceListener(messageSourceService);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a multi im op. set.");
        }

        if(messageSourceService != null)
        {
            OperationSetPresence opSetPresence =
                provider.getOperationSet(OperationSetPresence.class);

            if (opSetPresence != null)
            {
                opSetPresence
                    .addContactPresenceStatusListener(messageSourceService);
                opSetPresence
                    .addProviderPresenceStatusListener(messageSourceService);
                opSetPresence.addSubscriptionListener(messageSourceService);
            }

            messageSourceService.handleProviderAdded(provider, false);

            OperationSetContactCapabilities capOpSet
                = provider.getOperationSet(OperationSetContactCapabilities.class);

            if(capOpSet != null)
            {
                capOpSet.addContactCapabilitiesListener(messageSourceService);
            }
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

            if(this.messageSourceService != null)
                opSetIm.removeMessageListener(messageSourceService);
        }

        OperationSetSmsMessaging opSetSMS =
            provider.getOperationSet(OperationSetSmsMessaging.class);

        if (opSetSMS != null)
        {
            opSetSMS.removeMessageListener(this);

            if(this.messageSourceService != null)
                opSetSMS.removeMessageListener(messageSourceService);
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

            opSetMultiUChat.removePresenceListener(this);

            if(messageSourceService != null)
                opSetMultiUChat.removePresenceListener(messageSourceService);
        }

        if(messageSourceService != null)
        {
            OperationSetPresence opSetPresence =
                provider.getOperationSet(OperationSetPresence.class);

            if(opSetPresence != null)
            {
                opSetPresence
                    .removeContactPresenceStatusListener(messageSourceService);
                opSetPresence
                    .removeProviderPresenceStatusListener(messageSourceService);
                opSetPresence.removeSubscriptionListener(messageSourceService);
            }

            messageSourceService.handleProviderRemoved(provider);

            OperationSetContactCapabilities capOpSet
                = provider.getOperationSet(OperationSetContactCapabilities.class);

            if(capOpSet != null)
            {
                capOpSet.removeContactCapabilitiesListener(messageSourceService);
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
            {
                evt.getChatRoom().addMessageListener(this);

                if(this.messageSourceService != null)
                    evt.getChatRoom().addMessageListener(messageSourceService);
            }
        }
        else
        {
            evt.getChatRoom().removeMessageListener(this);

            if(this.messageSourceService != null)
                evt.getChatRoom().removeMessageListener(messageSourceService);
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        HashSet<EventObject> result = new HashSet<EventObject>();
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
        LinkedList<EventObject> result = new LinkedList<EventObject>();

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
        }
        catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        Collections.sort(result,
            new ChatRoomMessageEventComparator<EventObject>());
        int startIndex = result.size() - count;
        if(startIndex < 0)
            startIndex = 0;

        return result.subList(startIndex, result.size());
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
        LinkedList<EventObject> result = new LinkedList<EventObject>();

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
        }
        catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        Collections.sort(result,
            new ChatRoomMessageEventComparator<EventObject>());
        int toIndex = count;
        if(toIndex > result.size())
            toIndex = result.size();

        return result.subList(0, toIndex);
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
        LinkedList<EventObject> result = new LinkedList<EventObject>();

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
        }
        catch (IOException e)
        {
            logger.error("Could not read history", e);
        }

        Collections.sort(result,
            new ChatRoomMessageEventComparator<EventObject>());
        int startIndex = result.size() - count;
        if(startIndex < 0)
            startIndex = 0;

        return result.subList(startIndex, result.size());
    }

    /**
     * A wrapper around HistorySearchProgressListener
     * that fires events for MessageHistorySearchProgressListener
     */
    private class SearchProgressWrapper
        implements HistorySearchProgressListener
    {
        private MessageHistorySearchProgressListener listener = null;
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
         * @param evt the progress event
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

        private final Date messageReceivedDate;

        private String msgSubType;

        MessageImpl(String content, String contentType, String encoding,
            String subject, String messageUID, boolean isOutgoing,
            Date messageReceivedDate, String msgSubType)
        {
            super(content, contentType, encoding, subject, messageUID);

            this.isOutgoing = isOutgoing;
            this.messageReceivedDate = messageReceivedDate;
            this.msgSubType = msgSubType;
        }

        public Date getMessageReceivedDate()
        {
            return messageReceivedDate;
        }

        public String getMsgSubType()
        {
            return msgSubType;
        }
    }

    /**
     * Used to compare MessageDeliveredEvent or MessageReceivedEvent
     * and to be ordered in TreeSet according their timestamp
     */
    private static class MessageEventComparator<T>
        implements Comparator<T>
    {
        private final boolean reverseOrder;

        MessageEventComparator(boolean reverseOrder)
        {
            this.reverseOrder = reverseOrder;
        }

        MessageEventComparator()
        {
            this(false);
        }

        public int compare(T o1, T o2)
        {
            Date date1;
            Date date2;

            if(o1 instanceof MessageDeliveredEvent)
                date1 = ((MessageDeliveredEvent)o1).getTimestamp();
            else if(o1 instanceof MessageReceivedEvent)
                date1 = ((MessageReceivedEvent)o1).getTimestamp();
            else if(o1 instanceof ChatRoomMessageDeliveredEvent)
                date1 = ((ChatRoomMessageDeliveredEvent)o1).getTimestamp();
            else if(o1 instanceof ChatRoomMessageReceivedEvent)
                date1 = ((ChatRoomMessageReceivedEvent)o1).getTimestamp();
            else
                return 0;

            if(o2 instanceof MessageDeliveredEvent)
                date2 = ((MessageDeliveredEvent)o2).getTimestamp();
            else if(o2 instanceof MessageReceivedEvent)
                date2 = ((MessageReceivedEvent)o2).getTimestamp();
            else if(o2 instanceof ChatRoomMessageDeliveredEvent)
                date2 = ((ChatRoomMessageDeliveredEvent)o2).getTimestamp();
            else if(o2 instanceof ChatRoomMessageReceivedEvent)
                date2 = ((ChatRoomMessageReceivedEvent)o2).getTimestamp();
            else
                return 0;

            if(reverseOrder)
                return date2.compareTo(date1);
            else
                return date1.compareTo(date2);
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
            Date date1;
            Date date2;

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

            return date1.compareTo(date2);
        }
    }

    /**
     * Simple ChatRoomMember implementation.
     * Searches and for contact matches, to use its display name.
     */
    static class ChatRoomMemberImpl
        implements ChatRoomMember
    {
        private final ChatRoom chatRoom;
        private final String name;
        private ChatRoomMemberRole role;

        private Contact contact = null;

        private OperationSetPersistentPresence opsetPresence = null;

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
            String name = this.name;
            if(getContact() != null
                && getContact().getDisplayName() != null)
            {
                name = getContact().getDisplayName();
            }

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
            if(contact == null && opsetPresence == null)
            {
                opsetPresence = getProtocolProvider().getOperationSet(
                    OperationSetPersistentPresence.class);

                if(opsetPresence != null)
                {
                    contact
                        = opsetPresence.findContactByID(getContactAddress());
                }
            }

            return contact;
        }

        public ConferenceDescription getConferenceDescription()
        {
            return null;
        }

        public void setConferenceDescription(ConferenceDescription cd)
        {
            return;
        }

        @Override
        public PresenceStatus getPresenceStatus()
        {
            // FIXME is this correct response?
            return GlobalStatusEnum.ONLINE;
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
            if(evt.getPropertyName()
                .equals(MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED))
            {
                String newPropertyValue = (String) evt.getNewValue();
                isHistoryLoggingEnabled
                    = new Boolean(newPropertyValue).booleanValue();

                // If the message history is not enabled we stop here.
                if (isHistoryLoggingEnabled)
                    loadMessageHistoryService();
                else
                    stop(bundleContext);
            }
            else if(evt.getPropertyName().equals(
                MessageHistoryService.PNAME_IS_RECENT_MESSAGES_DISABLED))
            {
                String newPropertyValue = (String) evt.getNewValue();
                boolean isDisabled
                    = new Boolean(newPropertyValue).booleanValue();

                if(isDisabled)
                {
                    stopRecentMessages();
                }
                else if(isHistoryLoggingEnabled)
                {
                    loadRecentMessages();
                }
            }
        }
    }


    /**
     * Loads the History and MessageHistoryService. Registers the service in the
     * bundle context.
     */
    private void loadMessageHistoryService()
    {
        configService.addPropertyChangeListener(
            MessageHistoryService.PNAME_IS_RECENT_MESSAGES_DISABLED,
            msgHistoryPropListener);

        boolean isRecentMessagesDisabled = configService.getBoolean(
            MessageHistoryService.PNAME_IS_RECENT_MESSAGES_DISABLED,
            false);

        if(!isRecentMessagesDisabled)
            loadRecentMessages();

        // start listening for newly register or removed protocol providers
        bundleContext.addServiceListener(this);

        for (ProtocolProviderService pps : getCurrentlyAvailableProviders())
        {
            this.handleProviderAdded(pps);
        }
    }

    /**
     * Returns currently registered in osgi ProtocolProviderServices.
     * @return currently registered in osgi ProtocolProviderServices.
     */
    List<ProtocolProviderService> getCurrentlyAvailableProviders()
    {
        List<ProtocolProviderService> res
            = new ArrayList<ProtocolProviderService>();

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
            return res;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found "
                         + protocolProviderRefs.length
                         + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) bundleContext
                    .getService(protocolProviderRefs[i]);

                res.add(provider);
            }
        }

        return res;
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
        if(!isHistoryLoggingEnabled(
                evt.getSourceAdHocChatRoom().getIdentifier()))
        {
            // logging is switched off for this particular chat room
            return;
        }

        try
        {
            History history = this.
            getHistoryForAdHocMultiChat(
                    evt.getSourceAdHocChatRoom());

            writeMessage(
                history, "out", evt.getMessage(), evt.getTimestamp(), false);
        }
        catch (IOException e)
        {
            logger.error("Could not add message to history", e);
        }
    }

    public void messageDeliveryFailed(
            AdHocChatRoomMessageDeliveryFailedEvent evt)
    {
        // nothing to do for the history service
    }

    public void messageReceived(AdHocChatRoomMessageReceivedEvent evt)
    {
        if(!isHistoryLoggingEnabled(
                evt.getSourceChatRoom().getIdentifier()))
        {
            // logging is switched off for this particular chat room
            return;
        }

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

    /**
     * Permanently removes all locally stored message history.
     *
     * @throws java.io.IOException
     *         Thrown if the history could not be removed due to a IO error.
     */
    public void eraseLocallyStoredHistory()
        throws IOException
    {
        HistoryID historyId = HistoryID.createFromRawID(
                    new String[] {  "messages" });
        historyService.purgeLocallyStoredHistory(historyId);

        if(this.messageSourceService != null)
            this.messageSourceService.eraseLocallyStoredHistory();
    }

    /**
     * Permanently removes locally stored message history for the metacontact.
     *
     * @throws java.io.IOException
     *         Thrown if the history could not be removed due to a IO error.
     */
    public void eraseLocallyStoredHistory(MetaContact contact)
        throws IOException
    {
        Iterator<Contact> iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();

            History history = this.getHistory(null, item);
            historyService.purgeLocallyStoredHistory(history.getID());
        }

        if(this.messageSourceService != null)
            this.messageSourceService.eraseLocallyStoredHistory(contact);
    }

    /**
     * Permanently removes locally stored message history for the chatroom.
     *
     * @throws java.io.IOException
     *         Thrown if the history could not be removed due to a IO error.
     */
    public void eraseLocallyStoredHistory(ChatRoom room)
        throws IOException
    {
        History history = this.getHistoryForMultiChat(room);
        historyService.purgeLocallyStoredHistory(history.getID());

        if(this.messageSourceService != null)
            this.messageSourceService.eraseLocallyStoredHistory(room);
    }
    
    /**
     * Returns <code>true</code> if the "IS_MESSAGE_HISTORY_ENABLED"
     * property is true, otherwise - returns <code>false</code>.
     * Indicates to the user interface whether the history logging is enabled.
     * @return <code>true</code> if the "IS_MESSAGE_HISTORY_ENABLED"
     * property is true, otherwise - returns <code>false</code>.
     */
    public boolean isHistoryLoggingEnabled()
    {
        return isHistoryLoggingEnabled;
    }

    /**
     * Updates the "isHistoryLoggingEnabled" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isEnabled indicates if the history logging is
     * enabled.
     */
    public void setHistoryLoggingEnabled(boolean isEnabled)
    {
        isHistoryLoggingEnabled = isEnabled;

        configService.setProperty(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED,
            Boolean.toString(isHistoryLoggingEnabled));
    }

    /**
     * Returns <code>true</code> if the "IS_MESSAGE_HISTORY_ENABLED"
     * property is true for the <tt>id</tt>, otherwise - returns
     * <code>false</code>.
     * Indicates to the user interface whether the history logging is enabled
     * for the supplied id (id for metacontact or for chat room).
     * @return <code>true</code> if the "IS_MESSAGE_HISTORY_ENABLED"
     * property is true for the <tt>id</tt>, otherwise - returns
     * <code>false</code>.
     */
    public boolean isHistoryLoggingEnabled(String id)
    {
        return configService.getBoolean(MessageHistoryService
                    .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX
                        + "." + id, true);
    }

    /**
     * Updates the "isHistoryLoggingEnabled" property through the
     * <tt>ConfigurationService</tt> for the contact.
     *
     * @param isEnabled indicates if the history logging is
     * enabled for the contact.
     */
    public void setHistoryLoggingEnabled(
        boolean isEnabled, String id)
    {
        if(isEnabled)
            configService.setProperty(
                MessageHistoryService
                    .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX
                + "." + id, null);
        else
            configService.setProperty(
                MessageHistoryService
                    .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX
                + "." + id, isEnabled);
    }
}
