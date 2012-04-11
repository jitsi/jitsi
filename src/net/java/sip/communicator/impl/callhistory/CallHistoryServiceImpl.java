/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
        new String[] { "accountUID", "callStart", "callEnd", "dir",
            "callParticipantIDs", "callParticipantStart",
            "callParticipantEnd", "callParticipantStates", "callEndReason",
            "callParticipantNames"};

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(STRUCTURE_NAMES);

    private static final String DELIM = ",";

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    private HistoryService historyService = null;

    private Object syncRoot_HistoryService = new Object();

    private final Map<CallHistorySearchProgressListener, SearchProgressWrapper>
        progressListeners
            = new Hashtable<CallHistorySearchProgressListener,
                            SearchProgressWrapper>();

    private final List<CallRecordImpl> currentCallRecords =
        new Vector<CallRecordImpl>();

    private final CallChangeListener historyCallChangeListener =
        new HistoryCallChangeListener();

    private HistoryReader historyReader;

    /**
     * Returns the underlying history service.
     * @return the underlying history service
     */
    public HistoryService getHistoryService()
    {
        return historyService;
    }

    /**
     * Returns all the calls made by all the contacts in the supplied
     * <tt>contact</tt> after the given date.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param startDate Date the start date of the calls
     * @return the <tt>CallHistoryQuery</tt>, corresponding to this find
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByStartDate(
        MetaContact contact, Date startDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls made after the given date
     *
     * @param startDate Date the start date of the calls
     * @return the <tt>CallHistoryQuery</tt>, corresponding to this find
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
            historyReader = history.getReader();
            addHistorySearchProgressListeners(historyReader, 1);
            QueryResultSet<HistoryRecord> rs
                = historyReader.findByStartDate(startDate);
            while (rs.hasNext())
            {
                HistoryRecord hr = rs.next();
                result.add(convertHistoryRecordToCallRecord(hr));
            }
            removeHistorySearchProgressListeners(historyReader);
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
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByEndDate(MetaContact contact,
                                                Date endDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls made before the given date
     *
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByEndDate(Date endDate)
        throws RuntimeException
    {
        TreeSet<CallRecord> result
            = new TreeSet<CallRecord>(new CallRecordComparator());
        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            historyReader = history.getReader();
            addHistorySearchProgressListeners(historyReader, 1);
            QueryResultSet<HistoryRecord> rs
                = historyReader.findByEndDate(endDate);
            while (rs.hasNext())
            {
                HistoryRecord hr = rs.next();
                result.add(convertHistoryRecordToCallRecord(hr));
            }
            removeHistorySearchProgressListeners(historyReader);
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
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByPeriod(MetaContact contact,
        Date startDate, Date endDate)
        throws RuntimeException
    {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Returns all the calls made between the given dates
     *
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the conversations
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findByPeriod(Date startDate, Date endDate)
        throws RuntimeException
    {
        TreeSet<CallRecord> result
            = new TreeSet<CallRecord>(new CallRecordComparator());
        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            historyReader = history.getReader();
            addHistorySearchProgressListeners(historyReader, 1);
            QueryResultSet<HistoryRecord> rs
                = historyReader.findByPeriod(startDate, endDate);
            while (rs.hasNext())
            {
                HistoryRecord hr = rs.next();
                result.add(convertHistoryRecordToCallRecord(hr));
            }
            removeHistorySearchProgressListeners(historyReader);
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
     * @return Collection of CallRecords with CallPeerRecord
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
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public Collection<CallRecord> findLast(int count) throws RuntimeException
    {
        TreeSet<CallRecord> result
            = new TreeSet<CallRecord>(new CallRecordComparator());
        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            historyReader = history.getReader();
            QueryResultSet<HistoryRecord> rs = historyReader.findLast(count);
            while (rs.hasNext())
            {
                HistoryRecord hr = rs.next();
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
     * Find the calls made by the supplied peer address
     * @param address String the address of the peer
     * @param recordCount the number of records to return
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException
     */
    public CallHistoryQuery findByPeer(String address, int recordCount)
        throws RuntimeException
    {
        CallHistoryQueryImpl callQuery = null;

        try
        {
            // the default ones
            History history = this.getHistory(null, null);
            InteractiveHistoryReader historyReader
                = history.getInteractiveReader();
            HistoryQuery historyQuery
                = historyReader.findByKeyword(
                    address, "callParticipantIDs", recordCount);

            callQuery = new CallHistoryQueryImpl(historyQuery);
        }
        catch (IOException ex)
        {
            logger.error("Could not read history", ex);
        }

        return callQuery;
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
            retVal.setHistoryRecordsStructure(recordStructure);
        }
        else
        {
            retVal = this.historyService.createHistory(historyId,
                    recordStructure);
        }

        return retVal;
    }

    /**
     * Used to convert HistoryRecord in CallReord and CallPeerRecord
     * which are returned by the finder methods
     *
     * @param hr HistoryRecord
     * @return Object CallRecord
     */
    static CallRecord convertHistoryRecordToCallRecord(HistoryRecord hr)
    {
        CallRecordImpl result = new CallRecordImpl();

        List<String> callPeerIDs = null;
        List<String> callPeerNames = null;
        List<String> callPeerStart = null;
        List<String> callPeerEnd = null;
        List<CallPeerState> callPeerStates = null;

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

            if (propName.equals(STRUCTURE_NAMES[0]))
                result.setProtocolProvider(getProtocolProvider(value));
            else if(propName.equals(STRUCTURE_NAMES[1]))
                result.setStartTime(new Date(Long.parseLong(value)));
            else if(propName.equals(STRUCTURE_NAMES[2]))
                result.setEndTime(new Date(Long.parseLong(value)));
            else if(propName.equals(STRUCTURE_NAMES[3]))
                result.setDirection(value);
            else if(propName.equals(STRUCTURE_NAMES[4]))
                callPeerIDs = getCSVs(value);
            else if(propName.equals(STRUCTURE_NAMES[5]))
                callPeerStart = getCSVs(value);
            else if(propName.equals(STRUCTURE_NAMES[6]))
                callPeerEnd = getCSVs(value);
            else if(propName.equals(STRUCTURE_NAMES[7]))
                callPeerStates = getStates(value);
            else if(propName.equals(STRUCTURE_NAMES[8]))
                result.setEndReason(Integer.parseInt(value));
            else if(propName.equals(STRUCTURE_NAMES[9]))
                callPeerNames = getCSVs(value);
        }

        final int callPeerCount = callPeerIDs == null ? 0 : callPeerIDs.size();
        for (int i = 0; i < callPeerCount; i++)
        {
            // As we iterate over the CallPeer IDs we could not be sure that
            // for some reason the start or end call list could result in
            // different size lists, so we check this first.
            Date callPeerStartValue = null;
            Date callPeerEndValue = null;

            if (i < callPeerStart.size())
            {
                callPeerStartValue
                    = new Date(Long.parseLong(callPeerStart.get(i)));
            }
            else
            {
                callPeerStartValue = result.getStartTime();
                if (logger.isInfoEnabled())
                    logger.info(
                        "Call history start time list different from ids list: "
                        + hr.toString());
            }

            if (i < callPeerEnd.size())
            {
                callPeerEndValue
                    = new Date(Long.parseLong(callPeerEnd.get(i)));
            }
            else
            {
                callPeerEndValue = result.getEndTime();
                if (logger.isInfoEnabled())
                    logger.info(
                        "Call history end time list different from ids list: "
                        + hr.toString());
            }

            CallPeerRecordImpl cpr =
                new CallPeerRecordImpl(
                    callPeerIDs.get(i),
                    callPeerStartValue,
                    callPeerEndValue);

            // if there is no record about the states (backward compatibility)
            if (callPeerStates != null && i < callPeerStates.size())
                cpr.setState(callPeerStates.get(i));
            else if (logger.isInfoEnabled())
                    logger.info(
                        "Call history state list different from ids list: "
                        + hr.toString());

            result.getPeerRecords().add(cpr);

            if (callPeerNames != null && i < callPeerNames.size())
                cpr.setDisplayName(callPeerNames.get(i));
        }

        return result;
    }

    /**
     * Returns list of String items contained in the supplied string
     * separated by DELIM
     * @param str String
     * @return LinkedList
     */
    private static List<String> getCSVs(String str)
    {
        List<String> result = new LinkedList<String>();

        if(str == null)
            return result;

        StringTokenizer toks = new StringTokenizer(str, DELIM);
        while(toks.hasMoreTokens())
        {
            result.add(toks.nextToken());
        }
        return result;
    }

    /**
     * Get the delimited strings and converts them to CallPeerState
     *
     * @param str String delimited string states
     * @return LinkedList the converted values list
     */
    private static List<CallPeerState> getStates(String str)
    {
        List<CallPeerState> result =
            new LinkedList<CallPeerState>();
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
     * @return CallPeerState the state
     */
    private static CallPeerState convertStateStringToState(String state)
    {
        if(state.equals(CallPeerState._CONNECTED))
            return CallPeerState.CONNECTED;
        else if(state.equals(CallPeerState._BUSY))
            return CallPeerState.BUSY;
        else if(state.equals(CallPeerState._FAILED))
            return CallPeerState.FAILED;
        else if(state.equals(CallPeerState._DISCONNECTED))
            return CallPeerState.DISCONNECTED;
        else if(state.equals(CallPeerState._ALERTING_REMOTE_SIDE))
            return CallPeerState.ALERTING_REMOTE_SIDE;
        else if(state.equals(CallPeerState._CONNECTING))
            return CallPeerState.CONNECTING;
        else if(state.equals(CallPeerState._ON_HOLD_LOCALLY))
            return CallPeerState.ON_HOLD_LOCALLY;
        else if(state.equals(CallPeerState._ON_HOLD_MUTUALLY))
            return CallPeerState.ON_HOLD_MUTUALLY;
        else if(state.equals(CallPeerState._ON_HOLD_REMOTELY))
            return CallPeerState.ON_HOLD_REMOTELY;
        else if(state.equals(CallPeerState._INITIATING_CALL))
            return CallPeerState.INITIATING_CALL;
        else if(state.equals(CallPeerState._INCOMING_CALL))
            return CallPeerState.INCOMING_CALL;
        else return CallPeerState.UNKNOWN;
    }

    /**
     * starts the service. Check the current registerd protocol providers
     * which supports BasicTelephony and adds calls listener to them
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        if (logger.isDebugEnabled())
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
            if (logger.isDebugEnabled())
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
    private void writeCall( CallRecordImpl callRecord,
                            Contact source,
                            Contact destination)
    {
        try
        {
            History history = this.getHistory(source, destination);
            HistoryWriter historyWriter = history.getWriter();

            StringBuffer callPeerIDs = new StringBuffer();
            StringBuffer callPeerNames = new StringBuffer();
            StringBuffer callPeerStartTime = new StringBuffer();
            StringBuffer callPeerEndTime = new StringBuffer();
            StringBuffer callPeerStates = new StringBuffer();

            for (CallPeerRecord item : callRecord
                .getPeerRecords())
            {
                if (callPeerIDs.length() > 0)
                {
                    callPeerIDs.append(DELIM);
                    callPeerNames.append(DELIM);
                    callPeerStartTime.append(DELIM);
                    callPeerEndTime.append(DELIM);
                    callPeerStates.append(DELIM);
                }

                callPeerIDs.append(item.getPeerAddress());
                callPeerNames.append(item.getDisplayName());
                callPeerStartTime.append(String.valueOf(item
                    .getStartTime().getTime()));
                callPeerEndTime.append(String.valueOf(item.getEndTime()
                    .getTime()));
                callPeerStates.append(item.getState().getStateString());
            }

            historyWriter.addRecord(new String[] {
                    callRecord.getSourceCall().getProtocolProvider()
                        .getAccountID().getAccountUniqueID(),
                    String.valueOf(callRecord.getStartTime().getTime()),
                    String.valueOf(callRecord.getEndTime().getTime()),
                    callRecord.getDirection(),
                    callPeerIDs.toString(),
                    callPeerStartTime.toString(),
                    callPeerEndTime.toString(),
                    callPeerStates.toString(),
                    String.valueOf(callRecord.getEndReason()),
                    callPeerNames.toString()},
                    new Date());    // this date is when the history
                                    // record is written
        }
        catch (IOException e)
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

            if (logger.isDebugEnabled())
                logger.debug("New history service registered.");
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param hService HistoryService
     */
    public void unsetHistoryService(HistoryService hService)
    {
        synchronized (this.syncRoot_HistoryService)
        {
            if (this.historyService == hService)
            {
                this.historyService = null;

                if (logger.isDebugEnabled())
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
        Object sService
            = bundleContext.getService(serviceEvent.getServiceReference());

        if (logger.isTraceEnabled())
            logger.trace("Received a service event for: "
            + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        if (logger.isDebugEnabled())
            logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            if (logger.isDebugEnabled())
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
     * just registered protocol provider. Checks if the provider has
     * implementation of OperationSetBasicTelephony
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        if (logger.isDebugEnabled())
            logger.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic telephony operation set
        OperationSetBasicTelephony<?> opSetTelephony
            = provider.getOperationSet(OperationSetBasicTelephony.class);

        if (opSetTelephony != null)
        {
            opSetTelephony.addCallListener(this);
        }
        else
        {
            if (logger.isTraceEnabled())
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
        OperationSetBasicTelephony<?> opSetTelephony
            = provider.getOperationSet(OperationSetBasicTelephony.class);

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
        // We store the call in the callStateChangeEvent where we
        // have more information on the previous state of the call.
    }

    /**
     * Adding a record for joining peer
     * @param callPeer CallPeer
     */
    private void handlePeerAdded(CallPeer callPeer)
    {
        CallRecord callRecord = findCallRecord(callPeer.getCall());

        // no such call
        if(callRecord == null)
            return;

        callPeer.addCallPeerListener(new CallPeerAdapter()
        {
            @Override
            public void peerStateChanged(CallPeerChangeEvent evt)
            {
                if(evt.getNewValue().equals(CallPeerState.DISCONNECTED))
                    return;
                else
                {
                    CallPeerRecordImpl peerRecord =
                        findPeerRecord(evt.getSourceCallPeer());

                    if(peerRecord == null)
                        return;

                    CallPeerState newState =
                        (CallPeerState) evt.getNewValue();

                    if (newState.equals(CallPeerState.CONNECTED)
                        && !CallPeerState.isOnHold((CallPeerState)
                                evt.getOldValue()))
                        peerRecord.setStartTime(new Date());

                    peerRecord.setState(newState);

                    //Disconnected / Busy
                    //Disconnected / Connecting - fail
                    //Disconnected / Connected
                }
            }
        });

        Date startDate = new Date();
        CallPeerRecordImpl newRec = new CallPeerRecordImpl(
            callPeer.getAddress(),
            startDate,
            startDate);

        newRec.setDisplayName(callPeer.getDisplayName());

        callRecord.getPeerRecords().add(newRec);
    }

    /**
     * Adding a record for removing peer from call
     * @param callPeer CallPeer
     * @param srcCall Call
     */
    private void handlePeerRemoved( CallPeer callPeer,
                                    Call srcCall)
    {
        CallRecord callRecord = findCallRecord(srcCall);
        String pAddress = callPeer.getAddress();

        if (callRecord == null)
            return;

        CallPeerRecordImpl cpRecord =
            (CallPeerRecordImpl)callRecord.findPeerRecord(pAddress);

        // no such peer
        if(cpRecord == null)
            return;

        if(!callPeer.getState().equals(CallPeerState.DISCONNECTED))
            cpRecord.setState(callPeer.getState());

        CallPeerState cpRecordState = cpRecord.getState();

        if (cpRecordState.equals(CallPeerState.CONNECTED)
            || CallPeerState.isOnHold(cpRecordState))
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
     * Returns the peer record for the given peer
     * @param callPeer CallPeer peer
     * @return CallPeerRecordImpl the corresponding record
     */
    private CallPeerRecordImpl findPeerRecord(
        CallPeer callPeer)
    {
        CallRecord record = findCallRecord(callPeer.getCall());

        if (record == null)
            return null;

        return (CallPeerRecordImpl) record.findPeerRecord(
                callPeer.getAddress());
    }

    /**
     * Adding a record for a new call
     * @param sourceCall Call
     * @param direction String
     */
    private void handleNewCall(Call sourceCall, String direction)
    {
        // if call exist. its not new
        for (CallRecordImpl currentCallRecord : currentCallRecords)
        {
            if (currentCallRecord.getSourceCall().equals(sourceCall))
                return;
        }

        CallRecordImpl newRecord = new CallRecordImpl(
            direction,
            new Date(),
            null);
        newRecord.setSourceCall(sourceCall);

        sourceCall.addCallChangeListener(historyCallChangeListener);

        currentCallRecords.add(newRecord);

        // if has already perticipants Dispatch them
        Iterator<? extends CallPeer> iter = sourceCall.getCallPeers();
        while (iter.hasNext())
        {
            handlePeerAdded(iter.next());
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
            currentProgress
                += (historyProgress - lastHistoryProgress)/contactCount;

            if(historyProgress
                == HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE)
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
    private static class CallRecordComparator
        implements Comparator<CallRecord>
    {
        public int compare(CallRecord o1, CallRecord o2)
        {
            return o2.getStartTime().compareTo(o1.getStartTime());
        }
    }

    /**
     * Receive events for adding or removing peers from a call
     */
    private class HistoryCallChangeListener implements CallChangeListener
    {
        /**
         * Indicates that a new call peer has joined the source call.
         *
         * @param evt the <tt>CallPeerEvent</tt> containing the source call
         * and call peer.
         */
        public void callPeerAdded(CallPeerEvent evt)
        {
            handlePeerAdded(evt.getSourceCallPeer());
        }

        /**
         * Indicates that a call peer has left the source call.
         *
         * @param evt the <tt>CallPeerEvent</tt> containing the source call
         * and call peer.
         */
        public void callPeerRemoved(CallPeerEvent evt)
        {
            handlePeerRemoved(evt.getSourceCallPeer(),
                                     evt.getSourceCall());
        }

        /**
         * A dummy implementation of this listener's callStateChanged() method.
         *
         * @param evt the <tt>CallChangeEvent</tt> instance containing the source
         * calls and its old and new state.
         */
        public void callStateChanged(CallChangeEvent evt)
        {
            CallRecordImpl callRecord = findCallRecord(evt.getSourceCall());

            // no such call
            if (callRecord == null)
                return;

            if (evt.getNewValue().equals(CallState.CALL_ENDED))
            {
                if(evt.getOldValue().equals(CallState.CALL_INITIALIZATION))
                {
                    callRecord.setEndTime(callRecord.getStartTime());

                    // if call was answered elsewhere, add its reason
                    // so we can distinguish it from missed
                    if(evt.getCause() != null
                           && evt.getCause().getReasonCode() ==
                                CallPeerChangeEvent.NORMAL_CALL_CLEARING)
                    {
                        callRecord.setEndReason(evt.getCause().getReasonCode());
                    }
                }
                else
                    callRecord.setEndTime(new Date());

                writeCall(callRecord, null, null);

                currentCallRecords.remove(callRecord);
            }
        }
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> corresponding to the given
     * account identifier.
     * @param accountUID the identifier of the account.
     * @return the <tt>ProtocolProviderService</tt> corresponding to the given
     * account identifier
     */
    private static ProtocolProviderService getProtocolProvider(String accountUID)
    {
        for (ProtocolProviderFactory providerFactory
                : CallHistoryActivator.getProtocolProviderFactories().values())
        {
            ServiceReference serRef;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                if (accountID.getAccountUniqueID().equals(accountUID))
                {
                    serRef = providerFactory.getProviderForAccount(accountID);

                    return (ProtocolProviderService) CallHistoryActivator
                        .bundleContext.getService(serRef);
                }
            }
        }
        return null;
    }
}
