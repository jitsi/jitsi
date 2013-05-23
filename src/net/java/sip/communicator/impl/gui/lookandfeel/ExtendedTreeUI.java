/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.event.*;

import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ExtendedTreeUI</tt> is an extended implementation of the
 * <tt>SIPCommTreeUI</tt> specific for the gui implementation.
 */
public class ExtendedTreeUI
    extends SIPCommTreeUI
{
    /**
     * Do not select the <tt>ShowMoreContact</tt>.
     *
     * @param path the <tt>TreePath</tt> to select
     * @param event the <tt>MouseEvent</tt> that provoked the select
     */
    @Override
    protected void selectPathForEvent(TreePath path, MouseEvent event)
    {
        Object lastComponent = path.getLastPathComponent();

        // Open right button menu when right mouse is pressed.
        if (lastComponent instanceof ContactNode)
        {
            UIContact uiContact
                = ((ContactNode) lastComponent).getContactDescriptor();

            if (!(uiContact instanceof ShowMoreContact))
            {
                super.selectPathForEvent(path, event);
            }
        }
        else
            super.selectPathForEvent(path, event);
    }
}
