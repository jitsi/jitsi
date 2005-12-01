/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import java.util.NoSuchElementException;

import net.java.sip.communicator.service.history.records.HistoryRecord;


/**
*
* @author Alexander Pelov
*/
public interface QueryResultSet extends BidirectionalIterator {

	/**
	 * A strongly-typed variant of <code>next()</code>.
	 * 
	 * @return the next history record.
	 * 
	 * @throws NoSuchElementException iteration has no more elements. 
	 */
    HistoryRecord nextRecord() throws NoSuchElementException;
    
	/**
	 * A strongly-typed variant of <code>prev()</code>.
	 * 
	 * @return the previous history record.
	 * 
	 * @throws NoSuchElementException iteration has no more elements. 
	 */
    HistoryRecord prevRecord() throws NoSuchElementException;
    
}
