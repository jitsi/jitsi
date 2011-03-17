package net.java.sip.communicator.plugin.dnsconfig;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.EventObject;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;


import org.osgi.framework.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import static net.java.sip.communicator.util.NetworkUtils.*;
import static net.java.sip.communicator.util.dns.ParallelResolver.*;

/**
 * Page inside the advanced configuration options that allow the user to
 * modify the settings of the DNS parallel resolver.
 * 
 * @author Ingo Bauersachs
 */
public class DnsConfigPanel
    extends TransparentPanel
    implements ActionListener,
               ChangeListener,
               FocusListener
{
    private static final long serialVersionUID = 4393128042592738855L;

    //UI controls
    private JCheckBox chkBackupDnsEnabled;
    private JLabel lblBackupPort;
    private JLabel lblBackupResolver;
    private JLabel lblBackupResolverFallbackIP;
    private JLabel lblPatience;
    private JLabel lblRedemption;
    private JSpinner spnBackupResolverPort;
    private JSpinner spnDnsRedemption;
    private JSpinner spnDnsTimeout;
    private JTextField txtBackupResolver;
    private JFormattedTextField txtBackupResolverFallbackIP;

    //service references
    private ResourceManagementService R;
    private ConfigurationService configService;

    /**
     * Creates a new instance of this class and prepares the UI
     */
    public DnsConfigPanel()
    {
        initServices();
        initComponents();
        initBehavior();
        loadData();
    }

    /**
     * Loads all service references
     */
    private void initServices()
    {
        BundleContext bc = DnsConfigActivator.bundleContext;
        R = ServiceUtils.getService(bc, ResourceManagementService.class);
        configService = ServiceUtils.getService(bc, ConfigurationService.class);
    }

    /**
     * Create the UI components
     */
    private void initComponents()
    {
        chkBackupDnsEnabled = new SIPCommCheckBox();
        spnDnsTimeout = new JSpinner(new SpinnerNumberModel(0, 0, 30000, 100));
        spnDnsRedemption = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        spnBackupResolverPort = new JSpinner(
            new SpinnerNumberModel(0, 0, 65536, 1));
        txtBackupResolverFallbackIP =
            new JFormattedTextField(createIPFormatter());
        lblRedemption = new JLabel();
        txtBackupResolver = new JTextField();
        lblBackupResolver = new JLabel();
        lblBackupPort = new JLabel();
        lblBackupResolverFallbackIP = new JLabel();
        lblPatience = new JLabel();

        chkBackupDnsEnabled.setText(
            R.getI18NString("plugin.dnsconfig.chkBackupDnsEnabled.text"));

        lblRedemption.setText(
            R.getI18NString("plugin.dnsconfig.lblRedemption.text"));

        lblBackupResolver.setText(
            R.getI18NString("plugin.dnsconfig.lblBackupResolver.text"));

        lblBackupPort.setText(
            R.getI18NString("plugin.dnsconfig.lblBackupPort.text"));

        lblBackupResolverFallbackIP.setText(R.getI18NString(
            "plugin.dnsconfig.lblBackupResolverFallbackIP.text"));

        lblPatience.setText(
            R.getI18NString("plugin.dnsconfig.lblPatience.text"));

        this.setLayout(new BorderLayout());

        JPanel mainPanel = new TransparentPanel();

        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder(
            R.getI18NString("plugin.dnsconfig.border.TITLE")));

        GridBagConstraints cl = new GridBagConstraints();
        GridBagConstraints cr = new GridBagConstraints();

        chkBackupDnsEnabled.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel enablePanel =
            new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        enablePanel.add(chkBackupDnsEnabled);

        add(enablePanel, BorderLayout.NORTH);
        JPanel centerPanel = new TransparentPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(mainPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        cl.fill = GridBagConstraints.HORIZONTAL;
        cl.gridx = 0;
        cl.gridy = 0;
        cl.insets = new Insets(0, 3, 0, 0);
        mainPanel.add(lblBackupResolver, cl);
        cr.fill = GridBagConstraints.HORIZONTAL;
        cr.anchor = GridBagConstraints.WEST;
        cr.insets = new Insets(0, 3, 0, 3);
        cr.weightx = 1.0;
        cr.gridx = 1;
        cr.gridy = 0;
        mainPanel.add(txtBackupResolver, cr);

        cl.gridy = 1;
        cl.insets = new Insets(5, 3, 0, 0);
        cr.gridy = 1;
        cr.insets = new Insets(5, 3, 0, 3);
        mainPanel.add(lblBackupResolverFallbackIP, cl);
        mainPanel.add(txtBackupResolverFallbackIP, cr);

        cl.gridy = 2;
        cr.gridy = 2;
        mainPanel.add(lblBackupPort, cl);
        mainPanel.add(spnBackupResolverPort, cr);

        cl.gridy = 3;
        cr.gridy = 3;
        mainPanel.add(lblPatience, cl);
        mainPanel.add(spnDnsTimeout, cr);
        String label =
            R.getI18NString("plugin.dnsconfig.lblPatience.description");
        JLabel descriptionLabel = new JLabel(label);
        descriptionLabel.setToolTipText(label);
        descriptionLabel.setForeground(Color.GRAY);
        Font f = descriptionLabel.getFont().deriveFont(8);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(11f));
        cr.gridy = 4;
        mainPanel.add(descriptionLabel, cr);

        cl.gridy = 5;
        cr.gridy = 5;
        mainPanel.add(lblRedemption, cl);
        mainPanel.add(spnDnsRedemption, cr);
        label = R.getI18NString("plugin.dnsconfig.lblRedemption.description");
        descriptionLabel = new JLabel(label);
        descriptionLabel.setToolTipText(label);
        descriptionLabel.setForeground(Color.GRAY);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(11f));
        cr.gridy = 6;
        cr.insets = new Insets(5, 3, 3, 3);
        mainPanel.add(descriptionLabel, cr);

    }

    /**
     * Enables or disables all buttons.
     */
    private void updateButtonsState()
    {
        txtBackupResolver.setEnabled(chkBackupDnsEnabled.isSelected());
        lblBackupResolver.setEnabled(chkBackupDnsEnabled.isSelected());
        txtBackupResolverFallbackIP
            .setEnabled(chkBackupDnsEnabled.isSelected());
        lblBackupResolverFallbackIP
            .setEnabled(chkBackupDnsEnabled.isSelected());
        txtBackupResolverFallbackIP
            .setEnabled(chkBackupDnsEnabled.isSelected());
        lblBackupResolverFallbackIP
            .setEnabled(chkBackupDnsEnabled.isSelected());
        spnBackupResolverPort.setEnabled(chkBackupDnsEnabled.isSelected());
        lblBackupPort.setEnabled(chkBackupDnsEnabled.isSelected());
        spnDnsTimeout.setEnabled(chkBackupDnsEnabled.isSelected());
        lblPatience.setEnabled(chkBackupDnsEnabled.isSelected());
        spnDnsRedemption.setEnabled(chkBackupDnsEnabled.isSelected());
        lblRedemption.setEnabled(chkBackupDnsEnabled.isSelected());
    }

    /**
     * Adds the behavior to the UI controls
     */
    private void initBehavior()
    {
        chkBackupDnsEnabled.addActionListener(this);
        txtBackupResolver.addActionListener(this);
        txtBackupResolver.addFocusListener(this);
        txtBackupResolverFallbackIP.addActionListener(this);
        txtBackupResolverFallbackIP.addFocusListener(this);
        spnBackupResolverPort.addChangeListener(this);
        spnDnsTimeout.addChangeListener(this);
        spnDnsRedemption.addChangeListener(this);
    }

    /**
     * Action has occurred in the config form.
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e)
    {
        saveProperty(e);
    }

    /**
     * Action has occurred in the config form.
     * @param e the action event
     */
    public void stateChanged(ChangeEvent e)
    {
        saveProperty(e);
    }

    /**
     * Action has occurred in the config form.
     * @param e the action event
     */
    public void focusLost(FocusEvent e)
    {
        saveProperty(e);
    }

    /**
     * Action has occurred in the config form.
     * @param e the action event
     */
    public void focusGained(FocusEvent e)
    {}

    /**
     * Stores the changed UI value in the configuration
     * @param e An event object required to provide access to the
     *  source UI element
     */
    private void saveProperty(EventObject e)
    {
        if(e.getSource() == chkBackupDnsEnabled)
        {
            boolean enabled = chkBackupDnsEnabled.isSelected();

            configService.setProperty(
                PNAME_BACKUP_RESOLVER_ENABLED,
                enabled
            );

            updateButtonsState();
        }
        else if(e.getSource() == txtBackupResolver)
        {
            configService.setProperty(
                PNAME_BACKUP_RESOLVER,
                txtBackupResolver.getText()
            );
        }
        else if(e.getSource() == txtBackupResolverFallbackIP)
        {
            configService.setProperty(
                PNAME_BACKUP_RESOLVER_FALLBACK_IP,
                txtBackupResolverFallbackIP.getValue().toString()
            );
        }
        else if(e.getSource() == spnBackupResolverPort)
        {
            configService.setProperty(
                PNAME_BACKUP_RESOLVER_PORT,
                spnBackupResolverPort.getValue().toString()
            );
        }
        else if(e.getSource() == spnDnsTimeout)
        {
            configService.setProperty(
                PNAME_DNS_PATIENCE,
                spnDnsTimeout.getValue().toString()
            );
        }
        else if(e.getSource() == spnDnsRedemption)
        {
            configService.setProperty(
                PNAME_DNS_REDEMPTION,
                spnDnsRedemption.getValue().toString()
            );
        }
    }

    /**
     * Creates a formatter for the fallback IP textfield to validate whether
     * the entered text is actually an IP address
     * @return A formatter accepting only IP addresses in text form
     */
    @SuppressWarnings("serial")
    private DefaultFormatter createIPFormatter()
    {
        return new DefaultFormatter()
        {
            @Override
            public Object stringToValue(String string) throws ParseException
            {
                if(isIPv4Address(string) || isIPv6Address(string))
                    return super.stringToValue(string);
                throw new ParseException("Not a valid literal IP address", 0);
            }
        };
    }

    /**
     * Reads the configured properties or their defaults into the UI controls.
     */
    private void loadData()
    {
        chkBackupDnsEnabled.setSelected(configService.getBoolean(
            PNAME_BACKUP_RESOLVER_ENABLED, PDEFAULT_BACKUP_RESOLVER_ENABLED));
        txtBackupResolver.setText(configService.getString(
            PNAME_BACKUP_RESOLVER, DEFAULT_BACKUP_RESOLVER));
        txtBackupResolverFallbackIP.setValue(configService.getString(
            PNAME_BACKUP_RESOLVER_FALLBACK_IP,
            R.getSettingsString(PNAME_BACKUP_RESOLVER_FALLBACK_IP)));
        spnBackupResolverPort.setValue(configService.getInt(
            PNAME_BACKUP_RESOLVER_PORT, getDefaultDnsPort()));
        spnDnsTimeout.setValue(configService.getInt(
            PNAME_DNS_PATIENCE, DNS_PATIENCE));
        spnDnsRedemption.setValue(configService.getInt(
            PNAME_DNS_REDEMPTION, DNS_REDEMPTION));

        updateButtonsState();
    }
}
