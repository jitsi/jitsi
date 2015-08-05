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
package net.java.sip.communicator.service.credentialsstorage;

/**
 * Master password input dialog.
 *
 * @author Damian Minkov
 */
public interface MasterPasswordInputService
{
    /**
     * Shows an input dialog to the user to obtain the master password.
     *
     * @param prevSuccess <tt>true</tt> if any previous call returned a correct
     * master password and there is no need to show an extra "verification
     * failed" message
     * @return the master password obtained from the user or <tt>null</tt> if
     * none was provided
     */
    public String showInputDialog(boolean prevSuccess);
}
