/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added in the user interface
 * configuration window.
 *
 * @author Damien Roth
 */
public class AutoAwayConfigurationPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private JCheckBox enable;

    private JSpinner timer;

    /**
     * Create an instance of <tt>StatusConfigForm</tt>
     */
    public AutoAwayConfigurationPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel pnlSection
            = GeneralConfigPluginActivator.createConfigSectionComponent(
                    Resources.getString("service.gui.STATUS"));

        pnlSection.add(createMainPanel());
        add(pnlSection);

        initValues();
    }

    /**
     * Init the main panel.
     * @return the created component
     */
    private Component createMainPanel()
    {
        ResourceManagementService resources
            = GeneralConfigPluginActivator.getResources();

        enable
            = new SIPCommCheckBox(
                    resources.getI18NString(
                            "plugin.autoaway.ENABLE_CHANGE_STATUS"));
        enable.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        timer.setEnabled(enable.isSelected());
                        saveData();
                    }
                });

        // Spinner
        timer
            = new JSpinner(
                    new SpinnerNumberModel(
                            Preferences.DEFAULT_TIMER,
                            1,
                            180,
                            1));
        timer.addChangeListener(
                new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        saveData();
                    }
                });

        JPanel timerPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

        // Text
        timerPanel.add(
                new JLabel(
                        resources.getI18NString(
                                "plugin.autoaway.AWAY_MINUTES")));
        timerPanel.add(timer);

        try
        {
            // changes that are valid will be saved immediately while typing
            ((DefaultFormatter)((JSpinner.DefaultEditor)timer.getEditor())
                .getTextField().getFormatter()).setCommitsOnValidEdit(true);
        }
        catch(Throwable t)
        {}

        JPanel mainPanel = new TransparentPanel(new BorderLayout(5, 5));

        mainPanel.add(enable, BorderLayout.NORTH);
        mainPanel.add(timerPanel, BorderLayout.WEST);

        return mainPanel;
    }

    /**
     * Init the values of the widgets
     */
    private void initValues()
    {
        boolean enabled = Preferences.isEnabled();

        this.enable.setSelected(enabled);
        this.timer.setEnabled(enabled);

        this.timer.setValue(Preferences.getTimer());
    }

    /**
     * Save data in the configuration file
     */
    private void saveData()
    {
        Preferences.saveData(enable.isSelected(), timer.getValue().toString());
    }
}
