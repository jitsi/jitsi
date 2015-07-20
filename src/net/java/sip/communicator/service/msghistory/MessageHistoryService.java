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
package net.java.sip.communicator.service.msghistory;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.msghistory.event.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The Message History Service stores messages exchanged through the various protocols
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public interface MessageHistoryService
{
    /**
     * Name of the property that indicates whether the logging of messages is
     * enabled.
     */
    public static final String PNAME_IS_MESSAGE_HISTORY_ENABLED
        = "net.java.sip.communicator.service.msghistory."
            + "IS_MESSAGE_HISTORY_ENABLED";

    /**
     * Name of the property that indicates whether the recent messages is
     * enabled.
     */
    public static final String PNAME_IS_RECENT_MESSAGES_DISABLED
        = "net.java.sip.communicator.service.msghistory."
            + "IS_RECENT_MESSAGES_DISABLED";

    /**
     * Name of the property that indicates whether the logging of messages is
     * enabled.
     */
    public static final String
        PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX
            = "net.java.sip.communicator.service.msghistory.contact";

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact after the given date
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByStartDate(
            MetaContact contact, Date startDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact before the given date
     *
     * @param contact MetaContact
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByEndDate(
            MetaContact contact, Date endDate)
        throws RuntimeException;

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
    public Collection<EventObject> findByPeriod(
            MetaContact contact, Date startDate, Date endDate)
        throws RuntimeException;

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
    public Collection<EventObject> findByPeriod(
            MetaContact contact, Date startDate, Date endDate,
            String[] keywords)
        throws RuntimeException;

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
    public Collection<EventObject> findByPeriod(
            MetaContact contact, Date startDate, Date endDate,
            String[] keywords, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keyword
     *
     * @param contact MetaContact
     * @param keyword keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeyword(
            MetaContact contact, String keyword)
        throws RuntimeException;

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
    Collection<EventObject> findByKeyword(
            MetaContact contact, String keyword, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact having the given keywords
     *
     * @param contact MetaContact
     * @param keywords keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeywords(
            MetaContact contact, String[] keywords)
        throws RuntimeException;

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
    public Collection<EventObject> findByKeywords(
            MetaContact contact, String[] keywords, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages exchanged by all the contacts
     * in the supplied metacontact
     *
     * @param contact MetaContact
     * @param count messages count
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findLast(MetaContact contact, int count)
        throws RuntimeException;

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
    public Collection<EventObject> findFirstMessagesAfter(
            MetaContact contact, Date date, int count)
        throws RuntimeException;

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
    public Collection<EventObject> findLastMessagesBefore(
            MetaContact contact, Date date, int count)
        throws RuntimeException;

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
   public void addSearchProgressListener(
       MessageHistorySearchProgressListener listener);

   /**
    * Removing progress listener
    *
    * @param listener HistorySearchProgressListener
    */
   public void removeSearchProgressListener(
       MessageHistorySearchProgressListener listener);

   /**
     * Returns all the messages exchanged in the supplied
     * chat room after the given date
     *
     * @param room The chat room
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByStartDate(
            ChatRoom room, Date startDate)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged
     * in the supplied chat room before the given date
     *
     * @param room The chat room
     * @param endDate Date the end date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByEndDate(
            ChatRoom room, Date endDate)
        throws RuntimeException;

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
    public Collection<EventObject> findByPeriod(
            ChatRoom room, Date startDate, Date endDate)
        throws RuntimeException;

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
        throws RuntimeException;

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
    public Collection<EventObject> findByPeriod(
            ChatRoom room, Date startDate, Date endDate,
            String[] keywords, boolean caseSensitive)
        throws RuntimeException;

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
        throws RuntimeException;

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
    Collection<EventObject> findByKeyword(
            ChatRoom room, String keyword, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns all the messages exchanged
     * in the supplied chat room having the given keywords
     *
     * @param room The chat room
     * @param keywords keyword
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection<EventObject> findByKeywords(
            ChatRoom room, String[] keywords)
        throws RuntimeException;

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
    public Collection<EventObject> findByKeywords(
            ChatRoom room, String[] keywords, boolean caseSensitive)
        throws RuntimeException;

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
        throws RuntimeException;

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
    public Collection<EventObject> findFirstMessagesAfter(
            ChatRoom room, Date date, int count)
        throws RuntimeException;

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
    public Collection<EventObject> findLastMessagesBefore(
            ChatRoom room, Date date, int count)
        throws RuntimeException;

   /**
    * Permanently removes all locally stored message history.
    *
    * @throws java.io.IOException
    *         Thrown if the history could not be removed due to a IO error.
    */
   public void eraseLocallyStoredHistory()
       throws IOException;

   /**
    * Permanently removes locally stored message history for the metacontact.
    *
    * @throws java.io.IOException
    *         Thrown if the history could not be removed due to a IO error.
    */
   public void eraseLocallyStoredHistory(MetaContact contact)
       throws IOException;

   /**
    * Permanently removes locally stored message history for the chatroom.
    *
    * @throws java.io.IOException
    *         Thrown if the history could not be removed due to a IO error.
    */
   public void eraseLocallyStoredHistory(ChatRoom room)
       throws IOException;
   
   /**
    * Returns <code>true</code> if the "IS_MESSAGE_HISTORY_ENABLED"
    * property is true, otherwise - returns <code>false</code>.
    * Indicates to the user interface whether the history logging is enabled.
    * @return <code>true</code> if the "IS_MESSAGE_HISTORY_ENABLED"
    * property is true, otherwise - returns <code>false</code>.
    */
   public boolean isHistoryLoggingEnabled();
   
   /**
    * Updates the "isHistoryLoggingEnabled" property through the
    * <tt>ConfigurationService</tt>.
    *
    * @param isEnabled indicates if the history logging is
    * enabled.
    */
   public void setHistoryLoggingEnabled(boolean isEnabled);
   
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
   public boolean isHistoryLoggingEnabled(String id);
   
   /**
    * Updates the "isHistoryLoggingEnabled" property through the
    * <tt>ConfigurationService</tt> for the contact.
    *
    * @param isEnabled indicates if the history logging is
    * enabled for the contact.
    */
   public void setHistoryLoggingEnabled(boolean isEnabled, String id);
}
