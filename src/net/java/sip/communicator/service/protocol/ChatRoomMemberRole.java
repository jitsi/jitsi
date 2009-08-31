/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Indicates roles that a chat room member detains in its containing chat room.
 *
 * @author Emil Ivov
 */
public class ChatRoomMemberRole
    implements Comparable<ChatRoomMemberRole>
{
    /**
     * A role implying the full set of chat room permissions
     */
    public static final ChatRoomMemberRole OWNER
                                = new ChatRoomMemberRole("Owner", 70);

    /**
     * A role implying administrative permissions.
     */
    public static final ChatRoomMemberRole ADMINISTRATOR
        = new ChatRoomMemberRole("Administrator", 60);

    /**
     * A role implying moderator permissions.
     */
    public static final ChatRoomMemberRole MODERATOR
        = new ChatRoomMemberRole("Moderator", 50);

    /**
     * A role implying standard participant permissions.
     */
    public static final ChatRoomMemberRole MEMBER
        = new ChatRoomMemberRole("Member", 40);

    /**
     * A role implying standard participant permissions.
     */
    public static final ChatRoomMemberRole GUEST
        = new ChatRoomMemberRole("Guest", 30);


    /**
     * A role implying standard participant permissions without the right to
     * send messages/speak.
     */
    public static final ChatRoomMemberRole SILENT_MEMBER
        = new ChatRoomMemberRole("SilentMember", 30);

    /**
     * A role implying an explicit ban for the user to join the room.
     */
    public static final ChatRoomMemberRole OUTCAST
        = new ChatRoomMemberRole("Outcast", 20);

    /**
     * the name of this role.
     */
    private final String roleName;

    /**
     * The index of a role is used to allow ordering of roles by other modules
     * (like the UI) that would not necessarily "know" all possible roles.
     * Higher values of the role index indicate roles with more permissions and
     * lower values pertain to more restrictive roles.
     */
    private final int roleIndex;

    /**
     * Creates a role with the specified <tt>roleName</tt>. The constructor
     * is protected in case protocol implementations need to add extra roles
     * (this should only be done when absolutely necessary in order to assert
     * smooth interoperability with the user interface).
     *
     * @param roleName the name of this role.
     * @param roleIndex an int that would allow to compare this role to others
     * according to the set of permissions that it implies.
     *
     * @throws java.lang.NullPointerException if roleName is null.
     */
    protected ChatRoomMemberRole(String roleName, int roleIndex)
        throws NullPointerException
    {
        if(roleName == null)
            throw new NullPointerException("Role Name can't be null.");

        this.roleName = roleName;
        this.roleIndex = roleIndex;
    }

    /**
     * Returns the name of this role.
     *
     * @return the name of this role.
     */
    public String getRoleName()
    {
        return this.roleName;
    }

    /**
     * Returns a localized (i18n) name role name.
     *
     * @return a i18n version of this role name.
     */
    public String getLocalizedRoleName()
    {
        return this.roleName;
    }

    /**
     * Returns a role index that can be used to allow ordering of roles by
     * other modules (like the UI) that would not necessarily "know" all
     * possible roles.  Higher values of the role index indicate roles with
     * more permissions and lower values pertain to more restrictive roles.
     *
     * @return an <tt>int</tt> that when compared to role indexes of other
     * roles can provide an ordering for the different role instances.
     */
    public int getRoleIndex()
    {
        return roleIndex;
    }

    /**
     * Indicates whether some other object is "equal to" this role instance.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if obj is a role instance that has the same
     * name and role index as this one.
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        /*
         * XXX Implementing Object#equals(Object) with instanceof is error
         * prone. The safe and recommended approach is to return true only if
         * the runtime types of the two Objects being tested are one and the
         * same i.e. getClass().equals(obj.getClass()).
         */
        if (!(obj instanceof ChatRoomMemberRole))
            return false;

        ChatRoomMemberRole role = (ChatRoomMemberRole) obj;

        return role.getRoleName().equals(getRoleName())
                && (role.getRoleIndex() == getRoleIndex());
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * <p>
     * @return  a hash code value for this object.
     */
    public int hashCode()
    {
        return getRoleName().hashCode();
    }

    /**
     * Compares this role's role index with that of the specified object for
     * order.  Returns a negative integer, zero, or a positive integer as this
     * role is less than, equal to, or greater than the specified object.
     *
     * @param   obj the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *            is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type is not an
     * instance of ChatRoomMemberRole.
     */
    public int compareTo(ChatRoomMemberRole obj)
        throws ClassCastException
    {
        return getRoleIndex() - obj.getRoleIndex();
    }
}
