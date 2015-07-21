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

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.osgi.framework.*;

/**
 * The panel containing all buttons for the <tt>PluginManagerConfigForm</tt>.
 *
 * @author Yana Stamcheva
 */
public class ManageButtonsPanel
    extends TransparentPanel
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private Logger logger = Logger.getLogger(ManageButtonsPanel.class);

    /**
     * The deactivate skin button.
     */
    private JButton deactivateButton = new JButton(
            Resources.getString("service.gui.DEACTIVATE"));

    /**
     * The activate skin button.
     */
    private JButton activateButton = new JButton(
            Resources.getString("service.gui.ACTIVATE"));

    /**
     * The uninstall button.
     */
    private JButton uninstallButton = new JButton(
            Resources.getString("plugin.pluginmanager.UNINSTALL"));

    /**
     * The update button.
     */
    private JButton updateButton = new JButton(
            Resources.getString("plugin.pluginmanager.UPDATE"));

    /**
     * The new button.
     */
    private JButton newButton
        = new JButton(Resources.getString("plugin.pluginmanager.NEW"));

    /**
     * The panel, containing all buttons.
     */
    private JPanel buttonsPanel
        = new TransparentPanel(new GridLayout(0, 1, 8, 8));

    /**
     * The plugins table.
     */
    private JTable pluginTable;

    /**
     * Creates an instance of <tt>ManageButtonsPanel</tt>.
     * @param pluginTable the table of bundles
     */
    public ManageButtonsPanel(JTable pluginTable)
    {
        this.pluginTable = pluginTable;

        this.setLayout(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        this.newButton.setOpaque(false);
        this.activateButton.setOpaque(false);
        this.deactivateButton.setOpaque(false);
        this.uninstallButton.setOpaque(false);
        this.updateButton.setOpaque(false);

        this.buttonsPanel.add(newButton);
        this.buttonsPanel.add(activateButton);
        this.buttonsPanel.add(deactivateButton);
        this.buttonsPanel.add(uninstallButton);
        this.buttonsPanel.add(updateButton);

        this.add(buttonsPanel, BorderLayout.NORTH);

        this.newButton.addActionListener(this);
        this.activateButton.addActionListener(this);
        this.deactivateButton.addActionListener(this);
        this.uninstallButton.addActionListener(this);
        this.updateButton.addActionListener(this);

        //default as nothing is selected
        defaultButtonState();
    }

    /**
     * Performs corresponding operations when a button is pressed.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();

        if(sourceButton.equals(newButton))
        {
            NewBundleDialog dialog = new NewBundleDialog();

            dialog.pack();
            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2
                        - dialog.getWidth()/2,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2
                        - dialog.getHeight()/2
                    );

            dialog.setVisible(true);
        }
        else if(sourceButton.equals(activateButton))
        {
            int[] selectedRows = pluginTable.getSelectedRows();

            for (int i = 0; i < selectedRows.length; i++)
            {
                try
                {
                    ((Bundle)pluginTable.getModel()
                            .getValueAt(selectedRows[i], 0)).start();
                }
                catch (BundleException ex)
                {
                    logger.error("Failed to activate bundle.", ex);

                    PluginManagerActivator.getUIService().getPopupDialog()
                        .showMessagePopupDialog(ex.getMessage(), "Error",
                            PopupDialog.ERROR_MESSAGE);
                }
            }

            defaultButtonState();
        }
        else if(sourceButton.equals(deactivateButton))
        {
            int[] selectedRows = pluginTable.getSelectedRows();

            for (int i = 0; i < selectedRows.length; i++)
            {
                try
                {
                    ((Bundle)pluginTable.getModel()
                            .getValueAt(selectedRows[i], 0)).stop();
                }
                catch (BundleException ex)
                {
                    logger.error("Failed to desactivate bundle.", ex);

                    PluginManagerActivator.getUIService().getPopupDialog()
                        .showMessagePopupDialog(ex.getMessage(), "Error",
                        PopupDialog.ERROR_MESSAGE);
                }
            }

            defaultButtonState();
        }
        else if(sourceButton.equals(uninstallButton))
        {
            int[] selectedRows = pluginTable.getSelectedRows();

            for (int i = selectedRows.length - 1; i >= 0; i--)
            {
                try
                {
                    ((Bundle)pluginTable.getModel()
                            .getValueAt(selectedRows[i], 0)).uninstall();
                }
                catch (BundleException ex)
                {
                    logger.error("Failed to uninstall bundle.", ex);

                    PluginManagerActivator.getUIService().getPopupDialog()
                        .showMessagePopupDialog(ex.getMessage(), "Error",
                        PopupDialog.ERROR_MESSAGE);
                }
            }

            defaultButtonState();
        }
        else if(sourceButton.equals(updateButton))
        {
            int[] selectedRows = pluginTable.getSelectedRows();

            for (int i = 0; i < selectedRows.length; i++)
            {
                try
                {
                    ((Bundle)pluginTable.getModel()
                            .getValueAt(selectedRows[i], 0)).update();
                }
                catch (BundleException ex)
                {
                    logger.error("Failed to update bundle.", ex);

                    PluginManagerActivator.getUIService().getPopupDialog()
                    .showMessagePopupDialog(ex.getMessage(), "Error",
                        PopupDialog.ERROR_MESSAGE);
                }
            }

            // update button deselects bundles, revert buttons to defautl state
            defaultButtonState();
        }
    }

    /**
     * Default state of buttons, as nothing is selected
     */
    public void defaultButtonState()
    {
        enableActivateButton(false);
        enableDeactivateButton(false);
        enableUninstallButton(false);
        enableUpdateButton(false);
    }

    /**
     * Enable or disable the activate button.
     *
     * @param enable TRUE - to enable the activate button, FALSE - to disable it
     */
    public void enableActivateButton(boolean enable)
    {
        this.activateButton.setEnabled(enable);
    }

    /**
     * Enable or disable the deactivate button.
     *
     * @param enable TRUE - to enable the deactivate button, FALSE - to
     * disable it
     */
    public void enableDeactivateButton(boolean enable)
    {
        this.deactivateButton.setEnabled(enable);
    }

    /**
     * Enable or disable the uninstall button.
     *
     * @param enable TRUE - to enable the uninstall button, FALSE - to
     * disable it
     */
    public void enableUninstallButton(boolean enable)
    {
        this.uninstallButton.setEnabled(enable);
    }

    /**
     * Enable or disable the update button.
     *
     * @param enable TRUE - to enable the update button, FALSE - to disable it
     */
    public void enableUpdateButton(boolean enable)
    {
        this.updateButton.setEnabled(enable);
    }
}
