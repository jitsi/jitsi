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

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>FileTransferConversationComponent</tt> is the parent of all file
 * conversation components - for incoming, outgoing and history file transfers.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public abstract class FileTransferConversationComponent
    extends ChatConversationComponent
    implements  ActionListener,
                FileTransferProgressListener,
                Skinnable
{
    /**
     * The logger for this class.
     */
    private final Logger logger
        = Logger.getLogger(FileTransferConversationComponent.class);

    /**
     * Image default width.
     */
    protected static final int IMAGE_WIDTH = 64;

    /**
     * Image default height.
     */
    protected static final int IMAGE_HEIGHT = 64;

    /**
     * The image label.
     */
    protected final FileImageLabel imageLabel;

    /**
     * The title label.
     */
    protected final JLabel titleLabel = new JLabel();

    /**
     * The file label.
     */
    protected final JLabel fileLabel = new JLabel();

    /**
     * The error area.
     */
    private final JTextArea errorArea = new JTextArea();

    /**
     * The error icon label.
     */
    private final JLabel errorIconLabel = new JLabel(
        new ImageIcon(ImageLoader.getImage(ImageLoader.EXCLAMATION_MARK)));

    /**
     * The cancel button.
     */
    protected final ChatConversationButton cancelButton
        = new ChatConversationButton();

    /**
     * The retry button.
     */
    protected final ChatConversationButton retryButton
        = new ChatConversationButton();

    /**
     * The accept button.
     */
    protected final  ChatConversationButton acceptButton
        = new ChatConversationButton();

    /**
     * The reject button.
     */
    protected final ChatConversationButton rejectButton
        = new ChatConversationButton();

    /**
     * The open file button.
     */
    protected final ChatConversationButton openFileButton
        = new ChatConversationButton();

    /**
     * The open folder button.
     */
    protected final ChatConversationButton openFolderButton
        = new ChatConversationButton();

    /**
     * The progress bar.
     */
    protected final JProgressBar progressBar = new JProgressBar();

    /**
     * The progress properties panel.
     */
    private final TransparentPanel progressPropertiesPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    /**
     * The progress speed label.
     */
    private final JLabel progressSpeedLabel = new JLabel();

    /**
     * The estimated time label.
     */
    private final JLabel estimatedTimeLabel = new JLabel();

    /**
     * The download file.
     */
    private File downloadFile;

    /**
     * The file transfer.
     */
    private FileTransfer fileTransfer;

    /**
     * The speed calculated delay.
     */
    private final static int SPEED_CALCULATE_DELAY = 5000;

    /**
     * The transferred file size.
     */
    private long transferredFileSize = 0;

    /**
     * The time of the last calculated transfer speed.
     */
    private long lastSpeedTimestamp = 0;

    /**
     * The last estimated time for the transfer.
     */
    private long lastEstimatedTimeTimestamp = 0;

    /**
     * The number of bytes last transferred.
     */
    private long lastTransferredBytes = 0;

    /**
     * The last calculated progress speed.
     */
    private long lastProgressSpeed;

    /**
     * The last estimated time.
     */
    private long lastEstimatedTime;

    /**
     * Creates a file conversation component.
     */
    public FileTransferConversationComponent()
    {
        imageLabel = new FileImageLabel();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 4;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);

        add(imageLabel, constraints);
        imageLabel.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.DEFAULT_FILE_ICON)));

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);

        add(titleLabel, constraints);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 5, 5);

        add(fileLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.fill = GridBagConstraints.NONE;

        add(errorIconLabel, constraints);
        errorIconLabel.setVisible(false);

        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        add(errorArea, constraints);
        errorArea.setForeground(
            new Color(resources.getColor("service.gui.ERROR_FOREGROUND")));
        setTextAreaStyle(errorArea);
        errorArea.setVisible(false);

        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);

        add(retryButton, constraints);
        retryButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.RETRY"));
        retryButton.setVisible(false);

        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);

        add(cancelButton, constraints);
        cancelButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.CANCEL"));
        cancelButton.addActionListener(this);
        cancelButton.setVisible(false);

        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(0, 5, 0, 5);

        constraints.gridx = 3;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.LINE_END;
        constraints.insets = new Insets(0, 5, 0, 5);

        add(progressPropertiesPanel, constraints);

        estimatedTimeLabel.setFont(
            estimatedTimeLabel.getFont().deriveFont(11f));
        estimatedTimeLabel.setVisible(false);
        progressSpeedLabel.setFont(
            progressSpeedLabel.getFont().deriveFont(11f));
        progressSpeedLabel.setVisible(false);

        progressPropertiesPanel.add(progressSpeedLabel);
        progressPropertiesPanel.add(estimatedTimeLabel);

        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.fill = GridBagConstraints.NONE;

        add(acceptButton, constraints);
        acceptButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.ACCEPT"));
        acceptButton.setVisible(false);

        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.fill = GridBagConstraints.NONE;

        add(rejectButton, constraints);
        rejectButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.REJECT"));
        rejectButton.setVisible(false);

        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.fill = GridBagConstraints.NONE;

        add(openFileButton, constraints);
        openFileButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.OPEN"));
        openFileButton.setVisible(false);
        openFileButton.addActionListener(this);

        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.fill = GridBagConstraints.NONE;

        add(openFolderButton, constraints);
        openFolderButton.setText(
            GuiActivator.getResources().getI18NString(
                "service.gui.OPEN_FOLDER"));
        openFolderButton.setVisible(false);
        openFolderButton.addActionListener(this);

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.ipadx = 150;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        add(progressBar, constraints);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
    }

    /**
     * Sets a custom style for the given text area.
     *
     * @param textArea the text area to style
     */
    private void setTextAreaStyle(JTextArea textArea)
    {
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
    }

    /**
     * Shows the given error message in the error area of this component.
     *
     * @param message the message to show
     */
    @Override
    protected void showErrorMessage(String message)
    {
        errorArea.setText(message);
        errorIconLabel.setVisible(true);
        errorArea.setVisible(true);
    }

    /**
     * Sets the download file.
     *
     * @param file the file that has been downloaded or sent
     */
    protected void setCompletedDownloadFile(File file)
    {
        this.downloadFile = file;

        imageLabel.setFile(downloadFile);

        imageLabel.setToolTipText(
            resources.getI18NString("service.gui.OPEN_FILE_FROM_IMAGE"));

        imageLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() > 1)
                {
                    openFile(downloadFile);
                }
            }
        });
    }

    /**
     * Sets the file transfer.
     *
     * @param fileTransfer the file transfer
     * @param transferredFileSize the size of the transferred file
     */
    protected void setFileTransfer( FileTransfer fileTransfer,
                                    long transferredFileSize)
    {
        this.fileTransfer = fileTransfer;
        this.transferredFileSize = transferredFileSize;

        fileTransfer.addProgressListener(this);
    }

    /**
     * Handles buttons action events.
     *
     * @param evt the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton sourceButton = (JButton) evt.getSource();

        if (sourceButton.equals(openFileButton))
        {
            this.openFile(downloadFile);
        }
        else if (sourceButton.equals(openFolderButton))
        {
            try
            {
                File downloadDir = GuiActivator.getFileAccessService()
                    .getDefaultDownloadDirectory();

                GuiActivator.getDesktopService().open(downloadDir);
            }
            catch (IllegalArgumentException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_DOES_NOT_EXIST"));
            }
            catch (NullPointerException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_DOES_NOT_EXIST"));
            }
            catch (UnsupportedOperationException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FILE_OPEN_NOT_SUPPORTED"));
            }
            catch (SecurityException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_OPEN_NO_PERMISSION"));
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_OPEN_NO_APPLICATION"));
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Unable to open file.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_OPEN_FAILED"));
            }
        }
        else if (sourceButton.equals(cancelButton))
        {
            if (fileTransfer != null)
                fileTransfer.cancel();
        }
    }

    /**
     * Updates progress bar progress line every time a progress event has been
     * received.
     *
     * @param event the <tt>FileTransferProgressEvent</tt> that notified us
     */
    public void progressChanged(FileTransferProgressEvent event)
    {
        progressBar.setValue((int)event.getProgress());

        long transferredBytes = event.getFileTransfer().getTransferedBytes();
        long progressTimestamp = event.getTimestamp();

        String bytesString = ByteFormat.format(transferredBytes);

        if ((progressTimestamp - lastSpeedTimestamp)
                >= SPEED_CALCULATE_DELAY)
        {
            lastProgressSpeed
                = Math.round(calculateProgressSpeed(transferredBytes));

            this.lastSpeedTimestamp = progressTimestamp;
            this.lastTransferredBytes = transferredBytes;
        }

        if ((progressTimestamp - lastEstimatedTimeTimestamp)
                >= SPEED_CALCULATE_DELAY
            && lastProgressSpeed > 0)
        {
            lastEstimatedTime = Math.round(calculateEstimatedTransferTime(
                lastProgressSpeed,
                transferredFileSize - transferredBytes));

            lastEstimatedTimeTimestamp = progressTimestamp;
        }

        progressBar.setString(getProgressLabel(bytesString));

        if (lastProgressSpeed > 0)
        {
            progressSpeedLabel.setText(
                resources.getI18NString("service.gui.SPEED")
                + ByteFormat.format(lastProgressSpeed) + "/sec");
            progressSpeedLabel.setVisible(true);
        }

        if (lastEstimatedTime > 0)
        {
            estimatedTimeLabel.setText(
                resources.getI18NString("service.gui.ESTIMATED_TIME")
                + GuiUtils.formatSeconds(lastEstimatedTime*1000));
            estimatedTimeLabel.setVisible(true);
        }
    }

    /**
     * Returns the string, showing information for the given file.
     *
     *
     * @param file the file
     * @return the name of the given file
     */
    protected String getFileLabel(File file)
    {
        String fileName = file.getName();
        long fileSize = file.length();

        String text = ByteFormat.format(fileSize);

        return fileName + " (" + text + ")";
    }

    /**
     * Returns the string, showing information for the given file.
     *
     * @param fileName the name of the file
     * @param fileSize the size of the file
     * @return the name of the given file
     */
    protected String getFileLabel(String fileName, long fileSize)
    {
        String text = ByteFormat.format(fileSize);

        return fileName + " (" + text + ")";
    }

    /**
     * Hides all progress related components.
     */
    protected void hideProgressRelatedComponents()
    {
        progressBar.setVisible(false);
        progressSpeedLabel.setVisible(false);
        estimatedTimeLabel.setVisible(false);
    }

    /**
     * Returns the label to show on the progress bar.
     *
     * @param bytesString the bytes that have been transfered
     * @return the label to show on the progress bar
     */
    protected abstract String getProgressLabel(String bytesString);

    /**
     * Returns the speed of the transfer.
     *
     * @param transferredBytes the number of bytes that have been transferred
     * @return the speed of the transfer
     */
    private double calculateProgressSpeed(long transferredBytes)
    {
        // Bytes per second = bytes / SPEED_CALCULATE_DELAY miliseconds * 1000.
        return (transferredBytes - lastTransferredBytes)
                / SPEED_CALCULATE_DELAY * 1000;
    }

    /**
     * Returns the estimated transfer time left.
     *
     * @param speed the speed of the transfer
     * @param bytesLeft the size of the file
     * @return the estimated transfer time left
     */
    private double calculateEstimatedTransferTime(double speed, long bytesLeft)
    {
        return bytesLeft / speed;
    }

    /**
     * Reload images and colors.
     */
    @Override
    public void loadSkin()
    {
        errorIconLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.EXCLAMATION_MARK)));

        if(downloadFile != null)
            imageLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.DEFAULT_FILE_ICON)));

        errorArea.setForeground(
            new Color(resources.getColor("service.gui.ERROR_FOREGROUND")));
    }
}
