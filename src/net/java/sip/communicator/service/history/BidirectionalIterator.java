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
 * The standard Java Iterator is uni-directional, allowing the user to explore
 * the contents of a collection in one way only. This interface defines a
 * bi-directional iterator, permiting the user to go forwards and backwards in a
 * collection.
 *
 * @author Alexander Pelov
 */
public interface BidirectionalIterator<T> extends Iterator<T>
{
    /**
     * Returns true if the iteration has elements preceeding the current one.
     * (In other words, returns true if <tt>prev</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return true if the iterator has preceeding elements.
     */
    boolean hasPrev();

    /**
     * Returns the previous element in the iteration.
     *
     * @return the previous element in the iteration.
     *
     * @throws NoSuchElementException
     *             iteration has no more elements.
     */
    T prev() throws NoSuchElementException;
}
