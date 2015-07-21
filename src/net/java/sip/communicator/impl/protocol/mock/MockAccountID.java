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
package net.java.sip.communicator.impl.protocol.mock;

import net.java.sip.communicator.service.protocol.*;

/**
 * A default, 1-to-1 mock implementation of the account id.
 * @author Emil Ivov
 */
public class MockAccountID
    extends AccountID
{
    public static final String MOCK_SERVICE_NAME = "MockService";
    protected MockAccountID(String userName)
    {
        super(  userName
              , new java.util.Hashtable<String, String>()
              , ProtocolNames.SIP_COMMUNICATOR_MOCK
              , MOCK_SERVICE_NAME);
    }
}
