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

    public SettingsWindowMenuEntry(Container container,
                                   PluginComponentFactory parentFactory)
    {
        super(container, parentFactory);

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
