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
package net.java.sip.communicator.service.history;

/**
 * Object used to uniquely identify a group of history records.
 *
 * @author Alexander Pelov
 */
public class HistoryID
{
    private final String[] id;

    private final String stringRepresentation;

    private final int hashCode;

    private HistoryID(String[] id)
    {
        this.id = id;

        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < id.length; i++)
        {
            if (i > 0)
                buff.append(' ');
            buff.append(this.id[i]);
        }

        this.stringRepresentation = buff.toString();
        this.hashCode = this.stringRepresentation.hashCode();
    }

    /**
     * Create a HistoryID from a raw ID. You can pass any kind of strings and
     * they will be safely converted to valid IDs.
     */
    public static HistoryID createFromRawID(String[] rawid)
    {
        // TODO: Validate: Assert.assertNonNull(rawid, "Parameter RAWID should
        // be non-null");
        // TODO: Validate: Assert.assertTrue(rawid.length > 0, "RAWID.length
        // should be > 0");

        String[] id = new String[rawid.length];
        for (int i = 0; i < rawid.length; i++)
        {
            id[i] = HistoryID.readableHash(rawid[i]);
        }

        return new HistoryID(id);
    }

    /**
     * Create a HistoryID from a raw Strings. You can pass any kind of strings
     * and they will be checked and converted to valid IDs.
     */
    public static HistoryID createFromRawStrings(String[] rawStrings)
    {
        String[] id = new String[rawStrings.length];
        for (int i = 0; i < rawStrings.length; i++)
        {
            id[i] = HistoryID.decodeReadableHash(rawStrings[i]);
        }

        return new HistoryID(id);
    }

    /**
     * Create a HistoryID from a valid ID. You should pass only valid IDs (ones
     * produced from readableHash).
     *
     * @throws IllegalArgumentException
     *             Thrown if a string from the ID is not valid an exception.
     */
    public static HistoryID createFromID(String[] id)
            throws IllegalArgumentException
    {
        // TODO: Validate: Assert.assertNonNull(id, "Parameter ID should be
        // non-null");
        // TODO: Validate: Assert.assertTrue(id.length > 0, "ID.length should be
        // > 0");

        for (int i = 0; i < id.length; i++)
        {
            if (!HistoryID.isIDValid(id[i]))
            {
                throw new IllegalArgumentException("Not a valid ID: " + id[i]);
            }
        }

        String[] newID = new String[id.length];
        System.arraycopy(id, 0, newID, 0, id.length);
        return new HistoryID(newID);
    }

    public String[] getID()
    {
        return this.id;
    }

    @Override
    public String toString()
    {
        return this.stringRepresentation;
    }

    @Override
    public int hashCode()
    {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean eq = false;

        if (obj instanceof HistoryID)
        {
            String[] id = ((HistoryID) obj).id;

            if (this.id.length == id.length)
            {
                eq = true;

                for (int i = 0; i < id.length; i++)
                {
                    String s1 = id[i];
                    String s2 = this.id[i];

                    if (s1 != s2 && (s1 == null || !s1.equals(s2)))
                    {
                        eq = false;
                        break;
                    }
                }
            }
        }

        return eq;
    }

    /**
     * An one-way function returning a "human readable" containing no special
     * characters. All characters _, a-z, A-Z, 0-9 are kept unchainged. All
     * other are replaced with _ and the word is postfixed with $HASHCODE, where
     * HASHCODE is the hexadecimal hash value of the original string. If there
     * are no special characters the word is not postfixed.
     *
     * Note: This method does not use URLEncoder, because in url-encoding the *
     * sign is considered as "safe".
     *
     * @param rawString
     *            The string to be hashed.
     * @return The human-readable hash.
     */
    public static String readableHash(String rawString)
    {
        StringBuilder encodedString = new StringBuilder(rawString);
        boolean addHash = false;

        for (int i = 0; i < encodedString.length(); i++)
        {
            if (HistoryID.isSpecialChar(encodedString.charAt(i)))
            {
                addHash = true;
                encodedString.setCharAt(i, '_');
            }
        }

        if (addHash)
        {
            encodedString.append('$');
            encodedString.append(Integer.toHexString(rawString.hashCode()));
        }

        return encodedString.toString();
    }

    /**
     * Decodes readable hash.
     *
     * @param rawString The string to be checked.
     * @return The human-readable hash.
     */
    public static String decodeReadableHash(String rawString)
    {
        int replaceCharIx = rawString.indexOf("_");
        int hashCharIx = rawString.indexOf("$");

        if(replaceCharIx > -1
            && hashCharIx > -1
            && replaceCharIx < hashCharIx)
        {
            //String rawStrNotHashed = encodedString.substring(0, hashCharIx);
            // String hashValue = encodedString.substring(hashCharIx + 1);
            // TODO: we can check the string, just to be sure, if we now
            // the char to replace, when dealing with accounts it will be :

            return rawString;
        }
        else
            return rawString;
    }

    /**
     * Tests if an ID is valid.
     */
    private static boolean isIDValid(String id)
    {
        boolean isValid = true;

        int pos = id.indexOf('$');
        if (pos < 0)
        {
            // There is no $ in the id. In order to be valid all characters
            // should be non-special
            isValid = !hasSpecialChar(id);
        } else {
            // There is a $ sign in the id. In order to be valid it has
            // to be in the form X..X$Y..Y, where there should be no
            // special characters in X..X, and Y..Y should be a hexadecimal
            // number
            if (pos + 1 < id.length())
            {
                String start = id.substring(0, pos);
                String end = id.substring(pos + 1);

                // Check X..X
                isValid = !hasSpecialChar(start);
                if (isValid)
                {
                    // OK; Check Y..Y
                    try
                    {
                        Integer.parseInt(end, 16);
                        // OK
                        isValid = true;
                    }
                    catch (Exception e)
                    {
                        // Not OK
                        isValid = false;
                    }
                }
            } else {
                // The % sign is in the beginning - bad ID.
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Tests if a character is a special one. A character is special if it is
     * not in the range _, a-z, A-Z, 0-9.
     *
     * @param c
     *            The character to test.
     * @return Returns true if the character is special. False otherwise.
     */
    private static boolean isSpecialChar(char c)
    {
        return  (c != '_')
                && (c != '@')
                && (c != '.')
                && (c != '-')
                && (c != '+')
                && (c < 'A' || c > 'Z') && (c < 'a' || c > 'z')
                && (c < '0' || c > '9');
    }

    /**
     * Tests there is a special character in a string.
     */
    private static boolean hasSpecialChar(String str)
    {
        boolean hasSpecialChar = false;

        for (int i = 0; i < str.length(); i++)
        {
            if (isSpecialChar(str.charAt(i)))
            {
                hasSpecialChar = true;
                break;
            }
        }

        return hasSpecialChar;
    }

}
