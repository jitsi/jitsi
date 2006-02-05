/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import java.io.IOException;
import java.util.Iterator;

import net.java.sip.communicator.service.history.records.HistoryRecordStructure;

/**
 * This service provides the functionality to store history records. The
 * records are called <tt>HistoryRecord</tt>s and are grouped by ID.
 *
 * The ID may be used to set hierarchical structure. In a typical usage one
 * may set the first string to be the userID, and the second - the service name.
 *
 * @author Alexander Pelov
 */
public interface HistoryService {

	/**
	 * Returns the IDs of all existing histories.
	 *
	 * @return An iterator to a list of IDs.
	 */
	Iterator getExistingIDs();

	/**
	 * Returns the history associated with this ID.
	 *
	 * @param id The ID of the history.
	 * @return Returns the history with this ID.
	 * @throws IllegalArgumentException Thrown if there is no such history.
	 */
	History getHistory(HistoryID id)
		throws IllegalArgumentException;

	/**
	 * Tests if a history with the given ID exists.
	 *
	 * @param id The ID to test.
	 * @return True if a history with this ID exists. False otherwise.
	 */
	boolean isHistoryExisting(HistoryID id);

	/**
	 * Creates a new history for this ID.
	 *
	 * @param id The ID of the history to be created.
	 * @param recordStructure The structure of the data.
	 * @return Returns the history with this ID.
	 * @throws IllegalArgumentException Thrown if such history already exists.
	 * @throws IOException Thrown if the history could not be created due to
	 * 			a IO error.
	 */
	History createHistory(HistoryID id, HistoryRecordStructure recordStructure)
		throws IllegalArgumentException, IOException;
}
