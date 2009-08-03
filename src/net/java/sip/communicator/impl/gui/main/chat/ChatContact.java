/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatContact</tt> is a wrapping class for the <tt>Contact</tt> and
 * <tt>ChatRoomMember</tt> interface.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public abstract class ChatContact
{
    public static final int AVATAR_ICON_HEIGHT = 25;

    public static final int AVATAR_ICON_WIDTH = 25;

    private boolean isSelected;

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
     * Returns the descriptor object corresponding to this chat contact. In the
     * case of single chat this could be the <tt>MetaContact</tt> and in the
     * case of conference chat this could be the <tt>ChatRoomMember</tt>.
     * 
     * @return the descriptor object corresponding to this chat contact.
     */
    public abstract Object getDescriptor();

    /**
     * Returns the contact name.
     *
     * @return the contact name
     */
    public abstract String getName();

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
     * Gets the implementation-specific identifier which uniquely specifies this
     * contact.
     * 
     * @return an identifier which uniquely specifies this contact
     */
    public abstract String getUID();

    /**
     * Returns <code>true</code> if this is the currently selected contact in
     * the list of contacts for the chat, otherwise returns <code>false</code>.
     * @return <code>true</code> if this is the currently selected contact in
     * the list of contacts for the chat, otherwise returns <code>false</code>.
     */
    public boolean isSelected()
    {
        return isSelected;
    }

    /**
     * Sets this isSelected property of this chat contact.
     *
     * @param isSelected <code>true</code> to indicate that this contact would
     * be the selected contact in the list of chat window contacts, otherwise -
     * <code>false</code>
     */
    public void setSelected(boolean isSelected)
    {
        this.isSelected = isSelected;
    }
}
