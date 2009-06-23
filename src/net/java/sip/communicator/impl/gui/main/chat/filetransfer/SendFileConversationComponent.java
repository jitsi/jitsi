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
import net.java.sip.communicator.util.*;

/**
 * The <tt>SendFileConversationComponent</tt> is the component added in the
 * chat conversation when user sends a file.
 * 
 * @author Yana Stamcheva
 */
public class SendFileConversationComponent
    extends ChatConversationComponent
    implements  ActionListener,
                FileTransferStatusListener,
                FileTransferProgressListener
{
    private final Logger logger
        = Logger.getLogger(SendFileConversationComponent.class);

    private final FileImageLabel imageLabel = new FileImageLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel fileLabel = new JLabel();
    private final JTextArea errorArea = new JTextArea();
    private final JLabel errorIconLabel = new JLabel(
        new ImageIcon(ImageLoader.getImage(ImageLoader.EXCLAMATION_MARK)));

    private ChatConversationButton cancelButton = new ChatConversationButton();
    private ChatConversationButton retryButton = new ChatConversationButton();

    private JProgressBar progressBar = new JProgressBar();

    private final String toContactName;

    private FileTransfer fileTransfer;

    private final ChatPanel parentChatPanel;

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

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 4;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);

        add(imageLabel, constraints);
        this.setFileIcon(file);
        imageLabel.setToolTipText(
            resources.getI18NString("service.gui.OPEN_FILE_FROM_IMAGE"));
        imageLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() > 1)
                {
                    openFile(file);
                }
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);

        add(titleLabel, constraints);
        titleLabel.setText(resources.getI18NString(
            "service.gui.FILE_WAITING_TO_ACCEPT",
            new String[]{toContactName}));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 5, 5);

        add(fileLabel, constraints);
        fileLabel.setText(getFileName(file));

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
        retryButton.addActionListener(this);
        retryButton.setVisible(false);

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 5, 0, 5);
        constraints.ipadx = 150;

        add(progressBar, constraints);
        progressBar.setMaximum((int) file.length());
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
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
        this.fileTransfer = fileTransfer;

        fileTransfer.addStatusListener(this);
        fileTransfer.addProgressListener(this);
    }

    /**
     * Returns the name of the given file.
     * 
     * @param file the file
     * @return the name of the given file
     */
    private String getFileName(File file)
    {
        String fileName = file.getName();
        long fileSize = file.length();
        ByteFormat format = new ByteFormat();
        String text = format.format(fileSize);

        return fileName + " (" + text + ")";
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
                imageLabel.setToolTipImage(image);

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
     * Handles button actions.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();

        if (sourceButton.equals(cancelButton))
        {
            if (fileTransfer != null)
                fileTransfer.cancel();
        }
        else if (sourceButton.equals(retryButton))
        {
            parentChatPanel.sendFile(file, this);
        }
    }

    /**
     * Shows the given error message in the error area of this component.
     * 
     * @param message the message to show
     */
    protected void showErrorMessage(String message)
    {
        errorArea.setText(message);
        errorIconLabel.setVisible(true);
        errorArea.setVisible(true);
    }
}
