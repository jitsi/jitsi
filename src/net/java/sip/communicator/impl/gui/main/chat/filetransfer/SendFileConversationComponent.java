/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.filetransfer;

import java.awt.event.*;
import java.io.*;

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

        this.setCompletedDownloadFile(file);

        titleLabel.setText(resources.getI18NString(
            "service.gui.FILE_WAITING_TO_ACCEPT",
            new String[]{toContactName}));

        fileLabel.setText(getFileName(file));

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
        this.setFileTransfer(fileTransfer);

        fileTransfer.addStatusListener(this);
    }

    /**
     * Handles file transfer status changes. Updates the interface to reflect
     * the changes.
     */
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        int status = event.getNewStatus();

        if (status == FileTransferStatusChangeEvent.PREPARING)
        {
            progressBar.setVisible(false);
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_TRANSFER_PREPARING",
                new String[]{toContactName}));
            cancelButton.setVisible(true);
            retryButton.setVisible(false);
        }
        else if (status == FileTransferStatusChangeEvent.FAILED)
        {
            progressBar.setVisible(false);
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_UNABLE_TO_SEND",
                new String[]{toContactName}));
            cancelButton.setVisible(false);
            retryButton.setVisible(true);
            setWarningStyle(true);
        }
        else if (status == FileTransferStatusChangeEvent.IN_PROGRESS)
        {
            titleLabel.setText(resources.getI18NString(
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
            progressBar.setVisible(false);
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_SEND_COMPLETED",
                new String[]{toContactName}));
            cancelButton.setVisible(false);
            retryButton.setVisible(false);
        }
        else if (status == FileTransferStatusChangeEvent.CANCELED)
        {
            progressBar.setVisible(false);
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_TRANSFER_CANCELED"));
            cancelButton.setVisible(false);
            retryButton.setVisible(true);
            setWarningStyle(true);
        }
        else if (status == FileTransferStatusChangeEvent.REFUSED)
        {
            progressBar.setVisible(false);
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_SEND_REFUSED",
                new String[]{toContactName}));
            cancelButton.setVisible(false);
            retryButton.setVisible(true);
            setWarningStyle(true);
        }
    }
}
