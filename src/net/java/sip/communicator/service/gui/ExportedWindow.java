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
public interface ExportedWindow
{
    /*
     * WindowID-s
     */    
    public static final WindowID ADD_CONTACT_WINDOW
        = new WindowID("AddContactWindow");
    
    public static final WindowID ABOUT_WINDOW
        = new WindowID("AboutWindow");
    
    public static final WindowID CHAT_WINDOW
        = new WindowID("ChatWindow");
    
    public static final WindowID CONFIGURATION_WINDOW
        = new WindowID("ConfigurationWindow");
    
    public static final WindowID AUTHENTICATION_WINDOW
        = new WindowID("AuthenticationWindow");

    public static final WindowID MAIN_WINDOW
        = new WindowID("MainWindow");

    /**
     * Returns the WindowID corresponding to this window. The window id should
     * be one of the defined in this class XXX_WINDOW constants.
     *   
     * @return the WindowID corresponding to this window
     */
    public WindowID getIdentifier();
    
    /**
     * Returns TRUE if the component is visible and FALSE otherwise.
     * 
     * @return <code>true</code> if the component is visible and
     * <code>false</code> otherwise.
     */
    public boolean isVisible();
    
    /**
     * Returns TRUE if this component is currently the focused component,
     * FALSE - otherwise.
     * @return TRUE if this component is currently the focused component,
     * FALSE - otherwise.
     */
    public boolean isFocused();
    
    /**
     * Shows or hides this component.
     */
    public void setVisible(boolean isVisible);
    
    /**
     * Brings the focus to this window.
     */
    public void bringToFront();
    
    /**
     * Resizes the window with the given width and height.
     * 
     * @param width The new width.
     * @param height The new height.
     */
    public void setSize(int width, int height);
    
    /**
     * Moves the window to the given coordinates.
     * 
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void setLocation(int x, int y);
    
    /**
     * Minimizes the window.
     */
    public void minimize();
    
    /**
     * Maximizes the window.
     */
    public void maximize();
    
    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource();
}
