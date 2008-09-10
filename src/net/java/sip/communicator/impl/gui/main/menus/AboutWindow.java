package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;

public class AboutWindow
    extends JDialog
    implements  ActionListener,
                ExportedWindow
{
    private WindowBackground mainPanel = new WindowBackground();

    private JLabel versionLabel = new JLabel(" "
            + System.getProperty("sip-communicator.version"));

    public AboutWindow()
    {
        this.setModal(false);
        this.setResizable(false);

        this.setTitle(
            Messages.getI18NString("aboutWindowTitle",
            new String[]{ GuiActivator.getResources()
                    .getSettingsString("applicationName")}).getText());

        this.mainPanel.setLayout(null);

        this.versionLabel.setFont(Constants.FONT.deriveFont(12));
        this.versionLabel.setForeground(new Color(
            GuiActivator.getResources().getColor("splashScreenTitleColor")));
        this.versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.mainPanel.add(versionLabel);

        Insets insets = mainPanel.getInsets();
        versionLabel.setBounds(370 + insets.left, 307 + insets.top, 200, 20);

        this.getContentPane().add(mainPanel);

        // Close the splash screen on simple click or Esc.
        this.getGlassPane().addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                AboutWindow.this.close(false);
            }
        });

        this.getGlassPane().setVisible(true);

        ActionMap amap = this.getRootPane().getActionMap();

        amap.put("close", new CloseAction());

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

    protected void close(boolean isEscaped)
    {
        this.dispose();
    }

    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            AboutWindow.this.close(true);
        }
    }

    /**
     * Constructs the window background in order to have a background image.
     */
    private class WindowBackground
        extends JPanel
    {
        private Image bgImage;

        public WindowBackground()
        {
            this.setOpaque(true);

            bgImage = ImageLoader.getImage(
                        ImageLoader.ABOUT_WINDOW_BACKGROUND);

            this.setPreferredSize(new Dimension(bgImage.getWidth(this),
                bgImage.getHeight(this)));
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            g2.drawImage(bgImage, 0, 0, null);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        this.dispose();
    }

    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.ABOUT_WINDOW;
    }

    /**
     * This dialog could not be minimized.
     */
    public void minimize()
    {
    }

    /**
     * This dialog could not be maximized.
     */
    public void maximize()
    {
    }

    /**
     * Implements the <tt>ExportedWindow.bringToFront()</tt> method. Brings
     * this window to front.
     */
    public void bringToFront()
    {
        this.toFront();
    }

    public static void activateAntialiasing(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
    }
    
    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }
}
