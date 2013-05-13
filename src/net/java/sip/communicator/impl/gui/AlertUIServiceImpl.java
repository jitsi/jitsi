/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.event.*;

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
     * The event type name for the notification pop-ups.
     */
    private static final String NOTIFICATION_EVENT_TYPE = "AlertUI";
    
    /**
     * A boolean used to verify that this listener registers only once to
     * the popup message notification handler.
     */
    private boolean isRegisteredToPopupMessageListener = false;
    
    /**
     * The pop-up notification listener which handles the clicking on the 
     * pop-up notification.
     */
    private SystrayPopupMessageListener listener = null;
    
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
    public void showPopUpNotification(String title, String message)
    {
        showPopUpNotification(title, message, title, message, null);
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
    public void showPopUpNotification(String title, String message, Throwable e)
    {
        showPopUpNotification(title, message, title, message, e);
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
    public void showPopUpNotification(String title, String message, 
        String errorDialogTitle, String errorDialogMessage)
    {
        showPopUpNotification(title, message, errorDialogTitle, 
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
    public void showPopUpNotification(String title, String message, 
        String errorDialogTitle, String errorDialogMessage, Throwable e)
    {
        NotificationService notificationService
            = GuiActivator.getNotificationService();

        if(notificationService == null)
            return;
        
        // Registers only once to the popup message notification
        // handler.
        if(!isRegisteredToPopupMessageListener )
        {
            notificationService.registerDefaultNotificationForEvent(
                  NOTIFICATION_EVENT_TYPE,
                  NotificationAction.ACTION_POPUP_MESSAGE,
                  null, null);
            isRegisteredToPopupMessageListener = true;
            addOrRemovePopupMessageListener(true);
        }

        // Fires the popup notification.
        Map<String,Object> extras = new HashMap<String,Object>();

        extras.put(NotificationData.POPUP_MESSAGE_HANDLER_TAG_EXTRA,
                new ErrorDialogParams(title, message, e));
        notificationService.fireNotification(NOTIFICATION_EVENT_TYPE, title,
                message, null, extras);
        
    }
    
    /**
     * Adds/removes the listener instance as a <tt>PopupMessageListener</tt> 
     * to/from the<tt>NotificationService</tt> in order to be able to detect 
     * when the user clicks on a pop-up notification displayed by this instance
     *
     * @param add <tt>true</tt> to add the listener instance as a
     * <tt>PopupMessageListener</tt> to the <tt>NotificationService</tt> or
     * <tt>false</tt> to remove it
     */
    private void addOrRemovePopupMessageListener(boolean add)
    {
        Iterable<NotificationHandler> popupHandlers 
            = GuiActivator.getNotificationService()
                .getActionHandlers(NotificationAction.ACTION_POPUP_MESSAGE);

        for(NotificationHandler popupHandler : popupHandlers)
        {
            if(!(popupHandler instanceof PopupMessageNotificationHandler))
                continue;
            
            PopupMessageNotificationHandler popupMessageNotificationHandler
                = (PopupMessageNotificationHandler) popupHandler;
            
            if(listener == null)
            {
                listener = new SystrayPopupMessageListener()
                {
                    public void popupMessageClicked(
                        SystrayPopupMessageEvent evt)
                    {
                        Object tag = evt.getTag();
                        if(tag instanceof ErrorDialogParams)
                        {
                            ErrorDialogParams params = (ErrorDialogParams)tag;
                            if(params.getEx() != null)
                            {
                                showAlertDialog(params.getTitle(), 
                                    params.getMessage(), params.getEx());
                            }
                            else
                            {
                                showAlertDialog(params.getTitle(), 
                                    params.getMessage());
                            }
                        }
                        
                    }
                    
                };
            }
            
            if(add)
            {
                popupMessageNotificationHandler.addPopupMessageListener(
                    listener);
            }
            else
            {
                popupMessageNotificationHandler.removePopupMessageListener(
                    listener);
            }
            
        }
        
    }
    
    /**
     * Releases the resources acquired by this instance throughout its lifetime
     * and removes the listeners.
     */
    public void dispose()
    {
        addOrRemovePopupMessageListener(false);
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
