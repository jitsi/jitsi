package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.osgi.framework.*;
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

    /**
     * Constructs the <tt>WelcomeWindow</tt>.
     * @param c The application main class that runs the main application
     * window.
     * @param loginManager The login manager that runs all login windows.
     * @param context The bundle context.
     */
    public WelcomeWindow(CommunicatorMain c,
            LoginManager loginManager, BundleContext context) {
        super(c.getMainFrame(), Messages.getString("warning"));

        this.bc = context;
        this.communicator = c;
        this.loginManager = loginManager;

        this.exitButton.setMnemonic('X');
        this.continueButton.setMnemonic('C');

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

        this.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);

        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                dispose();
                communicator.showCommunicator(true);
                SwingUtilities.invokeLater(new RunLogin());
            }
        });

        getRootPane().getActionMap().put("close", new CloseAction());

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
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
     * Shows this window.
     */
    public void showWindow() {

        this.pack();

        this.setWindowLocation();

        this.setVisible(true);
    }

    /**
     * Sets the location of this window.
     */
    private void setWindowLocation() {

        int x = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth()
                - this.getWidth()) / 2;

        int y = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight()
                - this.getHeight()) / 2;

        this.setLocation(x, y);
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
                this.bc.getBundle(0).stop();
            } catch (BundleException ex) {
                logger.error("Failed to gently shutdown Oscar", ex);
            }
            System.exit(0);
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
     * The <tt>CloseAction</tt> is an <tt>AbstractAction</tt> that
     * closes this <tt>WelcomeWindow</tt> and shows the main application
     * window and the login windows.
     */
    private class CloseAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            dispose();
            communicator.showCommunicator(true);
            SwingUtilities.invokeLater(new RunLogin());
        }
    };

    /**
     * The <tt>ExitAction</tt> is an <tt>AbstractAction</tt> that
     * exits the application.
     */
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
