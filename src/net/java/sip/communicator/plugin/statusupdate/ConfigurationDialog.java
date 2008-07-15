/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.statusupdate;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.configuration.*;

/**
 * The configuration Dialog for the Mail Notification Plugin
 * 
 * @author Thomas Hofer
 * 
 */
public class ConfigurationDialog extends JDialog
{
    /**
     * 
     */
    private static final long serialVersionUID = -3850044618335728627L;

    private JCheckBox enable;
    private JSpinner timer;

    /**
     * Default Constructor
     */
    public ConfigurationDialog()
    {
        super();
        init();
        initValues();

        getContentPane().setPreferredSize(new Dimension(400, 200));
        getContentPane().setLayout(new GridLayout(1, 1));

        // move window to middle of screen
        setLocationRelativeTo(null);
        
        // Set title
        setTitle(Resources.getString("menuEntry"));
        
        // Set closing system
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                dispose();
            }
        });
    }

    /**
     * Initialize the ui-components
     */
    private void init()
    {
        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Description
        JTextArea infoLabel = new JTextArea(Resources.getString("infotext"));
        infoLabel.setBorder(BorderFactory.createTitledBorder(Resources
                .getString("info")));
        infoLabel.setEditable(false);
        infoLabel.setWrapStyleWord(true);
        infoLabel.setLineWrap(true);

        // Checkbox
        enable = new JCheckBox(Resources.getString("enable"));
        enable.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                timer.setEnabled(enable.isSelected());
            }
        });

        // Spinner
        timer = new JSpinner(new SpinnerNumberModel(15, 1, 180, 1));

        // Button panel : OK and Cancel button
        JPanel okCancelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton(Resources.getString("ok"));
        ok.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                saveData();
                dispose();
            }
        });
        JButton cancel = new JButton(Resources.getString("cancel"));
        cancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });

        okCancelPanel.add(ok);
        okCancelPanel.add(cancel);

        GridBagConstraints mainGBC = new GridBagConstraints();
        mainGBC.gridx = 0;
        mainGBC.gridy = 0;
        mainGBC.weightx = 1;
        mainGBC.fill = GridBagConstraints.BOTH;
        mainGBC.anchor = GridBagConstraints.NORTHWEST;
        mainGBC.weighty = 1;
        mainGBC.gridwidth = 3;

        mainPanel.add(infoLabel, mainGBC);

        mainGBC.fill = GridBagConstraints.HORIZONTAL;
        mainGBC.gridwidth = 1;
        mainGBC.gridy++;
        mainGBC.weightx = 1;
        mainGBC.weighty = 0;
        mainGBC.gridx = 0;
        mainPanel.add(enable, mainGBC);

        mainGBC.weightx = 0;
        mainGBC.gridx++;
        mainPanel.add(timer, mainGBC);
        mainGBC.weightx = 1;
        mainGBC.gridx++;
        mainPanel.add(new JLabel(Resources.getString("minutes")), mainGBC);

        mainGBC.gridwidth = 3;
        mainGBC.gridx = 0;
        mainGBC.gridy++;
        mainGBC.weighty = 0;
        mainPanel.add(okCancelPanel, mainGBC);

        this.getContentPane().add(mainPanel);
    }

    /**
     * (Re-)Initializes the values of the settings dependent on the selected
     * account
     */
    private void initValues()
    {
        ConfigurationService configService = StatusUpdateActivator
                .getConfigService();

        String e = (String) configService.getProperty(Preferences.ENABLE);
        if (e != null)
        {
            try
            {
                enable.setSelected(Boolean.parseBoolean(e));
                timer.setEnabled(Boolean.parseBoolean(e));
            } catch (NumberFormatException ex)
            {
                enable.setSelected(false);
                timer.setEnabled(false);
            }
        }
        else
        {
            enable.setSelected(false);
            timer.setEnabled(false);
        }

        String t = (String) configService.getString(Preferences.TIMER);
        if (t != null)
        {
            try
            {
                timer.setValue(Integer.parseInt(t));
            }
            catch (NumberFormatException ex)
            {
            }
        }
    }
    
    private void saveData()
    {
        ConfigurationService configService = StatusUpdateActivator
            .getConfigService();
        
        configService.setProperty(Preferences.ENABLE, Boolean
                .toString(enable.isSelected()));
        Integer interval = (Integer) timer.getValue();
        configService.setProperty(Preferences.TIMER, interval);
    }
}
