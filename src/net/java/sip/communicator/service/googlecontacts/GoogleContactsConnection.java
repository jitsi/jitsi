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
package net.java.sip.communicator.service.googlecontacts;

/**
 * Interface that define a Google Contacts connection.
 *
 * @author Sebastien Vincent
 */
public interface GoogleContactsConnection
{
    /**
     * Enumeration for connection status.
     *
     */
    public enum ConnectionStatus
    {
        /**
         * Connection has failed due to invalid credentials.
         */
        ERROR_INVALID_CREDENTIALS,

        /**
         * Connection has failed due to unknown reason.
         */
        ERROR_UNKNOWN,

        /**
         * Connection has succeed.
         */
        SUCCESS;
    }

    /**
     * Get login.
     *
     * @return login to connect to the service
     */
    public String getLogin();

    /**
     * Set login.
     *
     * @param login login to connect to the service
     */
    public void setLogin(String login);

    /**
     * Initialize connection.
     *
     * @return connection status
     */
    public ConnectionStatus connect();

    /**
     * Returns the google contacts prefix.
     *
     * @return the google contacts prefix
     */
    public String getPrefix();
}
