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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;

/**
 * A simple implementation of the <tt>OperationSetAvatar</tt> interface for the
 * jabber protocol.
 *
 * Actually there isn't any maximum size for the jabber protocol but GoogleTalk
 * fix it a 96x96.
 *
 * @author Damien Roth
 */
public class OperationSetAvatarJabberImpl extends
        AbstractOperationSetAvatar<ProtocolProviderServiceJabberImpl>
{

    /**
     * Creates a new instances of <tt>OperationSetAvatarJabberImpl</tt>.
     *
     * @param parentProvider a reference to the
     * <tt>ProtocolProviderServiceJabberImpl</tt> instance that created us.
     * @param accountInfoOpSet a reference to the
     * <tt>OperationSetServerStoredAccountInfo</tt>.
     */
    public OperationSetAvatarJabberImpl(
            ProtocolProviderServiceJabberImpl parentProvider,
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        super(parentProvider, accountInfoOpSet, 96, 96, 0);
    }

}
