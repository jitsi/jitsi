/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.profiler4j;

/**
 * Menu entry for the profiler plug-in
 *
 * @author Vladimir Skarupelov
 */
import java.awt.event.*;
import javax.swing.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.sf.profiler4j.console.*;

public class SettingsWindowMenuEntry implements PluginComponent
{
    private static final String PROFILER_NAME = "plugin.profiler.PLUGIN_NAME";
    private JMenuItem settingsMenuEntry;
    private Container container;

    public SettingsWindowMenuEntry(Container container)
    {
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
        this.container = container;
    }

    public Object getComponent()
    {
        return settingsMenuEntry;
    }

    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return container;
    }

    public String getName()
    {
        return Resources.getString( PROFILER_NAME );
    }

    public void setCurrentContact(Contact contact)
    {
    }
    
    public void setCurrentContact(MetaContact metaContact)
    {
    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {
    }

    public int getPositionIndex()
    {
        return -1;
    }

    public boolean isNativeComponent() {
        return false;
    }
}
