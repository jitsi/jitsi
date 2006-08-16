/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>DialogID</tt> wraps a string which is meant to point to an
 * application dialog, like per example a "Configuration" dialog or
 * "Add contact" dialog.
 *
 * @author Yana Stamcheva
 */
public class DialogID{

    private String dialogName;

    public DialogID(String dialogName){
        this.dialogName = dialogName;
    }

    public String getID(){
        return this.dialogName;
    }
}
