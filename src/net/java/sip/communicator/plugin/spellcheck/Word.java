/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.spellcheck;

import java.text.BreakIterator;

/**
 * Immutable representation of a word in the context of a document, bundling
 * the bounds with the text.
 * @author Damian Johnson
 */
class Word
{
    private static final BreakIterator WORD_ITR
        = BreakIterator.getWordInstance();
    private final int start;
    private final String text;

    /**
     * Provides the word before or after a given index. No bounds checking is
     * performed.
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
            if (start == BreakIterator.DONE) start = 0;
        }
        else
        {
            end = WORD_ITR.following(index);
            start = WORD_ITR.previous();
            if (end == BreakIterator.DONE) end = text.length() - 1;
        }

        return new Word(start, text.substring(start, end));
    }

    private Word(int start, String text)
    {
        this.start = start;
        this.text = text;
    }

    public int getStart()
    {
        return this.start;
    }

    public String getText()
    {
        return this.text;
    }

    public String toString()
    {
        return this.text;
    }
}