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

package net.java.sip.communicator.impl.protocol.ssh;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The SSH implementation of a sip-communicator account id.
 * @author Shobhit Jindal
 */
public class SSHAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier correspnding to thi account
     * @param accountProperties any other properties necessary for the account.
     */
    SSHAccountID(String userID, Map<String, String> accountProperties)
    {
        super(userID
              , accountProperties
              , "SSH"
              , "sip-communicator.org");
    }
}
