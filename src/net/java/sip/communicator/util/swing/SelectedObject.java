/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import javax.swing.*;

/**
 * A convenience class used to store combobox complex objects.
 * The <tt>SelectedObject</tt> is used for all account and status combo boxes
 * throughout this gui implementation.
 * 
 * @author Yana Stamcheva
 */
public class SelectedObject
{
    private String text;

    private Icon icon;

    private Object object;

    /**
     * Creates an instance of <tt>SelectedObject</tt> by specifying the text,
     * icon and object associated with it.
     * 
     * @param text The text.
     * @param icon The icon.
     * @param object The object.
     */
    public SelectedObject(String text, Icon icon, Object object)
    {
        this.text = text;
        this.icon = icon;
        this.object = object;
    }

    /**
     * Creates an instance of <tt>SelectedObject</tt> by specifying the
     * icon and object associated with it.
     * 
     * @param icon The icon.
     * @param object The object.
     */
    public SelectedObject(Icon icon, Object object)
    {
        this.icon = icon;
        this.object = object;
    }

    /**
     * Returns the text of this <tt>SelectedObject</tt>.
     * @return the text of this <tt>SelectedObject</tt>.
     */
    public String getText()
    {
        return text;
    }

    /**
     * Returns the icon of this <tt>SelectedObject</tt>.
     * @return the icon of this <tt>SelectedObject</tt>.
     */
    public Icon getIcon()
    {
        return icon;
    }

    /**
     * Returns the real object behind this <tt>SelectedObject</tt>.
     * @return the real object behind this <tt>SelectedObject</tt>.
     */
    public Object getObject()
    {
        return object;
    }
}
