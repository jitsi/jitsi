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
package net.java.sip.communicator.util.skin;

/**
 * Interface that represents all skinnable user interface components. Any
 * component interested in being reloaded after a new skin installation should
 * implement this interface.
 *
 * @author Adam Netocny
 */
public interface Skinnable
{
    /**
     * Loads the skin for this skinnable. This method is meant to be used by
     * user interface components interested in being skinnable. This is where
     * all images, forgrounds and backgrounds should be loaded in order new
     * skin to take effect.
     */
    public void loadSkin();
}
