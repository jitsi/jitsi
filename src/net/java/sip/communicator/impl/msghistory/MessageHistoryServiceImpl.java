/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.service.history.History;
import net.java.sip.communicator.service.history.HistoryID;
import net.java.sip.communicator.service.history.HistoryService;
import net.java.sip.communicator.service.history.HistoryWriter;
import net.java.sip.communicator.service.history.QueryResultSet;
import net.java.sip.communicator.service.history.records.HistoryRecordStructure;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.util.Logger;

public class MessageHistoryServiceImpl implements MessageHistoryService, MessageListener {

    /**
     * The logger for this class.
     */
    private static Logger log = Logger
        .getLogger(MessageHistoryServiceImpl.class); 
        
	private static HistoryRecordStructure recordStructure = new HistoryRecordStructure(
			new String[]{"dir",	"msg", "msgTyp", "enc",	"uid", "sub"});

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
    
	public QueryResultSet findByStartDate(Date startDate)
			throws RuntimeException {
		return null;
//		return this.historyReader.findByStartDate(startDate);
	}

	public QueryResultSet findByEndDate(Date endDate) throws RuntimeException {
		return null;
//		return this.historyReader.findByEndDate(endDate);
	}

	public QueryResultSet findByPeriod(Date startDate, Date endDate)
			throws RuntimeException {
		return null;
//		return this.historyReader.findByPeriod(startDate, endDate);
	}

	public QueryResultSet findByKeyword(String keyword) throws RuntimeException {
		return null;
//		return this.historyReader.findByKeyword(keyword);
	}

	public QueryResultSet findByKeywords(String[] keywords)
			throws RuntimeException {
		return null;
//		return this.historyReader.findByKeywords(keywords);
	}

	public QueryResultSet findByText(Date startDate, Date endDate,
			String[] keywords) throws UnsupportedOperationException {
		return null;
//		return this.historyReader.findByText(startDate, endDate, keywords);
	}
	
	private History getHistory(Contact localContact, Contact remoteContact) 
	throws IOException {
		History retVal = null;
		
		String localId = localContact == null ? "default" : localContact.getAddress();
		String remoteId = remoteContact == null ? "default" : remoteContact.getAddress();

		HistoryID historyId = HistoryID.createFromID(new String[] {
				"messages",
				localId,
				remoteId
		});
		
        if(this.historyService.isHistoryExisting(historyId)) {
        	retVal = this.historyService.getHistory(historyId);
        } else {
        	retVal = this.historyService.createHistory(historyId, 
        			recordStructure);
        }
        
        return retVal;
	}

    ////////////////////////////////////////////////////////////////////////////
	public void messageReceived(MessageReceivedEvent evt) {
		this.writeMessage("in", null, evt.getSourceContact(), 
				evt.getSourceMessage(), evt.getTimestamp());
	}
	public void messageDelivered(MessageDeliveredEvent evt) {
		this.writeMessage("out", null, evt.getDestinationContact(), 
				evt.getSourceMessage(), evt.getTimestamp());
	}
	public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {
	}
	
	private void writeMessage(String direction, Contact source, Contact destination, 
			Message message, Date timestamp) {		
		try {
			History history = this.getHistory(source, destination);
			HistoryWriter historyWriter = history.getWriter();
			historyWriter.addRecord(
					new String[]{
							direction, 
							message.getContent(), 
							message.getContentType(),
							message.getEncoding(), 
							message.getMessageUID(), 
							message.getSubject()
					}, 
					timestamp);
		} catch (IOException e) {
			log.error("Could not add message to history", e);
		}
	}

    ////////////////////////////////////////////////////////////////////////////
	public void addProtocolProvider(ProtocolProviderService protocolProvider) {
		Map operationSets = protocolProvider.getSupportedOperationSets();
		
		String key = OperationSetBasicInstantMessaging.class.getName();
		if(operationSets.containsKey(key)) {
			OperationSetBasicInstantMessaging basicIntantMessaging =
				(OperationSetBasicInstantMessaging)operationSets.get(key);
			basicIntantMessaging.addMessageListener(this);
			
            log.debug("New protocol provider service implementing the " +
            		"OperationSetBasicInstantMessaging registered: " +
            		protocolProvider.getProtocolName() +
            		". Listening for messages.");			
		}
	}
	
	public void removeProtocolProvider(ProtocolProviderService protocolProvider) {
		Map operationSets = protocolProvider.getSupportedOperationSets();
		
		String key = OperationSetBasicInstantMessaging.class.getName();
		if(operationSets.containsKey(key)) {
			OperationSetBasicInstantMessaging basicIntantMessaging =
				(OperationSetBasicInstantMessaging)operationSets.get(key);
			basicIntantMessaging.removeMessageListener(this);
			
            log.debug("Protocol provider service: " +
            		protocolProvider.getProtocolName() +
            		" unregistered.");			
		}
	}
	
    /**
     * Set the configuration service.
     *
     * @param configurationService
     */
    public void setConfigurationService(
        ConfigurationService configurationService)
    {
        synchronized (this.syncRoot_Config)
        {
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
        ConfigurationService configurationService)
    {
        synchronized (this.syncRoot_Config)
        {
            if (this.configurationService == configurationService)
            {
                this.configurationService = null;
                log.debug("Configuration service unregistered.");
            }
        }
    }

    /**
     * Set the configuration service.
     *
     * @param configurationService
     * @throws IOException 
     * @throws IllegalArgumentException 
     */
    public void setHistoryService(
        HistoryService historyService) throws IllegalArgumentException, IOException
    {
        synchronized (this.syncRoot_HistoryService)
        {
            this.historyService = historyService;
                        
            log.debug("New history service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param configurationService
     */
    public void unsetHistoryService(
    		HistoryService historyService)
    {
        synchronized (this.syncRoot_HistoryService)
        {
            if (this.historyService == historyService)
            {
                this.historyService = null;
                                
                log.debug("History service unregistered.");
            }
        }
    }
    
}
