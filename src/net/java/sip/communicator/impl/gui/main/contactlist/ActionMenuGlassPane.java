/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
 
public class ActionMenuGlassPane
    extends TransparentPanel
{
    MainFrame mainFrame = GuiActivator.getUIService().getMainFrame();

    /**
     * Layout each of the components in this JLayeredPane so that they all fill
     * the entire extents of the layered pane -- from (0,0) to (getWidth(),
     * getHeight())
     */
    @Override
    public void doLayout()
    {
        // Synchronizing on getTreeLock, because I see other layouts doing that.
        // see BorderLayout::layoutContainer(Container)
        synchronized(getTreeLock())
        {
            ContactListPane contactListPane = mainFrame.getContactListPanel();

            Point p = SwingUtilities.convertPoint(
                contactListPane, 0, 0, mainFrame.getRootPane());

            int x = (int) p.getX();
            int y = (int) p.getY();
            int w = contactListPane.getWidth();
            int h = contactListPane.getHeight();

            for(Component c : getComponents())
            {
                c.setBounds(x, y, w, h);
            }
        }
    }
}