/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.filehistory;

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
}
