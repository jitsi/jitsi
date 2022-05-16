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
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;

import net.java.sip.communicator.impl.neomedia.audio.*;
import net.java.sip.communicator.impl.neomedia.video.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.utils.*;

/**
 * Implements <tt>MediaConfigurationService</tt> i.e. represents a factory of
 * user interface which allows the user to configure the media-related
 * functionality of the application.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Boris Grozev
 * @author Vincent Lucas
 */
public class MediaConfigurationImpl
    implements MediaConfigurationService
{
    /**
     * The <tt>MediaService</tt> implementation used by
     * <tt>MediaConfigurationImpl</tt>.
     */
    private static final MediaServiceImpl mediaService
        = NeomediaActivator.getMediaServiceImpl();

    /**
     * Returns a component for encodings configuration for the given
     * <tt>mediaType</tt>
     *
     * @param mediaType             Either <tt>MediaType.AUDIO</tt> or
     *                              <tt>MediaType.VIDEO</tt>
     * @param encodingConfiguration The <tt>EncodingConfiguration</tt> instance
     *                              to use. If null, it will use the current
     *                              encoding configuration from the media
     *                              service.
     * @return The component for encodings configuration.
     */
    @Override
    public Component createEncodingControls(
        MediaType mediaType,
        EncodingConfiguration encodingConfiguration)
    {
        if (encodingConfiguration == null)
        {
            encodingConfiguration
                = mediaService.getCurrentEncodingConfiguration();
        }

        return new EncodingConfigurationTab(mediaType, encodingConfiguration);
    }

    /**
     * Returns the audio configuration panel.
     *
     * @return the audio configuration panel
     */
    @Override
    public Component createAudioConfigPanel()
    {
        return new AudioConfigPanel();
    }

    /**
     * Returns the video configuration panel.
     *
     * @return the video configuration panel
     */
    @Override
    public Component createVideoConfigPanel()
    {
        return new VideoConfigPanel();
    }

    /**
     * Returns the <tt>MediaService</tt> instance.
     *
     * @return the <tt>MediaService</tt> instance
     */
    @Override
    public MediaService getMediaService()
    {
        return mediaService;
    }
}
