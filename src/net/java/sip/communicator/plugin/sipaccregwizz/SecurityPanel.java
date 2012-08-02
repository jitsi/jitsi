package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.service.neomedia.*;

import ch.imvs.sdes4j.srtp.*;

/**
 * Contains the security settings for SIP media encryption.
 *
 * @author Ingo Bauersachs
 */
public class SecurityPanel
    extends TransparentPanel
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final SecurityAccountRegistration regform;

    private JPanel pnlAdvancedSettings;
    private JCheckBox enableDefaultEncryption;
    private JCheckBox enableSipZrtpAttribute;
    private JCheckBox enableSDesAttribute;
    private JComboBox cboSavpOption;
    private JTable tabCiphers;
    private CipherTableModel cipherModel;
    private JLabel cmdExpandAdvancedSettings;

    /**
     * Boolean used to display or not the SAVP options (only useful for SIP, not
     * for XMPP).
     */
    private boolean displaySavpOtions;

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
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

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

            if(defaultCiphers == null)
                return;

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

        public int getRowCount()
        {
            return data.size();
        }

        public int getColumnCount()
        {
            return 2;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return (columnIndex == 0);
        }

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
            {
                return sb.toString();
            }
            else
            {
                return sb.substring(0, sb.length()-1);
            }
        }
    }

    /**
     * Initiates a panel to configure the security (ZRTP and SDES) for SIP or
     * XMPP protocols.
     *
     * @param regform The registration form of the account to configure.
     * @param displaySavpOptions Boolean used to display or not the SAVP options
     * (only useful for SIP, not for XMPP).
     */
    public SecurityPanel(
            SecurityAccountRegistration regform,
            boolean displaySavpOptions)
    {
        super(new BorderLayout(10, 10));

        this.regform = regform;
        this.displaySavpOtions = displaySavpOptions;
        initComponents();
    }

    private void initComponents()
    {
        setLayout(new BorderLayout());
        final JPanel mainPanel = new TransparentPanel();
        add(mainPanel, BorderLayout.NORTH);

        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        //general encryption option
        enableDefaultEncryption = new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_DEFAULT_ENCRYPTION"),
            regform.isDefaultEncryption());
        enableDefaultEncryption.addActionListener(this);
        mainPanel.add(enableDefaultEncryption, c);

        //warning message and button to show advanced options
        JLabel lblWarning = new JLabel();
        lblWarning.setBorder(new EmptyBorder(10, 5, 10, 0));
        lblWarning.setText(Resources.getResources().getI18NString(
            "plugin.sipaccregwizz.SECURITY_WARNING",
            new String[]{
                Resources.getResources().getSettingsString(
                    "service.gui.APPLICATION_NAME")
            }
        ));
        c.gridy++;
        mainPanel.add(lblWarning, c);

        cmdExpandAdvancedSettings = new JLabel();
        cmdExpandAdvancedSettings.setBorder(new EmptyBorder(0, 5, 0, 0));
        cmdExpandAdvancedSettings.setIcon(Resources.getResources()
            .getImage("service.gui.icons.RIGHT_ARROW_ICON"));
        cmdExpandAdvancedSettings.setText(Resources
            .getString("plugin.sipaccregwizz.SHOW_ADVANCED"));
        cmdExpandAdvancedSettings.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                cmdExpandAdvancedSettings.setIcon(
                        Resources.getResources().getImage(
                            pnlAdvancedSettings.isVisible()
                                    ? "service.gui.icons.RIGHT_ARROW_ICON"
                                    : "service.gui.icons.DOWN_ARROW_ICON"));

                pnlAdvancedSettings.setVisible(
                    !pnlAdvancedSettings.isVisible());

                pnlAdvancedSettings.revalidate();
            }
        });
        c.gridy++;
        mainPanel.add(cmdExpandAdvancedSettings, c);

        pnlAdvancedSettings = new TransparentPanel();
        pnlAdvancedSettings.setLayout(new GridBagLayout());
        pnlAdvancedSettings.setVisible(false);
        c.gridy++;
        mainPanel.add(pnlAdvancedSettings, c);


        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;

        //ZRTP
        enableSipZrtpAttribute = new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_SIPZRTP_ATTRIBUTE"),
            regform.isSipZrtpAttribute());
        c.gridy++;
        pnlAdvancedSettings.add(enableSipZrtpAttribute, c);

        //SAVP selection
        JLabel lblSavpOption = new JLabel();
        lblSavpOption.setBorder(new EmptyBorder(5, 5, 5, 0));
        lblSavpOption.setText(
            Resources.getString("plugin.sipaccregwizz.SAVP_OPTION"));
        c.gridy++;
        if(this.displaySavpOtions)
        {
            pnlAdvancedSettings.add(lblSavpOption, c);
        }
        c.gridx = 2;
        c.weightx = 1;
        if(this.displaySavpOtions)
        {
            pnlAdvancedSettings.add(new JSeparator(), c);
        }
        cboSavpOption = new JComboBox(new SavpOption[]{
            new SavpOption(0),
            new SavpOption(1),
            new SavpOption(2)
        });
        c.gridx = 1;
        c.gridy++;
        c.insets = new Insets(0, 20, 0, 0);
        c.weightx = 0;
        if(this.displaySavpOtions)
        {
            pnlAdvancedSettings.add(cboSavpOption, c);
        }

        //SDES
        enableSDesAttribute = new SIPCommCheckBox(Resources
            .getString("plugin.sipaccregwizz.ENABLE_SDES_ATTRIBUTE"),
            regform.isSDesEnabled());
        enableSDesAttribute.addActionListener(this);
        c.gridy++;
        c.gridwidth = 1;
        c.insets = new Insets(15, 0, 0, 0);
        pnlAdvancedSettings.add(enableSDesAttribute, c);
        c.gridx = 2;
        c.weightx = 1;
        pnlAdvancedSettings.add(new JSeparator(), c);


        c.gridy++;
        c.gridx = 1;
        c.gridwidth = 2;
        c.insets = new Insets(0, 20, 0, 0);
        JLabel lblCipherInfo = new JLabel();
        lblCipherInfo.setText(Resources
            .getString("plugin.sipaccregwizz.CIPHER_SUITES"));
        pnlAdvancedSettings.add(lblCipherInfo, c);

        cipherModel = new CipherTableModel(regform.getSDesCipherSuites());
        tabCiphers = new JTable(cipherModel);
        tabCiphers.setShowGrid(false);
        tabCiphers.setTableHeader(null);
        TableColumnModel tableColumnModel = tabCiphers.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth());
        c.gridy++;
        c.insets = new Insets(0, 20, 0, 0);
        JScrollPane scrollPane = new JScrollPane(tabCiphers);
        scrollPane.setPreferredSize(new Dimension(tabCiphers.getWidth(), 100));
        pnlAdvancedSettings.add(scrollPane, c);
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     *
     * @param registration the SIPAccountRegistration
     * @return
     */
    public boolean commitPanel(SecurityAccountRegistration registration)
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
    public void loadAccount(AccountID accountID)
    {
        enableDefaultEncryption.setSelected(
                accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true));
        enableSipZrtpAttribute.setSelected(
                accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                        true));
        cboSavpOption.setSelectedIndex(
                accountID.getAccountPropertyInt(
                        ProtocolProviderFactory.SAVP_OPTION,
                        ProtocolProviderFactory.SAVP_OFF));
        enableSDesAttribute.setSelected(
                accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.SDES_ENABLED,
                        false));
        cipherModel.loadData(
                accountID.getAccountPropertyString(
                        ProtocolProviderFactory.SDES_CIPHER_SUITES));
        loadStates();
    }

    public void actionPerformed(ActionEvent e)
    {
        if((e.getSource() == enableDefaultEncryption)
                || (e.getSource() == enableSDesAttribute))
        {
            loadStates();
        }
        else if(e.getSource() == cmdExpandAdvancedSettings)
        {
            pnlAdvancedSettings.setVisible(!pnlAdvancedSettings.isVisible());
        }
    }

    private void loadStates()
    {
        boolean b = enableDefaultEncryption.isSelected();
        enableSipZrtpAttribute.setEnabled(b);
        cboSavpOption.setEnabled(b);
        enableSDesAttribute.setEnabled(b);
        tabCiphers.setEnabled(b && enableSDesAttribute.isSelected());
    }
}
