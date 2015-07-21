/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * @param mesg the object to display
     */
    public String showInputPopupDialog(Object mesg)
    {
        return showInputDialog(mesg);
    }

    /**
     * Implements the <tt>PopupDialog.showInputPopupDialog(Object, String)</tt>
     * method. Invokes the corresponding <tt>JOptionPane.showInputDialog</tt>
     * method.
     *
     * @param mesg the message to display
     * @param initialSelectionVal the value used to initialize the input
     * field.
     */
    public String showInputPopupDialog(Object mesg,
            String initialSelectionVal)
    {
        return showInputDialog(mesg, initialSelectionVal);
    }

    /**
     * Implements the
     * <tt>PopupDialog.showInputPopupDialog(Object, String, int)</tt> method.
     * Invokes the corresponding <tt>JOptionPane.showInputDialog</tt> method.
     *
     * @param mesg the message to display
     * @param mesgType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param title the String to display in the dialog title bar
     */
    public String showInputPopupDialog(Object mesg, String title,
        int mesgType)
    {
        return showInputDialog(null, mesg, title,
            popupDialog2JOptionPaneMessageType(mesgType));
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
     * @param mesg the message to display
     * @param mesgType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param title the String to display in the dialog title bar
     * @param selectionVal an array of Objects that gives the possible
     * selections
     * @param initialSelectionVal the value used to initialize the input field
     */
    public Object showInputPopupDialog(Object mesg, String title,
        int mesgType, Object[] selectionVal, Object initialSelectionVal)
    {
        return showInputDialog(null, mesg, title,
            popupDialog2JOptionPaneMessageType(mesgType), null,
            selectionVal, initialSelectionVal);
    }

    /**
     * Implements the
     * <tt>PopupDialog.showInputPopupDialog(Object, String, int, Object[],
     * Object)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showInputDialog</tt> method.
     *
     * @param mesg the message to display
     * @param mesgType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param title the String to display in the dialog title bar
     * @param selectionVal an array of Objects that gives the possible
     * selections
     * @param initialSelectionVal the value used to initialize the input field
     * @param icon the icon to show in the input window.
     */
    public Object showInputPopupDialog(Object mesg, String title,
        int mesgType, Object[] selectionVal,
        Object initialSelectionVal, byte[] icon)
    {
        return showInputDialog(null, mesg, title,
            popupDialog2JOptionPaneMessageType(mesgType),
            createImageIcon(icon), selectionVal, initialSelectionVal);
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
     * @param mesg the Object to display
     */
    public void showMessagePopupDialog(Object mesg)
    {
        showMessageDialog(null, mesg);
    }

    /**
     * Implements the <tt>PopupDialog.showMessagePopupDialog(Object, String,
     * int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showMessageDialog</tt> method.
     *
     * @param mesg the Object to display
     * @param title the title string for the dialog
     * @param mesgType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     */
    public void showMessagePopupDialog(Object mesg, String title,
        int mesgType)
    {
        showMessageDialog(null, mesg, title,
            popupDialog2JOptionPaneMessageType(mesgType));
    }

    /**
     * Implements the <tt>PopupDialog.showMessagePopupDialog(Object, String,
     * int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showMessageDialog</tt> method.
     *
     * @param mesg the Object to display
     * @param title the title string for the dialog
     * @param mesgType the type of message to be displayed: ERROR_MESSAGE,
     * INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param icon the image to display in the message dialog.
     */
    public void showMessagePopupDialog(Object mesg, String title,
        int mesgType, byte[] icon)
    {
        showMessageDialog(null, mesg, title,
            popupDialog2JOptionPaneMessageType(mesgType),
            createImageIcon(icon));
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object)</tt>
     * method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     *
     * @param mesg the message to display
     */
    public int showConfirmPopupDialog(Object mesg)
    {
        return showConfirmDialog(null, mesg);
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object, String,
     * int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     *
     * @param mesg the Object to display
     * @param title the title string for the dialog
     * @param optType an integer designating the options available on the
     * dialog: YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     */
    public int showConfirmPopupDialog(Object mesg, String title,
        int optType)
    {
        return showConfirmDialog(null, mesg, title,
            popupDialog2JOptionPaneOptionType(optType));
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
     * @param mesg the Object to display
     * @param title the title string for the dialog
     * @param optType an integer designating the options available on the
     * dialog: YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     * @param mesgType an integer designating the kind of message this is;
     * primarily used to determine the icon from the pluggable Look and Feel:
     * ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE,
     * or PLAIN_MESSAGE
     */
    public int showConfirmPopupDialog(Object mesg, String title,
        int optType, int mesgType)
    {
        return showConfirmDialog(null, mesg, title,
            popupDialog2JOptionPaneOptionType(optType),
            popupDialog2JOptionPaneMessageType(mesgType));
    }

    /**
     * Implements the <tt>PopupDialog.showConfirmPopupDialog(Object, String,
     * int, int)</tt> method. Invokes the corresponding
     * <tt>JOptionPane.showConfirmDialog</tt> method.
     *
     * @param mesg the Object to display
     * @param title the title string for the dialog
     * @param optType an integer designating the options available on the
     * dialog: YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     * @param mesgType an integer designating the kind of message this is;
     * primarily used to determine the icon from the pluggable Look and Feel:
     * ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE,
     * or PLAIN_MESSAGE
     * @param icon the icon to display in the dialog
     */
    public int showConfirmPopupDialog(Object mesg, String title,
        int optType, int mesgType, byte[] icon)
    {
        return showConfirmDialog(null, mesg, title,
            popupDialog2JOptionPaneOptionType(optType),
            popupDialog2JOptionPaneMessageType(mesgType),
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
