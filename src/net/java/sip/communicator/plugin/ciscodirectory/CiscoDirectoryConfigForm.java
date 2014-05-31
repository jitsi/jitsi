/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ciscodirectory;

import static net.java.sip.communicator.plugin.ciscodirectory
        .CiscoDirectoryActivator.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

/**
 * Simple form to configure the Cisco Directory source.
 *
 * @author Fabien Cortina <fabien.cortina@gmail.com>
 */
final class CiscoDirectoryConfigForm implements ConfigurationForm
{
    private final DirectorySettings settings;

    private JPanel formPanel;
    private JCheckBox enabledBox;
    private JTextField urlField;
    private JTextField prefixField;

    /**
     * Creates a form to manage the setting passed in argument.
     *
     * @param settings the interface to the configuration.
     */
    CiscoDirectoryConfigForm(DirectorySettings settings)
    {
        this.settings = settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle()
    {
        return _txt("plugin.ciscodirectory.TITLE");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getIcon()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getForm()
    {
        if (formPanel == null)
        {
            formPanel = createForm();
        }
        return formPanel;
    }

    /**
     * Creates the UI elements and the form panel that contains them.
     */
    private JPanel createForm()
    {
        DocumentListener fieldListener = new FieldListener();

        enabledBox = new SIPCommCheckBox(_txt("plugin.ciscodirectory.ENABLED"));
        enabledBox.setSelected(settings.isEnabled());
        enabledBox.addActionListener(new CheckBoxListener());

        urlField = new JTextField();
        urlField.setText(settings.getDirectoryUrl());
        urlField.getDocument().addDocumentListener(fieldListener);

        prefixField = new JTextField();
        prefixField.setText(settings.getPrefix());
        prefixField.getDocument().addDocumentListener(fieldListener);

        JPanel body = new TransparentPanel();
        body.setLayout(new GridLayout(0, 2));
        body.add(enabledBox);
        body.add(new JLabel(""));
        body.add(new JLabel(_txt("plugin.ciscodirectory.DIRECTORY_URL")));
        body.add(urlField);
        body.add(new JLabel(_txt("plugin.ciscodirectory.PHONE_PREFIX")));
        body.add(prefixField);

        updateSensitivity();

        formPanel = new TransparentPanel(new BorderLayout());
        formPanel.add(body, BorderLayout.NORTH);
        return formPanel;
    }

    /**
     * Updates the sensitivity of the field, depending on whether or not the
     * contact source is enabled or not.
     */
    private void updateSensitivity()
    {
        urlField.setEnabled(settings.isEnabled());
        prefixField.setEnabled(settings.isEnabled());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndex()
    {
        return 3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAdvanced()
    {
        return true;
    }

    /**
     * Monitor the fields and updates the state stored in {@link #settings}.
     */
    private final class FieldListener implements DocumentListener
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void insertUpdate(DocumentEvent event)
        {
            changedUpdate(event);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void changedUpdate(DocumentEvent event)
        {
            settings.setDirectoryUrl(urlField.getText());
            settings.setPrefix(prefixField.getText());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeUpdate(DocumentEvent event)
        {
            changedUpdate(event);
        }
    }

    /**
     * Monitor the check box and updates the state stored in {@link #settings}.
     */
    private final class CheckBoxListener implements ActionListener
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            settings.setEnabled(enabledBox.isSelected());
            updateSensitivity();
        }
    }
}
