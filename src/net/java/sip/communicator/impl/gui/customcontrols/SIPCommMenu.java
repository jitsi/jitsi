/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.event.*;

import javax.swing.*;

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
    private static final long serialVersionUID = 1L;
    private Object selectedObject;

    /**
     * Creates an instance of <tt>SIPCommMenu</tt>.
     */
    public SIPCommMenu()
    {
        super();
    }

    /**
     * Creates an instance of <tt>SIPCommMenu</tt> by specifying
     * the text and the icon.
     */
    public SIPCommMenu(String text, Icon defaultIcon)
    {
        super(text);

        this.setIcon(defaultIcon);
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
     * @param selectedObject The object to select.
     */
    public void setSelected(SelectedObject selectedObject)
    {
        if (selectedObject.getIcon() != null)
            this.setIcon(selectedObject.getIcon());

        if (selectedObject.getText() != null)
            this.setText(selectedObject.getText());

        if (selectedObject.getObject() != null)
            this.setSelectedObject(selectedObject.getObject());
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
        this.repaint();
    }
}
