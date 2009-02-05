/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import javax.swing.*;

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
    private static final long serialVersionUID = 0L;

	/**
     * Creates an instance of <tt>PopupDialogImpl</tt>.
     */
    public PopupDialogImpl()
    {
    }

    /**
     * Implements the <tt>PopupDialog.showInputPopupDialog(Object)</tt> method.
     * Invokes the corresponding <tt>JOptionPane.showInputDialog</tt> method.
     *
     * @param message the object to display
     */
    public String showInputPopupDialog(Object message)
    {
        return showInputDialog(message);
    }

    /**
     * Implements the <tt>PopupDialog.showInputPopupDialog(Object, String)</tt>
     * method. Invokes the corresponding <tt>JOptionPane.showInputDialog</tt>
     * method.
     *
     * @param message the message to display
     * @param initialSelectionValue the value used to initialize the input
     * field.
     */
    public String showInputPopupDialog(Object message,
            String initialSelectionValue)
    {
        return showInputDialog(message, initialSelectionValue);
    }

    /**
     * Implements the
     * <tt>PopupDialog.showInputPopupDialog(Object, String, int)</tt> method.
     * Invokes the corresponding <tt>JOptionPane.showInputDialog</tt> method.
     *
     * @param message the message to display
     * @param messageType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param title the String to display in the dialog title bar
     */
    public String showInputPopupDialog(Object message, String title,
        int messageType)
    {
        return (String) showInputDialog(null, message, title,
            popupDialog2JOptionPaneMessageType(messageType));
    }

    private static int popupDialog2JOptionPaneMessageType(int type)
    {
        switch (type) {
        case PopupDialog.ERROR_MESSAGE:
            return JOptionPane.ERROR_MESSAGE;
        case PopupDialog.INFORMATION_MESSAGE:
            return JOptionPane.INFORMATION_MESSAGE;
        case PopupDialog.QUESTION_MESSAGE:
            return JOptionPane.QUESTION_MESSAGE;
        case PopupDialog.WARNING_MESSAGE:
            return JOptionPane.WARNING_MESSAGE;
        default:
            return JOptionPane.PLAIN_MESSAGE;
        }
    }

    /**
     * Implements the
     * <tt>PopupDialog.showInputPopupDialog(Object, String, int, Object[],
     * Object)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showInputDialog</tt> method.
     *
     * @param message the message to display
     * @param messageType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param title the String to display in the dialog title bar
     * @param selectionValues an array of Objects that gives the possible
     * selections
     * @param initialSelectionValue the value used to initialize the input field
     */
    public Object showInputPopupDialog(Object message, String title,
        int messageType, Object[] selectionValues, Object initialSelectionValue)
    {
        return showInputDialog(null, message, title,
            popupDialog2JOptionPaneMessageType(messageType), null,
            selectionValues, initialSelectionValue);
    }

    /**
     * Implements the
     * <tt>PopupDialog.showInputPopupDialog(Object, String, int, Object[],
     * Object)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showInputDialog</tt> method.
     *
     * @param message the message to display
     * @param messageType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param title the String to display in the dialog title bar
     * @param selectionValues an array of Objects that gives the possible
     * selections
     * @param initialSelectionValue the value used to initialize the input field
     * @param icon the icon to show in the input window.
     */
    public Object showInputPopupDialog(Object message, String title,
        int messageType, Object[] selectionValues,
        Object initialSelectionValue, byte[] icon)
    {
        return showInputDialog(null, message, title,
            popupDialog2JOptionPaneMessageType(messageType),
            createImageIcon(icon), selectionValues, initialSelectionValue);
    }

    private static ImageIcon createImageIcon(byte[] icon)
    {
        return (icon == null) ? null : new ImageIcon(icon);
    }

    /**
     * Implements the <tt>PopupDialog.showMessagePopupDialog(Object)</tt>
     * method. Invokes the corresponding
     * <tt>JOptionPane.showMessageDialog</tt> method.
     *
     * @param message the Object to display
     */
    public void showMessagePopupDialog(Object message)
    {
        showMessageDialog(null, message);
    }

    /**
     * Implements the <tt>PopupDialog.showMessagePopupDialog(Object, String,
     * int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showMessageDialog</tt> method.
     *
     * @param message the Object to display
     * @param title the title string for the dialog
     * @param messageType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     */
    public void showMessagePopupDialog(Object message, String title,
        int messageType)
    {
        showMessageDialog(null, message, title,
            popupDialog2JOptionPaneMessageType(messageType));
    }

    /**
     * Implements the <tt>PopupDialog.showMessagePopupDialog(Object, String,
     * int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showMessageDialog</tt> method.
     *
     * @param message the Object to display
     * @param title the title string for the dialog
     * @param messageType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param icon the image to display in the message dialog.
     */
    public void showMessagePopupDialog(Object message, String title,
        int messageType, byte[] icon)
    {
        showMessageDialog(null, message, title,
            popupDialog2JOptionPaneMessageType(messageType),
            createImageIcon(icon));
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object)</tt>
     * method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     *
     * @param message the message to display
     */
    public int showConfirmPopupDialog(Object message)
    {
        return showConfirmDialog(null, message);
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object, String,
     * int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     *
     * @param message the Object to display
     * @param title the title string for the dialog
     * @param optionType an integer designating the options available on the
     * dialog: YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     */
    public int showConfirmPopupDialog(Object message, String title,
        int optionType)
    {
        return showConfirmDialog(null, message, title,
            popupDialog2JOptionPaneOptionType(optionType));
    }

    private static int popupDialog2JOptionPaneOptionType(int optionType)
    {
        switch (optionType) {
        case PopupDialog.OK_CANCEL_OPTION:
            return JOptionPane.OK_CANCEL_OPTION;
        case PopupDialog.YES_NO_OPTION:
            return JOptionPane.YES_NO_OPTION;
        case PopupDialog.YES_NO_CANCEL_OPTION:
            return JOptionPane.YES_NO_CANCEL_OPTION;
        default:
            return JOptionPane.DEFAULT_OPTION;
        }
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object, String,
     * int, int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     *
     * @param message the Object to display
     * @param title the title string for the dialog
     * @param optionType an integer designating the options available on the
     * dialog: YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     * @param messageType an integer designating the kind of message this is;
     * primarily used to determine the icon from the pluggable Look and Feel:
     * ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE,
     * or PLAIN_MESSAGE
     */
    public int showConfirmPopupDialog(Object message, String title,
        int optionType, int messageType)
    {
        return showConfirmDialog(null, message, title,
            popupDialog2JOptionPaneOptionType(optionType),
            popupDialog2JOptionPaneMessageType(messageType));
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object, String,
     * int, int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     *
     * @param message the Object to display
     * @param title the title string for the dialog
     * @param optionType an integer designating the options available on the
     * dialog: YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     * @param messageType an integer designating the kind of message this is;
     * primarily used to determine the icon from the pluggable Look and Feel:
     * ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE,
     * or PLAIN_MESSAGE
     * @param icon the icon to display in the dialog
     */
    public int showConfirmPopupDialog(Object message, String title,
        int optionType, int messageType, byte[] icon)
    {
        return showConfirmDialog(null, message, title,
            popupDialog2JOptionPaneOptionType(optionType),
            popupDialog2JOptionPaneMessageType(messageType),
            createImageIcon(icon));
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

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams)
    {
    }
}
