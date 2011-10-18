/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author George Politis
 */
@SuppressWarnings("serial")
public class OtrBuddyAuthenticationDialog
    extends JDialog
{
    private final Contact contact;

    /**
     * The {@link OtrBuddyAuthenticationDialog} ctor.
     * 
     * @param contact The {@link Contact} this
     *            {@link OtrBuddyAuthenticationDialog} refers to.
     */
    public OtrBuddyAuthenticationDialog(Contact contact)
    {
        this.contact = contact;

        initComponents();
        loadContact();
    }

    private JTextArea txtLocalFingerprint;

    private JTextArea txtRemoteFingerprint;

    private JComboBox cbAction;

    private JTextArea txtAction;

    /**
     * Sets up the {@link OtrBuddyAuthenticationDialog} components so that they
     * reflect the {@link OtrBuddyAuthenticationDialog#contact}
     */
    private void loadContact()
    {
        // Local fingerprint.
        String account =
            contact.getProtocolProvider().getAccountID().getDisplayName();
        String localFingerprint =
            OtrActivator.scOtrKeyManager.getLocalFingerprint(contact
                .getProtocolProvider().getAccountID());
        txtLocalFingerprint.setText(OtrActivator.resourceService.getI18NString(
            "plugin.otr.authbuddydialog.LOCAL_FINGERPRINT", new String[]
            { account, localFingerprint }));

        // Remote fingerprint.
        String user = contact.getDisplayName();
        String remoteFingerprint =
            OtrActivator.scOtrKeyManager.getRemoteFingerprint(contact);
        txtRemoteFingerprint.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.REMOTE_FINGERPRINT",
                new String[]
                { user, remoteFingerprint }));

        // Action
        txtAction.setText(OtrActivator.resourceService.getI18NString(
            "plugin.otr.authbuddydialog.VERIFY_ACTION", new String[]
            { user }));
    }

    /**
     * A special {@link JTextArea} for use in the
     * {@link OtrBuddyAuthenticationDialog}. It is meant to be used for
     * fingerprint representation and general information display.
     * 
     * @author George Politis
     */
    class CustomTextArea
        extends JTextArea
    {
        public CustomTextArea()
        {
            this.setBackground(new java.awt.Color(212, 208, 200));
            this.setColumns(20);
            this.setEditable(false);
            this.setLineWrap(true);
            this.setWrapStyleWord(true);
        }
    }

    /**
     * A simple enumeration that is meant to be used with
     * {@link ActionComboBoxItem} to distinguish them (like an ID).
     * 
     * @author George Politis
     */
    enum ActionComboBoxItemIndex
    {
        I_HAVE, I_HAVE_NOT
    }

    /**
     * A special {@link JComboBox} that is hosted in
     * {@link OtrBuddyAuthenticationDialog#cbAction}.
     * 
     * @author George Politis
     */
    class ActionComboBoxItem
    {
        public ActionComboBoxItemIndex action;

        private String text;

        public ActionComboBoxItem(ActionComboBoxItemIndex actionIndex)
        {
            this.action = actionIndex;
            switch (action)
            {
            case I_HAVE:
                text =
                    OtrActivator.resourceService
                        .getI18NString("plugin.otr.authbuddydialog.I_HAVE");
                break;
            case I_HAVE_NOT:
                text =
                    OtrActivator.resourceService
                        .getI18NString("plugin.otr.authbuddydialog.I_HAVE_NOT");
                break;
            }
        }

        public String toString()
        {
            return text;
        }
    }

    /**
     * Initializes the {@link OtrBuddyAuthenticationDialog} components.
     */
    private void initComponents()
    {
        this.setTitle(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.TITLE"));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setPreferredSize(new Dimension(400, 300));

        JTextArea generalInformation = new CustomTextArea();
        generalInformation.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.AUTHENTICATION_INFO"));
        mainPanel.add(generalInformation);

        txtLocalFingerprint = new CustomTextArea();
        mainPanel.add(txtLocalFingerprint);

        txtRemoteFingerprint = new CustomTextArea();
        mainPanel.add(txtRemoteFingerprint);

        // Action Panel (the panel that holds the I have/I have not dropdown)
        JPanel pnlAction = new JPanel(new GridBagLayout());
        pnlAction.setBorder(BorderFactory.createEtchedBorder());
        mainPanel.add(pnlAction);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 0.0;

        cbAction = new JComboBox();

        ActionComboBoxItem iHave =
            new ActionComboBoxItem(ActionComboBoxItemIndex.I_HAVE);
        ActionComboBoxItem iHaveNot =
            new ActionComboBoxItem(ActionComboBoxItemIndex.I_HAVE_NOT);
        cbAction.addItem(iHave);
        cbAction.addItem(iHaveNot);
        cbAction.setSelectedItem(OtrActivator.scOtrKeyManager
            .isVerified(contact) ? iHave : iHaveNot);

        pnlAction.add(cbAction, c);

        txtAction = new CustomTextArea();
        c.weightx = 1.0;
        pnlAction.add(txtAction, c);

        // Buttons panel.
        JPanel buttonPanel = new JPanel(new GridBagLayout());

        JButton helpButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.HELP"));
        helpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                OtrActivator.scOtrEngine.launchHelp();
            }
        });

        c.weightx = 0.0;
        buttonPanel.add(helpButton, c);

        // Provide space between help and the other two button, not sure if this
        // is optimal..
        c.weightx = 1.0;
        buttonPanel.add(new JLabel(), c);
        c.weightx = 0.0;

        JButton cancelButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.CANCEL"));
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        buttonPanel.add(cancelButton, c);

        JButton authenticateButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.AUTHENTICATE_BUDDY"));
        authenticateButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ActionComboBoxItem actionItem =
                    (ActionComboBoxItem) cbAction.getSelectedItem();
                switch (actionItem.action)
                {
                case I_HAVE:
                    OtrActivator.scOtrKeyManager.verify(contact);
                    break;
                case I_HAVE_NOT:
                    OtrActivator.scOtrKeyManager.unverify(contact);
                    break;
                }

                dispose();
            }
        });
        buttonPanel.add(authenticateButton, c);

        mainPanel.add(buttonPanel);

        this.getContentPane().add(mainPanel);
        this.pack();
    }
}
