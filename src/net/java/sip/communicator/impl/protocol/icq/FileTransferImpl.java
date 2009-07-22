/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.io.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.*;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.*;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.*;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.*;

/**
 * The Filetransfer imeplementation for icq.
 * @author Damian Minkov
 */
public class FileTransferImpl
    extends AbstractFileTransfer
{
    /**
     * Logger
     */
    private static final Logger logger =
        Logger.getLogger(FileTransferImpl.class);

    private String id = null;
    private Contact contact = null;
    private File file = null;
    private int direction = -1;
    private long transferedBytes;
    private FileTransfer fileTransfer;

    public FileTransferImpl(
        FileTransfer fileTransfer,
        String id, Contact contact, File file, int direction)
    {
        this.fileTransfer = fileTransfer;
        this.id = id;
        this.contact = contact;
        this.file = file;
        this.direction = direction;

        fileTransfer.addEventListener(new IcqFileTransferEventListener());
    }

    /**
     * Cancels this file transfer. When this method is called transfer should
     * be interrupted.
     */
    public void cancel()
    {
        fileTransfer.close();
    }

    /**
     * Returns the number of bytes already transfered through this file transfer.
     *
     * @return the number of bytes already transfered through this file transfer
     */
    public long getTransferedBytes()
    {
        return transferedBytes;
    }

    /**
     * Uniquie ID that is identifying the FileTransfer
     * if the request has been accepted.
     *
     * @return the id.
     */
    public String getID()
    {
        return id;
    }

    /**
     * The file transfer direction.
     * @return returns the direction of the file transfer : IN or OUT.
     */
    public int getDirection()
    {
        return direction;
    }

    /**
     * Returns the file that is transfered.
     *
     * @return the file
     */
    public File getFile()
    {
        return file;
    }

    /**
     * Returns the contact that we are transfering files with.
     * @return the contact.
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * @param transferedBytes the transferedBytes to set
     */
    public void setTransferedBytes(long transferedBytes)
    {
        this.transferedBytes = transferedBytes;
    }

    /**
     * Provides support for files sent and received.
     */
    class IcqFileTransferEventListener
        implements RvConnectionEventListener
    {
        public void handleEventWithStateChange(
            final RvConnection transfer,
            RvConnectionState state,
            RvConnectionEvent event)
        {
            if (state==FileTransferState.CONNECTING)
            {
                // this both are hacks that detects cancels while transfering
                // as only connection is closed from other side
                // we detect it by receiving ConnectionTimedOutEvent
                if(transfer instanceof IncomingFileTransferImpl)
                {
                    ((IncomingFileTransferImpl)transfer).getStateController().
                        addControllerListener(new ControllerListener()
                    {

                        public void handleControllerSucceeded(
                            StateController controller, SuccessfulStateInfo info)
                        {
                        }

                        public void handleControllerFailed(
                            StateController controller, FailedStateInfo info)
                        {
                            if(info instanceof FailureEventInfo
                                && ((FailureEventInfo)info).getEvent()
                                    instanceof ConnectionTimedOutEvent)
                            {
                                FileTransferImpl.this.fireStatusChangeEvent(
                                    FileTransferStatusChangeEvent.CANCELED);
                                fileTransfer.close();
                            }
                        }
                    });
                }
                else if(transfer instanceof OutgoingFileTransferImpl)
                {
                    ((OutgoingFileTransferImpl)transfer).getStateController().
                        addControllerListener(new ControllerListener()
                    {

                        public void handleControllerSucceeded(
                            StateController controller, SuccessfulStateInfo info)
                        {
                        }

                        public void handleControllerFailed(
                            StateController controller, FailedStateInfo info)
                        {
                            if(info instanceof FailureEventInfo
                                && ((FailureEventInfo)info).getEvent()
                                    instanceof ConnectionTimedOutEvent)
                            {
                                FileTransferImpl.this.fireStatusChangeEvent(
                                    FileTransferStatusChangeEvent.CANCELED);
                                fileTransfer.close();
                            }
                        }
                    });
                }
            }
            else if (state==FileTransferState.FINISHED)
            {
                fireStatusChangeEvent(FileTransferStatusChangeEvent.COMPLETED);
            }
            else if (state==FileTransferState.FAILED)
            {
                if(event instanceof LocallyCancelledEvent)
                {
                    // sender cancels before other party accepts
                    fireStatusChangeEvent(FileTransferStatusChangeEvent.CANCELED);
                }
                else if(event instanceof BuddyCancelledEvent)
                {
                    fireStatusChangeEvent(FileTransferStatusChangeEvent.REFUSED);
                }
                else
                    fireStatusChangeEvent(FileTransferStatusChangeEvent.FAILED);
            }
            else if (state==FileTransferState.TRANSFERRING)
            {
                if (event instanceof TransferringFileEvent)
                {
                    fireStatusChangeEvent(
                        FileTransferStatusChangeEvent.IN_PROGRESS);

                    final ProgressStatusProvider psp = (
                        (TransferringFileEvent)event).getProgressProvider();

                    new Thread("Transfer for " + transfer.getBuddyScreenname())
                    {
                        public void run()
                        {
                            while (transfer.isOpen())
                            {
                                long transfered = psp.getPosition();

                                setTransferedBytes(transfered);
                                fireProgressChangeEvent(
                                    System.currentTimeMillis(), transfered);

                                try {
                                    Thread.sleep(100);
                                }
                                catch (InterruptedException e)
                                {}
                            }
                        }
                    }.start();
                }
            }
        }
        public void handleEvent(RvConnection transfer, RvConnectionEvent event)
        {
        }
    }
}
