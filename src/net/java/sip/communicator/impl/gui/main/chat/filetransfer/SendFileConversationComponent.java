/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        FileTransfer fileTransfer = event.getFileTransfer();
        int status = event.getNewStatus();

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
    protected String getProgressLabel(String bytesString)
    {
        return bytesString
            + " " + resources.getI18NString("service.gui.SENT");
    }
}
