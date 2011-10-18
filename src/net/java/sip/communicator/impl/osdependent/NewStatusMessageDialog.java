/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>NewStatusMessageDialog</tt> is the dialog containing the form for
 * changing the status message.
 *
 * @author Yana Stamcheva
 */
public class NewStatusMessageDialog
    extends SIPCommDialog
    implements ActionListener
{
    private final Logger logger = Logger.getLogger(NewStatusMessageDialog.class);

    private final JTextField messageTextField = new JTextField();

    private final JButton cancelButton = new JButton(
        OsDependentActivator.getResources().getI18NString("service.gui.CANCEL"));

    private final ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>NewStatusMessageDialog</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>.
     */
    public NewStatusMessageDialog (ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;

        this.init();
        pack();
    }

    /**
     * Initializes the <tt>NewStatusMessageDialog</tt> by adding the buttons,
     * fields, etc.
     */
    private void init()
    {
        this.setTitle(OsDependentActivator.getResources()
                .getI18NString("service.gui.NEW_STATUS_MESSAGE"));

        JButton okButton =
            new JButton(OsDependentActivator.getResources()
                    .getI18NString("service.gui.OK"));
        this.getRootPane().setDefaultButton(okButton);

        this.setPreferredSize(new Dimension(500, 200));

        JTextArea infoArea = new JTextArea(
                OsDependentActivator.getResources().getI18NString(
                    "service.gui.STATUS_MESSAGE_INFO"));
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setOpaque(false);

        JLabel messageLabel = new JLabel(
                OsDependentActivator.getResources().getI18NString(
                    "service.gui.NEW_STATUS_MESSAGE"));

        JPanel dataPanel = new TransparentPanel(new BorderLayout(5, 5));
        dataPanel.add(messageLabel, BorderLayout.WEST);
        dataPanel.add(messageTextField, BorderLayout.CENTER);

        JLabel infoTitleLabel = new JLabel(
                OsDependentActivator.getResources().getI18NString(
                    "service.gui.NEW_STATUS_MESSAGE"));
        infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        infoTitleLabel.setFont(
            infoTitleLabel.getFont().deriveFont(Font.BOLD, 18.0f));

        JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));
        labelsPanel.add(infoTitleLabel);
        labelsPanel.add(infoArea);
        labelsPanel.add(dataPanel);

        JPanel messagePanel = new TransparentPanel(new GridBagLayout());
        GridBagConstraints messagePanelConstraints = new GridBagConstraints();
        messagePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        messagePanelConstraints.fill = GridBagConstraints.NONE;
        messagePanelConstraints.gridx = 0;
        messagePanelConstraints.gridy = 0;
        messagePanelConstraints.insets = new Insets(5, 0, 5, 10);
        messagePanelConstraints.weightx = 0;
        messagePanelConstraints.weighty = 0;
        messagePanel
            .add(new ImageCanvas(Resources
                .getImage("service.gui.icons.RENAME_DIALOG_ICON").getImage()),
                messagePanelConstraints);
        messagePanelConstraints.anchor = GridBagConstraints.NORTH;
        messagePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        messagePanelConstraints.gridx = 1;
        messagePanelConstraints.insets = new Insets(0, 0, 0, 0);
        messagePanelConstraints.weightx = 1;
        messagePanel.add(labelsPanel, messagePanelConstraints);

        okButton.setName("ok");
        this.cancelButton.setName("cancel");

        okButton.setMnemonic(
            OsDependentActivator.getResources()
                .getI18nMnemonic("service.gui.OK"));
        this.cancelButton.setMnemonic(
            OsDependentActivator.getResources()
                .getI18nMnemonic("service.gui.CANCEL"));

        okButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        JPanel buttonsPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        JPanel mainPanel = new TransparentPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.anchor = GridBagConstraints.NORTH;
        mainPanelConstraints.fill = GridBagConstraints.BOTH;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.weightx = 1;
        mainPanelConstraints.weighty = 1;
        mainPanel.add(messagePanel, mainPanelConstraints);
        mainPanelConstraints.anchor = GridBagConstraints.SOUTHEAST;
        mainPanelConstraints.fill = GridBagConstraints.NONE;
        mainPanelConstraints.gridy = 1;
        mainPanelConstraints.weightx = 0;
        mainPanelConstraints.weighty = 0;
        mainPanel.add(buttonsPanel, mainPanelConstraints);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Handles the <tt>ActionEvent</tt>. In order to change the status message
     * with the new one calls the <tt>PublishStatusMessageThread</tt>.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("ok"))
        {
            new PublishStatusMessageThread(messageTextField.getText()).start();
        }

        this.dispose();
    }

    /**
     * Requests the focus in the text field contained in this
     * dialog.
     */
    public void requestFocusInFiled()
    {
        this.messageTextField.requestFocus();
    }

    /**
     *  This class allow to use a thread to change the presence status message.
     */
    private class PublishStatusMessageThread extends Thread
    {
        private String message;

        private PresenceStatus currentStatus;

        private OperationSetPresence presenceOpSet;

        public PublishStatusMessageThread(String message)
        {
            this.message = message;

            presenceOpSet
                = (OperationSetPersistentPresence) protocolProvider
                    .getOperationSet(OperationSetPresence.class);

            this.currentStatus = presenceOpSet.getPresenceStatus();
        }

        public void run()
        {
            try
            {
                presenceOpSet.publishPresenceStatus(currentStatus, message);
            }
            catch (IllegalArgumentException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (IllegalStateException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (OperationFailedException e1)
            {

                if (e1.getErrorCode()
                    == OperationFailedException.GENERAL_ERROR)
                {
                    logger.error(
                        "General error occured while "
                        + "publishing presence status.",
                        e1);
                }
                else if (e1.getErrorCode()
                        == OperationFailedException
                            .NETWORK_FAILURE)
                {
                    logger.error(
                        "Network failure occured while "
                        + "publishing presence status.",
                        e1);
                }
                else if (e1.getErrorCode()
                        == OperationFailedException
                            .PROVIDER_NOT_REGISTERED)
                {
                    logger.error(
                        "Protocol provider must be"
                        + "registered in order to change status.",
                        e1);
                }
            }
        }
    }

    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }
}
