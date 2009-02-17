/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.systray;

import javax.swing.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>PopupMessage</tt> class encloses informations to show in a popup.
 * While a message title and a message body are mandatory informations,
 * a popup message could provides more stuffs like a component or an image which
 * may be used by a <tt>PopupMessageHandler</tt> capable to handle it.
 *
 * @author Symphorien Wanko
 */
public class PopupMessage
{

    /** message to show in the popup */
    private String message;

    /** title of the message */
    private String messageTitle;

    /** An icon representing the contact from which the notification comes */
    private ImageIcon imageIcon;

    /** A ready to show <tt>JComponet</tt> for this <tt>PopupMessage</tt> */
    private JComponent component;

    /** type of the message */
    private int messageType;

    /** the contact which is the cause of this popup message */
    private Contact contact;

    /**
     * Creates a <tt>PopupMessage</tt> with the given title and message inside
     *
     * @param messageTitle title of the message
     * @param message message to show in the systray
     */
    public PopupMessage(String messageTitle, String message)
    {
        this.messageTitle = messageTitle;
        this.message = message;
    }

    /**
     * Creates a system tray message with the given title and message content. The
     * message type will affect the icon used to present the message.
     *
     * @param title the title, which will be shown
     * @param content the content of the message to display
     * @param messageType the message type; one of XXX_MESSAGE_TYPE constants
     * declared in <tt>SystrayService
     */
    public PopupMessage(String title, String content, int messageType)
    {
        this(title, content);
        this.messageType = messageType;
    }

    /**
     * Creates a new <tt>PopupMessage</tt> with the given title, message and
     * icon.
     * 
     * @param messageTitle title of the message
     * @param message message to show in the systray
     * @param imageIcon an incon to show in the popup message.
     */
    public PopupMessage(String title, String message, ImageIcon imageIcon)
    {
        this(title, message);
        this.imageIcon = imageIcon;
    }

    /**
     * Creates a new <tt>PopupMessage</tt> with the given
     * <tt>JComponent</tt> as its content. This constructor also takes a title
     * and a message as replacements in cases the component is not usable.
     *
     * @param component the component to put in the <tt>PopupMessage</tt>
     * @param title of the message
     * @param message message to use in place of the component
     */
    public PopupMessage(JComponent component, String title, String message)
    {
        this(title, message);
        this.component = component;
    }

    /**
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * @return the messageTitle
     */
    public String getMessageTitle()
    {
        return messageTitle;
    }

    /**
     * @param messageTitle the messageTitle to set
     */
    public void setMessageTitle(String messageTitle)
    {
        this.messageTitle = messageTitle;
    }

    /**
     * @return the component
     */
    public JComponent getComponent()
    {
        return component;
    }

    /**
     * @param component the component to set
     */
    public void setComponent(JComponent component)
    {
        this.component = component;
    }

    /**
     * @return the imageIcon
     */
    public ImageIcon getIcon()
    {
        return imageIcon;
    }

    /**
     * @param imageIcon the imageIcon to set
     */
    public void setIcon(ImageIcon imageIcon)
    {
        this.imageIcon = imageIcon;
    }

    /**
     * @return the messageType
     */
    public int getMessageType()
    {
        return messageType;
    }

    /**
     * @param messageType the messageType to set
     */
    public void setMessageType(int messageType)
    {
        this.messageType = messageType;
    }

    /**
     * @return the contact
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * @param contact the contact to set
     */
    public void setContact(Contact contact)
    {
        this.contact = contact;
    }
}
