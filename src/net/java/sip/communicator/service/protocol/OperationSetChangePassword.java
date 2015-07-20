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
 * An operation set that allows "inband" change of the account password
 *
 * @author Boris Grozev
 */
public interface OperationSetChangePassword
    extends OperationSet
{
    /**
     * Changes the account password to newPass
     *
     * @param newPass the new password.
     * @throws IllegalStateException if the account is not registered.
     * @throws OperationFailedException if the change failed for another reason.
     */
    public void changePassword(String newPass)
            throws IllegalStateException, OperationFailedException;

    /**
     * Whether password changes are supported.
     * @return True if the server supports password change, false otherwise.
     */
    public boolean supportsPasswordChange();
}
