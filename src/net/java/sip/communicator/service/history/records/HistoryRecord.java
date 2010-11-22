/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history.records;

import java.util.*;

/**
 * @author Alexander Pelov
 */
public class HistoryRecord
{

    private Date timestamp;
    private String[] propertyNames;
    private String[] propertyValues;

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
        this(entryStructure.getPropertyNames(), propertyValues, new Date());
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
                         String[] propertyValues, Date timestamp)
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
    public HistoryRecord(String[] propertyNames, String[] propertyValues,
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

    public long getTimeInMillis()
    {
        return (timestamp == null) ? 0 : timestamp.getTime();
    }

    /**
     * Returns the String representation of this HistoryRecord.
     *
     * @return the String representation of this HistoryRecord
     */
    public String toString()
    {
        String s = "History Record: ";
        for (int i = 0; i < propertyNames.length; i++)
        {
            s += propertyNames[i] + "=" + propertyValues[i] + "\n";
        }

        return s;
    }
}
