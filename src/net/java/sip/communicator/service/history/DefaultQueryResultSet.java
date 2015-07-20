/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.history;

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
