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
package net.java.sip.communicator.impl.muc;


import java.beans.*;

import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.util.event.*;

/**
 * The <tt>ChatRoomWrapper</tt> is the representation of the <tt>ChatRoom</tt>
 * in the GUI. It stores the information for the chat room even when the
 * corresponding protocol provider is not connected.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class ChatRoomWrapperImpl
    extends PropertyChangeNotifier
    implements ChatRoomWrapper
{
    /**
     * The protocol provider to which the corresponding chat room belongs.
     */
    private final ChatRoomProviderWrapper parentProvider;

    /**
     * The room that is wrapped.
     */
    private ChatRoom chatRoom;

    /**
     * The room name.
     */
    private final String chatRoomName;

    /**
     * The room id.
     */
    private final String chatRoomID;

    /**
     * The property we use to store values in configuration service.
     */
    private static final String AUTOJOIN_PROPERTY_NAME = "autoJoin";

    /**
     * As isAutoJoin can be called from GUI many times we store its value once
     * retrieved to minimize calls to configuration service.
     */
    private Boolean autoJoin = null;

    /**
     * By default all chat rooms are persistent from UI point of view.
     * But we can override this and force not saving it.
     * If not overridden we query the wrapped room.
     */
    private Boolean persistent = null;
    
    /**
     * The prefix needed by the credential storage service to store the password
     * of the chat room.
     */
    private String passwordPrefix;
    
    /**
     * The property change listener for the message service.
     */
    private PropertyChangeListener propertyListener 
        = new PropertyChangeListener()
    {
        
        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            MessageHistoryService mhs = MUCActivator.getMessageHistoryService();
            if(!mhs.isHistoryLoggingEnabled() 
                || !mhs.isHistoryLoggingEnabled(getChatRoomID()))
            {
                MUCService.setChatRoomAutoOpenOption(
                    getParentProvider().getProtocolProvider(), 
                    getChatRoomID(), 
                    MUCService.OPEN_ON_ACTIVITY);
            }
        }

    };

    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the protocol provider,
     * the identifier and the name of the chat room.
     *
     * @param parentProvider the protocol provider to which the corresponding
     * chat room belongs
     * @param chatRoomID the identifier of the corresponding chat room
     * @param chatRoomName the name of the corresponding chat room
     */
    public ChatRoomWrapperImpl( ChatRoomProviderWrapper parentProvider,
                            String chatRoomID,
                            String chatRoomName)
    {
        this.parentProvider = parentProvider;
        this.chatRoomID = chatRoomID;
        this.chatRoomName = chatRoomName;
        passwordPrefix = ConfigurationUtils.getChatRoomPrefix(
            getParentProvider().getProtocolProvider().getAccountID()
            .getAccountUniqueID(), chatRoomID) + ".password";
        
        MUCActivator.getConfigurationService().addPropertyChangeListener(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED, 
            propertyListener);
        
        MUCActivator.getConfigurationService().addPropertyChangeListener(
            MessageHistoryService
                .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX + "."
                    + getChatRoomID(), 
                propertyListener);
    }

    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the corresponding chat
     * room.
     *
     * @param parentProvider the protocol provider to which the corresponding
     * chat room belongs
     * @param chatRoom the chat room to which this wrapper corresponds.
     */
    public ChatRoomWrapperImpl( ChatRoomProviderWrapper parentProvider,
                            ChatRoom chatRoom)
    {
        this(   parentProvider,
                chatRoom.getIdentifier(),
                chatRoom.getName());

        this.chatRoom = chatRoom;
    }

    /**
     * Returns the <tt>ChatRoom</tt> that this wrapper represents.
     *
     * @return the <tt>ChatRoom</tt> that this wrapper represents.
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    /**
     * Sets the <tt>ChatRoom</tt> that this wrapper represents.
     *
     * @param chatRoom the chat room
     */
    public void setChatRoom(ChatRoom chatRoom)
    {
        this.chatRoom = chatRoom;
    }

    /**
     * Returns the chat room name.
     *
     * @return the chat room name
     */
    public String getChatRoomName()
    {
        return chatRoomName;
    }

    /**
     * Returns the identifier of the chat room.
     *
     * @return the identifier of the chat room
     */
    public String getChatRoomID()
    {
        return chatRoomID;
    }

    /**
     * Returns the parent protocol provider.
     *
     * @return the parent protocol provider
     */
    public ChatRoomProviderWrapper getParentProvider()
    {
        return this.parentProvider;
    }

    /**
     * Returns <code>true</code> if the chat room is persistent,
     * otherwise - returns <code>false</code>.
     *
     * @return <code>true</code> if the chat room is persistent,
     * otherwise - returns <code>false</code>.
     */
    public boolean isPersistent()
    {
        if(persistent == null)
        {
            if(chatRoom != null)
                persistent = chatRoom.isPersistent();
            else
                return true;
        }

        return persistent;
    }

    /**
     * Change persistence of this room.
     * @param value set persistent state.
     */
    public void setPersistent(boolean value)
    {
        this.persistent = value;
    }
    
    
    /**
     * Stores the password for the chat room.
     * 
     * @param password the password to store
     */
    public void savePassword(String password)
    {
        
        MUCActivator.getCredentialsStorageService()
            .storePassword(passwordPrefix, password);
    }
    
    /**
     * Returns the password for the chat room.
     * 
     * @return the password
     */
    public String loadPassword()
    {
        return MUCActivator.getCredentialsStorageService()
            .loadPassword(passwordPrefix);
    }
    
    /**
     * Removes the saved password for the chat room.
     */
    public void removePassword()
    {
        MUCActivator.getCredentialsStorageService()
            .removePassword(passwordPrefix);
    }

    /**
     * Is room set to auto join on start-up.
     * @return is auto joining enabled.
     */
    public boolean isAutojoin()
    {
        if(autoJoin == null)
        {
            String val = ConfigurationUtils.getChatRoomProperty(
                getParentProvider().getProtocolProvider(),
                getChatRoomID(), AUTOJOIN_PROPERTY_NAME);

            autoJoin = Boolean.valueOf(val);
        }

        return autoJoin;
    }

    /**
     * Changes auto join value in configuration service.
     *
     * @param value change of auto join property.
     */
    public void setAutoJoin(boolean value)
    {
        autoJoin = value;

        // as the user wants to autojoin this room
        // and it maybe already created as non persistent
        // we must set it persistent and store it
        if(!isPersistent())
        {
            setPersistent(true);

            ConfigurationUtils.saveChatRoom(
                getParentProvider().getProtocolProvider(),
                getChatRoomID(),
                getChatRoomID(),
                getChatRoomName());
        }

        if(value)
        {
            ConfigurationUtils.updateChatRoomProperty(
                getParentProvider().getProtocolProvider(),
                chatRoomID, AUTOJOIN_PROPERTY_NAME, Boolean.toString(autoJoin));
        }
        else
        {
            ConfigurationUtils.updateChatRoomProperty(
                getParentProvider().getProtocolProvider(),
                chatRoomID, AUTOJOIN_PROPERTY_NAME, null);
        }
        MUCActivator.getMUCService().fireChatRoomListChangedEvent(this, 
            ChatRoomListChangeEvent.CHAT_ROOM_CHANGED);
    }

    /**
     * Removes the listeners.
     */
    public void removeListeners()
    {
        MUCActivator.getConfigurationService().removePropertyChangeListener(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED,
            propertyListener);
        MUCActivator.getConfigurationService().removePropertyChangeListener(
            MessageHistoryService
                .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX + "."
                    + getChatRoomID(),
            propertyListener);
    }

    /**
     * Fire property change.
     * @param property
     */
    public void firePropertyChange(String property)
    {
        super.firePropertyChange(property, null, null);
    }
}
