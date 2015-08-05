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
package net.java.sip.communicator.util;

import java.util.regex.*;

/**
 * Simple password quality meter. The JavaScript version found at
 * http://www.geekwisdom.com/js/passwordmeter.js was used as a base. Provides a
 * method to compute the relative score of the password that can be used to
 * model a progress bar, for example.
 *
 * @author Dmitri Melnikov
 */
public class PasswordQualityMeter
{
    /**
     * Maximum possible points.
     */
    public static final int TOTAL_POINTS = 42;

    /**
     * Assesses the strength of the password.
     *
     * @param pass the password to assess
     * @return the score for this password between 0 and <tt>TOTAL_POINTS</tt>
     */
    public int assessPassword(String pass)
    {
        int score = 0;

        if (pass == null || pass.length() == 0)
            return score;
        score += assessLength(pass);
        score += assessLetters(pass);
        score += assessNumbers(pass);
        score += assessSpecials(pass);
        return score;
    }

    /**
     * Assesses password length:
     * level 0 (3 point): less than 5 characters
     * level 1 (6 points): between 5 and 7 characters
     * level 2 (12 points): between 8 and 15 characters
     * level 3 (18 points): 16 or more characters
     *
     * @param pass the password to assess
     * @return the score based on the length
     */
    private int assessLength(String pass)
    {
        int len = pass.length();

        if (len < 5)
            return 3;
        else if (len >= 5 && len < 8)
            return 6;
        else if (len >= 8 && len < 16)
            return 12;
        // len >= 16
        return 18;
    }

    /**
     * Assesses letter cases:
     * level 0 (0 points): no letters
     * level 1 (5 points): all letters are either lower or upper case
     * level 2 (7 points): letters are mixed case
     *
     * @param pass the password to assess
     * @return the score based on the letters
     */
    private int assessLetters(String pass)
    {
        boolean lower = matches(pass, "[a-z]+");
        boolean upper = matches(pass, "[A-Z]+");

        if (lower && upper)
            return 7;
        if (lower || upper)
            return 5;
        return 0;
    }

    /**
     * Assesses number count:
     * level 0 (0 points): no numbers exist
     * level 1 (5 points): one or two number exists
     * level 1 (7 points): 3 or more numbers exists
     *
     * @param pass the password to assess
     * @return the score based on the numbers
     */
    private int assessNumbers(String pass)
    {
        int found = countMatches(pass, "\\d");

        if (found < 1)
            return 0;
        else if (found >= 1 && found < 3)
            return 5;
        return 7;
    }

    /**
     * Assesses special character count.
     * Here special characters are non-word and non-space ones.
     * level 0 (0 points): no special characters
     * level 1 (5 points): one special character exists
     * level 2 (10 points): more than one special character exists
     *
     * @param pass the password to assess
     * @return the score based on special characters
     */
    private int assessSpecials(String pass)
    {
        int found = countMatches(pass, "[^\\w\\s]");

        if (found < 1)
            return 0;
        else if (found <= 1 && found < 2)
            return 5;
        return 10;
    }

    /**
     * Counts the number of matches of a given pattern in a given string.
     *
     * @param str the string to search in
     * @param pattern the pattern to search for
     * @return number of matches of <tt>patter</tt> in <tt>str</tt>
     */
    private int countMatches(String str, String pattern)
    {
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(str);
        int found = 0;

        while (matcher.find())
            found++;
        return found;
    }

    /**
     * Wrapper around @link{Pattern} and @link{Matcher} classes.
     *
     * @param str the string to search in
     * @param pattern the pattern to search for
     * @return true if <tt>pattern</tt> has been found in <tt>str</tt>.
     */
    private boolean matches(String str, String pattern)
    {
        return Pattern.compile(pattern).matcher(str).find();
    }
}
