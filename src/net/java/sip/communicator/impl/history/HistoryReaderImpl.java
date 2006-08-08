/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import java.util.*;

import org.jaxen.*;
import org.jaxen.dom.*;
import org.jaxen.saxpath.*;
import org.w3c.dom.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.util.*;

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
        TreeSet result = new TreeSet(new HistoryRecordComparator());

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

                result.add(record);
            }
        }

        return new OrderedQueryResultSet(result);
    }

    /**
     * Used to compare HistoryRecords
     * ant to be ordered in TreeSet
     */
    private class HistoryRecordComparator
        implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            if(o1 instanceof HistoryRecord && o2 instanceof HistoryRecord)
            {
                return ((HistoryRecord)o1).getTimestamp().
                    compareTo(((HistoryRecord)o2).getTimestamp());
            }
            else
                return 0;
        }
    }
}
