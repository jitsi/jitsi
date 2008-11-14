/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * A custom frame that remembers its size and location and could have a
 * semi transparent background.
 * 
 * @author Yana Stamcheva
 */
public abstract class SIPCommFrame
    extends JFrame
    implements Observer
{
    private Logger logger = Logger.getLogger(SIPCommFrame.class);

    ActionMap amap;
    InputMap imap;
    KeybindingSet bindings = null;

    public SIPCommFrame()
    {
        this.init();
    }

    /**
     * Initializes this frame.
     */
    private void init()
    {
        this.setContentPane(new MainContentPane());

        this.setIconImage(
            ImageLoader.getImage(ImageLoader.SIP_COMMUNICATOR_LOGO));

        // In order to have the same icon when using option panes
        JOptionPane.getRootFrame().setIconImage(
                ImageLoader.getImage(ImageLoader.SIP_COMMUNICATOR_LOGO));

        this.addWindowListener(new FrameWindowAdapter());

        amap = this.getRootPane().getActionMap();

        amap.put("close", new CloseAction());

        imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            saveSizeAndLocation();
            close(true);
        }
    }
    
    /**
     * Sets the input map to utilize a given category of keybindings. The frame
     * is updated to reflect the new bindings when they change. This replaces
     * any previous bindings that have been added.
     * 
     * @param category set of keybindings to be utilized
     */
    protected void setKeybindingInput(KeybindingSet.Category category)
    {
        // Removes old binding set
        if (this.bindings != null)
        {
            this.bindings.deleteObserver(this);
            resetInputMap();
        }

        // Adds new bindings to input map
        this.bindings =
            GuiActivator.getKeybindingsService().getBindings(category);

        for (Map.Entry<KeyStroke, String> key2action : this.bindings
            .getBindings().entrySet())
        {
            imap.put(key2action.getKey(), key2action.getValue());
        }

        this.bindings.addObserver(this);
    }
    
    /**
    * Bindings the string representation for a keybinding to the action that
    * will be executed.
    * @param binding string representation of action used by input map
    * @param action the action which will be executed when user presses the
    *            given key combination
    */
    protected void addKeybindingAction(String binding, Action action)
    {
        amap.put(binding, action);
    }

    /**
     * Before closing the application window saves the current size and position
     * through the <tt>ConfigurationService</tt>.
     */
    public class FrameWindowAdapter extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            saveSizeAndLocation();

            close(false);
        }
    }

    /**
     * Saves the size and the location of this frame through the
     * <tt>ConfigurationService</tt>.
     */
    private void saveSizeAndLocation()
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String className = this.getClass().getName();

        try
        {
            configService.setProperty(
                className + ".width",
                new Integer(getWidth()));

            configService.setProperty(
                className + ".height",
                new Integer(getHeight()));

            configService.setProperty(
                className + ".x",
                new Integer(getX()));

            configService.setProperty(
                className + ".y",
                new Integer(getY()));
        }
        catch (PropertyVetoException e1)
        {
            logger.error("The proposed property change "
                    + "represents an unacceptable value");
        }
    }

    /**
     * Sets window size and position.
     */
    public void setSizeAndLocation()
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String className = this.getClass().getName();

        String widthString = configService.getString(
            className + ".width");

        String heightString = configService.getString(
            className + ".height");

        String xString = configService.getString(
            className + ".x");

        String yString = configService.getString(
            className + ".y");

        int width = 0;
        int height = 0;

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

        if(widthString != null && heightString != null)
        {
            width = new Integer(widthString).intValue();
            height = new Integer(heightString).intValue();

            if(width > 0 && height > 0
                && width <= screenWidth && height <= screenHeight)
                this.setSize(width, height);
        }

        int x = 0;
        int y = 0;

        if(xString != null && yString != null)
        {
            x = new Integer(xString).intValue();
            y = new Integer(yString).intValue();

            this.setLocation(x, y);
        } else
        {
            this.setCenterLocation();
        }
    }
    
    /**
     * Positions this window in the center of the screen.
     */
    private void setCenterLocation()
    {
        this.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - this.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - this.getHeight()/2
                );
    }

    /**
     * Checks whether the current component will 
     * exceeds the screen size and if it do will set a default size 
     */
    private void ensureOnScreenLocationAndSize()
    {
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
        if (!(virtualBounds.contains(x + width, y + height)))
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
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            this.setSizeAndLocation();

            this.ensureOnScreenLocationAndSize();
        }

        super.setVisible(isVisible);
    }
    
    /**
     * Overwrites the dispose method in order to save the size and the position
     * of this window before closing it.
     */
    public void dispose()
    {
        this.saveSizeAndLocation();

        /*
         * The keybinding service will outlive us so don't let us retain our
         * memory.
         */
        if (bindings != null)
            bindings.deleteObserver(this);
        
        super.dispose();
    }

    private void resetInputMap()
    {
        imap.clear();
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

    /**
     * Listens for changes in binding sets so they can be reflected in the input
     * map
     */
    public void update(Observable obs, Object arg)
    {
        if (obs instanceof KeybindingSet)
        {
            KeybindingSet changedBindings = (KeybindingSet) obs;

            resetInputMap();
            for (Map.Entry<KeyStroke, String> key2action : changedBindings
                .getBindings().entrySet())
            {
                imap.put(key2action.getKey(), key2action.getValue());
            }
        }
    }

    private class MainContentPane extends JPanel
    {
        String isColorBgEnabledProp
            = "net.java.sip.communicator.impl.gui.isWindowColorBackgroundEnabled";

        boolean isColorBgEnabled = new Boolean(
            GuiActivator.getResources().getSettingsString(isColorBgEnabledProp))
                .booleanValue();

        Color bgStartColor = new Color(GuiActivator.getResources()
            .getColor("mainBackgroundStartColor"));

        Color bgEndColor = new Color(GuiActivator.getResources()
            .getColor("mainBackgroundEndColor"));

        GeneralPath headerBackground = new GeneralPath();

        public MainContentPane()
        {
            super(new BorderLayout());

            this.setBackground(new Color(
                GuiActivator.getResources()
                .getColor("mainBackground")));

            int borderSize = GuiActivator.getResources()
                .getSettingsInt("mainWindowBorderSize");

            this.setBorder(BorderFactory
                .createEmptyBorder( borderSize,
                                    borderSize,
                                    borderSize,
                                    borderSize));
        }

        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            // If the custom color window background is not enabled we have
            // nothing to do here.
            if (!isColorBgEnabled)
                return;

            Graphics2D g2 = (Graphics2D) g;

            AntialiasingManager.activateAntialiasing(g2);

            GradientPaint bgGradientColor
                = new GradientPaint(this.getWidth()/2, 0,
                bgStartColor,
                this.getWidth()/2,
                80,
                bgEndColor);

            GradientPaint borderShadow = new GradientPaint(0, 0,
                new Color(255, 255, 255, 200),
                this.getWidth(),
                this.getHeight(),
                new Color(0, 0, 0, 150));

            g2.setPaint(borderShadow);
            g2.fillRoundRect(3, 3,
                this.getWidth() - 6,
                this.getHeight() - 6,
                10, 10);

            g2.setPaint(bgGradientColor);
            g2.fillRoundRect(5, 5, this.getWidth() - 10, 80, 10, 10);

            g2.setColor(bgEndColor);
            g2.fillRoundRect(5, 80,
                    this.getWidth() - 10,
                    this.getHeight() - 85, 10, 10);

            GradientPaint curveShadow = new GradientPaint(0, 0,
                new Color(255, 255, 255, 150),
                this.getWidth(),
                this.getHeight(),
                new Color(255, 255, 255, 50));

            g2.setPaint(curveShadow);
            g2.setStroke(new BasicStroke(1f));
            CubicCurve2D curve1 = new CubicCurve2D.Float(
                50, -1, 250, 30, 50, 150, 0, 300);

            g2.draw(curve1);

            CubicCurve2D curve2 = new CubicCurve2D.Float(
                this.getWidth() - 20, 0,
                this.getWidth(), 100,
                this.getWidth()/2, 100,
                0, 150);

            g2.draw(curve2);

            CubicCurve2D curve3 = new CubicCurve2D.Float(
                0, 90,
                this.getWidth()/3, 60,
                2*this.getWidth()/3, 60,
                this.getWidth(), 90);

            g2.draw(curve3);
        }
    }

    /**
     * All functions implemented in this method will be invoked when user
     * presses the Escape key. 
     */
    protected abstract void close(boolean isEscaped);
}
