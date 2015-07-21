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
package net.java.sip.communicator.service.resources;

import java.util.*;

/**
 * Default Skin Pack interface.
 *
 * @author Adam Netocny
 */
public interface SkinPack
    extends ResourcePack
{
    /**
     * Default resource name.
     */
    public static final String RESOURCE_NAME_DEFAULT_VALUE = "SkinPack";

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for image
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for image
     * resource pack.
     */
    public Map<String, String> getImageResources();

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for style
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for style
     * resource pack.
     */
    public Map<String, String> getStyleResources();

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for color
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for color
     * resource pack.
     */
    public Map<String, String> getColorResources();

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for settings
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for settings
     * resource pack.
     */
    public Map<String, String> getSettingsResources();
}
