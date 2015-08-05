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
package net.java.sip.communicator.impl.gui.main.chat;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>ChatContact</tt> is a wrapping class for the <tt>Contact</tt> and
 * <tt>ChatRoomMember</tt> interface.
 *
 * @param <T> the type of the descriptor
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public abstract class ChatContact<T>
{
    /**
     * The height of the avatar icon.
     */
    public static final int AVATAR_ICON_HEIGHT = 25;

    /**
     * The width of the avatar icon.
     */
    public static final int AVATAR_ICON_WIDTH = 25;

    /**
     * The avatar image corresponding to the source contact in the form of an
     * <code>ImageIcon</code>.
     */
    private ImageIcon avatar;

    /**
     * The avatar image corresponding to the source contact in the form of an
     * array of bytes.
     */
    private byte[] avatarBytes;

    /**
     * The descriptor being adapted by this instance.
     */
    protected final T descriptor;

    /**
     * If this instance is selected.
     */
    private boolean selected;

    /**
     * Initializes a new <tt>ChatContact</tt> instance with a specific
     * descriptor.
     *
     * @param descriptor the descriptor to be adapted by the new instance
     */
    protected ChatContact(T descriptor)
    {
        this.descriptor = descriptor;
    }

    /**
     * Determines whether a specific <tt>Object</tt> represents the same value
     * as this <tt>ChatContact</tt>.
     *
     * @param obj the <tt>Object</tt> to be checked for value equality with this
     * <tt>ChatContact</tt>
     * @return <tt>true</tt> if <tt>obj</tt> represents the same value as this
     * <tt>ChatContact</tt>; otherwise, <tt>false</tt>.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        /*
         * ChatContact is an adapter so two ChatContacts of the same runtime
         * type with equal descriptors are equal.
         */
        if (!getClass().isInstance(obj))
            return false;

        @SuppressWarnings("unchecked")
        ChatContact<T> chatContact = (ChatContact<T>) obj;

        return getDescriptor().equals(chatContact.getDescriptor());
    }

    /**
     * Returns the avatar image corresponding to the source contact. In the case
     * of multi user chat contact returns null.
     *
     * @return the avatar image corresponding to the source contact. In the case
     * of multi user chat contact returns null
     */
    public ImageIcon getAvatar()
    {
        byte[] avatarBytes = getAvatarBytes();

        if (this.avatarBytes != avatarBytes)
        {
            this.avatarBytes = avatarBytes;
            this.avatar = null;
        }
        if ((this.avatar == null)
                && (this.avatarBytes != null) && (this.avatarBytes.length > 0))
            this.avatar
                    = ImageUtils.getScaledRoundedIcon(
                            this.avatarBytes,
                            AVATAR_ICON_WIDTH,
                            AVATAR_ICON_HEIGHT);
        return this.avatar;
    }

    /**
     * Gets the avatar image corresponding to the source contact in the form of
     * an array of bytes.
     *
     * @return an array of bytes which represents the avatar image corresponding
     *         to the source contact
     */
    protected abstract byte[] getAvatarBytes();

    /**
     * Returns the descriptor object corresponding to this chat contact. In the
     * case of single chat this could be the <tt>MetaContact</tt> and in the
     * case of conference chat this could be the <tt>ChatRoomMember</tt>.
     *
     * @return the descriptor object corresponding to this chat contact.
     */
    public T getDescriptor()
    {
        return descriptor;
    }

    /**
     * Returns the contact name.
     *
     * @return the contact name
     */
    public abstract String getName();

    /**
     * Gets the implementation-specific identifier which uniquely specifies this
     * contact.
     *
     * @return an identifier which uniquely specifies this contact
     */
    public abstract String getUID();

    /**
     * Gets a hash code value for this object for the benefit of hashtables.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode()
    {
        /*
         * ChatContact is an adapter so two ChatContacts of the same runtime
         * type with equal descriptors are equal.
         */
        return getDescriptor().hashCode();
    }

    /**
     * Returns <code>true</code> if this is the currently selected contact in
     * the list of contacts for the chat, otherwise returns <code>false</code>.
     * @return <code>true</code> if this is the currently selected contact in
     * the list of contacts for the chat, otherwise returns <code>false</code>.
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * Sets this isSelected property of this chat contact.
     *
     * @param selected <code>true</code> to indicate that this contact would be
     * the selected contact in the list of chat window contacts; otherwise,
     * <code>false</code>
     */
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }
}
