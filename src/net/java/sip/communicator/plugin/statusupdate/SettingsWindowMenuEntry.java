package net.java.sip.communicator.plugin.statusupdate;

import java.awt.Dialog.*;
import java.awt.event.*;

import javax.swing.JMenuItem;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;

public class SettingsWindowMenuEntry implements PluginComponent
{

    private JMenuItem settingsMenuEntry = new JMenuItem(Resources
            .getString("menuEntry"));

    private Container container;

    public SettingsWindowMenuEntry(Container container)
    {
        settingsMenuEntry.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationDialog dialog = new ConfigurationDialog();
                dialog.pack();
                dialog.setModal(true);
                dialog.setVisible(true);

                StatusUpdateActivator.startThread();
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
        return Resources.getString("aboutMenuEntry");
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
}
