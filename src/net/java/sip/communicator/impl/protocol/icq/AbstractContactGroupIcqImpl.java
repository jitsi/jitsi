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
 * The reason for having an abstract class is only - being able to easily
 * recognize our own (ICQ) contacts.
 * @author Emil Ivov
 */
public abstract class AbstractContactGroupIcqImpl
    implements ContactGroup
{


}
