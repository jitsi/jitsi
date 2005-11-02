/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */package net.java.sip.communicator.service.history;

import java.util.NoSuchElementException;
import java.util.Vector;

import net.java.sip.communicator.service.history.records.HistoryRecord;

/**
 * @author Alexander Pelov
 */
public class DefaultQueryResultSet implements QueryResultSet {

	private Vector records = new Vector();
	private int currentPos = -1;
	
	public DefaultQueryResultSet(Vector records) {
		this.records = records;
	}
	
	public HistoryRecord nextRecord() throws NoSuchElementException {
		return (HistoryRecord)this.next();
	}

	public HistoryRecord prevRecord() throws NoSuchElementException {
		return (HistoryRecord)this.prev();
	}

	public boolean hasPrev() {
		return this.currentPos-1 >= 0;
	}

	public Object prev() throws NoSuchElementException {
		this.currentPos--;
		
		if(this.currentPos < 0) {
			throw new NoSuchElementException();
		}
		
		return records.get(this.currentPos);
	}

	public boolean hasNext() {
		return this.currentPos+1 < this.records.size();
	}

	public Object next() {
		this.currentPos++;
		
		if(this.currentPos >= this.records.size()) {
			throw new NoSuchElementException();
		}
		
		return records.get(this.currentPos);
	}

	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Cannot remove elements " +
				"from underlaying collection.");
	}
}
