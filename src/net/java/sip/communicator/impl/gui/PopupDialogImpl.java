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
public class PopupDialogImpl extends JOptionPane
    implements PopupDialog {

    private MainFrame parentWindow;
    
    /**
     * Creates an instance of <tt>PopupDialogImpl</tt>.
     * @param parentWindow The main application window.
     */
    public PopupDialogImpl(MainFrame parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    /**
     * Implements the <tt>PopupDialog.showInputPopupDialog(Object)</tt> method.
     * Invokes the corresponding <tt>JOptionPane.showInputDialog</tt> method.
     */
    public String showInputPopupDialog(Object message) {
        return showInputDialog(parentWindow, message);
    }
    
    /**
     * Implements the <tt>PopupDialog.showInputPopupDialog(Object, String)</tt>
     * method. Invokes the corresponding <tt>JOptionPane.showInputDialog</tt>
     * method.
     */
    public String showInputPopupDialog(Object message, 
            String initialSelectionValue) {
        return showInputDialog(parentWindow, message, 
                initialSelectionValue);
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
        return (String)showInputDialog(parentWindow, message,
                title, type);
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
        
       return showInputDialog(parentWindow, message, title, type, 
               null, selectionValues, initialSelectionValue);
    }

    /**
     * Implements the <tt>PopupDialog.showMessagePopupDialog(Object)</tt>
     * method. Invokes the corresponding
     * <tt>JOptionPane.showMessageDialog</tt> method.
     */
    public void showMessagePopupDialog(Object message) {
        showMessageDialog(parentWindow, message);
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
        
        showMessageDialog(parentWindow, message, title, type);
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object)</tt>
     * method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     */
    public int showConfirmPopupDialog(Object message) {
        return showConfirmDialog(parentWindow, message);
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
        
        return showConfirmDialog(parentWindow, message, title, type);
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
        
        return showConfirmDialog(parentWindow, message, title,
                optType, msgType);
    }

    /**
     * Implements the <tt>PopupDialog.isDialogVisible</tt> method.
     * @return <code>true</code> if the dialog is visible, <code>false</code>
     * otherwise.
     */
    public boolean isDialogVisible() {
        return this.isVisible();
    }

    /**
     * Implements the <tt>PopupDialog.isDialogVisible</tt> method.
     * Shows this <tt>JOptionPane</tt>.
     */
    public void showDialog() {
        this.setVisible(true);
    }

    /**
     * Implements the <tt>PopupDialog.isDialogVisible</tt> method.
     * Hides this <tt>JOptionPane</tt>.
     */
    public void hideDialog() {
        this.setVisible(false);
    }

    /**
     * Implements the <tt>PopupDialog.isDialogVisible</tt> method.
     * Resizes this <tt>JOptionPane</tt>.
     */
    public void resizeDialog(int width, int height) {
        this.setSize(width, height);
    }

    /**
     * Implements the <tt>PopupDialog.isDialogVisible</tt> method.
     * Moves this <tt>JOptionPane</tt>.
     */
    public void moveDialog(int x, int y) {
        this.setLocation(x, y);
    }

    public void minimize()
    {}

    public void maximize()
    {}    
}
