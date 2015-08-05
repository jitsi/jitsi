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
package net.java.sip.communicator.impl.globaldisplaydetails;

import java.util.*;

import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.globaldisplaydetails.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.util.*;

/**
 * The <tt>GlobalDisplayNameImpl</tt> offers generic access to a global
 * display name for the local user.
 * <p>
 *
 * @author Yana Stamcheva
 * @author Hristo Terezov
 */
public class GlobalDisplayDetailsImpl
    implements  GlobalDisplayDetailsService,
                RegistrationStateChangeListener,
                ServerStoredDetailsChangeListener,
                AvatarListener
{
    /**
     * Property to disable auto answer menu.
     */
    private static final String GLOBAL_DISPLAY_NAME_PROP =
        "net.java.sip.communicator.impl.gui.main.presence.GLOBAL_DISPLAY_NAME";

    /**
     * The display details listeners list.
     */
    private List<GlobalDisplayDetailsListener> displayDetailsListeners
        = new ArrayList<GlobalDisplayDetailsListener>();

    /**
     * The current first name.
     */
    private String currentFirstName;

    /**
     * The current last name.
     */
    private String currentLastName;

    /**
     * The current display name.
     */
    private String currentDisplayName;

    /**
     * The provisioned display name.
     */
    private String provisionedDisplayName;

    /**
     * The global avatar.
     */
    private static byte[] globalAvatar;

    /**
     * The global display name.
     */
    private String globalDisplayName;

    /**
     * Creates an instance of <tt>GlobalDisplayDetailsImpl</tt>.
     */
    public GlobalDisplayDetailsImpl()
    {
        provisionedDisplayName
            = GlobalDisplayDetailsActivator.getConfigurationService()
                .getString(GLOBAL_DISPLAY_NAME_PROP, null);

        Iterator<ProtocolProviderService> providersIter
            = AccountUtils.getRegisteredProviders().iterator();

        while (providersIter.hasNext())
            providersIter.next().addRegistrationStateChangeListener(this);
    }

    /**
     * Returns default display name for the given provider or the global display
     * name.
     * @param pps the given protocol provider service
     * @return default display name.
     */
    public String getDisplayName(ProtocolProviderService pps)
    {
        final OperationSetServerStoredAccountInfo accountInfoOpSet
            = pps.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);

        String displayName = "";
        if (accountInfoOpSet != null)
        {
            displayName = AccountInfoUtils.getDisplayName(accountInfoOpSet);
        }
        if(displayName == null || displayName.length() == 0)
        {
            displayName = getGlobalDisplayName();
            if(displayName == null || displayName.length() == 0)
            {
                displayName = pps.getAccountID().getUserID();
                if(displayName != null)
                {
                    int atIndex = displayName.lastIndexOf("@");
                    if (atIndex > 0)
                        displayName = displayName.substring(0, atIndex);
                }
            }
        }

        return displayName;
    }

    /**
     * Returns the global display name to be used to identify the local user.
     *
     * @return a string representing the global local user display name
     */
    public String getGlobalDisplayName()
    {
        if (!StringUtils.isNullOrEmpty(provisionedDisplayName))
            return provisionedDisplayName;

        return globalDisplayName;
    }

    /**
     * Sets the global local user display name.
     *
     * @param displayName the string representing the display name to set as
     * a global display name
     */
    public void setGlobalDisplayName(String displayName)
    {
        globalDisplayName = displayName;
    }

    /**
     * Returns the global avatar for the local user.
     *
     * @return a byte array containing the global avatar for the local user
     */
    public byte[] getGlobalDisplayAvatar()
    {
        return globalAvatar;
    }

    /**
     * Sets the global display avatar for the local user.
     *
     * @param avatar the byte array representing the avatar to set
     */
    public void setGlobalDisplayAvatar(byte[] avatar)
    {
        globalAvatar = avatar;
    }

    /**
     * Adds the given <tt>GlobalDisplayDetailsListener</tt> to listen for change
     * events concerning the global display details.
     *
     * @param l the <tt>GlobalDisplayDetailsListener</tt> to add
     */
    public void addGlobalDisplayDetailsListener(GlobalDisplayDetailsListener l)
    {
        synchronized (displayDetailsListeners)
        {
            if (!displayDetailsListeners.contains(l))
                displayDetailsListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>GlobalDisplayDetailsListener</tt> listening for
     * change events concerning the global display details.
     *
     * @param l the <tt>GlobalDisplayDetailsListener</tt> to remove
     */
    public void removeGlobalDisplayDetailsListener(
        GlobalDisplayDetailsListener l)
    {
        synchronized (displayDetailsListeners)
        {
            if (displayDetailsListeners.contains(l))
                displayDetailsListeners.remove(l);
        }
    }

    /**
     * Updates account information when a protocol provider is registered.
     * @param evt the <tt>RegistrationStateChangeEvent</tt> that notified us
     * of the change
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        ProtocolProviderService protocolProvider = evt.getProvider();

        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            /*
             * Check the support for OperationSetServerStoredAccountInfo prior
             * to starting the Thread because only a couple of the protocols
             * currently support it and thus starting a Thread that is not going
             * to do anything useful can be prevented.
             */
            OperationSetServerStoredAccountInfo accountInfoOpSet
                = protocolProvider.getOperationSet(
                        OperationSetServerStoredAccountInfo.class);

            if (accountInfoOpSet != null)
            {
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
                new UpdateAccountInfo(protocolProvider, accountInfoOpSet, false)
                    .start();
            }

            OperationSetAvatar avatarOpSet
                = protocolProvider.getOperationSet(OperationSetAvatar.class);
            if (avatarOpSet != null)
                avatarOpSet.addAvatarListener(this);

            OperationSetServerStoredAccountInfo serverStoredAccountInfo
                = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);
            if (serverStoredAccountInfo != null)
                serverStoredAccountInfo.addServerStoredDetailsChangeListener(
                        this);
        }
        else if (evt.getNewState().equals(RegistrationState.UNREGISTERING)
                || evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
        {
            OperationSetAvatar avatarOpSet
                = protocolProvider.getOperationSet(OperationSetAvatar.class);
            if (avatarOpSet != null)
                avatarOpSet.removeAvatarListener(this);

            OperationSetServerStoredAccountInfo serverStoredAccountInfo
                = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);
            if (serverStoredAccountInfo != null)
                serverStoredAccountInfo.removeServerStoredDetailsChangeListener(
                        this);
        }
    }

    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     *
     * @param event the event containing the new image
     */
    public void avatarChanged(AvatarEvent event)
    {
        globalAvatar = event.getNewAvatar();
        // If there is no avatar image set, then displays the default one.
        if(globalAvatar == null)
        {
            globalAvatar = GlobalDisplayDetailsActivator.getResources()
                .getImageInBytes("service.gui.DEFAULT_USER_PHOTO");
        }

        AvatarCacheUtils.cacheAvatar(
            event.getSourceProvider(), globalAvatar);

        fireGlobalAvatarEvent(globalAvatar);
    }

    /**
     * Registers a ServerStoredDetailsChangeListener with the operation sets
     * of the providers, if a provider change its name we use it in the UI.
     *
     * @param evt the <tt>ServerStoredDetailsChangeEvent</tt>
     * the event for name change.
     */
    public void serverStoredDetailsChanged(ServerStoredDetailsChangeEvent evt)
    {
        if(!StringUtils.isNullOrEmpty(provisionedDisplayName))
            return;

        if(evt.getNewValue() instanceof
                ServerStoredDetails.DisplayNameDetail
            && (evt.getEventID() == ServerStoredDetailsChangeEvent.DETAIL_ADDED
                || evt.getEventID()
                    == ServerStoredDetailsChangeEvent.DETAIL_REPLACED))
        {
            ProtocolProviderService protocolProvider = evt.getProvider();
            OperationSetServerStoredAccountInfo accountInfoOpSet
                = protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class);

            new UpdateAccountInfo(  evt.getProvider(),
                                    accountInfoOpSet,
                                    true).start();
        }
    }

    /**
     * Queries the operations sets to obtain names and display info.
     * Queries are done in separate thread.
     */
    private class UpdateAccountInfo
        extends Thread
    {
        /**
         * The protocol provider.
         */
        private ProtocolProviderService protocolProvider;

        /**
         * The account info operation set to query.
         */
        private OperationSetServerStoredAccountInfo accountInfoOpSet;

        /**
         * Indicates if the display name and avatar should be updated from this
         * provider even if they already have values.
         */
        private boolean isUpdate;

        /**
         * Constructs with provider and opset to use.
         * @param protocolProvider the provider.
         * @param accountInfoOpSet the opset.
         * @param isUpdate indicates if the display name and avatar should be
         * updated from this provider even if they already have values.
         */
        UpdateAccountInfo(
            ProtocolProviderService protocolProvider,
            OperationSetServerStoredAccountInfo accountInfoOpSet,
            boolean isUpdate)
        {
            this.protocolProvider = protocolProvider;
            this.accountInfoOpSet = accountInfoOpSet;
        }

        @Override
        public void run()
        {
            if (globalAvatar == null)
            {
                globalAvatar
                    = AvatarCacheUtils
                        .getCachedAvatar(protocolProvider);

                if (globalAvatar == null)
                {
                    byte[] accountImage
                        = AccountInfoUtils
                            .getImage(accountInfoOpSet);

                    // do not set empty images
                    if ((accountImage != null)
                            && (accountImage.length > 0))
                    {
                        globalAvatar = accountImage;

                        AvatarCacheUtils.cacheAvatar(
                            protocolProvider, accountImage);
                    }
                }

                if (globalAvatar != null && globalAvatar.length > 0)
                {
                    fireGlobalAvatarEvent(globalAvatar);
                }
            }

            if(!StringUtils.isNullOrEmpty(provisionedDisplayName)
                || (!StringUtils.isNullOrEmpty(globalDisplayName) && !isUpdate))
                return;

            if (currentFirstName == null)
            {
                String firstName = AccountInfoUtils
                    .getFirstName(accountInfoOpSet);

                if (!StringUtils.isNullOrEmpty(firstName))
                {
                    currentFirstName = firstName;
                }
            }

            if (currentLastName == null)
            {
                String lastName = AccountInfoUtils
                    .getLastName(accountInfoOpSet);

                if (!StringUtils.isNullOrEmpty(lastName))
                {
                    currentLastName = lastName;
                }
            }

            if (currentFirstName == null && currentLastName == null)
            {
                String displayName = AccountInfoUtils
                    .getDisplayName(accountInfoOpSet);

                if (displayName != null)
                    currentDisplayName = displayName;
            }

            setGlobalDisplayName();
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        protected void setGlobalDisplayName()
        {
            String accountName = null;
            if (!StringUtils.isNullOrEmpty(currentFirstName))
            {
                accountName = currentFirstName;
            }

            if (!StringUtils.isNullOrEmpty(currentLastName))
            {
                /*
                 * If accountName is null, don't use += because
                 * it will make the accountName start with the
                 * string "null".
                 */
                if (StringUtils.isNullOrEmpty(accountName))
                    accountName = currentLastName;
                else
                    accountName += " " + currentLastName;
            }

            if (currentFirstName == null && currentLastName == null)
            {
                if (currentDisplayName != null)
                    accountName = currentDisplayName;
            }

            globalDisplayName = accountName;

            if (!StringUtils.isNullOrEmpty(globalDisplayName))
            {
                fireGlobalDisplayNameEvent(globalDisplayName);
            }
        }
    }

    /**
     * Notifies all interested listeners of a global display details change.
     *
     * @param displayName the new display name
     */
    private void fireGlobalDisplayNameEvent(String displayName)
    {
        List<GlobalDisplayDetailsListener> listeners;
        synchronized (displayDetailsListeners)
        {
            listeners = Collections.unmodifiableList(displayDetailsListeners);
        }

        Iterator<GlobalDisplayDetailsListener> listIter
            = listeners.iterator();
        while (listIter.hasNext())
        {
            listIter.next().globalDisplayNameChanged(
                new GlobalDisplayNameChangeEvent(this, displayName));
        }
    }

    /**
     * Notifies all interested listeners of a global display details change.
     *
     * @param avatar the new avatar
     */
    private void fireGlobalAvatarEvent(byte[] avatar)
    {
        List<GlobalDisplayDetailsListener> listeners;
        synchronized (displayDetailsListeners)
        {
            listeners = Collections.unmodifiableList(displayDetailsListeners);
        }

        Iterator<GlobalDisplayDetailsListener> listIter
            = listeners.iterator();
        while (listIter.hasNext())
        {
            listIter.next().globalDisplayAvatarChanged(
                new GlobalAvatarChangeEvent(this, avatar));
        }
    }
}
