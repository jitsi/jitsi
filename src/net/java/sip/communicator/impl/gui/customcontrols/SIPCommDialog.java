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
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

public abstract class SIPCommDialog extends JDialog
{
    private Logger logger = Logger.getLogger(SIPCommDialog.class);
    
    private ActionMap amap;
    private InputMap imap;
    
    private boolean isSaveSizeAndLocation = true;

    public SIPCommDialog(Dialog owner)
    {
        super(owner);
        
        this.addWindowListener(new DialogWindowAdapter());
        
        this.initInputMap();        
    }
 
    public SIPCommDialog(Frame owner)
    {
        super(owner);
        
        this.addWindowListener(new DialogWindowAdapter());
        
        this.initInputMap();
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
     * Overwrites the setVisible method in order to set the size and the
     * position of this window before showing it.
     */
    public void setVisible(boolean isVisible)
    {   
        if(isVisible) {
            this.pack();
            
            if(isSaveSizeAndLocation)
                this.setSizeAndLocation();
            
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
    
    /**
     * All functions implemented in this method will be invoked when user
     * presses the Escape key. 
     */
    protected abstract void close(boolean isEscaped);
}
