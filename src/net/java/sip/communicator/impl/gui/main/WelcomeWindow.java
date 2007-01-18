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
    extends SIPCommFrame
    implements ActionListener {

    private JLabel welcomeLabel = new JLabel(
        "<html><font size=4 style=bold>"
        + "The SIP Communicator is currently under active development."
        + "The version you are running is only experimental and WILL NOT "
        + "work as expected. Please refer to "
        + "<a href=http://sip-communicator.org>http://sip-communicator.org</a>"
        + " for more information.</font></html>");

    private WindowBackground windowBackground = new WindowBackground();
    
    private I18NString closeString = Messages.getI18NString("close");
    
    private JButton closeButton = new JButton(closeString.getText());

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private Logger logger = Logger.getLogger(WelcomeWindow.class.getName());

    /**
     * Constructs the <tt>WelcomeWindow</tt>.
     * @param c The application main class that runs the main application
     * window.
     * @param loginManager The login manager that runs all login windows.
     */
    public WelcomeWindow()
    {   
        this.setTitle(Messages.getI18NString("warning").getText());

        this.closeButton.setMnemonic(closeString.getMnemonic());
        
        this.getRootPane().setDefaultButton(closeButton);

        this.closeButton.addActionListener(this);
        
        this.buttonPanel.add(closeButton);
        
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
    public void actionPerformed(ActionEvent e)
    {   
        dispose();        
    }
    
    protected void close(boolean isEscaped)
    {
        dispose();        
    }
}
