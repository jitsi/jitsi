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
package net.java.sip.communicator.service.callhistory;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.callhistory.event.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * The Call History Service stores info about calls made from various protocols
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 * @author Hristo Terezov
 */
public interface CallHistoryService
{
    /**
     * Returns all the calls made by all the contacts
     * in the supplied <tt>contact</tt> after the given date.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param startDate Date the start date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public Collection<CallRecord> findByStartDate(  MetaContact contact,
                                                    Date startDate)
        throws RuntimeException;

    /**
     * Returns all the calls made by all the contacts
     * in the supplied <tt>contact</tt> before the given date.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public Collection<CallRecord> findByEndDate(MetaContact contact,
                                                Date endDate)
        throws RuntimeException;

    /**
     * Returns all the calls made by all the contacts
     * in the supplied <tt>contact</tt> between the given dates.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public Collection<CallRecord> findByPeriod( MetaContact contact,
                                                Date startDate,
                                                Date endDate)
        throws RuntimeException;


    /**
     * Returns all the calls made after the given date.
     *
     * @param startDate Date the start date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public Collection<CallRecord> findByStartDate(Date startDate)
        throws RuntimeException;

    /**
     * Returns all the calls made before the given date.
     *
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public Collection<CallRecord> findByEndDate(Date endDate)
        throws RuntimeException;

    /**
     * Returns all the calls made between the given dates.
     *
     * @param startDate Date the start date of the calls
     * @param endDate Date the end date of the calls
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public Collection<CallRecord> findByPeriod(Date startDate, Date endDate)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent calls made by all the contacts
     * in the supplied <tt>contact</tt>.
     *
     * @param contact MetaContact which contacts participate in
     *      the returned calls
     * @param count calls count
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public Collection<CallRecord> findLast(MetaContact contact, int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent calls.
     *
     * @param count calls count
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public Collection<CallRecord> findLast(int count)
        throws RuntimeException;

    /**
     * Find the calls made by the supplied peer address
     * @param address String the address of the peer
     * @param recordCount the number of records to return
     * @return Collection of CallRecords with CallPeerRecord
     * @throws RuntimeException if something goes wrong
     */
    public CallHistoryQuery findByPeer(String address, int recordCount)
        throws RuntimeException;

    /**
     * Adding progress listener for monitoring progress of search process
     *
     * @param listener HistorySearchProgressListener
     */
    public void addSearchProgressListener(
        CallHistorySearchProgressListener listener);

   /**
    * Removing progress listener
    *
    * @param listener HistorySearchProgressListener
    */
    public void removeSearchProgressListener(
        CallHistorySearchProgressListener listener);

    /**
     * Updates the secondary address field of call record.
     * @param date the start date of the record which will be updated.
     * @param peer the peer of the record which will be updated.
     * @param address the value of the secondary address .
     */
    public void updateCallRecordPeerSecondaryAddress(final Date date,
        final String peerAddress,
        final String address);

    /**
     * Adding <tt>CallHistoryRecordListener</tt> listener to the list.
     *
     * @param listener CallHistoryRecordListener
     */
    public void addCallHistoryRecordListener(
        CallHistoryPeerRecordListener listener);

    /**
     * Removing <tt>CallHistoryRecordListener</tt> listener
     *
     * @param listener CallHistoryRecordListener
     */
    public void removeCallHistoryRecordListener(
        CallHistoryPeerRecordListener listener);

    /**
     * Permanently removes all locally stored call history.
     *
     * @throws java.io.IOException Thrown if the history could not be removed
     *             due to a IO error.
     */
    public void eraseLocallyStoredHistory() throws IOException;
}
