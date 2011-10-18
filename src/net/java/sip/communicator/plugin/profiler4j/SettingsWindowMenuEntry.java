/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.profiler4j;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.sf.profiler4j.console.*;

/**
 * Menu entry for the profiler plug-in
 *
 * @author Vladimir Skarupelov
 */
public class SettingsWindowMenuEntry
    extends AbstractPluginComponent
{
    private static final String PROFILER_NAME = "plugin.profiler.PLUGIN_NAME";
    private JMenuItem settingsMenuEntry;

    public SettingsWindowMenuEntry(Container container)
    {
        super(container);

        settingsMenuEntry = new JMenuItem(Resources.getString( PROFILER_NAME ));
        settingsMenuEntry.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Prefs prefs = new Prefs();

                System.setProperty("swing.aatext",
                        String.valueOf(prefs.isAntialiasing()));

                final Console app = new Console(prefs);
                app.connect();
                MainFrame f = new MainFrame(app);
                app.setMainFrame(f);
                f.pack();
                f.setVisible(true);
            }
        });
    }

    public Object getComponent()
    {
        return settingsMenuEntry;
    }

    public String getName()
    {
        return Resources.getString( PROFILER_NAME );
    }
}
