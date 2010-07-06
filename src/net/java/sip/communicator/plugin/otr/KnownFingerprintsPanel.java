package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * @author @George Politis
 * @author Yana Stamcheva
 */
public class KnownFingerprintsPanel
    extends TransparentPanel
{
    private JButton btnVerifyFingerprint;

    private JButton btnForgetFingerprint;

    private JTable contactsTable;

    public KnownFingerprintsPanel()
    {
        this.initComponents();

        this.setPreferredSize(new Dimension(400, 200));

        openContact(getSelectedContact());
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

                    openContact(getSelectedContact());

                }
            });

        JScrollPane pnlContacts = new JScrollPane(contactsTable);
        this.add(pnlContacts);

        JPanel pnlButtons = new TransparentPanel();
        this.add(pnlButtons);

        btnVerifyFingerprint = new JButton();
        btnVerifyFingerprint.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.configform.VERIFY_FINGERPRINT"));

        btnVerifyFingerprint.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                OtrActivator.scOtrKeyManager
                    .verify(getSelectedContact());
            }
        });

        pnlButtons.add(btnVerifyFingerprint);

        btnForgetFingerprint = new JButton();
        btnForgetFingerprint.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.configform.FORGET_FINGERPRINT"));
        btnForgetFingerprint.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                OtrActivator.scOtrKeyManager
                    .unverify(getSelectedContact());
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
        if (index < 0 || index > model.allContacts.size())
            return null;

        return model.allContacts.get(index);
    }

    /**
     * Sets up the {@link KnownFingerprintsTableModel} components so that they
     * reflect the {@link Contact} param.
     * 
     * @param contact the {@link Contact} to setup the components for.
     */
    private void openContact(Contact contact)
    {
        if (contact == null)
        {
            btnForgetFingerprint.setEnabled(false);
            btnVerifyFingerprint.setEnabled(false);
        }
        else
        {
            boolean verified
                = OtrActivator.scOtrKeyManager
                    .isVerified(contact);

            btnForgetFingerprint.setEnabled(verified);
            btnVerifyFingerprint.setEnabled(!verified);
        }
    }
}
