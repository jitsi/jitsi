/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import java.util.*;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.records.*;

import org.w3c.dom.*;

/**
 * The <tt>InteractiveHistoryReaderImpl</tt> is an implementation of the
 * <tt>InteractiveHistoryReader</tt> interface. It allows to search in the
 * history in an interactive way, i.e. be able to cancel the search at any time
 * and track the results through a <tt>HistoryQueryListener</tt>.
 *
 * @author Yana Stamcheva
 */
public class InteractiveHistoryReaderImpl
    implements InteractiveHistoryReader
{
    /**
     * The <tt>HistoryImpl</tt> where this reader is registered.
     */
    private final HistoryImpl history;

    /**
     * Creates an instance of <tt>InteractiveHistoryReaderImpl</tt> by
     * specifying the corresponding <tt>history</tt> implementation.
     * @param history the corresponding <tt>HistoryImpl</tt> to read from
     */
    public InteractiveHistoryReaderImpl(HistoryImpl history)
    {
        this.history = history;
    }

    /**
     * Searches the history for all records containing the <tt>keyword</tt>.
     *
     * @param keyword the keyword to search for
     * @param field the field where to look for the keyword
     * @param recordCount limits the result to this record count
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public HistoryQuery findByKeyword(  String keyword,
                                        String field,
                                        int recordCount)
    {
        return findByKeywords(new String[]{keyword}, field, recordCount);
    }

    /**
     * Searches the history for all records containing all <tt>keywords</tt>.
     *
     * @param keywords array of keywords we search for
     * @param field the field where to look for the keyword
     * @param recordCount limits the result to this record count
     * @return the found records
     * @throws RuntimeException
     *             Thrown if an exception occurs during the execution of the
     *             query, such as internal IO error.
     */
    public HistoryQuery findByKeywords( String[] keywords,
                                        String field,
                                        int recordCount)
    {
        return find(null, null, keywords, field, false, recordCount);
    }

    /**
     * Finds the history results corresponding to the given criteria.
     * @param startDate the start date
     * @param endDate the end date
     * @param keywords an array of keywords to search for
     * @param field the field, where to search the keywords
     * @param caseSensitive indicates if the search should be case sensitive
     * @param resultCount the desired number of results
     * @return the <tt>HistoryQuery</tt> that could be used to track the results
     * or to cancel the search
     */
    private HistoryQuery find(  final Date startDate,
                                final Date endDate,
                                final String[] keywords,
                                final String field,
                                final boolean caseSensitive,
                                final int resultCount)
    {
        final HistoryQueryImpl query = new HistoryQueryImpl();

        new Thread()
        {
            public void run()
            {
                find(startDate, endDate, keywords, field, caseSensitive,
                        resultCount, query);
            }
        }.start();

        return query;
    }

    /**
     * Finds the history results corresponding to the given criteria.
     * @param startDate the start date
     * @param endDate the end date
     * @param keywords an array of keywords to search for
     * @param field the field, where to search the keywords
     * @param caseSensitive indicates if the search should be case sensitive
     * @param resultCount the desired number of results
     * @param query the query tracking the results
     */
    private void find(  Date startDate,
                        Date endDate,
                        String[] keywords,
                        String field,
                        boolean caseSensitive,
                        int resultCount,
                        HistoryQueryImpl query)
    {
        Vector<String> filelist
            = HistoryReaderImpl.filterFilesByDate(  history.getFileList(),
                                                    startDate, endDate);

        Iterator<String> fileIterator = filelist.iterator();
        while (fileIterator.hasNext() && resultCount > 0 && !query.isCanceled())
        {
            String filename = fileIterator.next();

            Document doc = history.getDocumentForFile(filename);

            if(doc == null)
                continue;

            NodeList nodes = doc.getElementsByTagName("record");

            Node node;
            for (int i = 0; i < nodes.getLength() && !query.isCanceled(); i++)
            {
                node = nodes.item(i);

                String ts = node.getAttributes().getNamedItem("timestamp")
                        .getNodeValue();

                Date timestamp = new Date(Long.parseLong(ts));

                if(HistoryReaderImpl.isInPeriod(timestamp, startDate, endDate))
                {
                    NodeList propertyNodes = node.getChildNodes();

                    HistoryRecord record =
                        HistoryReaderImpl
                            .filterByKeyword(propertyNodes, timestamp,
                                        keywords, field, caseSensitive);

                    if(record != null)
                    {
                        query.addHistoryRecord(record);
                        resultCount--;
                    }
                }
            }
        }

        if (query.isCanceled())
            query.setStatus(HistoryQueryStatusEvent.QUERY_CANCELED);
        else
            query.setStatus(HistoryQueryStatusEvent.QUERY_COMPLETED);
    }
}
