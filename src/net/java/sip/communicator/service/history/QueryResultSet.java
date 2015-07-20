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
