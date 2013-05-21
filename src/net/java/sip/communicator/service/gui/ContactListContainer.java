/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>ContactListContainer</tt> is a container of a <tt>ContactList</tt>
 * component.
 *
 * @author Yana Stamcheva
 */
public interface ContactListContainer
{
    /**
     * Called when the ENTER key was typed when this container was the focused
     * container. Performs the appropriate actions depending on the current
     * state of the contained contact list.
     */
    public void enterKeyTyped();

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this
     * container was the focused container. Performs the appropriate actions
     * depending on the current state of the contained contact list.
     */
    public void ctrlEnterKeyTyped();

    /**
     * Returns <tt>true</tt> if this contact list container has the focus,
     * otherwise returns <tt>false</tt>.
     *
     * @return <tt>true</tt> if this contact list container has the focus,
     * otherwise returns <tt>false</tt>
     */
    public boolean isFocused();

    /**
     * Returns <tt>true</tt> if there's any currently selected menu related to
     * this <tt>ContactListContainer</tt>, <tt>false</tt> - otherwise.
     *
     * @return <tt>true</tt> if there's any currently selected menu related to
     * this <tt>ContactListContainer</tt>, <tt>false</tt> - otherwise
     */
    public boolean isMenuSelected();

    /**
     * Clears the current text in the search field.
     */
    public void clearCurrentSearchText();

    /**
     * Returns the current search text.
     *
     * @return the current search text
     */
    public String getCurrentSearchText();
}
