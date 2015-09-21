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
package net.java.sip.communicator.impl.protocol.irc;

import junit.framework.*;

public class FormattedTextBuilderTest
    extends TestCase
{

    public void testConstructFormattedTextBuilder()
    {
        new FormattedTextBuilder();
    }
    
    public void testFormatNothing()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        Assert.assertEquals("", formatted.done());
    }
    
    public void testPlainText()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.append("Hello world!");
        Assert.assertEquals("Hello world!", formatted.done());
    }
    
    public void testPlainChar()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.append('H');
        formatted.append('e');
        formatted.append('l');
        formatted.append('l');
        formatted.append('o');
        Assert.assertEquals("Hello", formatted.done());
    }
    
    public void testDoneWithoutFormatting()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.append("Hello world!");
        Assert.assertEquals("Hello world!", formatted.done());
    }
    
    public void testDoneRepeatedly()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.append("Hello world!");
        formatted.done();
        formatted.done();
        Assert.assertEquals("Hello world!", formatted.done());
    }
    
    public void testOnlyFormatting()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.apply(new ControlChar.Bold());
        Assert.assertEquals("<b></b>", formatted.done());
    }
    
    public void testMixedFormattingContent()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.apply(new ControlChar.Bold());
        formatted.append("Hello ");
        formatted.apply(new ControlChar.Italics());
        formatted.append("world");
        formatted.cancel(ControlChar.Bold.class, true);
        formatted.append("!!!");
        Assert.assertEquals("<b>Hello <i>world</i></b><i>!!!</i>",
            formatted.done());
    }
    
    public void testToStringIntermediateResult()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        formatted.apply(new ControlChar.Bold());
        formatted.append("Hello ");
        formatted.apply(new ControlChar.Italics());
        Assert.assertEquals("<b>Hello <i>", formatted.toString());
        formatted.append("world");
        formatted.cancel(ControlChar.Bold.class, true);
        formatted.append("!!!");
        Assert.assertEquals("<b>Hello <i>world</i></b><i>!!!",
            formatted.toString());
        Assert.assertEquals("<b>Hello <i>world</i></b><i>!!!</i>",
            formatted.done());
        Assert.assertEquals("<b>Hello <i>world</i></b><i>!!!</i>",
            formatted.toString());
    }
    
    public void testActiveFormatting()
    {
        FormattedTextBuilder formatted = new FormattedTextBuilder();
        Assert.assertFalse(formatted.isActive(ControlChar.Bold.class));
        formatted.apply(new ControlChar.Bold());
        Assert.assertTrue(formatted.isActive(ControlChar.Bold.class));
        formatted.append("Hello ");
        Assert.assertFalse(formatted.isActive(ControlChar.Italics.class));
        formatted.apply(new ControlChar.Italics());
        Assert.assertTrue(formatted.isActive(ControlChar.Italics.class));
        formatted.done();
        Assert.assertFalse(formatted.isActive(ControlChar.Bold.class));
        Assert.assertFalse(formatted.isActive(ControlChar.Italics.class));
    }
}
