/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

public abstract class SIPCommFrame extends JFrame
{
    private Logger logger = Logger.getLogger(SIPCommFrame.class);
    
    ActionMap amap;
    InputMap imap;
    
    public SIPCommFrame()
    {
        this.setIconImage(
            ImageLoader.getImage(ImageLoader.SIP_COMMUNICATOR_LOGO));
        
        this.addWindowListener(new FrameWindowAdapter());
        
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
            saveSizeAndLocation();
            close();
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
    public class FrameWindowAdapter extends WindowAdapter
    {
        public void windowClosing(WindowEvent e) {
            saveSizeAndLocation();
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
    public void setSizeAndLocation() {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String className = this.getClass().getName();
        
        String width = configService.getString(
            className + ".width");

        String height = configService.getString(
            className + ".height");

        String x = configService.getString(
            className + ".x");

        String y = configService.getString(
            className + ".y");

        if(width != null && height != null)
            this.setSize(new Integer(width).intValue(),
                    new Integer(height).intValue());            

        if(x != null && y != null)
            this.setLocation(new Integer(x).intValue(),
                    new Integer(y).intValue());
        else
            this.setCenterLocation();
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
     * Overwrites the setVisible method in order to set the size and the
     * position of this window before showing it.
     */
    public void setVisible(boolean isVisible)
    {
        if(isVisible) {
            this.pack();
            this.setSizeAndLocation();
        }
        
        super.setVisible(isVisible);
    }
    
    /**
     * All functions implemented in this method will be invoked when user
     * presses the Escape key. 
     */
    protected abstract void close();
}
