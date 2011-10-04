package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

import ch.imvs.sdes4j.srtp.SrtpCryptoSuite;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

public class SecurityPanel
    extends TransparentPanel
    implements ActionListener
{
    private SIPAccountRegistrationForm regform;

    private JCheckBox enableDefaultEncryption;
    private JCheckBox enableSipZrtpAttribute;
    private JCheckBox enableSDesAttribute;
    private JComboBox cboSavpOption;
    private JTable tabCiphers;
    private CipherTableModel cipherModel;

    private static class SavpOption
    {
        int option;

        SavpOption(int option)
        {
            this.option = option;
        }

        @Override
        public String toString()
        {
            return Resources.getString("plugin.sipaccregwizz.SAVP_OPTION_"
                + option);
        }
    }

    private static class Entry
    {
        String cipher;
        Boolean enabled;

        public Entry(String cipher, boolean enabled)
        {
            this.cipher = cipher;
            this.enabled = enabled;
        }
    }

    private static class CipherTableModel extends AbstractTableModel
    {
        private List<Entry> data = new ArrayList<Entry>();
        private final String defaultCiphers = Resources.getResources()
            .getSettingsString(SDesControl.SDES_CIPHER_SUITES);

        CipherTableModel(String ciphers)
        {
            loadData(ciphers);
        }

        public void loadData(String ciphers)
        {
            data.clear();
            if(ciphers == null)
                ciphers = defaultCiphers;
            //TODO the available ciphers should come from SDesControlImpl
            data.add(new Entry(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80, ciphers
                .contains(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80)));
            data.add(new Entry(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32, ciphers
                .contains(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32)));
            data.add(new Entry(SrtpCryptoSuite.F8_128_HMAC_SHA1_80, ciphers
                .contains(SrtpCryptoSuite.F8_128_HMAC_SHA1_80)));
            fireTableDataChanged();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            switch(columnIndex)
            {
                case 0:
                    return Boolean.class;
                case 1:
                    return String.class;
            }
            return null;
        }

        @Override
        public int getRowCount()
        {
            return data.size();
        }

        @Override
        public int getColumnCount()
        {
            return 2;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return (columnIndex == 0);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            Entry e = data.get(rowIndex);
            switch(columnIndex)
            {
                case 0:
                    return e.enabled;
                case 1:
                    return e.cipher;
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex)
        {
            if ((columnIndex == 0) && (value instanceof Boolean))
            {
                Entry e = data.get(rowIndex);
                e.enabled = (Boolean)value;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        String getEnabledCiphers()
        {
            StringBuilder sb = new StringBuilder();
            for (Entry e : data)
            {
                if(e.enabled)
                {
                    sb.append(e.cipher);
                    sb.append(',');
                }
            }
            if(sb.length() == 0)
                return null;
            String result = sb.substring(0, sb.length()-1);
            return defaultCiphers.equals(result) ? null : result;
        }
    }

    public SecurityPanel(SIPAccountRegistrationForm regform)
    {
        super(new BorderLayout(10, 10));

        this.regform = regform;
        initComponents();
        actionPerformed(null);
    }

    private void initComponents()
    {
        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new GridBagLayout());
        add(mainPanel, BorderLayout.NORTH);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;

        //general encryption option
        enableDefaultEncryption = new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_DEFAULT_ENCRYPTION"),
            regform.getRegistration().isDefaultEncryption());
        enableDefaultEncryption.addActionListener(this);
        c.gridy++;
        mainPanel.add(enableDefaultEncryption, c);

        //ZRTP
        enableSipZrtpAttribute = new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_SIPZRTP_ATTRIBUTE"),
            regform.getRegistration().isSipZrtpAttribute());
        c.gridy++;
        mainPanel.add(enableSipZrtpAttribute, c);

        //SAVP selection
        JLabel lblSavpOption = new JLabel();
        lblSavpOption.setBorder(new EmptyBorder(5, 5, 5, 0));
        lblSavpOption.setText(
            Resources.getString("plugin.sipaccregwizz.SAVP_OPTION"));
        c.gridy++;
        mainPanel.add(lblSavpOption, c);
        c.gridx = 2;
        c.weightx = 1;
        mainPanel.add(new JSeparator(), c);
        cboSavpOption = new JComboBox(new SavpOption[]{
            new SavpOption(0),
            new SavpOption(1),
            new SavpOption(2)
        });
        c.gridx = 1;
        c.gridy++;
        c.insets = new Insets(0, 20, 0, 0);
        c.weightx = 0;
        mainPanel.add(cboSavpOption, c);

        //SDES
        enableSDesAttribute = new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_SDES_ATTRIBUTE"),
            regform.getRegistration().isSDesEnabled());
        enableSDesAttribute.addActionListener(this);
        c.gridy++;
        c.gridwidth = 1;
        c.insets = new Insets(15, 0, 0, 0);
        mainPanel.add(enableSDesAttribute, c);
        c.gridx = 2;
        c.weightx = 1;
        mainPanel.add(new JSeparator(), c);

        cipherModel = new CipherTableModel(regform.getRegistration()
            .getSDesCipherSuites());
        tabCiphers = new JTable(cipherModel);
        TableColumnModel tableColumnModel = tabCiphers.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth());
        c.gridy++;
        c.gridx = 1;
        c.gridwidth = 2;
        c.insets = new Insets(0, 20, 0, 0);
        mainPanel.add(tabCiphers, c);
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     *
     * @param registration the SIPAccountRegistration
     * @return
     */
    boolean commitPanel(SIPAccountRegistration registration)
    {
        registration.setDefaultEncryption(enableDefaultEncryption.isSelected());
        registration.setSipZrtpAttribute(enableSipZrtpAttribute.isSelected());
        registration.setSavpOption(((SavpOption) cboSavpOption
            .getSelectedItem()).option);
        registration.setSDesEnabled(enableSDesAttribute.isSelected());
        registration.setSDesCipherSuites(cipherModel.getEnabledCiphers());
        return true;
    }

    /**
     * Loads the account with the given identifier.
     * @param accountID the account identifier
     */
    void loadAccount(AccountID accountID)
    {
        enableDefaultEncryption.setSelected(accountID
            .getAccountPropertyBoolean(
                ProtocolProviderFactory.DEFAULT_ENCRYPTION, true));
        enableSipZrtpAttribute.setSelected(accountID.getAccountPropertyBoolean(
            ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE, true));
        cboSavpOption.setSelectedIndex(accountID.getAccountPropertyInt(
            ProtocolProviderFactory.SAVP_OPTION,
            ProtocolProviderFactory.SAVP_OFF));
        enableSDesAttribute.setSelected(accountID.getAccountPropertyBoolean(
            ProtocolProviderFactory.SDES_ENABLED, false));
        cipherModel.loadData(accountID.getAccountPropertyString(
                    ProtocolProviderFactory.SDES_CIPHER_SUITES));
    }

    public void actionPerformed(ActionEvent e)
    {
        boolean b = enableDefaultEncryption.isSelected();
        enableSipZrtpAttribute.setEnabled(b);
        cboSavpOption.setEnabled(b);
        enableSDesAttribute.setEnabled(b);
        tabCiphers.setEnabled(b && enableSDesAttribute.isSelected());
    }
}
