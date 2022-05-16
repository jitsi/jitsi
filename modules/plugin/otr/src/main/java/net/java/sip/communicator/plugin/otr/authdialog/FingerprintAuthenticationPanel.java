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
package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;
import java.security.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;

/**
 * @author George Politis
 * @author Marin Dzhigarov
 */
@SuppressWarnings("serial")
public class FingerprintAuthenticationPanel
    extends TransparentPanel
    implements DocumentListener
{

    /**
     * The Contact that we are authenticating.
     */
    private final OtrContact otrContact;

    private SIPCommTextField txtRemoteFingerprintComparison;

    /**
     * Our fingerprint.
     */
    private JTextArea txtLocalFingerprint;

    /**
     * The purported fingerprint of the remote party.
     */
    private JTextArea txtRemoteFingerprint;

    /**
     * The "I have" / "I have not" combo box.
     */
    private JComboBox cbAction;

    private ActionComboBoxItem actionIHave =
        new ActionComboBoxItem(ActionComboBoxItemIndex.I_HAVE);

    private ActionComboBoxItem actionIHaveNot =
        new ActionComboBoxItem(ActionComboBoxItemIndex.I_HAVE_NOT);

    private JTextArea txtAction;

    /**
     * Creates an instance FingerprintAuthenticationPanel
     * 
     * @param contact The contact that this panel refers to.
     */
    FingerprintAuthenticationPanel(OtrContact contact)
    {
        this.otrContact = contact;
        initComponents();
        loadContact();
        
    }

    /**
     * Initializes the {@link FingerprintAuthenticationPanel} components.
     */
    private void initComponents()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(350, 300));

        JTextArea generalInformation = new CustomTextArea();
        generalInformation.setText(OtrActivator.resourceService
            .getI18NString(
                "plugin.otr.authbuddydialog.AUTHENTICATION_FINGERPRINT"));
        this.add(generalInformation);

        add(Box.createVerticalStrut(10));

        txtLocalFingerprint = new CustomTextArea();
        this.add(txtLocalFingerprint);

        add(Box.createVerticalStrut(10));

        txtRemoteFingerprint = new CustomTextArea();
        this.add(txtRemoteFingerprint);

        add(Box.createVerticalStrut(10));

        // Action Panel (the panel that holds the I have/I have not dropdown)
        JPanel pnlAction = new JPanel(new GridBagLayout());
        pnlAction.setBorder(BorderFactory.createEtchedBorder());
        this.add(pnlAction);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 0.0;

        cbAction = new JComboBox();
        cbAction.addItem(actionIHave);
        cbAction.addItem(actionIHaveNot);

        PublicKey pubKey = OtrActivator.scOtrEngine.getRemotePublicKey(otrContact);
        String remoteFingerprint =
            OtrActivator.scOtrKeyManager.getFingerprintFromPublicKey(pubKey);
        cbAction.setSelectedItem(OtrActivator.scOtrKeyManager
            .isVerified(otrContact.contact, remoteFingerprint)
                ? actionIHave : actionIHaveNot);

        pnlAction.add(cbAction, c);

        txtAction = new CustomTextArea();
        c.weightx = 1.0;
        pnlAction.add(txtAction, c);

        String resourceName = otrContact.resource != null ?
            "/" + otrContact.resource.getResourceName() : "";

            txtRemoteFingerprintComparison = new SIPCommTextField(
            OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.FINGERPRINT_CHECK",
                new String[]
                    {otrContact.contact.getDisplayName() + resourceName}));
        txtRemoteFingerprintComparison.getDocument().addDocumentListener(this);

        c.gridwidth = 2;
        c.gridy = 1;
        pnlAction.add(txtRemoteFingerprintComparison, c);
        c.gridwidth = 1;
        c.gridy = 0;
    }

    public JComboBox getCbAction()
    {
        return cbAction;
    }

    /**
     * Sets up the {@link OtrBuddyAuthenticationDialog} components so that they
     * reflect the {@link OtrBuddyAuthenticationDialog#otrContact}
     */
    private void loadContact()
    {
        // Local fingerprint.
        String account =
            otrContact.contact.getProtocolProvider().getAccountID().getDisplayName();
        String localFingerprint =
            OtrActivator.scOtrKeyManager.getLocalFingerprint(otrContact.contact
                .getProtocolProvider().getAccountID());
        txtLocalFingerprint.setText(OtrActivator.resourceService.getI18NString(
            "plugin.otr.authbuddydialog.LOCAL_FINGERPRINT", new String[]
            { account, localFingerprint }));

        // Remote fingerprint.
        String user = otrContact.contact.getDisplayName();
        PublicKey pubKey = OtrActivator.scOtrEngine.getRemotePublicKey(otrContact);
        String remoteFingerprint =
            OtrActivator.scOtrKeyManager.getFingerprintFromPublicKey(pubKey);
        txtRemoteFingerprint.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.authbuddydialog.REMOTE_FINGERPRINT",
                new String[]
                { user, remoteFingerprint }));

        // Action
        txtAction.setText(OtrActivator.resourceService.getI18NString(
            "plugin.otr.authbuddydialog.VERIFY_ACTION", new String[]
            { user }));
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
        PublicKey pubKey = OtrActivator.scOtrEngine.getRemotePublicKey(otrContact);
        String remoteFingerprint =
            OtrActivator.scOtrKeyManager.getFingerprintFromPublicKey(pubKey);

        if(txtRemoteFingerprintComparison.getText() == null
            || txtRemoteFingerprintComparison.getText().length() == 0)
        {
            txtRemoteFingerprintComparison.setBackground(Color.white);
            return;
        }
        if(txtRemoteFingerprintComparison.getText().toLowerCase().contains(
            remoteFingerprint.toLowerCase()))
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

        @Override
        public String toString()
        {
            return text;
        }
    }
}
