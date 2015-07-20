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

/**
 * The <tt>InteractiveHistoryReader</tt> allows to search in the history in an
 * interactive way, i.e. be able to cancel the search at any time and track the
 * results through a <tt>HistoryQueryListener</tt>.
 *
 * @author Yana Stamcheva
 */
public interface InteractiveHistoryReader
{
    /**
     * Searches the history for all records containing all <tt>keywords</tt>.
     *
     * @param keywords array of keywords we search for
     * @param field the field where to look for the keyword
     * @param recordCount limits the result to this record count
     * @return a <tt>HistoryQuery</tt> object allowing to track this query
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public HistoryQuery findByKeywords( String[] keywords,
                                        String field,
                                        int recordCount);

    /**
     * Searches the history for all records containing the <tt>keyword</tt>.
     *
     * @param keyword the keyword to search for
     * @param field the field where to look for the keyword
     * @param recordCount limits the result to this record count
     * @return a <tt>HistoryQuery</tt> object allowing to track this query
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public HistoryQuery findByKeyword(  String keyword,
                                        String field,
                                        int recordCount);
}
