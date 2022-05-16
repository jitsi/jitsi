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
import net.java.sip.communicator.service.resources.*;

/**
 * The default image resource pack.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class DefaultImagePackImpl
    extends AbstractResourcePack
    implements ImagePack
{
    private static final String DEFAULT_RESOURCE_PATH
        = "resources.images.images";

    private final Map<String, String> resources = new TreeMap<>();

    public DefaultImagePackImpl()
    {
        ResourceBundle rb = ResourceBundle.getBundle(DEFAULT_RESOURCE_PATH);
        initResources(rb, resources);
    }

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     */
    @Override
    public Map<String, String> getResources()
    {
        return resources;
    }
}
