package net.java.sip.communicator.impl.history;

import java.util.*;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;

/**
 * This implementation is the same as DefaultQueryResultSet but the
 * container holding the records is LinkedList - so guarantees that values are ordered
 *
 * @author Damian Minkov
 */
public class OrderedQueryResultSet
    implements QueryResultSet
{
    private LinkedList records = null;

    private int currentPos = -1;

    public OrderedQueryResultSet(Set records)
    {
        this.records = new LinkedList(records);
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements.
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext()
    {
        return this.currentPos + 1 < this.records.size();
    }

    /**
     * Returns true if the iteration has elements preceeding the current one.
     *
     * @return true if the iterator has preceeding elements.
     */
    public boolean hasPrev()
    {
        return this.currentPos - 1 >= 0;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     */
    public Object next()
    {
        this.currentPos++;

        if (this.currentPos >= this.records.size())
        {
            throw new NoSuchElementException();
        }

        return records.get(this.currentPos);
    }

    /**
     * A strongly-typed variant of <tt>next()</tt>.
     *
     * @return the next history record.
     * @throws NoSuchElementException iteration has no more elements.
     */
    public HistoryRecord nextRecord() throws NoSuchElementException
    {
        return (HistoryRecord) this.next();
    }

    /**
     * Returns the previous element in the iteration.
     *
     * @return the previous element in the iteration.
     * @throws NoSuchElementException iteration has no more elements.
     */
    public Object prev() throws NoSuchElementException
    {
        this.currentPos--;

        if (this.currentPos < 0)
        {
            throw new NoSuchElementException();
        }

        return records.get(this.currentPos);
    }

    /**
     * A strongly-typed variant of <tt>prev()</tt>.
     *
     * @return the previous history record.
     * @throws NoSuchElementException iteration has no more elements.
     */
    public HistoryRecord prevRecord() throws NoSuchElementException
    {
        return (HistoryRecord) this.prev();
    }

    /**
     * Removes from the underlying collection the last element returned by
     * the iterator (optional operation).
     */
    public void remove()
    {
        throw new UnsupportedOperationException("Cannot remove elements "
                + "from underlaying collection.");
    }
}
