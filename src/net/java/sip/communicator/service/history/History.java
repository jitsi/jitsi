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

import net.java.sip.communicator.service.history.records.*;

/**
 * @author Alexander Pelov
 * @author Yana Stamcheva
 */
public interface History
{
    /**
     * Returns an object which can be used to read and query this history.
     * @return an object which can be used to read and query this history
     */
    HistoryReader getReader();

    /**
     * Returns an object that can be used to read and query this history. The
     * <tt>InteractiveHistoryReader</tt> differs from the <tt>HistoryReader</tt>
     * in the way it manages query results. It allows to cancel a search at
     * any time and to track history results through a
     * <tt>HistoryQueryListener</tt>.
     * @return an object that can be used to read and query this history
     */
    InteractiveHistoryReader getInteractiveReader();

    /**
     * Returns an object which can be used to append records to this history.
     * @return an object which can be used to append records to this history
     */
    HistoryWriter getWriter();

    /**
     * @return Returns the ID of this history.
     */
    HistoryID getID();

    /**
     * @return Returns the structure of the history records in this history.
     */
    HistoryRecordStructure getHistoryRecordsStructure();

    /**
     * Sets the given <tt>structure</tt> to be the new history records
     * structure used in this history implementation.
     * @param structure the new <tt>HistoryRecordStructure</tt> to use
     */
    void setHistoryRecordsStructure(HistoryRecordStructure structure);
}
