/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import javax.swing.JOptionPane;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.service.gui.PopupDialog;

/**
 * Implements <code>PopupDialog</code>.
 * 
 * @author Yana Stamcheva
 */
public class PopupDialogImpl extends JOptionPane
    implements PopupDialog {

    private MainFrame parentWindow;
    
    public PopupDialogImpl(MainFrame parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    public String showInputPopupDialog(Object message) {
        return showInputDialog(parentWindow, message);
    }
    
    public String showInputPopupDialog(Object message, 
            String initialSelectionValue) {
        return showInputDialog(parentWindow, message, 
                initialSelectionValue);
    }    
    
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

    public void showMessagePopupDialog(Object message) {
        showMessageDialog(parentWindow, message);
    }

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

    public int showConfirmPopupDialog(Object message) {
        return showConfirmDialog(parentWindow, message);
    }

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

    public boolean isDialogVisible() {
        return this.isVisible();
    }

    public void showDialog() {
        this.setVisible(true);
    }

    public void hideDialog() {
        this.setVisible(false);
    }

    public void resizeDialog(int width, int height) {
        this.setSize(width, height);
    }

    public void moveDialog(int x, int y) {
        this.setLocation(x, y);
    }    
}
