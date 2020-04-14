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

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The ICQ implementation of a sip-communicator AccountID
 *
 * @author Emil Ivov
 */
public class IcqAccountID
    extends AccountID
{
    /**
     * Then name of a property which represenstots is this account icq or aim.
     */
    public static final String IS_AIM = "IS_AIM";

    /**
     * Creates an icq account id from the specified uin and account properties.
     * If property IS_AIM is set to true then this is an AIM account, else
     * an Icq one.
     * @param uin the uin identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    IcqAccountID(String uin, Map<String, String> accountProperties )
    {
        super(
            uin,
            accountProperties,
            isAIM(accountProperties) ? ProtocolNames.AIM : ProtocolNames.ICQ,
            isAIM(accountProperties) ? "aim.com" : "icq.com");
    }

    /**
     * Checks whether the specified set of account properties describes an AIM
     * account.
     *
     * @param accountProperties the set of account properties to be checked
     * whether they describe an AIM account
     * @return <tt>true</tt> if <tt>accountProperties</tt> describes an AIM
     * account; otherwise, <tt>false</tt>
     */
    static boolean isAIM(Map<String, String> accountProperties)
    {
        String isAim = accountProperties.get(IS_AIM);

        return "true".equalsIgnoreCase(isAim);
    }
}
