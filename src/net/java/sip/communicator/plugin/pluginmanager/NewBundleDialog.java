/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

public class NewBundleDialog
    extends SIPCommDialog
    implements ActionListener
{
    private JButton installButton
        = new JButton(Resources.getString("plugin.pluginmanager.INSTALL"));
    
    private JButton cancelButton
        = new JButton(Resources.getString("service.gui.CANCEL"));
    
    private JTextField bundlePathField = new JTextField();
    
    private JLabel bundlePathLabel
        = new JLabel(Resources.getString("plugin.pluginmanager.URL") + ": ");
    
    private JPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
    
    private JPanel dataPanel = new TransparentPanel(new BorderLayout(5, 5));
    
    private JPanel mainPanel = new TransparentPanel(new BorderLayout());
    
    private JButton fileChooserButton = new JButton(
        Resources.getString("plugin.pluginmanager.CHOOSE_FILE"));
    
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
                    ex.printStackTrace();
                    PluginManagerActivator.getUIService().getPopupDialog()
                        .showMessagePopupDialog(ex.getMessage(), "Error",
                        PopupDialog.ERROR_MESSAGE);
                }
                catch (Throwable ex)
                {
                    ex.printStackTrace();
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
            		null, "New bundle...");

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
                    ex.printStackTrace();
                }
            }
        }   
        else
            dispose();
    }

    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }
}
