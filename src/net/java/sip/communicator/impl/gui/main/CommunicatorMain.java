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

import net.java.sip.communicator.impl.gui.lookandfeel.SIPCommDefaultTheme;
import net.java.sip.communicator.impl.gui.lookandfeel.SIPCommLookAndFeel;
import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.util.Logger;

/**
 * Starts the GUI application using the SkinLookAndFeel of l2fprod.
 *
 * @author Yana Stamcheva
 */
public class CommunicatorMain {

    private MainFrame mainFrame;

    private Logger logger = Logger.getLogger(CommunicatorMain.class.getName());

    public CommunicatorMain() {

        this.setDefaultThemePack();

        ConfigurationFrame configFrame = new ConfigurationFrame();

        mainFrame = new MainFrame();

        mainFrame.setConfigFrame(configFrame);

        // In order to have the same icon when using option panes
        JOptionPane.getRootFrame().setIconImage(
                ImageLoader.getImage(ImageLoader.SIP_LOGO));
    }

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

    public void showCommunicator(boolean isVisible) {
        this.mainFrame.pack();
        this.mainFrame.setVisible(isVisible);
    }

    public static void main(String args[]) {

        CommunicatorMain communicatorMain = new CommunicatorMain();

        communicatorMain.showCommunicator(true);
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }
}
