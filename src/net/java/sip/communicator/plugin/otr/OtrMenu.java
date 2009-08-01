package net.java.sip.communicator.plugin.otr;

import java.awt.Component;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;

@SuppressWarnings("serial")
public class OtrMenu
    extends JMenu
    implements PluginComponent, ActionListener
{
    private static final String imageID = "plugin.otr.DECRYPTED_ICON";

    private ResourceManagementService resourceService;

    public OtrMenu(ResourceManagementService resourceService)
    {
        super("Encryption");
        this.setToolTipText("Options for OTR Encryption");

        if (resourceService != null)
            this.setIcon(resourceService.getImage(imageID));

        // TODO Internationalize Strings...
        JMenuItem mitmStartOtr = new JMenuItem("Start Private Conversation");
        JMenuItem mitmEndOtr = new JMenuItem("End Private Conversation");
        JMenuItem mitmRefreshOtr =
            new JMenuItem("Refresh Private Conversation");
        JMenuItem mitmAuthenticateBuddy = new JMenuItem("Authenticate Buddy");
        JMenuItem mitmWhatsThis = new JMenuItem("What's this?");

        // Shown if we don't have an OTR session.
        this.add(mitmStartOtr);
        // Shown if we have an OTR session.
        this.add(mitmEndOtr);
        this.add(mitmRefreshOtr);
        this.add(mitmAuthenticateBuddy);
        this.add(mitmWhatsThis);
    }

//    @Override
    public String getConstraints()
    {
        return null;
    }

    public Component getComponent()
    {
        return this;
    }

//    @Override
    public Container getContainer()
    {
        return Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU;
    }

//    @Override
    public int getPositionIndex()
    {
        return -1;
    }

//    @Override
    public boolean isNativeComponent()
    {
        return false;
    }

    private MetaContact metaContact;

//    @Override
    public void setCurrentContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

//    @Override
    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {
    }

//    @Override
    public void actionPerformed(ActionEvent e)
    {
    }

}