package net.java.sip.communicator.impl.protocol.icq;

import net.java.sip.communicator.service.protocol.*;

/**
 * The ICQ implementation of the service.protocol.ContactGroup interface. There
 * are two types of groups possible here. <tt>RootContactGroupIcqImpl</tt>
 * which is the root node of the ContactList itself and
 * <tt>ContactGroupIcqImpl</tt> which represents standard icq groups. The
 * reason for having those 2 is that generally, ICQ groups may not contain
 * subgroups. A contact list on the other hand may not directly contain buddies.
 *
 *
 * The reason for having an abstract class is only - being able to esily
 * recognize our own (ICQ) contacts.
 * @author Emil Ivov
 */
public abstract class AbstractContactGroupIcqImpl
    implements ContactGroup
{


}
