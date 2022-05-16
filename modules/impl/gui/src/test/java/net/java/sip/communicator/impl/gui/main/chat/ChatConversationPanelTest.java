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
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import junit.framework.*;

/**
 * Tests for functionality of the ChatConversationPanel.
 *
 * @author Danny van Heumen
 */
public class ChatConversationPanelTest
    extends TestCase
{

    /**
     * Test for various better and worse pieces of HTML to test the resilience
     * of the pattern.
     */
    public void testHtmlSnippetsOnTextToReplacePattern()
    {
        final HashMap<String, String[]> tests = new HashMap<String, String[]>();
        tests.put("just a piece of text",
            new String[] {"just a piece of text", ""});
        tests.put(">another piece of text",
            new String[] {">another piece of text", ""});
        tests.put("<another piece of text",
            new String[] {"", ""});
        tests.put("<another piece> of text",
            new String[] {"", " of text", ""});
        tests.put("<another attribute=\"piece\"> of text",
            new String[] {"", " of text", ""});
        tests.put("<another attribute=\"<\"> of text",
            new String[] {"", " of text", ""});
        tests.put("piece of text<tag>'nother piece</tag>stuff at the end",
            new String[]
            {"piece of text", "'nother piece", "stuff at the end", "" });
        tests.put("<br />", new String[] {"", ""});
        tests.put("<br />text", new String[] {"", "text", ""});
        tests.put("some<br />text", new String[] {"some", "text", ""});
        tests.put("<img src=\"blablabla.jpg\" />",
            new String[] {"", ""});
        tests.put("some<img src=\"blablabla.jpg\" />",
            new String[] {"some", ""});
        tests.put("some<img src=\"blablabla.jpg\" />foobar",
            new String[] {"some", "foobar", ""});
        tests.put(">some text between cut-off tags<",
            new String[] {">some text between cut-off tags", ""});
        tests.put("<some text between pointy brackets>",
            new String[] {"", ""});
        tests.put("fake &lt;br/&gt; tag",
            new String[] {"fake &lt;br/&gt; tag", ""});
        tests.put("fake &lt;br/> tag",
            new String[] {"fake &lt;br/> tag", ""});
        tests.put("fake <br/&gt; tag",
            new String[] {"fake ", ""});
        tests.put("a piece <b>of <u>formatted</u> text for </b>testing...",
            new String[] {"a piece ", "of ", "formatted", " text for ",
                "testing...", ""});
        tests.put("a piece <a href=\"www.google.com?query=blabla#blabla\">"
            + "www.google.com</a> hyperlinked text",
            new String[] {"a piece ", "www.google.com", " hyperlinked text",
                ""});
        tests.put("<another attribute=\">\"> of text",
            new String[] {"", " of text", ""});
        tests.put("<a name=\"Click here ><\" href=\"www.google.com\">"
            + "For a treat</a> or something ...",
            new String[] {"", "For a treat", " or something ...", ""});
        tests.put("and here is <a \"some weird syntax\"> to test",
            new String[] {"and here is ", " to test", ""});
        tests.put("and here <option name=\"opt\" checked> checked option",
            new String[] {"and here ", " checked option", ""});
        tests.put("incomplete <img href=\"www.goo",
            new String[] {"incomplete ", ""});
        tests.put("incomplete <img href=\"www.goo     >  <a href=\">test",
            new String[] {"incomplete ", "test", ""});
        tests.put("\"blablabla\">See if this text is ignored ...",
            new String[] {"\"blablabla\">See if this text is ignored ...", ""});
        tests.put("bla\">See if this<img src=\"test1\">test2</img>",
            new String[] {"bla\">See if this", "test2", ""});
        tests.put("<the-end", new String[] {"", ""});
        tests.put("&lt;this-is-not-a-tag>",
            new String[] {"&lt;this-is-not-a-tag>", ""});
        tests.put("<this-is-a-tag>", new String[] {"", ""});

        for (final Entry<String, String[]> entry : tests.entrySet())
        {
            final String input = entry.getKey();
            int index = 0;
            final Matcher matcher =
                ChatConversationPanel.TEXT_TO_REPLACE_PATTERN.matcher(input);
            while (matcher.find())
            {
                final String piece = matcher.group(1);
                Assert.assertEquals("INPUT [[" + input + "]]:",
                    entry.getValue()[index], piece);
                index++;
            }
            // ensure that we have checked all predicted pieces
            Assert.assertEquals(entry.getValue().length, index);
        }
    }
}
