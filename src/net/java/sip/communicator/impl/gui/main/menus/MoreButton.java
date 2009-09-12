/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;

/**
 * @author Yana Stamcheva
 */
public class MoreButton
    extends AbstractPluginComponent
    implements FocusListener,
               MouseListener
{
    private JLabel label;

    private JPopupMenu menu;

    private final Map<String, Component> menuItemsTable =
        new Hashtable<String, Component>();

    public MoreButton()
    {
        super(Container.CONTAINER_MAIN_TOOL_BAR);
    }

    public Object getComponent()
    {
        if (label == null)
        {
            label
                = new JLabel(
                        new ImageIcon(
                                ImageLoader.getImage(ImageLoader.MORE_BUTTON)),
                        JLabel.CENTER);

            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setHorizontalTextPosition(SwingConstants.CENTER);

            label.setToolTipText(
                GuiActivator.getResources().getI18NString("service.gui.MORE"));

            label.addFocusListener(this);
            label.addMouseListener(this);
        }
        return label;
    }

    public String getName()
    {
        return "";
    }

    public void setMouseOver(boolean isMouseOver)
    {
        if (label != null)
            label.repaint();
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
        this.setMouseOver(true);
    }

    public void mouseExited(MouseEvent e)
    {
        this.setMouseOver(false);
    }

    public void mousePressed(MouseEvent e)
    {
        if (label != null)
            return;

        label.requestFocus();

        if (menu == null)
            menu = createMenu();
        if (!menu.isVisible())
        {
            Point locationOnScreen = label.getLocationOnScreen();

            menu.setLocation(
                locationOnScreen.x,
                locationOnScreen.y + label.getHeight());

            menu.setVisible(true);
        }
        else
            menu.setVisible(false);
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void focusGained(FocusEvent arg0)
    {
    }

    public void focusLost(FocusEvent arg0)
    {
        if (menu != null)
            menu.setVisible(false);
    }

    public void addMenuItem(final JComponent c)
    {
        String name = c.getToolTipText();

        if (!this.containsItem(name))
        {
            JMenuItem item = new JMenuItem(name);
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    MouseEvent mouseEvent
                        = new MouseEvent(c,
                            MouseEvent.MOUSE_PRESSED,
                            System.currentTimeMillis(),
                            MouseEvent.BUTTON1,
                            c.getX(),
                            c.getY(),
                            1,
                            false);

                    for (MouseListener l : c.getMouseListeners())
                    {
                        l.mousePressed(mouseEvent);
                        l.mouseReleased(mouseEvent);
                        l.mouseClicked(mouseEvent);
                    }

                    if (menu != null)
                        menu.setVisible(false);
                }
            });

            if (menu == null)
                menu = createMenu();
            menu.add(item);

            menuItemsTable.put(name, item);
        }
    }
    
    public void removeMenuItem(JComponent c)
    {
        String name = c.getToolTipText();
        Component item = this.menuItemsTable.get(name);

        if (item != null)
        {
            if (menu != null)
                menu.remove(item);
            menuItemsTable.remove(name);
        }
    }

    public int getItemsCount()
    {
        return menuItemsTable.size();
    }
    
    public boolean containsItem(String name)
    {
        return menuItemsTable.containsKey(name);
    }

    private JPopupMenu createMenu()
    {
        return new JPopupMenu();
    }
}
