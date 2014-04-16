/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook.calendar;

import java.text.*;
import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.plugin.addrbook.msoutlook.*;
import net.java.sip.communicator.service.calendar.*;
import net.java.sip.communicator.service.protocol.*;
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
        PidLidAppointmentRecur(0x00008216, MAPIType.PT_BINARY);

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

        public long getID()
        {
            return id;
        }

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
            }
        }

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
            RecurringPattern pattern 
                = new RecurringPattern(recurringData, task);
            task.setPattern(pattern);
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
        private final Pattern inMeetingPresenceStatusNameWhitespace
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
            run();
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

                if (inMeetingPresenceStatusNameWhitespace
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
        private void run()
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

                    handleProtocolProvider(pps, isInMeeting);
                }
            }
        }

        public void handleProtocolProvider(ProtocolProviderService pps, 
            Boolean isInMeeting)
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
                    PresenceStatus presenceStatus
                        = presence.getPresenceStatus();

                    if (presenceStatus == null)
                    {
                        /*
                         * It is strange that an OperationSetPresence
                         * does not have a PresenceStatus so it may be
                         * safer to not mess with it.
                         */
                        forgetPresenceStatus(pps);
                    }
                    else if (!inMeetingPresenceStatus.equals(
                            presenceStatus))
                    {
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
                /*
                 * Offline accounts do not get their PresenceStatus
                 * modified for the purposes of "On the phone".
                 */
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
            synchronized(taskMap)
            {
                CalendarItemTimerTask task = taskMap.get(id);
                if(task != null)
                    task.remove();
            }
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
         * Callback method when receiving notifications for deleted items.
         */
        public void deleted(String id)
        {
            synchronized(taskMap)
            {
                CalendarItemTimerTask task = taskMap.get(id);
                if(task != null)
                    task.remove();
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
        inMeetingStatusPolicy.handleProtocolProvider(pps, null);
    }
}
