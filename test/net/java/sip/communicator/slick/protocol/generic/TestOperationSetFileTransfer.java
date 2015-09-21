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
package net.java.sip.communicator.slick.protocol.generic;

import java.io.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;

/**
 * Generic test for file transfer operationset.
 *
 * @author Damian Minkov
 */
public abstract class TestOperationSetFileTransfer
    extends TestCase
{
    private static final Logger logger
        = Logger.getLogger(TestOperationSetFileTransfer.class);

    private FileAccessService fileAccessService = null;

    public TestOperationSetFileTransfer(String name)
    {
        super(name);
    }

    public TestOperationSetFileTransfer()
    {
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        start();
    }

    @Override
    protected void tearDown() throws Exception
    {
        stop();
        super.tearDown();
    }

    private FileAccessService getFileService()
    {
        if(fileAccessService == null)
        {
            ServiceReference ref = getContext()
                .getServiceReference(FileAccessService.class.getName());
            fileAccessService = (FileAccessService)getContext().getService(ref);
        }

        return fileAccessService;
    }

    private File getTempFileToTransfer(int size)
        throws Exception
    {
        File fileToTransfer =
            getFileService().getTemporaryFile();
        byte[] buff = new byte[size];
        Arrays.fill(buff, (byte)1);
        FileOutputStream out = new FileOutputStream(fileToTransfer);
        out.write(buff);
        out.flush();
        out.close();

        return fileToTransfer;
    }

    /**
     * Full file transfer process. Receiver and sender must end with event
     * fired for successful finish of the transfers on both sides.
     * @throws Exception
     */
    public void testSendAndReceive()
        throws Exception
    {
        if(!enableTestSendAndReceive())
            return;

        logger.trace("Start test : send and receive ok.");

        File fileToTransfer = getTempFileToTransfer(123456);

        OperationSetFileTransfer ftOpSet1 = getOpSetFilTransfer1();
        OperationSetFileTransfer ftOpSet2 = getOpSetFilTransfer2();

        FileTransferStatusEventCollector senderStatusListener =
            new FileTransferStatusEventCollector("Sender");

        FileTransferEventCollector senderFTListerner =
            new FileTransferEventCollector("Sender", senderStatusListener);
        ftOpSet1.addFileTransferListener(senderFTListerner);

        FileTransferStatusEventCollector receiverStatusListener =
            new FileTransferStatusEventCollector("Receiver");
        FileTransferEventCollector receiverFTListerner =
            new FileTransferEventCollector("Receiver", receiverStatusListener);
        ftOpSet2.addFileTransferListener(receiverFTListerner);

        try
        {
            ftOpSet1.sendFile(getContact1(), fileToTransfer);

            senderFTListerner.waitForEvent(2000);
            receiverFTListerner.waitForEvent(8000);
            senderStatusListener.waitForEvent(2000);

            // sender
            assertEquals("A file transfer created must be received on send side"
                         , 1, senderFTListerner.collectedEvents.size());

            FileTransferCreatedEvent fileTransferCreatedEvent
                = (FileTransferCreatedEvent)
                    senderFTListerner.collectedEvents.get(0);

            assertEquals("FileTransfer file"
                         ,fileTransferCreatedEvent.
                            getFileTransfer().getLocalFile()
                         ,fileToTransfer);

            assertEquals("A file transfer status changed - " +
                        "preparing received on send side"
                         , 1, senderStatusListener.collectedEvents.size());

            FileTransferStatusChangeEvent fileTransferStatusEvent
                = senderStatusListener.collectedEvents.get(0);

            assertEquals("Event must be preparing"
                ,FileTransferStatusChangeEvent.PREPARING
                ,fileTransferStatusEvent.getNewStatus());

            // receiver
            assertEquals("A file transfer request must be " +
                        "received on the receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            FileTransferRequestEvent fileTransferRequestEvent
                = (FileTransferRequestEvent)
                    receiverFTListerner.collectedEvents.get(0);

            IncomingFileTransferRequest req =
                fileTransferRequestEvent.getRequest();

            assertEquals("FileTransfer file name must be the same"
                         ,req.getFileName()
                         ,fileToTransfer.getName());
            assertEquals("FileTransfer file size must be the same"
                         ,req.getFileSize()
                         ,fileToTransfer.length());

            // now we will accpet the file
            // but let first clear the event listeners
            senderFTListerner.clear();
            receiverFTListerner.clear();
            senderStatusListener.clear();

            File receiveFile = getFileService().getTemporaryFile();
            req.acceptFile(receiveFile);

            senderFTListerner.waitForEvent(4000);
            receiverFTListerner.waitForEvent(4000);

            //receiver
            assertEquals("A file transfer created must be " +
                        "received on receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            fileTransferCreatedEvent = (FileTransferCreatedEvent)
                receiverFTListerner.collectedEvents.get(0);

            assertEquals("FileTransfer file"
                         ,fileTransferCreatedEvent.
                            getFileTransfer().getLocalFile()
                         ,receiveFile);

            receiverStatusListener.waitForEvent(30000, 3);

            // Some times we can receive only two events,
            // when connection is quickly established the preparing event
            // is missing.

            assertTrue("A file transfer status changed - " +
                "preparing, inprogress and completed received on receiver side"
                         , 3 == receiverStatusListener.collectedEvents.size()
                            ||
                           2 == receiverStatusListener.collectedEvents.size());

            fileTransferStatusEvent
                = receiverStatusListener.collectedEvents.get(0);

            if(receiverStatusListener.collectedEvents.size() == 3)
            {
                assertEquals("Event must be preparing"
                             ,FileTransferStatusChangeEvent.PREPARING
                             ,fileTransferStatusEvent.getNewStatus());

                fileTransferStatusEvent
                    = receiverStatusListener.collectedEvents.get(1);
            }

            assertEquals("Event must be in_progress"
                         ,FileTransferStatusChangeEvent.IN_PROGRESS
                         ,fileTransferStatusEvent.getNewStatus());

            if(receiverStatusListener.collectedEvents.size() == 3)
                fileTransferStatusEvent
                    = receiverStatusListener.collectedEvents.get(2);
            else
                fileTransferStatusEvent
                    = receiverStatusListener.collectedEvents.get(1);

            assertEquals("Event must be completed"
                         ,FileTransferStatusChangeEvent.COMPLETED
                         ,fileTransferStatusEvent.getNewStatus());

            // sender
            senderStatusListener.waitForEvent(4000, 2);
            assertTrue("Completed event must be received",
                senderStatusListener.contains(
                    FileTransferStatusChangeEvent.COMPLETED));
        }
        finally
        {
            ftOpSet1.removeFileTransferListener(senderFTListerner);
            ftOpSet2.addFileTransferListener(receiverFTListerner);
        }
    }

    /**
     * Sender sends file transfer request and cancel it before the remote side
     * accept it. (This is not supported by all protocols)
     *
     * @throws Exception
     */
    public void testSenderCancelBeforeAccepted()
        throws Exception
    {
        if(!enableTestSenderCancelBeforeAccepted())
            return;

        logger.trace("Start test : sender will cancel transfer before "
            + "receiver accept or decline it");

        File fileToTransfer = getTempFileToTransfer(1234567);

        OperationSetFileTransfer ftOpSet1 = getOpSetFilTransfer1();
        OperationSetFileTransfer ftOpSet2 = getOpSetFilTransfer2();

        FileTransferStatusEventCollector senderStatusListener =
            new FileTransferStatusEventCollector("Sender");

        FileTransferEventCollector senderFTListerner =
            new FileTransferEventCollector("Sender", senderStatusListener);
        ftOpSet1.addFileTransferListener(senderFTListerner);

        FileTransferEventCollector receiverFTListerner =
            new FileTransferEventCollector("Receiver", null);
        ftOpSet2.addFileTransferListener(receiverFTListerner);

        try
        {
            FileTransfer ft1 = ftOpSet1.sendFile(getContact1(), fileToTransfer);

            senderFTListerner.waitForEvent(2000);
            receiverFTListerner.waitForEvent(2000);
            senderStatusListener.waitForEvent(2000);

            // sender
            assertEquals("A file transfer created must be received on send side"
                         , 1, senderFTListerner.collectedEvents.size());

            FileTransferCreatedEvent fileTransferCreatedEvent
                = (FileTransferCreatedEvent)
                    senderFTListerner.collectedEvents.get(0);

            assertEquals("FileTransfer file"
                         ,fileTransferCreatedEvent.
                            getFileTransfer().getLocalFile()
                         ,fileToTransfer);

            assertEquals("A file transfer status changed - " +
                        "preparing received on send side"
                         , 1, senderStatusListener.collectedEvents.size());

            FileTransferStatusChangeEvent fileTransferStatusEvent
                = senderStatusListener.collectedEvents.get(0);

            assertEquals("Event must be preparing"
                         ,FileTransferStatusChangeEvent.PREPARING
                         ,fileTransferStatusEvent.getNewStatus());

            // receiver
            assertEquals("A file transfer request must be received " +
                        "on the receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            FileTransferRequestEvent fileTransferRequestEvent
                = (FileTransferRequestEvent)
                    receiverFTListerner.collectedEvents.get(0);

            IncomingFileTransferRequest req =
                fileTransferRequestEvent.getRequest();

            assertEquals("FileTransfer file name must be the same"
                         ,req.getFileName()
                         ,fileToTransfer.getName());
            assertEquals("FileTransfer file size must be the same"
                         ,req.getFileSize()
                         ,fileToTransfer.length());

            // now we will cancel the file
            // but let first clear the event listeners
            senderFTListerner.clear();
            receiverFTListerner.clear();
            senderStatusListener.clear();

            // now cancel
            ft1.cancel();

            receiverFTListerner.waitForEvent(6000);
            senderStatusListener.waitForEvent(6000);

            // sender
            assertEquals("A file transfer status changed - " +
                        "cancel received on send side"
                         , 1, senderStatusListener.collectedEvents.size());

            fileTransferStatusEvent
                = senderStatusListener.collectedEvents.get(0);

            assertEquals("Event must be canceled"
                         ,FileTransferStatusChangeEvent.CANCELED
                         ,fileTransferStatusEvent.getNewStatus());

            // the receiver must receive event refused
            assertEquals("A file transfer cancel must be " +
                        "received on receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            fileTransferRequestEvent = (FileTransferRequestEvent)
                receiverFTListerner.collectedEvents.get(0);

            assertTrue("FileTransfer must be canceled"
                         ,receiverFTListerner.isCanceled());
        }
        finally
        {
            ftOpSet1.removeFileTransferListener(senderFTListerner);
            ftOpSet2.addFileTransferListener(receiverFTListerner);
        }
    }

    public void testReceiverDecline()
        throws Exception
    {
        if(!enableTestReceiverDecline())
            return;

        logger.info("Start test: receiver will decline incoming fileTransfer");

        File fileToTransfer = getTempFileToTransfer(12345);

        OperationSetFileTransfer ftOpSet1 = getOpSetFilTransfer1();
        OperationSetFileTransfer ftOpSet2 = getOpSetFilTransfer2();

        FileTransferStatusEventCollector senderStatusListener =
            new FileTransferStatusEventCollector("Sender");

        FileTransferEventCollector senderFTListerner =
            new FileTransferEventCollector("Sender", senderStatusListener);
        ftOpSet1.addFileTransferListener(senderFTListerner);

        FileTransferEventCollector receiverFTListerner =
            new FileTransferEventCollector("Receiver", null);
        ftOpSet2.addFileTransferListener(receiverFTListerner);

        try
        {
            ftOpSet1.sendFile(getContact1(), fileToTransfer);

            senderFTListerner.waitForEvent(2000);
            receiverFTListerner.waitForEvent(6000);
            senderStatusListener.waitForEvent(2000);

            // sender
            assertEquals("A file transfer created must be received on send side"
                         , 1, senderFTListerner.collectedEvents.size());

            FileTransferCreatedEvent fileTransferCreatedEvent
                = (FileTransferCreatedEvent)
                senderFTListerner.collectedEvents.get(0);

            assertEquals("FileTransfer file"
                         ,fileTransferCreatedEvent.
                            getFileTransfer().getLocalFile()
                         ,fileToTransfer);

            assertEquals("A file transfer status changed - " +
                        "preparing received on send side"
                         , 1, senderStatusListener.collectedEvents.size());

            FileTransferStatusChangeEvent fileTransferStatusEvent
                = senderStatusListener.collectedEvents.get(0);

            assertEquals("Event must be preparing"
                         ,FileTransferStatusChangeEvent.PREPARING
                         ,fileTransferStatusEvent.getNewStatus());

            // receiver
            assertEquals("A file transfer request must be " +
                        "received on the receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            FileTransferRequestEvent fileTransferRequestEvent
                = (FileTransferRequestEvent)
                    receiverFTListerner.collectedEvents.get(0);

            IncomingFileTransferRequest req =
                fileTransferRequestEvent.getRequest();

            assertEquals("FileTransfer file name must be the same"
                         ,req.getFileName()
                         ,fileToTransfer.getName());
            assertEquals("FileTransfer file size must be the same"
                         ,req.getFileSize()
                         ,fileToTransfer.length());

            // now we will decline the file
            // but let first clear the event listeners
            senderFTListerner.clear();
            receiverFTListerner.clear();
            senderStatusListener.clear();

            req.rejectFile();

            senderStatusListener.waitForEvent(4000);
            receiverFTListerner.waitForEvent(4000);

            // receiver
            assertEquals("A file transfer rejected must be " +
                        "received on receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            fileTransferRequestEvent = (FileTransferRequestEvent)
                receiverFTListerner.collectedEvents.get(0);

            assertTrue("FileTransfer must be rejected"
                         ,receiverFTListerner.isRejected());

            // sender
            assertEquals("A file transfer status changed - " +
                        "refused received on send side"
                         , 1, senderStatusListener.collectedEvents.size());

            fileTransferStatusEvent
                = senderStatusListener.collectedEvents.get(0);

            assertTrue("Event must be refused (or failed)"
                         ,(fileTransferStatusEvent.getNewStatus()
                            == FileTransferStatusChangeEvent.REFUSED)
                          || (fileTransferStatusEvent.getNewStatus()
                                == FileTransferStatusChangeEvent.FAILED));
        }
        finally
        {
            ftOpSet1.removeFileTransferListener(senderFTListerner);
            ftOpSet2.addFileTransferListener(receiverFTListerner);
        }
    }

    /**
     * When the transfer starts waits for a while and the receiver
     * cancels the transfer while its in progress.
     *
     * @throws Exception
     */
    public void testReceiverCancelsWhileTransfering()
        throws Exception
    {
        if(!enableTestReceiverCancelsWhileTransfering())
            return;

        logger.trace("Start test : receiver will cancel " +
            "fileTransfer whil transfering.");

        File fileToTransfer = getTempFileToTransfer(12345678);

        OperationSetFileTransfer ftOpSet1 = getOpSetFilTransfer1();
        OperationSetFileTransfer ftOpSet2 = getOpSetFilTransfer2();

        FileTransferStatusEventCollector senderStatusListener =
            new FileTransferStatusEventCollector("Sender");

        FileTransferEventCollector senderFTListerner =
            new FileTransferEventCollector("Sender", senderStatusListener);

        ftOpSet1.addFileTransferListener(senderFTListerner);

        FileTransferStatusEventCollector receiverStatusListener =
            new FileTransferStatusEventCollector("Receiver");

        FileTransferEventCollector receiverFTListerner =
            new FileTransferEventCollector("Receiver", receiverStatusListener);

        ftOpSet2.addFileTransferListener(receiverFTListerner);

        try
        {
            ftOpSet1.sendFile(getContact1(), fileToTransfer);

            senderFTListerner.waitForEvent(4000);
            receiverFTListerner.waitForEvent(4000);
            senderStatusListener.waitForEvent(4000);

            // sender
            assertEquals("A file transfer created must be received on send side"
                         , 1, senderFTListerner.collectedEvents.size());

            FileTransferCreatedEvent fileTransferCreatedEvent
                = (FileTransferCreatedEvent)
                    senderFTListerner.collectedEvents.get(0);

            assertEquals("FileTransfer file"
                         ,fileTransferCreatedEvent.
                            getFileTransfer().getLocalFile()
                         ,fileToTransfer);

            assertEquals("A file transfer status changed - " +
                        "preparing received on send side"
                         , 1, senderStatusListener.collectedEvents.size());

            FileTransferStatusChangeEvent fileTransferStatusEvent
                = senderStatusListener.collectedEvents.get(0);

            assertEquals("Event must be preparing"
                         ,FileTransferStatusChangeEvent.PREPARING
                         ,fileTransferStatusEvent.getNewStatus());

            // receiver
            assertEquals("A file transfer request must be " +
                        "received on the receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            FileTransferRequestEvent fileTransferRequestEvent
                = (FileTransferRequestEvent)
                    receiverFTListerner.collectedEvents.get(0);

            IncomingFileTransferRequest req =
                fileTransferRequestEvent.getRequest();

            assertEquals("FileTransfer file name must be the same"
                         ,req.getFileName()
                         ,fileToTransfer.getName());
            assertEquals("FileTransfer file size must be the same"
                         ,req.getFileSize()
                         ,fileToTransfer.length());

            // now we will accpet the file
            // but let first clear the event listeners
            senderFTListerner.clear();
            receiverFTListerner.clear();
            senderStatusListener.clear();

            File receiveFile = getFileService().getTemporaryFile();

            final FileTransfer ft2 = req.acceptFile(receiveFile);

            // wait for preapring
            receiverStatusListener.waitForEvent(16000, 1);

            assertTrue("A file transfer status changed - " +
                "preparing or and inProgress received on receiver side"
                         , receiverStatusListener.collectedEvents.size() >= 1);

            FileTransferStatusChangeEvent stat1 =
                receiverStatusListener.collectedEvents.get(0);

            assertEquals("Event must be preparing"
                         ,FileTransferStatusChangeEvent.PREPARING
                         ,stat1.getNewStatus());

            // now wait if some protocol filres inProgress
            // jabber doesn't fire inProgress here
            // yahoo fires it
            receiverStatusListener.waitForEvent(14000);

            // wait in_progress
            senderStatusListener.waitForEvent(14000);

            receiverStatusListener.collectedEvents.clear();
            senderStatusListener.clear();

            ft2.cancel();

            // now wait for cancel
            receiverStatusListener.waitForEvent(14000);

            FileTransferStatusChangeEvent stat3 =
                receiverStatusListener.collectedEvents.get(0);

            assertEquals("Event must be canceled"
                         ,FileTransferStatusChangeEvent.CANCELED
                         ,stat3.getNewStatus());

            receiverFTListerner.waitForEvent(14000);

            //receiver
            assertEquals("A file transfer created must be " +
                        "received on receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            fileTransferCreatedEvent = (FileTransferCreatedEvent)
                receiverFTListerner.collectedEvents.get(0);

            assertEquals("FileTransfer file"
                         ,fileTransferCreatedEvent.
                            getFileTransfer().getLocalFile()
                         ,receiveFile);

            // sender
            senderStatusListener.waitForEvent(14000, 2);

            assertTrue("Must contain canceled event",
                senderStatusListener.contains(
                    FileTransferStatusChangeEvent.CANCELED));
        }
        finally
        {
            ftOpSet1.removeFileTransferListener(senderFTListerner);
            ftOpSet2.addFileTransferListener(receiverFTListerner);
        }
    }

    /**
     * When the transfer starts waits for a while and the sender
     * cancels the transfer while its in progress.
     *
     * @throws Exception
     */
    public void testSenderCancelsWhileTransfering()
        throws Exception
    {
        if(!enableTestSenderCancelsWhileTransfering())
            return;

        logger.trace("Start test : Sender will cancel fileTransfer" +
            " while transfering.");

        File fileToTransfer = getTempFileToTransfer(12345678);

        OperationSetFileTransfer ftOpSet1 = getOpSetFilTransfer1();
        OperationSetFileTransfer ftOpSet2 = getOpSetFilTransfer2();

        FileTransferStatusEventCollector senderStatusListener =
            new FileTransferStatusEventCollector("Sender");

        FileTransferEventCollector senderFTListerner =
            new FileTransferEventCollector("Sender", senderStatusListener);
        ftOpSet1.addFileTransferListener(senderFTListerner);

        FileTransferEventCollector receiverFTListerner =
            new FileTransferEventCollector("Receiver", null);
        ftOpSet2.addFileTransferListener(receiverFTListerner);

        try
        {
            FileTransfer ft1 = ftOpSet1.sendFile(getContact1(), fileToTransfer);

            senderFTListerner.waitForEvent(4000);
            receiverFTListerner.waitForEvent(4000);
            senderStatusListener.waitForEvent(4000);

            // sender
            assertEquals("A file transfer created must be received on send side"
                         , 1, senderFTListerner.collectedEvents.size());

            FileTransferCreatedEvent fileTransferCreatedEvent
                = (FileTransferCreatedEvent)
                    senderFTListerner.collectedEvents.get(0);

            assertEquals("FileTransfer file"
                         ,fileTransferCreatedEvent.
                            getFileTransfer().getLocalFile()
                         ,fileToTransfer);

            assertEquals("A file transfer status changed - " +
                        "preparing received on send side"
                         , 1, senderStatusListener.collectedEvents.size());

            FileTransferStatusChangeEvent fileTransferStatusEvent
                = senderStatusListener.collectedEvents.get(0);

            assertEquals("Event must be preparing"
                         ,FileTransferStatusChangeEvent.PREPARING
                         ,fileTransferStatusEvent.getNewStatus());

            // receiver
            assertEquals("A file transfer request must be " +
                        "received on the receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            FileTransferRequestEvent fileTransferRequestEvent
                = (FileTransferRequestEvent)
                    receiverFTListerner.collectedEvents.get(0);

            IncomingFileTransferRequest req =
                fileTransferRequestEvent.getRequest();

            assertEquals("FileTransfer file name must be the same"
                         ,req.getFileName()
                         ,fileToTransfer.getName());
            assertEquals("FileTransfer file size must be the same"
                         ,req.getFileSize()
                         ,fileToTransfer.length());

            // now we will accpet the file
            // but let first clear the event listeners
            senderFTListerner.clear();
            receiverFTListerner.clear();
            senderStatusListener.clear();

            File receiveFile = getFileService().getTemporaryFile();
            FileTransferStatusEventCollector receiverStatusListener =
                new FileTransferStatusEventCollector("Receiver");

            final FileTransfer ft2 = req.acceptFile(receiveFile);
            ft2.addStatusListener(receiverStatusListener);

            // sender
            // now wait for progress
            senderStatusListener.waitForEvent(4000);
            FileTransferStatusChangeEvent stat1 =
                senderStatusListener.collectedEvents.get(0);
            senderStatusListener.collectedEvents.clear();

            assertEquals("Event must be inProgress"
                         ,FileTransferStatusChangeEvent.IN_PROGRESS
                         ,stat1.getNewStatus());

            ft1.cancel();

            // now wait for cancel
            senderStatusListener.waitForEvent(4000);
            FileTransferStatusChangeEvent stat2 =
                senderStatusListener.collectedEvents.get(0);
            senderStatusListener.collectedEvents.clear();

            assertEquals("Event must be canceled"
                         ,FileTransferStatusChangeEvent.CANCELED
                         ,stat2.getNewStatus());

            //receiver
            receiverFTListerner.waitForEvent(4000);
            assertEquals("A file transfer created must be received on " +
                        "receiver side"
                         , 1, receiverFTListerner.collectedEvents.size());

            fileTransferCreatedEvent = (FileTransferCreatedEvent)
                receiverFTListerner.collectedEvents.get(0);

            assertEquals("FileTransfer file"
                         ,fileTransferCreatedEvent.
                            getFileTransfer().getLocalFile()
                         ,receiveFile);

            receiverStatusListener.waitForEvent(4000, 3);
            assertTrue("Cancel event must be received",
                receiverStatusListener.contains(
                    FileTransferStatusChangeEvent.CANCELED));
        }
        finally
        {
            ftOpSet1.removeFileTransferListener(senderFTListerner);
            ftOpSet2.addFileTransferListener(receiverFTListerner);
        }
    }

    private static String statusToString(int s)
    {
        switch(s)
        {
            case FileTransferStatusChangeEvent.CANCELED : return "canceled";
            case FileTransferStatusChangeEvent.COMPLETED : return "completed";
            case FileTransferStatusChangeEvent.FAILED : return "failed";
            case FileTransferStatusChangeEvent.IN_PROGRESS :
                return "in_progress";
            case FileTransferStatusChangeEvent.PREPARING : return "preparing";
            case FileTransferStatusChangeEvent.REFUSED : return "refused";
            default : return "waiting";

        }
    }

    public abstract void start() throws Exception;
    public abstract BundleContext getContext();
    public abstract Contact getContact1();
    public abstract Contact getContact2();
    public abstract OperationSetFileTransfer getOpSetFilTransfer1();
    public abstract OperationSetFileTransfer getOpSetFilTransfer2();
    public abstract boolean enableTestSendAndReceive();
    public abstract boolean enableTestSenderCancelBeforeAccepted();
    public abstract boolean enableTestReceiverDecline();
    public abstract boolean enableTestReceiverCancelsWhileTransfering();
    public abstract boolean enableTestSenderCancelsWhileTransfering();
    public abstract void stop() throws Exception;

    /**
     * Allows tests to wait for and collect events issued upon creation and
     * reception of files.
     */
    public class FileTransferEventCollector
        implements FileTransferListener
    {
        public final List<EventObject> collectedEvents
            = new ArrayList<EventObject>();

        private boolean rejected = false;
        private boolean canceled = false;
        private FileTransferStatusEventCollector statusCollector = null;
        private String name = null;

        /**
         * Creates an instance of this call event collector and registers it
         * with listenedOpSet.
         * @param name of collector
         * @param statusCollector the event collector
         */
        public FileTransferEventCollector(
            String name, FileTransferStatusEventCollector statusCollector)
        {
            this.name = name;
            this.statusCollector = statusCollector;
        }

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            logger.trace("Waiting for a FileTransferEvent");

            synchronized(this)
            {
                if(collectedEvents.size() > 0){
                    logger.trace("Event already received. " + collectedEvents);
                    return;
                }

                try{
                    wait(waitFor);
                    if(collectedEvents.size() > 0)
                        logger.trace("Received a FileTransferEvent.");
                    else
                        logger.trace("No FileTransferEvent received for "+waitFor+"ms.");

                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a FileTransferEvent", ex);
                }
            }
        }

        /**
         * Stores the received event and notifies all waiting on this object
         * @param event the event
         */
        public void fileTransferRequestReceived(FileTransferRequestEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    name + " Collected evt("+collectedEvents.size()+")= "+event);

                this.collectedEvents.add(event);
                notifyAll();
            }
        }

        /**
         * Stores the received event and notifies all waiting on this object
         * @param event the event
         */
        public void fileTransferCreated(FileTransferCreatedEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    name + " Collected evt("+collectedEvents.size()+")= "+event);

                if(statusCollector != null)
                {
                    event.getFileTransfer().addStatusListener(statusCollector);
                }

                this.collectedEvents.add(event);
                notifyAll();
            }
        }

        /**
         * Stores the received event and notifies all waiting on this object
         * @param event the event
         */
        public void fileTransferRequestRejected(FileTransferRequestEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    name + " Collected evt("+collectedEvents.size()+")= "+event);

                rejected = true;
                this.collectedEvents.add(event);
                notifyAll();
            }
        }

        public void fileTransferRequestCanceled(FileTransferRequestEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    name + " Collected evt("+collectedEvents.size()+")= "+event);

                canceled = true;
                this.collectedEvents.add(event);
                notifyAll();
            }
        }

        public void clear()
        {
            collectedEvents.clear();
            rejected = false;
        }

        /**
         * @return the rejected
         */
        public boolean isRejected()
        {
            return rejected;
        }

        /**
         * @return the rejected
         */
        public boolean isCanceled()
        {
            return canceled;
        }
    }

    /**
     * Allows tests to wait for and collect events issued upon creation and
     * reception of files.
     */
    public class FileTransferStatusEventCollector
        implements FileTransferStatusListener
    {
        public ArrayList<FileTransferStatusChangeEvent> collectedEvents =
            new ArrayList<FileTransferStatusChangeEvent>();
        private String name = null;
        private int eventsNum = 1;

        FileTransferStatusEventCollector(String name)
        {
            this.name = name;
        }

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         * @param eventsNum number of events to wait for
         */
        public void waitForEvent(long waitFor, int eventsNum)
        {
            this.eventsNum = eventsNum;
            logger.trace("Waiting for a FileTransfer Status Event");

            synchronized(this)
            {
                if(collectedEvents.size() > (eventsNum - 1))
                {
                    logger.trace("Event already received. " + collectedEvents);
                    return;
                }

                try
                {
                    wait(waitFor);
                    if(collectedEvents.size() > (eventsNum - 1))
                        logger.trace("Received a FileTransferEvent.");
                    else
                        logger.trace("Not enough FileTransferEvent received for"
                            + waitFor + "ms.");
                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a FileTransferEvent", ex);
                }
            }
        }

        /**
         * Blocks until at least one event is received or until waitFor
         * miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            waitForEvent(waitFor, 1);
        }

        /**
         * Stores the received event and notifies all waiting on this object
         * @param event the event
         */
        public void statusChanged(FileTransferStatusChangeEvent event)
        {
            synchronized(this)
            {
                logger.debug(
                    name + " Collected evt("+collectedEvents.size()+")= "+event
                    +" status:"+statusToString(event.getNewStatus()));

                this.collectedEvents.add(event);

                // notifies that waiting for collecting events must stop
                // only if the needed number of events is collected
                if(collectedEvents.size() == eventsNum)
                    notifyAll();
            }

        }

        public boolean contains(int status)
        {
            synchronized(this)
            {
                Iterator<FileTransferStatusChangeEvent>
                    iter = collectedEvents.iterator();
                String statuses = "";
                while (iter.hasNext())
                {
                    FileTransferStatusChangeEvent e = iter.next();
                    if(e.getNewStatus() == status)
                        return true;

                    statuses += e.getNewStatus() + " ";
                }

                logger.warn("Status not found : " + status +
                    " between statuses : " + statuses);

                return false;
            }
        }

        /**
         * clears the received events and sets the initial value of
         * eventsNum.
         */
        public void clear()
        {
            collectedEvents.clear();
            eventsNum = 1;
        }


    }
}
