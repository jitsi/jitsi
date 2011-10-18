/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */package net.java.sip.communicator.service.history;

import java.util.*;

/**
 * @author Alexander Pelov
 */
public class DefaultQueryResultSet<T> implements QueryResultSet<T> {

    private Vector<T> records = new Vector<T>();

    private int currentPos = -1;

    public DefaultQueryResultSet(Vector<T> records)
    {
        this.records = records;
    }

    public T nextRecord() throws NoSuchElementException
    {
        return this.next();
    }

    public T prevRecord() throws NoSuchElementException
    {
        return this.prev();
    }

    public boolean hasPrev()
    {
        return this.currentPos - 1 >= 0;
    }

    public T prev() throws NoSuchElementException
    {
        this.currentPos--;

        if (this.currentPos < 0)
        {
            throw new NoSuchElementException();
        }

        return records.get(this.currentPos);
    }

    public boolean hasNext()
    {
        return this.currentPos + 1 < this.records.size();
    }

    public T next()
    {
        this.currentPos++;

        if (this.currentPos >= this.records.size())
        {
            throw new NoSuchElementException();
        }

        return records.get(this.currentPos);
    }

    public void remove() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Cannot remove elements "
                + "from underlaying collection.");
    }
}
