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
package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.osgi.framework.*;

/**
 * @author Yana Stamcheva
 */
public class NewBundleDialog
    extends SIPCommDialog
    implements ActionListener
{
    /**
     * The object used for logging.
     */
    private Logger logger = Logger.getLogger(NewBundleDialog.class);

    private static final long serialVersionUID = 7638976584338100969L;

    /**
     * The install button.
     */
    private JButton installButton
        = new JButton(Resources.getString("plugin.pluginmanager.INSTALL"));

    /**
     * The cancel button.
     */
    private JButton cancelButton
        = new JButton(Resources.getString("service.gui.CANCEL"));

    /**
     * The bundle path field.
     */
    private JTextField bundlePathField = new JTextField();

    /**
     * The bundle path label.
     */
    private JLabel bundlePathLabel
        = new JLabel(Resources.getString("plugin.pluginmanager.URL") + ": ");

    /**
     * The panel, containing all buttons.
     */
    private JPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    /**
     * The panel containing new bundle information.
     */
    private JPanel dataPanel = new TransparentPanel(new BorderLayout(5, 5));

    /**
     * The main panel, where all other panels are added.
     */
    private JPanel mainPanel = new TransparentPanel(new BorderLayout());

    /**
     * The button, from which to choose a file from the file system.
     */
    private JButton fileChooserButton = new JButton(
        Resources.getString("plugin.pluginmanager.CHOOSE_FILE"));

    /**
     * Creates an instance of <tt>NewBundleDialog</tt>.
     */
    public NewBundleDialog ()
    {
        this.mainPanel.setPreferredSize(new Dimension(450, 150));

        this.getContentPane().add(mainPanel);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        this.mainPanel.add(dataPanel, BorderLayout.NORTH);

        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.buttonsPanel.add(installButton);
        this.buttonsPanel.add(cancelButton);

        this.installButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        this.fileChooserButton.addActionListener(this);
        this.fileChooserButton.setOpaque(false);

        this.dataPanel.add(bundlePathLabel, BorderLayout.WEST);

        this.dataPanel.add(bundlePathField, BorderLayout.CENTER);

        this.dataPanel.add(fileChooserButton, BorderLayout.EAST);
    }

    /**
     * Performs corresponding actions, when a buttons is pressed.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed (ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();

        if (sourceButton.equals(installButton))
        {
            if (bundlePathField.getText().length() > 0)
            {
                try
                {
                    PluginManagerActivator.bundleContext
                        .installBundle(bundlePathField.getText());
                }
                catch (BundleException ex)
                {
                    logger.info("Failed to install bundle.", ex);
                    PluginManagerActivator.getUIService().getPopupDialog()
                        .showMessagePopupDialog(ex.getMessage(), "Error",
                        PopupDialog.ERROR_MESSAGE);
                }
                catch (Throwable ex)
                {
                    logger.info("Failed to install bundle.", ex);
                }
                finally
                {
                    dispose();
                }
            }
        }
        else if (sourceButton.equals(fileChooserButton))
        {
            SipCommFileChooser chooser = GenericFileDialog.create(
                null, "New bundle...",
                SipCommFileChooser.LOAD_FILE_OPERATION);

            File newBundleFile
                = chooser.getFileFromDialog();

            if (newBundleFile != null)
            {
                try
                {
                    bundlePathField.setText(newBundleFile.toURI()
                               .toURL().toString());
                }
                catch (MalformedURLException ex)
                {
                    logger.info("Failed parse URL.", ex);
                }
            }
        }
        else
            dispose();
    }

    /**
     * Presses programatically the cancel button, when Esc key is pressed.
     * @param isEscaped indicates if the Esc button was pressed on close
     */
    @Override
    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }
}
