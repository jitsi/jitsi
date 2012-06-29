/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import java.util.*;

import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.records.*;

/**
 * Used to serach over the history records
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public interface HistoryReader {

    /**
     * Searches the history for all records with timestamp after
     * <tt>startDate</tt>.
     *
     * @param startDate the date after all records will be returned
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByStartDate(Date startDate)
        throws RuntimeException;

    /**
     * Searches the history for all records with timestamp before
     * <tt>endDate</tt>.
     *
     * @param endDate the date before which all records will be returned
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByEndDate(Date endDate)
        throws RuntimeException;

    /**
     * Searches the history for all records with timestamp between
     * <tt>startDate</tt> and <tt>endDate</tt>.
     *
     * @param startDate start of the interval in which we search
     * @param endDate end of the interval in which we search
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByPeriod(  Date startDate,
                                                        Date endDate)
            throws RuntimeException;

    /**
     * Searches the history for all records containing the <tt>keyword</tt>.
     *
     * @param keyword the keyword to search for
     * @param field the field where to look for the keyword
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByKeyword( String keyword,
                                                        String field)
            throws RuntimeException;

    /**
     * Searches the history for all records containing the <tt>keyword</tt>.
     *
     * @param keyword the keyword to search for
     * @param field the field where to look for the keyword
     * @param caseSensitive is keywords search case sensitive
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByKeyword( String keyword,
                                                        String field,
                                                        boolean caseSensitive)
        throws RuntimeException;

    /**
     * Searches the history for all records containing all <tt>keywords</tt>.
     *
     * @param keywords array of keywords we search for
     * @param field the field where to look for the keyword
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByKeywords(String[] keywords,
                                                        String field)
        throws RuntimeException;

    /**
     * Searches the history for all records containing all <tt>keywords</tt>.
     *
     * @param keywords array of keywords we search for
     * @param field the field where to look for the keyword
     * @param caseSensitive is keywords search case sensitive
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByKeywords(String[] keywords,
                                                        String field,
                                                        boolean caseSensitive)
        throws RuntimeException;

    /**
     * Searches for all history records containing all <tt>keywords</tt>,
     * with timestamp between <tt>startDate</tt> and <tt>endDate</tt>.
     *
     * @param startDate start of the interval in which we search
     * @param endDate end of the interval in which we search
     * @param keywords array of keywords we search for
     * @param field the field where to look for the keyword
     * @return the found records
     * @throws UnsupportedOperationException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByPeriod(  Date startDate,
                                                        Date endDate,
                                                        String[] keywords,
                                                        String field)
            throws UnsupportedOperationException;

    /**
     * Searches for all history records containing all <tt>keywords</tt>,
     * with timestamp between <tt>startDate</tt> and <tt>endDate</tt>.
     *
     * @param startDate start of the interval in which we search
     * @param endDate end of the interval in which we search
     * @param keywords array of keywords we search for
     * @param field the field where to look for the keyword
     * @param caseSensitive is keywords search case sensitive
     * @return the found records
     * @throws UnsupportedOperationException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public QueryResultSet<HistoryRecord> findByPeriod(  Date startDate,
                                                        Date endDate,
                                                        String[] keywords,
                                                        String field,
                                                        boolean caseSensitive)
        throws UnsupportedOperationException;

    /**
     * Returns the supplied number of recent messages
     *
     * @param count messages count
     * @return the found records
     * @throws RuntimeException
     */
    QueryResultSet<HistoryRecord> findLast(int count) throws RuntimeException;

    /**
     * Returns the supplied number of recent messages after the given date
     *
     * @param date messages after date
     * @param count messages count
     * @return QueryResultSet the found records
     * @throws RuntimeException
     */
    public QueryResultSet<HistoryRecord> findFirstRecordsAfter( Date date,
                                                                int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent messages before the given date
     *
     * @param date messages before date
     * @param count messages count
     * @return QueryResultSet the found records
     * @throws RuntimeException
     */
    public QueryResultSet<HistoryRecord> findLastRecordsBefore( Date date,
                                                                int count)
        throws RuntimeException;

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
    public void addSearchProgressListener(
        HistorySearchProgressListener listener);

    /**
     * Removing progress listener
     *
     * @param listener HistorySearchProgressListener
     */
    public void removeSearchProgressListener(
        HistorySearchProgressListener listener);

    /**
     * Total count of records that current history reader will read through
     * 
     * @return the number of searched messages
     * @throws UnsupportedOperationException 
     *              Thrown if an exception occurs during the execution of the
     *              query, such as internal IO error.
     */
    public int countRecords()
        throws UnsupportedOperationException;
}
