/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The base class for all toggle buttons which control the call from the UI.
 * Allows extending buttons to focus on performing their toggle actions.
 *
 * @author Dmitri Melnikov
 */
public abstract class AbstractCallToggleButton
    extends SIPCommToggleButton
    implements ActionListener
{
    /**
     * The <tt>Call</tt> that this button controls.
     */
    protected final Call call;

    /**
     * Initializes a new <tt>AbstractCallToggleButton</tt> instance which is to
     * control a toggle action for a specific <tt>Call</tt>.
     * 
     * @param call the <tt>Call</tt> to be controlled by the instance
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     * @param iconImageID the <tt>ImageID</tt> of the image to be used as the
     * icon of the new instance
     * @param toolTipTextKey the key in the <tt>ResourceManagementService</tt>
     * of the internationalized string which is to be used as the tool tip text
     * of the new instance
     */
    public AbstractCallToggleButton(
            Call call,
            boolean fullScreen,
            boolean selected,
            ImageID iconImageID,
            String toolTipTextKey)
    {
        this.call = call;

        ImageID bgImage;
        ImageID bgRolloverImage;
        ImageID pressedImage;

        if (fullScreen)
        {
            bgImage = ImageLoader.FULL_SCREEN_BUTTON_BG;
            bgRolloverImage = ImageLoader.FULL_SCREEN_BUTTON_BG;
            pressedImage = ImageLoader.FULL_SCREEN_BUTTON_BG_PRESSED;
        }
        else
        {
            bgImage = ImageLoader.CALL_SETTING_BUTTON_BG;
            bgRolloverImage = ImageLoader.CALL_SETTING_BUTTON_BG;
            pressedImage = ImageLoader.CALL_SETTING_BUTTON_PRESSED_BG;
        }
        setBgImage(ImageLoader.getImage(bgImage));
        setBgRolloverImage(ImageLoader.getImage(bgRolloverImage));
        setPressedImage(ImageLoader.getImage(pressedImage));

        setIconImage(ImageLoader.getImage(iconImageID));
        if (toolTipTextKey != null)
        {
            setToolTipText(
                GuiActivator.getResources().getI18NString(toolTipTextKey));
        }

        addActionListener(this);
        setSelected(selected);
    }
    
    /**
     * Notifies this <tt>AbstractCallToggleButton</tt> that its associated
     * action has been performed and that it should execute its very logic.
     *
     * @param evt an <tt>ActionEvent</tt> which describes the specifics of the
     * performed action
     */
    public abstract void actionPerformed(ActionEvent evt);
}