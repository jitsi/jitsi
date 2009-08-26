package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;

@SuppressWarnings("serial")
public class OtrBuddyAuthenticationDialog
    extends JDialog
{
    private Contact contact;

    public OtrBuddyAuthenticationDialog(Contact contact)
    {
        this.contact = contact;

        initComponents();
        loadContact();
    }

    JTextArea txtLocalFingerprint;

    JTextArea txtRemoteFingerprint;

    private void loadContact()
    {
        // Local fingerprint.
        String account =
            contact.getProtocolProvider().getAccountID().getDisplayName();
        String localFingerprint =
            OtrActivator.scOtrEngine.getLocalFingerprint(contact
                .getProtocolProvider().getAccountID());
        txtLocalFingerprint.setText(OtrActivator.resourceService.getI18NString(
            "plugin.otr.authbuddydialog.LOCAL_FINGERPRINT", new String[]
            { account, localFingerprint }));

        // Remote fingerprint.
        String user = contact.getDisplayName();
        String remoteFingerprint =
            OtrActivator.scOtrEngine.getRemoteFingerprint(contact);
        txtRemoteFingerprint.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.REMOTE_FINGERPRINT",
                new String[]
                { user, remoteFingerprint }));
    }

    private void initComponents()
    {
        this.setTitle(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.TITLE"));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setPreferredSize(new Dimension(400, 300));

        JTextArea generalInformation = new JTextArea();
        generalInformation.setBackground(new java.awt.Color(212, 208, 200));
        generalInformation.setColumns(20);
        generalInformation.setEditable(false);
        generalInformation.setLineWrap(true);
        generalInformation.setWrapStyleWord(true);
        generalInformation.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.AUTHENTICATION_INFO"));
        mainPanel.add(generalInformation);

        txtLocalFingerprint = new JTextArea();
        txtLocalFingerprint.setBackground(new java.awt.Color(212, 208, 200));
        txtLocalFingerprint.setColumns(20);
        txtLocalFingerprint.setEditable(false);
        txtLocalFingerprint.setLineWrap(true);
        generalInformation.setWrapStyleWord(true);

        mainPanel.add(txtLocalFingerprint);

        txtRemoteFingerprint = new JTextArea();
        txtRemoteFingerprint.setBackground(new java.awt.Color(212, 208, 200));
        txtRemoteFingerprint.setColumns(20);
        txtRemoteFingerprint.setEditable(false);
        txtRemoteFingerprint.setLineWrap(true);
        generalInformation.setWrapStyleWord(true);

        mainPanel.add(txtRemoteFingerprint);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

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
        buttonPanel.add(helpButton);

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
        buttonPanel.add(cancelButton);

        JButton authenticateButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.AUTHENTICATE_BUDDY"));
        authenticateButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                OtrActivator.scOtrEngine.verifyContactFingerprint(contact);
                dispose();
            }
        });
        buttonPanel.add(authenticateButton);

        mainPanel.add(buttonPanel);

        this.getContentPane().add(mainPanel);
        this.pack();
    }
}
