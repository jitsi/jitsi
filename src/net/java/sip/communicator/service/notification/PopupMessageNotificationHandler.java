/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * The <tt>PopupMessageNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * popup message actions.
 * 
 * @author Yana Stamcheva
 */
public interface PopupMessageNotificationHandler
    extends NotificationActionHandler
{
    /**
     * Returns the default message to be used when no message is provided to the
     * <tt>popupMessage</tt> method.
     * 
     * @return the default message to be used when no message is provided to the
     * <tt>popupMessage</tt> method.
     */
    public String getDefaultMessage();
    
    /**
     * Pops up a message with the given <tt>message</tt> content and the given
     * <tt>title</tt>.
     * 
     * @param title the title of the popup
     * @param message the message to show in the popup
     */
    public void popupMessage(String title, String message);
}
