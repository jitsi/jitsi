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
package net.java.sip.communicator.impl.protocol.irc.exception;

/**
 * (Checked) Exception for unknown mode symbols.
 *
 * @author Danny van Heumen
 */
public class UnknownModeException extends Exception
{
    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Symbol of unknown mode.
     */
    private final char symbol;

    /**
     * Constructor.
     *
     * @param symbol the unknown mode's symbol
     */
    public UnknownModeException(final char symbol)
    {
        super("Encountered an unknown mode: " + symbol + ".");
        this.symbol = symbol;
    }

    /**
     * Get the symbol of the unknown mode.
     *
     * @return returns the IRC mode symbol
     */
    public char getSymbol()
    {
        return this.symbol;
    }
}
