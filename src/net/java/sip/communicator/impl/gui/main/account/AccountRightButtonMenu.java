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
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * AccountRightButtonMenu is the menu that opens when user right clicks on any
 * of his accounts in the account list section.
 * 
 * @author Marin Dzhigarov
 */
public class AccountRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                PluginComponentListener,
                Skinnable
{
    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger of this class.
     */
    private final Logger logger
        = Logger.getLogger(AccountRightButtonMenu.class);

    /**
     * The Account that is clicked on.
     */
    private  Account account = null;

    /**
     * A list of all PluginComponents that are registered through the OSGi
     * bundle context.
     */
    private java.util.List<PluginComponent> pluginComponents
        = new ArrayList<PluginComponent>();

    /**
     * The edit item.
     */
    private final JMenuItem editItem = new JMenuItem(
        GuiActivator.getResources().getI18NString(
            "service.gui.EDIT"));

    /**
     * Creates an instance of AccountRightButtonMenu
     */
    public AccountRightButtonMenu()
    {
        super();

        this.setLocation(getLocation());

        this.init();

        loadSkin();
    }

    /**
     * Sets the current Account that is clicked on.
     * @param account the Account that is clicked on.
     */
    public void setAccount(Account account)
    {
        this.account = account;
        editItem.setEnabled(account != null && this.account.isEnabled());
        for (PluginComponent pluginComponent : pluginComponents)
            pluginComponent.setCurrentAccountID(account.getAccountID());
    }

    /**
     * Returns the Account that was last clicked on.
     * @return the Account that was last clicked on.
     */
    public Account getAccount()
    {
        return account;
    }

    /**
     * Initialized the menu by adding all containing menu items.
     */
    private void init()
    {
        initPluginComponents();
        add(editItem);
        editItem.addActionListener(this);
    }

    /**
     * Initializes plug-in components for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        Collection<ServiceReference<PluginComponentFactory>> serRefs;
        String osgiFilter
            = "(" + Container.CONTAINER_ID + "="
                + Container.CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU.getID() + ")";

        try
        {
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        PluginComponentFactory.class,
                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            serRefs = null;
            logger.error("Could not obtain plugin reference.", ex);
        }

        if ((serRefs != null) && !serRefs.isEmpty())
        {
            for (ServiceReference<PluginComponentFactory> serRef : serRefs)
            {
                PluginComponentFactory factory
                    = GuiActivator.bundleContext.getService(serRef);
                PluginComponent component
                    = factory.getPluginComponentInstance(this);

                if (component.getComponent() == null)
                    continue;

                pluginComponents.add(component);

                int positionIndex = factory.getPositionIndex();

                if (positionIndex != -1)
                    add((Component)component.getComponent(), positionIndex);
                else
                    add((Component)component.getComponent());
            }
        }
        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Reloads skin related information.
     */
    public void loadSkin()
    {
        editItem.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.ACCOUNT_EDIT_ICON)));
    }

    /**
     * Indicates that a plugin component has been added to this container.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    /**
     * Indicates that a new plugin component has been added. Adds it to this
     * container if it belongs to it.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if (!factory.getContainer().equals(
                Container.CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU))
            return;

        PluginComponent c = factory.getPluginComponentInstance(this);

        this.add((Component) c.getComponent());

        this.repaint();
    }

    /**
     * Removes the according plug-in component from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if(factory.getContainer()
                .equals(Container.CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU))
        {
            Component c =
                (Component)factory.getPluginComponentInstance(this)
                    .getComponent();
            this.remove(c);
            pluginComponents.remove(c);
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and performs the appropriate operations.
     * @param e the <tt>ActionEvent</tt>, which notified us of the action
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        if (menuItem.equals(editItem))
        {
            if (account == null)
                return;

            AccountRegWizardContainerImpl wizard =
                (AccountRegWizardContainerImpl) GuiActivator.getUIService()
                    .getAccountRegWizardContainer();

            AccountRegistrationWizard protocolWizard =
                        wizard.getProtocolWizard(account.getProtocolProvider());

            ResourceManagementService resources = GuiActivator.getResources();
            if (protocolWizard != null)
            {
                wizard.setTitle(resources.getI18NString(
                                    "service.gui.ACCOUNT_REGISTRATION_WIZARD"));

                wizard.modifyAccount(account.getProtocolProvider());
                wizard.showDialog(false);
            }
            else
            {
                // There is no wizard for this account - just show an error
                // dialog:
                String title = resources.getI18NString("service.gui.ERROR");
                String message =
                      resources.getI18NString("service.gui.EDIT_NOT_SUPPORTED");
                ErrorDialog dialog = new ErrorDialog(null, title, message);
                dialog.setVisible(true);
            }
        }
    }
}
