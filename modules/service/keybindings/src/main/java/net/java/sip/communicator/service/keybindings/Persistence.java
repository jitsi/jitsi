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
package net.java.sip.communicator.service.keybindings;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

/**
 * Convenience methods providing a quick means of loading and saving
 * keybindings. None preserve disabled mappings. The formats provided are as
 * follows:<br>
 * SERIAL_HASH- Serialized hash map of bindings. Ordering is preserved if
 * available.<br>
 * SERIAL_INPUT- Serialized input map of bindings.<br>
 * PROPERTIES_PAIR- Persistence provided by java.util.Properties using plain
 * text key/value pairs.<br>
 * PROPERTIES_XML- Persistence provided by java.util.Properties using its XML
 * format.
 *
 * @author Damian Johnson (atagar1@gmail.com)
 * @version September 21, 2007
 */
public enum Persistence
{
    SERIAL_HASH, SERIAL_INPUT, PROPERTIES_PAIRS, PROPERTIES_XML;

    private static final String PROPERTIES_COMMENT =
        "Keybindings (mapping of KeyStrokes to string representations of actions)";

    /**
     * Returns the enum representation of a string. This is case sensitive.
     *
     * @param str toString representation of this enum
     * @return enum associated with a string
     * @throws IllegalArgumentException if argument is not represented by this
     *             enum.
     */
    public static Persistence fromString(String str)
    {
        for (Persistence type : Persistence.values())
        {
            if (str.equals(type.toString()))
                return type;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Attempts to load this type of persistent keystroke map from a given path.
     * This is unable to parse any null content.
     *
     * @param path absolute path to resource to be loaded
     * @return keybinding map reflecting file contents
     * @throws IOException if unable to load resource
     * @throws ParseException if unable to parse content
     */
    public LinkedHashMap<KeyStroke, String> load(String path)
        throws IOException,
        ParseException
    {
        return load(new FileInputStream(path));
    }

    /**
     * Attempts to load this type of persistent keystroke map from a given
     * stream. This is unable to parse any null content.
     *
     * @param input source of keybindings to be parsed
     * @return keybinding map reflecting file contents
     * @throws IOException if unable to load resource
     * @throws ParseException if unable to parse content
     */
    public LinkedHashMap<KeyStroke, String> load(InputStream input)
        throws IOException,
        ParseException
    {
        LinkedHashMap<KeyStroke, String> output =
            new LinkedHashMap<KeyStroke, String>();

        if (this == SERIAL_HASH || this == SERIAL_INPUT)
        {
            Object instance = null; // Loaded serialized object

            try
            {
                ObjectInputStream objectInput = new ObjectInputStream(input);
                instance = objectInput.readObject();
                objectInput.close();
            }
            catch (ClassNotFoundException exc)
            {
                throw new ParseException("Unable to load serialized content", 0);
            }

            if (this == SERIAL_HASH)
            {
                if (!(instance instanceof HashMap<?, ?>))
                {
                    throw new ParseException(
                        "Serialized resource doesn't represent a HashMap", 0);
                }

                HashMap<?, ?> mapping = (HashMap<?, ?>)instance;
                for (Object key : mapping.keySet())
                {
                    Object value = mapping.get(key);

                    if (key instanceof KeyStroke && value instanceof String)
                    {
                        output.put((KeyStroke) key, (String) value);
                    }
                    else
                    {
                        if (key == null || value == null)
                        {
                            throw new ParseException(
                                "Unable to load null content", 0);
                        }
                        else
                        {
                            StringBuilder message = new StringBuilder();
                            message
                                .append("Entry doesn't represent a keybinding: ");
                            message.append(key.getClass().getName());
                            message.append(" -> ");
                            message.append(value.getClass().getName());
                            message
                                .append("\nMust match KeyStroke -> String mapping");
                            throw new ParseException(message.toString(), 0);
                        }
                    }
                }
            }
            else
            {
                if (!(instance instanceof InputMap))
                {
                    throw new ParseException(
                        "Serialized resource doesn't represent an InputMap", 0);
                }

                InputMap mapping = (InputMap) instance;
                if (mapping.keys() != null)
                {
                    for (KeyStroke shortcut : mapping.keys())
                    {
                        if (shortcut == null || mapping.get(shortcut) == null)
                        {
                            throw new ParseException(
                                "Unable to load null content", 0);
                        }
                        else
                        {
                            output.put(shortcut, mapping.get(shortcut)
                                .toString());
                        }
                    }
                }
            }
        }
        else if (this == PROPERTIES_PAIRS || this == PROPERTIES_XML)
        {
            Properties properties = new Properties();
            if (this == PROPERTIES_PAIRS)
                properties.load(input);
            else if (this == PROPERTIES_XML)
                properties.loadFromXML(input);

            for (Object key : properties.keySet())
            {
                Object value = properties.get(key);

                if (key instanceof String && value instanceof String)
                {
                    KeyStroke keystroke = KeyStroke.getKeyStroke((String) key);
                    if (keystroke == null)
                    {
                        StringBuilder message = new StringBuilder();
                        message
                            .append("Unable to parse keystroke, see the getKeyStroke(String) method of ");
                        message.append(KeyStroke.class.getName());
                        message.append(" for proper format");
                        throw new ParseException(message.toString(), 0);
                    }
                    else
                    {
                        output.put(keystroke, (String) value);
                    }
                }
                else
                {
                    if (key == null || value == null)
                    {
                        throw new ParseException("Unable to load null content",
                            0);
                    }
                    else
                    {
                        StringBuilder message = new StringBuilder();
                        message
                            .append("Entry doesn't represent a keybinding: ");
                        message.append(key.getClass().getName());
                        message.append(" -> ");
                        message.append(value.getClass().getName());
                        message
                            .append("\nMust match String -> String mapping where the first string represents a keystroke");
                        throw new ParseException(message.toString(), 0);
                    }
                }
            }
        }

        input.close();
        return output;
    }

    /**
     * Writes the persistent state of the bindings to an output stream.
     *
     * @param output stream where persistent state should be written
     * @param bindings keybindings to be saved
     * @throws IOException if unable to save bindings
     * @throws UnsupportedOperationException if any keys or values of the
     *             binding are null
     */
    public void save(OutputStream output, Map<KeyStroke, String> bindings)
        throws IOException
    {
        for (KeyStroke key : bindings.keySet())
        {
            if (key == null || bindings.get(key) == null)
            {
                throw new UnsupportedOperationException(
                    "Invalid binding: Shortcuts and actions cannot be null");
            }
        }

        if (this == SERIAL_HASH || this == SERIAL_INPUT)
        {
            Object mapping; // Mapping to be serialized
            if (this == SERIAL_HASH)
                mapping = bindings;
            else
            {
                InputMap inputMap = new InputMap();
                for (KeyStroke shortcut : bindings.keySet())
                {
                    inputMap.put(shortcut, bindings.get(shortcut));
                }
                mapping = inputMap;
            }

            ObjectOutputStream objectOutput = new ObjectOutputStream(output);
            objectOutput.writeObject(mapping);
            objectOutput.flush();
            objectOutput.close();
        }
        else if (this == PROPERTIES_PAIRS || this == PROPERTIES_XML)
        {
            Properties properties = new Properties();
            for (KeyStroke shortcut : bindings.keySet())
            {
                properties.setProperty(shortcut.toString(), bindings
                    .get(shortcut));
            }

            if (this == PROPERTIES_PAIRS)
                properties.store(output, PROPERTIES_COMMENT);
            else
                properties.storeToXML(output, PROPERTIES_COMMENT);
        }
    }

    /**
     * Writes the persistent state of the bindings to a file.
     *
     * @param path absolute path to where bindings should be saved
     * @param bindings keybindings to be saved
     * @throws IOException if unable to save bindings
     * @throws UnsupportedOperationException if any keys or values of the
     *             binding are null
     */
    public void save(String path, Map<KeyStroke, String> bindings)
        throws IOException
    {
        FileOutputStream output = new FileOutputStream(path);
        try
        {
            save(output, bindings);
            output.flush();
            output.close();
        }
        catch (IOException exc)
        {
            output.flush();
            output.close();
            throw exc;
        }
        catch (UnsupportedOperationException exc)
        {
            output.flush();
            output.close();
            throw exc;
        }
    }

    /**
     * Provides the textual output of what this persistence format would save
     * given a set of bindings. This silently fails, returning null if unable to
     * generate output from bindings.
     *
     * @param bindings bindings for which to generate saved output
     * @return string reflecting what would be saved by this persistence format
     */
    public String getOutput(Map<KeyStroke, String> bindings)
    {
        /*-
         * This utilizes a rather lengthy chain of redirection to generate output as a string:
         * PipedOutputStream ->
         * PipedInputStream ->
         * Scanner ->
         * StringBuilder ->
         * String
         */

        PipedOutputStream pipeOut = new PipedOutputStream();
        PipedInputStream pipeIn = new PipedInputStream();
        Scanner scanner = new Scanner(pipeIn);

        try
        {
            pipeOut.connect(pipeIn);
            save(pipeOut, bindings);
            pipeOut.flush();
            pipeOut.close();

            StringBuilder builder = new StringBuilder();
            if (scanner.hasNextLine())
                builder.append(scanner.nextLine());
            while (scanner.hasNextLine())
            {
                builder.append("\n");
                builder.append(scanner.nextLine());
            }
            scanner.close();
            pipeIn.close();
            return builder.toString();
        }
        catch (IOException exc)
        {
            return null;
        }
        catch (UnsupportedOperationException exc)
        {
            return null;
        }
    }

    @Override
    public String toString()
    {
        if (this == SERIAL_HASH)
            return "Serialized Hash Map";
        else if (this == SERIAL_INPUT)
            return "Serialized Input Map";
        else if (this == PROPERTIES_XML)
            return "Properties XML";
        else
            return getReadableConstant(this.name());
    }

    /**
     * Provides a more readable version of constant names. Spaces replace
     * underscores and this changes the input to lowercase except the first
     * letter of each word. For instance, "RARE_CARDS" would become
     * "Rare Cards".
     *
     * @param input string to be converted
     * @return reader friendly variant of constant name
     */
    public static String getReadableConstant(String input)
    {
        char[] name = input.toCharArray();

        boolean isStartOfWord = true;
        for (int i = 0; i < name.length; ++i)
        {
            char chr = name[i];
            if (chr == '_')
                name[i] = ' ';
            else if (isStartOfWord)
                name[i] = Character.toUpperCase(chr);
            else
                name[i] = Character.toLowerCase(chr);
            isStartOfWord = chr == '_';
        }

        return new String(name);
    }
}
