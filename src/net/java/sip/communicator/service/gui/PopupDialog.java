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
package net.java.sip.communicator.service.gui;

/**
 * A configurable popup dialog, that could be used from other services for
 * simple interactions with the user, throught the gui interface. This dialog
 * allows showing error, warning or info messages, prompting the user for simple
 * one field input or choice, or asking the user for certain confirmation.
 *
 * Three types of dialogs are differentiated: Message, Confirm and Input dialog.
 * Each of them has several show methods corresponging, allowing additional
 * specific configuration, like specifying or not a title, confirmation option
 * or initial value.
 *
 * @author Yana Stamcheva
 */
public interface PopupDialog extends ExportedWindow
{
    public static final WindowID WINDOW_GENERAL_POPUP
        = new WindowID("GeneralPopupWindow");
    //
    // Option types
    //
    /** Type used for <code>showConfirmDialog</code>. */
    public static final int         YES_NO_OPTION = 0;
    /** Type used for <code>showConfirmDialog</code>. */
    public static final int         YES_NO_CANCEL_OPTION = 1;
    /** Type used for <code>showConfirmDialog</code>. */
    public static final int         OK_CANCEL_OPTION = 2;

    //
    // Return values.
    //
    /** Return value from class method if YES is chosen. */
    public static final int         YES_OPTION = 0;
    /** Return value from class method if NO is chosen. */
    public static final int         NO_OPTION = 1;
    /** Return value from class method if CANCEL is chosen. */
    public static final int         CANCEL_OPTION = 2;
    /** Return value form class method if OK is chosen. */
    public static final int         OK_OPTION = 0;
    /**
     * Return value from class method if user closes window without
     * selecting anything.
     */
    public static final int         CLOSED_OPTION = -1;

    /*
     * Message types. Meant to be used by the UI implementation to determine
     * what icon to display and possibly what behavior to give based on the
     * type.
     */
    /** Used for error messages. */
    public static final int  ERROR_MESSAGE = 0;
    /** Used for information messages. */
    public static final int  INFORMATION_MESSAGE = 1;
    /** Used for warning messages. */
    public static final int  WARNING_MESSAGE = 2;
    /** Used for questions. */
    public static final int  QUESTION_MESSAGE = 3;
    /** No icon is used. */
    public static final int  PLAIN_MESSAGE = -1;

    /**
     * Shows a question-message dialog requesting input from the user.
     *
     * @param message the <code>Object</code> to display.
     * @return user's input, or <code>null</code> meaning the user
     *          canceled the input
     */
    public abstract String showInputPopupDialog(Object message);

    /**
     * Shows a question-message dialog requesting input from the user, with
     * the input value initialized to <code>initialSelectionValue</code>.
     *
     * @param message the <code>Object</code> to display
     * @param initialSelectionValue the value used to initialize the input
     *                 field
     * @return user's input, or <code>null</code> meaning the user
     *          canceled the input
     */
    public abstract String showInputPopupDialog(Object message,
            String initialSelectionValue);


    /**
     * Shows a dialog with title <code>title</code> and message type
     * <code>messageType</code>, requesting input from the user. The message
     * type is meant to be used by the ui implementation to determine the
     * icon of the dialog.
     *
     * @param message  the <code>Object</code> to display
     * @param title    the <code>String</code> to display in the dialog
     *          title bar
     * @param messageType the type of message that is to be displayed:
     *                  <code>ERROR_MESSAGE</code>,
     *                  <code>INFORMATION_MESSAGE</code>,
     *                  <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *                  or <code>PLAIN_MESSAGE</code>
     * @return user's input, or <code>null</code> meaning the user
     *          canceled the input
     */
    public abstract String showInputPopupDialog(Object message, String title,
            int messageType);

    /**
     * Shows an input dialog, where all options like title, type of message
     * etc., could be configured. The user will be able to choose from
     * <code>selectionValues</code>, where <code>null</code> implies the
     * users can input whatever they wish.
     * <code>initialSelectionValue</code> is the initial value to prompt
     * the user with.
     * It is up to the UI implementation to decide how best to represent the
     * <code>selectionValues</code>. In the case of swing per example it could
     * be a <code>JComboBox</code>, <code>JList</code> or
     * <code>JTextField</code>. The message type is meant to be used by the ui
     * implementation to determine the icon of the dialog.
     *
     * @param message  the <code>Object</code> to display
     * @param title    the <code>String</code> to display in the
     *          dialog title bar
     * @param messageType the type of message to be displayed:
     *                  <code>ERROR_MESSAGE</code>,
     *                  <code>INFORMATION_MESSAGE</code>,
     *                  <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *                  or <code>PLAIN_MESSAGE</code>
     *
     * @param selectionValues an array of <code>Object</code>s that
     *          gives the possible selections
     * @param initialSelectionValue the value used to initialize the input
     *                 field
     * @return user's input, or <code>null</code> meaning the user
     *          canceled the input
     */
    public abstract Object showInputPopupDialog(Object message, String title,
            int messageType, Object[] selectionValues,
            Object initialSelectionValue);

    /**
     * Shows an information-message dialog titled "Message".
     *
     * @param message  the <code>Object</code> to display
     */
    public abstract void showMessagePopupDialog(Object message);

    /**
     * Shows a dialog that displays a message using a default
     * icon determined by the <code>messageType</code> parameter.
     *
     * @param message   the <code>Object</code> to display
     * @param title     the title string for the dialog
     * @param messageType the type of message to be displayed:
     *                  <code>ERROR_MESSAGE</code>,
     *                  <code>INFORMATION_MESSAGE</code>,
     *                  <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *                  or <code>PLAIN_MESSAGE</code>
     */
    public abstract void showMessagePopupDialog(Object message,
            String title, int messageType);

    /**
     * Shows a dialog that prompts the user for confirmation.
     *
     * @param message   the <code>Object</code> to display
     * @return one of the YES_OPTION, NO_OPTION,.., XXX_OPTION, indicating the
     * option selected by the user
     */
    public abstract int showConfirmPopupDialog(Object message);

    /**
     * Shows a dialog where the number of choices is determined
     * by the <code>optionType</code> parameter.
     *
     * @param message   the <code>Object</code> to display
     * @param title     the title string for the dialog
     * @param optionType an int designating the options available on the dialog:
     *                  <code>YES_NO_OPTION</code>, or
     *                  <code>YES_NO_CANCEL_OPTION</code>
     * @return one of the YES_OPTION, NO_OPTION,.., XXX_OPTION, indicating the
     * option selected by the user
     */
    public abstract int showConfirmPopupDialog(Object message,
            String title, int optionType);

    /**
     * Shows a dialog where the number of choices is determined
     * by the <code>optionType</code> parameter, where the
     * <code>messageType</code> parameter determines the icon to display.
     * The <code>messageType</code> parameter is primarily used to supply
     * a default icon for the dialog.
     *
     * @param message   the <code>Object</code> to display
     * @param title     the title string for the dialog
     * @param optionType an integer designating the options available
     *          on the dialog: <code>YES_NO_OPTION</code>,
     *          or <code>YES_NO_CANCEL_OPTION</code>
     * @param messageType an integer designating the kind of message this is;
     *                  <code>ERROR_MESSAGE</code>,
     *                  <code>INFORMATION_MESSAGE</code>,
     *                  <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *          or <code>PLAIN_MESSAGE</code>
     * @return one of the YES_OPTION, NO_OPTION,.., XXX_OPTION, indicating the
     * option selected by the user
     */
    public abstract int showConfirmPopupDialog(Object message, String title,
            int optionType, int messageType);

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
            Object initialSelectionValue, byte[] icon);

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
            int messageType, byte[] icon);

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
            int optionType, int messageType, byte[] icon);
}

