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

import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;

import com.l2fprod.gui.plaf.skin.Skin;
import com.l2fprod.gui.plaf.skin.SkinLookAndFeel;

// import examples.demo;

/**
 * @author Yana Stamcheva
 * 
 * Starts the GUI application using the SkinLookAndFeel of l2fprod.
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
            // Instantiate the look and feel locally so that it gets loaded
            // through the OSGI class loader and not the system one. Setting
            // the UIDefaults "ClassLoader" property does not seem to do the
            // trick and it looks like it is only being used when loading
            // resources.
            SkinLookAndFeel slnf = new SkinLookAndFeel();

            SkinLookAndFeel.setSkin(SkinLookAndFeel
                .loadThemePackDefinition(getClass()
                    .getClassLoader()
                    .getResource(
                            "net/java/sip/communicator/impl/gui/themepacks/"
                                    + "aquathemepack/skinlf-themepack.xml")));

            // we need to set the UIDefaults class loader so that it may access
            // resources packed inside OSGI bundles
            UIManager.put("ClassLoader", getClass()
                    .getClassLoader());

            UIManager.setLookAndFeel(slnf);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setThemePack(String themePack) {

        try {
            if (themePack.endsWith(".xml")) {

                SkinLookAndFeel.setSkin(SkinLookAndFeel
                        .loadThemePackDefinition(new File(
                                themePack).toURL()));

                UIManager.put("ClassLoader", getClass()
                        .getClassLoader());
                UIManager.setLookAndFeel
                    ("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");

            } else if (themePack.startsWith("class:")) {

                String classname = themePack
                        .substring("class:".length());
                SkinLookAndFeel.setSkin((Skin) Class.forName(
                        classname).newInstance());
                UIManager.setLookAndFeel
                    ("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");

            } else if (themePack.startsWith("theme:")) {

                String classname = themePack
                        .substring("theme:".length());
                MetalTheme theme = (MetalTheme) Class.forName(
                        classname).newInstance();
                MetalLookAndFeel metal = new MetalLookAndFeel();
                MetalLookAndFeel.setCurrentTheme(theme);
                UIManager.setLookAndFeel(metal);
                
            } else {

                SkinLookAndFeel.setSkin(SkinLookAndFeel
                        .loadThemePack(themePack));
                UIManager.setLookAndFeel
                    ("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");
            }
          
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void showCommunicator(boolean isVisible) {

        mainFrame.pack();

        mainFrame.setVisible(isVisible);
    }

    public static void main(String args[]) {

        CommunicatorMain communicatorMain = new CommunicatorMain();

        communicatorMain.showCommunicator(true);
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }
}
