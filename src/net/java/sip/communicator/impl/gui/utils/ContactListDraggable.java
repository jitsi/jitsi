/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * <tt>ContactListDraggable</tt> is a representation of an element
 * currently dragged over the contact list.
 */
public class ContactListDraggable
{
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
     * A visual <tt>Component</tt> related to the <tt>Contact</tt> or
     * <tt>MetaContact</tt> which we are moving.
     */
    private Component component;

    /**
     * Location of the image representing this <tt>ContactListDraggable</tt>
     */
    private Point location;

    /**
     * Image used to give a visual feedback of the DnD operation.
     */
    private Image image;

    /**
     * Creates a new instance of ContactListDraggable
     */
    public ContactListDraggable(MetaContact metaContact, Contact contact,
            Component component)
    {
        setMetaContact(metaContact);
        setContact(contact);
        setComponent(component);
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

    private void setMetaContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

    /**
     * Obtain the <tt>Contact</tt> associated with this
     * <tt>ContactListDraggable</tt>
     *
     * @return contact
     */
    public Contact getContact() {
        return contact;
    }

    private void setContact(Contact contact)
    {
        this.contact = contact;
    }

    /**
     * Obtain the <tt>Component</tt> associated with this
     * <tt>ContactListDraggable</tt>
     *
     * @return component
     */
    public Component getComponent()
    {
        return component;
    }

    private void setComponent(Component component) {
        this.component = component;
    }

    /**
     * @return location
     */
    public Point getLocation()
    {
        return location;
    }

    public void setLocation(Point p)
    {
        location = p;
    }

    /**
     * @return image
     */
    public Image getImage()
    {
        return image;
    }

    public void setImage(Image image)
    {
        this.image = image;
    }
}

