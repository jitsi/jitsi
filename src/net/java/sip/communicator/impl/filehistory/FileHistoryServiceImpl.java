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
package net.java.sip.communicator.impl.filehistory;

import static net.java.sip.communicator.service.history.HistoryService.*;

import java.io.*;
import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * File History Service stores info for file transfers from various protocols.
 * Uses History Service.
 *
 * @author Damian Minkov
 */
public class FileHistoryServiceImpl
    implements  FileHistoryService,
                ServiceListener,
                FileTransferStatusListener,
                FileTransferListener
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(FileHistoryServiceImpl.class);

    private static String[] STRUCTURE_NAMES =
        new String[] { "file", "dir", "date", "status", "id"};

    private static final String FILE_TRANSFER_ACTIVE = "active";

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(STRUCTURE_NAMES);

     // the field used to search by keywords
    private static final String SEARCH_FIELD = "file";

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * The <tt>HistoryService</tt> reference.
     */
    private HistoryService historyService = null;

    /**
     * Starts the service. Check the current registered protocol providers
     * which supports FileTransfer and adds a listener to them.
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        if (logger.isDebugEnabled())
            logger.debug("Starting the file history implementation.");
        this.bundleContext = bc;

        // start listening for newly register or removed protocol providers
        bc.addServiceListener(this);

        Collection<ServiceReference<ProtocolProviderService>> ppsRefs
            = ServiceUtils.getServiceReferences(
                    bc,
                    ProtocolProviderService.class);

        // in case we found any
        if (!ppsRefs.isEmpty())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Found " + ppsRefs.size()
                            + " already installed providers.");
            }
            for (ServiceReference<ProtocolProviderService> ppsRef : ppsRefs)
            {
                ProtocolProviderService pps = bc.getService(ppsRef);

                handleProviderAdded(pps);
            }
        }
    }

    /**
     * Stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        bc.removeServiceListener(this);

        Collection<ServiceReference<ProtocolProviderService>> ppsRefs
            = ServiceUtils.getServiceReferences(
                    bc,
                    ProtocolProviderService.class);

        // in case we found any
        if (!ppsRefs.isEmpty())
        {
            for (ServiceReference<ProtocolProviderService> ppsRef : ppsRefs)
            {
                ProtocolProviderService pps = bc.getService(ppsRef);

                handleProviderRemoved(pps);
            }
        }
    }

    /**
     * Used to attach the File History Service to existing or
     * just registered protocol provider. Checks if the provider has implementation
     * of OperationSetFileTransfer
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        if (logger.isDebugEnabled())
            logger.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a file transfer operation set
        OperationSetFileTransfer opSetFileTransfer
            = provider.getOperationSet(OperationSetFileTransfer.class);

        if (opSetFileTransfer != null)
        {
            opSetFileTransfer.addFileTransferListener(this);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a file transfer op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetFileTransfer opSetFileTransfer
            = provider.getOperationSet(OperationSetFileTransfer.class);

        if (opSetFileTransfer != null)
        {
            opSetFileTransfer.addFileTransferListener(this);
        }
    }

    /**
     * Set the history service.
     *
     * @param historyService HistoryService
     */
    public void setHistoryService(HistoryService historyService)
    {
        this.historyService = historyService;
    }

    /**
     * Gets all the history readers for the contacts in the given MetaContact
     * @param contact MetaContact
     * @return Hashtable
     */
    private Map<Contact, HistoryReader> getHistoryReaders(MetaContact contact)
    {
        Map<Contact, HistoryReader> readers
            = new Hashtable<Contact, HistoryReader>();
        Iterator<Contact> iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();

            try
            {
                History history = this.getHistory(null, item);
                readers.put(item, history.getReader());
            }
            catch (IOException e)
            {
                logger.error("Could not read history", e);
            }
        }
        return readers;
    }

    private FileRecord createFileRecordFromHistoryRecord(
        HistoryRecord hr, Contact contact)
    {
        String file = null;
        String dir = null;
        Date date = new Date(0);
        String status = null;
        String id = null;

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        for (int i = 0; i < hr.getPropertyNames().length; i++)
        {
            String propName = hr.getPropertyNames()[i];

            if (propName.equals(STRUCTURE_NAMES[0]))
                file = hr.getPropertyValues()[i];
            else if (propName.equals(STRUCTURE_NAMES[1]))
                dir = hr.getPropertyValues()[i];
            else if (propName.equals(STRUCTURE_NAMES[2]))
            {
                try
                {
                    date = sdf.parse(hr.getPropertyValues()[i]);
                }
                catch (ParseException e)
                {
                    date = new Date(Long.valueOf(hr.getPropertyValues()[i]));
                }
            }
            else if (propName.equals(STRUCTURE_NAMES[3]))
                status = hr.getPropertyValues()[i];
            else if (propName.equals(STRUCTURE_NAMES[4]))
                id = hr.getPropertyValues()[i];
        }

        return new FileRecord(id, contact, dir, date, new File(file), status);
    }

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
        throws RuntimeException
    {
        TreeSet<FileRecord> result =
            new TreeSet<FileRecord>(new FileRecordComparator());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact c = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            Iterator<HistoryRecord> recs = reader.findByStartDate(startDate);
            while (recs.hasNext())
            {
                result.add(createFileRecordFromHistoryRecord(recs.next(), c));
            }
        }

        return result;
    }

    /**
     * Returns all the file transfers made before the given date
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param endDate Date the end date of the transfers
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findByEndDate(MetaContact contact, Date endDate)
        throws RuntimeException
    {
        TreeSet<FileRecord> result =
            new TreeSet<FileRecord>(new FileRecordComparator());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact c = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            Iterator<HistoryRecord> recs = reader.findByEndDate(endDate);
            while (recs.hasNext())
            {
                result.add(createFileRecordFromHistoryRecord(recs.next(), c));
            }
        }

        return result;
    }

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
            throws RuntimeException
    {
        return findByPeriod(contact, startDate, endDate, keywords, false);
    }

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
    public Collection<FileRecord> findByPeriod(MetaContact contact, Date startDate, Date endDate,
                            String[] keywords, boolean caseSensitive)
        throws RuntimeException
    {
        TreeSet<FileRecord> result =
            new TreeSet<FileRecord>(new FileRecordComparator());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact c = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            Iterator<HistoryRecord> recs = reader.findByPeriod(
                startDate, endDate, keywords, SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(createFileRecordFromHistoryRecord(recs.next(), c));
            }
        }

        return result;
    }

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
        throws RuntimeException
    {
        TreeSet<FileRecord> result =
            new TreeSet<FileRecord>(new FileRecordComparator());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact c = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            Iterator<HistoryRecord> recs = reader.findByPeriod(startDate, endDate);
            while (recs.hasNext())
            {
                result.add(createFileRecordFromHistoryRecord(recs.next(), c));
            }
        }

        return result;
    }

    /**
     * Returns the supplied number of file transfers
     *
     * @param contact MetaContact the receiver or sender of the file
     * @param count filetransfer count
     * @return Collection of FileRecords
     * @throws RuntimeException
     */
    public Collection<FileRecord> findLast(MetaContact contact, int count)
        throws RuntimeException
    {
        TreeSet<FileRecord> result =
            new TreeSet<FileRecord>(new FileRecordComparator());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact c = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            Iterator<HistoryRecord> recs = reader.findLast(count);
            while (recs.hasNext())
            {
                result.add(createFileRecordFromHistoryRecord(recs.next(), c));
            }
        }

        return result;
    }

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
        throws RuntimeException
    {
        return findByKeyword(contact, keyword, false);
    }

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
        throws RuntimeException
    {
        return findByKeywords(contact, new String[]{keyword}, caseSensitive);
    }

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
        throws RuntimeException
    {
        return findByKeywords(contact, keywords, false);
    }

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
        throws RuntimeException
    {
        TreeSet<FileRecord> result =
            new TreeSet<FileRecord>(new FileRecordComparator());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact c = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            Iterator<HistoryRecord> recs =
                reader.findByKeywords(keywords, SEARCH_FIELD, caseSensitive);
            while (recs.hasNext())
            {
                result.add(createFileRecordFromHistoryRecord(recs.next(), c));
            }
        }

        return result;
    }

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
        throws RuntimeException
    {
        TreeSet<FileRecord> result =
            new TreeSet<FileRecord>(new FileRecordComparator());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact c = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            Iterator<HistoryRecord> recs =
                reader.findFirstRecordsAfter(date, count);
            while (recs.hasNext())
            {
                result.add(createFileRecordFromHistoryRecord(recs.next(), c));
            }
        }

        LinkedList<FileRecord> resultAsList = new LinkedList<FileRecord>(result);

        int toIndex = count;
        if(toIndex > resultAsList.size())
            toIndex = resultAsList.size();

        return resultAsList.subList(0, toIndex);
    }

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
        throws RuntimeException
    {
        TreeSet<FileRecord> result =
            new TreeSet<FileRecord>(new FileRecordComparator());
        // get the readers for this contact
        Map<Contact, HistoryReader> readers = getHistoryReaders(contact);

        for (Map.Entry<Contact, HistoryReader> readerEntry : readers.entrySet())
        {
            Contact c = readerEntry.getKey();
            HistoryReader reader = readerEntry.getValue();

            // add the progress listeners
            Iterator<HistoryRecord> recs =
                reader.findLastRecordsBefore(date, count);
            while (recs.hasNext())
            {
                result.add(createFileRecordFromHistoryRecord(recs.next(), c));
            }
        }

        LinkedList<FileRecord> resultAsList = new LinkedList<FileRecord>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * When new protocol provider is registered we check
     * does it supports FileTransfer and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService = bundleContext.getService(serviceEvent.getServiceReference());

        if (logger.isTraceEnabled())
            logger.trace("Received a service event for: " + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        if (logger.isDebugEnabled())
            logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            if (logger.isDebugEnabled())
                logger.debug("Handling registration of a new Protocol Provider.");

            this.handleProviderAdded((ProtocolProviderService)sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved( (ProtocolProviderService) sService);
        }
    }

    /**
     * Returns the history by specified local and remote contact
     * if one of them is null the default is used
     *
     * @param localContact Contact
     * @param remoteContact Contact
     * @return History
     * @throws IOException
     */
    private History getHistory(Contact localContact, Contact remoteContact)
            throws IOException
    {
        String localId = localContact == null ? "default" : localContact
                .getAddress();
        String remoteId = remoteContact == null ? "default" : remoteContact
                .getAddress();

        String account = "unkown";
        if (remoteContact != null)
            account = remoteContact.getProtocolProvider().getAccountID().
                getAccountUniqueID();

        HistoryID historyId = HistoryID.createFromRawID(
            new String[] {  "filehistory",
                            localId,
                            account,
                            remoteId });

        return this.historyService.createHistory(historyId, recordStructure);
    }

    /**
     * Listens for changes in file transfers.
     * @param event
     */
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        try
        {
            FileTransfer ft = event.getFileTransfer();
            String status = getStatus(ft.getStatus());

            // ignore events we don't need
            if(status == null)
                return;

            History history = getHistory(null, ft.getContact());
            HistoryWriter historyWriter = history.getWriter();

            historyWriter.updateRecord( STRUCTURE_NAMES[4],
                                        ft.getID(),
                                        STRUCTURE_NAMES[3],
                                        status);
        }
        catch (IOException e)
        {
            logger.error("Could not update file transfer log to history", e);
        }
    }

    private static String getDirection(int direction)
    {
        switch(direction)
        {
            case FileTransfer.IN :
                return FileRecord.IN;
            case FileTransfer.OUT :
                return FileRecord.OUT;
            default: return null;
        }
    }

    /**
     * Maps only the statuses we are interested in, otherwise returns null.
     * @param status the status as receive from FileTransfer
     * @return the corresponding status of FileRecord.
     */
    private static String getStatus(int status)
    {
        switch(status)
        {
            case FileTransferStatusChangeEvent.CANCELED :
                return FileRecord.CANCELED;
            case FileTransferStatusChangeEvent.COMPLETED :
                return FileRecord.COMPLETED;
            case FileTransferStatusChangeEvent.FAILED :
                return FileRecord.FAILED;
            case FileTransferStatusChangeEvent.REFUSED :
                return FileRecord.REFUSED;
            default: return null;
        }
    }

    /**
     * We ignore filetransfer requests.
     * @param event
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent event)
    {
        try
        {
            IncomingFileTransferRequest req = event.getRequest();

            History history = getHistory(null, req.getSender());
            HistoryWriter historyWriter = history.getWriter();

            SimpleDateFormat sdf
                = new SimpleDateFormat(HistoryService.DATE_FORMAT);
            historyWriter.addRecord(new String[]{
                req.getFileName(),
                getDirection(FileTransfer.IN),
                sdf.format(event.getTimestamp()),
                FILE_TRANSFER_ACTIVE,
                req.getID()
            });
        }
        catch (IOException e)
        {
            logger.error("Could not add file transfer log to history", e);
        }
    }

    /**
     * New file transfer was created.
     * @param event fileTransfer
     */
    public void fileTransferCreated(FileTransferCreatedEvent event)
    {
        FileTransfer fileTransfer = event.getFileTransfer();

        fileTransfer.addStatusListener(this);

        try
        {
            History history = getHistory(null, fileTransfer.getContact());
            HistoryWriter historyWriter = history.getWriter();

            if (fileTransfer.getDirection() == FileTransfer.IN)
            {
                historyWriter.updateRecord(
                    STRUCTURE_NAMES[4],
                    fileTransfer.getID(),
                    STRUCTURE_NAMES[0],
                    fileTransfer.getLocalFile().getCanonicalPath());
            }
            else if (fileTransfer.getDirection() == FileTransfer.OUT)
            {
                SimpleDateFormat sdf
                    = new SimpleDateFormat(HistoryService.DATE_FORMAT);

                historyWriter.addRecord(new String[]{
                    fileTransfer.getLocalFile().getCanonicalPath(),
                    getDirection(FileTransfer.OUT),
                    sdf.format(event.getTimestamp()),
                    FILE_TRANSFER_ACTIVE,
                    fileTransfer.getID()
                });
            }
        }
        catch (IOException e)
        {
            logger.error("Could not add file transfer log to history", e);
        }

    }

    /**
     * Called when a new <tt>IncomingFileTransferRequest</tt> has been rejected.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * received request which was rejected.
     */
    public void fileTransferRequestRejected(FileTransferRequestEvent event)
    {
        try
        {
            IncomingFileTransferRequest req = event.getRequest();

            History history = getHistory(null, req.getSender());
            HistoryWriter historyWriter = history.getWriter();

            historyWriter.updateRecord(
                STRUCTURE_NAMES[4],
                req.getID(),
                STRUCTURE_NAMES[3],
                FileRecord.REFUSED
            );
        }
        catch (IOException e)
        {
            logger.error("Could not add file transfer log to history", e);
        }
    }

    public void fileTransferRequestCanceled(FileTransferRequestEvent event)
    {
        try
        {
            IncomingFileTransferRequest req = event.getRequest();

            History history = getHistory(null, req.getSender());
            HistoryWriter historyWriter = history.getWriter();

            historyWriter.updateRecord(
                STRUCTURE_NAMES[4],
                req.getID(),
                STRUCTURE_NAMES[3],
                FileRecord.CANCELED
            );
        }
        catch (IOException e)
        {
            logger.error("Could not add file transfer log to history", e);
        }
    }

    /**
     * Permanently removes all locally stored file history.
     *
     * @throws java.io.IOException
     *         Thrown if the history could not be removed due to a IO error.
     */
    public void eraseLocallyStoredHistory()
        throws IOException
    {
        HistoryID historyId = HistoryID.createFromRawID(
            new String[] { "filehistory" });
        historyService.purgeLocallyStoredHistory(historyId);
    }

    /**
     * Permanently removes locally stored file history for the metacontact.
     *
     * @throws java.io.IOException
     *         Thrown if the history could not be removed due to a IO error.
     */
    public void eraseLocallyStoredHistory(MetaContact contact)
        throws IOException
    {
        Iterator<Contact> iter = contact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();

            History history = this.getHistory(null, item);
            historyService.purgeLocallyStoredHistory(history.getID());
        }
    }

    /**
     * Used to compare FileRecords
     * and to be ordered in TreeSet according their timestamp
     */
    private static class FileRecordComparator
        implements Comparator<FileRecord>
    {
        public int compare(FileRecord o1, FileRecord o2)
        {
            Date date1 = o1.getDate();
            Date date2 = o2.getDate();

            return date1.compareTo(date2);
        }
    }
}
