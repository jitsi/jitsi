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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.swing.*;
import net.java.sip.communicator.util.*;

public abstract class SIPCommDialog extends JDialog
{
    private Logger logger = Logger.getLogger(SIPCommDialog.class);

    private ActionMap amap;
    private InputMap imap;

    private boolean isSaveSizeAndLocation = true;

    public SIPCommDialog()
    {
        super();

        this.init();
    }

    public SIPCommDialog(Dialog owner)
    {
        super(owner);

        this.init();
    }
 
    public SIPCommDialog(Frame owner)
    {
        super(owner);

        this.init();
    }
    
    public SIPCommDialog(Dialog owner, boolean isSaveSizeAndLocation)
    {
        this(owner);

        this.isSaveSizeAndLocation = isSaveSizeAndLocation;
    }

    public SIPCommDialog(Frame owner, boolean isSaveSizeAndLocation)
    {
        this(owner);

        this.isSaveSizeAndLocation = isSaveSizeAndLocation;
    }

    /**
     * Initializes this dialog.
     */
    private void init()
    {
        this.setContentPane(new MainContentPane());

        this.addWindowListener(new DialogWindowAdapter());

        this.initInputMap();
    }

    private void initInputMap()
    {
        amap = this.getRootPane().getActionMap();

        amap.put("close", new CloseAction());

        imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }
    
    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends AbstractAction
    {
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
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
    
        String className = this.getClass().getName();
                
        try {
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
        catch (PropertyVetoException e1) {
            logger.error("The proposed property change "
                    + "represents an unacceptable value");
        }
    }
    
    /**
     * Sets window size and position.
     */
    private void setSizeAndLocation()
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
        
        if(widthString != null && heightString != null)
        {   
            width = new Integer(widthString).intValue();
            height = new Integer(heightString).intValue();
            
            if(width > 0 && height > 0)
                this.setSize(width, height);
        }
        
        if(xString != null && yString != null)
        {
            this.setLocation(new Integer(xString).intValue(),
                new Integer(yString).intValue());
        }
        else {
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
    public void dispose()
    {
        if(isSaveSizeAndLocation)
            this.saveSizeAndLocation();
        
        super.dispose();
    }
    
    private class MainContentPane extends JPanel
    {
        String isColorBgEnabledProp
            = "impl.gui.IS_WINDOW_COLOR_BACKGROUND_ENABLED";

        boolean isColorBgEnabled = new Boolean(
            GuiActivator.getResources().getSettingsString(isColorBgEnabledProp))
                .booleanValue();

        Color bgStartColor = new Color(GuiActivator.getResources()
            .getColor("service.gui.MAIN_BACKGROUND"));

        Color bgEndColor = new Color(GuiActivator.getResources()
            .getColor("service.gui.MAIN_BACKGROUND_GRADIENT"));

        GeneralPath headerBackground = new GeneralPath();

        public MainContentPane()
        {
            super(new BorderLayout());

            int borderSize = GuiActivator.getResources()
                .getSettingsInt("impl.gui.MAIN_WINDOW_BORDER_SIZE");

            if (isColorBgEnabled)
            {
                this.setBorder(BorderFactory
                    .createEmptyBorder( borderSize,
                                        borderSize,
                                        borderSize,
                                        borderSize));
            }
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();
            try
            {
                internalPaintComponent(g);
            }
            finally
            {
                g.dispose();
            }
        }

        private void internalPaintComponent(Graphics g)
        {
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

            g2.setPaint(bgGradientColor);
            g2.fillRect(0, 0, this.getWidth(), 80);

            g2.setColor(bgEndColor);
            g2.fillRect(0, 78,
                    this.getWidth(),
                    this.getHeight());

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
