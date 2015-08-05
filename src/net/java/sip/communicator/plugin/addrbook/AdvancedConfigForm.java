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
package net.java.sip.communicator.plugin.addrbook;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.addrbook.macosx.*;
import net.java.sip.communicator.plugin.addrbook.msoutlook.*;
import net.java.sip.communicator.plugin.desktoputil.*;

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
        descriptionTextPane.setText(
            AddrBookActivator.getResources().getI18NString(
                "plugin.addrbook.DESCRIPTION"));
        descriptionTextPane.setAlignmentX(Component.LEFT_ALIGNMENT);


        propertiesPanel.add(descriptionTextPane);
        propertiesPanel.add(Box.createVerticalStrut(15));

        if (OSUtils.IS_MAC)
            propertiesPanel.add(createEnableCheckBox(
                AddrBookActivator.PNAME_ENABLE_MACOSX_ADDRESS_BOOK_SEARCH,
                "plugin.addrbook.ENABLE_MACOSX_ADDRESSBOOK"));

        if (OSUtils.IS_WINDOWS)
        {
            propertiesPanel.add(createEnableCheckBox(
                AddrBookActivator.PNAME_ENABLE_MICROSOFT_OUTLOOK_SEARCH,
                "plugin.addrbook.ENABLE_MICROSOFT_OUTLOOK"));
            if(AddrBookActivator.getConfigService().getBoolean(
                AddrBookActivator.PNAME_ENABLE_DEFAULT_IM_APPLICATION_CHANGE, 
                true))
                propertiesPanel.add(createDefaultIMApplicationCheckBox(
                    AddrBookActivator.PNAME_MAKE_JITSI_DEFAULT_IM_APPLICATION,
                    "plugin.addrbook.DEFAULT_IM_APP"));
        }

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
        final JCheckBox checkBox = new SIPCommCheckBox(AddrBookActivator
            .getResources().getI18NString(
                labelNameKey),
                AddrBookActivator.getConfigService().getBoolean(configPropName,
                                                                true));
        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);

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
     * Creates the default IM application check box.
     *
     * @return the default IM application check box.
     */
    private Component createDefaultIMApplicationCheckBox(
        final String configPropName, String labelNameKey)
    {
        final JCheckBox checkBox = new SIPCommCheckBox(AddrBookActivator
            .getResources().getI18NString(
                labelNameKey),
                AddrBookActivator.getConfigService().getBoolean(configPropName,
                                                                false));
        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        checkBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                AddrBookActivator.getConfigService().setProperty(
                    configPropName,
                    new Boolean(checkBox.isSelected()).toString());

                if (checkBox.isSelected())
                    AddrBookActivator.setAsDefaultIMApplication();
                else
                    AddrBookActivator.unsetDefaultIMApplication();
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

        JPanel prefixPanel = new TransparentPanel();
        prefixPanel.setLayout(new BoxLayout(prefixPanel, BoxLayout.X_AXIS));

        prefixPanel.add(prefixLabel);
        prefixPanel.add(Box.createHorizontalStrut(10));
        prefixPanel.add(prefixField);
        prefixPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

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
