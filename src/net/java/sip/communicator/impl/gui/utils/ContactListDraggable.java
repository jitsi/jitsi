/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * <tt>ContactListDraggable</tt> is a representation of an element
 * currently dragged over the contact list. A <tt>ContactListDraggable</tt>
 * gives information on what we are dragging : it could be a <tt>MetaContact</tt>
 * or a <tt>Contact</tt> inside a <tt>MetaContact</tt>. So, a valid <tt>MetaContact</tt>
 * parameter is mandatory for a <tt>ContactListDraggable</tt> while the
 * <tt>Contact</tt> could be null.
 *
 * The class also provides additional information such as an image which can be
 * used to provide a visual feedback of the dnd operation.
 */
public class ContactListDraggable extends JComponent
{
    /**
     * Logger for this class.
     */
    private static Logger logger =
            Logger.getLogger(ContactListDraggable.class.getName());

    /**
     * Source <tt>MetaContact</tt> in the current move.
     */
    private MetaContact metaContact;

    /**
     * The spefic <tt>Contact</tt> moved from the <tt>MetaContact</tt>, if any.
     * null otherwise
     */
    private Contact contact;

    /**
     * Location of the image representing this <tt>ContactListDraggable</tt>.
     */
    private Point location;

    /**
     * Image used to give a visual feedback of the DnD operation.
     */
    private Image image;

    /**
     * The <tt>ContacList</tt> where this drag'n drop operation takes place.
     */
    private ContactList contactList;

    /**
     * A set of 2 cursors indicating if the dragged element is currently
     * located over the contactlist or not.
     */
    private static Hashtable cursors = new Hashtable(2);
    static {
        try 
        {
            cursors.put("valid", Cursor.getSystemCustomCursor("MoveDrop.32x32"));
        }
        catch (Exception ex)
        {
            logger.debug("Cursor \"MoveDrop.32x32\" isn't available " +
                "will use Cursor.MOVE_CURSOR instead", ex);
            cursors.put("valid", Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        try 
        {
            cursors.put("invalid", Cursor.getSystemCustomCursor("Invalid.32x32"));
        }
        catch (Exception ex)
        {
            logger.debug("Cursor \"Invalid.32x32\" isn't available " +
                "will use Cursor.WAIT_CURSOR instead", ex);
            cursors.put("invalid", Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }
    
    /**
     * Creates a new instance of ContactListDraggable which is a represantation
     * of an object currently dragged over the contactlist.
     *
     * @param metaContact the <tt>MetaContact</tt> which we drag.
     * @param contact specific <tt>Contact</tt> concerned within this
     *        <tt>MetaContact</tt> can be null if we are moving the whole
     *        <tt>MetaContact</tt>.
     * @param image an image used to give a visual feedback of 
     *        the drag operation.
     */
    public ContactListDraggable(ContactList contactList,
        MetaContact metaContact, Contact contact, Image image)
    {
        setOpaque(false);
        
        this.contactList = contactList;
        
        setMetaContact(metaContact);
        setContact(contact);
        setImage(image);
    }

    /**
     * Obtain the <tt>MetaContact</tt> associated with this
     * <tt>ContactListDraggable</tt>
     *
     * @return metacontact
     */
    public MetaContact getMetaContact()
    {
        return metaContact;
    }

    /**
     * Set the <tt>MetaContact</tt> associated with this
     * <tt>ContactListDraggable</tt>
     *
     * @param metaContact the <tt>MetaContact</tt> from wich we start dragging
     */
    private void setMetaContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

    /**
     * Obtain the <tt>Contact</tt> associated with this
     * <tt>ContactListDraggable</tt> (could be null)
     *
     * @return contact
     */
    public Contact getContact() {
        return contact;
    }

    /**     
     * Set the <tt>Contact</tt> associated with this <tt>ContactListDraggable</tt>.
     * Could be null if we are dragging a whole <tt>MetaContact</tt>.
     * If not null, this <tt>Contact</tt> <b>is</b> a subcontact of the
     * <tt>MetaContact</tt> involved in the dragging operation
     * 
     * @param contact the specific <tt>Contact</tt> wich we are moving
     */
    private void setContact(Contact contact)
    {
        this.contact = contact;
    }

    /**
     * Method <tt>getLocation</tt> gives the current coordinates of the object
     * we are dragging.
     *
     * @return location a <tt>Point</tt> reprensenting the actual coordinates
     * of the moved object
     */
    public Point getLocation()
    {
        return location;
    }

    /**
     * Updates the location of the object currently dragged over the
     * <tt>ContactList</tt>
     *
     * @param point the current coordinates of the dragged object.
     */
    public void setLocation(Point point)
    {
        location = point;
    }

    /**
     * Return the image representing the dragged object.
     *
     * @return image the image representing the dragged object.
     */
    public Image getImage()
    {
        return image;
    }

    /**
     * An image used to give a visual feedback of the dragging operation by
     * painting it at the current location of the dragged object.
     * In case we drag a <tt>MetaContact</tt>, this image is simply
     * made of the <tt>JLabel<tt> containing the <tt>MetaContact</tt>. If
     * we draw only a <tt>Contact</tt>, the image will be the protocol icon
     * associated to that contact.
     * (eg: a bulb if we are dragging a jabber contact)
     *
     * @param image an image representing the dragged object
     */
    private void setImage(Image image)
    {
        this.image = image;
    }

    /**
     * Paint the image associated to this contact list draggable
     * and choose a cursor indicating if we can currently do a drop or not.
     *
     * @param g graphics for this component
     */
    public void paintComponent(Graphics g)
    {
        // make the dragged element "transparent" with a coef. of 0.8
        // and paint it
        Graphics2D g2 = (Graphics2D) g;
        g2.setComposite(AlphaComposite.
            getInstance(AlphaComposite.SRC_OVER, 0.8f));
        
        g2.drawImage(
            image,
            (int) (location.getX() - image.getWidth(null) / 2),
            (int) (location.getY() - image.getHeight(null) / 2),
            null);
        
        g2.setColor(Color.GRAY);
        g2.drawRect((int) (location.getX() - image.getWidth(null) / 2),
                    (int) (location.getY() - image.getHeight(null) / 2),
                    image.getWidth(null),
                    image.getHeight(null));
        
        // check if we are located over the contact list
        Point p = SwingUtilities.convertPoint(this, location, contactList);
        if (contactList.contains(p))
        {
            setCursor((Cursor) cursors.get("valid"));
        }
        else
        {
            setCursor((Cursor) cursors.get("invalid"));
        }
    }
}

