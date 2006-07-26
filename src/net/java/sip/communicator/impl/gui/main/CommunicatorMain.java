/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.lookandfeel.SIPCommDefaultTheme;
import net.java.sip.communicator.impl.gui.lookandfeel.SIPCommLookAndFeel;
import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.util.Logger;

/**
 * Starts the GUI application using the SIPCommLookAndFeel with the
 * SIPCommDefaultTheme.
 *
 * @author Yana Stamcheva
 */
public class CommunicatorMain {

    private MainFrame mainFrame;

    private Logger logger = Logger.getLogger(CommunicatorMain.class.getName());

    /**
     * Creates an instance of <tt>CommunicatorMain</tt>. Creates the
     * <tt>MainFrame</tt>.
     */
    public CommunicatorMain() {

        this.setDefaultThemePack();

        mainFrame = new MainFrame();

        // In order to have the same icon when using option panes
        JOptionPane.getRootFrame().setIconImage(
                ImageLoader.getImage(ImageLoader.SIP_LOGO));
    }

    /**
     * Sets the look&feel and the theme.
     */
    public void setDefaultThemePack() {

        SIPCommLookAndFeel lf = new SIPCommLookAndFeel();
        SIPCommLookAndFeel.setCurrentTheme(new SIPCommDefaultTheme());

        // we need to set the UIDefaults class loader so that it may access
        // resources packed inside OSGI bundles
        UIManager.put("ClassLoader", getClass().getClassLoader());
        try {
            UIManager.setLookAndFeel(lf);
        } catch (UnsupportedLookAndFeelException e) {
            logger.error("The provided Look & Feel is not supported.", e);
        }
    }

    /**
     * Shows or hides the main application window.
     * 
     * @param isVisible <code>true</code> to show the main application
     * window, <code>false</code> to hide it.
     */
    public void showCommunicator(boolean isVisible) {
        
        this.mainFrame.setSizeAndLocation();
        
        this.mainFrame.setVisible(isVisible);
    }
    
    /**
     * Returns the main application window.
     * @return the main application window.
     */
    public MainFrame getMainFrame() {
        return mainFrame;
    }
}
