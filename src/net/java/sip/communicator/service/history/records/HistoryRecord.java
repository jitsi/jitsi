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
package net.java.sip.communicator.service.history.records;

import java.util.*;

/**
 * @author Alexander Pelov
 */
public class HistoryRecord
{
    private final Date timestamp;
    private final String[] propertyNames;
    private final String[] propertyValues;

    /**
     * Constructs an entry containing multiple name-value pairs, where the names
     * are taken from the defined structure. The timestamp is set to the time this
     * object is created.
     *
     * @param entryStructure
     * @param propertyValues
     */
    public HistoryRecord(HistoryRecordStructure entryStructure,
                         String[] propertyValues)
    {
        this(
                entryStructure.getPropertyNames(),
                propertyValues,
                new Date());
    }

    /**
     * Constructs an entry containing multiple name-value pairs, where the name is not
     * unique. The timestamp is set to the time this object is created.
     *
     * @param propertyNames
     * @param propertyValues
     */
    public HistoryRecord(String[] propertyNames, String[] propertyValues)
    {
        this(propertyNames, propertyValues, new Date());
    }

    /**
     * Constructs an entry containing multiple name-value pairs, where the names
     * are taken from the defined structure.
     *
     * @param entryStructure
     * @param propertyValues
     * @param timestamp
     */
    public HistoryRecord(HistoryRecordStructure entryStructure,
                         String[] propertyValues,
                         Date timestamp)
    {
        this(entryStructure.getPropertyNames(), propertyValues, timestamp);
    }

    /**
     * Constructs an entry containing multiple name-value pairs, where the name is not
     * unique.
     *
     * @param propertyNames
     * @param propertyValues
     * @param timestamp
     */
    public HistoryRecord(String[] propertyNames,
                         String[] propertyValues,
                         Date timestamp)
    {
        // TODO: Validate: Assert.assertNonNull(propertyNames, "The property names should be non-null.");
        // TODO: Validate: Assert.assertNonNull(propertyValues, "The property values should be non-null.");
        // TODO: Validate: Assert.assertNonNull(timestamp, "The timestamp should be non-null.");

        // TODO: Validate Assert.assertTrue(propertyNames.length == propertyValues.length,
        //"The length of the property names and property values should be equal.");

        this.propertyNames = propertyNames;
        this.propertyValues = propertyValues;
        this.timestamp = timestamp;
    }

    public String[] getPropertyNames()
    {
        return this.propertyNames;
    }

    public String[] getPropertyValues()
    {
        return this.propertyValues;
    }

    public Date getTimestamp()
    {
        return this.timestamp;
    }

    /**
     * Returns the String representation of this HistoryRecord.
     *
     * @return the String representation of this HistoryRecord
     */
    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder("History Record: ");

        for (int i = 0; i < propertyNames.length; i++)
        {
            s.append(propertyNames[i]);
            s.append('=');
            s.append(propertyValues[i]);
            s.append('\n');
        }
        return s.toString();
    }
}
