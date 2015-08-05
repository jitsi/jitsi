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
package net.java.sip.communicator.service.contactsource;

import java.util.*;
import java.util.regex.*;

/**
 * Provides an abstract implementation of a <tt>ContactQuery</tt> which runs in
 * a separate <tt>Thread</tt>.
 *
 * @author Lyubomir Marinov
 * @param <T> the very type of <tt>ContactSourceService</tt> which performs the
 * <tt>ContactQuery</tt>
 */
public abstract class AsyncContactQuery<T extends ContactSourceService>
    extends AbstractContactQuery<T>
{
    /**
     * The {@link #query} in the form of a <tt>String</tt> telephone number if
     * such parsing, formatting and validation is possible; otherwise,
     * <tt>null</tt>.
     */
    private String phoneNumberQuery;

    /**
     * The <tt>Pattern</tt> for which the associated
     * <tt>ContactSourceService</tt> is being queried.
     */
    protected final Pattern query;

    /**
     * The indicator which determines whether there has been an attempt to
     * convert {@link #query} to {@link #phoneNumberQuery}. If the conversion has
     * been successful, <tt>phoneNumberQuery</tt> will be non-<tt>null</tt>.
     */
    private boolean queryIsConvertedToPhoneNumber;

    /**
     * The <tt>SourceContact</tt>s which match {@link #query}.
     */
    private Collection<SourceContact> queryResults
        = new LinkedList<SourceContact>();

    /**
     * The <tt>Thread</tt> in which this <tt>AsyncContactQuery</tt> is
     * performing {@link #query}.
     */
    private Thread thread;

    /**
     * Initializes a new <tt>AsyncContactQuery</tt> instance which is to perform
     * a specific <tt>query</tt> on behalf of a specific <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is to
     * perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>Pattern</tt> for which <tt>contactSource</tt> is
     * being queried
     * @param isSorted indicates if the results of this query should be sorted
     */
    protected AsyncContactQuery(T contactSource,
                                Pattern query,
                                boolean isSorted)
    {
        super(contactSource);

        this.query = query;

        if (isSorted)
            queryResults = new TreeSet<SourceContact>();
    }

    /**
     * Initializes a new <tt>AsyncContactQuery</tt> instance which is to perform
     * a specific <tt>query</tt> on behalf of a specific <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is to
     * perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>Pattern</tt> for which <tt>contactSource</tt> is
     * being queried
     */
    protected AsyncContactQuery(T contactSource, Pattern query)
    {
        super(contactSource);

        this.query = query;
    }

    /**
     * Adds a specific <tt>SourceContact</tt> to the list of
     * <tt>SourceContact</tt>s to be returned by this <tt>ContactQuery</tt> in
     * response to {@link #getQueryResults()}.
     *
     * @param sourceContact the <tt>SourceContact</tt> to be added to the
     * <tt>queryResults</tt> of this <tt>ContactQuery</tt>
     * @param showMoreEnabled indicates whether show more label should be shown 
     * or not.
     * @return <tt>true</tt> if the <tt>queryResults</tt> of this
     * <tt>ContactQuery</tt> has changed in response to the call
     */
    protected boolean addQueryResult(SourceContact sourceContact, 
        boolean showMoreEnabled)
    {
        boolean changed;

        synchronized (queryResults)
        {
            changed = queryResults.add(sourceContact);
        }
        if (changed)
            fireContactReceived(sourceContact, showMoreEnabled);

        return changed;
    }

    /**
     * Adds a specific <tt>SourceContact</tt> to the list of
     * <tt>SourceContact</tt>s to be returned by this <tt>ContactQuery</tt> in
     * response to {@link #getQueryResults()}.
     *
     * @param sourceContact the <tt>SourceContact</tt> to be added to the
     * <tt>queryResults</tt> of this <tt>ContactQuery</tt>
     * @return <tt>true</tt> if the <tt>queryResults</tt> of this
     * <tt>ContactQuery</tt> has changed in response to the call
     */
    protected boolean addQueryResult(SourceContact sourceContact)
    {
        boolean changed;

        synchronized (queryResults)
        {
            changed = queryResults.add(sourceContact);
        }
        if (changed)
            fireContactReceived(sourceContact);

        return changed;
    }

    /**
     * Removes a specific <tt>SourceContact</tt> from the list of
     * <tt>SourceContact</tt>s.
     *
     * @param sourceContact the <tt>SourceContact</tt> to be removed from the
     * <tt>queryResults</tt> of this <tt>ContactQuery</tt>
     * @return <tt>true</tt> if the <tt>queryResults</tt> of this
     * <tt>ContactQuery</tt> has changed in response to the call
     */
    protected boolean removeQueryResult(SourceContact sourceContact)
    {
        boolean changed;

        synchronized (queryResults)
        {
            changed = queryResults.remove(sourceContact);
        }
        if (changed)
            fireContactRemoved(sourceContact);

        return changed;
    }

    /**
     * Adds a set of <tt>SourceContact</tt> instances to the list of
     * <tt>SourceContact</tt>s to be returned by this <tt>ContactQuery</tt> in
     * response to {@link #getQueryResults()}.
     *
     * @param sourceContacts the set of <tt>SourceContact</tt> to be added to
     * the <tt>queryResults</tt> of this <tt>ContactQuery</tt>
     * @return <tt>true</tt> if the <tt>queryResults</tt> of this
     * <tt>ContactQuery</tt> has changed in response to the call
     */
    protected boolean addQueryResults(
        final Set<? extends SourceContact> sourceContacts)
    {
        final boolean changed;

        synchronized (queryResults)
        {
            changed = queryResults.addAll(sourceContacts);
        }

        if (changed)
        {
            // TODO Need something to fire one event for multiple contacts.
            for (SourceContact contact : sourceContacts)
            {
                fireContactReceived(contact, false);
            }
        }

        return changed;
    }


    /**
     * Gets the {@link #query} of this <tt>AsyncContactQuery</tt> as a
     * <tt>String</tt> which represents a phone number (if possible).
     *
     * @return a <tt>String</tt> which represents the <tt>query</tt> of this
     * <tt>AsyncContactQuery</tt> as a phone number if such parsing, formatting
     * and validation is possible; otherwise, <tt>null</tt>
     */
    protected String getPhoneNumberQuery()
    {
        if ((phoneNumberQuery == null) && !queryIsConvertedToPhoneNumber)
        {
            try
            {
                String pattern = query.pattern();

                if (pattern != null)
                {
                    int patternLength = pattern.length();

                    if ((patternLength > 2)
                            && (pattern.charAt(0) == '^')
                            && (pattern.charAt(patternLength - 1) == '$'))
                    {
                        phoneNumberQuery
                            = pattern.substring(1, patternLength - 1);
                    }
                    else if ((patternLength > 4)
                        && (pattern.charAt(0) == '\\')
                        && (pattern.charAt(1) == 'Q')
                        && (pattern.charAt(patternLength - 2) == '\\')
                        && (pattern.charAt(patternLength - 1) == 'E'))
                    {
                        phoneNumberQuery
                            = pattern.substring(2, patternLength - 2);
                    }

                }
            }
            finally
            {
                queryIsConvertedToPhoneNumber = true;
            }
        }
        return phoneNumberQuery;
    }

    /**
     * Gets the number of <tt>SourceContact</tt>s which match this
     * <tt>ContactQuery</tt>.
     *
     * @return the number of <tt>SourceContact</tt> which match this
     * <tt>ContactQuery</tt>
     */
    public int getQueryResultCount()
    {
        synchronized (queryResults)
        {
            return queryResults.size();
        }
    }

    /**
     * Gets the <tt>List</tt> of <tt>SourceContact</tt>s which match this
     * <tt>ContactQuery</tt>.
     *
     * @return the <tt>List</tt> of <tt>SourceContact</tt>s which match this
     * <tt>ContactQuery</tt>
     * @see ContactQuery#getQueryResults()
     */
    public List<SourceContact> getQueryResults()
    {
        List<SourceContact> qr;

        synchronized (queryResults)
        {
            qr = new ArrayList<SourceContact>(queryResults.size());
            qr.addAll(queryResults);
        }
        return qr;
    }

    /**
     * Returns the query string, this query was created for.
     *
     * @return the query string, this query was created for
     */
    public String getQueryString()
    {
        return query.toString();
    }

    /**
     * Performs this <tt>ContactQuery</tt> in a background <tt>Thread</tt>.
     */
    protected abstract void run();

    /**
     * Starts this <tt>AsyncContactQuery</tt>.
     */
    public synchronized void start()
    {
        if (thread == null)
        {
            thread
                = new Thread()
                {
                    @Override
                    public void run()
                    {
                        boolean completed = false;

                        try
                        {
                            AsyncContactQuery.this.run();
                            completed = true;
                        }
                        finally
                        {
                            synchronized (AsyncContactQuery.this)
                            {
                                if (thread == Thread.currentThread())
                                    stopped(completed);
                            }
                        }
                    }
                };
            thread.setDaemon(true);
            thread.start();
        }
        else
            throw new IllegalStateException("thread");
    }

    /**
     * Notifies this <tt>AsyncContactQuery</tt> that it has stopped performing
     * in the associated background <tt>Thread</tt>.
     *
     * @param completed <tt>true</tt> if this <tt>ContactQuery</tt> has
     * successfully completed, <tt>false</tt> if an error has been encountered
     * during its execution
     */
    protected void stopped(boolean completed)
    {
        if (getStatus() == QUERY_IN_PROGRESS)
            setStatus(completed ? QUERY_COMPLETED : QUERY_ERROR);
    }

    /**
     * Determines whether a specific <tt>String</tt> phone number matches the
     * {@link #query} of this <tt>AsyncContactQuery</tt>.
     *
     * @param phoneNumber the <tt>String</tt> which represents the phone number
     * to match to the <tt>query</tt> of this <tt>AsyncContactQuery</tt>
     * @return <tt>true</tt> if the specified <tt>phoneNumber</tt> matches the
     * <tt>query</tt> of this <tt>AsyncContactQuery</tt>; otherwise,
     * <tt>false</tt>
     */
    protected boolean phoneNumberMatches(String phoneNumber)
    {
        /*
         * PhoneNumberI18nService implements functionality to aid the parsing,
         * formatting and validation of international phone numbers so attempt
         * to use it to determine whether the specified phoneNumber matches the
         * query. For example, check whether the normalized phoneNumber matches
         * the query.
         */

        boolean phoneNumberMatches = false;

        if (query
                .matcher(ContactSourceActivator.getPhoneNumberI18nService()
                    .normalize(phoneNumber)).find())
        {
            phoneNumberMatches = true;
        }
        else
        {
            /*
             * The fact that the normalized form of the phoneNumber doesn't
             * match the query doesn't mean that, for example, it doesn't
             * match the normalized form of the query. The latter, though,
             * requires the query to look like a phone number as well. In
             * order to not accidentally start matching all queries to phone
             * numbers, it seems justified to normalize the query only when
             * it is a phone number, not whenever it looks like a piece of a
             * phone number.
             */

            String phoneNumberQuery = getPhoneNumberQuery();

            if ((phoneNumberQuery != null)
                    && (phoneNumberQuery.length() != 0))
            {
                try
                {
                    phoneNumberMatches
                        = ContactSourceActivator.getPhoneNumberI18nService()
                            .phoneNumbersMatch(
                                phoneNumberQuery,
                                phoneNumber);
                }
                catch (IllegalArgumentException iaex)
                {
                    /*
                     * Ignore it, phoneNumberMatches will remain equal to
                     * false.
                     */
                }
            }
        }
        return phoneNumberMatches;
    }
}
