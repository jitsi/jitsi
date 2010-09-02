/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.skinmanager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.zip.ZipFile;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * 
 * @author Yana Stamcheva
 * @author Adam Netcony
 */
public class NewBundleDialog
        extends SIPCommDialog
        implements ActionListener
{
    private static final long serialVersionUID = 7638976584338100969L;

    /**
     * The object used for logging.
     */
    private Logger logger = Logger.getLogger(NewBundleDialog.class);

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

    private JTable skinTable;

    /**
     * Creates an instance of <tt>NewBundleDialog</tt>.
     * @param table the skin table
     */
    public NewBundleDialog(JTable table)
    {
        skinTable = table;

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
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();

        if (sourceButton.equals(installButton))
        {
            if (bundlePathField.getText().length() > 0)
            {
                try
                {
                    File jar = null;
                    try
                    {
                        jar = Resources.getResources()
                            .prepareSkinBundleFromZip(
                                new File(bundlePathField.getText()));
                    }
                    catch (Exception ex)
                    {
                        logger.info("Failed to load skin from zip.", ex);

                        SkinManagerActivator.getUIService().getPopupDialog()
                            .showMessagePopupDialog(ex.getMessage(), "Error",
                                PopupDialog.ERROR_MESSAGE);
                    }

                    if (jar != null)
                    {
                        try
                        {
                            Bundle newBundle = SkinManagerActivator
                                .bundleContext.installBundle(
                                    jar.toURI().toURL().toString());

                            for (int i = 0;
                                    i < skinTable.getModel().getRowCount(); i++)
                            {
                                try
                                {
                                    ((Bundle) skinTable.getModel()
                                        .getValueAt(i, 0)).stop();
                                }
                                catch (BundleException ex)
                                {
                                    logger.info("Failed to stop bundle.", ex);
                                }
                            }
                            newBundle.start();
                        }
                        catch (MalformedURLException ex)
                        {
                            logger.info("Failed to load skin from zip.", ex);
                        }
                    }
                }
                catch (BundleException ex)
                {
                    logger.info("Failed to install bundle.", ex);
                    SkinManagerActivator.getUIService().getPopupDialog()
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

            chooser.addFilter(new SipCommFileFilter()
            {
                @Override
                public boolean accept(File f)
                {
                    if (f.isDirectory())
                        return true;

                    boolean good = true;
                    try
                    {
                        ZipFile zip = new ZipFile(f);
                    }
                    catch (IOException ex)
                    {
                        good = false;
                    }

                    if (!f.getName().toLowerCase().endsWith(".zip"))
                    {
                        good = false;
                    }
                    return good;
                }

                @Override
                public String getDescription()
                {
                    return "Zip files (*.zip)";
                }
            });

            File newBundleFile = chooser.getFileFromDialog();

            if (newBundleFile != null)
            {
                try
                {
                    bundlePathField.setText(newBundleFile.getCanonicalPath());
                }
                catch (Exception ex)
                {
                    bundlePathField.setText(newBundleFile.getAbsolutePath());
                }
            }
        }
        else
        {
            dispose();
        }
    }

    /**
     * Presses programatically the cancel button, when Esc key is pressed.
     * @param isEscaped indicates if the Esc button was pressed on close
     */
    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }
}
