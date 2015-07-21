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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>CallHistoryButton</tt> is the button shown on the top of the contact
 * list.
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class CallHistoryButton
    extends SIPCommNotificationsButton
    implements  UINotificationListener,
                ActionListener,
                Skinnable
{
    /**
     * The tool tip shown by default over the history button.
     */
    private final static String callHistoryToolTip
        = GuiActivator.getResources().getI18NString(
            "service.gui.CALL_HISTORY_TOOL_TIP");

    /**
     * The tool tip shown when we're in history view.
     */
    private final static String showContactListToolTip
        = GuiActivator.getResources().getI18NString(
            "service.gui.SHOW_CONTACT_LIST_TOOL_TIP");

    /**
     * Creates a <tt>CallHistoryButton</tt>.
     */
    public CallHistoryButton()
    {
        super("");

        UINotificationManager.addNotificationListener(this);

        this.setToolTipText(callHistoryToolTip);

        this.addActionListener(this);
    }

    /**
     * Action performed on clicking the button.
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
        if(isToggleDisabled()
            && GuiActivator.getContactList().getCurrentFilter()
                    .equals(TreeContactList.historyFilter))
            return;

        if (!isToggleDisabled()
            && isHistoryVisible() && !hasNotifications())
        {
            GuiActivator.getContactList()
                .setDefaultFilter(TreeContactList.presenceFilter);
            GuiActivator.getContactList().applyDefaultFilter();

            setHistoryVisible(false);
        }
        else
        {
            GuiActivator.getContactList()
                .setDefaultFilter(TreeContactList.historyFilter);
            GuiActivator.getContactList().applyDefaultFilter();

            UINotificationManager.removeAllNotifications();

            setHistoryVisible(true);
        }
        setHistoryView();

        GuiActivator.getContactList().requestFocusInWindow();
        repaint();
    }

    /**
     * Indicates that a new notification is received.
     *
     * @param notification the notification that was received
     */
    public void notificationReceived(UINotification notification)
    {
        Collection<UINotificationGroup> notificationGroups
            = UINotificationManager.getNotificationGroups();

        if (!isHistoryVisible() && notificationGroups.size() > 0)
        {
            setNotificationView(notificationGroups);
        }
        else
        {
            setHistoryView();
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the history view.
     */
    private void setHistoryView()
    {
        clearNotifications();

        if (!isToggleDisabled() && isHistoryVisible())
        {
            setToolTipText(showContactListToolTip);
        }
        else
        {
            setToolTipText(callHistoryToolTip);
        }
    }

    /**
     * Sets the notifications view of this button.
     *
     * @param notificationGroups the list of unread notification groups
     */
    private void setNotificationView(
        final Collection<UINotificationGroup> notificationGroups)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setNotificationView(notificationGroups);
                }
            });
            return;
        }

        int notificationCount = 0;

        Iterator<UINotificationGroup> groupsIter
            = notificationGroups.iterator();

        String tooltipText = "<html>";

        while (groupsIter.hasNext())
        {
            UINotificationGroup group = groupsIter.next();

            tooltipText += "<b>" + group.getGroupDisplayName() + "</b><br/>";

            notificationCount += group.getUnreadNotificationsCount();

            int visibleNotifsPerGroup = 5;
            Iterator<UINotification> notifsIter = group.getUnreadNotifications();

            while (notifsIter.hasNext() && visibleNotifsPerGroup > 0)
            {
                UINotification missedCall = notifsIter.next();
                tooltipText += GuiUtils.formatTime(missedCall.getTime())
                    + "   " + missedCall.getDisplayName() + "<br/>";

                visibleNotifsPerGroup--;

                if (visibleNotifsPerGroup == 0 && notifsIter.hasNext())
                    tooltipText += GuiActivator.getResources()
                        .getI18NString("service.gui.MISSED_CALLS_MORE_TOOL_TIP",
                            new String[]{ new Integer(
                                notificationCount - 5).toString()});
            }
        }

        this.setToolTipText(tooltipText + "</html>");

        setNotifications(notificationCount);
    }

    private boolean isHistoryVisible()
    {
        return isDefaultViewVisible();
    }

    private void setHistoryVisible(boolean value)
    {
        setDefaultViewVisible(value);
    }

    /**
     * Loads images and sets history view.
     */
    @Override
    public void loadSkin()
    {
        defaultImage
            = ImageLoader.getImage(ImageLoader.CALL_HISTORY_BUTTON);

        if(!isToggleDisabled())
            pressedImage
                = ImageLoader.getImage(ImageLoader.CALL_HISTORY_BUTTON_PRESSED);

        setHistoryView();

        super.loadSkin();
    }
}
