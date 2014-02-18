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
     * Apply a control char for formatting.
     * 
     * @param c the control char
     */
    public void apply(ControlChar c)
    {
        if (formatting.contains(c))
        {
            // cancel active control char
            cancel(c);
        }
        else
        {
            // start control char formatting
            this.text.append(c.getHtmlStart());
        }
    }
    
    /**
     * Cancel the specified control char.
     * 
     * @param c the control char
     */
    private void cancel(ControlChar c)
    {
        final Stack<ControlChar> unwind = new Stack<ControlChar>();
        while (!this.formatting.empty())
        {
            // unwind control chars looking for the cancelled control char
            ControlChar current = this.formatting.pop();
            this.text.append(current.getHtmlEnd());
            if (current == c)
            {
                break;
            }
            else
            {
                unwind.push(current);
            }
        }
        while (!unwind.empty())
        {
            // rewind remaining control characters
            ControlChar current = unwind.pop();
            this.text.append(current.getHtmlStart());
            this.formatting.push(current);
        }
    }

    /**
     * Cancel all remaining control chars.
     */
    private void cancelAll()
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
     * Return the formatted string. If it is not yet finished (outstanding
     * formatting) also finish up remaining control chars.
     */
    public String toString()
    {
        return done();
    }
}
