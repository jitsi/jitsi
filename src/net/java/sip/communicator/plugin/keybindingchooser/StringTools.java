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
package net.java.sip.communicator.plugin.keybindingchooser;

/**
 * Common methods for string manipulations.
 * @author Damian Johnson (atagar1@gmail.com)
 * @version August 8, 2007
 */
public class StringTools {
  /**
   * Provides a more readable version of constant names. Spaces replace underscores and this changes
   * the input to lowercase except the first letter of each word. For instance, "RARE_CARDS" would
   * become "Rare Cards".
   * @param input string to be converted
   * @return reader friendly variant of constant name
   */
  public static String getReadableConstant(String input) {
    char[] name = input.toCharArray();

    boolean isStartOfWord = true;
    for (int i = 0; i < name.length; ++i) {
      char chr = name[i];
      if (chr == '_') name[i] = ' ';
      else if (isStartOfWord) name[i] = Character.toUpperCase(chr);
      else name[i] = Character.toLowerCase(chr);
      isStartOfWord = chr == '_';
    }

    return new String(name);
  }
}
