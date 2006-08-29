/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import java.io.*;
import java.security.*;
import java.util.*;

import org.w3c.dom.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;

/**
 * @author Alexander Pelov
 */
public class HistoryWriterImpl implements HistoryWriter {

    public static final int MAX_RECORDS_PER_FILE = 150;
    private static final String CDATA_SUFFIX = "_CDATA";

    private Object docCreateLock = new Object();

    private Object docWriteLock = new Object();

    private HistoryImpl historyImpl;

    private String[] structPropertyNames;

    private Document currentDoc = null;

    private String currentFile = null;

    private int currentDocElements = -1;

    protected HistoryWriterImpl(HistoryImpl historyImpl)
    {
        this.historyImpl = historyImpl;

        HistoryRecordStructure struct = this.historyImpl
                .getHistoryRecordsStructure();
        this.structPropertyNames = struct.getPropertyNames();
    }

    public void addRecord(HistoryRecord record)
        throws IOException
    {
        this.addRecord(record.getPropertyNames(), record.getPropertyValues(),
                record.getTimestamp());
    }

    public void addRecord(String[] propertyValues)
        throws IOException
    {
        this.addRecord(structPropertyNames, propertyValues, new Date());
    }

    public void addRecord(String[] propertyValues, Date timestamp)
            throws IOException {
        this.addRecord(structPropertyNames, propertyValues, timestamp);
    }

    /**
     * Adds new record to the current history document
     * when the record property name ends with _CDATA this is removed from the
     * property name and a CDATA text node is created to store the text value
     *
     * @param propertyNames String[]
     * @param propertyValues String[]
     * @param date Date
     * @throws InvalidParameterException
     * @throws IOException
     */
    private void addRecord(String[] propertyNames,
                           String[] propertyValues,
                           Date date)
        throws InvalidParameterException, IOException
    {
        // Synchronized to assure that two concurent threads can insert records
        // safely.
        synchronized (this.docCreateLock)
        {
            if (this.currentDoc == null
                    || this.currentDocElements > MAX_RECORDS_PER_FILE)
            {
                this.createNewDoc(date, this.currentDoc == null);
            }
        }

        synchronized (this.currentDoc)
        {
            Node root = this.currentDoc.getFirstChild();
            synchronized (root)
            {
                Element elem = this.currentDoc.createElement("record");
                elem.setAttribute("timestamp", Long.toString(date.getTime()));

                for (int i = 0; i < propertyNames.length; i++)
                {
                    String propertyName = propertyNames[i];

                    if(propertyName.endsWith(CDATA_SUFFIX))
                    {
                        if (propertyValues[i] != null)
                        {
                            propertyName = propertyName.replaceFirst(CDATA_SUFFIX, "");

                            Element propertyElement = this.currentDoc
                                .createElement(propertyName);

                            Text value = this.currentDoc
                                .createCDATASection(propertyValues[i].replaceAll("\0", " "));
                            propertyElement.appendChild(value);

                            elem.appendChild(propertyElement);
                        }
                    }
                    else
                    {
                        if (propertyValues[i] != null)
                        {
                            Element propertyElement = this.currentDoc
                                .createElement(propertyName);

                            Text value = this.currentDoc
                                .createTextNode(propertyValues[i].replaceAll("\0", " "));
                            propertyElement.appendChild(value);

                            elem.appendChild(propertyElement);
                        }
                    }
                }

                root.appendChild(elem);
                this.currentDocElements++;
            }
        }

        // write changes
        synchronized (this.docWriteLock)
        {
            if(historyImpl.getHistoryServiceImpl().isCacheEnabled())
                this.historyImpl.writeFile(this.currentFile);
            else
                this.historyImpl.writeFile(this.currentFile, this.currentDoc);
        }
    }

    /**
     * If no file is currently loaded loads the last opened file. If it does not
     * exists or if the current file was set - create a new file.
     *
     * @param date Date
     * @param loadLastFile boolean
     */
    private void createNewDoc(Date date, boolean loadLastFile)
    {
        boolean loaded = false;

        if (loadLastFile)
        {
            Iterator files = historyImpl.getFileList();

            String file = null;
            while (files.hasNext())
            {
                file = (String) files.next();
            }

            if (file != null)
            {
                this.currentDoc = this.historyImpl.getDocumentForFile(file);
                this.currentFile = file;
                loaded = true;
            }
        }

        if (!loaded)
        {
            this.currentFile = Long.toString(date.getTime());
//            while (this.currentFile.length() < 8)
//            {
//                this.currentFile = "0" + this.currentFile;
//            }
            this.currentFile += ".xml";

            this.currentDoc = this.historyImpl.createDocument(this.currentFile);
        }

        // TODO: Assert: Assert.assertNonNull(this.currentDoc,
        // "There should be a current document created.");

        this.currentDocElements = this.currentDoc.getFirstChild()
                .getChildNodes().getLength();
    }

}
