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
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Dict implementation of a sip-communicator account id.
 * @author LITZELMANN Cedric
 * @author ROTH Damien
 */
public class DictAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     *
     * @param userID the user identifier correspnding to the account
     * @param accountProperties any other properties necessary for the account.
     */
    DictAccountID(String userID, Map<String, String> accountProperties)
    {
        super(userID, accountProperties, ProtocolNames.DICT, "dict.org");
    }

    /**
     * Returns the dict server adress
     * @return the dict server adress
     */
    public String getHost()
    {
        return getAccountPropertyString(ProtocolProviderFactory.SERVER_ADDRESS);
    }

    /**
     * Returns the dict server port
     * @return the dict server port
     */
    public int getPort()
    {
        return Integer
            .parseInt(getAccountPropertyString(ProtocolProviderFactory.SERVER_PORT));
    }

    /**
     * Returns the selected strategy
     * @return the selected strategy
     */
    public String getStrategy()
    {
        return getAccountPropertyString(ProtocolProviderFactory.STRATEGY);
    }
}
