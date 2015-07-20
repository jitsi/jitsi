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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author @George Politis
 * @author Yana Stamcheva
 */
public class KnownFingerprintsPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private JButton btnVerifyFingerprint;

    private JButton btnForgetFingerprint;

    private JTable contactsTable;

    /**
     * Constructor.
     */
    public KnownFingerprintsPanel()
    {
        this.initComponents();

        this.setPreferredSize(new Dimension(400, 200));

        openContact(getSelectedContact(), getSelectedFingerprint());
    }

    /**
     * Initializes the {@link KnownFingerprintsTableModel} components.
     */
    private void initComponents()
    {
        this.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            OtrActivator.resourceService
                .getI18NString("plugin.otr.configform.KNOWN_FINGERPRINTS")));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        contactsTable = new JTable();
        contactsTable.setModel(new KnownFingerprintsTableModel());
        contactsTable
            .setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        contactsTable.setCellSelectionEnabled(false);
        contactsTable.setColumnSelectionAllowed(false);
        contactsTable.setRowSelectionAllowed(true);
        contactsTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    if (e.getValueIsAdjusting())
                        return;

                    openContact(getSelectedContact(), getSelectedFingerprint());

                }
            });

        JScrollPane pnlContacts = new JScrollPane(contactsTable);
        this.add(pnlContacts);

        JPanel pnlButtons = new TransparentPanel();
        this.add(pnlButtons);

        btnVerifyFingerprint = new JButton();
        btnVerifyFingerprint.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.configform.VERIFY_FINGERPRINT"));
        btnVerifyFingerprint.setEnabled(false);

        btnVerifyFingerprint.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                OtrActivator.scOtrKeyManager
                    .verify(OtrContactManager.getOtrContact(
                        getSelectedContact(), null), getSelectedFingerprint());
                openContact(getSelectedContact(), getSelectedFingerprint());
                contactsTable.updateUI();
            }
        });

        pnlButtons.add(btnVerifyFingerprint);

        btnForgetFingerprint = new JButton();
        btnForgetFingerprint.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.configform.FORGET_FINGERPRINT"));
        btnForgetFingerprint.setEnabled(false);

        btnForgetFingerprint.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                OtrActivator.scOtrKeyManager
                    .unverify(OtrContactManager.getOtrContact(
                        getSelectedContact(), null), getSelectedFingerprint());
                openContact(getSelectedContact(), getSelectedFingerprint());
                contactsTable.updateUI();
            }
        });
        pnlButtons.add(btnForgetFingerprint);
    }

    /**
     * Gets the selected {@link Contact} for this
     * {@link KnownFingerprintsTableModel}.
     *
     * @return the selected {@link Contact}
     */
    private Contact getSelectedContact()
    {
        KnownFingerprintsTableModel model =
            (KnownFingerprintsTableModel) contactsTable.getModel();
        int index = contactsTable.getSelectedRow();
        if (index < 0 || index > model.getRowCount())
            return null;

        return model.getContactFromRow(index);
    }

    /**
     * Gets the selected fingerprint for this
     * {@link KnownFingerprintsTableModel}
     * 
     * return the selected fingerprint
     */
    private String getSelectedFingerprint()
    {
        KnownFingerprintsTableModel model =
            (KnownFingerprintsTableModel) contactsTable.getModel();
        int index = contactsTable.getSelectedRow();
        if (index < 0 || index > model.getRowCount())
            return null;

        return model.getFingerprintFromRow(index);
    }

    /**
     * Sets up the {@link KnownFingerprintsTableModel} components so that they
     * reflect the {@link Contact} param.
     *
     * @param contact the {@link Contact} to setup the components for.
     * @param fingerprint the fingerprint to setup the components for.
     */
    private void openContact(Contact contact, String fingerprint)
    {
        if (contact == null || fingerprint == null)
        {
            btnForgetFingerprint.setEnabled(false);
            btnVerifyFingerprint.setEnabled(false);
        }
        else
        {
            boolean verified
                = OtrActivator.scOtrKeyManager
                    .isVerified(contact, fingerprint);

            btnForgetFingerprint.setEnabled(verified);
            btnVerifyFingerprint.setEnabled(!verified);
        }
    }
}
