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
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The base class for all toggle buttons which control the call from the UI.
 * Allows extending buttons to focus on performing their toggle actions.
 *
 * @author Dmitri Melnikov
 * @author Adam Netocny
 * @author Yana Stamcheva
 */
public abstract class AbstractCallToggleButton
    extends SIPCommToggleButton
    implements Skinnable
{
    /**
     * The <tt>Call</tt> that this button controls.
     */
    protected final Call call;

    /**
     * The background image.
     */
    protected ImageID bgImage;

    /**
     * The rollover image
     */
    protected ImageID bgRolloverImage;

    /**
     * The pressed image.
     */
    protected ImageID pressedImage;

    /**
     * The icon image.
     */
    protected ImageID iconImageID;

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
        this.iconImageID = iconImageID;

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

        if (toolTipTextKey != null)
        {
            setToolTipText(
                GuiActivator.getResources().getI18NString(toolTipTextKey));
        }

        setSelected(selected);

        setModel(new CallToggleButtonModel(call));

        // All items are now instantiated and could safely load the skin.
        loadSkin();
    }

    /**
     * The button model of this call toggle button.
     */
    private class CallToggleButtonModel
        extends ToggleButtonModel
        implements ActionListener,
                   Runnable
    {
        private final Call call;

        private Thread runner;

        public CallToggleButtonModel(Call call)
        {
            this.call = call;

            addActionListener(this);
        }

        public synchronized void actionPerformed(ActionEvent event)
        {
            if (runner == null)
            {
                runner = new Thread(this, LocalVideoButton.class.getName());
                runner.setDaemon(true);

                setEnabled(false);
                runner.start();
            }
        }

        public void run()
        {
            try
            {
                doRun();
            }
            finally
            {
                synchronized (this)
                {
                    if (Thread.currentThread().equals(runner))
                    {
                        runner = null;
                        setEnabled(true);
                    }
                }
            }
        }

        private void doRun()
        {
            buttonPressed();
        }
    }

    /**
     * Loads images.
     */
    public void loadSkin()
    {
        setBgImage(ImageLoader.getImage(bgImage));
        setBgRolloverImage(ImageLoader.getImage(bgRolloverImage));
        setPressedImage(ImageLoader.getImage(pressedImage));
        setIconImage(ImageLoader.getImage(iconImageID));
    }

    /**
     * Notifies this <tt>AbstractCallToggleButton</tt> that its associated
     * action has been performed and that it should execute its very logic.
     */
    public abstract void buttonPressed();
}