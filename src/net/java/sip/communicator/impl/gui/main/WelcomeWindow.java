package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.util.*;

/**
 * The <tt>WelcomeWindow</tt> is the window shown at the very beginning,
 * when the application is run and indicates that the current version is
 * not a final release and may not work as expected.
 *
 * @author Yana Stamcheva
 */
public class WelcomeWindow
    extends SIPCommDialog
    implements ActionListener {

    private JLabel welcomeLabel = new JLabel(
        "<html><font size=4 style=bold>"
        + "The SIP Communicator is currently under active development."
        + "The version you are running is only experimental and WILL NOT "
        + "work as expected. Please refer to "
        + "<a href=http://sip-communicator.org>http://sip-communicator.org</a>"
        + " for more information.</font></html>");

    private WindowBackground windowBackground = new WindowBackground();

    private I18NString continueString = Messages.getI18NString("continue");
    
    private I18NString exitString = Messages.getI18NString("exit");
    
    private JButton continueButton
        = new JButton(continueString.getText());

    private JButton exitButton = new JButton(exitString.getText());

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private CommunicatorMain communicator;

    private LoginManager loginManager;

    private Logger logger = Logger.getLogger(WelcomeWindow.class.getName());

    /**
     * Constructs the <tt>WelcomeWindow</tt>.
     * @param c The application main class that runs the main application
     * window.
     * @param loginManager The login manager that runs all login windows.
     */
    public WelcomeWindow(CommunicatorMain c,
            LoginManager loginManager) {
        super(c.getMainFrame());

        this.communicator = c;
        this.loginManager = loginManager;

        this.setTitle(Messages.getI18NString("warning").getText());

        this.exitButton.setMnemonic(exitString.getMnemonic());
        this.continueButton.setMnemonic(continueString.getMnemonic());

        this.getRootPane().setDefaultButton(continueButton);

        this.continueButton.addActionListener(this);
        this.exitButton.addActionListener(this);

        this.buttonPanel.add(continueButton);
        this.buttonPanel.add(exitButton);

        this.windowBackground.setLayout(new BorderLayout());
        this.getContentPane().setLayout(new BorderLayout());

        this.welcomeLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));
        this.windowBackground.add(welcomeLabel, BorderLayout.CENTER);
        this.windowBackground.add(buttonPanel, BorderLayout.SOUTH);

        this.getContentPane().add(windowBackground);

        this.setTransparent(true);

        this.windowBackground.setBorder(BorderFactory.createEmptyBorder(5, 10,
                5, 5));

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                dispose();
                communicator.showCommunicator(true);
                SwingUtilities.invokeLater(new RunLogin());
            }
        });
    }

    /**
     * Sets a transparent background to some components, because of the
     * background image of this window.
     *
     * @param transparent <code>true</code> to make components transparent,
     * <code>false</code> otherwise.
     */
    private void setTransparent(boolean transparent) {
        this.buttonPanel.setOpaque(!transparent);
    }
   
    /**
     * Constructs the window background in order to have a background image.
     */
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

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on one
     * of the buttons.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(continueButton)) {
            this.dispose();
            this.communicator.showCommunicator(true);
            SwingUtilities.invokeLater(new RunLogin());
        } else {
            try {
                GuiActivator.bundleContext.getBundle(0).stop();
            } catch (BundleException ex) {
                logger.error("Failed to gently shutdown Felix", ex);
                System.exit(0);
            }
            //stopping a bundle doesn't leave the time to the felix thread to
            //properly end all bundles and call their Activator.stop() methods.
            //if this causes problems don't uncomment the following line but
            //try and see why felix isn't exiting (suggesting: is it running
            //in embedded mode?)
            //System.exit(0);

        }
    }

    /**
     * The <tt>RunLogin</tt> implements the Runnable interface and is used to
     * shows the login windows in new thread.
     */
    private class RunLogin implements Runnable {
        public void run() {
            loginManager.runLogin(communicator.getMainFrame());
        }
    }

    /**
     * The <tt>ExitAction</tt> is an <tt>AbstractAction</tt> that
     * exits the application.
     */
    private class ExitAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            try {
                GuiActivator.bundleContext.getBundle(0).stop();
            } catch (BundleException ex) {
                logger.error("Failed to gently shutdown Oscar", ex);
            }
            System.exit(0);
        }
    }

    protected void close(boolean isEscaped)
    {
        dispose();
        communicator.showCommunicator(true);
        SwingUtilities.invokeLater(new RunLogin());
    }
}
