/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import java.io.IOException;

import net.java.sip.communicator.service.history.records.HistoryRecord;

/**
 * @author Alexander Pelov
 */
public interface HistoryWriter {
	
	/**
	 * Stores the passed record complying with the historyRecordStructure.
	 * 
	 * @param record The record to be added.
	 * 
	 * @throws IOException
	 */
	void addRecord(HistoryRecord record) throws	IOException;
	
	/**
	 * Stores the passed propertyValues complying with the historyRecordStructure.
	 * 
	 * @param propertyValues The values of the record.
	 * 
	 * @throws IOException
	 */
	void addRecord(String[] propertyValues) throws	IOException;
	
}
