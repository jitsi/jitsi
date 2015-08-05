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
package net.java.sip.communicator.impl.replacement.directimage;

import java.net.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.directimage.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for direct
 * image links.
 *
 * @author Purvesh Sahoo
 * @author Marin Dzhigarov
 */
public class ReplacementServiceDirectImageImpl
    implements DirectImageReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceDirectImageImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String URL_PATTERN =
        "https?\\:\\/\\/(www\\.)*.*\\.(?:jpg|png|gif)";

    /**
     * Configuration label shown in the config form.
     */
    public static final String DIRECT_IMAGE_CONFIG_LABEL = "Direct Image Link";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "DIRECTIMAGE";

    /**
    * Maximum allowed size of the image in bytes. The default size is 2MB.
    */
    private long imgMaxSize = 2097152;

    /**
    * Configuration property name for maximum allowed size of the image in 
    * bytes.
    */
    private static final String MAX_IMG_SIZE = 
        "net.java.sip.communicator.impl.replacement.directimage.MAX_IMG_SIZE";

    /**
     * Constructor for <tt>ReplacementServiceDirectImageImpl</tt>.
     */
    public ReplacementServiceDirectImageImpl()
    {
        setMaxImgSizeFromConf();
        logger.trace("Creating a Direct Image Link Source.");
    }

    /**
    * Gets the max allowed size value in bytes from Configuration service and 
    * sets the value to <tt>imgMaxSize</tt> if succeed. If the configuration 
    * property isn't available or the value can't be parsed correctly 
    * the value of <tt>imgMaxSize</tt> isn't changed. 
    */
    private void setMaxImgSizeFromConf()
    {
        ConfigurationService configService =
            DirectImageActivator.getConfigService();

        if(configService != null)
        {
            String confImgSizeStr = 
                (String) configService.getProperty(MAX_IMG_SIZE);
            try
            {
                if (confImgSizeStr != null)
                {
                    imgMaxSize = Long.parseLong(confImgSizeStr);
                }
                else
                {
                    configService.setProperty(MAX_IMG_SIZE, imgMaxSize);
                }
            }
            catch (NumberFormatException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to parse max image size: "
                        + confImgSizeStr + ". Going for default.");
            }
        }
    }

    /**
     * Returns the thumbnail URL of the image link provided.
     *
     * @param sourceString the original image link.
     * @return the thumbnail image link; the original link in case of no match.
     */
    public String getReplacement(String sourceString)
    {
        return sourceString;
    }

    /**
     * Returns the source name
     *
     * @return the source name
     */
    public String getSourceName()
    {
        return SOURCE_NAME;
    }

    /**
     * Returns the pattern of the source
     *
     * @return the source pattern
     */
    public String getPattern()
    {
        return URL_PATTERN;
    }

    @Override
    /**
     * Returns the size of the image in bytes.
     * @param sourceString the image link.
     * @return the file size in bytes of the image link provided; -1 if the size
     * isn't available or exceeds the max allowed image size.
     */
    public int getImageSize(String sourceString)
    {
        int length = -1;
        try
        {
            
            URL url = new URL(sourceString);
            String protocol = url.getProtocol();
            if (protocol.equals("http") || protocol.equals("https"))
            {
                HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();
                length = connection.getContentLength();
                connection.disconnect();
            }
            else if (protocol.equals("ftp"))
            {
                FTPUtils ftp = new FTPUtils(sourceString);
                length = ftp.getSize();
                ftp.disconnect();
            }
            
            if (length > imgMaxSize)
            {
                length = -1;
            }
        }
        catch (Exception e)
        {
            logger.debug("Failed to get the length of the image in bytes", e);
        }
        return length;
    }

    /**
     * Returns true if the content type of the resource
     * pointed by sourceString is an image.
     * @param sourceString the original image link.
     * @return true if the content type of the resource
     * pointed by sourceString is an image.
     */
    @Override
    public boolean isDirectImage(String sourceString)
    {
        boolean isDirectImage = false;
        try
        {
            URL url = new URL(sourceString);
            String protocol = url.getProtocol();
            if (protocol.equals("http") || protocol.equals("https"))
            {
                HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();
                isDirectImage = connection.getContentType().contains("image");
                connection.disconnect();
            }
            else if (protocol.equals("ftp"))
            {
                if (sourceString.endsWith(".png")
                    || sourceString.endsWith(".jpg")
                    || sourceString.endsWith(".gif"))
                {
                    isDirectImage = true;
                }
            }
        }
        catch (Exception e)
        {
            logger.debug("Failed to retrieve content type information for"
                + sourceString, e);
        }
        return isDirectImage;
    }
}
