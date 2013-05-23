/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.presence;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>NewStatusMessageDialog</tt> is the dialog containing the form for
 * changing the status message for a protocol provider.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class NewStatusMessageDialog
    extends SIPCommDialog
    implements  ActionListener,
                Skinnable
{
    /**
     * The icon.
     */
    private final static String RENAME_DIALOG_ICON =
        "service.gui.icons.RENAME_DIALOG_ICON";

    /**
     * The field, containing the status message.
     */
    private final JTextField messageTextField = new JTextField();

    /**
     * The button, used to cancel this dialog.
     */
    private final JButton cancelButton = new JButton(
        DesktopUtilActivator.getResources().getI18NString("service.gui.CANCEL"));

    /**
     * The presence operation set through which we change the status message.
     */
    private final OperationSetPresence presenceOpSet;

    /**
     * Message panel.
     */
    private JPanel messagePanel;

    /**
     * The parent menu.
     */
    private StatusMessageMenu parentMenu;

    /**
     * A checkbox when checked, the new message will be saved.
     */
    private JCheckBox saveNewMessage;

    /**
     * Creates an instance of <tt>NewStatusMessageDialog</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>.
     */
    public NewStatusMessageDialog (ProtocolProviderService protocolProvider,
                                   StatusMessageMenu parentMenu)
    {
        super(false);

        presenceOpSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);
        this.parentMenu = parentMenu;

        this.init();
        pack();
    }

    /**
     * Initializes the <tt>NewStatusMessageDialog</tt> by adding the buttons,
     * fields, etc.
     */
    private void init()
    {
        JLabel messageLabel = new JLabel(
            DesktopUtilActivator.getResources().getI18NString(
                "service.gui.NEW_STATUS_MESSAGE"));

        JPanel dataPanel
            = new TransparentPanel(new BorderLayout(5, 5));

        JTextArea infoArea = new JTextArea(
            DesktopUtilActivator.getResources().getI18NString(
                "service.gui.STATUS_MESSAGE_INFO"));

        JLabel infoTitleLabel = new JLabel(
            DesktopUtilActivator.getResources().getI18NString(
                "service.gui.NEW_STATUS_MESSAGE"));

        JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));

        JButton okButton = new JButton(
            DesktopUtilActivator.getResources().getI18NString(
                "service.gui.OK"));

        JPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        saveNewMessage = new JCheckBox(
            DesktopUtilActivator.getResources().getI18NString(
                "service.gui.NEW_STATUS_MESSAGE_SAVE"));

        this.setTitle(DesktopUtilActivator.getResources()
                .getI18NString("service.gui.NEW_STATUS_MESSAGE"));

        this.getRootPane().setDefaultButton(okButton);

        this.setPreferredSize(new Dimension(550, 200));

        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setOpaque(false);

        dataPanel.add(messageLabel, BorderLayout.WEST);

        messageTextField.setText(presenceOpSet.getCurrentStatusMessage());
        dataPanel.add(messageTextField, BorderLayout.CENTER);

        infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        infoTitleLabel.setFont(
            infoTitleLabel.getFont().deriveFont(Font.BOLD, 18.0f));

        saveNewMessage.setSelected(true);
        JPanel saveToCustomPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.RIGHT));
        saveToCustomPanel.add(saveNewMessage);

        labelsPanel.add(infoTitleLabel);
        labelsPanel.add(infoArea);
        labelsPanel.add(dataPanel);
        labelsPanel.add(saveToCustomPanel);

        messagePanel = new TransparentPanel(new GridBagLayout());
        GridBagConstraints messagePanelConstraints = new GridBagConstraints();
        messagePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        messagePanelConstraints.fill = GridBagConstraints.NONE;
        messagePanelConstraints.gridx = 0;
        messagePanelConstraints.gridy = 0;
        messagePanelConstraints.insets = new Insets(5, 0, 5, 10);
        messagePanelConstraints.weightx = 0;
        messagePanelConstraints.weighty = 0;
        messagePanel
            .add(new ImageCanvas(
                        DesktopUtilActivator.getImage(RENAME_DIALOG_ICON)),
                 messagePanelConstraints);

        messagePanelConstraints.anchor = GridBagConstraints.NORTH;
        messagePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        messagePanelConstraints.gridx = 1;
        messagePanelConstraints.insets = new Insets(0, 0, 0, 0);
        messagePanelConstraints.weightx = 1;
        messagePanel.add(labelsPanel, messagePanelConstraints);

        okButton.setName("ok");
        cancelButton.setName("cancel");

        okButton.setMnemonic(
            DesktopUtilActivator.getResources().getI18nMnemonic("service.gui.OK"));
        cancelButton.setMnemonic(
            DesktopUtilActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

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
     * @param e the event that notified us of the action
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("ok"))
        {
            parentMenu.publishStatusMessage(messageTextField.getText(),
                                            parentMenu.getNewMessageItem(),
                                            saveNewMessage.isSelected());
        }

        this.dispose();
    }

    /**
     * Requests the focus in the text field contained in this
     * dialog.
     */
    public void requestFocusInField()
    {
        this.messageTextField.requestFocus();
    }

    /**
     * Artificially clicks the cancel button when this panel is escaped.
     * @param isEscaped indicates if this dialog is closed by the Esc shortcut
     */
    @Override
    protected void close(boolean isEscaped)
    {
        if (isEscaped)
            cancelButton.doClick();
    }

    /**
     * Reloads icon.
     */
    public void loadSkin()
    {
        if(messagePanel != null)
        {
            for(Component component : messagePanel.getComponents())
            {
                if(component instanceof ImageCanvas)
                {
                    ImageCanvas cmp = (ImageCanvas)component;
                    cmp.setImage(
                            DesktopUtilActivator.getImage(RENAME_DIALOG_ICON));
                }
            }
        }
    }
}
