/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.callhistory;

import java.io.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.callhistory.event.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.history.event.ProgressEvent;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The Call History Service stores info about the calls made.
 * Logs calls info for all protocol providers that support basic telephony
 * (i.e. those that implement OperationSetBasicTelephony).
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class CallHistoryServiceImpl
    implements  CallHistoryService,
                CallListener,
                ServiceListener
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(CallHistoryServiceImpl.class);

    private static String[] STRUCTURE_NAMES =
        new String[] { "callStart", "callEnd", "dir", "callParticipantIDs",
            "callParticipantStart", "callParticipantEnd","callParticipantStates" };

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(STRUCTURE_NAMES);

    private static final String DELIM = ",";

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    private HistoryService historyService = null;

    private Object syncRoot_HistoryService = new Object();

    private final Map<CallHistorySearchProgressListener, SearchProgressWrapper> progressListeners =
        new Hashtable<CallHistorySearchProgressListener, SearchProgressWrapper>();

    private final List<CallRecordImpl> currentCallRecords =
        new Vector<CallRecordImpl>();

    private final CallChangeListener historyCallChangeListener =
        new HistoryCallChangeListener();

    public HistoryService getHistoryService()
    {
        return historyService;
    }

    /**
     * Returns all the calls made by all the contacts
     * in the supplied metacontact after the given date
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param startDate Date the start date of the calls
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByStartDate(MetaContact contact, Date startDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls made after the given date
     *
     * @param startDate Date the start date of the calls
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByStartDate(Date startDate)
    {
        TreeSet<CallRecord> result =
            new TreeSet<CallRecord>(new CallRecordComparator());
        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            HistoryReader reader = history.getReader();
            addHistorySearchProgressListeners(reader, 1);
            QueryResultSet rs = reader.findByStartDate(startDate);
            while (rs.hasNext())
            {
                HistoryRecord hr = (HistoryRecord) rs.next();
                result.add(convertHistoryRecordToCallRecord(hr));
            }
            removeHistorySearchProgressListeners(reader);
        }
        catch (IOException ex)
        {
            logger.error("Could not read history", ex);
        }

        return result;
    }

    /**
     * Returns all the calls made by all the contacts
     * in the supplied metacontact before the given date
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByEndDate(MetaContact contact, Date endDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls made before the given date
     *
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByEndDate(Date endDate) throws RuntimeException
    {
        TreeSet<CallRecord> result = new TreeSet<CallRecord>(new CallRecordComparator());
        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            HistoryReader reader = history.getReader();
            addHistorySearchProgressListeners(reader, 1);
            QueryResultSet rs = reader.findByEndDate(endDate);
            while (rs.hasNext())
            {
                HistoryRecord hr = (HistoryRecord) rs.next();
                result.add(convertHistoryRecordToCallRecord(hr));
            }
            removeHistorySearchProgressListeners(reader);
        }
        catch (IOException ex)
        {
            logger.error("Could not read history", ex);
        }

        return result;
    }

    /**
     * Returns all the calls made by all the contacts
     * in the supplied metacontact between the given dates
     *
     * @param contact MetaContact
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the conversations
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByPeriod(MetaContact contact, Date startDate, Date endDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls made between the given dates
     *
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the conversations
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByPeriod(Date startDate, Date endDate) throws
        RuntimeException
    {
        TreeSet<CallRecord> result = new TreeSet<CallRecord>(new CallRecordComparator());
        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            HistoryReader reader = history.getReader();
            addHistorySearchProgressListeners(reader, 1);
            QueryResultSet rs = reader.findByPeriod(startDate, endDate);
            while (rs.hasNext())
            {
                HistoryRecord hr = (HistoryRecord) rs.next();
                result.add(convertHistoryRecordToCallRecord(hr));
            }
            removeHistorySearchProgressListeners(reader);
        }
        catch (IOException ex)
        {
            logger.error("Could not read history", ex);
        }

        return result;
    }

    /**
     * Returns the supplied number of calls by all the contacts
     * in the supplied metacontact
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param count calls count
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findLast(MetaContact contact, int count)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns the supplied number of calls made
     *
     * @param count calls count
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findLast(int count) throws RuntimeException
    {
        TreeSet<CallRecord> result = new TreeSet<CallRecord>(new CallRecordComparator());
        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            QueryResultSet rs = history.getReader().findLast(count);
            while (rs.hasNext())
            {
                HistoryRecord hr = (HistoryRecord) rs.next();
                result.add(convertHistoryRecordToCallRecord(hr));
            }
        }
        catch (IOException ex)
        {
            logger.error("Could not read history", ex);
        }

        return result;
    }

    /**
     * Find the calls made by the supplied participant address
     * @param address String the address of the participant
     * @return Collection of CallRecords with CallParticipantRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByParticipant(String address)
        throws RuntimeException
    {
        TreeSet<CallRecord> result = new TreeSet<CallRecord>(new CallRecordComparator());
        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            HistoryReader reader = history.getReader();
            addHistorySearchProgressListeners(reader, 1);
            QueryResultSet rs =
                reader.findByKeyword(address, "callParticipantIDs");
            while (rs.hasNext())
            {
                HistoryRecord hr = (HistoryRecord) rs.next();
                result.add(convertHistoryRecordToCallRecord(hr));
            }
            removeHistorySearchProgressListeners(reader);
        }
        catch (IOException ex)
        {
            logger.error("Could not read history", ex);
        }

        return result;
    }

    /**
     * Returns the history by specified local and remote contact
     * if one of them is null the default is used
     *
     * @param localContact Contact
     * @param remoteContact Contact
     * @return History
     * @throws IOException
     */
    private History getHistory(Contact localContact, Contact remoteContact)
            throws IOException {
        History retVal = null;

        String localId = localContact == null ? "default" : localContact
                .getAddress();
        String remoteId = remoteContact == null ? "default" : remoteContact
                .getAddress();

        HistoryID historyId = HistoryID.createFromRawID(
            new String[] {  "callhistory",
                            localId,
                            remoteId });

        if (this.historyService.isHistoryExisting(historyId))
        {
            retVal = this.historyService.getHistory(historyId);
        } else {
            retVal = this.historyService.createHistory(historyId,
                    recordStructure);
        }

        return retVal;
    }

    /**
     * Used to convert HistoryRecord in CallReord and CallParticipantRecord
     * which are returned by the finder methods
     *
     * @param hr HistoryRecord
     * @return Object CallRecord
     */
    private CallRecord convertHistoryRecordToCallRecord(HistoryRecord hr)
    {
        CallRecordImpl result = new CallRecordImpl();

        List<String> callParticipantIDs = null;
        List<String> callParticipantStart = null;
        List<String> callParticipantEnd = null;
        List<CallParticipantState> callParticipantStates = null;

        // History structure
        // 0 - callStart
        // 1 - callEnd
        // 2 - dir
        // 3 - callParticipantIDs
        // 4 - callParticipantStart
        // 5 - callParticipantEnd

        for (int i = 0; i < hr.getPropertyNames().length; i++)
        {
            String propName = hr.getPropertyNames()[i];
            String value = hr.getPropertyValues()[i];

            if(propName.equals(STRUCTURE_NAMES[0]))
                result.setStartTime(new Date(Long.parseLong(value)));
            else if(propName.equals(STRUCTURE_NAMES[1]))
                result.setEndTime(new Date(Long.parseLong(value)));
            else if(propName.equals(STRUCTURE_NAMES[2]))
                result.setDirection(value);
            else if(propName.equals(STRUCTURE_NAMES[3]))
                callParticipantIDs = getCSVs(value);
            else if(propName.equals(STRUCTURE_NAMES[4]))
                callParticipantStart = getCSVs(value);
            else if(propName.equals(STRUCTURE_NAMES[5]))
                callParticipantEnd = getCSVs(value);
            else if(propName.equals(STRUCTURE_NAMES[6]))
                callParticipantStates = getStates(value);
        }

        final int callParticipantCount = callParticipantIDs.size();
        for (int i = 0; i < callParticipantCount; i++)
        {
            CallParticipantRecordImpl cpr =
                new CallParticipantRecordImpl(callParticipantIDs.get(i),
                    new Date(Long.parseLong(callParticipantStart.get(i))),
                    new Date(Long.parseLong(callParticipantEnd.get(i))));

            // if there is no record about the states (backward compability)
            if (callParticipantStates != null)
                cpr.setState(callParticipantStates.get(i));

            result.getParticipantRecords().add(cpr);
        }

        return result;
    }

    /**
     * Returns list of String items contained in the supplied string
     * separated by DELIM
     * @param str String
     * @return LinkedList
     */
    private List<String> getCSVs(String str)
    {
        List<String> result = new LinkedList<String>();
        StringTokenizer toks = new StringTokenizer(str, DELIM);
        while(toks.hasMoreTokens())
        {
            result.add(toks.nextToken());
        }
        return result;
    }

    /**
     * Get the delimited strings and converts them to CallParticipantState
     * 
     * @param str String delimited string states
     * @return LinkedList the converted values list
     */
    private List<CallParticipantState> getStates(String str)
    {
        List<CallParticipantState> result =
            new LinkedList<CallParticipantState>();
        Collection<String> stateStrs = getCSVs(str);

        for (String item : stateStrs)
        {
            result.add(convertStateStringToState(item));
        }

        return result;
    }

    /**
     * Converts the state string to state
     * @param state String the string
     * @return CallParticipantState the state
     */
    private CallParticipantState convertStateStringToState(String state)
    {
        if(state.equals(CallParticipantState._CONNECTED))
            return CallParticipantState.CONNECTED;
        else if(state.equals(CallParticipantState._BUSY))
            return CallParticipantState.BUSY;
        else if(state.equals(CallParticipantState._FAILED))
            return CallParticipantState.FAILED;
        else if(state.equals(CallParticipantState._DISCONNECTED))
            return CallParticipantState.DISCONNECTED;
        else if(state.equals(CallParticipantState._ALERTING_REMOTE_SIDE))
            return CallParticipantState.ALERTING_REMOTE_SIDE;
        else if(state.equals(CallParticipantState._CONNECTING))
            return CallParticipantState.CONNECTING;
        else if(state.equals(CallParticipantState._ON_HOLD_LOCALLY))
            return CallParticipantState.ON_HOLD_LOCALLY;
        else if(state.equals(CallParticipantState._ON_HOLD_MUTUALLY))
            return CallParticipantState.ON_HOLD_MUTUALLY;
        else if(state.equals(CallParticipantState._ON_HOLD_REMOTELY))
            return CallParticipantState.ON_HOLD_REMOTELY;
        else if(state.equals(CallParticipantState._INITIATING_CALL))
            return CallParticipantState.INITIATING_CALL;
        else if(state.equals(CallParticipantState._INCOMING_CALL))
            return CallParticipantState.INCOMING_CALL;
        else return CallParticipantState.UNKNOWN;
    }

    /**
     * starts the service. Check the current registerd protocol providers
     * which supports BasicTelephony and adds calls listener to them
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        logger.debug("Starting the call history implementation.");
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

    /**
     * stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        bc.removeServiceListener(this);

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
            logger.error("Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);

                this.handleProviderRemoved(provider);
            }
        }
    }


    /**
     * Writes the given record to the history service
     * @param callRecord CallRecord
     * @param source Contact
     * @param destination Contact
     */
    private void writeCall(CallRecord callRecord, Contact source,
            Contact destination)
    {
        try {
            History history = this.getHistory(source, destination);
            HistoryWriter historyWriter = history.getWriter();

            StringBuffer callParticipantIDs = new StringBuffer();
            StringBuffer callParticipantStartTime = new StringBuffer();
            StringBuffer callParticipantEndTime = new StringBuffer();
            StringBuffer callParticipantStates = new StringBuffer();

            for (CallParticipantRecord item : callRecord
                .getParticipantRecords())
            {
                if (callParticipantIDs.length() > 0)
                {
                    callParticipantIDs.append(DELIM);
                    callParticipantStartTime.append(DELIM);
                    callParticipantEndTime.append(DELIM);
                    callParticipantStates.append(DELIM);
                }

                callParticipantIDs.append(item.getParticipantAddress());
                callParticipantStartTime.append(String.valueOf(item
                    .getStartTime().getTime()));
                callParticipantEndTime.append(String.valueOf(item.getEndTime()
                    .getTime()));
                callParticipantStates.append(item.getState().getStateString());
            }

            historyWriter.addRecord(new String[] {
                    String.valueOf(callRecord.getStartTime().getTime()),
                    String.valueOf(callRecord.getEndTime().getTime()),
                    callRecord.getDirection(),
                    callParticipantIDs.toString(),
                    callParticipantStartTime.toString(),
                    callParticipantEndTime.toString(),
                    callParticipantStates.toString()},
                    new Date()); // this date is when the history record is written
        } catch (IOException e)
        {
            logger.error("Could not add call to history", e);
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
        synchronized (this.syncRoot_HistoryService)
        {
            this.historyService = historyService;

            logger.debug("New history service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param historyService HistoryService
     */
    public void unsetHistoryService(HistoryService historyService)
    {
        synchronized (this.syncRoot_HistoryService)
        {
            if (this.historyService == historyService)
            {
                this.historyService = null;

                logger.debug("History service unregistered.");
            }
        }
    }

    /**
     * When new protocol provider is registered we check
     * does it supports BasicTelephony and if so add a listener to it
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
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved( (ProtocolProviderService) sService);
        }

    }

    /**
     * Used to attach the Call History Service to existing or
     * just registered protocol provider. Checks if the provider has implementation
     * of OperationSetBasicTelephony
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        logger.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic telephony operation set
        OperationSetBasicTelephony opSetTelephony =
            (OperationSetBasicTelephony) provider
                .getOperationSet(OperationSetBasicTelephony.class);

        if (opSetTelephony != null)
        {
            opSetTelephony.addCallListener(this);
        }
        else
        {
            logger.trace("Service did not have a basic telephony op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the calls made by it
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicTelephony opSetTelephony =
            (OperationSetBasicTelephony) provider
                .getOperationSet(OperationSetBasicTelephony.class);

        if (opSetTelephony != null)
        {
            opSetTelephony.removeCallListener(this);
        }
    }

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
    public void addSearchProgressListener(CallHistorySearchProgressListener
                                          listener)
    {
        synchronized (progressListeners)
        {
            progressListeners
                .put(listener, new SearchProgressWrapper(listener));
        }
    }

    /**
     * Removing progress listener
     *
     * @param listener HistorySearchProgressListener
     */
    public void removeSearchProgressListener(
        CallHistorySearchProgressListener listener)
    {
        synchronized(progressListeners){
            progressListeners.remove(listener);
        }
    }

    /**
     * Add the registered CallHistorySearchProgressListeners to the given
     * HistoryReader
     * 
     * @param reader HistoryReader
     * @param countContacts number of contacts will search
     */
    private void addHistorySearchProgressListeners(HistoryReader reader,
        int countContacts)
    {
        synchronized (progressListeners)
        {
            for (SearchProgressWrapper l : progressListeners.values())
            {
                l.contactCount = countContacts;
                reader.addSearchProgressListener(l);
            }
        }
    }

    /**
     * Removes the registered CallHistorySearchProgressListeners from the given
     * HistoryReader
     * 
     * @param reader HistoryReader
     */
    private void removeHistorySearchProgressListeners(HistoryReader reader)
    {
        synchronized (progressListeners)
        {
            for (SearchProgressWrapper l : progressListeners.values())
            {
                l.clear();
                reader.removeSearchProgressListener(l);
            }
        }
    }

    /**
     * Gets all the history readers for the contacts in the given MetaContact
     * 
     * @param contact MetaContact
     * @return Hashtable
     */
    private Map<Contact, HistoryReader> getHistoryReaders(MetaContact contact)
    {
        Map<Contact, HistoryReader> readers =
            new Hashtable<Contact, HistoryReader>();
        Iterator<Contact> iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();

            try
            {
                History history = this.getHistory(null, item);
                readers.put(item, history.getReader());
            }
            catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }
        return readers;
    }

    /**
     * CallListener implementation for incoming calls
     * @param event CallEvent
     */
    public void incomingCallReceived(CallEvent event)
    {
        handleNewCall(event.getSourceCall(), CallRecord.IN);
    }

    /**
     * CallListener implementation for outgoing calls
     * @param event CallEvent
     */
    public void outgoingCallCreated(CallEvent event)
    {
        handleNewCall(event.getSourceCall(), CallRecord.OUT);
    }

    /**
     * CallListener implementation for call endings
     * @param event CallEvent
     */
    public void callEnded(CallEvent event)
    {
        CallRecordImpl callRecord = findCallRecord(event.getSourceCall());

        // no such call
        if (callRecord == null)
            return;

        callRecord.setEndTime(new Date());

        writeCall(callRecord, null, null);

        currentCallRecords.remove(callRecord);
    }

    /**
     * Adding a record for joining participant
     * @param callParticipant CallParticipant
     */
    private void handleParticipantAdded(CallParticipant callParticipant)
    {
        CallRecord callRecord = findCallRecord(callParticipant.getCall());

        // no such call
        if(callRecord == null)
            return;

        callParticipant.addCallParticipantListener(new CallParticipantAdapter()
        {
            public void participantStateChanged(CallParticipantChangeEvent evt)
            {
                if(evt.getNewValue().equals(CallParticipantState.DISCONNECTED))
                    return;
                else
                {
                    CallParticipantRecordImpl participantRecord =
                        findParticipantRecord(evt.getSourceCallParticipant());

                    if(participantRecord == null)
                        return;

                    CallParticipantState newState =
                        (CallParticipantState) evt.getNewValue();

                    if (newState.equals(CallParticipantState.CONNECTED)
                        && !CallParticipantState.isOnHold((CallParticipantState)
                                evt.getOldValue()))
                        participantRecord.setStartTime(new Date());

                    participantRecord.setState(newState);

                    //Disconnected / Busy
                    //Disconnected / Connecting - fail
                    //Disconnected / Connected
                }
            }
        });

        Date startDate = new Date();
        CallParticipantRecordImpl newRec = new CallParticipantRecordImpl(
            callParticipant.getAddress(),
            startDate,
            startDate);

        callRecord.getParticipantRecords().add(newRec);
    }

    /**
     * Adding a record for removing participant from call
     * @param callParticipant CallParticipant
     * @param srcCall Call
     */
    private void handleParticipantRemoved(CallParticipant callParticipant,
        Call srcCall)
    {
        CallRecord callRecord = findCallRecord(srcCall);
        String pAddress = callParticipant.getAddress();

        CallParticipantRecordImpl cpRecord =
            (CallParticipantRecordImpl)callRecord.findParticipantRecord(pAddress);

        // no such participant
        if(cpRecord == null)
            return;

        if(!callParticipant.getState().equals(CallParticipantState.DISCONNECTED))
            cpRecord.setState(callParticipant.getState());

        CallParticipantState cpRecordState = cpRecord.getState();

        if (cpRecordState.equals(CallParticipantState.CONNECTED)
            || CallParticipantState.isOnHold(cpRecordState))
        {
            cpRecord.setEndTime(new Date());
        }
    }

    /**
     * Finding a CallRecord for the given call
     * 
     * @param call Call
     * @return CallRecord
     */
    private CallRecordImpl findCallRecord(Call call)
    {
        for (CallRecordImpl item : currentCallRecords)
        {
            if (item.getSourceCall().equals(call))
                return item;
        }

        return null;
    }

    /**
     * Returns the participant record for the given participant
     * @param callParticipant CallParticipant participant
     * @return CallParticipantRecordImpl the corresponding record
     */
    private CallParticipantRecordImpl findParticipantRecord(
        CallParticipant callParticipant)
    {
        CallRecord record = findCallRecord(callParticipant.getCall());

        if (record == null)
            return null;

        return (CallParticipantRecordImpl) record.findParticipantRecord(
                callParticipant.getAddress());
    }

    /**
     * Adding a record for a new call
     * @param sourceCall Call
     * @param direction String
     */
    private void handleNewCall(Call sourceCall, String direction)
    {
        // if call exist. its not new
        if(currentCallRecords.contains(sourceCall))
            return;

        CallRecordImpl newRecord = new CallRecordImpl(
            direction,
            new Date(),
            null);
        newRecord.setSourceCall(sourceCall);

        sourceCall.addCallChangeListener(historyCallChangeListener);

        currentCallRecords.add(newRecord);

        // if has already perticipants Dispatch them
        Iterator<CallParticipant> iter = sourceCall.getCallParticipants();
        while (iter.hasNext())
        {
            handleParticipantAdded(iter.next());
        }
    }

    /**
     * A wrapper around HistorySearchProgressListener
     * that fires events for CallHistorySearchProgressListener
     */
    private class SearchProgressWrapper
        implements HistorySearchProgressListener
    {
        private CallHistorySearchProgressListener listener = null;
        int contactCount = 0;
        int currentContactCount = 0;
        int currentProgress = 0;
        int lastHistoryProgress = 0;

        SearchProgressWrapper(CallHistorySearchProgressListener listener)
        {
            this.listener = listener;
        }

        public void progressChanged(ProgressEvent evt)
        {
            int progress = getProgressMapping(evt.getProgress());

            listener.progressChanged(
                new net.java.sip.communicator.service.callhistory.event.
                    ProgressEvent(CallHistoryServiceImpl.this, evt, progress));
        }

        /**
         * Calculates the progress according the count of the contacts
         * we will search
         * @param historyProgress int
         * @return int
         */
        private int getProgressMapping(int historyProgress)
        {
            currentProgress += (historyProgress - lastHistoryProgress)/contactCount;

            if(historyProgress == HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE)
            {
                currentContactCount++;
                lastHistoryProgress = 0;

                // this is the last one and the last event fire the max
                // there will be looses in currentProgress due to the devision
                if(currentContactCount == contactCount)
                    currentProgress =
                        CallHistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE;
            }
            else
                lastHistoryProgress = historyProgress;

            return currentProgress;
        }

        /**
         * clear the values
         */
        void clear()
        {
            contactCount = 0;
            currentProgress = 0;
            lastHistoryProgress = 0;
            currentContactCount = 0;
        }
    }

    /**
     * Used to compare CallRecords and to be ordered in TreeSet according their
     * timestamp
     */
    private class CallRecordComparator
        implements Comparator<CallRecord>
    {
        public int compare(CallRecord o1, CallRecord o2)
        {
            return o2.getStartTime().compareTo(o1.getStartTime());
        }
    }

    /**
     * Receive events for adding or removing participants from a call
     */
    private class HistoryCallChangeListener
        extends CallChangeAdapter
    {
        public void callParticipantAdded(CallParticipantEvent evt)
        {
            handleParticipantAdded(evt.getSourceCallParticipant());
        }

        public void callParticipantRemoved(CallParticipantEvent evt)
        {
            handleParticipantRemoved(evt.getSourceCallParticipant(),
                                     evt.getSourceCall());
        }
    }
}
