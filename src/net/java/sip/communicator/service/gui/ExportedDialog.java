/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * A dialog that could be shown, hidden, resized or moved. Meant to be used
 * from other services to show an application window, like par example a 
 * "Configuration" or "Add contact" window.
 * 
 * @author Yana Stamcheva
 */
public interface ExportedDialog {

    /**
     * Returns TRUE if the dialog is visible and FALSE otherwise.
     * 
     * @return <code>true</code> if the dialog is visible and
     * <code>false</code> otherwise.
     */
    public boolean isDialogVisible();
    
    /**
     * Shows the dialog.
     */
    public void showDialog();
    
    /**
     * Hides the dialog.
     */
    public void hideDialog();
    
    /**
     * Resizes the dialog with the given width and height.
     * 
     * @param width The new width.
     * @param height The new height.
     */
    public void resizeDialog(int width, int height);
    
    /**
     * Moves the dialog to the given coordinates.
     * 
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void moveDialog(int x, int y);
}
