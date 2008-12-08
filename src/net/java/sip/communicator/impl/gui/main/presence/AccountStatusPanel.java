/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
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

    private JMenuBar statusMenuBar = new JMenuBar();

    private GlobalStatusSelectorBox statusComboBox;

    private TransparentPanel rightPanel
        = new TransparentPanel(new GridLayout(0, 1, 0, 0));

    private ImageIcon imageIcon
        = new ImageIcon(ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO));

    private FramedImage accountImageLabel
        = new FramedImage(  imageIcon,
                            AVATAR_ICON_WIDTH,
                            AVATAR_ICON_HEIGHT);

    private JLabel accountNameLabel
        = new JLabel(GuiActivator.getResources().getI18NString("accountMe"));

    public AccountStatusPanel(MainFrame mainFrame)
    {
        super(new BorderLayout(10, 0));

        statusComboBox = new GlobalStatusSelectorBox(mainFrame);

        if (ConfigurationManager.isTransparentWindowEnabled())
            this.setUI(new SIPCommOpaquePanelUI());

        this.accountNameLabel.setOpaque(false);
        this.statusMenuBar.setOpaque(false);

        // Align status combo box with account name field.
        statusMenuBar.setLayout(new BorderLayout(0, 0));
        statusMenuBar.setUI(new SIPCommMenuBarUI());
        statusComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        accountNameLabel.setFont(
            accountNameLabel.getFont().deriveFont(Font.BOLD));

        statusMenuBar.add(statusComboBox);

        this.add(accountImageLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

        this.rightPanel.add(accountNameLabel);
        this.rightPanel.add(statusMenuBar);
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
        final ProtocolProviderService protocolProvider = evt.getProvider();

        this.updateStatus(protocolProvider);

        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            new Thread(new Runnable() {

                public void run()
                {
                    OperationSetServerStoredAccountInfo accountInfoOpSet
                        = (OperationSetServerStoredAccountInfo) protocolProvider
                            .getOperationSet(OperationSetServerStoredAccountInfo.class);

                    if (accountInfoOpSet != null)
                    {
                        byte[] accountImage
                            = AccountInfoUtils.getImage(accountInfoOpSet);

                        if (accountImage != null)
                        {
                            accountImageLabel
                                .setImageIcon(new ImageIcon(accountImage));
                        }

                        String firstName
                            = AccountInfoUtils.getFirstName(accountInfoOpSet);

                        String lastName
                            = AccountInfoUtils.getLastName(accountInfoOpSet);

                        String accountName = "";
                        if (firstName != null)
                        {
                            accountName += firstName;
                        }

                        if (lastName != null)
                        {
                            accountName += " " + lastName;
                        }

                        if(accountName.length() == 0)
                        {
                            String displayName =
                                AccountInfoUtils.getDisplayName(accountInfoOpSet);

                            if(displayName != null)
                                accountName = displayName;
                        }

                        if (accountName.length() > 0)
                        {
                            accountNameLabel.setText(accountName);
                        }

                        revalidate();
                        repaint();
                    }
                }
            }).start();
        }
    }
}
