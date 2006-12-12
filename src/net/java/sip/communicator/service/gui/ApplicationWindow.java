/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * A window that could be shown, hidden, resized, moved, etc. Meant to be used
 * from other services to show an application window, like for example a 
 * "Configuration" or "Add contact" window.
 * 
 * @author Yana Stamcheva
 */
public interface ApplicationWindow {

    /**
     * Returns TRUE if the window is visible and FALSE otherwise.
     * 
     * @return <code>true</code> if the window is visible and
     * <code>false</code> otherwise.
     */
    public boolean isVisible();
    
    /**
     * Shows the window.
     */
    public void show();
    
    /**
     * Hides the window.
     */
    public void hide();
    
    /**
     * Resizes the window with the given width and height.
     * 
     * @param width The new width.
     * @param height The new height.
     */
    public void resize(int width, int height);
    
    /**
     * Moves the window to the given coordinates.
     * 
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void move(int x, int y);
    
    /**
     * Minimizes the window.
     */
    public void minimize();
    
    /**
     * Maximizes the window.
     */
    public void maximize();
}
