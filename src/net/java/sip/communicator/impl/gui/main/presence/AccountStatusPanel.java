/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.presence.avatar.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.globaldisplaydetails.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.util.*;

/**
 * The panel shown on the top of the contact list. It contains user name,
 * current status menu and the avatar of the user.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class AccountStatusPanel
    extends TransparentPanel
    implements  RegistrationStateChangeListener,
                PluginComponentListener,
                GlobalDisplayDetailsListener,
                Skinnable
{
    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        AccountStatusPanel.class.getName() +  "OpaquePanelUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            SIPCommOpaquePanelUI.class.getName());
    }

    /**
     * The desired height of the avatar.
     */
    private static final int AVATAR_ICON_HEIGHT = 40;

    /**
     * The desired width of the avatar.
     */
    private static final int AVATAR_ICON_WIDTH = 40;

    /**
     * The image object storing the avatar.
     */
    private final FramedImage accountImageLabel;

    /**
     * The label showing the name of the user.
     */
    private final JLabel accountNameLabel
        = new EmphasizedLabel(
                GuiActivator
                    .getResources().getI18NString("service.gui.ACCOUNT_ME"));

    /**
     * The background color property.
     */
    private Color bgColor;

    /**
     * The background image property.
     */
    private Image logoBgImage;

    /**
     * The combo box containing status menu.
     */
    private final GlobalStatusSelectorBox statusComboBox;

    /**
     * TexturePaint used to paint background image.
     */
    private TexturePaint texture;

    /**
     * The tool bar plug-in container.
     */
    private final TransparentPanel toolbarPluginPanel;

    /**
     * The south plug-in container.
     */
    private final TransparentPanel southPluginPanel;

    /**
     * Keep reference to plugin container or it will loose its
     * listener.
     */
    private final PluginContainer southPluginContainer;

    /**
     * Keep reference to plugin container or it will loose its
     * listener.
     */
    private final PluginContainer mainToolbarPluginContainer;

    /**
     * Creates an instance of <tt>AccountStatusPanel</tt> by specifying the
     * main window, where this panel is added.
     * @param mainFrame the main window, where this panel is added
     */
    public AccountStatusPanel(MainFrame mainFrame)
    {
        super(new BorderLayout(10, 0));

        FramedImageWithMenu imageWithMenu
            = new FramedImageWithMenu(
                    mainFrame,
                    new ImageIcon(
                            ImageLoader
                                .getImage(ImageLoader.DEFAULT_USER_PHOTO)),
                    AVATAR_ICON_WIDTH,
                    AVATAR_ICON_HEIGHT);
        imageWithMenu.setPopupMenu(new SelectAvatarMenu(imageWithMenu));
        this.accountImageLabel = imageWithMenu;

        accountNameLabel.setFont(
            accountNameLabel.getFont().deriveFont(12f));
        accountNameLabel.setOpaque(false);

        statusComboBox = new GlobalStatusSelectorBox();
        // Align status combo box with account name field.
        statusComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        TransparentPanel statusToolsPanel
            = new TransparentPanel(new BorderLayout(0, 0));

        SIPCommMenuBar statusMenuBar = new SIPCommMenuBar();
        statusMenuBar.add(statusComboBox);
        statusToolsPanel.add(statusMenuBar, BorderLayout.WEST);

        toolbarPluginPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        mainToolbarPluginContainer = new PluginContainer(toolbarPluginPanel,
                            Container.CONTAINER_MAIN_TOOL_BAR);

        statusToolsPanel.add(toolbarPluginPanel, BorderLayout.EAST);

        TransparentPanel rightPanel = new TransparentPanel();
        rightPanel.setLayout(new BorderLayout(0, 0));
        rightPanel.add(accountNameLabel, BorderLayout.NORTH);
        rightPanel.add(statusToolsPanel, BorderLayout.SOUTH);

        this.add(accountImageLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

        southPluginPanel = new TransparentPanel(new BorderLayout());

        southPluginContainer = new PluginContainer(
            southPluginPanel,
            Container.CONTAINER_ACCOUNT_SOUTH);

        this.add(southPluginPanel, BorderLayout.SOUTH);

        loadSkin();

        GuiActivator.getUIService().addPluginComponentListener(this);
        GuiActivator.getGlobalDisplayDetailsService()
            .addGlobalDisplayDetailsListener(this);

        String globalDisplayName
            = GuiActivator.getGlobalDisplayDetailsService()
                .getGlobalDisplayName();

        if(!StringUtils.isNullOrEmpty(globalDisplayName))
            accountNameLabel.setText(globalDisplayName);
    }

    /**
     * Adds the account given by <tt>protocolProvider</tt> in the contained
     * status combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account to add
     */
    public void addAccount(final ProtocolProviderService protocolProvider)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addAccount(protocolProvider);
                }
            });
            return;
        }

        statusComboBox.addAccount(protocolProvider);

        protocolProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Removes the account given by <tt>protocolProvider</tt> from the contained
     * status combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account to remove
     */
    public void removeAccount(final ProtocolProviderService protocolProvider)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeAccount(protocolProvider);
                }
            });
            return;
        }

        if (containsAccount(protocolProvider))
        {
            statusComboBox.removeAccount(protocolProvider);
            protocolProvider.removeRegistrationStateChangeListener(this);
        }
    }

    /**
     * Checks if an account corresponding to the given <tt>protocolProvider</tt>
     * is contained in the contained status combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account to check for
     * @return <tt>true</tt> to indicate that an account corresponding to the
     * given <tt>protocolProvider</tt> is contained in the status box,
     * <tt>false</tt> - otherwise
     */
    public boolean containsAccount(ProtocolProviderService protocolProvider)
    {
        return statusComboBox.containsAccount(protocolProvider);
    }

    /**
     * Updates the current status of the <tt>protocolProvider</tt> with the
     * <tt>newStatus</tt>. If status is null uses the current status.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to update
     * @param newStatus the new status to set
     */
    public void updateStatus(final ProtocolProviderService protocolProvider,
                             final PresenceStatus newStatus)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateStatus(protocolProvider, newStatus);
                }
            });
            return;
        }

        if(newStatus != null)
            statusComboBox.updateStatus(protocolProvider, newStatus);
        else
            statusComboBox.updateStatus(protocolProvider);
    }

    /**
     * Updates the current status of the <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to update
     */
    public void updateStatus( ProtocolProviderService protocolProvider)
    {
        updateStatus(protocolProvider, null);
    }

    /**
     * Updates the image that is shown.
     * @param img the new image.
     */
    public void updateImage(ImageIcon img)
    {
        accountImageLabel.setImageIcon(img.getImage());
        accountImageLabel.setMaximumSize(
            new Dimension(AVATAR_ICON_WIDTH, AVATAR_ICON_HEIGHT));
        revalidate();
        repaint();
    }

    /**
     * Starts connecting user interface for the given <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to start
     * connecting for
     */
    public void startConnecting(final ProtocolProviderService protocolProvider)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    startConnecting(protocolProvider);
                }
            });
            return;
        }

        statusComboBox.startConnecting(protocolProvider);
    }

    /**
     * Stops connecting user interface for the given <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to stop
     * connecting for
     */
    public void stopConnecting(final ProtocolProviderService protocolProvider)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    stopConnecting(protocolProvider);
                }
            });
            return;
        }

        statusComboBox.stopConnecting(protocolProvider);
    }

    /**
     * Returns <tt>true</tt> if there are selected status selector boxes,
     * otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if there are selected status selector boxes,
     * otherwise returns <tt>false</tt>
     */
    public boolean hasSelectedMenus()
    {
        return statusComboBox.hasSelectedMenus();
    }

    /**
     * Paints this component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (logoBgImage != null)
        {
            Graphics2D g2 = (Graphics2D) g;

            g.setColor(bgColor);
            g2.setPaint(texture);
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());

            g.drawImage(
                logoBgImage,
                this.getWidth() - logoBgImage.getWidth(null),
                0,
                null);
        }
    }

    /**
     * Indicates that a plug-in component is registered to be added in a
     * container. If the plug-in component in the given event is registered for
     * this container then we add it.
     * @param event <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();
        Container containerID = pluginComponent.getContainer();
        /*
        // avoid early creating of components by calling getComponent
        Object component = pluginComponent.getComponent();

        if (!(component instanceof Component))
            return;
        */

        if (containerID.equals(Container.CONTAINER_MAIN_TOOL_BAR)
           || containerID.equals(Container.CONTAINER_ACCOUNT_SOUTH))
        {
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Indicates that a plug-in component is registered to be removed from a
     * container. If the plug-in component in the given event is registered for
     * this container then we remove it.
     * @param event <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent pluginComponent = event.getPluginComponent();
        Container pluginContainer = pluginComponent.getContainer();
        /*Object component = pluginComponent.getComponent();

        if (!(component instanceof Component))
            return;
        */

        if (pluginContainer.equals(Container.CONTAINER_MAIN_TOOL_BAR)
            || pluginContainer.equals(Container.CONTAINER_ACCOUNT_SOUTH))
        {
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     *
     * @param event the event containing the new image
     */
    public void globalDisplayNameChanged(
            final GlobalDisplayNameChangeEvent event)
    {
        if (!SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    globalDisplayNameChanged(event);
                    return;
                }
            });

        String displayName = event.getNewDisplayName();

        if(!StringUtils.isNullOrEmpty(displayName))
            accountNameLabel.setText(displayName);
    }

    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     *
     * @param event the event containing the new image
     */
    public void globalDisplayAvatarChanged(
            final GlobalAvatarChangeEvent event)
    {
        if (!SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    globalDisplayAvatarChanged(event);
                    return;
                }
            });

        byte[] avatarImage = event.getNewAvatar();

        // If there is no avatar image set, then displays the default one.
        if(avatarImage != null)
        {
            accountImageLabel.setImageIcon(avatarImage);
        }
    }

    /**
     * Updates account information when a protocol provider is registered.
     * @param evt the <tt>RegistrationStateChangeEvent</tt> that notified us
     * of the change
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        ProtocolProviderService protocolProvider = evt.getProvider();

        // There is nothing we can do when account is registering,
        // will set only connecting state later.
        // While dispatching the registering if the state of the provider
        // changes to registered we may end with client logged off
        // this may happen if registered is coming too quickly after registered
        // Dispatching registering is doing some swing stuff which
        // is scheduled in EDT and so can be executing when already registered
        if (evt.getNewState().equals(RegistrationState.REGISTERING))
        {
            startConnecting(protocolProvider);
        }
        else
            this.updateStatus(protocolProvider);
    }

    /**
     * Loads images for the account status panel.
     */
    public void loadSkin()
    {
        bgColor
            = new Color(GuiActivator.getResources()
                .getColor("service.gui.LOGO_BAR_BACKGROUND"));

        logoBgImage
            = ImageLoader.getImage(ImageLoader.WINDOW_TITLE_BAR);

        // texture
        BufferedImage bgImage
            = ImageLoader.getImage(ImageLoader.WINDOW_TITLE_BAR_BG);
        texture
            = new TexturePaint(
                    bgImage,
                    new Rectangle(
                            0,
                            0,
                            bgImage.getWidth(null),
                            bgImage.getHeight(null)));

        GuiActivator.getUIService().addPluginComponentListener(this);

        byte[] avatar = GuiActivator.getGlobalDisplayDetailsService()
            .getGlobalDisplayAvatar();
        if (avatar == null || avatar.length <= 0)
            accountImageLabel.setImageIcon(ImageLoader
                .getImage(ImageLoader.DEFAULT_USER_PHOTO));
        else
            accountImageLabel.setImageIcon(avatar);
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @Override
    public String getUIClassID()
    {
        if(ConfigurationUtils.isTransparentWindowEnabled())
            return uiClassID;
        else
            return super.getUIClassID();
    }
}
