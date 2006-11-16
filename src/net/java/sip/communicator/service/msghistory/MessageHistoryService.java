/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.msghistory;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.msghistory.event.*;

/**
 * The Message History Service stores messages exchanged through the various protocols
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public interface MessageHistoryService
{
    /**
     * Returns all the messages exchanged by all the contacts
     * in the supplied metacontact after the given date
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the conversations
     * @return Collection of MessageReceivedEvents or MessageDeliveredEvents
     * @throws RuntimeException
     */
    public Collection findByStartDate(MetaContact contact, Date startDate)
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
    public Collection findByEndDate(MetaContact contact, Date endDate)
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
    public Collection findByPeriod(MetaContact contact, Date startDate, Date endDate)
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
    public Collection findByPeriod(MetaContact contact, Date startDate, Date endDate, String[] keywords)
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
    public Collection findByPeriod(MetaContact contact, Date startDate, Date endDate,
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
    public Collection findByKeyword(MetaContact contact, String keyword)
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
    Collection findByKeyword(MetaContact contact, String keyword, boolean caseSensitive)
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
    public Collection findByKeywords(MetaContact contact, String[] keywords)
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
    public Collection findByKeywords(MetaContact contact, String[] keywords, boolean caseSensitive)
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
    public Collection findLast(MetaContact contact, int count)
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
    public Collection getFirstMessagesAfter(MetaContact contact, Date date, int count)
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
    public Collection getLastMessagesBefore(MetaContact contact, Date date, int count)
        throws RuntimeException;

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
   public void addSearchProgressListener(MessageHistorySearchProgressListener listener);

   /**
    * Removing progress listener
    *
    * @param listener HistorySearchProgressListener
    */
   public void removeSearchProgressListener(MessageHistorySearchProgressListener listener);
}
