/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>AlertUIServiceImpl</tt> is an implementation of the
 * <tt>AlertUIService</tt> that allows to show swing error dialogs.
 *
 * @author Yana Stamcheva
 */
public class AlertUIServiceImpl
    implements AlertUIService
{
    /**
     * The pop-up notification listener which handles the clicking on the
     * pop-up notification.
     */
    private SystrayPopupMessageListener listener = null;

    /**
     * The <tt>Logger</tt> used by the <tt>AlertUIServiceImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AlertUIServiceImpl.class);

    /**
     * Shows an alert dialog with the given title and message.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     */
    public void showAlertDialog(String title, String message)
    {
        new ErrorDialog(GuiActivator.getUIService().getMainFrame(),
                        title,
                        message).showDialog();
    }

    /**
     * Shows an alert dialog with the given title message and exception
     * corresponding to the error.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param e the exception corresponding to the error
     */
    public void showAlertDialog(String title, String message, Throwable e)
    {
        new ErrorDialog(GuiActivator.getUIService().getMainFrame(),
            title,
            message,
            e).showDialog();
    }

    /**
     * Shows an alert dialog with the given title, message and type of message.
     *
     * @param title the title of the error dialog
     * @param message the message to be displayed
     * @param type the dialog type (warning or error)
     */
    public void showAlertDialog(String title, String message, int type)
    {
        new ErrorDialog(GuiActivator.getUIService().getMainFrame(),
            title,
            message,
            type).showDialog();
    }

    /**
     * Shows an notification pop-up which can be clicked. An error dialog is
     * shown when the notification is clicked.
     *
     * @param title the title of the error dialog and the notification pop-up
     * @param message the message to be displayed in the error dialog and the
     * pop-up
     */
    public void showAlertPopup(String title, String message)
    {
        showAlertPopup(title, message, title, message, null);
    }

    /**
     * Shows an notification pop-up which can be clicked. An error dialog is
     * shown when the notification is clicked.
     *
     * @param title the title of the error dialog and the notification pop-up
     * @param message the message to be displayed in the error dialog and the
     * pop-up
     * @param e the exception that can be shown in the error dialog
     */
    public void showAlertPopup(String title, String message, Throwable e)
    {
        showAlertPopup(title, message, title, message, e);
    }

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
        String errorDialogTitle, String errorDialogMessage)
    {
        showAlertPopup(title, message, errorDialogTitle,
            errorDialogMessage, null);
    }

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
        String errorDialogTitle, String errorDialogMessage, Throwable e)
    {
        SystrayService systray = GuiActivator.getSystrayService();
        if(systray == null)
        {
            logger.warn("SystrayService not available.");
            return;
        }

        if(listener == null)
        {
            listener = new SystrayPopupMessageListener()
            {
                public void popupMessageClicked(SystrayPopupMessageEvent evt)
                {
                    Object tag = evt.getTag();
                    if(tag instanceof ErrorDialogParams)
                    {
                        ErrorDialogParams params = (ErrorDialogParams)tag;
                        Throwable e = params.getEx();
                        if(e != null)
                        {
                            showAlertDialog(params.getTitle(),
                                params.getMessage(), e);
                        }
                        else
                        {
                            showAlertDialog(params.getTitle(),
                                params.getMessage());
                        }
                    }

                }

            };
            systray.addPopupMessageListener(listener);
        }

        systray.showPopupMessage(
                new PopupMessage(title, message, null,
                    new ErrorDialogParams(errorDialogTitle,
                        errorDialogMessage, e)));
    }

    /**
     * Releases the resources acquired by this instance throughout its lifetime
     * and removes the listeners.
     */
    public void dispose()
    {
        SystrayService systray = GuiActivator.getSystrayService();
        if(systray == null)
        {
            logger.warn("SystrayService not available.");
            return;
        }
        systray.removePopupMessageListener(listener);
    }

    /**
     * The ErrorDialogParams class is holder for the parameters needed by the
     * error dialog to be shown.
     */
    class ErrorDialogParams
    {
        /**
         * The title parameter.
         */
        private String title;

        /**
         * The message parameter.
         */
        private String message;

        /**
         * The exception parameter.
         */
        private Throwable e;

        public ErrorDialogParams(String title, String message, Throwable e)
        {
            this.title = title;
            this.message = message;
            this.e = e;
        }

        /**
         * @return the title
         */
        public String getTitle()
        {
            return title;
        }

        /**
         * @return the message
         */
        public String getMessage()
        {
            return message;
        }

        /**
         * @return the exception
         */
        public Throwable getEx()
        {
            return e;
        }
    }
}
