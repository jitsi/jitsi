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
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.java.sip.communicator.impl.gui.main.customcontrols.events.CloseListener;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.login.LoginManager;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * @author Yana Stamcheva
 */
public class WelcomeWindow extends JDialog
    implements ActionListener {

    private JLabel welcomeLabel = new JLabel(
        "<html><font size=4 style=bold>"
        + "The SIP Communicator is currently under active development."
        + "The version you are running is only experimental and WILL NOT "
        + "work as expected. Please refer to "
        + "<a href=http://sip-communicator.org>http://sip-communicator.org</a>"
        + " for more information.</font></html>");

    private WindowBackground windowBackground = new WindowBackground();

    private JButton continueButton 
        = new JButton(Messages.getString("continue"));

    private JButton exitButton = new JButton(Messages.getString("exit"));

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private CommunicatorMain communicator;

    private LoginManager loginManager;

    private BundleContext bc;

    private Logger logger = Logger.getLogger(WelcomeWindow.class.getName());

    public WelcomeWindow(CommunicatorMain c,
            LoginManager loginManager, BundleContext context) {
        super(c.getMainFrame(), Messages.getString("warning"));
        
        this.bc = context;
        this.communicator = c;
        this.loginManager = loginManager;

        this.getRootPane().setDefaultButton(continueButton);
        
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

        this.windowBackground.setBorder(BorderFactory.createEmptyBorder(5, 10,
                5, 5));
        
        this.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        
        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                dispose();
                communicator.showCommunicator(true);
                SwingUtilities.invokeLater(new RunLogin());
            }
        });
        
        getRootPane().getActionMap().put("close", new CloseAction());
        getRootPane().getActionMap().put("exit", new ExitAction());
        
        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, 
                KeyEvent.ALT_DOWN_MASK), "exit");
    }

    private void setTransparent(boolean transparent) {
        this.buttonPanel.setOpaque(!transparent);
    }

    public void showWindow() {

        this.pack();

        this.setWindowLocation();

        this.setVisible(true);
    }

    private void setWindowLocation() {

        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth()
                - this.getWidth()) / 2;

        int y = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight()
                - this.getHeight()) / 2;

        this.setLocation(x, y);
    }

    private class WindowBackground extends JPanel {

        private Image bgImage 
            = ImageLoader.getImage(ImageLoader.LOGIN_WINDOW_LOGO);

        public WindowBackground() {
            this.setPreferredSize(new Dimension(bgImage.getWidth(this), bgImage
                    .getHeight(this)));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.drawImage(bgImage, 0, 0, null);

            g2.setColor(new Color(255, 255, 255, 200));

            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(continueButton)) {
            this.dispose();
            this.communicator.showCommunicator(true);
            SwingUtilities.invokeLater(new RunLogin());
        } else {            
            try {
                this.bc.getBundle(0).stop();
            } catch (BundleException ex) {
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
    
    private class CloseAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            dispose();
            communicator.showCommunicator(true);
            SwingUtilities.invokeLater(new RunLogin());
        }
    };
    
    private class ExitAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            try {
                bc.getBundle(0).stop();
            } catch (BundleException ex) {
                logger.error("Failed to gently shutdown Oscar", ex);
            }
            System.exit(0);
        }
    };
}
