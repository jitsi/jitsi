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

import org.osgi.framework.*;

public class NewBundleDialog
    extends JDialog
    implements ActionListener
{
    private JButton installButton = new JButton(Resources.getString("install"));
    
    private JButton cancelButton = new JButton(Resources.getString("cancel"));
    
    private JTextField bundlePathField = new JTextField();
    
    private JLabel bundlePathLabel = new JLabel(Resources.getString("url") + ": ");
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    
    private JPanel dataPanel = new JPanel(new BorderLayout(5, 5));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private JButton fileChooserButton = new JButton(Resources.getString("chooseFile")
        );
    
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
                        .installBundle(bundlePathField.getText(), null);                    
                }
                catch (BundleException ex)
                {
                    ex.printStackTrace();
                    PluginManagerActivator.getUIService().getPopupDialog()
                        .showMessagePopupDialog(ex.getMessage(), "Error",
                        PopupDialog.ERROR_MESSAGE);
                }
                finally
                {
                    dispose();
                }
            }
        }
        else if (sourceButton.equals(fileChooserButton))
        {
            JFileChooser fileChooser
                = new JFileChooser();

            int result
                = fileChooser.showOpenDialog(NewBundleDialog.this);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                try
                {
                    File newBundleFile = fileChooser.getSelectedFile();

                    bundlePathField.setText(newBundleFile.toURL().toString());
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
}
