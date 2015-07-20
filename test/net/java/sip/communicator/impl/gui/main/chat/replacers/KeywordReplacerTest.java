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
package net.java.sip.communicator.impl.gui.main.chat.replacers;

import junit.framework.*;

/**
 * Tests for keyword replacer.
 *
 * @author Danny van Heumen
 */
public class KeywordReplacerTest
    extends TestCase
{
    public void testConstructWithoutKeyword()
    {
        new KeywordReplacer(null);
    }

    public void testConstructEmptyKeyword()
    {
        new KeywordReplacer("");
    }

    public void testConstructWithKeyword()
    {
        new KeywordReplacer("keyword");
    }

    public void testExpectPlainText()
    {
        KeywordReplacer replacer = new KeywordReplacer("test");
        Assert.assertTrue(replacer.expectsPlainText());
    }

    public void testNullKeywordReplacement()
    {
        KeywordReplacer replacer = new KeywordReplacer(null);
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "this is a piece of text");
        Assert.assertEquals("this is a piece of text", target.toString());
    }

    public void testEmptyKeywordReplacement()
    {
        KeywordReplacer replacer = new KeywordReplacer("");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "this is a piece of text");
        Assert.assertEquals("this is a piece of text", target.toString());
    }

    public void testTrivialKeywordReplace()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "word");
        Assert.assertEquals("<b>word</b>", target.toString());
    }

    public void testKeywordTooSmallForReplacement()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "wor");
        Assert.assertEquals("wor", target.toString());
    }

    public void testKeywordInSentence()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "some word in a sentence");
        Assert.assertEquals("some <b>word</b> in a sentence", target.toString());
    }

    public void testKeywordAtSentenceStart()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "word first in a sentence");
        Assert.assertEquals("<b>word</b> first in a sentence", target.toString());
    }

    public void testKeywordAtSentenceEnd()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "last in a sentence word");
        Assert.assertEquals("last in a sentence <b>word</b>", target.toString());
    }

    public void testKeywordInSentenceMultipleHits()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "1 word 2 word 3 word 4");
        Assert.assertEquals("1 <b>word</b> 2 <b>word</b> 3 <b>word</b> 4", target.toString());
    }

    public void testDontReplaceKeywordInsideWord()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "A sentence containing keywords.");
        Assert.assertEquals("A sentence containing keywords.", target.toString());
    }

    public void testDontReplaceKeywordHeadingWord()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "I am the wordsmith.");
        Assert.assertEquals("I am the wordsmith.", target.toString());
    }

    public void testDontReplaceKeywordTrailingWord()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "Don't find the word in keyword.");
        Assert.assertEquals("Don't find the <b>word</b> in keyword.", target.toString());
    }

    public void testReplaceKeywordAllowPunctuation()
    {
        KeywordReplacer replacer = new KeywordReplacer("word");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "Find the hidden word, word. (word) between parentheses.");
        Assert.assertEquals("Find the hidden <b>word</b>, <b>word</b>. (<b>word</b>) between parentheses.", target.toString());
    }

    public void testReplaceKeywordAllowPunctuation2()
    {
        KeywordReplacer replacer = new KeywordReplacer("fo");
        StringBuilder target = new StringBuilder();
        replacer.replace(target, "fo: Whenever someone writes \"for\" or any other word that starts with \"fo\" it recognizes it as my nickname ...");
        Assert.assertEquals("<b>fo</b>: Whenever someone writes &quot;for&quot; or any other word that starts with &quot;<b>fo</b>&quot; it recognizes it as my nickname ...", target.toString());
    }
}
