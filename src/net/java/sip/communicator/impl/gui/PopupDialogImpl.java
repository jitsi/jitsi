/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;

/**
 * Implements <code>PopupDialog</code> interface. 
 * 
 * @author Yana Stamcheva
 */
public class PopupDialogImpl
    extends JOptionPane
    implements PopupDialog
{    
    /**
     * Creates an instance of <tt>PopupDialogImpl</tt>.
     */
    public PopupDialogImpl()
    {
    }
    
    /**
     * Implements the <tt>PopupDialog.showInputPopupDialog(Object)</tt> method.
     * Invokes the corresponding <tt>JOptionPane.showInputDialog</tt> method.
     */
    public String showInputPopupDialog(Object message) {
        return showInputDialog(message);
    }
    
    /**
     * Implements the <tt>PopupDialog.showInputPopupDialog(Object, String)</tt>
     * method. Invokes the corresponding <tt>JOptionPane.showInputDialog</tt>
     * method.
     */
    public String showInputPopupDialog(Object message, 
            String initialSelectionValue) {
        return showInputDialog(message, initialSelectionValue);
    }    
    
    /**
     * Implements the 
     * <tt>PopupDialog.showInputPopupDialog(Object, String, int)</tt> method.
     * Invokes the corresponding <tt>JOptionPane.showInputDialog</tt> method.
     */
    public String showInputPopupDialog(Object message, String title,
            int messageType) {
        
        int type;
        if (messageType == PopupDialog.ERROR_MESSAGE) {
            type = JOptionPane.ERROR_MESSAGE;
        }
        else if (messageType == PopupDialog.INFORMATION_MESSAGE) {
            type = JOptionPane.INFORMATION_MESSAGE;
        }
        else if (messageType == PopupDialog.QUESTION_MESSAGE) {
            type = JOptionPane.QUESTION_MESSAGE;
        }
        else if (messageType == PopupDialog.WARNING_MESSAGE) {
            type = JOptionPane.WARNING_MESSAGE;
        }
        else {
            type = JOptionPane.PLAIN_MESSAGE;
        }
        return (String)showInputDialog(null, message, title, type);
    }

    /**
     * Implements the 
     * <tt>PopupDialog.showInputPopupDialog(Object, String, int, Object[],
     * Object)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showInputDialog</tt> method.
     */
    public Object showInputPopupDialog(Object message, String title,
            int messageType, Object[] selectionValues,
            Object initialSelectionValue) {
        
        int type;
        if (messageType == PopupDialog.ERROR_MESSAGE) {
            type = JOptionPane.ERROR_MESSAGE;
        }
        else if (messageType == PopupDialog.INFORMATION_MESSAGE) {
            type = JOptionPane.INFORMATION_MESSAGE;
        }
        else if (messageType == PopupDialog.QUESTION_MESSAGE) {
            type = JOptionPane.QUESTION_MESSAGE;
        }
        else if (messageType == PopupDialog.WARNING_MESSAGE) {
            type = JOptionPane.WARNING_MESSAGE;
        }
        else {
            type = JOptionPane.PLAIN_MESSAGE;
        }
        
       return showInputDialog(null, message, title, type, 
               null, selectionValues, initialSelectionValue);
    }

    /**
     * Implements the <tt>PopupDialog.showMessagePopupDialog(Object)</tt>
     * method. Invokes the corresponding
     * <tt>JOptionPane.showMessageDialog</tt> method.
     */
    public void showMessagePopupDialog(Object message) {
        showMessageDialog(null, message);
    }

    /**
     * Implements the <tt>PopupDialog.showMessagePopupDialog(Object, String,
     * int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showMessageDialog</tt> method.
     */
    public void showMessagePopupDialog(Object message, String title,
            int messageType) {
        int type;
        if (messageType == PopupDialog.ERROR_MESSAGE) {
            type = JOptionPane.ERROR_MESSAGE;
        }
        else if (messageType == PopupDialog.INFORMATION_MESSAGE) {
            type = JOptionPane.INFORMATION_MESSAGE;
        }
        else if (messageType == PopupDialog.QUESTION_MESSAGE) {
            type = JOptionPane.QUESTION_MESSAGE;
        }
        else if (messageType == PopupDialog.WARNING_MESSAGE) {
            type = JOptionPane.WARNING_MESSAGE;
        }
        else {
            type = JOptionPane.PLAIN_MESSAGE;
        }
        
        showMessageDialog(null, message, title, type);
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object)</tt>
     * method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     */
    public int showConfirmPopupDialog(Object message) {
        return showConfirmDialog(null, message);
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object, String, 
     * int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     */
    public int showConfirmPopupDialog(Object message, String title,
            int optionType) {
        int type;
        if (optionType == PopupDialog.OK_CANCEL_OPTION) {
            type = JOptionPane.OK_CANCEL_OPTION;
        }       
        else if (optionType == PopupDialog.YES_NO_OPTION) {
            type = JOptionPane.YES_NO_OPTION;
        }
        else if (optionType == PopupDialog.YES_NO_CANCEL_OPTION) {
            type = JOptionPane.YES_NO_CANCEL_OPTION;
        }
        else {
            type = JOptionPane.DEFAULT_OPTION;
        }
        
        return showConfirmDialog(null, message, title, type);
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object, String, 
     * int, int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     */
    public int showConfirmPopupDialog(Object message, String title,
            int optionType, int messageType) {
        
        int optType;
        if (optionType == PopupDialog.OK_CANCEL_OPTION) {
            optType = JOptionPane.OK_CANCEL_OPTION;
        }       
        else if (optionType == PopupDialog.YES_NO_OPTION) {
            optType = JOptionPane.YES_NO_OPTION;
        }
        else if (optionType == PopupDialog.YES_NO_CANCEL_OPTION) {
            optType = JOptionPane.YES_NO_CANCEL_OPTION;
        }
        else {
            optType = JOptionPane.DEFAULT_OPTION;
        }
        
        int msgType;
        if (messageType == PopupDialog.ERROR_MESSAGE) {
            msgType = JOptionPane.ERROR_MESSAGE;
        }
        else if (messageType == PopupDialog.INFORMATION_MESSAGE) {
            msgType = JOptionPane.INFORMATION_MESSAGE;
        }
        else if (messageType == PopupDialog.QUESTION_MESSAGE) {
            msgType = JOptionPane.QUESTION_MESSAGE;
        }
        else if (messageType == PopupDialog.WARNING_MESSAGE) {
            msgType = JOptionPane.WARNING_MESSAGE;
        }
        else {
            msgType = JOptionPane.PLAIN_MESSAGE;
        }
        
        return showConfirmDialog(null, message, title,
                optType, msgType);
    }
    
    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
     */
    public WindowID getIdentifier()
    {
        return WINDOW_GENERAL_POPUP;
    }

    /**
     * Implements the <tt>ExportedWindow.isFocused()</tt> method. Returns TRUE
     * if this dialog is the focus owner, FALSE - otherwise.
     */
    public boolean isFocused()
    {
        return super.isFocusOwner();
    }

    /**
     * Implements the <tt>ExportedWindow.bringToFront()</tt> method. Brings this
     * window to front.
     */
    public void bringToFront()
    {
        this.requestFocusInWindow();
    }
    
    /**
     * This dialog could not be minimized.
     */
    public void minimize()
    {
    }

    /**
     * This dialog could not be maximized.
     */
    public void maximize()
    {   
    }

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }
}
