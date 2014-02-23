/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import net.java.sip.communicator.util.skin.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * The <tt>ExtendedPopupMenu</tt> is the dialog shown to let the user choose
 * from several options.
 *
 * @author Damian Minkov
 */
public class ExtendedPopupMenu
    extends SIPCommPopupMenu
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The invoker component.
     */
    private final JComponent invoker;
 
    /**
     * Creates this dialog by specifying a list of items to choose from.
     *
     * @param invoker the invoker of this pop up
     * @param title
     * @param menuItems the list of items to select through
     */
    public ExtendedPopupMenu(JComponent invoker,
                             String title,
                             List<JMenuItem> menuItems)
    {
        if (invoker != null)
            this.invoker = invoker;
        else
            this.invoker = new JPanel();

        this.init(title);

        for (JMenuItem menuItem : menuItems)
        {
            this.add(menuItem);
        }
    }

    /**
     * Initializes and add some common components.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     */
    private void init(String infoString)
    {
        setInvoker(invoker);

        if(infoString != null)
        {
            this.add(createInfoLabel(infoString));

            this.addSeparator();
        }

        this.setFocusable(true);
    }

    /**
     * Shows the dialog at the given location.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void showPopupMenu(int x, int y)
    {
        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Shows this popup menu regarding to its invoker location.
     */
    public void showPopupMenu()
    {
        Point location = new Point(invoker.getX(),
            invoker.getY() + invoker.getHeight());

        SwingUtilities
            .convertPointToScreen(location, invoker.getParent());
        setLocation(location);
        setVisible(true);
    }

    /**
     * Creates the info label.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     * @return the created info label
     */
    private Component createInfoLabel(String infoString)
    {
        JMenuItem infoLabel = new JMenuItem();

        infoLabel.setEnabled(false);
        infoLabel.setFocusable(false);

        infoLabel.setText("<html><b>" + infoString + "</b></html>");

        return infoLabel;
    }

    /**
     * Reloads all menu items.
     */
    public void loadSkin()
    {
        Component[] components = getComponents();
        for(Component component : components)
        {
            if(component instanceof Skinnable)
            {
                Skinnable skinnableComponent = (Skinnable) component;
                skinnableComponent.loadSkin();
            }
        }
    }
}
