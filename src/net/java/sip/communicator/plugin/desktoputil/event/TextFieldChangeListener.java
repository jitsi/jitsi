/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.event;

/**
 * The <tt>TextFieldChangeListener</tt> listens for any changes in the text
 * contained in a <tt>SIPCommTextField</tt>. It is notified every time a char
 * is inserted or removed from the field.
 *
 * @author Yana Stamcheva
 */
public interface TextFieldChangeListener
{
    /**
     * Indicates that a text has been removed from the text field.
     */
    public void textRemoved();

    /**
     * Indicates that a text has been inserted to the text field.
     */
    public void textInserted();
}
