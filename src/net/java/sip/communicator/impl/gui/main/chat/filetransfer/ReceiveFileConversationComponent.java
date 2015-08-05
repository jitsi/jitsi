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
package net.java.sip.communicator.impl.gui.main.chat.filetransfer;

import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
// Disambiguates SwingWorker on Java 6 in the presence of javax.swing.*

/**
 * The <tt>ReceiveFileConversationComponent</tt> is the component shown in the
 * conversation area of the chat window to display a incoming file transfer.
 *
 * @author Yana Stamcheva
 */
public class ReceiveFileConversationComponent
    extends FileTransferConversationComponent
    implements  ActionListener,
                FileTransferStatusListener,
                FileTransferListener
{
    private final Logger logger
        = Logger.getLogger(ReceiveFileConversationComponent.class);

    private final IncomingFileTransferRequest fileTransferRequest;

    private final OperationSetFileTransfer fileTransferOpSet;

    private final ChatPanel chatPanel;

    private final Date date;

    private final String dateString;

    private File downloadFile;

    /**
     * Creates a <tt>ReceiveFileConversationComponent</tt>.
     * @param chatPanel the chat panel
     * @param opSet the <tt>OperationSetFileTransfer</tt>
     * @param request the <tt>IncomingFileTransferRequest</tt>
     * associated with this component
     * @param date the date
     */
    public ReceiveFileConversationComponent(
        ChatPanel chatPanel,
        final OperationSetFileTransfer opSet,
        final IncomingFileTransferRequest request,
        final Date date)
    {
        this.chatPanel = chatPanel;
        this.fileTransferOpSet = opSet;
        this.fileTransferRequest = request;
        this.date = date;
        this.dateString = getDateString(date);

        fileTransferOpSet.addFileTransferListener(this);

        byte[] thumbnail = request.getThumbnail();

        if (thumbnail != null && thumbnail.length > 0)
        {
            ImageIcon thumbnailIcon = new ImageIcon(thumbnail);

            if (thumbnailIcon.getIconWidth() > IMAGE_WIDTH
                || thumbnailIcon.getIconHeight() > IMAGE_HEIGHT)
            {
                thumbnailIcon
                    = ImageUtils.getScaledRoundedIcon(
                        thumbnail, IMAGE_WIDTH, IMAGE_WIDTH);
            }

            imageLabel.setIcon(thumbnailIcon);
        }

        titleLabel.setText(
            dateString
            + resources.getI18NString(
            "service.gui.FILE_TRANSFER_REQUEST_RECIEVED",
            new String[]{fileTransferRequest.getSender().getDisplayName()}));

        String fileName
            = getFileLabel(request.getFileName(), request.getFileSize());
        fileLabel.setText(fileName);

        acceptButton.setVisible(true);
        acceptButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                titleLabel.setText(
                    dateString
                    + resources
                    .getI18NString("service.gui.FILE_TRANSFER_PREPARING",
                                    new String[]{fileTransferRequest.getSender()
                                                .getDisplayName()}));
                acceptButton.setVisible(false);
                rejectButton.setVisible(false);
                cancelButton.setVisible(true);
                progressBar.setVisible(true);

                downloadFile = createFile(fileTransferRequest);

                new AcceptFile(downloadFile).start();
            }
        });

        rejectButton.setVisible(true);
        rejectButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                fileTransferRequest.rejectFile();

                acceptButton.setVisible(false);
                rejectButton.setVisible(false);
                setWarningStyle(true);
                fileLabel.setText("");
                titleLabel.setText(
                    dateString
                    + resources.getI18NString(
                        "service.gui.FILE_TRANSFER_REFUSED"));
                ReceiveFileConversationComponent.this.chatPanel
                    .removeActiveFileTransfer(fileTransferRequest.getID());
            }
        });

        progressBar.setMaximum((int)fileTransferRequest.getFileSize());
    }

    /**
     * Creates the file to download.
     *
     * @return the file to download.
     */
    private File createFile(IncomingFileTransferRequest fileTransferRequest)
    {
        File downloadFile = null;
        File downloadDir = null;

        String incomingFileName = fileTransferRequest.getFileName();
        try
        {
            downloadDir = GuiActivator.getFileAccessService()
                .getDefaultDownloadDirectory();

            if (!downloadDir.exists())
            {
                if (!downloadDir.mkdirs())
                {
                    logger.error("Could not create the download directory : "
                        + downloadDir.getAbsolutePath());
                }
                if (logger.isDebugEnabled())
                    logger.debug("Download directory created : "
                        + downloadDir.getAbsolutePath());
            }
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Unable to find download directory.", e);
        }

        downloadFile = new File(downloadDir, incomingFileName);

        // If a file with the given name already exists, add an index to the
        // file name.
        int index = 0;
        int filenameLenght = incomingFileName.lastIndexOf(".");
        if(filenameLenght == -1)
        {
            filenameLenght = incomingFileName.length();
        }
        while (downloadFile.exists())
        {
            String newFileName
             = incomingFileName.substring(0, filenameLenght)
                 + "-" + ++index
                 + incomingFileName.substring(filenameLenght);

            downloadFile = new File(downloadDir, newFileName);
        }

        // Change the file name to the name we would use on the local file
        // system.
        if (!downloadFile.getName().equals(fileTransferRequest.getFileName()))
        {
            String fileName
                = getFileLabel( downloadFile.getName(),
                                fileTransferRequest.getFileSize());

            fileLabel.setText(fileName);
        }

        return downloadFile;
    }

    /**
     * Handles status changes in file transfer.
     */
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        FileTransfer fileTransfer = event.getFileTransfer();
        int status = event.getNewStatus();

        String fromContactName
            = fileTransferRequest.getSender().getDisplayName();

        if (status == FileTransferStatusChangeEvent.COMPLETED
            || status == FileTransferStatusChangeEvent.CANCELED
            || status == FileTransferStatusChangeEvent.FAILED
            || status == FileTransferStatusChangeEvent.REFUSED)
        {
            fileTransfer.removeStatusListener(this);
        }

        if (status == FileTransferStatusChangeEvent.PREPARING)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_PREPARING",
                new String[]{fromContactName}));
        }
        else if (status == FileTransferStatusChangeEvent.FAILED)
        {
            hideProgressRelatedComponents();
            cancelButton.setVisible(false);

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_RECEIVE_FAILED",
                new String[]{fromContactName}));

            setWarningStyle(true);
        }
        else if (status == FileTransferStatusChangeEvent.IN_PROGRESS)
        {
            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_RECEIVING_FROM",
                new String[]{fromContactName}));
            setWarningStyle(false);

            if (!progressBar.isVisible())
            {
                progressBar.setVisible(true);
            }
        }
        else if (status == FileTransferStatusChangeEvent.COMPLETED)
        {
            this.setCompletedDownloadFile(downloadFile);

            hideProgressRelatedComponents();
            cancelButton.setVisible(false);

            openFileButton.setVisible(true);
            openFolderButton.setVisible(true);

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_RECEIVE_COMPLETED",
                new String[]{fromContactName}));
        }
        else if (status == FileTransferStatusChangeEvent.CANCELED)
        {
            hideProgressRelatedComponents();

            cancelButton.setVisible(false);

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_CANCELED"));
            setWarningStyle(true);
        }
        else if (status == FileTransferStatusChangeEvent.REFUSED)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_REFUSED",
                new String[]{fromContactName}));
            cancelButton.setVisible(false);
            openFileButton.setVisible(false);
            openFolderButton.setVisible(false);

            setWarningStyle(true);
        }
    }

    /**
     * Returns the date of the component event.
     *
     * @return the date of the component event
     */
    @Override
    public Date getDate()
    {
        return date;
    }

    /**
     * Accepts the file in a new thread.
     */
    private class AcceptFile extends SwingWorker
    {
        private FileTransfer fileTransfer;

        private final File downloadFile;

        public AcceptFile(File downloadFile)
        {
            this.downloadFile = downloadFile;
        }

        @Override
        public Object construct()
        {
            fileTransfer = fileTransferRequest.acceptFile(downloadFile);

            chatPanel.addActiveFileTransfer(fileTransfer.getID(), fileTransfer);

            // Remove previously added listener, that notified us for request
            // cancellations.
            fileTransferOpSet.removeFileTransferListener(
                ReceiveFileConversationComponent.this);

            // Add the status listener that would notify us when the file
            // transfer has been completed and should be removed from
            // active components.
            fileTransfer.addStatusListener(chatPanel);

            fileTransfer.addStatusListener(
                ReceiveFileConversationComponent.this);

            return "";
        }

        @Override
        public void finished()
        {
            if (fileTransfer != null)
            {
                setFileTransfer(fileTransfer, fileTransferRequest.getFileSize());
            }
        }
    }

    /**
     * Returns the label to show on the progress bar.
     *
     * @param bytesString the bytes that have been transfered
     * @return the label to show on the progress bar
     */
    @Override
    protected String getProgressLabel(String bytesString)
    {
        return resources.getI18NString("service.gui.RECEIVED",
            new String[]{bytesString});
    }

    public void fileTransferCreated(FileTransferCreatedEvent event)
    {}

    public void fileTransferRequestCanceled(FileTransferRequestEvent event)
    {
        IncomingFileTransferRequest request = event.getRequest();

        if (request.equals(fileTransferRequest))
        {
            acceptButton.setVisible(false);
            rejectButton.setVisible(false);

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_CANCELED"));

            setWarningStyle(true);
        }
    }

    public void fileTransferRequestReceived(FileTransferRequestEvent event)
    {}

    public void fileTransferRequestRejected(FileTransferRequestEvent event)
    {}
}
