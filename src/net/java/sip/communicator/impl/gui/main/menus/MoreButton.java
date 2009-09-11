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
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author Yana Stamcheva
 */
public class MoreButton
    extends JLabel
    implements  PluginComponent,
                MouseListener,
                FocusListener
{
    private boolean isMouseOver = false;

    private final JPopupMenu menu = new JPopupMenu();

    private final Map<String, Component> menuItemsTable =
        new Hashtable<String, Component>();

    public MoreButton()
    {
        super(new ImageIcon(ImageLoader.getImage(ImageLoader.MORE_BUTTON)),
            JLabel.CENTER);

        this.setVerticalTextPosition(SwingConstants.BOTTOM);
        this.setHorizontalTextPosition(SwingConstants.CENTER);

        this.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.MORE"));

        this.addMouseListener(this);

        this.addFocusListener(this);
    }

    public Object getComponent()
    {
        return this;
    }

    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return Container.CONTAINER_MAIN_TOOL_BAR;
    }

    public String getName()
    {
        return this.getText();
    }

    public void setMouseOver(boolean isMouseOver)
    {
        this.isMouseOver = isMouseOver;
        this.repaint();
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
        requestFocus();

        if (!menu.isVisible())
        {
            Point locationOnScreen = getLocationOnScreen();

            menu.setLocation(
                locationOnScreen.x,
                locationOnScreen.y + getHeight());

            menu.setVisible(true);
        }
        else
            menu.setVisible(false);
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    /**
     * Specifies the position of this component in the container, where it
     * will be added.
     * @return 0 to indicate the first position in the container.
     */
    public int getPositionIndex()
    {
        return -1;
    }

    public void focusGained(FocusEvent arg0)
    {
    }

    public void focusLost(FocusEvent arg0)
    {
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
                    
                    menu.setVisible(false);
                }
            });

            this.menu.add(item);
            this.menuItemsTable.put(name, item);
        }
    }
    
    public void removeMenuItem(JComponent c)
    {
        String name = c.getToolTipText();
        Component item = this.menuItemsTable.get(name);

        if (item != null)
        {
            this.menu.remove(item);
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

    public boolean isNativeComponent()
    {
        return false;
    }
}