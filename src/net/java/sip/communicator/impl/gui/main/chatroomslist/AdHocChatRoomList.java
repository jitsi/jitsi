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
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>AdHocChatRoomsList</tt> is the list containing all ad-hoc chat rooms.
 *
 * @author Valentin Martinet
 */
public class AdHocChatRoomList
{
    /**
     * The list containing all chat servers and ad-hoc rooms.
     */
    private final List<AdHocChatRoomProviderWrapper> providersList
        = new Vector<AdHocChatRoomProviderWrapper>();

    /**
     * Initializes the list of ad-hoc chat rooms.
     */
    public void loadList()
    {
        Collection<ServiceReference<ProtocolProviderService>> serRefs
            = ServiceUtils.getServiceReferences(
                    GuiActivator.bundleContext,
                    ProtocolProviderService.class);

        if ((serRefs != null) && !serRefs.isEmpty())
        {
            for (ServiceReference<ProtocolProviderService> serRef : serRefs)
            {
                ProtocolProviderService protocolProvider
                    = GuiActivator.bundleContext.getService(serRef);
                Object adHocMultiUserChatOpSet
                    = protocolProvider.getOperationSet(
                            OperationSetAdHocMultiUserChat.class);

                if (adHocMultiUserChatOpSet != null)
                    addChatProvider(protocolProvider);
            }
        }
    }

    /**
     * Adds a chat server and all its existing ad-hoc chat rooms.
     *
     * @param pps the <tt>ProtocolProviderService</tt> corresponding to the chat
     * server
     */
    public void addChatProvider(ProtocolProviderService pps)
    {
        AdHocChatRoomProviderWrapper chatRoomProvider
            = new AdHocChatRoomProviderWrapper(pps);

        providersList.add(chatRoomProvider);

        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts =
            configService.getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts) {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(pps
                    .getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService
                    .getPropertyNamesByPrefix(
                        accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomID
                        = configService.getString(chatRoomPropName);

                    String chatRoomName = configService.getString(
                        chatRoomPropName + ".chatRoomName");

                    AdHocChatRoomWrapper chatRoomWrapper
                        = new AdHocChatRoomWrapper( chatRoomProvider,
                                                    chatRoomID,
                                                    chatRoomName);

                    chatRoomProvider.addAdHocChatRoom(chatRoomWrapper);
                }
            }
        }
    }

    /**
     * Removes the corresponding server and all related ad-hoc chat rooms from
     * this list.
     *
     * @param pps the <tt>ProtocolProviderService</tt> corresponding to the
     *            server to remove
     */
    public void removeChatProvider(ProtocolProviderService pps)
    {
        AdHocChatRoomProviderWrapper wrapper =
            findServerWrapperFromProvider(pps);

        if (wrapper != null)
            removeChatProvider(wrapper);
    }

    /**
     * Removes the corresponding server and all related ad-hoc chat rooms from
     * this list.
     *
     * @param adHocChatRoomProvider the <tt>AdHocChatRoomProviderWrapper</tt>
     *            corresponding to the server to remove
     */
    private void removeChatProvider(
            AdHocChatRoomProviderWrapper adHocChatRoomProvider)
    {
        providersList.remove(adHocChatRoomProvider);

        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        String providerAccountUID
            = adHocChatRoomProvider
                    .getProtocolProvider().getAccountID().getAccountUniqueID();

        for (String accountRootPropName
                : configService.getPropertyNamesByPrefix(prefix, true))
        {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(providerAccountUID))
            {
                List<String> chatRooms
                    = configService.getPropertyNamesByPrefix(
                            accountRootPropName + ".chatRooms",
                            true);

                for (String chatRoomPropName : chatRooms)
                {
                    configService.setProperty(
                        chatRoomPropName + ".chatRoomName",
                        null);
                }

                configService.setProperty(accountRootPropName, null);
            }
        }
    }

    /**
     * Adds a chat room to this list.
     *
     * @param adHocChatRoomWrapper the <tt>AdHocChatRoom</tt> to add
     */
    public void addAdHocChatRoom(AdHocChatRoomWrapper adHocChatRoomWrapper)
    {
        AdHocChatRoomProviderWrapper adHocChatRoomProvider
            = adHocChatRoomWrapper.getParentProvider();

        if (!adHocChatRoomProvider.containsAdHocChatRoom(adHocChatRoomWrapper))
            adHocChatRoomProvider.addAdHocChatRoom(adHocChatRoomWrapper);
    }

    /**
     * Removes the given <tt>AdHocChatRoom</tt> from the list of all ad-hoc
     * chat rooms.
     *
     * @param adHocChatRoomWrapper the <tt>AdHocChatRoomWrapper</tt> to remove
     */
    public void removeChatRoom(AdHocChatRoomWrapper adHocChatRoomWrapper)
    {
        AdHocChatRoomProviderWrapper adHocChatRoomProvider
            = adHocChatRoomWrapper.getParentProvider();

        if (providersList.contains(adHocChatRoomProvider))
        {
            adHocChatRoomProvider.removeChatRoom(adHocChatRoomWrapper);
        }
    }

    /**
     * Returns the <tt>AdHocChatRoomWrapper</tt> that correspond to the given
     * <tt>AdHocChatRoom</tt>. If the list of ad-hoc chat rooms doesn't contain
     * a corresponding wrapper - returns null.
     *
     * @param adHocChatRoom the <tt>ChatRoom</tt> that we're looking for
     * @return the <tt>ChatRoomWrapper</tt> object corresponding to the given
     * <tt>ChatRoom</tt>
     */
    public AdHocChatRoomWrapper findChatRoomWrapperFromAdHocChatRoom(
            AdHocChatRoom adHocChatRoom)
    {
        for (AdHocChatRoomProviderWrapper provider : providersList)
        {
            AdHocChatRoomWrapper chatRoomWrapper
                = provider.findChatRoomWrapperForAdHocChatRoom(
                        adHocChatRoom);

            if (chatRoomWrapper != null)
            {
                // stored chatrooms has no chatroom, but their
                // id is the same as the chatroom we are searching wrapper
                // for
                if(chatRoomWrapper.getAdHocChatRoom() == null)
                {
                    chatRoomWrapper.setAdHocChatRoom(adHocChatRoom);
                }

                return chatRoomWrapper;
            }
        }
        return null;
    }

    /**
     * Returns the <tt>AdHocChatRoomProviderWrapper</tt> that correspond to the
     * given <tt>ProtocolProviderService</tt>. If the list doesn't contain a
     * corresponding wrapper - returns null.
     *
     * @param protocolProvider the protocol provider that we're looking for
     * @return the <tt>AdHocChatRoomProvider</tt> object corresponding to
     * the given <tt>ProtocolProviderService</tt>
     */
    public AdHocChatRoomProviderWrapper findServerWrapperFromProvider(
        ProtocolProviderService protocolProvider)
    {
        for(AdHocChatRoomProviderWrapper chatRoomProvider : providersList)
        {
            if(chatRoomProvider.getProtocolProvider().equals(protocolProvider))
            {
                return chatRoomProvider;
            }
        }

        return null;
    }
}
