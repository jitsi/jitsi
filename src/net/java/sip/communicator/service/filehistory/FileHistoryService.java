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
package net.java.sip.communicator.service.filehistory;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * File History Service stores info for file transfers from various protocols.
 *
 * @author Damian Minkov
 */
public interface FileHistoryService
{
    /**
     * Returns all the file transfers made after the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param startDate Date the start date of the transfers
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByStartDate(
            MetaContact contact, Date startDate)
        throws RuntimeException;

    /**
     * Returns all the file transfers made before the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param endDate Date the end date of the transfers
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByEndDate(
            MetaContact contact, Date endDate)
        throws RuntimeException;

    /**
     * Returns all the file transfers made between the given dates
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param startDate Date the start date of the transfers
     * @param endDate Date the end date of the transfers
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByPeriod(
            MetaContact contact, Date startDate, Date endDate)
        throws RuntimeException;

    /**
     * Returns all the file transfers made between the given dates and
     * having the given keywords in the filename
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param startDate Date the start date of the transfers
     * @param endDate Date the end date of the transfers
     * @param keywords array of keywords
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByPeriod(MetaContact contact,
        Date startDate, Date endDate, String[] keywords)
            throws RuntimeException;

    /**
     * Returns all the file transfers made between the given dates
     * and having the given keywords in the filename
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param startDate Date the start date of the transfers
     * @param endDate Date the end date of the transfers
     * @param keywords array of keywords
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByPeriod(
            MetaContact contact, Date startDate, Date endDate,
            String[] keywords, boolean caseSensitive)
        throws RuntimeException;


    /**
     * Returns the supplied number of file transfers
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param count filetransfer count
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findLast(MetaContact contact, int count)
        throws RuntimeException;

    /**
     * Returns all the file transfers having the given keyword in the filename
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param keyword keyword
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByKeyword(
            MetaContact contact, String keyword)
        throws RuntimeException;

    /**
     * Returns all the file transfers having the given keyword in the filename
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param keyword keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByKeyword(
            MetaContact contact, String keyword, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns all the file transfers having the given keywords in the filename
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param keywords keyword
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByKeywords(
            MetaContact contact, String[] keywords)
        throws RuntimeException;

    /**
     * Returns all the file transfershaving the given keywords in the filename
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param keywords keyword
     * @param caseSensitive is keywords search case sensitive
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByKeywords(
            MetaContact contact, String[] keywords, boolean caseSensitive)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent file transfers after the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param date transfers after date
     * @param count transfers count
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findFirstRecordsAfter(
            MetaContact contact, Date date, int count)
        throws RuntimeException;

    /**
     * Returns the supplied number of recent file transfers before the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param date transfers before date
     * @param count transfers count
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findLastRecordsBefore(
            MetaContact contact, Date date, int count)
        throws RuntimeException;

    /**
     * Permanently removes all locally stored file history.
     *
     * @throws java.io.IOException
     *         Thrown if the history could not be removed due to a IO error.
     */
    public void eraseLocallyStoredHistory()
        throws
        IOException;

    /**
     * Permanently removes locally stored file history for the metacontact.
     *
     * @throws java.io.IOException
     *         Thrown if the history could not be removed due to a IO error.
     */
    public void eraseLocallyStoredHistory(MetaContact contact)
        throws IOException;

}
