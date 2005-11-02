/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import net.java.sip.communicator.service.history.records.HistoryRecordStructure;

/**
 * @author Alexander Pelov
 */
public interface History {
	
	/**
	 * @return Returns an object which can be used to read and query
	 * this history.
	 */
	HistoryReader getReader();
	
	/**
	 * @return Returns an object which can be used to append records to
	 * this history.
	 */
	HistoryWriter getWriter();
	
	/**
	 * @return Returns the ID of this history.
	 */
	HistoryID getID();
	
	/**
	 * @return Returns the structure of the history records in this history.
	 */
	HistoryRecordStructure getHistoryRecordsStructure();
	
}
