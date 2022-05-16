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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChooseCallAccountDialog</tt> is the dialog shown when calling a
 * contact in order to let the user choose the account he'd prefer to use in
 * order to call this contact.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChooseUIContactDetailPopupMenu
    extends SIPCommPopupMenu
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The invoker component.
     */
    private final JComponent invoker;

    /**
     * Creates this dialog by specifying a list of telephony contacts to choose
     * from.
     *
     * @param invoker the invoker of this pop up
     * @param telephonyObjects the list of telephony contacts to select through
     * @param opSetClass the operation class, which indicates what action would
     * be performed if an item is selected from the list
     */
    public ChooseUIContactDetailPopupMenu(JComponent invoker,
                                      List<UIContactDetail> contactDetails,
                                      UIContactDetailAction action)
    {
        this.invoker = invoker;
        this.init(GuiActivator.getResources()
                    .getI18NString("service.gui.CHOOSE_CONTACT"));

        for (UIContactDetail detail : contactDetails)
        {
            if (detail instanceof UIContactDetailImpl)
                this.addContactDetailItem((UIContactDetailImpl) detail, action);
        }
    }

    /**
     * Initializes and add some common components.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     */
    private void init(String infoString)
    {
        setInvoker(invoker);

        this.add(createInfoLabel(infoString));

        this.addSeparator();

        this.setFocusable(true);
    }

    /**
     * Adds the given <tt>telephonyContact</tt> to the list of available
     * telephony contact.
     *
     * @param telephonyContact the telephony contact to add
     * @param opSetClass the operation set class, that indicates the action that
     * would be performed when an item is selected
     */
    private void addContactDetailItem(
        final UIContactDetailImpl contactDetail,
        final UIContactDetailAction contactDetailAction)
    {
        final ContactMenuItem contactItem
            = new ContactMenuItem(contactDetail);

        contactItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                contactDetailAction.actionPerformed(contactDetail,
                                                    getX(), getY());

                ChooseUIContactDetailPopupMenu.this.setVisible(false);
            }
        });

        add(contactItem);
    }

    /**
     * Shows the dialog at the given location.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void showPopupMenu(int x, int y)
    {
        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Shows this popup menu regarding to its invoker location.
     */
    public void showPopupMenu()
    {
        Point location = new Point(invoker.getX(),
            invoker.getY() + invoker.getHeight());

        SwingUtilities
            .convertPointToScreen(location, invoker.getParent());
        setLocation(location);
        setVisible(true);
    }

    /**
     * Creates the info label.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     * @return the created info label
     */
    private Component createInfoLabel(String infoString)
    {
        JMenuItem infoLabel = new JMenuItem();

        infoLabel.setEnabled(false);
        infoLabel.setFocusable(false);

        infoLabel.setText("<html><b>" + infoString + "</b></html>");

        return infoLabel;
    }

    /**
     * A custom menu item corresponding to a specific protocol <tt>Contact</tt>.
     */
    private class ContactMenuItem
        extends JMenuItem
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final UIContactDetailImpl contact;

        public ContactMenuItem(UIContactDetailImpl contact)
        {
            this.contact = contact;

            String itemName = "<html>";
            Iterator<String> labels = contact.getLabels();

            if (labels != null && labels.hasNext())
                while (labels.hasNext())
                    itemName += "<b style=\"color: gray\">"
                                + labels.next().toLowerCase() + "</b> ";

            itemName += contact.getAddress() + "</html>";

            this.setText(itemName);
            loadSkin();
        }

        /**
         * Reloads contact icon.
         */
        public void loadSkin()
        {
            ImageIcon contactIcon = contact.getStatusIcon();

            if (contactIcon == null)
            {
                PresenceStatus status = contact.getPresenceStatus();

                BufferedImage statusIcon = null;
                if (status != null)
                    statusIcon = Constants.getStatusIcon(status);

                if (statusIcon != null)
                    contactIcon = ImageLoader.getIndexedProtocolIcon(
                        statusIcon,
                        contact.getPreferredProtocolProvider(null));
            }

            if (contactIcon != null)
                this.setIcon(ImageLoader.getIndexedProtocolIcon(
                    contactIcon.getImage(),
                    contact.getPreferredProtocolProvider(null)));
        }
    }

    /**
     * Reloads all menu items.
     */
    public void loadSkin()
    {
        Component[] components = getComponents();
        for(Component component : components)
        {
            if(component instanceof Skinnable)
            {
                Skinnable skinnableComponent = (Skinnable) component;
                skinnableComponent.loadSkin();
            }
        }
    }
}
