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

import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.xdata.FormField;
import org.xmlpull.v1.*;

/**
 * Internal Search service Provider. It parses the <tt>UserSeachIQ</tt> packets.
 */
public class UserSearchProvider extends IQProvider<UserSearchIQ>
{
    @Override
    public UserSearchIQ parse(XmlPullParser parser, int depth) throws Exception
    {
        UserSearchIQ search = new UserSearchIQ();
        boolean done = false;

        while (!done)
        {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG
                && parser.getName().equals("item"))
            {
                search.setData(parseItems(parser));
                return search;
            }
            else if (eventType == XmlPullParser.START_TAG
                && parser.getName().equals("instructions"))
            {
                continue;
            }
            else if (eventType == XmlPullParser.START_TAG
                && !parser.getNamespace().equals("jabber:x:data"))
            {
                search.addField(parser.getName(), null);

            }
            else if (eventType == XmlPullParser.START_TAG
                && parser.getNamespace().equals("jabber:x:data"))
            {
                // Otherwise, it must be a packet extension.
                search.addExtension(PacketParserUtils.parsePacketExtension(
                    parser.getName(), parser.getNamespace(), parser));

            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals("query"))
                    done = true;
            }
        }

        return search;
    }

    /**
     * Parses the items from the result packet.
     * @param parser the parser.
     * @return <tt>ReportedData</tt> instance with the search results.
     * @throws Exception if parser error occurred.
     */
    protected ReportedData parseItems(XmlPullParser parser) throws Exception
    {
        ReportedData data = new ReportedData();
        data.addColumn(
            new ReportedData.Column("JID", "jid", FormField.Type.jid_single));

        boolean done = false;

        List<ReportedData.Field> fields = new ArrayList<>();
        while (!done)
        {
            if (parser.getAttributeCount() > 0)
            {
                String jid = parser.getAttributeValue("", "jid");
                List<String> valueList = new ArrayList<>();
                valueList.add(jid);
                ReportedData.Field field = new ReportedData.Field("jid", valueList);
                fields.add(field);
            }

            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG
                && parser.getName().equals("item"))
            {
                fields = new ArrayList<>();
            }
            else if (eventType == XmlPullParser.END_TAG
                && parser.getName().equals("item"))
            {
                ReportedData.Row row = new ReportedData.Row(fields);
                data.addRow(row);
            }
            else if (eventType == XmlPullParser.START_TAG)
            {
                String name = parser.getName();
                String value = parser.nextText();

                List<String> valueList = new ArrayList<>();
                valueList.add(value);
                ReportedData.Field field
                    = new ReportedData.Field(name, valueList);
                fields.add(field);

                boolean exists = false;
                for (ReportedData.Column column : data.getColumns())
                {
                    if (column.getVariable().equals(name))
                    {
                        exists = true;
                        break;
                    }
                }

                // Column name should be the same
                if (!exists)
                {
                    ReportedData.Column column = new ReportedData.Column(
                        name, name, FormField.Type.text_single);
                    data.addColumn(column);
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals("query"))
                    done = true;
            }

        }

        return data;
    }
}
