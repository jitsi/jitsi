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
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;

/**
 * A simple implementation of the <tt>OperationSetAvatar</tt> interface for the
 * jabber protocol.
 *
 * @author Damian Minkov
 */
public class OperationSetAvatarSipImpl extends
        AbstractOperationSetAvatar<ProtocolProviderServiceSipImpl>
{

    /**
     * Constructs a new <tt>OperationSetAvatarSipImpl</tt>.
     *
     * @param parentProvider parent protocol provider service
     * @param accountInfoOpSet account info operation set
     */
    public OperationSetAvatarSipImpl(
            ProtocolProviderServiceSipImpl parentProvider,
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        super(parentProvider, accountInfoOpSet, 96, 96, 0);
    }

}
