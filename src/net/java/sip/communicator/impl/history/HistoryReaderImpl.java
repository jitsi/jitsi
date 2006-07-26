/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import net.java.sip.communicator.service.history.BidirectionalIterator;
import net.java.sip.communicator.service.history.HistoryReader;
import net.java.sip.communicator.service.history.QueryResultSet;
import net.java.sip.communicator.service.history.DefaultQueryResultSet;
import net.java.sip.communicator.service.history.records.HistoryRecord;

import org.jaxen.JaxenException;
import org.jaxen.Navigator;
import org.jaxen.XPath;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.saxpath.SAXPathException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import net.java.sip.communicator.util.Logger;

/**
 * @author Alexander Pelov
 */
public class HistoryReaderImpl implements HistoryReader {
    private static Logger logger = Logger.getLogger(HistoryReaderImpl.class);

    private HistoryImpl historyImpl;

    protected HistoryReaderImpl(HistoryImpl historyImpl) {
        this.historyImpl = historyImpl;
    }

    public QueryResultSet findByStartDate(Date startDate)
            throws RuntimeException {
        String expr = "/history/record[@timestamp>" + startDate.getTime() + "]";

        return this.findByXpath(expr);
    }

    public QueryResultSet findByEndDate(Date endDate) throws RuntimeException {
        String expr = "/history/record[@timestamp<" + endDate.getTime() + "]";

        return this.findByXpath(expr);
    }

    public QueryResultSet findByPeriod(Date startDate, Date endDate)
            throws RuntimeException {
        String expr = "/history/record[@timestamp>" + startDate.getTime() + "]"
                + "[@timestamp<" + endDate.getTime() + "]";

        return this.findByXpath(expr);
    }

    public QueryResultSet findByKeyword(String keyword, String field) throws RuntimeException {
        return findByKeywords(new String[] { keyword }, field);
    }

    public QueryResultSet findByKeywords(String[] keywords, String field)
            throws RuntimeException {

        String expr = "/history/record";
        for (int i = 0; i < keywords.length; i++) {
            expr += "[contains(" + field + "/text(),'" + keywords[i] + "')]";
        }

        return this.findByXpath(expr);
    }

    public QueryResultSet findByPeriod(Date startDate, Date endDate,
            String[] keywords, String field) throws UnsupportedOperationException {
        String expr = "/history/record[@timestamp>" + startDate.getTime() + "]"
                + "[@timestamp<" + endDate.getTime() + "]";
        for (int i = 0; i < keywords.length; i++) {
            expr += "[contains(" + field + "/text(),'" + keywords[i] + "')]";
        }

        return this.findByXpath(expr);
    }

    public BidirectionalIterator bidirectionalIterator() {
        String expr = "/history/record";

        return this.findByXpath(expr);
    }

    public Iterator iterator() {
        return this.bidirectionalIterator();
    }

    private QueryResultSet findByXpath(String xpathExpression) {
        Vector vect = new Vector();

        Iterator filelist = this.historyImpl.getFileList();

        Navigator navigator = DocumentNavigator.getInstance();
        XPath xpath;

        try {
            xpath = navigator.parseXPath(xpathExpression);
        } catch (SAXPathException e) {
            throw new RuntimeException(e);
        }

        while (filelist.hasNext()) {
            String filename = (String) filelist.next();

            Document doc = this.historyImpl.getDocumentForFile(filename);

            List nodes;
            try {
                nodes = xpath.selectNodes(doc);
            } catch (JaxenException e) {
                throw new RuntimeException(e);
            }

            Iterator i = nodes.iterator();
            while (i.hasNext()) {
                Node node = (Node) i.next();

                NodeList propertyNodes = node.getChildNodes();

                String ts = node.getAttributes().getNamedItem("timestamp")
                        .getNodeValue();
                Date timestamp = new Date(Long.parseLong(ts));

                ArrayList nameVals = new ArrayList();

                int len = propertyNodes.getLength();
                for (int j = 0; j < len; j++) {
                    Node propertyNode = propertyNodes.item(j);
                    if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                        nameVals.add(propertyNode.getNodeName());
                        // Get nested TEXT node's value
                        nameVals.add(propertyNode.getFirstChild()
                                .getNodeValue());
                    }
                }

                String[] propertyNames = new String[nameVals.size() / 2];
                String[] propertyValues = new String[propertyNames.length];
                for (int j = 0; j < propertyNames.length; j++) {
                    propertyNames[j] = (String) nameVals.get(j * 2);
                    propertyValues[j] = (String) nameVals.get(j * 2 + 1);
                }

                HistoryRecord record = new HistoryRecord(propertyNames,
                        propertyValues, timestamp);

                vect.add(record);
            }
        }

        return new DefaultQueryResultSet(vect);

    }
}
