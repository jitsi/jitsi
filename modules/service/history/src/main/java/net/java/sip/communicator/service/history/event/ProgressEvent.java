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
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The start date in the search condition.
     */
    private Date startDate = null;

    /**
     * The end date in the search condition.
     */
    private Date endDate = null;

    /**
     * The keywords in the search condition.
     */
    private String[] keywords = null;

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
     * @param keywords String[] The keywords in the search condition.
     * @param progress int The current progress that we will pass.
     */
    public ProgressEvent(Object source,
                         Date startDate, Date endDate,
                         String[] keywords,
                         int progress)
    {
        super(source);

        this.startDate = startDate;
        this.endDate = endDate;
        this.keywords = keywords;
        this.progress = progress;
    }

    /**
     * Constructs a new <tt>ProgressEvent</tt>.
     *
     * @param source Object The source firing this event
     * @param startDate Date The start date in the search condition.
     * @param endDate Date The end date in the search condition.
     * @param keywords String[] The keywords in the search condition.
     */
    public ProgressEvent(Object source,
                         Date startDate, Date endDate,
                         String[] keywords)
    {
        this(source, startDate, endDate, keywords, 0);
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
        this(source, startDate, endDate, null, 0);
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
     * The keywords in the search condition.
     * @return String[] array of keywords fo searching
     */
    public String[] getKeywords()
    {
        return keywords;
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
