/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.history.records.*;

/**
 * @author Alexander Pelov
 */
public interface HistoryWriter {

    /**
     * Stores the passed record complying with the historyRecordStructure.
     * 
     * @param record
     *            The record to be added.
     * 
     * @throws IOException
     */
    void addRecord(HistoryRecord record) throws IOException;

    /**
     * Stores the passed propertyValues complying with the
     * historyRecordStructure.
     * 
     * @param propertyValues
     *            The values of the record.
     * 
     * @throws IOException
     */
    void addRecord(String[] propertyValues) throws IOException;

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
    void addRecord(String[] propertyValues, Date timestamp) throws IOException;

    /**
     * Updates a record by searching for record with idProperty which have idValue
     * and updating/creating the property with newValue.
     *
     * @param idProperty name of the id property
     * @param idValue value of the id property
     * @param property the property to change
     * @param newValue the value of the changed property.
     */
    public void updateRecord(String idProperty, String idValue,
        String property, String newValue) throws IOException;
}
