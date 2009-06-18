/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.filetransfer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ReceiveFileConversationComponent</tt> is the component shown in the
 * conversation area of the chat window to display a incoming file transfer.
 * 
 * @author Yana Stamcheva
 */
public class ReceiveFileConversationComponent
    extends ChatConversationComponent
    implements  ActionListener,
                FileTransferStatusListener,
                FileTransferProgressListener
{
    private final Logger logger
        = Logger.getLogger(SendFileConversationComponent.class);

    private final FileDragLabel imageLabel = new FileDragLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel fileLabel = new JLabel();
    private final JTextArea errorArea = new JTextArea();
    private final JLabel errorIconLabel = new JLabel(
        new ImageIcon(ImageLoader.getImage(ImageLoader.EXCLAMATION_MARK)));

    private ChatConversationButton acceptButton = new ChatConversationButton();
    private ChatConversationButton rejectButton = new ChatConversationButton();
    private ChatConversationButton cancelButton = new ChatConversationButton();

    private ChatConversationButton openFileButton
        = new ChatConversationButton();
    private ChatConversationButton openFolderButton
        = new ChatConversationButton();

    private JProgressBar progressBar = new JProgressBar();

    private static final ResourceManagementService resources
        = GuiActivator.getResources();

    private IncomingFileTransferRequest fileTransferRequest;

    private FileTransfer fileTransfer;

    private File downloadFile;

    /**
     * Creates a <tt>ReceiveFileConversationComponent</tt>.
     * 
     * @param fileTransferRequest the <tt>IncomingFileTransferRequest</tt>
     * associated with this component
     */
    public ReceiveFileConversationComponent(
        IncomingFileTransferRequest fileTransferRequest)
    {
        this.fileTransferRequest = fileTransferRequest;

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
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);

        add(titleLabel, constraints);
        titleLabel.setText(resources.getI18NString(
            "service.gui.FILE_TRANSFER_REQUEST_RECIEVED",
            new String[]{fileTransferRequest.getSender().getDisplayName()}));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 5, 5);

        add(fileLabel, constraints);
        fileLabel.setText(fileTransferRequest.getFileName());

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.fill = GridBagConstraints.NONE;

        add(errorIconLabel, constraints);
        errorIconLabel.setVisible(false);

        constraints.gridx = 2;
        constraints.gridy = 2;
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
        constraints.fill = GridBagConstraints.NONE;

        add(acceptButton, constraints);
        acceptButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.ACCEPT"));
        acceptButton.addActionListener(this);

        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);

        add(rejectButton, constraints);
        rejectButton.setText(
            GuiActivator.getResources().getI18NString("service.gui.REJECT"));
        rejectButton.addActionListener(this);

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

        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);

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

        add(openFolderButton, constraints);
        openFolderButton.setText(
            GuiActivator.getResources().getI18NString(
                "service.gui.OPEN_FOLDER"));
        openFolderButton.setVisible(false);
        openFolderButton.addActionListener(this);

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.ipadx = 150;

        add(progressBar, constraints);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        progressBar.setMaximum((int)fileTransferRequest.getFileSize());
    }

    /**
     * Handles button actions.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton sourceButton = (JButton) evt.getSource();

        if (sourceButton.equals(acceptButton))
        {
            titleLabel.setText(resources
                .getI18NString("service.gui.FILE_TRANSFER_PREPARING",
                                new String[]{fileTransferRequest.getSender()
                                            .getDisplayName()}));
            acceptButton.setVisible(false);
            rejectButton.setVisible(false);
            cancelButton.setVisible(true);
            progressBar.setVisible(true);

            downloadFile = createFile();

            new AcceptFile().start();
        }
        else if (sourceButton.equals(rejectButton))
        {
            fileTransferRequest.rejectFile();

            acceptButton.setVisible(false);
            rejectButton.setVisible(false);
            fileLabel.setText("");
            titleLabel.setText(
                resources.getI18NString("service.gui.FILE_TRANSFER_CANCELED"));
        }
        else if (sourceButton.equals(cancelButton))
        {
            fileTransfer.cancel();
        }
        else if (sourceButton.equals(openFileButton))
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
                logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_DOES_NOT_EXIST"));
            }
            catch (NullPointerException e)
            {
                logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_DOES_NOT_EXIST"));
            }
            catch (UnsupportedOperationException e)
            {
                logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FILE_OPEN_NOT_SUPPORTED"));
            }
            catch (SecurityException e)
            {
                logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_OPEN_NO_PERMISSION"));
            }
            catch (IOException e)
            {
                logger.debug("Unable to open folder.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_OPEN_NO_APPLICATION"));
            }
            catch (Exception e)
            {
                logger.debug("Unable to open file.", e);

                this.showErrorMessage(
                    resources.getI18NString(
                        "service.gui.FOLDER_OPEN_FAILED"));
            }
        }
    }

    /**
     * Creates the file to download.
     * 
     * @return the file to download.
     */
    private File createFile()
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
                logger.debug("Download directory created : "
                        + downloadDir.getAbsolutePath());
            }
        }
        catch (IOException e)
        {
            logger.debug("Unable to find download directory.", e);
        }

        downloadFile = new File(downloadDir, incomingFileName);

        // If a file with the given name already exists, add an index to the
        // file name.
        int index = 0;
        while (downloadFile.exists())
        {
            String newFileName
             = incomingFileName.substring(0, incomingFileName.lastIndexOf("."))
                 + "-" + ++index
                 + incomingFileName.substring(incomingFileName.lastIndexOf("."));

            downloadFile = new File(downloadDir, newFileName);
        }

        fileLabel.setText(downloadFile.getName());

        return downloadFile;
    }

    /**
     * Handles status changes in file transfer.
     */
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        int status = event.getNewStatus();
        String fromContactName
            = fileTransferRequest.getSender().getDisplayName();

        if (status == FileTransfer.PREPARING)
        {
            progressBar.setVisible(false);
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_TRANSFER_PREPARING",
                new String[]{fromContactName}));
        }
        else if (status == FileTransfer.FAILED)
        {
            progressBar.setVisible(false);
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_RECEIVE_FAILED",
                new String[]{fromContactName}));

            setWarningStyle(true);
        }
        else if (status == FileTransfer.IN_PROGRESS)
        {
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_RECEIVING_FROM",
                new String[]{fromContactName}));
            setWarningStyle(false);

            if (!progressBar.isVisible())
            {
                progressBar.setVisible(true);
            }
        }
        else if (status == FileTransfer.COMPLETED)
        {
            if (downloadFile != null)
            {
                imageLabel.setFile(downloadFile);
                setFileIcon(downloadFile);
            }

            progressBar.setVisible(false);
            cancelButton.setVisible(false);
            openFileButton.setVisible(true);
            openFolderButton.setVisible(true);

            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_RECEIVE_COMPLETED",
                new String[]{fromContactName}));

            imageLabel.setToolTipText(
                resources.getI18NString("service.gui.OPEN_FILE_FROM_IMAGE"));
            imageLabel.addMouseListener(new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getClickCount() > 1)
                    {
                        openFile(downloadFile);
                    }
                }
            });
        }
        else if (status == FileTransfer.CANCELED)
        {
            progressBar.setVisible(false);
            cancelButton.setVisible(false);

            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_TRANSFER_CANCELED"));
            setWarningStyle(true);
        }
        else if (status == FileTransfer.REFUSED)
        {
            progressBar.setVisible(false);
            titleLabel.setText(resources.getI18NString(
                "service.gui.FILE_TRANSFER_REFUSED",
                new String[]{fromContactName}));
            cancelButton.setVisible(false);
            setWarningStyle(true);
        }
    }

    /**
     * Updates progress bar progress line every time a progress event has been
     * received.
     */
    public void progressChanged(FileTransferProgressEvent event)
    {
        progressBar.setValue((int) event.getProgress());

        ByteFormat format = new ByteFormat();
        String bytesSent = format.format(
            event.getFileTransfer().getTransferedBytes());
        progressBar.setString(bytesSent
            + " " + resources.getI18NString("service.gui.SENT"));
    }
    
    /**
     * Sets the icon for the given file.
     * 
     * @param file the file to set an icon for
     */
    private void setFileIcon(File file)
    {
        if (FileUtils.isImage(file.getName()))
        {
            try
            {
                ImageIcon image = new ImageIcon(file.toURI().toURL());
                image = ImageUtils
                    .getScaledRoundedIcon(image.getImage(), 64, 64);
                imageLabel.setIcon(image);
            }
            catch (MalformedURLException e)
            {
                logger.debug("Could not locate image.", e);
                imageLabel.setIcon(new ImageIcon(
                    ImageLoader.getImage(ImageLoader.DEFAULT_FILE_ICON)));
            }
        }
        else
        {
            Icon icon = FileUtils.getIcon(file);

            if (icon == null)
                icon = new ImageIcon(
                    ImageLoader.getImage(ImageLoader.DEFAULT_FILE_ICON));

            imageLabel.setIcon(icon);
        }
    }

    /**
     * Accepts the file in a new thread.
     */
    private class AcceptFile extends SwingWorker
    {
        public Object construct()
        {
            fileTransfer = fileTransferRequest.acceptFile(downloadFile);

            return "";
        }

        public void finished()
        {
            if (fileTransfer != null)
            {
                fileTransfer.addStatusListener(
                    ReceiveFileConversationComponent.this);
                fileTransfer.addProgressListener(
                    ReceiveFileConversationComponent.this);
            }
        }
    }

    /**
     * Shows the given error message in the error area of this component.
     * 
     * @param message the message to show
     */
    private void showErrorMessage(String message)
    {
        errorArea.setText(message);
        errorIconLabel.setVisible(true);
        errorArea.setVisible(true);
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
     * Opens the given file through the <tt>DesktopService</tt>.
     * 
     * @param downloadFile the file to open
     */
    private void openFile(File downloadFile)
    {
        try
        {
            GuiActivator.getDesktopService().open(downloadFile);
        }
        catch (IllegalArgumentException e)
        {
            logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_DOES_NOT_EXIST"));
        }
        catch (NullPointerException e)
        {
            logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_DOES_NOT_EXIST"));
        }
        catch (UnsupportedOperationException e)
        {
            logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_OPEN_NOT_SUPPORTED"));
        }
        catch (SecurityException e)
        {
            logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_OPEN_NO_PERMISSION"));
        }
        catch (IOException e)
        {
            logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_OPEN_NO_APPLICATION"));
        }
        catch (Exception e)
        {
            logger.debug("Unable to open file.", e);

            this.showErrorMessage(
                resources.getI18NString(
                    "service.gui.FILE_OPEN_FAILED"));
        }
    }
}
