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

import java.net.*;

import net.java.sip.communicator.service.protocol.*;

/**
 *
 * @author Damian Minkov
 */
public class OperationSetWebContactInfoIcqImpl
    implements OperationSetWebContactInfo
{
    public OperationSetWebContactInfoIcqImpl()
    {
    }

    /**
     * Returns the URL of a page containing information on <tt>contact</tt>
     *
     * @param contact the <tt>Contact</tt> that we'd like to get information
     *   about.
     * @return the URL of a page containing information on the specified
     *   contact.
     */
    public URL getWebContactInfo(Contact contact)
    {
        return getWebContactInfo(contact.getAddress());
    }

    /**
     * Returns the URL of a page containing information on the contact with
     * the specified <tt>contactAddress</tt>.
     *
     * @param contactAddress the <tt>contactAddress</tt> that we'd like to
     *   get information about.
     * @return the URL of a page containing information on the specified
     *   contact.
     */
    public URL getWebContactInfo(String contactAddress)
    {
        try
        {
            return new URL(
                "http://www.icq.com/people/about_me.php?uin=" +
                contactAddress);
        }
        catch (MalformedURLException ex)
        {
            return null;
        }
    }
}
