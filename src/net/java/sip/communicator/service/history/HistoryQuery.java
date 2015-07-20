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

import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.records.*;

/**
 * The <tt>HistoryQuery</tt> corresponds to a query made through the
 * <tt>InteractiveHistoryReader</tt>. It allows to be canceled, to listen for
 * changes in the results and to obtain initial results if available.
 *
 * @author Yana Stamcheva
 */
public interface HistoryQuery
{
    /**
     * Cancels this query.
     */
    public void cancel();

    /**
     * Returns the query string, this query was created for.
     *
     * @return the query string, this query was created for
     */
    public String getQueryString();

    /**
     * Returns a collection of the results for this query. It's up to
     * the implementation to determine how and when to fill this list of
     * results.
     * <p>
     * This method could be used in order to obtain first fast initial
     * results and then obtain the additional results through the
     * <tt>HistoryQueryListener</tt>, which should improve user experience when
     * waiting for results.
     *
     * @return a collection of the initial results for this query
     */
    public Collection<HistoryRecord> getHistoryRecords();

    /**
     * Adds the given <tt>HistoryQueryListener</tt> to the list of
     * listeners interested in query result changes.
     * @param l the <tt>HistoryQueryListener</tt> to add
     */
    public void addHistoryRecordsListener(HistoryQueryListener l);

    /**
     * Removes the given <tt>HistoryQueryListener</tt> from the list of
     * listeners interested in query result changes.
     * @param l the <tt>HistoryQueryListener</tt> to remove
     */
    public void removeHistoryRecordsListener(HistoryQueryListener l);
}
