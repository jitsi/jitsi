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
     * stack with formatting control chars
     */
    private final Stack<ControlChar> formatting = new Stack<ControlChar>();

    /**
     * formatted text container
     */
    private final StringBuilder text = new StringBuilder();
    
    /**
     * Append a string of text.
     * 
     * @param text
     */
    public void append(String text)
    {
        this.text.append(text);
    }
    
    /**
     * Append a character.
     * 
     * @param c character
     */
    public void append(char c)
    {
        this.text.append(c);
    }

    /**
     * Apply a control char for formatting.
     * 
     * TODO Explicitly deny handling ControlChar.NORMAL?
     * 
     * @param c the control char
     */
    public void apply(ControlChar c)
    {
        // start control char formatting
        this.formatting.add(c);
        this.text.append(c.getHtmlStart());
    }
    
    /**
     * Test whether or not a control character is already active.
     * 
     * @param c the control char
     * @return returns true if control char's kind of formatting is active, or
     *         false otherwise.
     */
    public boolean isActive(Class<? extends ControlChar> controlClass)
    {
        for (ControlChar c : this.formatting)
        {
            if (c.getClass() == controlClass)
                return true;
        }
        return false;
    }
    
    /**
     * Cancel the specified control char.
     * 
     * @param c the control char
     */
    public void cancel(Class<? extends ControlChar> controlClass,
        boolean stopAfterFirst)
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
                    break;
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
     */
    public String done()
    {
        cancelAll();
        return this.text.toString();
    }

    /**
     * Return the formatted string in its current state. (This means that if
     * {@link #done()} was not yet called, it will print an intermediate state of
     * the formatted text.)
     */
    public String toString()
    {
        return this.text.toString();
    }
}
