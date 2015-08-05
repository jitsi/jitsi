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
package net.java.sip.communicator.impl.resources;

import java.io.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.impl.resources.util.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * A default implementation of the <tt>ResourceManagementService</tt>.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ResourceManagementServiceImpl
    extends AbstractResourcesService
{
    /**
     * The <tt>Logger</tt> used by the <tt>ResourceManagementServiceImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ResourceManagementServiceImpl.class);

    /**
     * UI Service reference.
     */
    private UIService uiService = null;

    /**
     * Initializes already registered default resource packs.
     */
    ResourceManagementServiceImpl()
    {
        super(ResourceManagementActivator.bundleContext);

        UIService serv = getUIService();
        if (serv != null)
        {
            serv.repaintUI();
        }
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    private UIService getUIService()
    {
        if (uiService == null)
        {
            uiService
                = ServiceUtils.getService(
                        ResourceManagementActivator.bundleContext,
                        UIService.class);
        }
        return uiService;
    }

    /**
     * Gets a reference to the <tt>UIService</tt> when this one is registered.
     *
     * @param event the <tt>ServiceEvent</tt> that has notified us
     */
    @Override
    public void serviceChanged(ServiceEvent event)
    {
        super.serviceChanged(event);

        Object sService = ResourceManagementActivator.bundleContext
                .getService(event.getServiceReference());

        if (sService instanceof UIService && uiService == null
                && event.getType() == ServiceEvent.REGISTERED)
        {
            uiService = (UIService) sService;
            uiService.repaintUI();
        }
        else if (sService instanceof UIService
                && event.getType() == ServiceEvent.UNREGISTERING)
        {
            if (uiService != null && uiService.equals(sService))
            {
                uiService = null;
            }
        }
    }

    /**
     * Repaints the whole UI when a skin pack has changed.
     */
    @Override
    protected void onSkinPackChanged()
    {
        UIService serv = getUIService();
        if (serv != null)
        {
            serv.repaintUI();
        }
    }

    /**
     * Returns the int representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the int representation of the color corresponding to the
     * given key.
     */
    public int getColor(String key)
    {
        String res = getColorResources().get(key);

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return 0xFFFFFF;
        }
        else
            return Integer.parseInt(res, 16);
    }

    /**
     * Returns the string representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the string representation of the color corresponding to the
     * given key.
     */
    public String getColorString(String key)
    {
        String res = getColorResources().get(key);

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return "0xFFFFFF";
        }
        else
            return res;
    }

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     *
     * @param path The path to the image file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     */
    public InputStream getImageInputStreamForPath(String path)
    {
        SkinPack skinPack = getSkinPack();
        if(skinPack!=null)
        {
            if(skinPack.getClass().getClassLoader()
                .getResourceAsStream(path)!=null)
            {
                return skinPack.getClass().getClassLoader()
                        .getResourceAsStream(path);
            }
        }

        ImagePack imagePack = getImagePack();
        if (path != null && imagePack != null)
            return imagePack.getClass().getClassLoader()
                    .getResourceAsStream(path);

        return null;
    }

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     *
     * @param streamKey The identifier of the image in the resource properties
     * file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     */
    public InputStream getImageInputStream(String streamKey)
    {
        String path = getImagePath(streamKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }

        return getImageInputStreamForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given key.
     *
     * @param urlKey The identifier of the image in the resource properties file.
     * @return the <tt>URL</tt> of the image corresponding to the given key
     */
    public URL getImageURL(String urlKey)
    {
        String path = getImagePath(urlKey);

        if (path == null || path.length() == 0)
        {
            if (logger.isInfoEnabled())
                logger.info("Missing resource for key: " + urlKey);
            return null;
        }
        return getImageURLForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given path.
     *
     * @param path The path to the given image file.
     * @return the <tt>URL</tt> of the image corresponding to the given path.
     */
    public URL getImageURLForPath(String path)
    {
        SkinPack skinPack = getSkinPack();
        if(skinPack!=null)
        {
            if(skinPack.getClass().getClassLoader().getResource(path)!=null)
            {
                return skinPack.getClass().getClassLoader().getResource(path);
            }
        }

        ImagePack imagePack = getImagePack();
        return imagePack.getClass().getClassLoader().getResource(path);
    }

    /**
     * Returns the <tt>URL</tt> of the sound corresponding to the given
     * property key.
     *
     * @return the <tt>URL</tt> of the sound corresponding to the given
     * property key.
     */
    public URL getSoundURL(String urlKey)
    {
        String path = getSoundPath(urlKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }
        return getSoundURLForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the sound corresponding to the given path.
     *
     * @param path the path, for which we're looking for a sound URL
     * @return the <tt>URL</tt> of the sound corresponding to the given path.
     */
    public URL getSoundURLForPath(String path)
    {
        return getSoundPack().getClass().getClassLoader().getResource(path);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    @Override
    public byte[] getImageInBytes(String imageID)
    {
        InputStream in = getImageInputStream(imageID);

        if(in == null)
            return null;

        byte[] image = null;

        try
        {
            image = new byte[in.available()];
            in.read(image);
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }

        return image;
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    @Override
    public ImageIcon getImage(String imageID)
    {
        URL imageURL = getImageURL(imageID);

        return (imageURL == null) ? null : new ImageIcon(imageURL);
    }

    /**
     * Builds a new skin bundle from the zip file content.
     *
     * @param zipFile Zip file with skin information.
     * @return <tt>File</tt> for the bundle.
     * @throws Exception When something goes wrong.
     */
    public File prepareSkinBundleFromZip(File zipFile)
        throws Exception
    {
        return SkinJarBuilder.createBundleFromZip(zipFile, getImagePack());
    }
}
