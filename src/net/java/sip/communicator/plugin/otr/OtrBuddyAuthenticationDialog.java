/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * @author George Politis
 */
@SuppressWarnings("serial")
public class OtrBuddyAuthenticationDialog
    extends SIPCommDialog
    implements DocumentListener
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
        super(false);
        this.contact = contact;

        initComponents();
        loadContact();
    }

    private SIPCommTextField txtRemoteFingerprintComparison;

    private JTextArea txtLocalFingerprint;

    private JTextArea txtRemoteFingerprint;

    private JComboBox cbAction;
    ActionComboBoxItem actionIHave =
        new ActionComboBoxItem(ActionComboBoxItemIndex.I_HAVE);
    ActionComboBoxItem actionIHaveNot =
        new ActionComboBoxItem(ActionComboBoxItemIndex.I_HAVE_NOT);

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
            this.setBackground(new Color(0,0,0,0));
            this.setOpaque(false);
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

        TransparentPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setPreferredSize(new Dimension(350, 400));

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
        cbAction.addItem(actionIHave);
        cbAction.addItem(actionIHaveNot);
        cbAction.setSelectedItem(OtrActivator.scOtrKeyManager
            .isVerified(contact) ? actionIHave : actionIHaveNot);

        pnlAction.add(cbAction, c);

        txtAction = new CustomTextArea();
        c.weightx = 1.0;
        pnlAction.add(txtAction, c);

        txtRemoteFingerprintComparison = new SIPCommTextField(
            OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.FINGERPRINT_CHECK",
                new String[]{contact.getDisplayName()}));
        txtRemoteFingerprintComparison.getDocument().addDocumentListener(this);

        c.gridwidth = 2;
        c.gridy = 1;
        pnlAction.add(txtRemoteFingerprintComparison, c);
        c.gridwidth = 1;
        c.gridy = 0;

        // Buttons panel.
        JPanel buttonPanel = new TransparentPanel(new GridBagLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

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

        this.getContentPane().add(mainPanel, BorderLayout.NORTH);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        this.pack();
    }

    public void removeUpdate(DocumentEvent e)
    {
        compareFingerprints();
    }

    public void insertUpdate(DocumentEvent e)
    {
        compareFingerprints();
    }

    public void changedUpdate(DocumentEvent e)
    {
        compareFingerprints();
    }

    public void compareFingerprints()
    {
        if(txtRemoteFingerprintComparison.getText() == null
            || txtRemoteFingerprintComparison.getText().length() == 0)
        {
            txtRemoteFingerprintComparison.setBackground(Color.white);
            return;
        }
        if(txtRemoteFingerprintComparison.getText().contains(
            OtrActivator.scOtrKeyManager.getRemoteFingerprint(contact)))
        {
            txtRemoteFingerprintComparison.setBackground(Color.green);
            cbAction.setSelectedItem(actionIHave);
        }
        else
        {
            txtRemoteFingerprintComparison.setBackground(
                new Color(243, 72, 48));
            cbAction.setSelectedItem(actionIHaveNot);
        }
    }
}
