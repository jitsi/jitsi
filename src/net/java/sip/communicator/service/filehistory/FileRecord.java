/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.filehistory;

import java.io.*;
import net.java.sip.communicator.service.protocol.Contact;

/**
 * Structure used for encapsulating data when writing or reading
 * File History Data.
 *
 * @author Damian Minkov
 */
public class FileRecord
{
    /**
     * Direction of the transfer: out
     */
    public final static String OUT = "out";

    /**
     * Direction of the transfer: in
     */
    public final static String IN = "in";

    /**
     * Status indicating that the file transfer has been completed.
     */
    public static final String COMPLETED = "completed";

    /**
     * Status indicating that the file transfer has been canceled.
     */
    public static final String CANCELED = "canceled";

    /**
     * Status indicating that the file transfer has failed.
     */
    public static final String FAILED = "failed";

    /**
     * Status indicating that the file transfer has been refused.
     */
    public static final String REFUSED = "refused";

    private String direction = null;

    private long date;

    private File file = null;
    private String status;

    private Contact contact;

    private String id = null;

    /**
     * Constructs new FileRecord
     *
     * @param id
     * @param contact
     * @param direction
     * @param date
     * @param file
     * @param status
     */
    public FileRecord(
        String id,
        Contact contact,
        String direction,
        long date,
        File file,
        String status)
    {
        this.contact = contact;
        this.direction = direction;
        this.date = date;
        this.file = file;
        this.status = status;
        this.id = id;
    }

    /**
     * The direction of the transfer.
     * @return the direction
     */
    public String getDirection()
    {
        return direction;
    }

    /**
     * The date of the record.
     * @return the date
     */
    public long getDate()
    {
        return date;
    }

    /**
     * The file that was transfered.
     * @return the file
     */
    public File getFile()
    {
        return file;
    }

    /**
     * The status of the transfer.
     * @return the status
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * The contact.
     * @return the contact
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * The id.
     * @return id.
     */
    public String getID()
    {
        return id;
    }
}
