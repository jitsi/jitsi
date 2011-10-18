/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.metahistory;

import java.util.*;
import net.java.sip.communicator.service.history.event.*;

/**
 * The Meta History Service is wrapper around the other known
 * history services. Query them all at once, sort the result and return all
 * merged records in one collection.
 *
 * @author Damian Minkov
 */
public interface MetaHistoryService
{
    /**
     * Returns all the records for the descriptor after the given date.
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByStartDate(String[] services,
            Object descriptor, Date startDate)
        throws RuntimeException;

    /**
     * Returns all the records before the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param endDate Date the date of the last record to return
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByEndDate(String[] services,
            Object descriptor, Date endDate)
        throws RuntimeException;

    /**
     * Returns all the records between the given dates
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @param endDate Date the date of the last record to return
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByPeriod(String[] services,
            Object descriptor, Date startDate, Date endDate)
        throws RuntimeException;

    /**
     * Returns all the records between the given dates and having the given
     * keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @param endDate Date the date of the last record to return
     * @param keywords array of keywords
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByPeriod(String[] services,
            Object descriptor, Date startDate, Date endDate, String[] keywords)
        throws RuntimeException;

    /**
     * Returns all the records between the given dates and having the given
     * keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param startDate Date the date of the first record to return
     * @param endDate Date the date of the last record to return
     * @param keywords array of keywords
     * @param caseSensitive is keywords search case sensitive
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByPeriod(String[] services,
            Object descriptor, Date startDate, Date endDate,
            String[] keywords, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns all the records having the given keyword
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param keyword keyword
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeyword(String[] services,
            Object descriptor, String keyword)
        throws RuntimeException;

    /**
     * Returns all the records having the given keyword
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param keyword keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeyword(String[] services,
            Object descriptor, String keyword, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns all the records having the given keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param keywords keyword
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeywords(String[] services,
            Object descriptor, String[] keywords)
        throws RuntimeException;

    /**
     * Returns all the records having the given keywords
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param keywords keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeywords(String[] services,
            Object descriptor, String[] keywords, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent records.
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findLast(String[] services,
            Object descriptor, int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent records after the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param date messages after date
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findFirstMessagesAfter(String[] services,
            Object descriptor, Date date, int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent records before the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param date messages before date
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findLastMessagesBefore(String[] services,
            Object descriptor, Date date, int count)
        throws RuntimeException;

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
   public void addSearchProgressListener(HistorySearchProgressListener listener);

   /**
    * Removing progress listener
    *
    * @param listener HistorySearchProgressListener
    */
   public void removeSearchProgressListener(HistorySearchProgressListener listener);
}
