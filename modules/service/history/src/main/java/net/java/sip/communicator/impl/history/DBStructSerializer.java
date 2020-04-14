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

import java.io.*;
import java.text.*;
import java.util.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;

import org.jitsi.util.xml.XMLUtils;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 *
 * @author Alexander Pelov
 */
public class DBStructSerializer {

    private HistoryServiceImpl historyService;

    /**
     * Constructor.
     *
     * @param historyService the history service
     */
    public DBStructSerializer(HistoryServiceImpl historyService)
    {
        this.historyService = historyService;
    }

    /**
     * Write the history.
     *
     * @param dbDatFile the database file
     * @param history the history to write
     * @throws IOException if write failed for any reason
     */
    public void writeHistory(File dbDatFile, History history)
            throws IOException {
        DocumentBuilder builder = this.historyService.getDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("dbstruct");
        root.setAttribute("version", "1.0");

        Element structure = this.createStructureTag(doc, history
                .getHistoryRecordsStructure());
        Element id = this.createIDTag(doc, history.getID());

        root.appendChild(structure);
        root.appendChild(id);

        doc.appendChild(root);

        XMLUtils.writeXML(doc, dbDatFile);
    }

    private Element createIDTag(Document doc, HistoryID historyID)
    {
        Element idroot = doc.createElement("id");
        Element current = idroot;

        String[] idelements = historyID.getID();
        for (int i = 0; i < idelements.length; i++)
        {
            Element idnode = doc.createElement("component");
            idnode.setAttribute("value", idelements[i]);

            current.appendChild(idnode);
            current = idnode;
        }

        return idroot;
    }

    private Element createStructureTag(Document doc,
            HistoryRecordStructure recordStructure)
    {
        Element structure = doc.createElement("structure");
        String[] propertyNames = recordStructure.getPropertyNames();
        int count = recordStructure.getPropertyCount();
        for (int i = 0; i < count; i++)
        {
            Element property = doc.createElement("property");
            property.setAttribute("name", propertyNames[i]);
            structure.appendChild(property);
        }

        return structure;
    }

    /**
     * This method parses an XML file, and returns a History object created with
     * the information from it. The parsing is non-validating, so if a malformed
     * XML is passed the results are undefined. The file should be with the
     * following structure:
     *
     * <dbstruct version="1.0"> <id value="idcomponent1"> <id
     * value="idcomponent2"> <id value="idcomponent3"/> </id> </id>
     *
     * <structure> <property name="propertyName" type="textType" /> <property
     * name="propertyName" type="textType" /> <property name="propertyName"
     * type="textType" /> </structure> </dbstruct>
     *
     * @param dbDatFile
     *            The file to be parsed.
     * @return A History object corresponding to this dbstruct file.
     * @throws SAXException
     *             Thrown if an error occurs during XML parsing.
     * @throws IOException
     *             Thrown if an IO error occurs.
     * @throws ParseException
     *             Thrown if there is error in the XML data format.
     */
    public History loadHistory(File dbDatFile) throws SAXException,
            IOException, ParseException {
        Document doc = historyService.parse(dbDatFile);

        Node root = doc.getFirstChild();
        HistoryID id = loadID(root);
        HistoryRecordStructure structure = loadStructure(root);

        return new HistoryImpl(id, dbDatFile.getParentFile(), structure,
                historyService);
    }

    /**
     * This method parses a "structure" tag and returns the corresponging
     * HistoryRecordStructure.
     *
     * @throws ParseException
     *             Thrown if there is no structure tag.
     */
    private HistoryRecordStructure loadStructure(Node root)
            throws ParseException {
        Element structNode = findElement(root, "structure");
        if (structNode == null)
        {
            throw new ParseException("There is no structure tag defined!", 0);
        }

        NodeList nodes = structNode.getChildNodes();
        int count = nodes.getLength();

        ArrayList<String> propertyNames = new ArrayList<String>(count);

        for (int i = 0; i < count; i++)
        {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && "property".equals(node.getNodeName()))
            {
                Element parameter = (Element) node;
                String paramName = parameter.getAttribute("name");

                if (paramName == null)
                {
                    continue;
                }

                propertyNames.add(paramName);
            }
        }

        String[] names = new String[propertyNames.size()];
        propertyNames.toArray(names);

        return new HistoryRecordStructure(names);
    }

    private HistoryID loadID(Node parent) throws ParseException
    {
        Element idnode = findElement(parent, "id");
        ArrayList<String> al = loadID(new ArrayList<String>(), idnode);

        String[] id = new String[al.size()];
        al.toArray(id);

        return HistoryID.createFromID(id);
    }

    private ArrayList<String> loadID(ArrayList<String> loadedIDs, Node parent)
            throws ParseException {
        Element node = findElement(parent, "component");
        if (node != null)
        {
            String idValue = node.getAttribute("value");
            if (idValue != null)
            {
                loadedIDs.add(idValue);
            } else {
                throw new ParseException(
                        "There is an ID object without value.", 0);
            }
        }
        else
        {
            // no more nodes
            return loadedIDs;
        }

        return loadID(loadedIDs, node);
    }

    /**
     * This method seraches through all children of a given node and returns the
     * first with the name matching the given one. If no node is found, null is
     * returned.
     */
    private Element findElement(Node parent, String name)
    {
        Element retVal = null;
        NodeList nodes = parent.getChildNodes();
        int count = nodes.getLength();

        for (int i = 0; i < count; i++)
        {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && name.equals(node.getNodeName()))
            {
                retVal = (Element) node;
                break;
            }
        }
        return retVal;
    }
}
