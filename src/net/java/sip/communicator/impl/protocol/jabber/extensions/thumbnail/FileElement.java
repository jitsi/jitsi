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

import java.text.*;
import java.util.*;

import org.jitsi.util.Logger;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.packet.StreamInitiation.File;
import org.jivesoftware.smackx.provider.*;
import org.xmlpull.v1.*;

/**
 * The <tt>FileElement</tt> extends the smackx <tt>StreamInitiation.File</tt>
 * in order to provide a file that supports thumbnails.
 *
 * @author Yana Stamcheva
 */
public class FileElement
    extends File
    implements IQProvider
{
    private static final Logger logger = Logger.getLogger(FileElement.class);

    private static final List<DateFormat> DATE_FORMATS
        = new ArrayList<DateFormat>();

    /**
     * The element name of this <tt>IQProvider</tt>.
     */
    public static final String ELEMENT_NAME = "si";

    /**
     * The namespace of this <tt>IQProvider</tt>.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/si";

    static
    {
        // DATE_FORMATS
        DateFormat fmt;

        // XEP-0091
        DATE_FORMATS.add(DelayInformation.XEP_0091_UTC_FORMAT);
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

    private ThumbnailElement thumbnail;

    /**
     * An empty constructor used to initialize this class as an
     * <tt>IQProvider</tt>.
     */
    public FileElement()
    {
        this("", 0);
    }

    /**
     * Creates a <tt>FileElement</tt> by specifying a base file and a thumbnail
     * to extend it with.
     *
     * @param baseFile the file used as a base
     * @param thumbnail the thumbnail to add
     */
    public FileElement(File baseFile, ThumbnailElement thumbnail)
    {
        this(baseFile.getName(), baseFile.getSize());

        this.thumbnail = thumbnail;
    }

    /**
     * Creates a <tt>FileElement</tt> by specifying the name and the size of the
     * file.
     *
     * @param name the name of the file
     * @param size the size of the file
     */
    public FileElement(String name, long size)
    {
        super(name, size);
    }

    /**
     * Represents this <tt>FileElement</tt> in an XML.
     *
     * @see File#toXML()
     */
    @Override
    public String toXML()
    {
        StringBuilder buffer = new StringBuilder();

        buffer.append("<").append(getElementName()).append(" xmlns=\"")
            .append(getNamespace()).append("\" ");

        if (getName() != null)
        {
            buffer.append("name=\"").append(
                StringUtils.escapeForXML(getName())).append("\" ");
        }

        if (getSize() > 0)
        {
            buffer.append("size=\"").append(getSize()).append("\" ");
        }

        if (getDate() != null)
        {
            buffer.append("date=\"").append(
                StringUtils.formatXEP0082Date(this.getDate())).append("\" ");
        }

        if (getHash() != null)
        {
            buffer.append("hash=\"").append(getHash()).append("\" ");
        }

        if ((this.getDesc() != null && getDesc().length() > 0)
                || isRanged()
                || thumbnail != null)
        {
            buffer.append(">");

            if (getDesc() != null && getDesc().length() > 0)
            {
                buffer.append("<desc>").append(
                    StringUtils.escapeForXML(getDesc())).append("</desc>");
            }

            if (isRanged())
            {
                buffer.append("<range/>");
            }

            if (thumbnail != null)
            {
                buffer.append(thumbnail.toXML());
            }

            buffer.append("</").append(getElementName()).append(">");
        }
        else
        {
            buffer.append("/>");
        }

        return buffer.toString();
    }

    /**
     * Returns the <tt>ThumbnailElement</tt> contained in this
     * <tt>FileElement</tt>.
     * @return the <tt>ThumbnailElement</tt> contained in this
     * <tt>FileElement</tt>
     */
    public ThumbnailElement getThumbnailElement()
    {
        return thumbnail;
    }

    /**
     * Sets the given <tt>thumbnail</tt> to this <tt>FileElement</tt>.
     * @param thumbnail the <tt>ThumbnailElement</tt> to set
     */
    public void setThumbnailElement(ThumbnailElement thumbnail)
    {
        this.thumbnail = thumbnail;
    }

    /**
     * Parses the given <tt>parser</tt> in order to create a
     * <tt>FileElement</tt> from it.
     * @param parser the parser to parse
     * @see IQProvider#parseIQ(XmlPullParser)
     */
    public IQ parseIQ(final XmlPullParser parser)
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
                    form = (DataForm) dataFormProvider.parseExtension(parser);
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

        initiation.setSesssionID(id);
        initiation.setMimeType(mimeType);
        initiation.setFeatureNegotiationForm(form);

        return initiation;
    }
}
