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

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>NewStatusMessageDialog</tt> is the dialog containing the form for
 * changing the status message. 
 * 
 * @author Yana Stamcheva
 */
public class NewStatusMessageDialog
    extends JDialog
    implements ActionListener
{
    private Logger logger = Logger.getLogger(NewStatusMessageDialog.class);

    private JPanel messagePanel = new JPanel(new BorderLayout());

    private JLabel messageLabel = new JLabel(
        Messages.getI18NString("newStatusMessage").getText());

    private JTextField messageTextField = new JTextField();

    private JPanel dataPanel = new JPanel(new BorderLayout(5, 5));

    private JTextArea infoArea = new JTextArea(
        Messages.getI18NString("statusMessageInfo").getText());

    private JLabel infoTitleLabel = new JLabel(
        Messages.getI18NString("newStatusMessage").getText());

    private JLabel iconLabel = new JLabel(new ImageIcon(
        ImageLoader.getImage(ImageLoader.RENAME_DIALOG_ICON)));

    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));

    private JPanel rightPanel = new JPanel(new BorderLayout());

    private JButton okButton
        = new JButton(Messages.getI18NString("ok").getText());

    private JButton cancelButton
        = new JButton(Messages.getI18NString("cancel").getText());

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private JPanel mainPanel = new JPanel(new BorderLayout());

    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>NewStatusMessageDialog</tt>.
     * 
     * @param protocolProvider the <tt>ProtocolProviderService</tt>.
     */
    public NewStatusMessageDialog (ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;

        this.setSize(new Dimension(520, 270));

        this.init();
    }
    
    /**
     * Initializes the <tt>NewStatusMessageDialog</tt> by adding the buttons,
     * fields, etc.
     */
    private void init()
    {
        this.setTitle(Messages.getI18NString("newStatusMessage").getText());

        this.getRootPane().setDefaultButton(okButton);

        this.setPreferredSize(new Dimension(500, 200));

        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));

        this.infoArea.setEditable(false);
        this.infoArea.setLineWrap(true);
        this.infoArea.setWrapStyleWord(true);

        this.dataPanel.add(messageLabel, BorderLayout.WEST);

        this.dataPanel.add(messageTextField, BorderLayout.CENTER);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(
            infoTitleLabel.getFont().deriveFont(Font.BOLD, 18.0f));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoArea);
        this.labelsPanel.add(dataPanel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);

        this.messagePanel.add(iconLabel, BorderLayout.WEST);
        this.messagePanel.add(rightPanel, BorderLayout.CENTER);

        this.okButton.setName("ok");
        this.cancelButton.setName("cancel");

        this.okButton.setMnemonic(
            Messages.getI18NString("ok").getMnemonic());
        this.cancelButton.setMnemonic(
            Messages.getI18NString("cancel").getMnemonic());

        this.okButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.buttonsPanel.add(okButton);
        this.buttonsPanel.add(cancelButton);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        this.mainPanel.add(messagePanel, BorderLayout.NORTH);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

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
}
