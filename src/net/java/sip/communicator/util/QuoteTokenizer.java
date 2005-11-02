/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A tokenizer that takes in account the existence of " and ' and
 * does skips all delimeters enclosed in those characters.
 * 
 * Thus the following string:
 *  
 * <code>This is a "stupid 'hot' dog", boy!</code>
 *  
 * Parsed created with the default constructor will produce the
 * following tokens:
 * 
 * This is a
 * "stupid 'hot' dog",
 * boy! 
 * 
 * And the string:
 * <code>This is a 'smart "hot" dog', boy!</code>
 * 
 * This is a
 * 'stupid "hot" dog',
 * boy!
 * 
 * @author Alexander Pelov
 * 
 * TODO: TEST
 */
public class QuoteTokenizer implements Enumeration {
    
    /**
     * Delimeters defined for internal purposes.
     */
    private static final String DELIMS = "'\"";

    /**
     * The text to be tokenized.
     */
    private String text;
    
    /**
     * Delimeters requested by the user.
     */
    private String delims;
    
    /**
     * The number of tokens in the text.
     */
    private int tokens = -1;

    /**
     * Pointer to the current tokenizer position.
     */
    private int currentTokenEnd = -1;

    /**
     * Return the delimeters?
     */
    private boolean returnDelim;

    /**
     * Constructs a tokenizer using spaces and tabs as delimeters,
     * not returning the delimeters as tokens.
     * 
     * @param text The text to be tokenized
     */
    public QuoteTokenizer(String text) {
        this(text, " \t");
    }

    /**
     * Constructs a tokenizer using the characters specified in delims
     * as delimeters, not returning the delimeters as tokens.
     * 
     * @param text The text to be tokenized
     * @param delims The delimeters in a string
     */
    public QuoteTokenizer(String text, String delims) {
        this(text, delims, false);
    }
    
    /**
     * Constructs a tokenizer using the specified delimeters, specifying
     * if the delimeters should be returned as tokens.
     * 
     * @param text The text to be tokenized
     * @param delims The delimeters in a string
     * @param returnDelim Flag specifying if the delimeters should be returned
     */
    public QuoteTokenizer(String text, String delims, boolean returnDelim) {
        this.text = text;
        this.delims = delims;
        this.returnDelim = returnDelim;
    }

    /**
     * Calculates the number of times that this tokenizer's 
     * <code>nextToken</code> method can be called before it 
     * generates an exception. The current position is not advanced.
     * 
     * @return Returns the number of tokens remaining in the 
     * string using the current delimiter set
     */
    public int countTokens() {
        // See if the tokens were already count
        if(this.tokens == -1) {
            this.tokens = performTokenCount();
        }
        
        return this.tokens;
    }
    
    /**
     * Returns the same value as the <code>hasMoreTokens</code> method. 
     * It exists so that this class can implement the Enumeration interface.
     */
    public boolean hasMoreElements() {
        return this.hasMoreTokens();
    }

    /**
     * Tests if there are more tokens available from this 
     * tokenizer's string. If this method returns true, then a 
     * subsequent call to nextToken with no argument will 
     * successfully return a token.
     * 
     * @return Returns true if and only if there is at 
     * least one token in the string after the current position; 
     * false otherwise.
     */
    public boolean hasMoreTokens() {
        return this.currentTokenEnd < this.text.length();
    }

    /**
     * Returns the same value as the nextToken method, except that 
     * its declared return value is Object rather than String. 
     * It exists so that this class can implement the Enumeration interface.
     * 
     * @return Returns the next token in the string. 
     * 
     * @throws Throws NoSuchElementException - if there are no more 
     * tokens in this tokenizer's string. 
     */
    public Object nextElement() {
        return this.nextToken();
    }

    /**
     * Returns the next token from this string tokenizer.
     * 
     * @return Returns the next token from this string tokenizer.
     * @throws Throws NoSuchElementException - if there are no more 
     * tokens in this tokenizer's string.
     */
    public String nextToken() throws NoSuchElementException {
        if(!this.hasMoreTokens()) {
            throw new NoSuchElementException();
        }
        
        int tokStart = this.getTokenStart(this.currentTokenEnd);
        int tokEnd = this.getTokenEnd(tokStart);
        
        this.currentTokenEnd = tokEnd;
        return this.text.substring(tokStart, tokEnd);
    }

    /**
     * Returns the next token in this string tokenizer's string. 
     * First, the set of characters considered to be delimiters 
     * by this StringTokenizer object is changed to be the characters 
     * in the string delim. Then the next token in the string after 
     * the current position is returned. The current position is 
     * advanced beyond the recognized token. The new delimiter set 
     * remains the default after this call.
     * 
     * @param delims - the new delimeters.
     * @return Returns the next token, after switching to the 
     * new delimiter set.
     * 
     * @throws Throws NoSuchElementException - if there are no more 
     * tokens in this tokenizer's string.
     */
    public String nextToken(String delims) {
        this.delims = delims;
        this.tokens = -1;

        return this.nextToken();
    }
    
    /**
     * Restarts the tokenizer.
     */
    public void reset() {
        this.currentTokenEnd = -1;
    }
    
    /**
     * Restarts the tokenizer and sets new parameters.
     */
    public void reset(String delims, boolean returnDelim) {
        this.reset();
        this.delims = delims;
        this.returnDelim = returnDelim;
        this.tokens = -1;
    }


    private int performTokenCount() {
        // No; Count them
        int count = 1;
        
        int tokStart = this.getTokenStart(0);
        int tokEnd = this.getTokenEnd(tokStart);
        
        int textLen = this.text.length();
        while(tokEnd < textLen) {
            tokStart = this.getTokenStart(tokEnd);
            tokEnd = this.getTokenEnd(tokStart);
            
            count++;
        }
        
        return count;
    }
    
    private int getTokenStart(int prevTokenEnd) {
        int tokenStart = prevTokenEnd;
        
        if(!this.returnDelim) {
            int textLen = this.text.length();
            while(tokenStart < textLen 
                    && 
                    this.delims.indexOf(this.text.charAt(tokenStart)) >= 0
            ) {
                tokenStart++;
            }
        }

        return tokenStart;
    }
    
    private int getTokenEnd(int tokenStart) {
        return findNextDelim(tokenStart)+1;
    }
    
    private int findNextDelim(int start) {
        // The end of the token
        int nextDelim;
        
        // Mark if a " or ' was met
        char stringChar = 0;
        
        int textLen = this.text.length();
        for(nextDelim = start; nextDelim < textLen; nextDelim++) {
            char c = this.text.charAt(nextDelim);
            
            // Have we met a " or ' ?
            if(stringChar == 0) {
                // No;
                if(this.delims.indexOf(c) >= 0) {
                    // Found Delim!! Quit!
                    break;
                } else if(DELIMS.indexOf(c) >= 0) {
                    stringChar = c; 
                }
            } else {
                // Yes;
                if(c == stringChar) {
                    stringChar = 0;
                }
            }
        }
        
        return nextDelim;
    }

        
}
