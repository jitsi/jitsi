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
package net.java.sip.communicator.plugin.addrbook.msoutlook.calendar;

import java.beans.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;


import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.plugin.addrbook.msoutlook.*;
import net.java.sip.communicator.service.calendar.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A implementation of <tt>CalendarService</tt> for MS Outlook calendar.
 * The class resolves the free busy status and also changes the presence status
 * according to free busy status.
 *
 * @author Hristo Terezov
 */
public class CalendarServiceImpl implements CalendarService
{
    /**
     * Types for the MAPI properties values.
     */
    public enum MAPIType
    {
        PT_SYSTIME,
        PT_LONG,
        PT_BOOL,
        PT_BINARY
    };

    /**
     * Response statuses of the calendar events (meeting objects).
     */
    public static enum ResponseStatus
    {
        /**
         * No response is required for this object.
         */
        respNone(0x00000000),

        /**
         * This meeting belongs to the organizer.
         */
        respOrganized(0x00000001),

        /**
         * This value on the attendee's meeting indicates that the attendee has
         * tentatively accepted the meeting request.
         */
        respTentative(0x00000002),

        /**
         * This value on the attendee's meeting t indicates that the attendee
         * has accepted the meeting request.
         */
        respAccepted(0x00000003),

        /**
         * This value on the attendee's meeting indicates that the attendee has
         * declined the meeting request.
         */
        respDeclined(0x00000004),

        /**
         * This value on the attendee's meeting indicates the attendee has not
         * yet responded.
         */
        respNotResponded(0x00000005);

        /**
         * The ID of the property
         */
        private final long id;

        private ResponseStatus(int id)
        {
            this.id = id;
        }

        /**
         * Finds <tt>ResponseStatuse</tt> instance by given value of the status.
         * @param value the value of the status we are searching for.
         * @return the status or <tt>FREE</tt> if no status is found.
         */
        public static ResponseStatus getFromLong(long value)
        {
            for(ResponseStatus state : values())
            {
                if(state.getID() == value)
                {
                    return state;
                }
            }
            return respNone;
        }

        /**
         * Returns the ID of the status.
         * @return the ID of the status.
         */
        private long getID()
        {
            return id;
        }
    };

    /**
     * MAPI properties that we use to get information about the calendar items.
     */
    public static enum MAPICalendarProperties
    {
        /**
         * A property for the start date of the calendar item.
         */
        PidLidAppointmentStartWhole(0x0000820D, MAPIType.PT_SYSTIME),

        /**
         * A property for the end date of the calendar item.
         */
        PidLidAppointmentEndWhole(0x0000820E, MAPIType.PT_SYSTIME),

        /**
         * A property for the free busy status of the calendar item.
         */
        PidLidBusyStatus(0x00008205, MAPIType.PT_LONG),

        /**
         * A property that indicates if the calendar item is recurring or not.
         */
        PidLidRecurring(0x00008223, MAPIType.PT_BOOL),

        /**
         * A property with information about the recurrent pattern of the event.
         */
        PidLidAppointmentRecur(0x00008216, MAPIType.PT_BINARY),

        /**
         * A property with information about the accepted state of the event.
         */
        PidLidResponseStatus(0x00008218, MAPIType.PT_LONG);

        /**
         * The id of the property
         */
        private final long id;

        /**
         * The <tt>MAPIType</tt> of the property.
         */
        private final MAPIType type;

        /**
         * Constructs new property.
         * @param id the id
         * @param type the type
         */
        MAPICalendarProperties(long id, MAPIType type)
        {
            this.id = id;
            this.type = type;
        }

        /**
         * Returns array of IDs of created properties.
         * @return array of IDs of created properties.
         */
        public static long[] getALLPropertyIDs()
        {
            MAPICalendarProperties properties[] = values();
            long[] result = new long[properties.length];
            for(int i = 0; i < properties.length; i++)
            {
                result[i] = properties[i].getID();
            }
            return result;
        }

        /**
         * Returns the ID of the property.
         * @return the ID of the property.
         */
        public long getID()
        {
            return id;
        }

        /**
         * Returns the type of the property
         * @return the type of the property
         */
        public MAPIType getType()
        {
            return type;
        }

        /**
         * Returns <tt>MAPICalendarProperties</tt> instance by given order ID
         * @param i the order ID
         * @return <tt>MAPICalendarProperties</tt> instance
         */
        public static MAPICalendarProperties getByOrderId(int i)
        {
            return values()[i];
        }
    }

    /**
     * The <tt>Logger</tt> used by the <tt>CalendarServiceImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CalendarServiceImpl.class);

    /**
     * A list with currently active <tt>CalendarItemTimerTask</tt>s
     */
    private List<CalendarItemTimerTask> currentCalendarItems
        = new LinkedList<CalendarItemTimerTask>();

    /**
     * A map with the calendar items IDs and <tt>CalendarItemTimerTask</tt>s.
     * The map contains the current and future calendar items.
     */
    private Map<String, CalendarItemTimerTask> taskMap
        = new HashMap<String, CalendarItemTimerTask>();

    /**
     * The current free busy status.
     */
    private BusyStatusEnum currentState = BusyStatusEnum.FREE;

    /**
     * Instance of <tt>InMeetingStatusPolicy</tt> class which is used to update
     * the presence status according the current free busy status.
     */
    private InMeetingStatusPolicy inMeetingStatusPolicy
        = new InMeetingStatusPolicy();
    
    public ProviderPresenceStatusListener presenceStatusListener 
        = new ProviderPresenceStatusListener()
        {
            
            @Override
            public void providerStatusMessageChanged(PropertyChangeEvent evt)
            {
                
            }
            
            @Override
            public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
            {
                if(evt.getNewStatus().isOnline())
                {
                    inMeetingStatusPolicy.handleProtocolProvider(
                        evt.getProvider(), null, false, true);
                }
            }
        };

    /**
     * The flag which signals that MAPI strings should be returned in the
     * unicode character set.
     */
    public static final long MAPI_UNICODE = 0x80000000;

    static
    {
        System.loadLibrary("jmsoutlookaddrbook");
    }

    /**
     * Adds <tt>CalendarItemTimerTask</tt> to the map of tasks.
     * @param id the id of the calendar item to be added.
     * @param task the <tt>CalendarItemTimerTask</tt> instance to be added.
     */
    public void addToTaskMap(String id, CalendarItemTimerTask task)
    {
        synchronized(taskMap)
        {
            taskMap.put(id, task);
        }
    }

    /**
     * Removes <tt>CalendarItemTimerTask</tt> from the map of tasks.
     * @param id the id of the calendar item to be removed.
     */
    public void removeFromTaskMap(String id)
    {
        synchronized(taskMap)
        {
            taskMap.remove(id);
        }
    }

    /**
     * Adds <tt>CalendarItemTimerTask</tt> to the list of current tasks.
     * @param task the <tt>CalendarItemTimerTask</tt> instance to be added.
     */
    public void addToCurrentItems(CalendarItemTimerTask task)
    {
        synchronized(currentCalendarItems)
        {
            currentCalendarItems.add(task);
        }
    }

    /**
     * Removes <tt>CalendarItemTimerTask</tt> from the list of current tasks.
     * @param task the task of the calendar item to be removed.
     */
    public void removeFromCurrentItems(CalendarItemTimerTask task)
    {
        synchronized(currentCalendarItems)
        {
            currentCalendarItems.remove(task);
        }
    }

    /**
     * Retrieves, parses and stores all the calendar items from the outlook.
     */
    public void start()
    {
        getAllCalendarItems(new NotificationsDelegate());
    }

    /**
     * Retrieves, parses and stores all the calendar items from the outlook.
     * @param callback the callback object that receives the results.
     */
    private static native void getAllCalendarItems(
        NotificationsDelegate callback);

    /**
     * Returns array of property values for the given calendar item.
     * @param entryId the entry id of the calendar item.
     * @param propIds the IDs of the properties that we are interested for.
     * @param flags the flags.
     * @return array of property values for the given calendar item.
     * @throws MsOutlookMAPIHResultException
     */
    public static native Object[] IMAPIProp_GetProps(String entryId,
        long[] propIds, long flags)
    throws MsOutlookMAPIHResultException;

    /**
     * Gets the property values of given calendar item and creates
     * <tt>CalendarItemTimerTask</tt> instance for it.
     * @param id The outlook calendar item identifier.
     *
     * @throws MsOutlookMAPIHResultException if anything goes wrong while
     * getting the properties of the calendar item.
     */
    private synchronized void insert(String id)
        throws MsOutlookMAPIHResultException
    {
        Object[] props = null;
        props
            = IMAPIProp_GetProps(id, MAPICalendarProperties.getALLPropertyIDs(),
                MAPI_UNICODE);

        addCalendarItem(props, id);
    }

    /**
     * Parses the property values of calendar item and creates
     * <tt>CalendarItemTimerTask</tt> instance for the calendar item.
     * @param props the property values.
     * @param id the ID of the calendar item.
     */
    private void addCalendarItem(Object[] props, String id)
    {
        Date startTime = null, endTime = null;
        BusyStatusEnum status = BusyStatusEnum.FREE;
        ResponseStatus responseStatus = ResponseStatus.respNone;
        boolean isRecurring = false;
        byte[] recurringData = null;
        for(int i = 0; i < props.length; i++)
        {
            if(props[i] == null)
                continue;
            MAPICalendarProperties propertyName
                = MAPICalendarProperties.getByOrderId(i);
            switch(propertyName)
            {
                case PidLidAppointmentStartWhole:
                    try
                    {
                        long time
                            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
                                .parse((String)props[i] + " UTC").getTime();
                        startTime = new Date(time);
                    }
                    catch (ParseException e)
                    {
                        logger.error("Cannot parse date string: " + props[i]);
                        return;
                    }
                    break;
                case PidLidAppointmentEndWhole:
                    try
                    {
                        long time
                            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
                                .parse((String)props[i] + " UTC").getTime();
                        endTime = new Date(time);
                    }
                    catch (ParseException e)
                    {
                        logger.error("Cannot parse date string: " + props[i]);
                        return;
                    }
                    break;
                case PidLidBusyStatus:
                    status = BusyStatusEnum.getFromLong((Long)props[i]);
                    break;
                case PidLidRecurring:
                    isRecurring = (Boolean)props[i];
                    break;
                case PidLidAppointmentRecur:
                    recurringData = ((byte[])props[i]);
                    break;
                case PidLidResponseStatus:
                    responseStatus
                        = ResponseStatus.getFromLong((Long) props[i]);
                    break;
            }
        }

        if(responseStatus != ResponseStatus.respNone
            && responseStatus != ResponseStatus.respAccepted
            && responseStatus != ResponseStatus.respOrganized)
        return;

        if(status == BusyStatusEnum.FREE || startTime == null || endTime == null)
            return;

        Date currentTime = new Date();

        boolean executeNow = false;

        if(startTime.before(currentTime) || startTime.equals(currentTime))
            executeNow = true;

        CalendarItemTimerTask task = null;
        if(recurringData != null)
        {
            task = new CalendarItemTimerTask(status, startTime, endTime, id,
                executeNow, null);
            try
            {
                RecurringPattern pattern
                    = new RecurringPattern(recurringData, task);
                task.setPattern(pattern);
            }
            catch(IndexOutOfBoundsException e)
            {
                logger.error(
                    "Error parsing reccuring pattern." + e.getMessage(),e);
                logger.error("Reccuring data:\n" + bytesToHex(recurringData));
                return;
            }
        }

        if(endTime.before(currentTime) || endTime.equals(currentTime))
        {
            if(isRecurring)
            {
                task = task.getPattern().next(startTime, endTime);
            }
            else
                return;
        }

        if(task == null)
            task = new CalendarItemTimerTask(status, startTime, endTime, id,
                executeNow, null);

        task.scheduleTasks();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b: bytes)
           sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    /**
     * Changes the value of the current status
     * @param state the new value.
     */
    protected void setCurrentState(BusyStatusEnum state)
    {
        if(currentState == state)
            return;
        BusyStatusEnum oldState = currentState;
        this.currentState = state;
        if((oldState == BusyStatusEnum.FREE && state != BusyStatusEnum.FREE)
            || (oldState != BusyStatusEnum.FREE && state == BusyStatusEnum.FREE))
        {
            inMeetingStatusPolicy.freeBusyStateChanged();
        }

    }

    /**
     * Handles presence status changed from "On the Phone"
     *
     * @param presenceStatuses the remembered presence statuses
     * @return <tt>true</tt> if the status is changed.
     */
    public boolean onThePhoneStatusChanged(
        Map<ProtocolProviderService,PresenceStatus> presenceStatuses)
    {
        if(currentState != BusyStatusEnum.FREE)
        {
            inMeetingStatusPolicy.onThePhoneStatusChanged(presenceStatuses);
            return true;
        }
        return false;
    }

    /**
     * Calculates and changes the value of current status using the current
     * active calendar items and their statuses.
     */
    public void updateStateFromCurrentItems()
    {
        BusyStatusEnum tmpState = BusyStatusEnum.FREE;
        synchronized(currentCalendarItems)
        {
            for(CalendarItemTimerTask task : currentCalendarItems)
            {
                if(tmpState.getPriority() < task.getStatus().getPriority())
                {
                    tmpState = task.getStatus();
                }
            }
        }
        setCurrentState(tmpState);
    }

    @Override
    public BusyStatusEnum getStatus()
    {
        return currentState;
    }

    /**
     * The method is not implemented yet.
     */
    @Override
    public void addFreeBusySateListener(FreeBusySateListener listener)
    {

    }

    /**
     * The method is not implemented yet.
     */
    @Override
    public void removeFreeBusySateListener(FreeBusySateListener listener)
    {

    }

    /**
     * Implements the policy to have the presence statuses of online accounts
     * (i.e. registered <tt>ProtocolProviderService</tt>s) set to
     * &quot;In meeting&quot; according the free busy status.
     *
     */
    private class InMeetingStatusPolicy
    {
        /**
         * The regular expression which removes whitespace from the
         * <tt>statusName</tt> property value of <tt>PresenceStatus</tt>
         * instances in order to recognize the <tt>PresenceStatus</tt> which
         * represents &quot;In meeting&quot;.
         */
        private final Pattern presenceStatusNameWhitespace
            = Pattern.compile("\\p{Space}");

        /**
         * The <tt>PresenceStatus</tt>es of <tt>ProtocolProviderService</tt>s
         * before they were changed to &quot;In meeting&quot; remembered so
         * that they can be restored.
         */
        private final Map<ProtocolProviderService,PresenceStatus>
            presenceStatuses
                = Collections.synchronizedMap(
                        new WeakHashMap<ProtocolProviderService,PresenceStatus>());

        /**
         * Notifies this instance that the free busy status has changed.
         */
        public void freeBusyStateChanged()
        {
            run(false);
        }

        /**
         * Handles presence status changed from "On the Phone"
         * @param presenceStatuses the remembered presence statuses
         */
        public void onThePhoneStatusChanged(
            Map<ProtocolProviderService,PresenceStatus> presenceStatuses)
        {
            run(true);
            for(ProtocolProviderService pps : presenceStatuses.keySet())
                rememberPresenceStatus(pps, presenceStatuses.get(pps));
        }

        /**
         * Returns the remembered presence statuses
         * @return the remembered presence statuses
         */
        public Map<ProtocolProviderService,PresenceStatus> getRememberedStatuses()
        {
            return presenceStatuses;
        }

        /**
         * Finds the first <tt>PresenceStatus</tt> among the set of
         * <tt>PresenceStatus</tt>es supported by a specific
         * <tt>OperationSetPresence</tt> which represents
         * &quot;In meeting&quot;.
         *
         * @param presence the <tt>OperationSetPresence</tt> which represents
         * the set of supported <tt>PresenceStatus</tt>es
         * @return the first <tt>PresenceStatus</tt> among the set of
         * <tt>PresenceStatus</tt>es supported by <tt>presence</tt> which
         * represents &quot;In meeting&quot; if such a <tt>PresenceStatus</tt>
         * was found; otherwise, <tt>null</tt>
         */
        private PresenceStatus findInMeetingPresenceStatus(
                OperationSetPresence presence)
        {
            for (Iterator<PresenceStatus> i = presence.getSupportedStatusSet();
                    i.hasNext();)
            {
                PresenceStatus presenceStatus = i.next();

                if (presenceStatusNameWhitespace
                        .matcher(presenceStatus.getStatusName())
                            .replaceAll("")
                                .equalsIgnoreCase("InAMeeting"))
                {
                    return presenceStatus;
                }
            }
            return null;
        }

        /**
         * Finds the first <tt>PresenceStatus</tt> among the set of
         * <tt>PresenceStatus</tt>es supported by a specific
         * <tt>OperationSetPresence</tt> which represents
         * &quot;On the phone&quot;.
         *
         * @param presence the <tt>OperationSetPresence</tt> which represents
         * the set of supported <tt>PresenceStatus</tt>es
         * @return the first <tt>PresenceStatus</tt> among the set of
         * <tt>PresenceStatus</tt>es supported by <tt>presence</tt> which
         * represents &quot;On the phone&quot; if such a <tt>PresenceStatus</tt>
         * was found; otherwise, <tt>null</tt>
         */
        private PresenceStatus findOnThePhonePresenceStatus(
                OperationSetPresence presence)
        {
            for (Iterator<PresenceStatus> i = presence.getSupportedStatusSet();
                    i.hasNext();)
            {
                PresenceStatus presenceStatus = i.next();

                if (presenceStatusNameWhitespace
                        .matcher(presenceStatus.getStatusName())
                            .replaceAll("")
                                .equalsIgnoreCase("OnThePhone"))
                {
                    return presenceStatus;
                }
            }
            return null;
        }

        /**
         * Removes the remembered presence status for given provider
         * @param pps the provider
         * @return the removed value
         */
        private PresenceStatus forgetPresenceStatus(ProtocolProviderService pps)
        {
            return presenceStatuses.remove(pps);
        }

        /**
         * Removes all remembered presence statuses.
         */
        private void forgetPresenceStatuses()
        {
            presenceStatuses.clear();
        }

        /**
         * Determines whether the free busy status is busy or not
         *
         * @return <tt>true</tt> if the status is busy and <tt>false</tt> if the
         * status is free
         */
        private boolean isInMeeting()
        {
            return currentState != BusyStatusEnum.FREE;
        }

        /**
         * Invokes
         * {@link OperationSetPresence#publishPresenceStatus(PresenceStatus,
         * String)} on a specific <tt>OperationSetPresence</tt> with a specific
         * <tt>PresenceStatus</tt> and catches any exceptions.
         *
         * @param presence the <tt>OperationSetPresence</tt> on which the method
         * is to be invoked
         * @param presenceStatus the <tt>PresenceStatus</tt> to provide as the
         * respective method argument value
         */
        private void publishPresenceStatus(
                OperationSetPresence presence,
                PresenceStatus presenceStatus)
        {
            try
            {
                presence.publishPresenceStatus(presenceStatus, null);
            }
            catch (Throwable t)
            {
                if (t instanceof InterruptedException)
                    Thread.currentThread().interrupt();
                else if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
            }
        }

        private PresenceStatus rememberPresenceStatus(
                ProtocolProviderService pps,
                PresenceStatus presenceStatus)
        {
            return presenceStatuses.put(pps, presenceStatus);
        }

        /**
         * Applies this policy to the current state of the application.
         */
        private void run(boolean onThePhoneStatusChanged)
        {
            List<ProtocolProviderService> providers
                = AddrBookActivator.getProtocolProviders();

            if ((providers == null) || (providers.size() == 0))
            {
                forgetPresenceStatuses();
            }
            else
            {
                boolean isInMeeting = isInMeeting();

                for (ProtocolProviderService pps : providers)
                {
                    if (pps == null)
                        continue;

                    handleProtocolProvider(pps, isInMeeting, 
                        onThePhoneStatusChanged, false);
                }
            }
        }

        public void handleProtocolProvider(ProtocolProviderService pps,
            Boolean isInMeeting, boolean onThePhoneStatusChanged, 
            boolean dontAddListeners)
        {
            
            if(isInMeeting == null)
                isInMeeting = isInMeeting();

            OperationSetPresence presence
                = pps.getOperationSet(OperationSetPresence.class);
            if (presence == null)
            {
                /*
                 * "In meeting" is a PresenceStatus so it is available
                 * only to accounts which support presence in the first
                 * place.
                 */
                forgetPresenceStatus(pps);
            }
            else if (pps.isRegistered())
            {
                PresenceStatus inMeetingPresenceStatus
                    = findInMeetingPresenceStatus(presence);

                PresenceStatus onThePhone
                    = findOnThePhonePresenceStatus(presence);

                if (inMeetingPresenceStatus == null)
                {
                    /*
                     * If do not know how to define "On the phone" for
                     * an OperationSetPresence, then we'd better not
                     * mess with it in the first place.
                     */
                    forgetPresenceStatus(pps);
                }
                else if (isInMeeting)
                {
                    if(!dontAddListeners)
                    {
                        presence.addProviderPresenceStatusListener(
                            presenceStatusListener);
                    }
                    PresenceStatus presenceStatus
                        = presence.getPresenceStatus();

                    if (presenceStatus == null)
                    {
                        logger.info("HANDLE provider 55");
                        /*
                         * It is strange that an OperationSetPresence
                         * does not have a PresenceStatus so it may be
                         * safer to not mess with it.
                         */
                        forgetPresenceStatus(pps);
                        presence.removeProviderPresenceStatusListener(
                            presenceStatusListener);
                    }
                    else if (!inMeetingPresenceStatus.equals(
                            presenceStatus)
                            && (!presenceStatus.equals(onThePhone)
                                || onThePhoneStatusChanged))
                    {
                        if(!dontAddListeners)
                        {
                            if(!presenceStatus.isOnline())
                            {
                                return;
                            }
                            presence.removeProviderPresenceStatusListener(
                                presenceStatusListener);
                        }
                        publishPresenceStatus(
                                presence,
                                inMeetingPresenceStatus);
                        if (inMeetingPresenceStatus.equals(
                                presence.getPresenceStatus()))
                        {
                            rememberPresenceStatus(pps, presenceStatus);
                        }
                        else
                        {
                            forgetPresenceStatus(pps);
                        }
                    }
                    else
                    {
                        presence.removeProviderPresenceStatusListener(
                            presenceStatusListener);
                    }
                }
                else
                {
                    PresenceStatus presenceStatus
                        = forgetPresenceStatus(pps);

                    if ((presenceStatus != null)
                            && inMeetingPresenceStatus.equals(
                                    presence.getPresenceStatus()))
                    {
                        publishPresenceStatus(presence, presenceStatus);
                    }
                }
            }
            else
            {
                forgetPresenceStatus(pps);
            }

        }
    }

    /**
     * Delegate class to be notified for calendar changes.
     */
    public class NotificationsDelegate
    {
        /**
         * Callback method when receiving notifications for inserted items.
         */
        public void inserted(String id)
        {
            try
            {
                insert(id);
            }
            catch (MsOutlookMAPIHResultException e)
            {
                e.printStackTrace();
            }
        }

        /**
         * Callback method when receiving notifications for updated items.
         */
        public void updated(String id)
        {
            try
            {
                synchronized(taskMap)
                {
                    CalendarItemTimerTask task = taskMap.get(id);
                    //Expired tasks can be removed earlier from the taskMap.
                    if(task != null)
                    {
                        task.remove();
                    }
                }
                insert(id);
            }
            catch (MsOutlookMAPIHResultException e)
            {
                e.printStackTrace();
            }
        }

        /**
         * Callback method when receiving notifications for deleted items.
         */
        public void deleted(String id)
        {
            synchronized(taskMap)
            {
                CalendarItemTimerTask task = taskMap.get(id);
                if(task != null)
                {
                    task.remove();
                }
            }
        }

        /**
         * Callback method when receiving notifications for deleted items.
         */
        public boolean callback(String id)
        {
            try
            {
                insert(id);
            }
            catch (MsOutlookMAPIHResultException e)
            {
                e.printStackTrace();
            }
            return true;
        }
    }

    public void handleProviderAdded(ProtocolProviderService pps)
    {
        inMeetingStatusPolicy.handleProtocolProvider(pps, null, false, false);
    }

    /**
     * Returns the remembered presence statuses
     * @return the remembered presence statuses
     */
    public Map<ProtocolProviderService,PresenceStatus> getRememberedStatuses()
    {
        return inMeetingStatusPolicy.getRememberedStatuses();
    }
}
