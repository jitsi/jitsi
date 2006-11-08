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
        
    /**
     * Creates an instance of <tt>SIPCommMenu</tt>.
     */
    public SIPCommMenu()
    {        
        this.setUI(new SIPCommSelectorMenuUI());
        this.setPreferredSize(new Dimension(28, 24));
    }

    /**
     * Creates an instance of <tt>SIPCommMenu</tt> by specifying the
     * initialy selected item.
     * 
     * @param selectedItem The item that is initialy selected.
     */
    public SIPCommMenu(Object selectedItem)
    {
        this.selectedItem = selectedItem;

        if (selectedItem instanceof Image)
            this.setIcon(new ImageIcon((Image) selectedItem));
        else
            this.setText(selectedItem.toString());
    }

    /**
     * Adds an item to the "choice list" of this selector box.
     * 
     * @param text The text of the item.
     * @param icon The icon of the item.
     * @param actionListener The <tt>ActionListener</tt>, which handles the
     *            case, when the item is selected.
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
}
