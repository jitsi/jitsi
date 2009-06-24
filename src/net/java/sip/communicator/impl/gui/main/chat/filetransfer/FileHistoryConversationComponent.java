/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.filetransfer;

import net.java.sip.communicator.service.filehistory.*;

/**
 * The component used to show a file transfer history record in the chat or
 * history window.
 * 
 * @author Yana Stamcheva
 */
public class FileHistoryConversationComponent
    extends FileTransferConversationComponent
{
    public FileHistoryConversationComponent(FileRecord fileRecord)
    {
        String contactName = fileRecord.getContact().getDisplayName();

        String titleString = "";
        if (fileRecord.getDirection().equals(FileRecord.IN))
        {
            if (fileRecord.getStatus().equals(FileRecord.COMPLETED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_RECEIVE_COMPLETED",
                    new String[]{contactName});

                setWarningStyle(false);
            }
            else if (fileRecord.getStatus().equals(FileRecord.CANCELED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_TRANSFER_CANCELED");

                setWarningStyle(true);
            }
            else if (fileRecord.getStatus().equals(FileRecord.FAILED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_RECEIVE_FAILED",
                    new String[]{contactName});

                setWarningStyle(true);
            }
            else if (fileRecord.getStatus().equals(FileRecord.REFUSED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_TRANSFER_REFUSED",
                    new String[]{contactName});

                setWarningStyle(true);
            }
        }
        else if (fileRecord.getDirection().equals(FileRecord.OUT))
        {
            if (fileRecord.getStatus().equals(FileRecord.COMPLETED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_SEND_COMPLETED",
                    new String[]{contactName});

                setWarningStyle(false);
            }
            else if (fileRecord.getStatus().equals(FileRecord.CANCELED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_TRANSFER_CANCELED");

                setWarningStyle(true);
            }
            else if (fileRecord.getStatus().equals(FileRecord.FAILED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_UNABLE_TO_SEND",
                    new String[]{contactName});

                setWarningStyle(true);
            }
            else if (fileRecord.getStatus().equals(FileRecord.REFUSED))
            {
                titleString = resources.getI18NString(
                    "service.gui.FILE_SEND_REFUSED",
                    new String[]{contactName});
                setWarningStyle(true);
            }
        }

        this.setCompletedDownloadFile(fileRecord.getFile());

        titleLabel.setText(titleString);
        fileLabel.setText(getFileName(fileRecord.getFile()));
        openFileButton.setVisible(true);
        openFolderButton.setVisible(true);
    }
}
