/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>StatusPanel</tt> is the place where the user can see and change its
 * status for all registered protocols.
 * 
 * @author Yana Stamcheva
 */
public class StatusPanel
    extends JMenuBar
    implements ComponentListener
{
    private Hashtable protocolStatusCombos = new Hashtable();

    private GlobalStatusSelectorBox globalStatusBox;

    private MainFrame mainFrame;
    
    private int hiddenProviders = 0;

    /**
     * Creates an instance of <tt>StatusPanel</tt>.
     * 
     * @param mainFrame The main application window.
     */
    public StatusPanel(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    }

    /**
     * Creates the selector box, containing all protocol statuses, adds it to
     * the StatusPanel and refreshes the panel.
     * 
     * @param protocolProvider The protocol provider.
     */
    public void addAccount(ProtocolProviderService protocolProvider)
    {
        StatusSelectorBox protocolStatusCombo;

        int providerIndex = this.mainFrame.getProviderIndex(protocolProvider);

        if (mainFrame.getProtocolPresenceOpSet(protocolProvider) != null)
        {
            protocolStatusCombo =
                new PresenceStatusSelectorBox(this.mainFrame, protocolProvider,
                    providerIndex);
        }
        else
        {
            protocolStatusCombo =
                new SimpleStatusSelectorBox(this.mainFrame, protocolProvider,
                    providerIndex);
        }

        protocolStatusCombo.addComponentListener(this);
        
        boolean isHidden = 
            protocolProvider.getAccountID().
                getAccountProperties().get("HIDDEN_PROTOCOL") != null;
        
        if(isHidden)
            hiddenProviders++;

        if (protocolStatusCombos.size() - hiddenProviders == 1 && !isHidden)
        {
            this.globalStatusBox = new GlobalStatusSelectorBox(mainFrame);

            Icon statusSeparatorIcon = new ImageIcon(
                ImageLoader.getImage(ImageLoader.STATUS_SEPARATOR_ICON));

            this.add(new JLabel(statusSeparatorIcon), FlowLayout.LEFT);
            this.add(globalStatusBox, FlowLayout.LEFT);
        }

        this.protocolStatusCombos.put(protocolProvider, protocolStatusCombo);

        if(!isHidden)
        {
            this.add(protocolStatusCombo);

            this.getParent().validate();
        }
            
    }

    /**
     * Removes the selector box, containing all protocol statuses, from the
     * StatusPanel and refreshes the panel.
     * 
     * @param pps The protocol provider to remove.
     */
    public void removeAccount(ProtocolProviderService pps)
    {
        StatusSelectorBox protocolStatusCombo =
            (StatusSelectorBox) this.protocolStatusCombos.get(pps);

        boolean isHidden = 
            pps.getAccountID().getAccountProperties().
                get("HIDDEN_PROTOCOL") != null;
        
        if(isHidden)
            hiddenProviders--;
        
        this.protocolStatusCombos.remove(pps);

        if (protocolStatusCombos.size() - hiddenProviders == 1 && 
            globalStatusBox != null)
        {
            this.remove(globalStatusBox);
        }

        if(protocolStatusCombo == null)
            return;
            
        this.remove(protocolStatusCombo);

        this.revalidate();
        this.repaint();
    }

    /**
     * Updates the account index for the given protocol provider.
     * 
     * @param protocolProvider the protocol provider for the account to update
     */
    public void updateAccountIndex(ProtocolProviderService protocolProvider)
    {
        StatusSelectorBox protocolStatusCombo =
            (StatusSelectorBox) this.protocolStatusCombos.get(protocolProvider);

        if(protocolStatusCombo == null)
            return;
        
        protocolStatusCombo.setAccountIndex(mainFrame
            .getProviderIndex(protocolProvider));

        this.revalidate();
        this.repaint();
    }

    /**
     * Shows the protocol animated icon, which indicates that it is in a
     * connecting state.
     * 
     * @param protocolProvider The protocol provider.
     */
    public void startConnecting(ProtocolProviderService protocolProvider)
    {
        StatusSelectorBox selectorBox =
            (StatusSelectorBox) protocolStatusCombos.get(protocolProvider);
     
        if(selectorBox == null)
            return;
        
        BufferedImage[] animatedImage =
            ImageLoader.getAnimatedImage(protocolProvider.getProtocolIcon()
                .getConnectingIcon());

        if (animatedImage != null && animatedImage.length > 0)
            selectorBox.startConnecting(animatedImage);
        else
            selectorBox.setSelectedIcon(new ImageIcon(protocolProvider
                .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16)));

        selectorBox.repaint();
    }

    /**
     * Returns the last contact status saved in the configuration.
     * 
     * @param protocolProvider the protocol provider to which the status
     *            corresponds
     * @return the last contact status saved in the configuration.
     */
    public String getLastStatusString(ProtocolProviderService protocolProvider)
    {
        ConfigurationService configService =
            GuiActivator.getConfigurationService();

        // find the last contact status saved in the configuration.
        String lastStatus = null;

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List accounts = configService.getPropertyNamesByPrefix(prefix, true);

        Iterator accountsIter = accounts.iterator();

        while (accountsIter.hasNext())
        {
            String accountRootPropName = (String) accountsIter.next();

            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID()
                .getAccountUniqueID()))
            {
                lastStatus =
                    configService.getString(accountRootPropName
                        + ".lastAccountStatus");

                if (lastStatus != null)
                    break;
            }
        }

        return lastStatus;
    }

    /**
     * Returns the last status that was stored in the configuration xml for the
     * given protocol provider.
     * 
     * @param protocolProvider the protocol provider
     * @return the last status that was stored in the configuration xml for the
     *         given protocol provider
     */
    public PresenceStatus getLastPresenceStatus(
        ProtocolProviderService protocolProvider)
    {
        String lastStatus = getLastStatusString(protocolProvider);

        OperationSetPresence presence =
            mainFrame.getProtocolPresenceOpSet(protocolProvider);

        if (presence == null)
            return null;

        Iterator i = presence.getSupportedStatusSet();

        if (lastStatus != null)
        {
            PresenceStatus status;
            while (i.hasNext())
            {
                status = (PresenceStatus) i.next();
                if (status.getStatusName().equals(lastStatus))
                {
                    return status;
                }
            }
        }
        return null;
    }

    /**
     * Updates the status for this protocol provider in the corresponding
     * status selector box.
     * 
     * @param protocolProvider the protocol provider, which presence status to
     * update.
     */
    public void updateStatus(ProtocolProviderService protocolProvider)
    {
        StatusSelectorBox selectorBox =
            (StatusSelectorBox) protocolStatusCombos.get(protocolProvider);

        if (selectorBox == null)
            return;

        if (selectorBox instanceof PresenceStatusSelectorBox)
        {
            PresenceStatusSelectorBox presenceSelectorBox =
                (PresenceStatusSelectorBox) selectorBox;

            if (!protocolProvider.isRegistered())
                presenceSelectorBox.updateStatus(presenceSelectorBox
                    .getOfflineStatus());
            else
            {
                if (presenceSelectorBox.getLastSelectedStatus() != null)
                {
                    presenceSelectorBox.updateStatus(presenceSelectorBox
                        .getLastSelectedStatus());
                }
                else
                {
                    PresenceStatus lastStatus =
                        getLastPresenceStatus(protocolProvider);

                    if (lastStatus == null)
                    {
                        presenceSelectorBox.updateStatus(presenceSelectorBox
                            .getOnlineStatus());
                    }
                    else
                    {
                        presenceSelectorBox.updateStatus(lastStatus);
                    }
                }
            }
        }
        else
        {
            ((SimpleStatusSelectorBox) selectorBox).updateStatus();
        }

        selectorBox.repaint();

        // Update the global status.
        if (globalStatusBox != null)
        {
            globalStatusBox.updateStatus();
        }
    }

    /**
     * Changes the current status of the given protocol provider, selected in
     * the selector box with the given status.
     * 
     * @param pps the protocol provider, which status should be updated.
     * @param status the new status to set
     */
    public void updateStatus(ProtocolProviderService pps, PresenceStatus status)
    {
        StatusSelectorBox selectorBox =
            (StatusSelectorBox) protocolStatusCombos.get(pps);

        if (selectorBox == null)
            return;

        if (selectorBox instanceof PresenceStatusSelectorBox)
        {
            PresenceStatusSelectorBox presenceSelectorBox =
                (PresenceStatusSelectorBox) selectorBox;

            presenceSelectorBox.updateStatus(status);
        }

        // Update the global status.
        if (globalStatusBox != null)
        {
            globalStatusBox.updateStatus();
        }
    }

    /**
     * Checks if the given protocol has already its <tt>StatusSelectorBox</tt>
     * in the <tt>StatusPanel</tt>.
     * 
     * @param pps The protocol provider to check.
     * @return True if the protocol has already its StatusSelectorBox in the
     *         StatusPanel, False otherwise.
     */
    public boolean containsAccount(ProtocolProviderService pps)
    {
        if (protocolStatusCombos.containsKey(pps))
            return true;
        else
            return false;
    }

    /**
     * Returns TRUE if there are selected status selector boxes, otherwise
     * returns FALSE.
     */
    public boolean hasSelectedMenus()
    {
        Enumeration statusCombos = protocolStatusCombos.elements();

        while (statusCombos.hasMoreElements())
        {
            StatusSelectorBox statusSelectorBox =
                (StatusSelectorBox) statusCombos.nextElement();

            if (statusSelectorBox.isSelected())
            {
                return true;
            }
        }
        return false;
    }

    public void componentHidden(ComponentEvent e)
    {
    }

    public void componentMoved(ComponentEvent e)
    {
        int compCount = this.getComponentCount();
        int buttonHeight = e.getComponent().getHeight();

        int biggestY = 0;
        for (int i = 0; i < compCount; i++)
        {
            Component c = this.getComponent(i);

            if (c instanceof StatusSelectorBox)
            {
                if (c.getY() > biggestY)
                    biggestY = c.getY();
            }
        }

        this.setPreferredSize(new Dimension(this.getWidth(), biggestY
            + buttonHeight));

        ((JPanel) this.getParent()).revalidate();
        ((JPanel) this.getParent()).repaint();
    }

    public void componentResized(ComponentEvent e)
    {
    }

    public void componentShown(ComponentEvent e)
    {
    }
}
