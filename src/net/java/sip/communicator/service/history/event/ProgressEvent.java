/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history.event;

import java.util.*;

/**
 * A "ProgressEvent" event gets delivered through the search process
 * of HistoryReader Service.
 * The event is created with arguments - the search conditions
 * (if they do not exist null is passed).
 *
 * We must know the search conditions due the fact that we must
 * differ searches if more than one exist.
 * The real information is the progress of the current search.
 *
 * @author Damian Minkov
 */
public class ProgressEvent
    extends java.util.EventObject
{
    /**
     * The start date in the search condition.
     */
    private Date startDate = null;

    /**
     * The end date in the search condition.
     */
    private Date endDate = null;

    /**
     * The keyword in the search condition.
     */
    private String keyword = null;

    /**
     * The keywords in the search condition.
     */
    private String[] keywords = null;

    /**
     * The number of last messages in the search condition.
     */
    private int last = -1;

    /**
     * The current progress that we will pass.
     */
    private int progress = 0;

    /**
     * Constructs a new <tt>ProgressEvent</tt>.
     *
     * @param source Object The source firing this event
     * @param startDate Date The start date in the search condition.
     * @param endDate Date The end date in the search condition.
     * @param keyword String The keyword in the search condition.
     * @param keywords String[] The keywords in the search condition.
     * @param last int The number of last messages in the search condition.
     * @param progress int The current progress that we will pass.
     */
    public ProgressEvent(Object source,
                         Date startDate, Date endDate,
                         String keyword, String[] keywords,
                         int last,
                         int progress)
    {
        super(source);

        this.startDate = startDate;
        this.endDate = endDate;
        this.keyword = keyword;
        this.keywords = keywords;
        this.last = last;
        this.progress = progress;
    }

    /**
     * Constructs a new <tt>ProgressEvent</tt>.
     *
     * @param source Object The source firing this event
     * @param startDate Date The start date in the search condition.
     * @param endDate Date The end date in the search condition.
     * @param keyword String The keyword in the search condition.
     * @param keywords String[] The keywords in the search condition.
     */
    public ProgressEvent(Object source,
                         Date startDate, Date endDate,
                         String keyword, String[] keywords)
    {
        this(source, startDate, endDate, keyword, keywords, -1, 0);
    }

    /**
     * Constructs a new <tt>ProgressEvent</tt>.
     *
     * @param source Object The source firing this event
     * @param startDate Date The start date in the search condition.
     * @param endDate Date The end date in the search condition.
     */
    public ProgressEvent(Object source, Date startDate, Date endDate)
    {
        this(source, startDate, endDate, null, null, -1, 0);
    }

    /**
     * Gets the current progress that will be fired.
     * @return int the progress value
     */
    public int getProgress()
    {
        return progress;
    }

    /**
     * The end date in the search condition.
     * @return Date end date value
     */
    public Date getEndDate()
    {
        return endDate;
    }

    /**
     * The keyword in the search condition.
     * @return String keyword for searching
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * The keywords in the search condition.
     * @return String[] array of keywords fo searching
     */
    public String[] getKeywords()
    {
        return keywords;
    }

    /**
     * The number of last messages in the search condition.
     * @return int number of last messages
     */
    public int getLast()
    {
        return last;
    }

    /**
     * The start date in the search condition.
     * @return Date start date value
     */
    public Date getStartDate()
    {
        return startDate;
    }

    /**
     * Sets the progress that will be fired
     * @param progress int progress value
     */
    public void setProgress(int progress)
    {
        this.progress = progress;
    }

}
