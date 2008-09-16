package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

public class AccountStatusPanel
    extends JPanel
    implements RegistrationStateChangeListener
{
    private JMenuBar statusMenuBar = new JMenuBar();

    private GlobalStatusSelectorBox statusComboBox;

    private JPanel rightPanel = new JPanel(new GridLayout(0, 1, 0, 0));

    private ImageIcon imageIcon = ImageUtils.scaleIconWithinBounds(
        new ImageIcon(ImageLoader
                .getImage(ImageLoader.DEFAULT_USER_PHOTO)),
        40, 45);

    private JLabel accountImageLabel = new JLabel(imageIcon);

    private JLabel accountNameLabel
        = new JLabel(GuiActivator.getResources().getI18NString("accountMe"));

    private MainFrame mainFrame;

    public AccountStatusPanel(MainFrame mainFrame)
    {
        super(new BorderLayout(10, 0));

        if (ConfigurationManager.isTransparentWindowEnabled())
            this.setUI(new SIPCommOpaquePanelUI());

        this.mainFrame = mainFrame;

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.setOpaque(false);
        this.accountNameLabel.setOpaque(false);
        this.rightPanel.setOpaque(false);

        statusComboBox = new GlobalStatusSelectorBox(mainFrame);

        // Align status combo box with account name field.
        statusMenuBar.setLayout(new BorderLayout(0, 0));
        statusComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        accountNameLabel.setFont(
            accountNameLabel.getFont().deriveFont(Font.BOLD));
        statusMenuBar.add(statusComboBox);

        this.add(accountImageLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

        this.rightPanel.add(accountNameLabel);
        this.rightPanel.add(statusMenuBar);

        this.initAccountImageLabel();

        this.initAccountNameLabel();
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

    private void initAccountImageLabel()
    {
        
    }

    private void initAccountNameLabel()
    {

    }

    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        final ProtocolProviderService protocolProvider = evt.getProvider();

        this.updateStatus(protocolProvider);

        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            SwingUtilities.invokeLater(new Runnable() {

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
                            accountImageLabel.setIcon(new ImageIcon(accountImage));
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
                            accountName +=
                                AccountInfoUtils.getDisplayName(accountInfoOpSet);
                        }

                        if (accountName.length() > 0)
                        {
                            accountNameLabel.setText(accountName);
                        }

                        revalidate();
                        repaint();
                    }
                }
            });
        }
    }

    protected void paintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        super.paintComponent(g);

        Color bgColor = new Color(
            GuiActivator.getResources()
                .getColor("accountRegistrationBackground"));

        g.setColor(bgColor);

        g.fillRoundRect(5, 5, this.getWidth() - 10, this.getHeight() - 10, 8, 8);
    }
}