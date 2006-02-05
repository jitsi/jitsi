/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import java.util.Date;

/**
 * @author Alexander Pelov
 */
public interface HistoryReader {

   /**
    * Searches the history for all records with timestamp
    * after <tt>startDate</tt>.
    *
    * @throws RuntimeException Thrown if an exception occurs during
    *          the execution of the query, such as internal IO error.
    */
   QueryResultSet findByStartDate(Date startDate)
       throws RuntimeException;

	/**
    * Searches the history for all records with timestamp
    * before <tt>endDate</tt>.
    *
    * @throws RuntimeException Thrown if an exception occurs during
    *          the execution of the query, such as internal IO error.
    */
   QueryResultSet findByEndDate(Date endDate)
       throws RuntimeException;

   /**
    * Searches the history for all records with timestamp
    * between <tt>startDate</tt> and <tt>endDate</tt>.
    *
    * @throws RuntimeException Thrown if an exception occurs during
    *          the execution of the query, such as internal IO error.
    */
   QueryResultSet findByPeriod(Date startDate, Date endDate)
   	throws RuntimeException;

   /**
    * Searches the history for all records containing the <tt>keyword</tt>.
    *
    * @throws RuntimeException Thrown if an exception occurs during
    *          the execution of the query, such as internal IO error.
    */
   QueryResultSet findByKeyword(String keyword)
       throws RuntimeException;

   /**
    * Searches the history for all records containing all <tt>keywords</tt>.
    *
    * @throws RuntimeException Thrown if an exception occurs during
    *          the execution of the query, such as internal IO error.
    */
   QueryResultSet findByKeywords(String[] keywords)
       throws RuntimeException;

   /**
    * Searches for all history records containing all <tt>keywords</tt>,
    * with timestamp between <tt>startDate</tt> and <tt>endDate</tt>.
    *
    * @throws RuntimeException Thrown if an exception occurs during
    *          the execution of the query, such as internal IO error.
    */
   QueryResultSet findByText(Date startDate, Date endDate, String[] keywords)
       throws UnsupportedOperationException;

}
