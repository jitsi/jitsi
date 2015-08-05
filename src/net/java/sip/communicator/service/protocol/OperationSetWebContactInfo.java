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
package net.java.sip.communicator.service.protocol;

import java.net.*;

/**
 * The operation set is a very simplified version of the server stored info
 * operation sets, allowing protocol providers to implement a quick way of
 * showing user information, by simply returning a URL where the information
 * of a specific user is to be found.
 */
public interface OperationSetWebContactInfo
    extends OperationSet
{
    /**
     * Returns the URL of a page containing information on <tt>contact</tt>
     * @param contact the <tt>Contact</tt> that we'd like to get information
     * about.
     * @return the URL of a page containing information on the specified
     * contact.
     */
    public URL getWebContactInfo(Contact contact);

    /**
     * Returns the URL of a page containing information on the contact with the
     * specified <tt>contactAddress</tt>.
     * @param contactAddress the <tt>contactAddress</tt> that we'd like to get
     * information about.
     * @return the URL of a page containing information on the specified
     * contact.
     */
    public URL getWebContactInfo(String contactAddress);
}
