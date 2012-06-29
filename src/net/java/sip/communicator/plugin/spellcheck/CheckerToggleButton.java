/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.spellcheck;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Toggle-able button that sets the spell checker to be either enabled or
 * disabled.
 * 
 * @author Damian Johnson
 */
public class CheckerToggleButton
    implements PluginComponent
{
    private final JToggleButton toggleButton;

    CheckerToggleButton(final SpellChecker checker)
    {
        this.toggleButton =
            new JToggleButton(Resources.getImage("plugin.spellcheck.DISABLE"));
        this.toggleButton.setSelectedIcon(Resources
            .getImage("plugin.spellcheck.ENABLE"));
        this.toggleButton.setSelected(checker.isEnabled());
        this.toggleButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                checker.setEnabled(toggleButton.isSelected());
            }
        });
    }

    public Object getComponent()
    {
        return this.toggleButton;
    }

    public String getConstraints()
    {
        return Container.RIGHT;
    }

    public Container getContainer()
    {
        return Container.CONTAINER_CHAT_TOOL_BAR;
    }

    public String getName()
    {
        return "Spell Checker Toggle";
    }

    public int getPositionIndex()
    {
        return -1;
    }

    public boolean isNativeComponent()
    {
        return false;
    }

    public void setCurrentContact(MetaContact metaContact)
    {

    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {

    }

    public void setCurrentContact(Contact contact)
    {

    }
}
