/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.osdependent.jdic;

import java.awt.*;
import java.awt.Taskbar.*;
import java.awt.image.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.*;

import lombok.extern.slf4j.*;
import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.impl.osdependent.systemtray.SystemTray;
import net.java.sip.communicator.impl.osdependent.systemtray.TrayIcon;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * The <tt>Systray</tt> provides a Icon and the associated <tt>TrayMenu</tt>
 * in the system tray using the Jdic library.
 *
 * @author Nicolas Chamouard
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Symphorien Wanko
 * @author Pawel Domas
 */
@Slf4j
public class SystrayServiceJdicImpl
    extends AbstractSystrayService
{
    /**
     * The systray.
     */
    private final SystemTray systray;

    /**
     * The icon in the system tray.
     */
    private TrayIcon trayIcon;

    /**
     * The menu that spring with a right click.
     */
    private Object menu;

    /**
     * The various icons used on the systray
     */
    private ImageIcon currentIcon;

    private ImageIcon logoIcon;

    private ImageIcon logoIconOffline;

    private ImageIcon logoIconAway;

    private ImageIcon logoIconExtendedAway;

    private ImageIcon logoIconFFC;

    private ImageIcon logoIconDND;

    private ImageIcon logoIconWhite;

    /**
     * The dock Icons used only in Mac version
     */
    private URL dockIconOnline;

    private URL dockIconOffline;

    private URL dockIconAway;

    private URL dockIconExtendedAway;

    private URL dockIconFFC;

    private URL dockIconDND;

    private Image originalDockImage = null;

    private boolean initialized = false;

    /**
     * The listener which gets notified about pop-up message events (e.g. clicks
     * on the pop-up).
     */
    private final SystrayPopupMessageListener popupMessageListener
        = new SystrayPopupMessageListenerImpl();

    /**
     * Initializes a new <tt>SystrayServiceJdicImpl</tt> instance.
     */
    public SystrayServiceJdicImpl(BundleContext bundleContext,
        ConfigurationService configService)
    {
        super(bundleContext, configService);
        SystemTray systray;
        try
        {
            systray = SystemTray.getSystemTray();
        }
        catch (Exception t)
        {
            systray = null;
            if (!GraphicsEnvironment.isHeadless())
                logger.error("Failed to create a systray!", t);
        }

        this.systray = systray;
        if (this.systray != null)
        {
            initSystray();
        }
    }

    @Override
    public Map<String, String> getSystrayModes()
    {
        return new HashMap<>()
        {{
            put("disabled", "service.systray.mode.DISABLED");
            if (java.awt.SystemTray.isSupported())
            {
                put("native", "service.systray.mode.NATIVE");
            }

            if (!OSUtils.IS_MAC && !OSUtils.IS_WINDOWS)
            {
                put("appindicator",
                    "service.systray.mode.APPINDICATOR");
                put("appindicator_static",
                    "service.systray.mode.APPINDICATOR_STATIC");
            }
        }};
    }

    @Override
    public String getActiveSystrayMode()
    {
        return SystemTray.getSystemTrayMode();
    }

    /**
     * Initializes the systray icon and related listeners.
     */
    private void initSystray()
    {
        UIService uiService = OsDependentActivator.getUIService();

        if (uiService == null)
        {
            /*
             * Delay the call to the #initSystray() method until the UIService
             * implementation becomes available.
             */
            try
            {
                OsDependentActivator.bundleContext.addServiceListener(
                        new DelayedInitSystrayServiceListener(),
                        '('
                            + Constants.OBJECTCLASS
                            + '='
                            + UIService.class.getName()
                            + ')');
            }
            catch (InvalidSyntaxException ise)
            {
                /*
                 * Oh, it should not really happen. Besides, it is not clear at
                 * the time of this writing what is supposed to happen in the
                 * case of such an exception here.
                 */
            }
            return;
        }

        Pair<Object, Object> createdMenu = TrayMenuFactory.createTrayMenu(
            systray.useSwingPopupMenu(),
            systray.supportsDynamicMenu());
        menu = createdMenu.getLeft();

        boolean isMac = OSUtils.IS_MAC;

        logoIcon = Resources.getImage("service.systray.TRAY_ICON_WINDOWS");
        logoIconOffline = Resources.getImage(
            "service.systray.TRAY_ICON_WINDOWS_OFFLINE");
        logoIconAway = Resources.getImage(
            "service.systray.TRAY_ICON_WINDOWS_AWAY");
        logoIconExtendedAway = Resources.getImage(
            "service.systray.TRAY_ICON_WINDOWS_EXTENDED_AWAY");
        logoIconFFC = Resources.getImage(
            "service.systray.TRAY_ICON_WINDOWS_FFC");
        logoIconDND = Resources.getImage(
            "service.systray.TRAY_ICON_WINDOWS_DND");

        // If we're running under Mac OS X, we use special black and white
        // icons without background.
        if (isMac)
        {
            logoIcon = Resources.getImage("service.systray.TRAY_ICON_MACOSX");
            logoIconWhite = Resources.getImage(
                "service.systray.TRAY_ICON_MACOSX_WHITE");
        }

        /*
         * Default to set offline , if any protocols become online will set it
         * to online.
         */
        currentIcon = isMac ? logoIcon : logoIconOffline;

        trayIcon
            = systray.createTrayIcon(
                    currentIcon,
                    Resources.getApplicationString(
                            "service.gui.APPLICATION_NAME"),
                    menu);
        trayIcon.setIconAutoSize(true);

        if (isMac)
        {
            // init dock Icons
            dockIconOnline = Resources.getImageURL(
                "service.systray.DOCK_ICON_ONLINE");
            dockIconOffline = Resources.getImageURL(
                "service.systray.DOCK_ICON_OFFLINE");
            dockIconAway = Resources.getImageURL(
                "service.systray.DOCK_ICON_AWAY");
            dockIconExtendedAway = Resources.getImageURL(
                "service.systray.DOCK_ICON_EXTENDED_AWAY");
            dockIconFFC =
                    Resources.getImageURL("service.systray.DOCK_ICON_FFC");
            dockIconDND =
                    Resources.getImageURL("service.systray.DOCK_ICON_DND");
        }

        //Show/hide the contact list when user clicks on the systray.
        final Object defaultActionItem = createdMenu.getRight();

        /*
         * Change the Mac OS X icon with the white one when the pop-up menu
         * appears.
         */
        if (isMac)
        {
            TrayMenuFactory.addPopupMenuListener(
                    menu,
                    new PopupMenuListener()
                    {
                        public void popupMenuWillBecomeVisible(PopupMenuEvent e)
                        {
                            ImageIcon newIcon = logoIconWhite;
                            trayIcon.setIcon(newIcon);
                            currentIcon = newIcon;
                        }

                        public void popupMenuWillBecomeInvisible(
                                PopupMenuEvent e)
                        {
                            ImageIcon newIcon = logoIcon;
                            getTrayIcon().setIcon(newIcon);
                            currentIcon = newIcon;
                        }

                        public void popupMenuCanceled(PopupMenuEvent e)
                        {
                            popupMenuWillBecomeInvisible(e);
                        }
                    });
        }

        PopupMessageHandler pmh = null;

        if (!isMac)
        {
            pmh = new PopupMessageHandlerTrayIconImpl(trayIcon);
            ServiceRegistration<PopupMessageHandler> ref
                = bundleContext.registerService(
                    PopupMessageHandler.class,
                    pmh,
                    null);
            addPopupHandler(pmh, ref.getReference());
        }

        initHandlers();

        /*
         * Either we have an incorrect configuration value or the default pop-up
         * handler is not available yet. We will use the available pop-up
         * handler and will automatically switch to the configured one when it
         * becomes available. We will be aware of it since we listen for new
         * registered services in the BundleContext.
         */
        if ((getActivePopupMessageHandler() == null) && (pmh != null))
            setActivePopupMessageHandler(pmh);

        SwingUtilities.invokeLater(() ->
        {
            systray.addTrayIcon(trayIcon);
            trayIcon.setDefaultAction(defaultActionItem);
        });

        initialized = true;
        uiService.setMainWindowCanHide(true);
    }

    /**
     * Sets a new Systray icon.
     *
     * @param imageType the type of the image to set.
     */
    public void setSystrayIcon(int imageType)
    {
        if (!checkInitialized())
            return;

        boolean isMac = OSUtils.IS_MAC;
        ImageIcon systrayIconToSet = null;

        switch (imageType)
        {
        case SystrayService.SC_IMG_TYPE:
            systrayIconToSet
                = (isMac && TrayMenuFactory.isVisible(menu))
                    ? logoIconWhite
                    : logoIcon;
            break;
        case SystrayService.SC_IMG_OFFLINE_TYPE:
            if (!isMac)
                systrayIconToSet = logoIconOffline;
            break;
        case SystrayService.SC_IMG_AWAY_TYPE:
            if (!isMac)
                systrayIconToSet = logoIconAway;
            break;
        case SystrayService.SC_IMG_EXTENDED_AWAY_TYPE:
            if (!isMac)
                systrayIconToSet = logoIconExtendedAway;
            break;
        case SystrayService.SC_IMG_FFC_TYPE:
            if (!isMac)
                systrayIconToSet = logoIconFFC;
            break;
        case SystrayService.SC_IMG_DND_TYPE:
            if (!isMac)
                systrayIconToSet = logoIconDND;
            break;
        }

        if (systrayIconToSet != null)
        {
            this.trayIcon.setIcon(systrayIconToSet);
            this.currentIcon = systrayIconToSet;
        }

        if (isMac)
        {
            URL dockIconURLToSet;

            switch (imageType)
            {
            case SystrayService.SC_IMG_TYPE:
                dockIconURLToSet = dockIconOnline;
                break;
            case SystrayService.SC_IMG_OFFLINE_TYPE:
                dockIconURLToSet = dockIconOffline;
                break;
            case SystrayService.SC_IMG_AWAY_TYPE:
                dockIconURLToSet = dockIconAway;
                break;
            case SystrayService.SC_IMG_EXTENDED_AWAY_TYPE:
                dockIconURLToSet = dockIconExtendedAway;
                break;
            case SystrayService.SC_IMG_FFC_TYPE:
                dockIconURLToSet = dockIconFFC;
                break;
            case SystrayService.SC_IMG_DND_TYPE:
                dockIconURLToSet = dockIconDND;
                break;
            default:
                dockIconURLToSet = null;
                break;
            }

            try
            {
                if (Taskbar.isTaskbarSupported())
                {
                    var taskbar = Taskbar.getTaskbar();
                    if (taskbar != null && taskbar.isSupported(Feature.ICON_IMAGE))
                    {
                        if (originalDockImage == null)
                        {
                            originalDockImage = taskbar.getIconImage();
                        }

                        if (dockIconURLToSet != null)
                        {
                            taskbar.setIconImage(
                                    Toolkit.getDefaultToolkit().getImage(
                                            dockIconURLToSet));
                        }
                        else if (originalDockImage != null)
                        {
                            taskbar.setIconImage(originalDockImage);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Failed to change taskbar icon", e);
            }
        }
    }

    @Override
    public boolean checkInitialized()
    {
        return initialized;
    }

    /**
     * Set the number of pending notifications to the the application icon
     * (Dock on OSX, TaskBar on Windows, nothing on Linux currently).
     */
    @Override
    public void setNotificationCount(int count)
    {
        if (Taskbar.isTaskbarSupported())
        {
            var taskbar = Taskbar.getTaskbar();
            if (taskbar != null && taskbar.isSupported(Feature.ICON_BADGE_NUMBER))
            {
                if (count == 0)
                {
                    taskbar.setIconBadge(null);
                }
                else
                {
                    taskbar.setIconBadge(Integer.toString(count));
                }
            }
            else if (taskbar != null && taskbar.isSupported(Feature.ICON_BADGE_IMAGE_WINDOW))
            {
                if (count == 0)
                {
                    taskbar.setIconImage(null);
                }
                else
                {
                    taskbar.setIconImage(createOverlayImage(Integer.toString(count)));
                }
            }
        }
    }

    private BufferedImage createOverlayImage(String text)
    {
        int size = 16;
        BufferedImage image =
            new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        //background
        g.setPaint(new Color(0, 0, 0, 102));
        g.fillRoundRect(0, 0, size, size, size, size);

        //filling
        int mainRadius = 14;
        g.setPaint(new Color(255, 98, 89));
        g.fillRoundRect(size / 2 - mainRadius / 2, size / 2 - mainRadius / 2,
            mainRadius, mainRadius, size, size);

        //text
        Font font = g.getFont();
        g.setFont(new Font(font.getName(), Font.BOLD, 9));
        FontMetrics fontMetrics = g.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        g.setColor(Color.white);
        g.drawString(text, size / 2 - textWidth / 2,
            size / 2 - fontMetrics.getHeight() / 2 + fontMetrics.getAscent());

        return image;
    }

    /**
     * @return the trayIcon
     */
    public TrayIcon getTrayIcon()
    {
        return trayIcon;
    }

    /**
     * Set the handler which will be used for popup message
     * @param newHandler the handler to set. providing a null handler is like
     * disabling popup.
     * @return the previously used popup handler
     */
    public PopupMessageHandler setActivePopupMessageHandler(
            PopupMessageHandler newHandler)
    {
        PopupMessageHandler oldHandler = getActivePopupHandler();

        if (oldHandler != null)
            oldHandler.removePopupMessageListener(popupMessageListener);
        if (newHandler != null)
            newHandler.addPopupMessageListener(popupMessageListener);

        return super.setActivePopupMessageHandler(newHandler);
    }

    /** our listener for popup message click */
    private static class SystrayPopupMessageListenerImpl
        implements SystrayPopupMessageListener
    {

        /**
         * Handles a user click on a systray popup message. If the popup
         * notification was the result of an incoming message from a contact,
         * the chat window with that contact will be opened, if not already, and
         * brought to front.
         *
         * @param evt the event triggered when user clicks on a systray popup
         * message
         */
        public void popupMessageClicked(SystrayPopupMessageEvent evt)
        {
            Object o = evt.getTag();

            if (o instanceof Contact)
                OsDependentActivator.getUIService().
                    getChat((Contact) o).setChatVisible(true);
        }
    }

    /**
     * Implements a <tt>ServiceListener</tt> which waits for an
     * <tt>UIService</tt> implementation to become available, invokes
     * {@link #initSystray()} and unregisters itself.
     */
    private class DelayedInitSystrayServiceListener
        implements ServiceListener
    {
        public void serviceChanged(ServiceEvent serviceEvent)
        {
            if (serviceEvent.getType() == ServiceEvent.REGISTERED)
            {
                UIService uiService = OsDependentActivator.getUIService();

                if (uiService != null)
                {
                    /*
                     * This ServiceListener has successfully waited for an
                     * UIService implementation to become available so it no
                     * longer need to listen.
                     */
                    OsDependentActivator.bundleContext.removeServiceListener(
                            this);

                    if (!initialized && systray != null)
                        initSystray();
                }
            }
        }
    }
}
