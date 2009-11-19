/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

public class ActionMenuPanel
    extends JPanel
    implements  ActionListener
{
    private static final boolean setButtonContentAreaFilled = isWindows();

    private static boolean isWindows()
    {
        String osName = System.getProperty("os.name");
        return (osName != null) && (osName.indexOf("Windows") != -1);
    }

    private final Logger logger = Logger.getLogger(ActionMenuPanel.class);

    private Color baseStartColor = new Color(
        GuiActivator.getResources().getColor(
            "service.gui.FAVORITES_PANEL_BACKGROUND"));

    private Color baseEndColor = new Color(
        GuiActivator.getResources().getColor(
            "service.gui.FAVORITES_PANEL_BACKGROUND_GRADIENT"));

    private Color startBgColor = new Color( baseStartColor.getRed(),
                                            baseStartColor.getGreen(),
                                            baseStartColor.getBlue(),
                                            220);

    private Color endBgColor = new Color(   baseEndColor.getRed(),
                                            baseEndColor.getGreen(),
                                            baseEndColor.getBlue(),
                                            220);

    public ActionMenuPanel()
    {
        super(new FlowLayout(FlowLayout.CENTER, 15, 15));

        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.initListeners();

        addMoreActionsButton(
            ImageLoader.getImage(ImageLoader.QUICK_MENU_ADD_ICON),
            "Contacts",
            "addContact");
        addMoreActionsButton(
            ImageLoader.getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON),
            "Options",
            "options");
        addMoreActionsButton(
            ImageLoader.getImage(
                ConfigurationManager.isShowOffline()
                    ? ImageLoader.QUICK_MENU_SHOW_OFFLINE_ICON
                    : ImageLoader.QUICK_MENU_HIDE_OFFLINE_ICON),
            "Show/Hide",
            "showOffline");

        AudioNotifierService audioNotifier = GuiActivator.getAudioNotifier();
        if (audioNotifier != null)
            addMoreActionsButton(
                ImageLoader.getImage(
                    audioNotifier.isMute()
                        ? ImageLoader.QUICK_MENU_SOUND_OFF_ICON
                        : ImageLoader.QUICK_MENU_SOUND_ON_ICON),
                "Sound",
                "sound");

        addMoreActionsButton(
            ImageLoader.getImage(ImageLoader.QUICK_MENU_MY_CHAT_ROOMS_ICON),
            "Chatrooms",
            "chatRooms");

        this.initPluginComponents();
    }

    private SIPCommButton addMoreActionsButton(Image bgImage,
                                               String text,
                                               String name)
    {
        SIPCommButton button = new SIPCommButton(bgImage);
        if (name != null)
        {
            button.setName(name);
            button.addActionListener(this);
        }
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setToolTipText(text);
        // Windows defaults to a non-transparent button background.
        if (setButtonContentAreaFilled)
            button.setContentAreaFilled(false);

        JLabel buttonLabel = new JLabel(text);
        buttonLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        buttonLabel.setForeground(Color.WHITE);

        Font font = buttonLabel.getFont();
        buttonLabel.setFont(
            font.deriveFont(font.getStyle(), font.getSize() - 3));

        JPanel buttonPanel = new TransparentPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setPreferredSize(new Dimension(60, 60));

        buttonPanel.add(button);
        buttonPanel.add(buttonLabel);

        buttonPanel.doLayout();

        add(buttonPanel);
        return button;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();

            GradientPaint p =
                new GradientPaint(width / 2, 0, startBgColor, width / 2,
                    height, endBgColor);

            g2.setPaint(p);
            g2.fillRect(0, 0, width - 1, height - 1);

            g2.setColor(baseStartColor);
            g2.drawRect(0, 0, width - 1, height - 1);
        }
        finally
        {
            g.dispose();
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        SIPCommButton button = (SIPCommButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equals("addContact"))
        {
            AddContactWizard wizard = new AddContactWizard(
                    GuiActivator.getUIService().getMainFrame());

            wizard.setVisible(true);
        }
        else if (buttonName.equals("options"))
        {
            GuiActivator.getUIService().setConfigurationWindowVisible(true);
        }
        else if (buttonName.equals("showOffline"))
        {
            boolean isShowOffline = ConfigurationManager.isShowOffline();

            updateShowOfflineButton(button, !isShowOffline);

            GuiActivator.getUIService().getMainFrame().getContactListPanel()
                .getContactList().setShowOffline(!isShowOffline);
        }
        else if (buttonName.equals("sound"))
        {
            boolean isMute = GuiActivator.getAudioNotifier().isMute();

            updateMuteButton(button, !isMute);
            GuiActivator.getAudioNotifier().setMute(!isMute);
        }
        else if (buttonName.equals("chatRooms"))
        {
            ChatRoomListDialog.showChatRoomListDialog();
        }
    }

    public void updateMuteButton(SIPCommButton soundButton, boolean isMute)
    {
        if(!isMute)
            soundButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.QUICK_MENU_SOUND_ON_ICON));
        else
            soundButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.QUICK_MENU_SOUND_OFF_ICON));

        soundButton.repaint();
        this.getParent().repaint();
    }

    public void updateShowOfflineButton(SIPCommButton showOfflineButton,
                                        boolean isShowOffline)
    {
        if(!isShowOffline)
            showOfflineButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.QUICK_MENU_HIDE_OFFLINE_ICON));
        else
            showOfflineButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.QUICK_MENU_SHOW_OFFLINE_ICON));

        showOfflineButton.repaint();
        this.getParent().repaint();
    }

    /**
     * We need to add explicitly empty mouse listeners in order to catch all
     * mouse events from the glass pane. Otherwise all underlying panel
     * listeners are active.
     */
    private void initListeners()
    {
        this.addMouseListener(new MouseAdapter(){});

        this.addMouseMotionListener(new MouseMotionAdapter(){});
    }

    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                FavoritesButton.class.getName(), null);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.trace("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                FavoritesButton c = (FavoritesButton)
                    GuiActivator.bundleContext.getService(serRef);

                addPluginComponent(c);
            }
        }
    }

    private void addPluginComponent(final FavoritesButton c)
    {
        SIPCommButton button
            = addMoreActionsButton(
                ImageLoader.getBytesInImage(c.getImage()),
                c.getText(),
                null);

        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                c.actionPerformed();
            }
        });
    }
}
