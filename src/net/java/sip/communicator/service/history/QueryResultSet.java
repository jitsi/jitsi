/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import java.util.*;

/**
 * 
 * @author Alexander Pelov
 */
public interface QueryResultSet<T> extends BidirectionalIterator<T> {

    /**
     * A strongly-typed variant of <tt>next()</tt>.
     * 
     * @return the next history record.
     * 
     * @throws NoSuchElementException
     *             iteration has no more elements.
     */
    T nextRecord() throws NoSuchElementException;

    /**
     * A strongly-typed variant of <tt>prev()</tt>.
     * 
     * @return the previous history record.
     * 
     * @throws NoSuchElementException
     *             iteration has no more elements.
     */
    T prevRecord() throws NoSuchElementException;

}
