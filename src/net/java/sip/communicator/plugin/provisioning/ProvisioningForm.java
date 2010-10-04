/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.provisioning;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ProvisioningForm
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The enable provisioning check box.
     */
    private final JCheckBox enableCheckBox;

    /**
     * The DHCP provisioning discovery button.
     */
    private final JRadioButton dhcpButton;

    /**
     * The DNS provisioning discovery button.
     */
    private final JRadioButton dnsButton;

    /**
     * The Bonjour provisioning discovery button.
     */
    private final JRadioButton bonjourButton;

    /**
     * The manual provisioning button.
     */
    private final JRadioButton manualButton;

    /**
     * The URI field to specify manually a provisioning server.
     */
    private final SIPCommTextField uriField;

    /**
     * Creates an instance of the <tt>ProvisioningForm</tt>.
     */
    public ProvisioningForm()
    {
        super(new BorderLayout());

        ResourceManagementService resources
            = ProvisioningActivator.getResourceService();

        enableCheckBox = new JCheckBox(
            resources.getI18NString("plugin.provisioning.ENABLE_DISABLE"));

        dhcpButton = new JRadioButton(
            resources.getI18NString("plugin.provisioning.DHCP"));

        dnsButton = new JRadioButton(
            resources.getI18NString("plugin.provisioning.DNS"));

        bonjourButton = new JRadioButton(
            resources.getI18NString("plugin.provisioning.BONJOUR"));

        manualButton = new JRadioButton(
            resources.getI18NString("plugin.provisioning.MANUAL"));

        uriField = new SIPCommTextField(
            resources.getI18NString("plugin.provisioning.URI"));

        JPanel mainPanel = new TransparentPanel();

        add(mainPanel, BorderLayout.NORTH);

        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        enableCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        mainPanel.add(enableCheckBox, c);

        final ButtonGroup buttonGroup = new ButtonGroup();

        buttonGroup.add(dhcpButton);
        buttonGroup.add(dnsButton);
        buttonGroup.add(bonjourButton);
        buttonGroup.add(manualButton);

        final JPanel radioButtonPanel
            = new TransparentPanel(new GridLayout(0, 1));

        radioButtonPanel.setBorder(BorderFactory.createTitledBorder(
            resources.getI18NString("plugin.provisioning.AUTO")));

        radioButtonPanel.add(dhcpButton);
        radioButtonPanel.add(dnsButton);
        radioButtonPanel.add(bonjourButton);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 20, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        mainPanel.add(radioButtonPanel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 26, 0, 0);
        c.gridx = 0;
        c.gridy = 2;
        mainPanel.add(manualButton, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 51, 0, 0);
        c.gridx = 0;
        c.gridy = 3;
        mainPanel.add(uriField, c);

        initButtonStates();
        initListeners();
    }

    /**
     * Initializes all contained components.
     */
    private void initButtonStates()
    {
        String provMethod = ProvisioningActivator.getProvisioningMethod();
        boolean isProvEnabled
            = (provMethod != null && !provMethod.equals("NONE"));

        enableCheckBox.setSelected(isProvEnabled);

        if (isProvEnabled)
        {
            if (provMethod.equals("DHCP"))
                dhcpButton.setSelected(true);
            else if (provMethod.equals("DNS"))
                dnsButton.setSelected(true);
            else if (provMethod.equals("Bonjour"))
                bonjourButton.setSelected(true);
            else if (provMethod.equals("Manual"))
            {
                manualButton.setSelected(true);

                String uri = ProvisioningActivator.getProvisioningUri();
                if (uri != null)
                    uriField.setText(uri);
            }
        }

        dhcpButton.setEnabled(isProvEnabled);
        manualButton.setEnabled(isProvEnabled);
        uriField.setEnabled(manualButton.isSelected());
        dnsButton.setEnabled(false);
        bonjourButton.setEnabled(false);
    }

    /**
     * Initializes all listeners.
     */
    private void initListeners()
    {
        enableCheckBox.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                boolean isSelected = enableCheckBox.isSelected();

                dhcpButton.setEnabled(isSelected);
                manualButton.setEnabled(isSelected);

                String provisioningMethod = null;

                if (isSelected)
                {
                    if (dhcpButton.isSelected())
                    {
                        provisioningMethod = "DHCP";
                    }
                    else if (dnsButton.isSelected())
                    {
                        provisioningMethod = "DNS";
                    }
                    else if (bonjourButton.isSelected())
                    {
                        provisioningMethod = "Bonjour";
                    }
                    else if (manualButton.isSelected())
                    {
                        provisioningMethod = "Manual";
                    }
                    else
                    {
                        dhcpButton.setSelected(true);
                        provisioningMethod = "DHCP";
                    }
                }

                ProvisioningActivator
                    .setProvisioningMethod(provisioningMethod);
            }
        });

        dhcpButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (dhcpButton.isSelected())
                    ProvisioningActivator
                        .setProvisioningMethod("DHCP");
            }
        });

        dnsButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (dnsButton.isSelected())
                    ProvisioningActivator
                        .setProvisioningMethod("DNS");
            }
        });

        bonjourButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (bonjourButton.isSelected())
                    ProvisioningActivator
                        .setProvisioningMethod("Bonjour");
            }
        });

        manualButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                boolean isSelected = manualButton.isSelected();

                uriField.setEnabled(isSelected);

                if (isSelected)
                {
                    ProvisioningActivator
                        .setProvisioningMethod("Manual");

                    String uriText = uriField.getText();
                    if (uriText != null && uriText.length() > 0)
                        ProvisioningActivator.setProvisioningUri(uriText);
                }
                else
                    ProvisioningActivator.setProvisioningUri(null);
            }
        });

        uriField.addFocusListener(new FocusListener()
        {
            public void focusLost(FocusEvent e)
            {
                // If the manual button isn't selected we have nothing more
                // to do here.
                if (!manualButton.isSelected())
                    return;

                String uriText = uriField.getText();
                if (uriText != null && uriText.length() > 0)
                    ProvisioningActivator.setProvisioningUri(uriText);
            }

            public void focusGained(FocusEvent e) {}
        });
    }
}
