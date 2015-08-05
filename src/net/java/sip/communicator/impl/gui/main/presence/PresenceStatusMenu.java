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
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.presence.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.util.*;

/**
 * The <tt>StatusSelectorBox</tt> is a <tt>SIPCommMenu</tt> that contains
 * the list of statuses for a protocol provider. This is where the user could
 * select its status.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class PresenceStatusMenu
    extends StatusSelectorMenu
    implements ActionListener,
               PropertyChangeListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final Logger logger = Logger.getLogger(PresenceStatusMenu.class);

    /**
     * The area will display the account display name.
     */
    private JLabel titleArea;

    /**
     * The area will display the account status message that is active, if any.
     */
    private JLabel messageArea;

    /**
     * The status message menu.
     */
    private StatusMessageMenu statusMessageMenu;

    /**
     * Take care for global status items, that only one is selected.
     */
    private ButtonGroup group = new ButtonGroup();

    /**
     * Initializes a new <tt>PresenceStatusMenu</tt> instance which is to
     * depict and change the presence status of a specific
     * <tt>ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> which is to
     * have its presence status depicted and changed by the new instance
     */
    public PresenceStatusMenu(ProtocolProviderService protocolProvider)
    {
        super(protocolProvider.getAccountID().getDisplayName(),
            ImageLoader.getAccountStatusImage(protocolProvider),
            protocolProvider);

        this.presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

        Iterator<PresenceStatus> statusIterator
            = this.presence.getSupportedStatusSet();

        String tooltip =
            "<html><b>" + protocolProvider.getAccountID().getDisplayName()
                + "</b><br>Connecting</html>";

        this.setToolTipText(tooltip);

        titleArea = new JLabel();
        titleArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleArea.setOpaque(false);
        titleArea.setFont(titleArea.getFont().deriveFont(Font.BOLD));

        messageArea = new JLabel();
        messageArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        messageArea.setOpaque(false);
        messageArea.setVisible(false);
        messageArea.setFont(messageArea.getFont().deriveFont(Font.PLAIN));

        statusMessageMenu = new StatusMessageMenu(protocolProvider, true);
        statusMessageMenu.addPropertyChangeListener(this);
        updateTitleArea();

        this.add(titleArea);
        this.add(messageArea);
        this.addSeparator();

        while (statusIterator.hasNext())
        {
            PresenceStatus status = statusIterator.next();
            byte[] statusIcon = status.getStatusIcon();

            addItem(
                    status.getStatusName(),
                    (statusIcon == null) ? null : new ImageIcon(statusIcon),
                    this);
        }

        this.addSeparator();

        this.add((JMenu)statusMessageMenu.getMenu());

        this.setSelectedStatus(getOfflineStatus());
        updateStatus(getOfflineStatus());
    }

    /**
     * Adds an item to the "choice list" of this selector box.
     *
     * @param text The text of the item.
     * @param icon The icon of the item.
     * @param actionListener The <tt>ActionListener</tt>, which handles the
     * case, when the item is selected.
     */
    @Override
    public void addItem(String text, Icon icon, ActionListener actionListener)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(text, icon);

        item.setName(text);
        group.add(item);

        item.addActionListener(actionListener);

        add(item);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items in the
     * list is selected.
     *
     * @param e an <tt>ActionEvent</tt> which carries the data associated with
     * the performed action
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() instanceof JMenuItem)
        {
            String menuItemText = ((JMenuItem) e.getSource()).getText();

            Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

            while (statusSet.hasNext())
            {
                PresenceStatus status = statusSet.next();

                if (status.getStatusName().equals(menuItemText))
                {
                    if(GuiActivator.getGlobalStatusService() != null)
                    {
                        GuiActivator.getGlobalStatusService()
                            .publishStatus(protocolProvider, status);
                    }

                    setSelectedStatus(status);

                    break;
                }
            }
        }
    }

    /**
     * Selects a specific <tt>PresenceStatus</tt> in this instance and the
     * <tt>ProtocolProviderService</tt> it depicts.
     *
     * @param presenceStatus the <tt>PresenceStatus</tt> to be selected in this
     * instance and the <tt>ProtocolProviderService</tt> it depicts
     */
    public void updateStatus(PresenceStatus presenceStatus)
    {
        if (logger.isTraceEnabled())
            logger.trace("Update status for provider: "
            + protocolProvider.getAccountID().getAccountAddress()
            + ". The new status will be: " + presenceStatus.getStatusName());

        this.setSelectedStatus(presenceStatus);

        for(int i =0; i < getItemCount(); i++)
        {
            JMenuItem item = getItem(i);

            if(item instanceof JCheckBoxMenuItem)
            {
                if(item.getName().equals(presenceStatus.getStatusName()))
                {
                    item.setSelected(true);
                    //item.setText("<html><b>" + item.getName() + "</b></html>");
                }
                else
                {
                    item.setText(item.getName());
                }
            }
        }
    }

    /**
     * Updates the current title area with the account display name
     * and its status.
     */
    private void updateTitleArea()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    updateTitleArea();
                }
            });

            return;
        }

        titleArea.setText(protocolProvider.getAccountID().getDisplayName());
        final String statusMessage = statusMessageMenu.getCurrentMessage();
        if (StringUtils.isNullOrEmpty(statusMessage))
        {
            this.messageArea.setText("");
            this.messageArea.setVisible(false);
        }
        else
        {
            this.messageArea.setText(statusMessage);
            this.messageArea.setVisible(true);
        }
    }

    /**
     * Selects the given status in the status menu.
     *
     * @param status the status to select
     */
    public void setSelectedStatus(PresenceStatus status)
    {
        Icon statusImage = ImageLoader.getAccountStatusImage(protocolProvider);

        SelectedObject selectedObject
            = new SelectedObject(statusImage,
                                status.getStatusName());

        this.setSelected(selectedObject);

        String tooltip = this.getToolTipText();

        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));

        this.setToolTipText(tooltip.concat("<br>" + status.getStatusName()));
    }

    /**
     * Loads resources for this component.
     */
    @Override
    public void loadSkin()
    {
        super.loadSkin();

        this.setIcon(ImageLoader.getAccountStatusImage(protocolProvider));
    }

    /**
     * Listens for change in the status message coming from StatusMessageMenu.
     * @param evt the event.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(evt.getPropertyName()
            .equals(StatusMessageMenu.STATUS_MESSAGE_UPDATED_PROP))
        {
            updateTitleArea();
        }
    }

    /**
     * Clears resources.
     */
    public void dispose()
    {
        super.dispose();

        presence = null;
        titleArea = null;

        if(statusMessageMenu != null)
        {
            statusMessageMenu.removePropertyChangeListener(this);
            statusMessageMenu.dispose();
        }
        statusMessageMenu = null;
    }
}
