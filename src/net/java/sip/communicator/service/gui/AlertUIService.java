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
 * The <tt>AlertUIService</tt> is a service that allows to show error messages
 * and warnings.
 *
 * @author Yana Stamcheva
 */
public interface AlertUIService
{
    /**
     * Indicates that the OK button is pressed.
     */
    public static final int OK_RETURN_CODE = 0;

    /**
     * Indicates that the Cancel button is pressed.
     */
    public static final int CANCEL_RETURN_CODE = 1;

    /**
     * Indicates that the OK button is pressed and the Don't ask check box is
     * checked.
     */
    public static final int OK_DONT_ASK_CODE = 2;

    /**
     * The type of the alert dialog, which displays a warning instead of an
     * error.
     */
    public static final int WARNING = 1;

    /**
     * The type of alert dialog which displays a warning instead of an error.
     */
    public static final int ERROR = 0;

    /**
     * Shows an alert dialog with the given title and message.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     */
    public void showAlertDialog(String title,
                                String message);

    /**
     * Shows an alert dialog with the given title message and exception
     * corresponding to the error.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param e the exception corresponding to the error
     */
    public void showAlertDialog(String title,
                                String message,
                                Throwable e);

    /**
     * Shows an alert dialog with the given title, message and type of message.
     *
     * @param title the title of the error dialog
     * @param message the message to be displayed
     * @param type the dialog type (warning or error)
     */
    public void showAlertDialog(String title,
                                String message,
                                int type);

    /**
     * Shows an notification pop-up which can be clicked. An error dialog is
     * shown when the notification is clicked.
     *
     * @param title the title of the error dialog and the notification pop-up
     * @param message the message to be displayed in the error dialog and the
     * pop-up
     */
    public void showAlertPopup(String title, String message);

    /**
     * Shows an notification pop-up which can be clicked. An error dialog is
     * shown when the notification is clicked.
     *
     * @param title the title of the error dialog and the notification pop-up
     * @param message the message to be displayed in the error dialog and the
     * pop-up
     * @param e the exception that can be shown in the error dialog
     */
    public void showAlertPopup(String title, String message,
                                        Throwable e);

    /**
     * Shows an notification pop-up which can be clicked. An error dialog is
     * shown when the notification is clicked.
     *
     * @param title the title of the notification pop-up
     * @param message the message of the pop-up
     * @param errorDialogTitle the title of the error dialog
     * @param errorDialogMessage the message of the error dialog
     */
    public void showAlertPopup(String title, String message,
        String errorDialogTitle, String errorDialogMessage);

    /**
     * Shows an notification pop-up which can be clicked. An error dialog is
     * shown when the notification is clicked.
     *
     * @param title the title of the notification pop-up
     * @param message the message of the pop-up
     * @param errorDialogTitle the title of the error dialog
     * @param errorDialogMessage the message of the error dialog
     * @param e the exception that can be shown in the error dialog
     */
    public void showAlertPopup(String title, String message,
        String errorDialogTitle, String errorDialogMessage, Throwable e);

    /**
     * Releases the resources acquired by this instance throughout its lifetime
     * and removes the listeners.
     */
    public void dispose();
}
