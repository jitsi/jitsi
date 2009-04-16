/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    private Logger logger = Logger.getLogger(ManageButtonsPanel.class);

    private JButton deactivateButton = new JButton(
            Resources.getString("service.gui.DEACTIVATE"));

    private JButton activateButton = new JButton(
            Resources.getString("service.gui.ACTIVATE"));

    private JButton uninstallButton = new JButton(
            Resources.getString("plugin.pluginmanager.UNINSTALL"));

    private JButton updateButton = new JButton(
            Resources.getString("plugin.pluginmanager.UPDATE"));

    private JButton newButton
        = new JButton(Resources.getString("plugin.pluginmanager.NEW"));

    private JCheckBox showSysBundlesCheckBox = new SIPCommCheckBox(
            Resources.getString("plugin.pluginmanager.SHOW_SYSTEM_BUNDLES"));

    private JPanel buttonsPanel =
        new TransparentPanel(new GridLayout(0, 1, 8, 8));

    private JTable pluginTable;

    public ManageButtonsPanel(JTable pluginTable)
    {
        this.pluginTable = pluginTable;

        this.setLayout(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        //Obtains previously saved value for the showSystemBundles check box.
        String showSystemBundlesProp = PluginManagerActivator
            .getConfigurationService().getString(
            "net.java.sip.communicator.plugin.pluginManager.showSystemBundles");

        if(showSystemBundlesProp != null)
        {
            boolean isShowSystemBundles
                = new Boolean(showSystemBundlesProp).booleanValue();

            this.showSysBundlesCheckBox.setSelected(isShowSystemBundles);

            ((PluginTableModel)pluginTable.getModel())
                .setShowSystemBundles(isShowSystemBundles);
        }

        this.showSysBundlesCheckBox
            .addChangeListener(new ShowSystemBundlesChangeListener());

        this.buttonsPanel.add(newButton);
        this.buttonsPanel.add(activateButton);
        this.buttonsPanel.add(deactivateButton);
        this.buttonsPanel.add(uninstallButton);
        this.buttonsPanel.add(updateButton);
        this.buttonsPanel.add(showSysBundlesCheckBox);

        this.add(buttonsPanel, BorderLayout.NORTH);

        this.newButton.addActionListener(this);
        this.activateButton.addActionListener(this);
        this.deactivateButton.addActionListener(this);
        this.uninstallButton.addActionListener(this);
        this.updateButton.addActionListener(this);

        //default as nothing is selected
        enableActivateButton(false);
        enableDeactivateButton(false);
        enableUninstallButton(false);
    }

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
        }
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
     * Adds all system bundles to the bundles list when the check box is
     * selected and removes them when user deselect it.
     */
    private class ShowSystemBundlesChangeListener implements ChangeListener
    {
        private boolean currentValue = false;

        public ShowSystemBundlesChangeListener()
        {
            currentValue = showSysBundlesCheckBox.isSelected();
        }

        public void stateChanged(ChangeEvent e)
        {
            if (currentValue == showSysBundlesCheckBox.isSelected())
            {
                return;
            }
            currentValue = showSysBundlesCheckBox.isSelected();
            //Save the current value of the showSystemBundles check box.
            PluginManagerActivator.getConfigurationService().setProperty(
                "net.java.sip.communicator.plugin.pluginManager.showSystemBundles",
                new Boolean(showSysBundlesCheckBox.isSelected()));

            PluginTableModel tableModel
                = (PluginTableModel)pluginTable.getModel();

            tableModel.setShowSystemBundles(showSysBundlesCheckBox.isSelected());

            tableModel.update();

            // as this changes the selection to none, make the buttons
            // at defautl state
            enableActivateButton(false);
            enableDeactivateButton(false);
            enableUninstallButton(false);
        }
    }

    /**
     * Returns the current value of the "showSystemBundles" check box.
     * @return TRUE if the the "showSystemBundles" check box is selected,
     * FALSE - otherwise.
     */
    public boolean isShowSystemBundles()
    {
        return showSysBundlesCheckBox.isSelected();
    }
}
