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

/**
 * This is a protocol provider extensions class. Its called by the protocol
 * provider. This is the place to put custom OperationSets used in custom
 * branches of SIP Communicator.
 *
 * @author Damian Minkov
 */
public class ProtocolProviderExtensions
{
    /**
     * Method called by the protocol provider which is passed as an argument.
     * A place to register any custom OperationSets.
     * @param provider the protocol provider.
     */
    public static void registerCustomOperationSets(
        ProtocolProviderServiceSipImpl provider)
    {
    }
}
