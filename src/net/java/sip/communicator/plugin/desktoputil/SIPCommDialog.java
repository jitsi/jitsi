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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.util.Logger;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

/**
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class SIPCommDialog
    extends JDialog
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>SIPCommDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(SIPCommDialog.class);

    /**
     * The action map of this dialog.
     */
    private ActionMap amap;

    /**
     * The input map of this dialog.
     */
    private InputMap imap;

    /**
     * Indicates if the size and location of this dialog are stored after
     * closing.
     */
    private boolean isSaveSizeAndLocation = true;

    /**
     * Creates an instance of <tt>SIPCommDialog</tt>.
     */
    public SIPCommDialog()
    {
        super();

        this.init();
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying the
     * <tt>Dialog</tt>owner of this dialog.
     * @param owner the owner of this dialog
     */
    public SIPCommDialog(Dialog owner)
    {
        super(owner);

        this.init();
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying the
     * <tt>Frame</tt> owner.
     * @param owner the owner of this dialog
     */
    public SIPCommDialog(Frame owner)
    {
        super(owner);

        this.init();
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying the
     * <tt>Frame</tt> owner.
     * @param owner the owner of this dialog
     */
    public SIPCommDialog(Window owner)
    {
        super(owner);

        this.init();

        if(owner == null)
        {
            SIPCommFrame.updateIconImages(this);
        }
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying explicitly
     * if the size and location properties are saved. By default size and
     * location are stored.
     * @param isSaveSizeAndLocation indicates whether to save the size and
     * location of this dialog
     */
    public SIPCommDialog(boolean isSaveSizeAndLocation)
    {
        this();

        this.isSaveSizeAndLocation = isSaveSizeAndLocation;
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying the owner
     * of this dialog and indicating whether to save the size and location
     * properties.
     * @param owner the owner of this dialog
     * @param isSaveSizeAndLocation indicates whether to save the size and
     * location of this dialog
     */
    public SIPCommDialog(Dialog owner, boolean isSaveSizeAndLocation)
    {
        this(owner);

        this.isSaveSizeAndLocation = isSaveSizeAndLocation;
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying the owner
     * of this dialog and indicating whether to save the size and location
     * properties.
     * @param owner the owner of this dialog
     * @param isSaveSizeAndLocation indicates whether to save the size and
     * location of this dialog
     */
    public SIPCommDialog(Frame owner, boolean isSaveSizeAndLocation)
    {
        this(owner);

        this.isSaveSizeAndLocation = isSaveSizeAndLocation;
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying the owner
     * of this dialog and indicating whether to save the size and location
     * properties.
     * @param owner the owner of this dialog
     * @param isSaveSizeAndLocation indicates whether to save the size and
     * location of this dialog
     */
    public SIPCommDialog(Window owner, boolean isSaveSizeAndLocation)
    {
        this(owner);

        this.isSaveSizeAndLocation = isSaveSizeAndLocation;
    }

    /**
     * Initializes this dialog.
     */
    private void init()
    {
        // If on MacOS we would use the native background.
        if (!OSUtils.IS_MAC)
            this.setContentPane(new SIPCommFrame.MainContentPane());

        this.addWindowListener(new DialogWindowAdapter());

        this.initInputMap();

        WindowUtils.addWindow(this);
    }

    private void initInputMap()
    {
        amap = this.getRootPane().getActionMap();

        amap.put("close", new CloseAction());

        imap = this.getRootPane().getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

        // put the defaults for macosx
        if(OSUtils.IS_MAC)
        {
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK),
                "close");
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
                "close");
        }
    }

    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends UIAction
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            if(isSaveSizeAndLocation)
                saveSizeAndLocation();

            close(true);
        }
    }

    /**
     * Adds a key - action pair for this frame.
     *
     * @param keyStroke the key combination
     * @param action the action which will be executed when user presses the
     * given key combination
     */
    protected void addKeyBinding(KeyStroke keyStroke, Action action)
    {
        String actionID = action.getClass().getName();

        amap.put(actionID, action);

        imap.put(keyStroke, actionID);
    }

    /**
     * Before closing the application window saves the current size and position
     * through the <tt>ConfigurationService</tt>.
     */
    public class DialogWindowAdapter extends WindowAdapter
    {
        /**
         * Invoked when this window is in the process of being closed.
         * @param e the <tt>WindowEvent</tt> that notified us
         */
        @Override
        public void windowClosing(WindowEvent e)
        {
            if(isSaveSizeAndLocation)
                saveSizeAndLocation();

            close(false);
        }
    }

    /**
     * Saves the size and the location of this dialog through the
     * <tt>ConfigurationService</tt>.
     */
    private void saveSizeAndLocation()
    {
        try
        {
            SIPCommFrame.saveSizeAndLocation(this, true, true);
        }
        catch (ConfigPropertyVetoException e1)
        {
            logger.error("The proposed property change "
                    + "represents an unacceptable value");
        }
    }

    /**
     * Sets window size and position.
     */
    private void setSizeAndLocation()
    {
        ConfigurationService config = DesktopUtilActivator.getConfigurationService();
        String className = this.getClass().getName().replaceAll("\\$", "_");

        int width = config.getInt(className + ".width", 0);
        int height = config.getInt(className + ".height", 0);

        String xString = config.getString(className + ".x");
        String yString = config.getString(className + ".y");

        if(width > 0 && height > 0)
            this.setSize(width, height);

        if(xString != null && yString != null)
        {
            int x = Integer.parseInt(xString);
            int y = Integer.parseInt(yString);
            if(ScreenInformation.
                isTitleOnScreen(new Rectangle(x, y, width, height))
                || config.getBoolean(
                    SIPCommFrame.PNAME_CALCULATED_POSITIONING, true))
            {
                this.setLocation(x, y);
            }
        }
        else
            this.setCenterLocation();
    }

    /**
     * Positions this window in the center of the screen.
     */
    private void setCenterLocation()
    {
        setLocationRelativeTo(getParent());
    }

    /**
     * Checks whether the current component will
     * exceeds the screen size and if it do will set a default size
     */
    private void ensureOnScreenLocationAndSize()
    {
        ConfigurationService config = DesktopUtilActivator.getConfigurationService();
        if(!config.getBoolean(SIPCommFrame.PNAME_CALCULATED_POSITIONING, true))
            return;

        int x = this.getX();
        int y = this.getY();

        int width = this.getWidth();
        int height = this.getHeight();

        Rectangle virtualBounds = ScreenInformation.getScreenBounds();

        // the default distance to the screen border
        final int borderDistance = 10;

        // in case any of the sizes exceeds the screen size
        // we set default one
        // get the left upper point of the window
        if (!(virtualBounds.contains(x, y)))
        {
            // top left exceeds screen bounds
            if (x < virtualBounds.x)
            {
                // window is too far to the left
                // move it to the right
                x = virtualBounds.x + borderDistance;
            } else if (x > virtualBounds.x)
            {
                // window is too far to the right
                // can only occour, when screen resolution is
                // changed or displayed are disconnected

                // move the window in the bounds to the very right
                x = virtualBounds.x + virtualBounds.width - width
                        - borderDistance;
                if (x < virtualBounds.x + borderDistance)
                {
                    x = virtualBounds.x + borderDistance;
                }
            }

            // top left exceeds screen bounds
            if (y < virtualBounds.y)
            {
                // window is too far to the top
                // move it to the bottom
                y = virtualBounds.y + borderDistance;
            } else if (y > virtualBounds.y)
            {
                // window is too far to the bottom
                // can only occour, when screen resolution is
                // changed or displayed are disconnected

                // move the window in the bounds to the very bottom
                y = virtualBounds.y + virtualBounds.height - height
                        - borderDistance;
                if (y < virtualBounds.y + borderDistance)
                {
                    y = virtualBounds.y + borderDistance;
                }
            }
            this.setLocation(x, y);
        }

        // check the lower right corder
        if (!(virtualBounds.contains(x, y, width, height)))
        {
            if (x + width > virtualBounds.x + virtualBounds.width)
            {
                // location of window is too far to the right, its right
                // border is out of bounds

                // calculate a new horizontal position
                // move the whole window to the left
                x = virtualBounds.x + virtualBounds.width - width
                        - borderDistance;
                if (x < virtualBounds.x + borderDistance)
                {
                    // window is already on left side, it is too wide.
                    x = virtualBounds.x + borderDistance;
                    // reduce the width, so it surely fits
                    width = virtualBounds.width - 2 * borderDistance;
                }
            }
            if (y + height > virtualBounds.y + virtualBounds.height)
            {
                // location of window is too far to the bottom, its bottom
                // border is out of bounds

                // calculate a new vertical position
                // move the whole window to the top
                y = virtualBounds.y + virtualBounds.height - height
                        - borderDistance;
                if (y < virtualBounds.y + borderDistance)
                {
                    // window is already on top, it is too high.
                    y = virtualBounds.y + borderDistance;
                    // reduce the width, so it surely fits
                    height = virtualBounds.height - 2 * borderDistance;
                }
            }
            this.setPreferredSize(new Dimension(width, height));
            this.setSize(width, height);
            this.setLocation(x, y);
        }
    }

    /**
     * Overwrites the setVisible method in order to set the size and the
     * position of this window before showing it.
     * @param isVisible indicates if the dialog should be visible
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        if(isVisible)
        {
            this.pack();

            if(isSaveSizeAndLocation)
                this.setSizeAndLocation();
            else
            {
                this.pack();
                this.setCenterLocation();
            }

            ensureOnScreenLocationAndSize();

            JButton button = this.getRootPane().getDefaultButton();

            if(button != null)
                button.requestFocus();
        }
        super.setVisible(isVisible);
    }

    /**
     * Overwrites the dispose method in order to save the size and the position
     * of this window before closing it.
     */
    @Override
    public void dispose()
    {
        if(isSaveSizeAndLocation)
            this.saveSizeAndLocation();

        super.dispose();
    }

    /**
     * All functions implemented in this method will be invoked when user
     * presses the Escape key.
     *
     * @param escaped <tt>true</tt> if this frame has been closed by pressing
     * the Esc key; otherwise, <tt>false</tt>
     */
    protected void close(boolean escaped)
    {
    }
}
