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

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.history.records.*;

/**
 * @author Alexander Pelov
 * @author Hristo Terezov
 */
public interface HistoryWriter
{
    /**
     * Stores the passed record complying with the historyRecordStructure.
     *
     * @param record
     *            The record to be added.
     *
     * @throws IOException
     */
    public void addRecord(HistoryRecord record)
        throws IOException;

    /**
     * Stores the passed propertyValues complying with the
     * historyRecordStructure.
     *
     * @param propertyValues
     *            The values of the record.
     *
     * @throws IOException
     */
    public void addRecord(String[] propertyValues)
        throws IOException;

    /**
     * Stores the passed propertyValues complying with the
     * historyRecordStructure.
     *
     * @param propertyValues
     *            The values of the record.
     * @param maxNumberOfRecords the maximum number of records to keep or
     * value of -1 to ignore this param.
     *
     * @throws IOException
     */
    public void addRecord(String[] propertyValues,
                   int maxNumberOfRecords)
        throws IOException;

    /**
     * Stores the passed propertyValues complying with the
     * historyRecordStructure.
     *
     * @param propertyValues
     *            The values of the record.
     * @param timestamp
     *            The timestamp of the record.
     *
     * @throws IOException
     */
    public void addRecord(String[] propertyValues, Date timestamp)
        throws IOException;

    /**
     * Stores the passed propertyValues complying with the
     * historyRecordStructure.
     *
     * @param propertyValues
     *            The values of the record.
     * @param timestamp
     *            The timestamp of the record.
     *
     * @throws IOException
     */
    public void insertRecord(
            String[] propertyValues, Date timestamp, String timestampProperty)
        throws IOException;

    /**
     * Updates a record by searching for record with idProperty which have idValue
     * and updating/creating the property with newValue.
     *
     * @param idProperty name of the id property
     * @param idValue value of the id property
     * @param property the property to change
     * @param newValue the value of the changed property.
     */
    public void updateRecord(
            String idProperty, String idValue, String property, String newValue)
        throws IOException;

    /**
     * Updates history record using given <tt>HistoryRecordUpdater</tt> instance
     * to find which is the record to be updated and to get the new values for
     * the fields
     * @param updater the <tt>HistoryRecordUpdater</tt> instance.
     */
    public void updateRecord(HistoryRecordUpdater updater)
        throws IOException;

    /**
     * This interface is used to find a history record to update and to get the
     * new values for the record.
     */
    public interface HistoryRecordUpdater
    {
        /**
         * Sets the current history record.
         * @param historyRecord the history record.
         */
        public void setHistoryRecord(HistoryRecord historyRecord);

        /**
         * Checks if the history record should be updated or not
         * @return <tt>true<tt> if the record should be updated.
         */
        public boolean isMatching();

        /**
         * Returns a map with the names and new values of the fields that will
         * be updated
         * @return a map with the names and new values of the fields that will
         * be updated
         */
        public Map<String, String> getUpdateChanges();
    }
}
