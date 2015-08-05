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
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.text.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ContactSearchFieldUI</tt> class is used to draw the SearchField UI.
 * The class extends <tt>SearchFieldUI</tt> and adds custom behavior for the 
 * icons in the search field.
 * 
 * @author Marin Dzhigarov
 * @author Hristo Terezov
 */
public class ContactSearchFieldUI
    extends SearchFieldUI
    implements Skinnable
{
    /**
     * Class logger.
     */
    private final static Logger logger
        = Logger.getLogger(ContactSearchFieldUI.class);

    /**
     * Indicates whether the call button should be enabled or not.
     */
    private boolean isCallButtonEnabled = true;

    /**
     * Indicates whether the sms button should be enabled or not.
     */
    private boolean isSmsButtonEnabled = false;

    /**
     * Listener for registration state change for the protocol provider service.
     */
    private ProtocolProviderRegistrationStateListener 
        providerRegistrationStateListener 
            = new ProtocolProviderRegistrationStateListener(); 
    
    /**
     * List of protocol providers that added <tt>ContactSearchFieldUI</tt> as 
     * listener.
     */
    private List<ProtocolProviderService> providers 
        = new LinkedList<ProtocolProviderService>();
    
    /**
     * Listener for registration state change for the protocol provider service.
     */
    private ProtocolProviderRegListener 
        providerRegListener = null;
    
    /**
     * Creates a UI for a SearchFieldUI.
     * 
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new ContactSearchFieldUI();
    }

    public ContactSearchFieldUI()
    {
        // Indicates if the big call button outside the search is enabled.
        String callButtonEnabledString
            = UtilActivator.getResources().getSettingsString(
                    "impl.gui.CALL_BUTTON_ENABLED");

        if ((callButtonEnabledString != null)
                && (callButtonEnabledString.length() > 0))
        {
            // If the outside call button is enabled the call button in this
            // search field is disabled.
            setCallButtonEnabled(!Boolean.parseBoolean(callButtonEnabledString));
        }
        
        loadSkin();
    }
    
    /**
     * Setups the listeners used in <tt>ContactSearchFieldUI</tt>.
     */
    public void setupListeners()
    {
        providerRegListener = new ProtocolProviderRegListener();
        try
        {
            GuiActivator.bundleContext.addServiceListener(providerRegListener,
                "(objectclass="
                    + ProtocolProviderService.class.getName() + ")");
        }
        catch (InvalidSyntaxException e)
        {
            // this should really not happen
            logger.error(e);
            return;
        }

        for(ProtocolProviderFactory providerFactory
                : GuiActivator.getProtocolProviderFactories().values())
        {
            for(AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                ProtocolProviderService pps 
                    = GuiActivator.bundleContext.getService(
                            providerFactory.getProviderForAccount(accountID));
                providers.add(pps);
                pps.addRegistrationStateChangeListener(
                    providerRegistrationStateListener);
            }
        }
        setCallButtonEnabled(isCallButtonEnabled);
    }
    
    @Override
    public void setCallButtonEnabled(boolean isEnabled)
    {
        isCallButtonEnabled = isEnabled;
        super.setCallButtonEnabled(isEnabled
            && CallManager.getTelephonyProviders().size() > 0);
    }

    @Override
    public void setSMSButtonEnabled(boolean isEnabled)
    {
        isSmsButtonEnabled = isEnabled;
    }

    /**
     * Paints the background of the associated component.
     * 
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    protected void customPaintBackground(Graphics g)
    {
        if(isSmsButtonEnabled)
        {
            super.setSMSButtonEnabled(
                GuiActivator.getUIService().getMainFrame()
                    .hasOperationSet(OperationSetSmsMessaging.class));
        }

        super.customPaintBackground(g);
    }

    /**
     * Creates a call when the mouse is clicked on the call icon.
     * 
     * @param ev the mouse event that has prompted us to create the call.
     */
    @Override
    protected void updateIcon(MouseEvent ev)
    {
        super.updateIcon(ev);

        if ((ev.getID() == MouseEvent.MOUSE_CLICKED))
        {
            int x = ev.getX();
            int y = ev.getY();

            if (isCallIconVisible && getCallButtonRect().contains(x, y))
            {
                JTextComponent c = getComponent();
                String searchText = c.getText();

                if (searchText != null)
                    CallManager.createCall(searchText, c);
            }
            else if (isSMSIconVisible && getSMSButtonRect().contains(x, y))
            {
                JTextComponent c = getComponent();
                final String searchText = c.getText();

                if (searchText == null)
                    return;

                SMSManager.sendSMS(getComponent(), searchText);
            }
        }
    }
    
    /**
     * Listens for <tt>ProtocolProviderService</tt> registrations.
     */
    private class ProtocolProviderRegListener
        implements ServiceListener
    {
        @Override
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want to
            // know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            {
                return;
            }

            ProtocolProviderService pps = (ProtocolProviderService)
                GuiActivator.bundleContext.getService(serviceRef);
            switch (event.getType())
            {
            case ServiceEvent.REGISTERED:
                providers.add(pps);
                pps.addRegistrationStateChangeListener(
                        providerRegistrationStateListener);
                break;
            case ServiceEvent.UNREGISTERING:
                providers.remove(pps);
                pps.removeRegistrationStateChangeListener(
                        providerRegistrationStateListener);
                break;
            }
        }
    }
    
    /**
     * Listener for the provider registration state.
     */
    private class ProtocolProviderRegistrationStateListener 
        implements RegistrationStateChangeListener
    {

        @Override
        public void registrationStateChanged(
            final RegistrationStateChangeEvent evt)
        {
            if(!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        registrationStateChanged(evt);
                    }
                });
                return;
            }

            setCallButtonEnabled(isCallButtonEnabled);
        }
    }

    /**
     * Removes all listeners that were added earlier.
     */
    public void removeListeners()
    {
        if(providerRegListener != null )
        {
            GuiActivator.bundleContext.removeServiceListener(providerRegListener);
            providerRegListener = null;
        }
        
        if(providers.size() != 0 && providerRegistrationStateListener != null)
        {
            for(ProtocolProviderService pps : providers)
            {
                pps.removeRegistrationStateChangeListener(
                    providerRegistrationStateListener);
            }
            providerRegistrationStateListener = null;
        }
    }
}
