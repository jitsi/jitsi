/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.message;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
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
    private Logger logger = Logger.getLogger(NewStatusMessageDialog.class);

    private JLabel messageLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.NEW_STATUS_MESSAGE"));

    private JTextField messageTextField = new JTextField();

    private JPanel dataPanel = new TransparentPanel(new BorderLayout(5, 5));

    private JTextArea infoArea = new JTextArea(
        GuiActivator.getResources().getI18NString(
            "service.gui.STATUS_MESSAGE_INFO"));

    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.NEW_STATUS_MESSAGE"));

    private JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));

    private JButton okButton
        = new JButton(GuiActivator.getResources().getI18NString("service.gui.OK"));

    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private JPanel buttonsPanel = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private ProtocolProviderService protocolProvider;

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
        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.NEW_STATUS_MESSAGE"));

        this.getRootPane().setDefaultButton(okButton);

        this.setPreferredSize(new Dimension(500, 200));

        this.infoArea.setEditable(false);
        this.infoArea.setLineWrap(true);
        this.infoArea.setWrapStyleWord(true);
        this.infoArea.setOpaque(false);

        this.dataPanel.add(messageLabel, BorderLayout.WEST);

        this.dataPanel.add(messageTextField, BorderLayout.CENTER);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(
            infoTitleLabel.getFont().deriveFont(Font.BOLD, 18.0f));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoArea);
        this.labelsPanel.add(dataPanel);

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
            .add(new ImageCanvas(ImageLoader
                .getImage(ImageLoader.RENAME_DIALOG_ICON)),
                messagePanelConstraints);
        messagePanelConstraints.anchor = GridBagConstraints.NORTH;
        messagePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        messagePanelConstraints.gridx = 1;
        messagePanelConstraints.insets = new Insets(0, 0, 0, 0);
        messagePanelConstraints.weightx = 1;
        messagePanel.add(labelsPanel, messagePanelConstraints);

        this.okButton.setName("ok");
        this.cancelButton.setName("cancel");

        this.okButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.OK"));
        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.okButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.buttonsPanel.add(okButton);
        this.buttonsPanel.add(cancelButton);

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
