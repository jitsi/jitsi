/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>WindowID</tt> wraps a string which is meant to point to an
 * application dialog, like per example a "Configuration" dialog or
 * "Add contact" dialog.
 *
 * @author Yana Stamcheva
 */
public class WindowID{

    private String dialogName;

    /**
     * Creates a new WindowID.
     * @param dialogName the name of the dialog
     */
    public WindowID(String dialogName){
        this.dialogName = dialogName;
    }

    /**
     * Get the ID.
     *
     * @return the ID
     */
    public String getID(){
        return this.dialogName;
    }
}
