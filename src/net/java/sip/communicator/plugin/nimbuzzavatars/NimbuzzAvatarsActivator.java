/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.nimbuzzavatars;

import net.java.sip.communicator.service.customavatar.*;
import org.osgi.framework.*;

import java.io.*;
import java.net.*;

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
