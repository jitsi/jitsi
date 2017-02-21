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
package net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail;

import org.jitsi.util.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.si.packet.*;
import org.jivesoftware.smackx.xdata.packet.*;
import org.jivesoftware.smackx.xdata.provider.*;
import org.xmlpull.v1.*;

import java.text.*;
import java.util.*;

public class FileElementIQProvider extends IQProvider
{
    private static final Logger logger
        = Logger.getLogger(FileElementIQProvider.class);

    private static final List<DateFormat> DATE_FORMATS
        = new ArrayList<>();

    static
    {
        // DATE_FORMATS
        DateFormat fmt;

        // XEP-0091
        fmt = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        DATE_FORMATS.add(fmt);
        fmt = new SimpleDateFormat("yyyyMd'T'HH:mm:ss'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        DATE_FORMATS.add(fmt);

        // XEP-0082
        fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        DATE_FORMATS.add(fmt);
        fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        DATE_FORMATS.add(fmt);
        DATE_FORMATS.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
        DATE_FORMATS.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
    }

    /**
     * Parses the given <tt>parser</tt> in order to create a
     * <tt>FileElement</tt> from it.
     * @param parser the parser to parse
     */
    @Override
    public IQ parse(final XmlPullParser parser, int depth)
        throws Exception
    {
        boolean done = false;

        // si
        String id = parser.getAttributeValue("", "id");
        String mimeType = parser.getAttributeValue("", "mime-type");
        StreamInitiation initiation = new StreamInitiation();

        // file
        String name = null;
        String size = null;
        String hash = null;
        String date = null;
        String desc = null;
        ThumbnailElement thumbnail = null;
        boolean isRanged = false;

        // feature
        DataForm form = null;
        DataFormProvider dataFormProvider = new DataFormProvider();

        int eventType;
        String elementName;
        String namespace;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.START_TAG)
            {
                if (elementName.equals("file"))
                {
                    name = parser.getAttributeValue("", "name");
                    size = parser.getAttributeValue("", "size");
                    hash = parser.getAttributeValue("", "hash");
                    date = parser.getAttributeValue("", "date");
                }
                else if (elementName.equals("desc"))
                {
                    desc = parser.nextText();
                }
                else if (elementName.equals("range"))
                {
                    isRanged = true;
                }
                else if (elementName.equals("x")
                    && namespace.equals("jabber:x:data"))
                {
                    form = (DataForm) dataFormProvider.parse(parser);
                }
                else if (elementName.equals("thumbnail"))
                {
                    thumbnail = new ThumbnailElement(parser.getText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (elementName.equals("si"))
                {
                    done = true;
                }
                // The name-attribute is required per XEP-0096, so ignore the
                // IQ if the name is not set to avoid exceptions. Particularly,
                // the SI response of Empathy contains an invalid, empty
                // file-tag.
                else if (elementName.equals("file") && name != null)
                {
                    long fileSize = 0;

                    if(size != null && size.trim().length() !=0)
                    {
                        try
                        {
                            fileSize = Long.parseLong(size);
                        }
                        catch (NumberFormatException e)
                        {
                            logger.warn("Received an invalid file size,"
                                + " continuing with fileSize set to 0", e);
                        }
                    }

                    FileElement file = new FileElement(name, fileSize);
                    file.setHash(hash);

                    if (date != null)
                    {
                        // try all known date formats
                        boolean found = false;
                        if (date.matches(
                            ".*?T\\d+:\\d+:\\d+(\\.\\d+)?(\\+|-)\\d+:\\d+"))
                        {
                            int timeZoneColon = date.lastIndexOf(":");
                            date = date.substring(0, timeZoneColon)
                                + date.substring(
                                timeZoneColon+1, date.length());
                        }
                        for (DateFormat fmt : DATE_FORMATS)
                        {
                            try
                            {
                                file.setDate(fmt.parse(date));
                                found = true;
                                break;
                            }
                            catch (ParseException ex)
                            {
                            }
                        }

                        if (!found)
                        {
                            logger.warn(
                                "Unknown dateformat on incoming file transfer: "
                                    + date);
                        }
                    }

                    if (thumbnail != null)
                        file.setThumbnailElement(thumbnail);

                    file.setDesc(desc);
                    file.setRanged(isRanged);
                    initiation.setFile(file);
                }
            }
        }

        initiation.setSessionID(id);
        initiation.setMimeType(mimeType);
        initiation.setFeatureNegotiationForm(form);

        return initiation;
    }
}
