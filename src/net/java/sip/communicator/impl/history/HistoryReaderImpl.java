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
import net.java.sip.communicator.service.history.event.*;

/**
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public class HistoryReaderImpl implements HistoryReader {
    private static Logger logger = Logger.getLogger(HistoryReaderImpl.class);

    private HistoryImpl historyImpl;
    private Vector progressListeners = new Vector();

    protected HistoryReaderImpl(HistoryImpl historyImpl)
    {
        this.historyImpl = historyImpl;
    }

    public QueryResultSet findByStartDate(Date startDate)
            throws RuntimeException {
        String expr = "/history/record[@timestamp>" + startDate.getTime() + "]";

        return this.findByXpath(expr, startDate, null, new ProgressEvent(this, startDate, null));
    }

    public QueryResultSet findByEndDate(Date endDate)
        throws RuntimeException
    {
        String expr = "/history/record[@timestamp<" + endDate.getTime() + "]";

        return this.findByXpath(expr, null, endDate,
                                new ProgressEvent(this, null, endDate, null, null));
    }

    public QueryResultSet findByPeriod(Date startDate, Date endDate)
            throws RuntimeException {
        String expr = "/history/record[@timestamp>" + startDate.getTime() + "]"
                + "[@timestamp<" + endDate.getTime() + "]";

        return this.findByXpath(expr, startDate, endDate,
                                new ProgressEvent(this, startDate, endDate, null, null));
    }

    public QueryResultSet findByKeyword(String keyword, String field)
        throws RuntimeException
    {
        return findByKeywords(new String[] { keyword }, field);
    }

    public QueryResultSet findByKeywords(String[] keywords, String field)
            throws RuntimeException {

        String expr = "/history/record";
        for (int i = 0; i < keywords.length; i++)
        {
            expr += "[contains(" + field + "/text(),'" + keywords[i] + "')]";
        }

        return this.findByXpath(expr, null, null, new ProgressEvent(this, null, null, null, keywords));
    }

    public QueryResultSet findByPeriod(Date startDate, Date endDate,
            String[] keywords, String field) throws UnsupportedOperationException
    {
        String expr = "/history/record[@timestamp>" + startDate.getTime() + "]"
                + "[@timestamp<" + endDate.getTime() + "]";
        for (int i = 0; i < keywords.length; i++)
        {
            expr += "[contains(" + field + "/text(),'" + keywords[i] + "')]";
        }

        return this.findByXpath(expr, startDate, endDate,
            new ProgressEvent(this, startDate, endDate, null, keywords));
    }

    public BidirectionalIterator bidirectionalIterator()
    {
        String expr = "/history/record";

        return this.findByXpath(expr, null, null, new ProgressEvent(this, null, null, null, null));
    }

    public Iterator iterator()
    {
        return this.bidirectionalIterator();
    }


    private QueryResultSet findByXpath(String xpathExpression,
                                       Date startDate, Date endDate,
                                       ProgressEvent progressEvent)
    {
        TreeSet result = new TreeSet(new HistoryRecordComparator());

        Vector filelist =
            filterFilesByDate(this.historyImpl.getFileList(), startDate, endDate);

        int currentProgress = HistorySearchProgressListener.PROGRESS_MINIMUM_VALUE;
        int fileProgressStep = HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE;

        if(filelist.size() != 0)
            fileProgressStep =
                HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE / filelist.size();

        // start progress - minimum value
        fireProgressStateChanged(progressEvent, HistorySearchProgressListener.PROGRESS_MINIMUM_VALUE);

        Navigator navigator = DocumentNavigator.getInstance();
        XPath xpath;

        try
        {
            xpath = navigator.parseXPath(xpathExpression);
        } catch (SAXPathException e)
        {
            throw new RuntimeException(e);
        }

        Iterator fileIterator = filelist.iterator();
        while (fileIterator.hasNext())
        {
            String filename = (String) fileIterator.next();

            Document doc = this.historyImpl.getDocumentForFile(filename);

            List nodes;
            try {
                nodes = xpath.selectNodes(doc);
            }
            catch (JaxenException e)
            {
                throw new RuntimeException(e);
            }

            int nodesProgressStep = fileProgressStep;

            if(nodes.size() != 0)
                nodesProgressStep = fileProgressStep / nodes.size();

            Iterator i = nodes.iterator();
            while (i.hasNext())
            {
                Node node = (Node) i.next();

                NodeList propertyNodes = node.getChildNodes();

                String ts = node.getAttributes().getNamedItem("timestamp")
                        .getNodeValue();
                Date timestamp = new Date(Long.parseLong(ts));

                ArrayList nameVals = new ArrayList();

                int len = propertyNodes.getLength();
                for (int j = 0; j < len; j++)
                {
                    Node propertyNode = propertyNodes.item(j);
                    if (propertyNode.getNodeType() == Node.ELEMENT_NODE)
                    {
                        nameVals.add(propertyNode.getNodeName());
                        // Get nested TEXT node's value
                        nameVals.add(propertyNode.getFirstChild()
                                .getNodeValue());
                    }
                }

                String[] propertyNames = new String[nameVals.size() / 2];
                String[] propertyValues = new String[propertyNames.length];
                for (int j = 0; j < propertyNames.length; j++)
                {
                    propertyNames[j] = (String) nameVals.get(j * 2);
                    propertyValues[j] = (String) nameVals.get(j * 2 + 1);
                }

                HistoryRecord record = new HistoryRecord(propertyNames,
                        propertyValues, timestamp);

                result.add(record);

                currentProgress += nodesProgressStep;
                fireProgressStateChanged(progressEvent, currentProgress);
            }
        }

        // end progress - maximum value
        fireProgressStateChanged(progressEvent, HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE);

        return new OrderedQueryResultSet(result);
    }

    /**
     * Used to limit the files if any starting or ending date exist
     * So only few files to be searched.
     *
     * Start or end date must not be equals to null
     *
     * @param filelist Iterator
     * @param startDate Date
     * @param endDate Date
     * @return Iterator
     */
    private Vector filterFilesByDate(Iterator filelist, Date startDate, Date endDate)
    {
        if(startDate == null && endDate == null)
        {
            // no filtering needed then just return the same list
            Vector result = new Vector();
            while (filelist.hasNext())
            {
                result.add(filelist.next());
            }
            return result;
        }
        // first convert all files to long
        TreeSet files = new TreeSet();
        while (filelist.hasNext())
        {
            String filename = (String)filelist.next();

            files.add(
                new Long(filename.substring(0, filename.length() - 4)));
        }

        Vector resultAsLong = new Vector();

        // if there is no startDate limit only to end date
        if(startDate == null)
        {
            Long endLong = new Long(endDate.getTime());
            files.add(endLong);

            resultAsLong.addAll(files.subSet(files.first(), endLong));
            resultAsLong.remove(endLong);
        }
        else if(endDate == null)
        {
            // end date is null get all the inclusive the one record before the startdate
            Long startLong = new Long(startDate.getTime());
            files.add(startLong);
            resultAsLong.addAll(files.subSet(startLong, files.last()));

            resultAsLong.add(files.last());

            // here we must get and the element before startLong
            resultAsLong.add(files.subSet(files.first(), startLong).last());

            resultAsLong.remove(startLong);
        }
        else
        {
            // if both are present we must return all the elements between
            // the two dates and the one before the start date
            Long startLong = new Long(startDate.getTime());
            Long endLong = new Long(endDate.getTime());
            files.add(startLong);
            files.add(endLong);

            resultAsLong.addAll(files.subSet(startLong, endLong));

            // here we must get and the element before startLong
            SortedSet theFirstToStart = files.subSet(files.first(), startLong);
            if(!theFirstToStart.isEmpty())
                resultAsLong.add(theFirstToStart.last());

            resultAsLong.remove(startLong);
            resultAsLong.remove(endLong);
        }

        Vector result = new Vector();

        Iterator iter = resultAsLong.iterator();
        while (iter.hasNext())
        {
            Long item = (Long) iter.next();
            result.add(item.toString() + ".xml");
        }

        return result;
    }

    private void fireProgressStateChanged(ProgressEvent event, int progress)
    {
        event.setProgress(progress);
        Iterator iter = progressListeners.iterator();
        while (iter.hasNext())
        {
            HistorySearchProgressListener item = (HistorySearchProgressListener) iter.next();
            item.progressChanged(event);
        }
    }

    public void addSearchProgressListener(HistorySearchProgressListener
                                          listener)
    {
        progressListeners.add(listener);
    }

    public void removeSearchProgressListener(HistorySearchProgressListener
                                             listener)
    {
        progressListeners.remove(listener);
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
