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
package net.java.sip.communicator.impl.history;

import static
    net.java.sip.communicator.service.history.HistoryService.DATE_FORMAT;

import java.io.*;
import java.security.*;
import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;

import org.jitsi.util.xml.XMLUtils;
import org.w3c.dom.*;

import com.google.common.xml.*;

/**
 * @author Alexander Pelov
 */
public class HistoryWriterImpl
    implements HistoryWriter
{
    /**
     * Maximum records per file.
     */
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
        this.addRecord(
            record.getPropertyNames(),
            record.getPropertyValues(),
            record.getTimestamp(),
            -1);
    }

    public void addRecord(String[] propertyValues)
        throws IOException
    {
        addRecord(structPropertyNames, propertyValues, new Date(), -1);
    }

    public void addRecord(String[] propertyValues, Date timestamp)
            throws IOException {
        this.addRecord(structPropertyNames, propertyValues, timestamp, -1);
    }

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
        throws IOException
    {
        addRecord(
            structPropertyNames,
            propertyValues,
            new Date(),
            maxNumberOfRecords);
    }

    /**
     * Adds new record to the current history document
     * when the record property name ends with _CDATA this is removed from the
     * property name and a CDATA text node is created to store the text value
     *
     * @param propertyNames String[]
     * @param propertyValues String[]
     * @param date Date
     * @param maxNumberOfRecords the maximum number of records to keep or
     * value of -1 to ignore this param.
     * @throws InvalidParameterException
     * @throws IOException
     */
    private void addRecord(String[] propertyNames,
                           String[] propertyValues,
                           Date date,
                           int maxNumberOfRecords)
        throws InvalidParameterException, IOException
    {
        // Synchronized to assure that two concurrent threads can insert records
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
                // if we have setting for max number of records,
                // check the number and when exceed them, remove the first one
                if( maxNumberOfRecords > -1
                    && this.currentDocElements >= maxNumberOfRecords)
                {
                    // lets remove the first one
                    removeFirstRecord(root);
                }

                Element elem = createRecord(
                    this.currentDoc, propertyNames, propertyValues, date);
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
     * Creates a record element for the supplied <tt>doc</tt> and populates it
     * with the property names from <tt>propertyNames</tt> and corresponding
     * values from <tt>propertyValues</tt>. The <tt>date</tt> will be used
     * for the record timestamp attribute.
     * @param doc the parent of the element.
     * @param propertyNames property names for the element
     * @param propertyValues values for the properties
     * @param date the of creation of the record
     * @return the newly created element.
     */
    private Element createRecord(Document doc,
                                 String[] propertyNames,
                                 String[] propertyValues,
                                 Date date)
    {
        Element elem = doc.createElement("record");
        SimpleDateFormat sdf
            = new SimpleDateFormat(DATE_FORMAT);
        elem.setAttribute("timestamp", sdf.format(date));

        for (int i = 0; i < propertyNames.length; i++)
        {
            String propertyName = propertyNames[i];

            if(propertyName.endsWith(CDATA_SUFFIX))
            {
                if (propertyValues[i] != null)
                {
                    propertyName =
                        propertyName.replaceFirst(CDATA_SUFFIX, "");

                    Element propertyElement = doc.createElement(propertyName);

                    Text value = doc.createCDATASection(
                        XmlEscapers.xmlContentEscaper().escape(
                            propertyValues[i].replaceAll("\0", " ")
                        ));
                    propertyElement.appendChild(value);

                    elem.appendChild(propertyElement);
                }
            }
            else
            {
                if (propertyValues[i] != null)
                {
                    Element propertyElement = doc.createElement(propertyName);

                    Text value = doc.createTextNode(
                        XmlEscapers.xmlContentEscaper().escape(
                            propertyValues[i].replaceAll("\0", " ")
                        ));
                    propertyElement.appendChild(value);

                    elem.appendChild(propertyElement);
                }
            }
        }

        return elem;
    }

    /**
     * Finds the oldest node by timestamp in current root and deletes it.
     * @param root where to search for records
     */
    private void removeFirstRecord(Node root)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        NodeList nodes = ((Element)root).getElementsByTagName("record");

        Node oldestNode = null;
        Date oldestTimeStamp = null;

        Node node;
        for (int i = 0; i < nodes.getLength(); i++)
        {
            node = nodes.item(i);

            Date timestamp;
            String ts
                = node.getAttributes().getNamedItem("timestamp").getNodeValue();
            try
            {
                timestamp = sdf.parse(ts);
            }
            catch (ParseException e)
            {
                timestamp = new Date(Long.parseLong(ts));
            }

            if(oldestNode == null
                || (oldestTimeStamp.after(timestamp)))
            {
                oldestNode = node;
                oldestTimeStamp = timestamp;
                continue;
            }

        }

        if(oldestNode != null)
            root.removeChild(oldestNode);
    }

    /**
     * Inserts a record from the passed <tt>propertyValues</tt> complying with
     * the current historyRecordStructure.
     * First searches for the file to use to import the record, as files hold
     * records with consecutive times and this fact is used for searching and
     * filtering records by date. This is why when inserting an old record
     * we need to insert it on the correct position.
     *
     * @param propertyValues The values of the record.
     * @param timestamp The timestamp of the record.
     * @param timestampProperty the property name for the timestamp of the
     * record
     *
     * @throws IOException
     */
    public void insertRecord(
            String[] propertyValues, Date timestamp, String timestampProperty)
        throws IOException
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        Iterator<String> fileIterator
            = HistoryReaderImpl.filterFilesByDate(
                    this.historyImpl.getFileList(), timestamp, null)
                .iterator();
        String filename = null;
        while (fileIterator.hasNext())
        {
            filename = fileIterator.next();

            Document doc = this.historyImpl.getDocumentForFile(filename);

            if(doc == null)
                continue;

            NodeList nodes = doc.getElementsByTagName("record");

            boolean changed = false;

            Node node;
            for (int i = 0; i < nodes.getLength(); i++)
            {
                node = nodes.item(i);

                Element idNode = XMLUtils.findChild(
                    (Element)node, timestampProperty);
                if(idNode == null)
                    continue;

                Node nestedNode = idNode.getFirstChild();
                if(nestedNode == null)
                    continue;

                // Get nested TEXT node's value
                String nodeValue = nestedNode.getNodeValue();

                Date nodeTimeStamp;
                try
                {
                    nodeTimeStamp = sdf.parse(nodeValue);
                }
                catch (ParseException e)
                {
                    nodeTimeStamp = new Date(Long.parseLong(nodeValue));
                }

                if(nodeTimeStamp.before(timestamp))
                    continue;

                Element newElem = createRecord(
                    doc, structPropertyNames, propertyValues, timestamp);

                doc.getFirstChild().insertBefore(newElem, node);

                changed = true;
                break;
            }

            if(changed)
            {
                // write changes
                synchronized (this.docWriteLock)
                {
                    this.historyImpl.writeFile(filename, doc);
                }

                // this prevents that the current writer, which holds
                // instance for the last document he is editing will not
                // override our last changes to the document
                if(filename.equals(this.currentFile))
                {
                    this.currentDoc = doc;
                }

                break;
            }
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
            Iterator<String> files = historyImpl.getFileList();

            String file = null;
            while (files.hasNext())
            {
                file = files.next();
            }

            if (file != null)
            {
                this.currentDoc = this.historyImpl.getDocumentForFile(file);
                this.currentFile = file;
                loaded = true;
            }

            // if something happened and file was not loaded
            // then we must create new one
            if(this.currentDoc == null)
            {
                loaded = false;
            }
        }

        if (!loaded)
        {
            this.currentFile = Long.toString(date.getTime());
            this.currentFile += ".xml";

            this.currentDoc = this.historyImpl.createDocument(this.currentFile);
        }

        // TODO: Assert: Assert.assertNonNull(this.currentDoc,
        // "There should be a current document created.");

        this.currentDocElements = this.currentDoc.getFirstChild()
                .getChildNodes().getLength();
    }

    /**
     * Updates a record by searching for record with idProperty which have
     * idValue and updating/creating the property with newValue.
     *
     * @param idProperty name of the id property
     * @param idValue value of the id property
     * @param property the property to change
     * @param newValue the value of the changed property.
     */
    public void updateRecord(String idProperty, String idValue,
            String property, String newValue)
        throws IOException
    {
        Iterator<String> fileIterator = this.historyImpl.getFileList();
        String filename = null;
        while (fileIterator.hasNext())
        {
            filename = fileIterator.next();

            Document doc = this.historyImpl.getDocumentForFile(filename);

            if(doc == null)
                continue;

            NodeList nodes = doc.getElementsByTagName("record");

            boolean changed = false;

            Node node;
            for (int i = 0; i < nodes.getLength(); i++)
            {
                node = nodes.item(i);

                Element idNode = XMLUtils.findChild((Element)node, idProperty);
                if(idNode == null)
                    continue;

                Node nestedNode = idNode.getFirstChild();
                if(nestedNode == null)
                    continue;

                // Get nested TEXT node's value
                String nodeValue = nestedNode.getNodeValue();

                if(!nodeValue.equals(idValue))
                    continue;

                Element changedNode =
                    XMLUtils.findChild((Element)node, property);

                if(changedNode != null)
                {
                    Node changedNestedNode = changedNode.getFirstChild();

                    changedNestedNode.setNodeValue(newValue);
                }
                else
                {
                    Element propertyElement = this.currentDoc
                        .createElement(property);

                    Text value = this.currentDoc
                        .createTextNode(newValue.replaceAll("\0", " "));
                    propertyElement.appendChild(value);

                    node.appendChild(propertyElement);
                }

                // change the timestamp, to reflect there was a change
                SimpleDateFormat sdf
                    = new SimpleDateFormat(DATE_FORMAT);
                ((Element)node).setAttribute("timestamp",
                    sdf.format(new Date()));

                changed = true;
                break;
            }

            if(changed)
            {
                // write changes
                synchronized (this.docWriteLock)
                {
                    this.historyImpl.writeFile(filename, doc);
                }

                // this prevents that the current writer, which holds
                // instance for the last document he is editing will not
                // override our last changes to the document
                if(filename.equals(this.currentFile))
                {
                    this.currentDoc = doc;
                }

                break;
            }
        }
    }

    /**
     * Updates history record using given <tt>HistoryRecordUpdater</tt> instance
     * to find which is the record to be updated and to get the new values for
     * the fields
     * @param updater the <tt>HistoryRecordUpdater</tt> instance.
     */
    public void updateRecord(HistoryRecordUpdater updater) throws IOException
    {
        Iterator<String> fileIterator = this.historyImpl.getFileList();
        String filename = null;
        while (fileIterator.hasNext())
        {
            filename = fileIterator.next();

            Document doc = this.historyImpl.getDocumentForFile(filename);

            if(doc == null)
                continue;

            NodeList nodes = doc.getElementsByTagName("record");

            boolean changed = false;

            Node node;
            for (int i = 0; i < nodes.getLength(); i++)
            {
                node = nodes.item(i);
                updater.setHistoryRecord(createHistoryRecordFromNode(node));
                if(!updater.isMatching())
                    continue;

                // change the timestamp, to reflect there was a change
                SimpleDateFormat sdf
                    = new SimpleDateFormat(DATE_FORMAT);
                ((Element)node).setAttribute("timestamp",
                    sdf.format(new Date()));

                Map<String, String> updates = updater.getUpdateChanges();
                for(String nodeName : updates.keySet())
                {
                    Element changedNode =
                        XMLUtils.findChild((Element)node, nodeName);

                    if(changedNode != null)
                    {
                        Node changedNestedNode = changedNode.getFirstChild();

                        changedNestedNode.setNodeValue(updates.get(nodeName));
                        changed = true;
                    }
                }
            }

            if(changed)
            {
                // write changes
                synchronized (this.docWriteLock)
                {
                    this.historyImpl.writeFile(filename, doc);
                }

                // this prevents that the current writer, which holds
                // instance for the last document he is editing will not
                // override our last changes to the document
                if(filename.equals(this.currentFile))
                {
                    this.currentDoc = doc;
                }

                break;
            }
        }
    }

    /**
     * Creates <tt>HistoryRecord</tt> instance from <tt>Node</tt> object.
     * @param node the node
     * @return the <tt>HistoryRecord</tt> instance
     */
    private HistoryRecord createHistoryRecordFromNode(Node node)
    {

        HistoryRecordStructure structure
            = historyImpl.getHistoryRecordsStructure();
        String propertyValues[] = new String[structure.getPropertyCount()];

        int i = 0;
        for(String propertyName : structure.getPropertyNames())
        {
            Element childNode = XMLUtils.findChild((Element)node, propertyName);
            if(childNode == null)
            {
                i++;
                continue;
            }
            propertyValues[i] = childNode.getTextContent();
            i++;
        }

        return new HistoryRecord(structure, propertyValues);
    }
}
