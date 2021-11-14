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
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.util.*;

public abstract class AbstractResourcePack
{
    /**
     * Fills the given resource map with all (key,value) pairs obtained from the
     * given {@link ResourceBundle}. This method will look in the properties
     * files for references to other properties files and will include in the
     * final map data from all referenced files.
     *
     * @param rb        The initial {@link ResourceBundle}, corresponding to the
     *                  "main" properties file.
     * @param resources A {@link Map} that would store the data.
     */
    protected final void initResources(ResourceBundle rb,
        Map<String, String> resources)
    {
        for (String key : rb.keySet())
        {
            resources.put(key, rb.getString(key));
        }
    }
}
