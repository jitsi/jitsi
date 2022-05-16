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
 * The operation set is there to propose an extremely simple and light way of
 * registering new accounts for a given protocol. The
 * getAccountRegistrationURL() returns a URL that when opened in a web browser
 * allows you to register your own account with the corresponding protocol.
 * <p>
 *
 * @author Emil Ivov
 */
public interface OperationSetWebAccountRegistration
    extends OperationSet
{
    /**
     * Returns a URL that points to a page which allows for on-line registration
     * of accounts belonging to the service supported by this protocol provider.
     * <p>
     * @return a URL pointing to a web page where one could register their
     * account for the current protocol.
     */
    public URL getAccountRegistrationURL();
}
