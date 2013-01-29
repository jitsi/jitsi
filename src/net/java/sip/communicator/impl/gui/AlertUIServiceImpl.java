/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

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
}
