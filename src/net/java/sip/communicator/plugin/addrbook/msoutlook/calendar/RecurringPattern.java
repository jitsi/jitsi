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

import java.nio.*;
import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.calendar.*;

/**
 * The class represents the recurring pattern structure of calendar item.
 * 
 * @author Hristo Terezov
 */
public class RecurringPattern
{
    /**
     * Enum for the type of the pattern.
     */
    public enum PatternType
    {
        /**
         * Daily recurrence.
         */
        Day((short)0x0000),

        /**
         * Weekly recurrence.
         */
        Week((short)0x0001),

        /**
         * Monthly recurrence.
         */
        Month((short)0x0002),

        /**
         * Monthly recurrence.
         */
        MonthNth((short)0x0003),

        /**
         * Monthly recurrence.
         */
        MonthEnd((short)0x004),

        /**
         * Monthly recurrence.
         */
        HjMonth((short)0x000A),

        /**
         * Monthly recurrence.
         */
        HjMonthNth((short)0x000B),

        /**
         * Monthly recurrence.
         */
        HjMonthEnd((short)0x000C);

        /**
         * The value of the type.
         */
        private final short value;

        /**
         * Constructs new <tt>PatternType</tt> instance.
         * @param value the value.
         */
        PatternType(short value)
        {
            this.value = value;
        }

        /**
         * Returns the value of the <tt>PatternType</tt> instance.
         * @return the value
         */
        public short getValue()
        {
            return value;
        }

        /**
         * Finds the <tt>PatternType</tt> by given value.
         * @param value the value
         * @return the found <tt>PatternType</tt> instance or null if no type is
         * found.
         */
        public static PatternType getFromShort(short value)
        {
            for(PatternType type : values())
            {
                if(type.getValue() == value)
                    return type;
            }
            return null;
        }
    }

    /**
     * The value of recurFrequency field.
     */
    private short recurFrequency;

    /**
     * The value of patternType field.
     */
    private PatternType patternType;

    /**
     * The value of calendarType field.
     */
    private short calendarType;

    /**
     * The value of firstDateTime field.
     */
    private int firstDateTime;

    /**
     * The value of period field.
     */
    private int period;

    /**
     * The value of slidingFlag field.
     */
    private int slidingFlag;

    /**
     * The value of patternSpecific1 field.
     */
    private int patternSpecific1;

    /**
     * The value of patternSpecific2 field.
     */
    private int patternSpecific2;

    /**
     * The value of endType field.
     */
    private int endType;

    /**
     * The value of occurenceCount field.
     */
    private int occurenceCount;

    /**
     * The value of firstDow field.
     */
    private int firstDow;

    /**
     * The value of deletedInstanceCount field.
     */
    private int deletedInstanceCount;

    /**
     * The value of modifiedInstanceCount field.
     */
    private int modifiedInstanceCount;

    /**
     * The value of startDate field.
     */
    private int startDate;

    /**
     * The value of endDate field.
     */
    private int endDate;

    /**
     * List with the start dates of deleted instances.
     */
    private List<Date> deletedInstances = new ArrayList<Date>();

    /**
     * Array with the start dates of modified instances.
     */
    private int[] modifiedInstances;

    /**
     * List of exception info structures included in the pattern.
     */
    private List<ExceptionInfo> exceptionInfo;

    /**
     * The source calendar item of the recurrent series.
     */
    private CalendarItemTimerTask sourceTask;

    /**
     * List of days of week when the calendar item occurred.
     */
    private List<Integer> allowedDaysOfWeek = new LinkedList<Integer>();

    /**
     * The binary data of the pattern.
     */
    private ByteBuffer dataBuffer;

    /**
     * Array with masks for days of week when the calendar item occurs.
     */
    public static int[] weekOfDayMask 
        = {0x00000001, 0x00000002, 0x00000004, 0x00000008, 0x00000010, 
        0x00000020, 0x00000040};

    /**
     * Parses the binary data that describes the recurrent pattern.
     * @param data the binary data.
     * @param sourceTask the calendar item.
     * @throws IndexOutOfBoundsException if data can't be parsed.
     */
    public RecurringPattern(byte[] data, CalendarItemTimerTask sourceTask)
    throws IndexOutOfBoundsException
    {
        this.sourceTask = sourceTask;
        dataBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        
        int offset = 4;
        recurFrequency = dataBuffer.getShort(offset);
        offset += 2;

        patternType = PatternType.getFromShort(dataBuffer.getShort(offset));
        offset += 2;

        calendarType = dataBuffer.getShort(offset);
        offset += 2;

        firstDateTime = dataBuffer.getInt(offset);
        offset += 4;

        period = dataBuffer.getInt(offset);
        offset += 4;

        slidingFlag = dataBuffer.getInt(offset);
        offset += 4;

        switch(patternType)
        {
        case Week:
        case Month:
        case MonthEnd:
        case HjMonth:
        case HjMonthEnd:
            patternSpecific1 = dataBuffer.getInt(offset);
            patternSpecific2 = 0;
            offset +=4;
            if(patternType == PatternType.Week)
            {
                for(int day = firstDow; day < firstDow + 7; day++)
                {
                    if((patternSpecific1 & (weekOfDayMask[day%7])) != 0)
                        allowedDaysOfWeek.add((day%7) + 1);
                }
            }
            break;
        case MonthNth:
        case HjMonthNth:
            patternSpecific1 = dataBuffer.getInt(offset);
            patternSpecific2 = dataBuffer.getInt(offset + 4);
            if(patternSpecific1 == 0x7f && patternSpecific2 != 0x5)
            {
                patternType = PatternType.Month;
            }
            for(int day = 0; day < 7; day++)
            {
                if((patternSpecific1 & (weekOfDayMask[day])) != 0)
                    allowedDaysOfWeek.add((day) + 1);
            }
            offset +=8;
            break;
        default:
            break;
        }

        //endType
        endType = dataBuffer.getInt(offset);
        offset += 4;

        occurenceCount = dataBuffer.getInt(offset);
        offset += 4;

        firstDow = dataBuffer.getInt(offset);
        offset += 4;

        deletedInstanceCount = dataBuffer.getInt(offset);
        offset += 4;

        //deleted instances
        for(int i = 0; i < deletedInstanceCount; i ++)
        {
            deletedInstances.add(
                windowsTimeToDateObject(dataBuffer.getInt(offset)));
            offset += 4;
        }


        modifiedInstanceCount  = dataBuffer.getInt(offset);
        offset += 4;

        //modified instances
        modifiedInstances = new int[modifiedInstanceCount];

        for(int i = 0; i < modifiedInstanceCount; i ++)
        {
            modifiedInstances[i] = dataBuffer.getInt(offset);
            offset += 4;
        }


        startDate = dataBuffer.getInt(offset);
        offset += 4;

        endDate = dataBuffer.getInt(offset);
        offset += 4;

        offset += 16;

        short exceptionCount = dataBuffer.getShort(offset);
        offset += 2;
        exceptionInfo = new ArrayList<ExceptionInfo>(exceptionCount);
        for(int i = 0; i < exceptionCount;i++)
        {
            ExceptionInfo tmpExceptionInfo = new ExceptionInfo(offset);
            exceptionInfo.add(tmpExceptionInfo);
            offset += tmpExceptionInfo.sizeInBytes();

            CalendarService.BusyStatusEnum status 
                = tmpExceptionInfo.getBusyStatus();
            Date startTime = tmpExceptionInfo.getStartDate();
            Date endTime = tmpExceptionInfo.getEndDate();
            if(status == CalendarService.BusyStatusEnum.FREE 
                || startTime == null || endTime == null)
                continue;
            Date currentTime = new Date();

            if(endTime.before(currentTime) || endTime.equals(currentTime))
                return;

            boolean executeNow = false;

            if(startTime.before(currentTime) || startTime.equals(currentTime))
                executeNow = true;

            CalendarItemTimerTask task = new CalendarItemTimerTask(status, 
                startTime, endTime, sourceTask.getId(), executeNow, this);

            task.scheduleTasks();
        }
    }

    /**
     * Converts windows time in minutes from 1/1/1601 to <tt>Date</tt> object.
     * @param time the number of minutes from 1/1/1601
     * @return the <tt>Date</tt> object
     */
    public static Date windowsTimeToDateObject(long time) {
        // Date.parse("1/1/1601") == 11644473600000L 
        long date = time * 60000 - 11644473600000L;
        date -= TimeZone.getDefault().getOffset(date);
        return new Date(date);
    }

    /**
     * Prints the properties of the class for debugging purpose.
     */
    @Override
    public String toString()
    {
        String result = "";
        result 
            += "recurFrequency: " + String.format("%#02x", this.recurFrequency) 
                + "\n";
        result += "patternType: " 
            + String.format("%#02x", this.patternType.getValue()) + "\n";
        result += "calendarType: " 
            + String.format("%#02x", this.calendarType) + "\n";
        result += "endType: " + String.format("%#04x", this.endType) + "\n";

        result += "period: " + this.period + "\n";
        result += "occurenceCount: " 
            + String.format("%#04x", this.occurenceCount) + "\n";
        result += "patternSpecific1: " 
            + String.format("%#04x", this.patternSpecific1) + "\n";
        result += "patternSpecific2: " 
            + String.format("%#04x", this.patternSpecific2) + "\n";
        result += "startDate hex: " + String.format("%#04x", this.startDate) 
            + "\n";
        result += "endDate hex: " + String.format("%#04x", this.endDate) + "\n";

        result += "startDate: " 
            +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                windowsTimeToDateObject(this.startDate))  + "\n";
        result += "endDate: " 
            +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                windowsTimeToDateObject(this.endDate))  + "\n";


        for(int i = 0; i < modifiedInstanceCount; i++)
        {
            result += "modified Instance date hex: " 
                + String.format("%#04x", this.modifiedInstances[i]) + "\n";

            result += "modified Instance date: " 
                +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(
                    windowsTimeToDateObject(this.modifiedInstances[i]))  + "\n";
        }

        for(int i = 0; i < deletedInstanceCount; i++)
        {
            result += "deleted Instance date: "
                +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(
                    deletedInstances.get(i))  + "\n";
        }
        result += "patternSpecific2: " 
            + String.format("%#04x", this.patternSpecific2) + "\n";

        result += "\n\n =====================Exeptions====================\n\n";

        for(ExceptionInfo info : exceptionInfo)
        {
            result += info.toString() + "\n\n";
        }
        return result;
    }

    /**
     * Checks whether the given date is in the recurrent pattern range or not
     * @param date the date
     * @return <tt>true</tt> if the date is in the pattern range.
     */
    private boolean dateOutOfRange(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        if((endType != 0x00002023) && (endType != 0xFFFFFFFF) 
            && cal.getTime().after(windowsTimeToDateObject(this.endDate)))
        {
            return true;// the series are finished
        }
        return false;
    }

    /**
     * Calculates and creates the next calendar item.
     * @param previousStartDate the start date of the previous occurrence.
     * @param previousEndDate the end date of the previous occurrence.
     * @return the new calendar item or null if there are no more calendar items
     * from that recurrent series.
     */
    public CalendarItemTimerTask next(Date previousStartDate, 
        Date previousEndDate)
    {
        if(dateOutOfRange(new Date()))
        {
            return null;
        }
        Date startDate = previousStartDate;
        Date endDate = null;
        boolean executeNow = false;
        long duration = sourceTask.getEndDate().getTime() 
            - sourceTask.getStartDate().getTime();
        switch(patternType)
        {
        case Day:
        {
            startDate 
                = new Date(startDate.getTime() + period * 60000);
            endDate = new Date(
                previousEndDate.getTime() + period * 60000);
            Date currentDate = new Date();
            if(endDate.before(currentDate))
            {
                long offset 
                    = currentDate.getTime() - endDate.getTime();
                offset -= offset %  (period * 60000);
                if(endDate.getTime() + offset  < currentDate.getTime())
                {
                    offset += period * 60000;
                }

                startDate = new Date(startDate.getTime() + offset);

            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            Calendar cal2 = (Calendar) cal.clone();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            while(deletedInstances.contains(cal.getTime()))
            {
                cal.add(Calendar.MINUTE, period);
                cal2.add(Calendar.MINUTE, period);
            }

            if(dateOutOfRange(cal.getTime()))
            {
                return null;
            }
            startDate = cal2.getTime();
            endDate = new Date(startDate.getTime() + duration);
            if(startDate.before(currentDate))
            {
                executeNow = true;
            }

            return new CalendarItemTimerTask(
                sourceTask.getStatus(), 
                startDate, endDate, sourceTask.getId(), executeNow, this);
        }
        case Week:
        {
            Calendar cal = Calendar.getInstance();
            /**
             * The enum for the firstDow field is the same as Calendar day of 
             * week enum + 1 day
             */
            cal.setFirstDayOfWeek(firstDow + 1);
            cal.setTime(startDate);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int index = allowedDaysOfWeek.indexOf(dayOfWeek);
            if(++index < allowedDaysOfWeek.size())
            {
                cal.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(index));
                startDate = cal.getTime();
                endDate = new Date(startDate.getTime()  + duration);
            }
            else
            {
                cal.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(0));
                cal.add(Calendar.WEEK_OF_YEAR, period);
                startDate = cal.getTime();
                endDate = new Date(startDate.getTime()  + duration);
            }
            Date currentDate = new Date();
            if(endDate.before(currentDate))
            {
                cal.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(0));
                endDate = new Date(cal.getTimeInMillis() + duration);
                long offset = (currentDate.getTime() - endDate.getTime());

                //1 week = 604800000 is milliseconds
                offset -= offset % (period * 604800000);
                if(endDate.getTime() + offset  < currentDate.getTime())
                {
                    cal.add(Calendar.WEEK_OF_YEAR, 
                        (int)(offset / (period * 604800000)));
                    int i = 1;
                    while(((cal.getTimeInMillis() + duration) 
                        < (currentDate.getTime())))
                    {
                        if(i == allowedDaysOfWeek.size())
                        {
                            cal.add(Calendar.WEEK_OF_YEAR, period);
                            i = 0;
                        }
                        cal.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(i));
                        i++;
                    }

                    startDate = cal.getTime();
                }
                else
                {
                    startDate = new Date(cal.getTimeInMillis() + offset);
                }
            }

            cal.setTime(startDate);
            Calendar cal2 = (Calendar) cal.clone();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            index = allowedDaysOfWeek.indexOf(dayOfWeek) + 1;
            while(deletedInstances.contains(cal.getTime()))
            {
                if(index >= allowedDaysOfWeek.size())
                {
                    index = 0;
                    cal.add(Calendar.WEEK_OF_YEAR, period);
                    cal2.add(Calendar.WEEK_OF_YEAR, period);
                }
                cal.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(index));
                cal2.set(Calendar.DAY_OF_WEEK, allowedDaysOfWeek.get(index));
                index++;

            }
            startDate = cal2.getTime();
            endDate = new Date(startDate.getTime() + duration);
            if(dateOutOfRange(endDate))
                return null;
            if(startDate.before(currentDate))
            {
                executeNow = true;
            }

            return new CalendarItemTimerTask(
                sourceTask.getStatus(), 
                startDate, endDate, sourceTask.getId(), executeNow, this);
        }
        case Month:
        case MonthEnd:
        case HjMonth:
        case HjMonthEnd:
        {
            return nextMonth(startDate, endDate, false);
        }
        case MonthNth:
        case HjMonthNth:
        {
            if(patternSpecific1 == 0x7f && patternSpecific2 == 0x05)
                return nextMonth(startDate, endDate, true);
            else
                return nextMonthN(startDate, endDate);
        }
        }
        return null;
    }

    /**
     * Finds the occurrence of the events in the next months
     * @param cal the calendar object
     * @param lastDay if <tt>true</tt> it will return the last day of the month
     * @param period the number of months to add
     * @return the calendar object with set date
     */
    private Calendar incrementMonths(Calendar cal, boolean lastDay, 
        int period)
    {
        int dayOfMonth = patternSpecific1;
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, period);
        if(lastDay 
            || (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < dayOfMonth))
            dayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return cal;
    }

    /**
     * Finds the next occurrence for monthly recurrence.
     * @param startDate the start date of the previous calendar item.
     * @param endDate the end date of the previous calendar item.
     * @param lastDay if <tt>true</tt> we are interested in last day of the 
     * month
     * @return the next item
     */
    public CalendarItemTimerTask nextMonth(Date startDate, Date endDate, 
        boolean lastDay)
    {
        long duration = sourceTask.getEndDate().getTime() 
            - sourceTask.getStartDate().getTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal = incrementMonths(cal, lastDay, period);
        Date currentDate = new Date();
        if(cal.getTimeInMillis() + duration < currentDate.getTime())
        {
            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(currentDate);
            int years 
                = cal2.get(Calendar.YEAR) - cal.get(Calendar.YEAR);
            int months = (years * 12) 
                + (cal2.get(Calendar.MONTH) - cal.get(Calendar.MONTH));
            int monthsToAdd = months;
            monthsToAdd -= months % period;
            cal = incrementMonths(cal, lastDay, monthsToAdd);
            if(cal.getTimeInMillis() + duration < currentDate.getTime())
            {
                cal = incrementMonths(cal, lastDay, period);
            }
        }

        Calendar cal2 = (Calendar) cal.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        while(deletedInstances.contains(cal.getTime()))
        {
            cal = incrementMonths(cal, lastDay, period);
            cal2 = incrementMonths(cal2, lastDay, period);
        }

        startDate = cal2.getTime();
        endDate = new Date(startDate.getTime() + duration);
        if(dateOutOfRange(endDate))
        {
            return null;
        }

        boolean executeNow = startDate.before(currentDate);

        return new CalendarItemTimerTask(
            sourceTask.getStatus(), 
            startDate, endDate, sourceTask.getId(), executeNow, this);
    }

    /**
     * Finds the occurrence of the events in the next months
     * @param startDate the start date if the calendar item
     * @param dayOfWeekInMonth the number of week days occurrences
     * @return the date of the next occurrence
     */
    private Date getMonthNStartDate(Date startDate, int dayOfWeekInMonth)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);

        if(dayOfWeekInMonth == -1)
        {
            Date result = null;
            cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
            for(int day : allowedDaysOfWeek)
            {
                cal.set(Calendar.DAY_OF_WEEK, day);
                if(result == null || result.before(cal.getTime()))
                    result = cal.getTime();
            }
            return result;
        }
        else
            while(dayOfWeekInMonth > 0)
            {
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if(allowedDaysOfWeek.contains(dayOfWeek))
                    dayOfWeekInMonth--;
                if(dayOfWeekInMonth > 0)
                    cal.add(Calendar.DAY_OF_MONTH, 1);

            }
        return cal.getTime();
    }

    /**
     * Finds the next occurrence for monthly Nth recurrence.
     * @param startDate the start date of the previous calendar item.
     * @param endDate the end date of the previous calendar item.
     * @return the next item
     */
    public CalendarItemTimerTask nextMonthN(Date startDate, Date endDate)
    {
        int dayOfWeekInMonth = (patternSpecific2 == 5? -1 : patternSpecific2);
        long duration = sourceTask.getEndDate().getTime() 
            - sourceTask.getStartDate().getTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, period);
        cal.setTime(getMonthNStartDate(cal.getTime(), dayOfWeekInMonth));
        Date currentDate = new Date();
        if(cal.getTimeInMillis() + duration < currentDate.getTime())
        {
            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(currentDate);
            int years 
                = cal2.get(Calendar.YEAR) - cal.get(Calendar.YEAR);
            int months = (years * 12) 
                + (cal2.get(Calendar.MONTH) - cal.get(Calendar.MONTH));
            int monthsToAdd = months;
            monthsToAdd -= months % period;
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, monthsToAdd);
            cal.setTime(getMonthNStartDate(cal.getTime(), dayOfWeekInMonth));
            if(cal.getTimeInMillis() + duration < currentDate.getTime())
            {
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.add(Calendar.MONTH, monthsToAdd);
                cal.setTime(getMonthNStartDate(cal.getTime(), dayOfWeekInMonth));
            }
        }

        Calendar cal2 = (Calendar) cal.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        while(deletedInstances.contains(cal.getTime()))
        {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, period);
            startDate = null;
            for(int dayOfWeek : allowedDaysOfWeek)
            {
                cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                cal.set(Calendar.DAY_OF_WEEK_IN_MONTH,dayOfWeekInMonth);
                if((cal.after(startDate) && dayOfWeekInMonth == -1) 
                    || (cal.before(startDate) && dayOfWeekInMonth != -1) 
                    || startDate == null)
                {
                    startDate = cal.getTime();
                    cal2.set(Calendar.YEAR,cal.get(Calendar.YEAR));
                    cal2.set(Calendar.MONTH,cal.get(Calendar.MONTH));
                    cal2.set(Calendar.DATE,cal.get(Calendar.DATE));
                }
            }
        }

        startDate = cal2.getTime();
        endDate = new Date(startDate.getTime() + duration);

        if(dateOutOfRange(endDate))
            return null;

        boolean executeNow = false;
        if(startDate.before(currentDate))
        {
            executeNow  = true;
        }

        return new CalendarItemTimerTask(
            sourceTask.getStatus(), 
            startDate, endDate, sourceTask.getId(), executeNow, this);
    }

    /**
     * Represents the exception info structure.
     */
    public class ExceptionInfo
    {
        /**
         * The start date of the exception.
         */
        private final Date startDate;

        /**
         * The end date of the exception.
         */
        private final Date endDate;

        /**
         * The original start date of the exception.
         */
        private final Date originalStartDate;

        /**
         * The modified flags of the exception.
         */
        private final short overrideFlags;

        /**
         * The new busy status of the exception.
         */
        private CalendarService.BusyStatusEnum busyStatus;

        /**
         * The size of the fixed fields.
         */
        private int size = 22;

        /**
         * Parses the data of the exception.
         * @param offset the position where the exception starts in the binary 
         * data
         */
        public ExceptionInfo(int offset)
        {
            startDate = windowsTimeToDateObject(dataBuffer.getInt(offset));
            offset += 4;

            endDate = windowsTimeToDateObject(dataBuffer.getInt(offset));
            offset += 4;

            originalStartDate 
                = windowsTimeToDateObject(dataBuffer.getInt(offset));
            offset += 4;

            overrideFlags = dataBuffer.getShort(offset);
            offset += 2;
            int[] fieldMasks = {0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 
                0x0020, 0x0040, 0x0080};
            for(int mask : fieldMasks)
            {
                if(mask == 0x0020)
                {
                    if((overrideFlags & mask) != 0)
                    {
                        busyStatus = CalendarService.BusyStatusEnum.getFromLong(
                            (long)dataBuffer.getInt(offset));
                    }

                    if(busyStatus == null)
                    {
                        busyStatus = sourceTask.getStatus();
                    }
                }

                if((overrideFlags & mask) != 0)
                {
                    if(mask == 0x0010 || mask == 0x0001)
                    {
                        short size = dataBuffer.getShort(offset + 2);
                        offset += size;
                        size += size;
                    }
                    offset += 4;
                    size += 4;
                }
            }

            offset += 4;
            int reservedBlockSize = dataBuffer.getShort(offset);
            size += reservedBlockSize;

        }

        /**
         * Returns the size of the exception
         * @return the size of the exception
         */
        public int sizeInBytes()
        {
            return size;
        }

        /**
         * Returns the start date
         * @return the start date
         */
        public Date getStartDate()
        {
            return startDate;
        }

        /**
         * Returns the end date
         * @return the end date
         */
        public Date getEndDate()
        {
            return endDate;
        }

        /**
         * Returns the busy status
         * @return the busy status
         */
        public CalendarService.BusyStatusEnum getBusyStatus()
        {
            return busyStatus;
        }

        /**
         * Prints the properties of the class for debugging purpose.
         */
        @Override
        public String toString()
        {
            String result = "";
            result += "startDate: " 
                +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
                    .format(startDate) + "\n";
            result += "endDate: "
                    +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
                        .format(endDate) + "\n";
            result += "originalStartDate: " 
                    +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
                        .format(originalStartDate) + "\n";
            return result;
        }
    }
}
