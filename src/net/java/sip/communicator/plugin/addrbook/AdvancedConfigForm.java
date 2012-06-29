/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.addrbook.macosx.*;
import net.java.sip.communicator.plugin.addrbook.msoutlook.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.util.*;

/**
 * Implementation of the advanced address book configuration form.
 *
 * @author Yana Stamcheva
 */
public class AdvancedConfigForm
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates the form.
     */
    public AdvancedConfigForm()
    {
        super(new BorderLayout());

        JPanel propertiesPanel = new TransparentPanel();
        propertiesPanel.setLayout(
            new BoxLayout(propertiesPanel, BoxLayout.Y_AXIS));

        JTextPane descriptionTextPane = new JTextPane();
        descriptionTextPane.setEditable(false);
        descriptionTextPane.setOpaque(false);
        descriptionTextPane.setForeground(Color.GRAY);
        descriptionTextPane.setText(
            AddrBookActivator.getResources().getI18NString(
                "plugin.addrbook.DESCRIPTION"));

        propertiesPanel.add(descriptionTextPane);
        propertiesPanel.add(Box.createVerticalStrut(15));

        if (OSUtils.IS_MAC)
            propertiesPanel.add(createEnableCheckBox(
                AddrBookActivator.PNAME_ENABLE_MACOSX_ADDRESS_BOOK_SEARCH,
                "plugin.addrbook.ENABLE_MACOSX_ADDRESSBOOK"));

        if (OSUtils.IS_WINDOWS)
            propertiesPanel.add(createEnableCheckBox(
                AddrBookActivator.PNAME_ENABLE_MICROSOFT_OUTLOOK_SEARCH,
                "plugin.addrbook.ENABLE_MICROSOFT_OUTLOOK"));

        propertiesPanel.add(Box.createVerticalStrut(15));

        propertiesPanel.add(createPrefixPanel());

        add(propertiesPanel, BorderLayout.NORTH);
    }

    /**
     * Creates the enable check box.
     *
     * @return the created enable check box
     */
    private Component createEnableCheckBox(final String configPropName,
                                                 String labelNameKey)
    {
        final JCheckBox checkBox = new JCheckBox(AddrBookActivator
            .getResources().getI18NString(
                labelNameKey),
                AddrBookActivator.getConfigService().getBoolean(configPropName,
                                                                true));

        checkBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                AddrBookActivator.getConfigService().setProperty(
                    configPropName,
                    new Boolean(checkBox.isSelected()).toString());

                if (checkBox.isSelected())
                    AddrBookActivator.startService();
                else
                    AddrBookActivator.stopService();
            }
        });
        return checkBox;
    }

    /**
     * Creates the prefix panel.
     *
     * @return the created prefix panel
     */
    private JComponent createPrefixPanel()
    {
        JLabel prefixLabel = new JLabel(AddrBookActivator
            .getResources().getI18NString("plugin.addrbook.PREFIX"));

        final SIPCommTextField prefixField = new SIPCommTextField(
            AddrBookActivator.getResources()
                .getI18NString("plugin.addrbook.PREFIX_EXAMPLE"));

        String storedPrefix = null;
        if (OSUtils.IS_MAC)
            storedPrefix = AddrBookActivator.getConfigService().getString(
                MacOSXAddrBookContactSourceService
                    .MACOSX_ADDR_BOOK_PREFIX);

        if (OSUtils.IS_WINDOWS)
            storedPrefix = AddrBookActivator.getConfigService().getString(
                MsOutlookAddrBookContactSourceService
                    .OUTLOOK_ADDR_BOOK_PREFIX);

        if (storedPrefix != null && storedPrefix.length() > 0)
            prefixField.setText(storedPrefix);

        JPanel prefixPanel = new TransparentPanel(new BorderLayout());

        prefixPanel.add(prefixLabel, BorderLayout.WEST);
        prefixPanel.add(prefixField);

        prefixField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                String prefix = prefixField.getText();

                if (prefix == null || prefix.length() <= 0)
                    return;

                if (OSUtils.IS_MAC)
                    AddrBookActivator.getConfigService().setProperty(
                        MacOSXAddrBookContactSourceService
                            .MACOSX_ADDR_BOOK_PREFIX,
                        prefix);

                if (OSUtils.IS_WINDOWS)
                    AddrBookActivator.getConfigService().setProperty(
                        MsOutlookAddrBookContactSourceService
                            .OUTLOOK_ADDR_BOOK_PREFIX,
                        prefix);
            }
        });
        return prefixPanel;
    }
}
