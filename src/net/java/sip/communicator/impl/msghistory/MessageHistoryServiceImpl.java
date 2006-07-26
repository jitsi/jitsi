

/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * @author Alexander Pelov
 * @author Damian Minkov
 */
public class MessageHistoryServiceImpl implements MessageHistoryService,
        MessageListener {

    /**
     * The logger for this class.
     */
    private static Logger log = Logger
            .getLogger(MessageHistoryServiceImpl.class);

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(
            new String[] { "dir", "msg_CDATA", "msgTyp", "enc", "uid", "sub" });

    private static final String SEARCH_FIELD = "msg";

    private ConfigurationService configurationService = null;

    private HistoryService historyService = null;

    private Object syncRoot_Config = new Object();

    private Object syncRoot_HistoryService = new Object();

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    public QueryResultSet findByStartDate(MetaContact contact, Date startDate)
        throws RuntimeException
    {
        Vector result = new Vector();

        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);

                Iterator recs = history.getReader().findByStartDate(startDate);
                while (recs.hasNext())
                {
                    result.add(recs.next());
                }
            } catch (IOException e)
            {
                log.error("Could not read history", e);
            }
        }

        return new DefaultQueryResultSet(result);
    }

    public QueryResultSet findByEndDate(MetaContact contact, Date endDate)
        throws RuntimeException
    {
        Vector result = new Vector();

        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);

                Iterator recs = history.getReader().findByEndDate(endDate);
                while (recs.hasNext())
                {
                    result.add(recs.next());
                }
            } catch (IOException e)
            {
                log.error("Could not read history", e);
            }
        }

        return new DefaultQueryResultSet(result);
    }

    public QueryResultSet findByPeriod(MetaContact contact, Date startDate, Date endDate)
        throws RuntimeException
    {
        Vector result = new Vector();

        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);

                Iterator recs = history.getReader().findByPeriod(startDate, endDate);
                while (recs.hasNext())
                {
                    result.add(recs.next());
                }
            } catch (IOException e)
            {
                log.error("Could not read history", e);
            }
        }

        return new DefaultQueryResultSet(result);
    }

    public QueryResultSet findByPeriod(MetaContact contact,
                                       Date startDate, Date endDate,
                                       String[] keywords)
        throws UnsupportedOperationException
    {
        Vector result = new Vector();

        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);

                Iterator recs = history.getReader().
                    findByPeriod(startDate, endDate, keywords, SEARCH_FIELD);
                while (recs.hasNext())
                {
                    result.add(recs.next());
                }
            } catch (IOException e)
            {
                log.error("Could not read history", e);
            }
        }

        return new DefaultQueryResultSet(result);
    }

    public QueryResultSet findByKeyword(MetaContact contact, String keyword)
        throws RuntimeException
    {
        Vector result = new Vector();

        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);

                Iterator recs = history.getReader().
                    findByKeyword(keyword, SEARCH_FIELD);
                while (recs.hasNext())
                {
                    result.add(recs.next());
                }
            } catch (IOException e)
            {
                log.error("Could not read history", e);
            }
        }

        return new DefaultQueryResultSet(result);
    }

    public QueryResultSet findByKeywords(MetaContact contact, String[] keywords)
        throws RuntimeException
    {
        Vector result = new Vector();

        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);

                Iterator recs = history.getReader().
                    findByKeywords(keywords, SEARCH_FIELD);
                while (recs.hasNext())
                {
                    result.add(recs.next());
                }
            } catch (IOException e)
            {
                log.error("Could not read history", e);
            }
        }

        return new DefaultQueryResultSet(result);
    }

    public QueryResultSet findLast(MetaContact contact, int count)
        throws RuntimeException
    {
        List result = new ArrayList();

        // too stupid but there is no such metod in the history service
        // to be implemented
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 10);
        Date startDate = c.getTime();

        Iterator iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact) iter.next();

            try
            {
                History history = this.getHistory(null, item);

                Iterator recs = history.getReader().findByStartDate(startDate);
                while (recs.hasNext())
                {
                    result.add(recs.next());
                }
            } catch (IOException e)
            {
                log.error("Could not read history", e);
            }
        }

        if(result.size() > count)
        {
            result = result.subList(result.size() - count, result.size());
        }

        return new DefaultQueryResultSet(new Vector(result));
    }

    private History getHistory(Contact localContact, Contact remoteContact)
            throws IOException {
        History retVal = null;

        String localId = localContact == null ? "default" : localContact
                .getAddress();
        String remoteId = remoteContact == null ? "default" : remoteContact
                .getAddress();

        HistoryID historyId = HistoryID.createFromID(new String[] { "messages",
                localId, remoteId });

        if (this.historyService.isHistoryExisting(historyId)) {
            retVal = this.historyService.getHistory(historyId);
        } else {
            retVal = this.historyService.createHistory(historyId,
                    recordStructure);
        }

        return retVal;
    }

    // //////////////////////////////////////////////////////////////////////////
    public void messageReceived(MessageReceivedEvent evt) {
        this.writeMessage("in", null, evt.getSourceContact(), evt
                .getSourceMessage(), evt.getTimestamp());
    }

    public void messageDelivered(MessageDeliveredEvent evt) {
        this.writeMessage("out", null, evt.getDestinationContact(), evt
                .getSourceMessage(), evt.getTimestamp());
    }

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {
    }

    private void writeMessage(String direction, Contact source,
            Contact destination, Message message, Date timestamp) {
        try {
            History history = this.getHistory(source, destination);
            HistoryWriter historyWriter = history.getWriter();
            historyWriter.addRecord(new String[] { direction,
                    message.getContent(), message.getContentType(),
                    message.getEncoding(), message.getMessageUID(),
                    message.getSubject() }, timestamp);
        } catch (IOException e) {
            log.error("Could not add message to history", e);
        }
    }

    // //////////////////////////////////////////////////////////////////////////
    public void addProtocolProvider(ProtocolProviderService protocolProvider) {
        Map operationSets = protocolProvider.getSupportedOperationSets();

        String key = OperationSetBasicInstantMessaging.class.getName();
        if (operationSets.containsKey(key)) {
            OperationSetBasicInstantMessaging basicIntantMessaging = (OperationSetBasicInstantMessaging) operationSets
                    .get(key);
            basicIntantMessaging.addMessageListener(this);

            log.debug("New protocol provider service implementing the "
                    + "OperationSetBasicInstantMessaging registered: "
                    + protocolProvider.getProtocolName()
                    + ". Listening for messages.");
        }
    }

    public void removeProtocolProvider(ProtocolProviderService protocolProvider) {
        Map operationSets = protocolProvider.getSupportedOperationSets();

        String key = OperationSetBasicInstantMessaging.class.getName();
        if (operationSets.containsKey(key)) {
            OperationSetBasicInstantMessaging basicIntantMessaging = (OperationSetBasicInstantMessaging) operationSets
                    .get(key);
            basicIntantMessaging.removeMessageListener(this);

            log.debug("Protocol provider service: "
                    + protocolProvider.getProtocolName() + " unregistered.");
        }
    }

    /**
     * Set the configuration service.
     *
     * @param configurationService
     */
    public void setConfigurationService(
            ConfigurationService configurationService) {
        synchronized (this.syncRoot_Config) {
            this.configurationService = configurationService;
            log.debug("New configuration service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param configurationService
     */
    public void unsetConfigurationService(
            ConfigurationService configurationService) {
        synchronized (this.syncRoot_Config) {
            if (this.configurationService == configurationService) {
                this.configurationService = null;
                log.debug("Configuration service unregistered.");
            }
        }
    }

    /**
     * Set the configuration service.
     *
     * @param historyService
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void setHistoryService(HistoryService historyService)
            throws IllegalArgumentException, IOException {
        synchronized (this.syncRoot_HistoryService) {
            this.historyService = historyService;

            log.debug("New history service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param historyService
     */
    public void unsetHistoryService(HistoryService historyService) {
        synchronized (this.syncRoot_HistoryService) {
            if (this.historyService == historyService) {
                this.historyService = null;

                log.debug("History service unregistered.");
            }
        }
    }

}
