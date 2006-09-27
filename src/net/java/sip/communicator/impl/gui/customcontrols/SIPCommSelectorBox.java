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

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>SIPCommSelectorBox</tt> is very similar to a JComboBox. The main
 * component here is a JLabel only with an icon. When user clicks on the
 * icon a popup menu is opened, containing a list of icon-text pairs from
 * which the user could choose one item. When user selects the desired item,
 * the icon of the selected item is set to the main component label.
 *  
 * @author Yana Stamcheva
 */
public class SIPCommSelectorBox extends JLabel
    implements MouseListener {

    private JPopupMenu popup;

    private Object selectedItem;

    private Image backgroundImage = ImageLoader
            .getImage(ImageLoader.STATUS_SELECTOR_BOX);

    private Object selectedObject;

    /**
     * Creates an instance of <tt>SIPCommSelectorBox</tt>.
     */
    public SIPCommSelectorBox() {
        this.setPreferredSize(new Dimension(
                this.backgroundImage.getWidth(this), this.backgroundImage
                        .getHeight(this)));

        this.setVerticalAlignment(JLabel.CENTER);

        this.setHorizontalAlignment(JLabel.CENTER);

        this.popup = new JPopupMenu();

        this.popup.setInvoker(this);

        this.addMouseListener(this);
    }

    /**
     * Creates an instance of <tt>SIPCommSelectorBox</tt> by specifying
     * the initialy selected item.
     * @param selectedItem The item that is initialy selected.
     */
    public SIPCommSelectorBox(Object selectedItem) {
        this();

        this.selectedItem = selectedItem;

        if (selectedItem instanceof Image)
            this.setIcon(new ImageIcon((Image) selectedItem));
        else
            this.setText(selectedItem.toString());
    }

    /**
     * Adds an item to the "choice list" of this selector box.
     * @param text The text of the item.
     * @param icon The icon of the item.
     * @param actionListener The <tt>ActionListener</tt>, which handles
     * the case, when the item is selected.
     */
    public void addItem(String text, Icon icon, 
                        ActionListener actionListener) {

        JMenuItem item = new JMenuItem(text, icon);

        item.addActionListener(actionListener);

        this.popup.add(item);
    }

    /**
     * Calculates the "choice list" popup location depending on the
     * main label coordinates.
     * 
     * @return The <tt>Point</tt> where the popup should be shown.
     */
    public Point calculatePopupLocation() {

        Component component = this;
        Point point = new Point();
        int x = this.getX();
        int y = this.getY();

        while (component.getParent() != null) {

            component = component.getParent();

            x += component.getX();
            y += component.getY();
        }

        point.x = x;
        point.y = y + this.getHeight();

        return point;
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JLabel</tt>
     * to provide a different look for the main label component.
     */
    protected void paintComponent(Graphics g) {

        AntialiasingManager.activateAntialiasing(g);

        g.drawImage(this.backgroundImage, 0, 0, this);

        super.paintComponent(g);
    }

    /**
     * Shows the "choice list" popup window on a mouse click over
     * the main label component.
     */
    public void mouseClicked(MouseEvent e) {

        if (!this.popup.isVisible()) {
            this.popup.setLocation(calculatePopupLocation());
            this.popup.setVisible(true);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Selects the given item.
     * @param text The object to select.
     * @param icon The icon to select.
     */
    public void setSelected(String text, Icon icon) {
        this.setIcon(icon);
        this.setSelectedObject(text);
    }

    /**
     * Selects the given object.
     * @param o The <tt>Object</tt> to select.
     */
    public void setSelectedObject(Object o) {
        this.selectedObject = o;
    }

    /**
     * Returns the selected object.
     * @return the selected object.
     */
    public Object getSelectedObject() {
        return this.selectedObject;
    }

    /**
     * Returns the popup menu for this selector box.
     * @return the popup menu for this selector box.
     */
    public JPopupMenu getPopup() {
        return popup;
    }   
}
