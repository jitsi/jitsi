/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>SIPCommMenu</tt> is very similar to a JComboBox. The main
 * component here is a JLabel only with an icon. When user clicks on the icon a
 * popup menu is opened, containing a list of icon-text pairs from which the
 * user could choose one item. When user selects the desired item, the icon of
 * the selected item is set to the main component label.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommMenu
    extends JMenu
{
    private Object selectedItem;

    private Object selectedObject;

    private boolean isMouseOver;

    /**
     * Creates an instance of <tt>SIPCommMenu</tt>.
     */
    public SIPCommMenu()
    {
        this.setUI(new SIPCommSelectorMenuUI());
    }

    /**
     * Creates an instance of <tt>SIPCommMenu</tt> by specifying the
     * initialy selected item.
     * 
     * @param text The item that is initialy selected.
     */
    public SIPCommMenu(String text)
    {
        super(text);
    }
    
    /**
     * Adds an item to the "choice list" of this selector box.
     * 
     * @param text The text of the item.
     * @param icon The icon of the item.
     * @param actionListener The <tt>ActionListener</tt>, which handles the
     * case, when the item is selected.
     */
    public void addItem(String text, Icon icon, ActionListener actionListener)
    {
        JMenuItem item = new JMenuItem(text, icon);

        item.addActionListener(actionListener);

        this.add(item);
    }

    /**
     * Selects the given item.
     * 
     * @param o The object to select.
     * @param icon The icon to select.
     */
    public void setSelected(Object o, ImageIcon icon)
    {
        this.setIcon(icon);
        this.setSelectedObject(o);
    }

    /**
     * Selects the given object.
     * 
     * @param o The <tt>Object</tt> to select.
     */
    public void setSelectedObject(Object o)
    {
        this.selectedObject = o;
    }

    /**
     * Returns the selected object.
     * 
     * @return the selected object.
     */
    public Object getSelectedObject()
    {
        return this.selectedObject;
    }

    /**
     * Sets the isMouseOver property value and repaints this component.
     * 
     * @param isMouseOver <code>true</code> to indicate that the mouse is over
     * this component, <code>false</code> - otherwise.
     */
    public void setMouseOver(boolean isMouseOver)
    {
        this.isMouseOver = isMouseOver;
        this.repaint();
    }

    /**
     * Overwrites the <tt>paintComponent(Graphics g)</tt> method in order to
     * provide a new look and the mouse moves over this component.
     */
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);

        super.paintComponent(g2);

        g2.setStroke(new BasicStroke(1.5f));

        g2.setColor(new Color(0x646464));

        if (isMouseOver)
            g.drawRoundRect(0, 0, this.getWidth() - 1,
                            this.getHeight() - 3, 5, 5);
    }
}
