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
package net.java.sip.communicator.plugin.spellcheck;

import java.text.*;

/**
 * Immutable representation of a word in the context of a document, bundling the
 * bounds with the text.
 *
 * @author Damian Johnson
 */
class Word
{
    private static final BreakIterator WORD_ITR = BreakIterator
        .getWordInstance();

    private final int start;

    private final String text;

    private final int end;

    /**
     * Provides the word before or after a given index. No bounds checking is
     * performed.
     *
     * @param text text to be checked
     * @param index index in which to begin search (inclusive)
     * @param before search is before index if true, after otherwise
     * @return index of word boundary
     */
    public static synchronized Word getWord(String text, int index,
        boolean before)
    {
        int start, end;
        WORD_ITR.setText(text);

        if (before)
        {
            start = WORD_ITR.preceding(index);
            end = WORD_ITR.next();
            if (start == BreakIterator.DONE)
                start = 0;
        }
        else
        {
            end = WORD_ITR.following(index);
            start = WORD_ITR.previous();
            if (end == BreakIterator.DONE)
                end = text.length() - 1;
        }

        return new Word(start, end, text.substring(start, end));
    }

    private Word(int start, int end, String text)
    {
        this.start = start;
        this.end = end;
        this.text = text;
    }

    public int getStart()
    {
        return this.start;
    }

    public int getEnd()
    {
        return this.end;
    }

    public String getText()
    {
        return this.text;
    }

    @Override
    public String toString()
    {
        return this.text;
    }
}
