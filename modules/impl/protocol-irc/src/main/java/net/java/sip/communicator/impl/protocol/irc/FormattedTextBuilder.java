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

import java.util.*;

/**
 * Builder for constructing a formatted text.
 *
 * @author Danny van Heumen
 */
public class FormattedTextBuilder
{
    /**
     * stack with formatting control chars.
     */
    private final Stack<ControlChar> formatting = new Stack<ControlChar>();

    /**
     * formatted text container.
     */
    private final StringBuilder text = new StringBuilder();

    /**
     * Append a string of text.
     *
     * Make sure that the text is safe for your purposes, as it is appended
     * without further modifications.
     *
     * @param text string of text
     */
    public void append(final String text)
    {
        this.text.append(text);
    }

    /**
     * Append a character.
     *
     * Make sure that the character is safe for your purposes, as it is appended
     * without further modifications.
     *
     * @param c character
     */
    public void append(final char c)
    {
        this.text.append(c);
    }

    /**
     * Apply a control char for formatting.
     *
     * @param c the control char
     */
    public void apply(final ControlChar c)
    {
        // start control char formatting
        this.formatting.add(c);
        this.text.append(c.getHtmlStart());
    }

    /**
     * Test whether or not a control character is already active.
     *
     * @param controlClass the class of control char
     * @return returns true if control char's class of formatting is active, or
     *         false otherwise.
     */
    public boolean isActive(final Class<? extends ControlChar> controlClass)
    {
        for (ControlChar c : this.formatting)
        {
            if (c.getClass() == controlClass)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancel the active control char of the specified class.
     *
     * @param controlClass the class of control char
     * @param stopAfterFirst stop after the first occurrence
     */
    public void cancel(final Class<? extends ControlChar> controlClass,
        final boolean stopAfterFirst)
    {
        final Stack<ControlChar> rewind = new Stack<ControlChar>();
        while (!this.formatting.empty())
        {
            // unwind control chars looking for the cancelled control char
            ControlChar current = this.formatting.pop();
            this.text.append(current.getHtmlEnd());
            if (current.getClass() == controlClass)
            {
                if (stopAfterFirst)
                {
                    break;
                }
            }
            else
            {
                rewind.push(current);
            }
        }
        while (!rewind.empty())
        {
            // reapply remaining control characters
            ControlChar current = rewind.pop();
            apply(current);
        }
    }

    /**
     * Cancel all active formatting.
     */
    public void cancelAll()
    {
        while (!this.formatting.empty())
        {
            ControlChar c = this.formatting.pop();
            this.text.append(c.getHtmlEnd());
        }
    }

    /**
     * Finish building the text string. Close outstanding control char
     * formatting and returns the result.
     *
     * @return returns the complete string as it is built
     */
    public String done()
    {
        cancelAll();
        return this.text.toString();
    }

    /**
     * Return the formatted string in its current state. (This means that if
     * {@link #done()} was not yet called, it will print an intermediate state
     * of the formatted text.)
     *
     * @return returns current state of the formatted text
     */
    public String toString()
    {
        return this.text.toString();
    }
}
