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

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>SendFileConversationComponent</tt> is the component added in the
 * chat conversation when user sends a file.
 *
 * @author Yana Stamcheva
 */
public class SendFileConversationComponent
    extends FileTransferConversationComponent
    implements FileTransferStatusListener
{
    private final String toContactName;

    private final ChatPanel parentChatPanel;

    private final Date date;

    private final String dateString;

    private final File file;

    /**
     * Creates a <tt>SendFileConversationComponent</tt> by specifying the parent
     * chat panel, where this component is added, the destination contact of
     * the transfer and file to transfer.
     *
     * @param chatPanel the parent chat panel, where this component is added
     * @param toContactName the name of the destination contact
     * @param file the file to transfer
     */
    public SendFileConversationComponent(   ChatPanel chatPanel,
                                            String toContactName,
                                            final File file)
    {
        this.parentChatPanel = chatPanel;
        this.toContactName = toContactName;
        this.file = file;

        // Create the date that would be shown in the component.
        this.date = new Date();
        this.dateString = getDateString(date);

        this.setCompletedDownloadFile(file);

        titleLabel.setText(
            dateString
            + resources.getI18NString(
                "service.gui.FILE_WAITING_TO_ACCEPT",
                    new String[]{toContactName}));

        fileLabel.setText(getFileLabel(file));

        progressBar.setMaximum((int) file.length());
        cancelButton.setVisible(true);

        retryButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JButton sourceButton = (JButton) e.getSource();

                if (sourceButton.equals(retryButton))
                {
                    setWarningStyle(false);

                    parentChatPanel
                        .sendFile(file, SendFileConversationComponent.this);
                }
            }
        });
    }

    /**
     * Sets the <tt>FileTransfer</tt> object received from the protocol and
     * corresponding to the file transfer process associated with this panel.
     *
     * @param fileTransfer the <tt>FileTransfer</tt> object associated with this
     * panel
     */
    public void setProtocolFileTransfer(FileTransfer fileTransfer)
    {
        this.setFileTransfer(fileTransfer, file.length());

        fileTransfer.addStatusListener(this);
    }

    /**
     * Handles file transfer status changes. Updates the interface to reflect
     * the changes.
     */
    public void statusChanged(final FileTransferStatusChangeEvent event)
    {
        FileTransfer fileTransfer = event.getFileTransfer();
        int status = event.getNewStatus();

        // We need to be sure that ui related work is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    statusChanged(event);
                }
            });
            return;
        }

        if (status == FileTransferStatusChangeEvent.COMPLETED
            || status == FileTransferStatusChangeEvent.CANCELED
            || status == FileTransferStatusChangeEvent.FAILED
            || status == FileTransferStatusChangeEvent.REFUSED)
        {
            parentChatPanel.removeActiveFileTransfer(fileTransfer.getID());
            fileTransfer.removeStatusListener(this);
        }

        if (status == FileTransferStatusChangeEvent.PREPARING)
        {
            hideProgressRelatedComponents();
            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_PREPARING",
                new String[]{toContactName}));
            cancelButton.setVisible(true);
            retryButton.setVisible(false);
        }
        else if (status == FileTransferStatusChangeEvent.FAILED)
        {
            setFailed();
            retryButton.setVisible(true);
        }
        else if (status == FileTransferStatusChangeEvent.IN_PROGRESS)
        {
            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_SENDING_TO",
                new String[]{toContactName}));
            setWarningStyle(false);

            if (!progressBar.isVisible())
            {
                progressBar.setVisible(true);
            }
            cancelButton.setVisible(true);
            retryButton.setVisible(false);
        }
        else if (status == FileTransferStatusChangeEvent.COMPLETED)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_SEND_COMPLETED",
                new String[]{toContactName}));

            cancelButton.setVisible(false);
            retryButton.setVisible(false);

            openFileButton.setVisible(true);
            openFolderButton.setVisible(true);
        }
        else if (status == FileTransferStatusChangeEvent.CANCELED)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_TRANSFER_CANCELED"));
            cancelButton.setVisible(false);
            retryButton.setVisible(true);
            setWarningStyle(true);
        }
        else if (status == FileTransferStatusChangeEvent.REFUSED)
        {
            hideProgressRelatedComponents();

            titleLabel.setText(
                dateString
                + resources.getI18NString(
                "service.gui.FILE_SEND_REFUSED",
                new String[]{toContactName}));
            cancelButton.setVisible(false);
            retryButton.setVisible(true);
            setWarningStyle(true);
        }
    }

    /**
     * Change the style of the component to be failed.
     */
    public void setFailed()
    {
        // We need to be sure that UI related work is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setFailed();
                }
            });
            return;
        }

        hideProgressRelatedComponents();

        titleLabel.setText(
            dateString
            + resources.getI18NString(
            "service.gui.FILE_UNABLE_TO_SEND",
            new String[]{toContactName}));
        cancelButton.setVisible(false);
        setWarningStyle(true);
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
     * Returns the label to show on the progress bar.
     *
     * @param bytesString the bytes that have been transfered
     * @return the label to show on the progress bar
     */
    @Override
    protected String getProgressLabel(String bytesString)
    {
        return bytesString
            + " " + resources.getI18NString("service.gui.SENT");
    }
}
