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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent;

import java.util.*;

import javax.xml.namespace.*;

import org.w3c.dom.*;

/**
 * The XCAP content element.
 * <p/>
 * Compliant with Presence Content XDM Specification v1.0
 *
 * @author Grigorii Balutsel
 */
public class ContentType
{
    /**
     * The data element.
     */
    private DataType data;

    /**
     * The mime-type element.
     */
    private MimeType mimeType;

    /**
     * The encoding element.
     */
    private EncodingType encoding;

    /**
     * The list of description elements.
     */
    private List<DescriptionType> description;

    /**
     * The list of any elements.
     */
    private List<Element> any;

    /**
     * The map of any attributes.
     */
    private Map<QName, String> anyAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the mimeType property.
     *
     * @return the mimeType property.
     */
    public MimeType getMimeType()
    {
        return mimeType;
    }

    /**
     * Sets the value of the mimeType property.
     *
     * @param mimeType the mimeType to set.
     */
    public void setMimeType(MimeType mimeType)
    {
        this.mimeType = mimeType;
    }


    /**
     * Gets the value of the encoding property.
     *
     * @return the encoding property.
     */
    public EncodingType getEncoding()
    {
        return encoding;
    }

    /**
     * Sets the value of the encoding property.
     *
     * @param encoding the encoding to set.
     */
    public void setEncoding(EncodingType encoding)
    {
        this.encoding = encoding;
    }


    /**
     * Gets the value of the description property.
     *
     * @return the description property.
     */
    public List<DescriptionType> getDescription()
    {
        if (description == null)
        {
            description = new ArrayList<DescriptionType>();
        }
        return this.description;
    }


    /**
     * Gets the value of the data property.
     *
     * @return the data property.
     */
    public DataType getData()
    {
        return data;
    }

    /**
     * Sets the value of the data property.
     *
     * @param data the data to set.
     */
    public void setData(DataType data)
    {
        this.data = data;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<Element>();
        }
        return this.any;
    }

    /**
     * Sets the value of the any property.
     *
     * @param any the any to set.
     */
    public void setAny(List<Element> any)
    {
        this.any = any;
    }

    /**
     * Gets the value of the anyAttributes property.
     *
     * @return the anyAttributes property.
     */
    public Map<QName, String> getAnyAttributes()
    {
        return anyAttributes;
    }

    /**
     * Sets the value of the anyAttributes property.
     *
     * @param anyAttributes the anyAttributes to set.
     */
    public void setAnyAttributes(Map<QName, String> anyAttributes)
    {
        this.anyAttributes = anyAttributes;
    }
}
