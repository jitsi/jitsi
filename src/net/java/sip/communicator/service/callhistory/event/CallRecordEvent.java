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
package net.java.sip.communicator.service.callhistory.event;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;

/**
 * The <tt>CallRecordEvent</tt> indicates that a <tt>CallRecord</tt> has been
 * received as a result of a <tt>CallHistoryQuery</tt>.
 *
 * @author Yana Stamcheva
 */
public class CallRecordEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>CallRecord</tt> this event is about.
     */
    private final CallRecord callRecord;

    /**
     * Creates a <tt>CallRecordEvent</tt> by specifying the parent <tt>query</tt>
     * and the <tt>callRecord</tt> this event is about.
     * @param query the source that triggered this event
     * @param callRecord the <tt>CallRecord</tt> this event is about
     */
    public CallRecordEvent(CallHistoryQuery query,
                           CallRecord callRecord)
    {
        super(query);

        this.callRecord = callRecord;
    }

    /**
     * Returns the <tt>ContactQuery</tt> that triggered this event.
     * @return the <tt>ContactQuery</tt> that triggered this event
     */
    public CallHistoryQuery getQuerySource()
    {
        return (CallHistoryQuery) source;
    }

    /**
     * Returns the <tt>CallRecord</tt>s this event is about.
     * @return the <tt>CallRecord</tt>s this event is about
     */
    public CallRecord getCallRecord()
    {
        return callRecord;
    }
}
