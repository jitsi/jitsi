/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.yahoo;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Yahoo implementation of the service.protocol.ContactGroup interface. There
 * are two types of groups possible here. <tt>RootContactGroupYahooImpl</tt>
 * which is the root node of the ContactList itself and
 * <tt>ContactGroupYahooImpl</tt> which represents standard groups. The
 * reason for having those 2 is that generally, Yahoo groups may not contain
 * subgroups. A contact list on the other hand may not directly contain buddies.
 *
 *
 * The reason for having an abstract class is only - being able to esily
 * recognize our own (Yahoo) contacts.
 * @author Damian Minkov
 */
public abstract class AbstractContactGroupYahooImpl
    implements ContactGroup
{


}
