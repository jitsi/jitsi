/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.io.File;
import java.net.MalformedURLException;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import net.java.sip.communicator.impl.gui.lookandfeel.SIPCommDefaultTheme;
import net.java.sip.communicator.impl.gui.lookandfeel.SIPCommLookAndFeel;
import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * Starts the GUI application using the SkinLookAndFeel of l2fprod.
 *
 * @author Yana Stamcheva
 */
public class CommunicatorMain {

    private MainFrame mainFrame;

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

        try {
            SIPCommLookAndFeel lf = new SIPCommLookAndFeel();
            SIPCommLookAndFeel.setCurrentTheme(new SIPCommDefaultTheme());

            // we need to set the UIDefaults class loader so that it may access
            // resources packed inside OSGI bundles
            UIManager.put("ClassLoader", getClass()
                    .getClassLoader());

            UIManager.setLookAndFeel(lf);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
