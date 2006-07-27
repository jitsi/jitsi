/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import java.io.*;
import java.util.*;

import org.osgi.framework.*;
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
public class MessageHistoryServiceImpl
    implements  MessageHistoryService,
                MessageListener,
                ServiceListener
{
    /**
     * The logger for this class.
     */
    private static Logger logger = Logger
            .getLogger(MessageHistoryServiceImpl.class);

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(
            new String[] { "dir", "msg_CDATA", "msgTyp", "enc", "uid", "sub" });

    private static final String SEARCH_FIELD = "msg";

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

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
                logger.error("Could not read history", e);
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
                logger.error("Could not read history", e);
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
                logger.error("Could not read history", e);
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
                logger.error("Could not read history", e);
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
                logger.error("Could not read history", e);
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
                logger.error("Could not read history", e);
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
                logger.error("Could not read history", e);
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

    /**
     * starts the service. Check the current registerd protocol providers
     * which supports BasicIM and adds message listener to them
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        logger.debug("Starting the meta contact list implementation.");
        this.bundleContext = bc;

        // start listening for newly register or removed protocol providers
        bc.addServiceListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            logger.debug("Found "
                         + protocolProviderRefs.length
                         + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
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
            logger.error("Could not add message to history", e);
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

            logger.debug("New protocol provider service implementing the "
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

            logger.debug("Protocol provider service: "
                    + protocolProvider.getProtocolName() + " unregistered.");
        }
    }

    /**
     * Set the configuration service.
     *
     * @param configurationService ConfigurationService
     */
    public void setConfigurationService(
            ConfigurationService configurationService) {
        synchronized (this.syncRoot_Config) {
            this.configurationService = configurationService;
            logger.debug("New configuration service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param configurationService ConfigurationService
     */
    public void unsetConfigurationService(
            ConfigurationService configurationService) {
        synchronized (this.syncRoot_Config) {
            if (this.configurationService == configurationService) {
                this.configurationService = null;
                logger.debug("Configuration service unregistered.");
            }
        }
    }

    /**
     * Set the configuration service.
     *
     * @param historyService HistoryService
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void setHistoryService(HistoryService historyService)
            throws IllegalArgumentException, IOException {
        synchronized (this.syncRoot_HistoryService) {
            this.historyService = historyService;

            logger.debug("New history service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param historyService HistoryService
     */
    public void unsetHistoryService(HistoryService historyService) {
        synchronized (this.syncRoot_HistoryService) {
            if (this.historyService == historyService) {
                this.historyService = null;

                logger.debug("History service unregistered.");
            }
        }
    }

    /**
     * When new protocol provider is registered we check
     * does it supports BasicIM and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService = bundleContext.getService(serviceEvent.getServiceReference());

        logger.trace("Received a service event for: " + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            logger.debug("Handling registration of a new Protocol Provider.");

            this.handleProviderAdded((ProtocolProviderService)sService);
        }
    }

    private void handleProviderAdded(
        ProtocolProviderService provider)
    {
        logger.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic im operation set
        OperationSetBasicInstantMessaging opSetIm
            = (OperationSetBasicInstantMessaging) provider
            .getSupportedOperationSets().get(
                OperationSetBasicInstantMessaging.class.getName());

        if (opSetIm != null)
        {
            opSetIm.addMessageListener(this);
        }
        else
        {
            logger.debug("Service did not have a im op. set.");
        }
    }

}
