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
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

public class AccountStatusPanel
    extends TransparentPanel
    implements  RegistrationStateChangeListener
{
    private static final int AVATAR_ICON_HEIGHT = 45;

    private static final int AVATAR_ICON_WIDTH = 45;

    private final FramedImage accountImageLabel;

    private final JLabel accountNameLabel
        = new JLabel(
                GuiActivator
                    .getResources().getI18NString("service.gui.ACCOUNT_ME"));

    private final Color bgColor
        = new Color(
                GuiActivator
                    .getResources()
                        .getColor("service.gui.LOGO_BAR_BACKGROUND"));

    private final Image logoBgImage
        = ImageLoader.getImage(ImageLoader.WINDOW_TITLE_BAR);

    private final GlobalStatusSelectorBox statusComboBox;

    private final TexturePaint texture;

    public AccountStatusPanel(MainFrame mainFrame)
    {
        super(new BorderLayout(10, 0));

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (ConfigurationManager.isTransparentWindowEnabled())
            this.setUI(new SIPCommOpaquePanelUI());

        accountImageLabel
            = new FramedImage(
                    new ImageIcon(
                            ImageLoader
                                .getImage(ImageLoader.DEFAULT_USER_PHOTO)),
                    AVATAR_ICON_WIDTH,
                    AVATAR_ICON_HEIGHT);

        accountNameLabel.setFont(
            accountNameLabel.getFont().deriveFont(Font.BOLD));
        accountNameLabel.setOpaque(false);

        statusComboBox = new GlobalStatusSelectorBox(mainFrame);
        // Align status combo box with account name field.
        statusComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        SIPCommMenuBar statusMenuBar = new SIPCommMenuBar();
        statusMenuBar.setLayout(new BorderLayout(0, 0));
        statusMenuBar.add(statusComboBox);

        Container rightPanel = new TransparentPanel(new GridLayout(0, 1, 0, 0));
        rightPanel.add(accountNameLabel);
        rightPanel.add(statusMenuBar);

        this.add(accountImageLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

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
    }

    public void addAccount(ProtocolProviderService protocolProvider)
    {
        statusComboBox.addAccount(protocolProvider);

        protocolProvider.addRegistrationStateChangeListener(this);
    }

    public void removeAccount(ProtocolProviderService protocolProvider)
    {
        statusComboBox.removeAccount(protocolProvider);

        protocolProvider.removeRegistrationStateChangeListener(this);
    }

    public boolean containsAccount(ProtocolProviderService protocolProvider)
    {
        return statusComboBox.containsAccount(protocolProvider);
    }

    public Object getLastPresenceStatus(
                                    ProtocolProviderService protocolProvider)
    {
        return statusComboBox.getLastPresenceStatus(protocolProvider);
    }

    public String getLastStatusString(ProtocolProviderService protocolProvider)
    {
        return statusComboBox.getLastStatusString(protocolProvider);
    }

    public void updateStatus(   ProtocolProviderService protocolProvider,
                                PresenceStatus newStatus)
    {
        statusComboBox.updateStatus(protocolProvider, newStatus);
    }

    public void updateStatus( ProtocolProviderService protocolProvider)
    {
        statusComboBox.updateStatus(protocolProvider);
    }

    /**
     * Returns TRUE if there are selected status selector boxes, otherwise
     * returns FALSE.
     */
    public boolean hasSelectedMenus()
    {
        return statusComboBox.hasSelectedMenus();
    }

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
                        byte[] accountImage
                            = AccountInfoUtils.getImage(accountInfoOpSet);

                        // do not set empty images
                        if ((accountImage != null) && (accountImage.length > 0))
                            accountImageLabel
                                .setImageIcon(new ImageIcon(accountImage));

                        String firstName
                            = AccountInfoUtils.getFirstName(accountInfoOpSet);
                        String lastName
                            = AccountInfoUtils.getLastName(accountInfoOpSet);
                        String accountName;

                        if (firstName != null)
                            if (lastName != null)
                                accountName = firstName + " " + lastName;
                            else
                                accountName = firstName;
                        else if (lastName != null)
                            accountName = lastName;
                        else
                            accountName = "";

                        if (accountName.length() == 0)
                        {
                            String displayName
                                = AccountInfoUtils
                                    .getDisplayName(accountInfoOpSet);

                            if (displayName != null)
                                accountName = displayName;
                        }

                        if (accountName.length() > 0)
                            accountNameLabel.setText(accountName);
                    }
                }.start();
        }
    }

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
}
