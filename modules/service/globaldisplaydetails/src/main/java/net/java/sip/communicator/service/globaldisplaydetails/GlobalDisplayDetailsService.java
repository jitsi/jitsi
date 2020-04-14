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
package net.java.sip.communicator.service.globaldisplaydetails;

import net.java.sip.communicator.service.globaldisplaydetails.event.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>GlobalDisplayNameService</tt> offers generic access to a global
 * display name and an avatar for the local user. It could be used to show or
 * set the local user display name or avatar.
 * <p>
 * A global display name implementation could determine the information by going
 * through all different accounts' server stored information or by taking into
 * account a provisioned display name if any is available or choose any other
 * approach.
 *
 * @author Yana Stamcheva
 * @author Hristo Terezov
 *
 */
public interface GlobalDisplayDetailsService
{
    /**
     * Returns default display name for the given provider or the global display
     * name.
     * @param pps the given protocol provider service
     * @return default display name.
     */
    public String getDisplayName(ProtocolProviderService pps);

    /**
     * Returns the global display name to be used to identify the local user.
     *
     * @return a string representing the global local user display name
     */
    public String getGlobalDisplayName();

    /**
     * Sets the global local user display name.
     *
     * @param displayName the string representing the display name to set as
     * a global display name
     */
    public void setGlobalDisplayName(String displayName);

    /**
     * Returns the global avatar for the local user.
     *
     * @return a byte array containing the global avatar for the local user
     */
    public byte[] getGlobalDisplayAvatar();

    /**
     * Sets the global display avatar for the local user.
     *
     * @param avatar the byte array representing the avatar to set
     */
    public void setGlobalDisplayAvatar(byte[] avatar);

    /**
     * Adds the given <tt>GlobalDisplayDetailsListener</tt> to listen for change
     * events concerning the global display details.
     *
     * @param l the <tt>GlobalDisplayDetailsListener</tt> to add
     */
    public void addGlobalDisplayDetailsListener(
        GlobalDisplayDetailsListener l);

    /**
     * Removes the given <tt>GlobalDisplayDetailsListener</tt> listening for
     * change events concerning the global display details.
     *
     * @param l the <tt>GlobalDisplayDetailsListener</tt> to remove
     */
    public void removeGlobalDisplayDetailsListener(
        GlobalDisplayDetailsListener l);
}
