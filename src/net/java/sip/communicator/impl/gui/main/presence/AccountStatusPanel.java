/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

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
                AvatarListener,
                Skinnable
{
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

    private static byte[] currentImage;

    private String currentFirstName;

    private String currentLastName;

    /**
     * Creates an instance of <tt>AccountStatusPanel</tt> by specifying the
     * main window, where this panel is added.
     * @param mainFrame the main window, where this panel is added
     */
    public AccountStatusPanel(MainFrame mainFrame)
    {
        super(new BorderLayout(10, 0));

        if (ConfigurationManager.isTransparentWindowEnabled())
            this.setUI(new SIPCommOpaquePanelUI());

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

        statusComboBox = new GlobalStatusSelectorBox(mainFrame);
        // Align status combo box with account name field.
        statusComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        TransparentPanel statusToolsPanel
            = new TransparentPanel(new BorderLayout(0, 0));

        SIPCommMenuBar statusMenuBar = new SIPCommMenuBar();
        statusMenuBar.add(statusComboBox);
        statusToolsPanel.add(statusMenuBar, BorderLayout.WEST);

        toolbarPluginPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        new PluginContainer(toolbarPluginPanel,
                            Container.CONTAINER_MAIN_TOOL_BAR);

        statusToolsPanel.add(toolbarPluginPanel, BorderLayout.EAST);

        TransparentPanel rightPanel = new TransparentPanel();
        rightPanel.setLayout(new BorderLayout(0, 0));
        rightPanel.add(accountNameLabel, BorderLayout.NORTH);
        rightPanel.add(statusToolsPanel, BorderLayout.SOUTH);

        this.add(accountImageLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

        southPluginPanel = new TransparentPanel(new BorderLayout());

        new PluginContainer(
            southPluginPanel,
            Container.CONTAINER_ACCOUNT_SOUTH);

        this.add(southPluginPanel, BorderLayout.SOUTH);

        loadSkin();

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Adds the account given by <tt>protocolProvider</tt> in the contained
     * status combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account to add
     */
    public void addAccount(ProtocolProviderService protocolProvider)
    {
        statusComboBox.addAccount(protocolProvider);

        protocolProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Removes the account given by <tt>protocolProvider</tt> from the contained
     * status combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account to remove
     */
    public void removeAccount(ProtocolProviderService protocolProvider)
    {
        statusComboBox.removeAccount(protocolProvider);

        protocolProvider.removeRegistrationStateChangeListener(this);
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
     * Returns the last used presence status for the given
     * <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account we're looking for
     * @return the last used presence status for the given
     * <tt>protocolProvider</tt>
     */
    public Object getLastPresenceStatus(
                                    ProtocolProviderService protocolProvider)
    {
        return statusComboBox.getLastPresenceStatus(protocolProvider);
    }

    /**
     * Returns the last used status for the given <tt>protocolProvider</tt>
     * as a String.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to the account we're looking for
     * @return a String representation of the last used status for the given
     * <tt>protocolProvider</tt>
     */
    public String getLastStatusString(ProtocolProviderService protocolProvider)
    {
        return statusComboBox.getLastStatusString(protocolProvider);
    }

    /**
     * Updates the current status of the <tt>protocolProvider</tt> with the
     * <tt>newStatus</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to update
     * @param newStatus the new status to set
     */
    public void updateStatus(   ProtocolProviderService protocolProvider,
                                PresenceStatus newStatus)
    {
        statusComboBox.updateStatus(protocolProvider, newStatus);
    }

    /**
     * Updates the current status of the <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to update
     */
    public void updateStatus( ProtocolProviderService protocolProvider)
    {
        statusComboBox.updateStatus(protocolProvider);
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
    public void startConnecting(ProtocolProviderService protocolProvider)
    {
        statusComboBox.startConnecting(protocolProvider);
    }

    /**
     * Stops connecting user interface for the given <tt>protocolProvider</tt>.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to stop
     * connecting for
     */
    public void stopConnecting(ProtocolProviderService protocolProvider)
    {
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
     * Updates account information when a protocol provider is registered.
     * @param evt the <tt>RegistrationStateChangeEvent</tt> that notified us
     * of the change
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        ProtocolProviderService protocolProvider = evt.getProvider();

        this.updateStatus(protocolProvider);

        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            /*
             * Check the support for OperationSetServerStoredAccountInfo prior
             * to starting the Thread because only a couple of the protocols
             * currently support it and thus starting a Thread that is not going
             * to do anything useful can be prevented.
             */
            final OperationSetServerStoredAccountInfo accountInfoOpSet
                = protocolProvider.getOperationSet(
                        OperationSetServerStoredAccountInfo.class);

            if (accountInfoOpSet != null)
                /*
                 * FIXME Starting a separate Thread for each
                 * ProtocolProviderService is uncontrollable because the
                 * application is multi-protocol and having multiple accounts is
                 * expected so one is likely to end up with a multitude of
                 * Threads. Besides, it not very clear when retrieving the first
                 * and last name is to stop so one ProtocolProviderService being
                 * able to supply both the first and the last name may be
                 * overwritten by a ProtocolProviderService which is able to
                 * provide just one of them.
                 */
                new Thread()
                {
                    public void run()
                    {
                        if (currentImage == null)
                        {
                            byte[] accountImage
                                = AccountInfoUtils.getImage(accountInfoOpSet);

                            // do not set empty images
                            if ((accountImage != null)
                                    && (accountImage.length > 0))
                            {
                                currentImage = accountImage;
                                accountImageLabel.setImageIcon(currentImage);
                            }
                        }

                        String accountName = null;
                        if (currentFirstName == null)
                        {
                            String firstName = AccountInfoUtils
                                .getFirstName(accountInfoOpSet);

                            if (firstName != null && firstName.length() > 0)
                            {
                                currentFirstName = firstName;
                                accountName = currentFirstName;
                            }
                        }

                        if (currentLastName == null)
                        {
                            String lastName = AccountInfoUtils
                                .getLastName(accountInfoOpSet);

                            if (lastName != null && lastName.length() > 0)
                            {
                                currentLastName = lastName;
                                /*
                                 * If accountName is null, don't use += because
                                 * it will make the accountName start with the
                                 * string "null".
                                 */
                                if ((accountName == null)
                                        || (accountName.length() == 0))
                                    accountName = currentLastName;
                                else
                                    accountName += " " + currentLastName;
                            }
                        }

                        if (currentFirstName == null && currentLastName == null)
                        {
                            String displayName = AccountInfoUtils
                                .getDisplayName(accountInfoOpSet);

                            if (displayName != null)
                                accountName = displayName;
                        }

                        if (accountName != null && accountName.length() > 0)
                            accountNameLabel.setText(accountName);
                    }
                }.start();

            OperationSetAvatar avatarOpSet
                = protocolProvider.getOperationSet(OperationSetAvatar.class);
            if (avatarOpSet != null)
                avatarOpSet.addAvatarListener(this);
        }
        else if (evt.getNewState().equals(RegistrationState.UNREGISTERING)
                || evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
        {
            OperationSetAvatar avatarOpSet
                = protocolProvider.getOperationSet(OperationSetAvatar.class);
            if (avatarOpSet != null)
                avatarOpSet.removeAvatarListener(this);
        }
    }

    /**
     * Paints this component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
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
        String containerID = pluginComponent.getContainer().getID();
        Object component = pluginComponent.getComponent();

        if (!(component instanceof Component))
            return;

        if (containerID.equals(Container.CONTAINER_MAIN_TOOL_BAR))
            toolbarPluginPanel.add((Component) component);
        else if (containerID.equals(Container.CONTAINER_ACCOUNT_SOUTH))
            southPluginPanel.add((Component) component);

        this.revalidate();
        this.repaint();
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
        Object component = pluginComponent.getComponent();

        if (!(component instanceof Component))
            return;

        if (pluginContainer.equals(Container.CONTAINER_MAIN_TOOL_BAR))
            toolbarPluginPanel.remove((Component) component);
        else if (pluginContainer.equals(Container.CONTAINER_ACCOUNT_SOUTH))
            southPluginPanel.remove((Component) component);

        this.revalidate();
        this.repaint();
    }

    /**
     * Returns the global account image currently shown on the top of the
     * application window.
     * @return the global account image
     */
    public static byte[] getGlobalAccountImage()
    {
        return currentImage;
    }

    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     *
     * @param event the event containing the new image
     */
    public void avatarChanged(AvatarEvent event)
    {
        currentImage = event.getNewAvatar();
        accountImageLabel.setImageIcon(currentImage);
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

        if(currentImage == null)
            accountImageLabel.setImageIcon(ImageLoader
                .getImage(ImageLoader.DEFAULT_USER_PHOTO));
    }
}