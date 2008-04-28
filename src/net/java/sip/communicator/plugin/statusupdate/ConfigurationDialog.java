/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.statusupdate;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;

import net.java.sip.communicator.service.configuration.ConfigurationService;

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
        setLocation(
                (Toolkit.getDefaultToolkit().getScreenSize().width - getPreferredSize().width) / 2,
                (Toolkit.getDefaultToolkit().getScreenSize().height - getPreferredSize().height) / 2);
    }

    /**
     * Initialize the ui-components
     */
    private void init()
    {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
            }
        });

        final ConfigurationService configService = StatusUpdateActivator
                .getConfigService();

        JPanel mainPanel = new JPanel();
        mainPanel.setForeground(Color.GRAY);
        mainPanel.setLayout(new GridBagLayout());

        JTextArea infoLabel = new JTextArea(Resources.getString("infotext"));
        infoLabel.setBorder(BorderFactory.createTitledBorder(Resources
                .getString("info")));
        infoLabel.setEditable(false);
        infoLabel.setWrapStyleWord(true);
        infoLabel.setLineWrap(true);

        enable = new JCheckBox(Resources.getString("enable"));
        enable.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                timer.setEnabled(enable.isSelected());
            }
        });

        timer = new JSpinner(new SpinnerNumberModel(15, 1, 180, 1));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        JPanel okCancelPanel = new JPanel();
        JButton ok = new JButton(Resources.getString("ok"));
        ok.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                configService.setProperty(Preferences.ENABLE, Boolean
                        .toString(enable.isSelected()));
                Integer interval = (Integer) timer.getValue();
                configService.setProperty(Preferences.TIMER, interval);
                setVisible(false);
            }
        });
        JButton cancel = new JButton(Resources.getString("cancel"));
        cancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        });

        okCancelPanel.add(ok);
        okCancelPanel.add(cancel);
        okCancelPanel.setBorder(BorderFactory.createTitledBorder(" "));

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
        } else
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
            } catch (NumberFormatException ex)
            {
            }
        }
    }
}
