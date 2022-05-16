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
package net.java.sip.communicator.service.muc;

import net.java.sip.communicator.service.protocol.*;

import java.beans.*;

/**
 * The <tt>ChatRoomWrapper</tt> is the representation of the <tt>ChatRoom</tt>
 * in the GUI. It stores the information for the chat room even when the
 * corresponding protocol provider is not connected.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public interface ChatRoomWrapper
{
    /**
     * Property to be fired when successfully joined to chat room.
     */
    public static final String JOIN_SUCCESS_PROP = "Success";

    /**
     * Property to be fired when authentication failed
     * while joining a chat room.
     */
    public static final String JOIN_AUTHENTICATION_FAILED_PROP
        = "AuthenticationFailed";
    /**
     * Property to be fired when chat room requires registration and we failed
     * while joining the chat room.
     */
    public static final String JOIN_REGISTRATION_REQUIRED_PROP
        = "RegistrationRequired";

    /**
     * Property to be fired when provider is not registered
     * while joining a chat room.
     */
    public static final String JOIN_PROVIDER_NOT_REGISTERED_PROP
        = "ProviderNotRegistered";

    /**
     * Property to be fired when we try to join twice the same chat room.
     */
    public static final String JOIN_SUBSCRIPTION_ALREADY_EXISTS_PROP
        = "SubscriptionAlreadyExists";

    /**
     * Property to be fired when unknown error occurred
     * while joining a chat room.
     */
    public static final String JOIN_UNKNOWN_ERROR_PROP = "UnknownError";

    /**
     * Returns the <tt>ChatRoom</tt> that this wrapper represents.
     *
     * @return the <tt>ChatRoom</tt> that this wrapper represents.
     */
    public ChatRoom getChatRoom();

    /**
     * Sets the <tt>ChatRoom</tt> that this wrapper represents.
     *
     * @param chatRoom the chat room
     */
    public void setChatRoom(ChatRoom chatRoom);

    /**
     * Returns the chat room name.
     *
     * @return the chat room name
     */
    public String getChatRoomName();

    /**
     * Returns the identifier of the chat room.
     *
     * @return the identifier of the chat room
     */
    public String getChatRoomID();

    /**
     * Returns the parent protocol provider.
     *
     * @return the parent protocol provider
     */
    public ChatRoomProviderWrapper getParentProvider();

    /**
     * Returns <code>true</code> if the chat room is persistent,
     * otherwise - returns <code>false</code>.
     *
     * @return <code>true</code> if the chat room is persistent,
     * otherwise - returns <code>false</code>.
     */
    public boolean isPersistent();

    /**
     * Change persistence of this room.
     * @param value set persistent state.
     */
    public void setPersistent(boolean value);
    
    /**
     * Stores the password for the chat room.
     * 
     * @param password the password to store
     */
    public void savePassword(String password);
    
    /**
     * Returns the password for the chat room.
     * 
     * @return the password
     */
    public String loadPassword();
    
    /**
     * Removes the saved password for the chat room.
     */
    public void removePassword();

    /**
     * Is room set to auto join on start-up.
     * @return is auto joining enabled.
     */
    public boolean isAutojoin();

    /**
     * Changes auto join value in configuration service.
     *
     * @param value change of auto join property.
     */
    public void setAutoJoin(boolean value);
    
    /**
     * Removes the listeners.
     */
    public void removeListeners();

    /**
     * Property changes for the room wrapper. Like join status changes.
     * @param listener the listener to be notified.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes property change listener.
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);
}
