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
package net.java.sip.communicator.service.history.event;

import java.util.*;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;

/**
 * The <tt>HistoryRecordEvent</tt> indicates that a <tt>HistoryRecord</tt>s has
 * been received as a result of a <tt>HistoryQuery</tt>.
 *
 * @author Yana Stamcheva
 */
public class HistoryRecordEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>HistoryRecord</tt> this event is about.
     */
    private final HistoryRecord historyRecord;

    /**
     * Creates a <tt>HistoryRecordEvent</tt> by specifying the initial query
     * and the record this event is about.
     * @param query the source that triggered this event
     * @param historyRecord the <tt>HistoryRecord</tt> this event is about
     */
    public HistoryRecordEvent(  HistoryQuery query,
                                HistoryRecord historyRecord)
    {
        super(query);

        this.historyRecord = historyRecord;
    }

    /**
     * Returns the <tt>HistoryQuery</tt> that triggered this event.
     * @return the <tt>HistoryQuery</tt> that triggered this event
     */
    public HistoryQuery getQuerySource()
    {
        return (HistoryQuery) source;
    }

    /**
     * Returns the <tt>HistoryRecord</tt>s this event is about.
     * @return the <tt>HistoryRecord</tt>s this event is about
     */
    public HistoryRecord getHistoryRecord()
    {
        return historyRecord;
    }
}
