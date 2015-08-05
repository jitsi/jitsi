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
package net.java.sip.communicator.util;

import java.util.*;

/**
 * The <tt>StringUtils</tt> class is used through this ui implementation for
 * some special operations with strings.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class GuiUtils
{
    private static final Calendar c1 = Calendar.getInstance();

    private static final Calendar c2 = Calendar.getInstance();

    /**
     * Number of milliseconds in a second.
     */
    public static final long MILLIS_PER_SECOND = 1000;

    /**
     * Number of milliseconds in a standard minute.
     */
    public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;

    /**
     * Number of milliseconds in a standard hour.
     */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    /**
     * Number of milliseconds in a standard day.
     */
    public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    // These mappings map a character (key) to a specific digit that should
    // replace it for normalization purposes. Non-European digits that may be
    // used in phone numbers are mapped to a European equivalent.
    private static final Map<Character, Character> DIGIT_MAPPINGS;

    /**
     *  Characters and their replacement in created folder names
     */
    private final static String[][] ESCAPE_SEQUENCES = new String[][]
    {
        {"&", "&_amp"},
        {"/", "&_sl"},
        {"\\\\", "&_bs"},   // the char \
        {":", "&_co"},
        {"\\*", "&_as"},    // the char *
        {"\\?", "&_qm"},    // the char ?
        {"\"", "&_pa"},     // the char "
        {"<", "&_lt"},
        {">", "&_gt"},
        {"\\|", "&_pp"}     // the char |
    };

    static
    {
        HashMap<Character, Character> digitMap
            = new HashMap<Character, Character>(50);

        digitMap.put('0', '0');
        digitMap.put('\uFF10', '0');  // Fullwidth digit 0
        digitMap.put('\u0660', '0');  // Arabic-indic digit 0
        digitMap.put('1', '1');
        digitMap.put('\uFF11', '1');  // Fullwidth digit 1
        digitMap.put('\u0661', '1');  // Arabic-indic digit 1
        digitMap.put('2', '2');
        digitMap.put('\uFF12', '2');  // Fullwidth digit 2
        digitMap.put('\u0662', '2');  // Arabic-indic digit 2
        digitMap.put('3', '3');
        digitMap.put('\uFF13', '3');  // Fullwidth digit 3
        digitMap.put('\u0663', '3');  // Arabic-indic digit 3
        digitMap.put('4', '4');
        digitMap.put('\uFF14', '4');  // Fullwidth digit 4
        digitMap.put('\u0664', '4');  // Arabic-indic digit 4
        digitMap.put('5', '5');
        digitMap.put('\uFF15', '5');  // Fullwidth digit 5
        digitMap.put('\u0665', '5');  // Arabic-indic digit 5
        digitMap.put('6', '6');
        digitMap.put('\uFF16', '6');  // Fullwidth digit 6
        digitMap.put('\u0666', '6');  // Arabic-indic digit 6
        digitMap.put('7', '7');
        digitMap.put('\uFF17', '7');  // Fullwidth digit 7
        digitMap.put('\u0667', '7');  // Arabic-indic digit 7
        digitMap.put('8', '8');
        digitMap.put('\uFF18', '8');  // Fullwidth digit 8
        digitMap.put('\u0668', '8');  // Arabic-indic digit 8
        digitMap.put('9', '9');
        digitMap.put('\uFF19', '9');  // Fullwidth digit 9
        digitMap.put('\u0669', '9');  // Arabic-indic digit 9
        DIGIT_MAPPINGS = Collections.unmodifiableMap(digitMap);
    }

    /**
     * Replaces some chars that are special in a regular expression.
     * @param text The initial text.
     * @return the formatted text
     */
    public static String replaceSpecialRegExpChars(String text)
    {
        return text.replaceAll("([.()^&$*|])", "\\\\$1");
    }

    /**
     * Counts occurrences of the <tt>needle</tt> character in the given
     * <tt>text</tt>.
     * @param text the text in which we search
     * @param needle the character we're looking for
     * @return the count of occurrences of the <tt>needle</tt> chat in the
     * given <tt>text</tt>
     */
    public static int countOccurrences(String text, char needle)
    {
        int count = 0;

        for (char c : text.toCharArray())
        {
            if (c == needle)
               ++count;
        }
        return count;
    }

    /**
     * Compares the two dates. The comparison is based only on the day, month
     * and year values. Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is after
     * the second one.
     * @param date1 the first date to compare
     * @param date2 the second date to compare with
     * @return Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is after
     * the second one
     */
    public static int compareDates(Date date1, Date date2)
    {
        return date1.compareTo(date2);
    }

    /**
     * Compares the two dates. The comparison is based only on the day, month
     * and year values. Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is after
     * the second one.
     * @param date1 the first date to compare
     * @param date2 the second date to compare with
     * @return Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is after
     * the second one
     */
    public static int compareDates(long date1, long date2)
    {
        return (date1 < date2 ? -1 : (date1 == date2 ? 0 : 1));
    }

    /**
     * Compares the two dates. The comparison is based only on the day, month
     * and year values. Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is
     * after the second one.
     * @param date1 the first date to compare
     * @param date2 the second date to compare with
     * @return Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is
     * after the second one
     */
    public static int compareDatesOnly(long date1, long date2)
    {
        c1.setTimeInMillis(date1);
        c2.setTimeInMillis(date2);

        int day1 = c1.get(Calendar.DAY_OF_MONTH);
        int month1 = c1.get(Calendar.MONTH);
        int year1 = c1.get(Calendar.YEAR);

        int day2 = c2.get(Calendar.DAY_OF_MONTH);
        int month2 = c2.get(Calendar.MONTH);
        int year2 = c2.get(Calendar.YEAR);

        if (year1 < year2)
        {
            return -1;
        }
        else if (year1 == year2)
        {
            if (month1 < month2)
                return -1;
            else if (month1 == month2)
            {
                if (day1 < day2)
                    return -1;
                else if (day1 == day2)
                    return 0;
                else
                    return 1;
            }
            else
                return 1;
        }
        else
        {
            return 1;
        }
    }

    /**
     * Compares the two dates. The comparison is based only on the day, month
     * and year values. Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is
     * after the second one.
     * @param date1 the first date to compare
     * @param date2 the second date to compare with
     * @return Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is
     * after the second one
     */
    public static int compareDatesOnly(Date date1, Date date2)
    {
        return compareDatesOnly(date1.getTime(), date2.getTime());
    }

    /**
     * Formats the given date. The result format is the following:
     * [Month] [Day], [Year]. For example: Dec 24, 2000.
     * @param date the date to format
     * @return the formatted date string
     */
    public static String formatDate(Date date)
    {
        return formatDate(date.getTime());
    }

    /**
     * Formats the given date. The result format is the following:
     * [Month] [Day], [Year]. For example: Dec 24, 2000.
     * @param date the date to format
     * @return the formatted date string
     */
    public static String formatDate(final long date)
    {
        StringBuffer strBuf = new StringBuffer();

        formatDate(date, strBuf);
        return strBuf.toString();
    }

    /**
     * Formats the given date as: Month DD, YYYY and appends it to the given
     * <tt>dateStrBuf</tt> string buffer.
     * @param date the date to format
     * @param dateStrBuf the <tt>StringBuffer</tt>, where to append the
     * formatted date
     */
    public static void formatDate(long date, StringBuffer dateStrBuf)
    {
        c1.setTimeInMillis(date);

        dateStrBuf.append(GuiUtils.processMonth(c1.get(Calendar.MONTH)));
        dateStrBuf.append(' ');
        GuiUtils.formatTime(c1.get(Calendar.DAY_OF_MONTH), dateStrBuf);
        dateStrBuf.append(", ");
        GuiUtils.formatTime(c1.get(Calendar.YEAR), dateStrBuf);
    }

    /**
     * Formats the given date as: Month DD, YYYY and appends it to the given
     * <tt>dateStrBuf</tt> string buffer.
     * @param date the date to format
     * @param dateStrBuf the <tt>StringBuffer</tt>, where to append the
     * formatted date
     */
    public static void formatDate(Date date, StringBuffer dateStrBuf)
    {
        c1.setTime(date);

        dateStrBuf.append(GuiUtils.processMonth(c1.get(Calendar.MONTH)));
        dateStrBuf.append(' ');
        GuiUtils.formatTime(c1.get(Calendar.DAY_OF_MONTH), dateStrBuf);
        dateStrBuf.append(", ");
        GuiUtils.formatTime(c1.get(Calendar.YEAR), dateStrBuf);
    }

    /**
     * Formats the time for the given date. The result format is the following:
     * [Hour]:[Minute]:[Second]. For example: 12:25:30.
     * @param date the date to format
     * @return the formatted hour string
     */
    public static String formatTime(Date date)
    {
        return formatTime(date.getTime());
    }

    /**
     * Formats the time for the given date. The result format is the following:
     * [Hour]:[Minute]:[Second]. For example: 12:25:30.
     * @param time the date to format
     * @return the formatted hour string
     */
    public static String formatTime(long time)
    {
        c1.setTimeInMillis(time);

        StringBuffer timeStrBuf = new StringBuffer();

        GuiUtils.formatTime(c1.get(Calendar.HOUR_OF_DAY), timeStrBuf);
        timeStrBuf.append(':');
        GuiUtils.formatTime(c1.get(Calendar.MINUTE), timeStrBuf);
        timeStrBuf.append(':');
        GuiUtils.formatTime(c1.get(Calendar.SECOND), timeStrBuf);
        return timeStrBuf.toString();
    }

    /**
     * Formats the time period duration for the given start date and end date.
     * The result format is the following:
     * [Hour]:[Minute]:[Second]. For example: 12:25:30.
     * @param startDate the start date
     * @param endDate the end date
     * @return the formatted hour string
     */
    public static String formatTime(Date startDate, Date endDate)
    {
        return formatTime(startDate.getTime(), endDate.getTime());
    }

    /**
     * Formats the time period duration for the given start date and end date.
     * The result format is the following:
     * [Hour]:[Minute]:[Second]. For example: 12:25:30.
     * @param start the start date in milliseconds
     * @param end the end date in milliseconds
     * @return the formatted hour string
     */
    public static String formatTime(long start, long end)
    {
        long duration = end - start;

        long milPerSec = 1000;
        long milPerMin = milPerSec*60;
        long milPerHour = milPerMin*60;

        long hours = duration / milPerHour;
        long minutes
            = ( duration - hours*milPerHour ) / milPerMin;
        long seconds
            = ( duration - hours*milPerHour - minutes*milPerMin)
                    / milPerSec;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Gets the display/human-readable string representation of the month with
     * the specified zero-based month number.
     *
     * @param month the zero-based month number
     * @return the corresponding month abbreviation
     */
    private static String processMonth(int month)
    {
        String monthStringKey;

        switch (month)
        {
        case 0: monthStringKey = "service.gui.JANUARY"; break;
        case 1: monthStringKey = "service.gui.FEBRUARY"; break;
        case 2: monthStringKey = "service.gui.MARCH"; break;
        case 3: monthStringKey = "service.gui.APRIL"; break;
        case 4: monthStringKey = "service.gui.MAY"; break;
        case 5: monthStringKey = "service.gui.JUNE"; break;
        case 6: monthStringKey = "service.gui.JULY"; break;
        case 7: monthStringKey = "service.gui.AUGUST"; break;
        case 8: monthStringKey = "service.gui.SEPTEMBER"; break;
        case 9: monthStringKey = "service.gui.OCTOBER"; break;
        case 10: monthStringKey = "service.gui.NOVEMBER"; break;
        case 11: monthStringKey = "service.gui.DECEMBER"; break;
        default: return "";
        }

        return UtilActivator.getResources().getI18NString(monthStringKey);
    }

    /**
     * Adds a 0 in the beginning of one digit numbers.
     *
     * @param time The time parameter could be hours, minutes or seconds.
     * @param timeStrBuf the <tt>StringBuffer</tt> to which the formatted
     * minutes string is to be appended
     */
    private static void formatTime(int time, StringBuffer timeStrBuf)
    {
        String timeString = Integer.toString(time);

        if (timeString.length() < 2)
            timeStrBuf.append('0');
        timeStrBuf.append(timeString);
    }

    /**
     * Formats the given long to X hour, Y min, Z sec.
     * @param millis the time in milliseconds to format
     * @return the formatted seconds
     */
    public static String formatSeconds(long millis)
    {
        long[] values = new long[4];
        values[0] = millis / MILLIS_PER_DAY;
        values[1] = (millis / MILLIS_PER_HOUR) % 24;
        values[2] = (millis / MILLIS_PER_MINUTE) % 60;
        values[3] = (millis / MILLIS_PER_SECOND) % 60;

        String[] fields = { " d ", " h ", " min ", " sec" };

        StringBuffer buf = new StringBuffer(64);
        boolean valueOutput = false;

        for (int i = 0; i < 4; i++)
        {
            long value = values[i];

            if (value == 0)
            {
                if (valueOutput)
                    buf.append('0').append(fields[i]);
            }
            else
            {
                valueOutput = true;
                buf.append(value).append(fields[i]);
            }
        }

        return buf.toString().trim();
    }

    /**
     * Replaces the characters that we must escape used for the created
     * filename.
     *
     * @param string the <tt>String</tt> which is to have its characters escaped
     * @return a <tt>String</tt> derived from the specified <tt>id</tt> by
     * escaping characters
     */
    public static String escapeFileNameSpecialCharacters(String string)
    {
        String resultId = string;

        for (int j = 0; j < ESCAPE_SEQUENCES.length; j++)
        {
            resultId = resultId.
                replaceAll(ESCAPE_SEQUENCES[j][0], ESCAPE_SEQUENCES[j][1]);
        }
        return resultId;
    }

    /**
     * Escapes special HTML characters such as &lt;, &gt;, &amp; and &quot; in
     * the specified message.
     *
     * @param message the message to be processed
     * @return the processed message with escaped special HTML characters
     */
    public static String escapeHTMLChars(String message)
    {
        return message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }
}
