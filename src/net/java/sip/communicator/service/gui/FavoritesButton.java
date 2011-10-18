/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.gui;

/**
 * The <tt>FavoriteButton</tt> interface is meant to be used by plugins in order
 * to register their own components in the menu of favorites opened, by clicking
 * the arrow button above the contact list.
 * 
 * @author Yana Stamcheva
 */
public interface FavoritesButton
{
    /**
     * Returns the image to be set on the favorites button.
     * 
     * @return the image to be set on the favorites button.
     */
    public byte[] getImage();

    /**
     * Returns the text to be set to the favorites button.
     * 
     * @return the text to be set to the favorites button.
     */
    public String getText();

    /**
     * This method will be called when one clicks on the button.
     */
    public void actionPerformed();
}
