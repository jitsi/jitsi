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
package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 *
 * @author Marin Dzhigarov
 */
public class NewPropertyDialog
    extends SIPCommDialog
    implements ActionListener
{

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    private ConfigurationService confService = PropertiesEditorActivator
        .getConfigurationService();

    private ResourceManagementService resourceManagementService =
        PropertiesEditorActivator.getResourceManagementService();

    /**
     * The ok button.
     */
    private JButton okButton = new JButton(
        resourceManagementService.getI18NString("service.gui.OK"));

    /**
     * The cancel button.
     */
    private JButton cancelButton = new JButton(
        resourceManagementService.getI18NString("service.gui.CANCEL"));

    /**
     * The property name text field.
     */
    private JTextField propertyNameTextField = new JTextField();

    /**
     * The property value text field.
     */
    private JTextField propertyValueTextField = new JTextField();

    /**
     * The property name label.
     */
    private JLabel propertyNameLabel = new JLabel(
        resourceManagementService.getI18NString("service.gui.NAME") + ": ");

    /**
     * The property value label.
     */
    private JLabel propertyValueLabel = new JLabel(
        resourceManagementService.getI18NString("service.gui.VALUE") + ": ");

    /**
     * The panel containing all buttons.
     */
    private JPanel buttonsPanel = new TransparentPanel(new FlowLayout(
        FlowLayout.CENTER));

    /**
     * The panel containing the property value and name panels.
     */
    private JPanel dataPanel = new TransparentPanel(new GridBagLayout());

    /**
     * Creates an instance of <tt>NewPropertyDialog</tt>.
     */
    public NewPropertyDialog()
    {
        setTitle(resourceManagementService
            .getI18NString("plugin.propertieseditor.NEW_PROPERTY_TITLE"));
        JPanel fields = new TransparentPanel(new BorderLayout());
        fields.setPreferredSize(new Dimension(450, 150));

        this.getContentPane().add(fields);

        fields.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        fields.add(dataPanel, BorderLayout.NORTH);
        fields.add(buttonsPanel, BorderLayout.SOUTH);

        this.buttonsPanel.add(okButton);
        this.buttonsPanel.add(cancelButton);
        okButton.setEnabled(false);

        this.okButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        GridBagConstraints first = new GridBagConstraints();
        first.gridx = 0;
        first.gridy = 0;
        first.weightx = 0;
        first.anchor = GridBagConstraints.LINE_START;
        first.gridwidth = 1;
        first.insets = new Insets(2, 4, 2, 4);
        first.fill = GridBagConstraints.HORIZONTAL;

        GridBagConstraints second = new GridBagConstraints();
        second.gridx = 1;
        second.gridy = 0;
        second.weightx = 2;
        second.anchor = GridBagConstraints.LINE_START;
        second.gridwidth = 1; // GridBagConstraints.REMAINDER;
        second.insets = first.insets;
        second.fill = GridBagConstraints.HORIZONTAL;

        dataPanel.add(propertyNameLabel, first);
        dataPanel.add(propertyNameTextField, second);

        first.gridy = ++second.gridy;

        dataPanel.add(propertyValueLabel, first);
        dataPanel.add(propertyValueTextField, second);

        propertyNameTextField.getDocument().addDocumentListener(
            new DocumentListener()
            {
                public void insertUpdate(DocumentEvent e)
                {
                    okButton.setEnabled(true);
                }

                public void removeUpdate(DocumentEvent e)
                {
                    if (propertyNameTextField.getText().length() == 0)
                        okButton.setEnabled(false);
                }

                public void changedUpdate(DocumentEvent e)
                {
                }
            });
    }

    /**
     * Performs corresponding actions, when a button is pressed.
     * 
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();

        if (sourceButton.equals(okButton))
        {
            confService.setProperty(propertyNameTextField.getText(),
                propertyValueTextField.getText());
        }
        dispose();
    }

    /**
     * Presses programmatically the cancel button, when Esc key is pressed.
     * 
     * @param isEscaped indicates if the Esc button was pressed on close
     */
    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }

}
