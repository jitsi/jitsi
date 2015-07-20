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
package net.java.sip.communicator.service.customcontactactions;

import java.util.*;

/**
 * The <tt>CustomContactActionsService</tt> can be used to define a set of
 * custom actions for a contact list entry.
 *
 * @author Damian Minkov
 */
public interface CustomContactActionsService<T>
{
    /**
     * Returns the template class that this service has been initialized with
     *
     * @return the template class
     */
    public Class<T> getContactSourceClass();

    /**
     * Returns all custom actions defined by this service.
     *
     * @return an iterator over a list of <tt>ContactAction</tt>s
     */
    public Iterator<ContactAction<T>> getCustomContactActions();
    
    /**
     * Returns all custom actions menu items defined by this service.
     *
     * @return an iterator over a list of <tt>ContactActionMenuItem</tt>s
     */
    public Iterator<ContactActionMenuItem<T>> getCustomContactActionsMenuItems();
}
