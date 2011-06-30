/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history.records;

/**
 * @author Alexander Pelov
 */
public class HistoryRecord
{
    private final long timestamp;
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
                System.currentTimeMillis());
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
        this(propertyNames, propertyValues, System.currentTimeMillis());
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
                         long timestamp)
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
                         long timestamp)
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

    public long getTimestamp()
    {
        return this.timestamp;
    }

    /**
     * Returns the String representation of this HistoryRecord.
     *
     * @return the String representation of this HistoryRecord
     */
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
