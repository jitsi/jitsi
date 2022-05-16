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

import java.util.*;

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
    private final FileRecord fileRecord;

    public FileHistoryConversationComponent(FileRecord fileRecord)
    {
        this.fileRecord = fileRecord;

        String contactName = fileRecord.getContact().getDisplayName();

        openFileButton.setVisible(true);
        openFolderButton.setVisible(true);

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

                openFileButton.setVisible(false);
                openFolderButton.setVisible(false);

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

        Date date = fileRecord.getDate();

        titleLabel.setText(
            getDateString(date) + titleString);
        fileLabel.setText(getFileLabel(fileRecord.getFile()));
    }

    /**
     * Returns the date of the component event.
     *
     * @return the date of the component event
     */
    @Override
    public Date getDate()
    {
        return fileRecord.getDate();
    }

    /**
     * We don't have progress label in history.
     *
     * @return empty string
     */
    @Override
    protected String getProgressLabel(String bytesString)
    {
        return "";
    }
}
