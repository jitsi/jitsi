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

/**
 * Implemented by the user interface, this interface allows a protocol provider
 * to asynchronously demand passwords necessary for authentication against
 * various realms.
 * <p>
 * Or in other (simpler words) this is a callback or a hook that the UI would
 * give a protocol provider so that the protocol provider could
 * requestCredentials() when necessary (when a password is not available for
 * a server, or once it has changed, or re-demand one after a faulty
 * authentication)
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface SecurityAuthority
{
    /**
     * Indicates that the reason for obtaining credentials is that an
     * authentication is required.
     */
    public static final int AUTHENTICATION_REQUIRED = 0;

    /**
     * Indicates that the reason for obtaining credentials is that the last time
     * a wrong password has been provided.
     */
    public static final int WRONG_PASSWORD = 1;

    /**
     * Indicates that the reason for obtaining credentials is that the last time
     * a wrong user name has been provided.
     */
    public static final int WRONG_USERNAME = 2;

    /**
     * Indicates that the reason for obtaining credentials is that the last time
     * a wrong user name has been provided.
     */
    public static final int CONNECTION_FAILED = 3;

    /**
     * Returns a UserCredentials object associated with the specified realm, by
     * specifying the reason of this operation.
     * <p>
     * @param realm The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @param reasonCode indicates the reason for which we're obtaining the
     * credentials.
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    public UserCredentials obtainCredentials(String             realm,
                                             UserCredentials    defaultValues,
                                             int                reasonCode);

    /**
     * Returns a UserCredentials object associated with the specified realm, by
     * specifying the reason of this operation.
     * <p>
     * @param realm The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    public UserCredentials obtainCredentials(String             realm,
                                             UserCredentials    defaultValues);

    /**
     * Sets the userNameEditable property, which should indicate to the
     * implementations of this interface if the user name could be changed by
     * user or not.
     *
     * @param isUserNameEditable indicates if the user name could be changed by
     * user in the implementation of this interface.
     */
    public void setUserNameEditable(boolean isUserNameEditable);

    /**
     * Indicates if the user name is currently editable, i.e. could be changed
     * by user or not.
     *
     * @return <code>true</code> if the user name could be changed,
     * <code>false</code> - otherwise.
     */
    public boolean isUserNameEditable();
}
