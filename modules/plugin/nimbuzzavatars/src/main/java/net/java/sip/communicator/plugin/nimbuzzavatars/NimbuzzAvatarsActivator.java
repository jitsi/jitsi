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
package net.java.sip.communicator.plugin.nimbuzzavatars;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.customavatar.*;

import org.osgi.framework.*;

/**
 * OSGi bundle activator for the Nimbuzz custom avatars service.
 *
 * @author Damian Minkov
 */
public class NimbuzzAvatarsActivator
    implements BundleActivator,
               CustomAvatarService
{
    /**
     * The context.
     */
    static BundleContext bundleContext;

    /**
     * The download link location.
     */
    private static final String AVATAR_DOWNLOAD_LINK =
        "http://avatar.nimbuzz.com/getAvatar?jid=";

    /**
     * The service name we are handling.
     */
    private static final String SERVICE_NAME = "nimbuzz.com";


    /**
     * Starts this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        bc.registerService(
            CustomAvatarService.class.getName(),
            this, null);
    }

    /**
     * Stops this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void stop(BundleContext bc)
        throws Exception
    {
    }

    /**
     * Returns the avatar bytes for the given contact address.
     * @param address the address to search for its avatar.
     * @return image bytes.
     */
    public byte[] getAvatar(String address)
    {
        if(address == null || !address.endsWith(SERVICE_NAME))
        {
            return null;
        }

        try
        {
            URL sourceURL = new URL(AVATAR_DOWNLOAD_LINK + address);
            URLConnection conn = sourceURL.openConnection();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = conn.getInputStream();

            byte[] b = new byte[1024];
            int read;
            while((read = in.read(b)) != -1)
            {
                out.write(b, 0, read);
            }
            in.close();

            return out.toByteArray();
        }
        catch(Throwable t)
        {}

        return null;
    }
}
