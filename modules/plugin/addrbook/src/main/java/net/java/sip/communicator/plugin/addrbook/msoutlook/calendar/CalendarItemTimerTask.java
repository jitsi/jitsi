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

import java.util.*;

import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.service.calendar.*;

/**
 * A class that represents one calendar item. It schedules tasks for the 
 * beginning and for the end of the calendar item to update the free busy status
 * 
 * @author Hristo Terezov
 */
public class CalendarItemTimerTask
{
    /**
     * The status of the calendar item.
     */
    private final CalendarService.BusyStatusEnum state;

    /**
     * The start date of the calendar item.
     */
    private final Date startDate;

    /**
     * The end date of the calendar item.
     */
    private final Date endDate;

    /**
     * The ID of the calendar item.
     */
    private final String id;

    /**
     * Indicates if the start task should be executed immediately or not. This 
     * flag is <tt>true</tt> if the start date is before the current date.
     */
    private final boolean executeNow;

    /**
     * The <tt>CalendarServiceImpl</tt> instance.
     */
    private final CalendarServiceImpl calendarService 
        = AddrBookActivator.getCalendarService();

    /**
     * The <tt>Timer</tt> instance that schedules the tasks.
     */
    private static Timer timer = new Timer();

    /**
     * The <tt>RecurringPattern</tt> instance associated with the calendar item.
     * This must be <tt>null</tt> if the calendar item is not recurring.
     */
    private RecurringPattern pattern;

    /**
     * The task that will be executed at the beginning of the task.
     */
    private TimerTask startTask = new TimerTask()
    {
        @Override
        public void run()
        {
            start();
        }
    };

    /**
     * The task that will be executed at the end of the task.
     */
    private TimerTask endTask = new TimerTask()
    {
        @Override
        public void run()
        {
            stop();
        }
    };

    /**
     * Constructs new <tt>CalendarItemTimerTask</tt> instance.
     * @param state the state of the calendar item.
     * @param startDate the start date of the calendar item.
     * @param endDate the end date of the calendar item.
     * @param id the ID of the calendar item.
     * @param executeNow Indicates if the start task should be executed 
     * immediately or not
     * @param pattern the <tt>RecurringPattern</tt> instance associated with the
     * calendar item. It must be <tt>null</tt> if the calendar item is not 
     * recurring.
     */
    public CalendarItemTimerTask(CalendarService.BusyStatusEnum state, 
        Date startDate, Date endDate, String id, boolean executeNow, 
        RecurringPattern pattern)
    {
        this.state = state;
        this.startDate = startDate;
        this.endDate = endDate;
        this.id = id;
        calendarService.addToTaskMap(id, this);
        this.executeNow = executeNow;
        this.pattern = pattern;
    }

    /**
     * Returns the <tt>RecurringPattern</tt> instance associated with the 
     * calendar item.
     * @return the <tt>RecurringPattern</tt> instance associated with the 
     * calendar item.
     */
    public RecurringPattern getPattern()
    {
        return pattern;
    }

    /**
     * Returns the ID of the calendar item.
     * @return the ID of the calendar item.
     */
    public String getId()
    {
        return id;
    }

    /**
     * This method is executed in the beginning of the calendar item.
     */
    protected void start()
    {
        calendarService.addToCurrentItems(this);
        calendarService.updateStateFromCurrentItems();
    }

    /**
     * This method is executed in the end of the calendar item.
     */
    protected void stop()
    {
        calendarService.removeFromTaskMap(id);
        calendarService.removeFromCurrentItems(this);
        calendarService.updateStateFromCurrentItems();
        if(pattern != null)
        {
            CalendarItemTimerTask nextTask 
                = pattern.next(startDate, endDate);
            this.pattern = null;
            nextTask.scheduleTasks();
        }

    }

    /**
     * Schedules the start and end tasks of the calendar item.
     */
    public void scheduleTasks()
    {
        if(!executeNow)
        {
            timer.schedule(startTask, startDate);
        }
        else
        {
            startTask.run();
        }
        timer.schedule(endTask, endDate);
    }

   /**
    * Removes the task. 
    */
    public void remove()
    {
        startTask.cancel();
        endTask.cancel();
        calendarService.removeFromTaskMap(id);
        calendarService.removeFromCurrentItems(this);
        calendarService.updateStateFromCurrentItems();
    }

    /**
     * Returns the free busy status of the calendar item.
     * @return the free busy status of the calendar item.
     */
    public CalendarService.BusyStatusEnum getStatus()
    {
        return state;
    }


    /**
     * Returns the start date of the calendar item
     * @return the start date of the calendar item
     */
    public Date getStartDate()
    {
        return startDate;
    }

    /**
     * Returns the end date of the calendar item
     * @return the end date of the calendar item
     */
    public Date getEndDate()
    {
        return endDate;
    }

    /**
     * Sets the <tt>RecurringPattern</tt> associated with the calendar item.
     * @param pattern the pattern to set
     */
    public void setPattern(RecurringPattern pattern)
    {
        this.pattern = pattern;
    }
}
