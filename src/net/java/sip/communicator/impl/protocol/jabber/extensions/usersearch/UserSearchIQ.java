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
package net.java.sip.communicator.impl.protocol.jabber.extensions.usersearch;

import java.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jivesoftware.smackx.search.*;
import org.jivesoftware.smackx.xdata.*;
import org.jivesoftware.smackx.xdata.packet.*;

/**
 * Implements the <tt>IQ</tt> packets for user search (XEP-0055)
 *
 * @author Hristo Terezov
 */
public class UserSearchIQ extends IQ
{
    /**
     * This field represents the result of the search.
     */
    private ReportedData data;

    /**
     * This map stores the supported fields that are not defined in the data
     * form and their values.
     */
    private Map<String, String> simpleFieldsNames
        = new HashMap<String, String>();

    public UserSearchIQ()
    {
        super("query", "jabber:iq:search");
    }

    @Override
    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder buf)
    {
        if(getExtension("x", "jabber:x:data") == null)
        {
            buf.rightAngleBracket();
            for (Map.Entry<String, String> entry : simpleFieldsNames.entrySet())
            {
                buf.optElement(entry.getKey(), entry.getValue());
            }
        }
        else
        {
            buf.setEmptyElement();
        }

        return buf;
    }

    /**
     * Sets the <tt>data</tt> property of the class.
     * @param data the data to be set.
     */
    public void setData(ReportedData data)
    {
        this.data = data;
    }

    /**
     * Returns the <tt>data</tt> property of the class.
     * @return
     */
    public ReportedData getData()
    {
        ReportedData data = ReportedData.getReportedDataFrom(this);
        if(data == null)
            return this.data;
        return data;
    }

    /**
     * Adds filter field to the <tt>IQ</tt> packet and value for the field.
     * @param field the field name.
     * @param value the value of the field.
     */
    public void addField(String field, String value)
    {
        simpleFieldsNames.put(field, value);
    }

    /**
     * Returns the names of the fields that are not included in the data form.
     * @return the field names.
     */
    public Set<String> getFields()
    {
        return simpleFieldsNames.keySet();
    }

    /**
     * Creates and returns answer form.
     * @return the answer form.
     */
    public Form getAnswerForm()
    {
        Form form = Form.getFormFrom(this);
        if(form == null)
            return null;
        return form.createAnswerForm();
    }

    /**
     * Sets data form in the <tt>IQ</tt> packet.
     * @param form the form to be set.
     */
    public void setForm(DataForm form)
    {
        addExtension(form);
    }

}
