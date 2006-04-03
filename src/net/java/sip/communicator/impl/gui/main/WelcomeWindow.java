package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.login.LoginManager;
import net.java.sip.communicator.impl.gui.main.login.LoginWindow;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.BrowserLauncher;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.util.Logger;

public class WelcomeWindow extends JDialog
    implements ActionListener{

    private JLabel welcomeLabel 
        = new JLabel("<html><font style=bold>"
                + "The SIP Communicator is currently under active development."
                + "The version you are running is only experimental and WILL NOT "
                + "work as expected. Please refer to "
                + "<a href=http://sip-communicator.org>http://sip-communicator.org</a>"
                + " for more information.</font></html>");
    
    private WindowBackground windowBackground = new WindowBackground();
    
    private JButton continueButton = new JButton(Messages.getString("continue"));
    private JButton exitButton = new JButton(Messages.getString("exit"));
    
    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            
    private CommunicatorMain communicator;
    
    private LoginManager loginManager;
    
    private BundleContext bc;
    
    private Logger logger = Logger.getLogger(WelcomeWindow.class.getName());
    
    public WelcomeWindow(CommunicatorMain communicator, 
                                            LoginManager loginManager,
                                            BundleContext bc){
        super(communicator.getMainFrame(), Messages.getString("warning"));
        
        this.bc = bc;
        this.communicator = communicator;
        this.loginManager = loginManager;
        
        this.continueButton.addActionListener(this);
        this.exitButton.addActionListener(this);
        
        this.buttonPanel.add(continueButton);
        this.buttonPanel.add(exitButton);
        
        this.windowBackground.setLayout(new BorderLayout());
        this.getContentPane().setLayout(new BorderLayout());
        
        this.windowBackground.add(welcomeLabel, BorderLayout.CENTER);
        this.windowBackground.add(buttonPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(windowBackground);
        
        this.setTransparent(true);
        
        this.windowBackground.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
    }
    
    private void setTransparent(boolean transparent){        
        this.buttonPanel.setOpaque(!transparent);
    }
    
    
    public void showWindow(){
    
        this.pack();
        
        this.setWindowLocation();       
        
        this.setVisible(true);
    }
    
    private void setWindowLocation(){
        
        int x = (int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()
                - this.getWidth()) / 2;
        
        int y = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight()
                - this.getHeight()) / 2;
        
        this.setLocation(x, y);
    }

    private class WindowBackground extends JPanel {

        Image bgImage = ImageLoader.getImage(ImageLoader.LOGIN_WINDOW_LOGO);
        
        public WindowBackground(){
            this.setPreferredSize(
                    new Dimension(bgImage.getWidth(this), bgImage.getHeight(this)));
        }
        
        protected void paintComponent(Graphics g) {
            
            super.paintComponent(g);
            
            AntialiasingManager.activateAntialiasing(g);
            
            Graphics2D g2 = (Graphics2D) g;            
            
            g2.drawImage(bgImage,
                    0, 0, null);
            
            g2.setColor(new Color(255, 255, 255, 200));

            g2.fillRect(0, 0, getWidth(), getHeight());            
        }
       
    }

    public void actionPerformed(ActionEvent e) {
       if(e.getSource().equals(continueButton)){           
           this.dispose();
           this.communicator.showCommunicator(true);           
           SwingUtilities.invokeLater(new RunLogin());
       }
       else{
           try{
               this.bc.getBundle(0).stop();
           }
           catch (BundleException ex) {
               logger.error("Failed to gently shutdown Oscar", ex);
           }          
           System.exit(0);
       }
    }
    
    private class RunLogin implements Runnable {
        public void run() {
            loginManager.showLoginWindows(communicator.getMainFrame());            
        }
    }
}
