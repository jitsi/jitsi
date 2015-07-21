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
package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.*;

import org.jitsi.service.resources.*;

/**
 * Manages the access to the properties file containing all sounds paths.
 *
 * @author Yana Stamcheva
 */
public final class SoundProperties
{
    /**
     * The zero tone sound id.
     */
    public static final String DIAL_ZERO;

    /**
     * The one tone sound id.
     */
    public static final String DIAL_ONE;

    /**
     * The two tone sound id.
     */
    public static final String DIAL_TWO;

    /**
     * The three tone sound id.
     */
    public static final String DIAL_THREE;

    /**
     * The four tone sound id.
     */
    public static final String DIAL_FOUR;

    /**
     * The five tone sound id.
     */
    public static final String DIAL_FIVE;

    /**
     * The six tone sound id.
     */
    public static final String DIAL_SIX;

    /**
     * The seven tone sound id.
     */
    public static final String DIAL_SEVEN;

    /**
     * The eight tone sound id.
     */
    public static final String DIAL_EIGHT;

    /**
     * The nine tone sound id.
     */
    public static final String DIAL_NINE;

    /**
     * The diez tone sound id.
     */
    public static final String DIAL_DIEZ;

    /**
     * The star tone sound id.
     */
    public static final String DIAL_STAR;

    static
    {

        /*
         * Call GuiActivator.getResources() once because (1) it's not a trivial
         * getter, it caches the reference so it always checks whether the cache
         * has already been built and (2) accessing a local variable is supposed
         * to be faster than calling a method (even if the method is a trivial
         * getter and it's inlined at runtime, it's still supposed to be slower
         * because it will be accessing a field, not a local variable).
         */
        ResourceManagementService resources = GuiActivator.getResources();

        DIAL_ZERO = resources.getSoundPath("DIAL_ZERO");
        DIAL_ONE = resources.getSoundPath("DIAL_ONE");
        DIAL_TWO = resources.getSoundPath("DIAL_TWO");
        DIAL_THREE = resources.getSoundPath("DIAL_THREE");
        DIAL_FOUR = resources.getSoundPath("DIAL_FOUR");
        DIAL_FIVE = resources.getSoundPath("DIAL_FIVE");
        DIAL_SIX = resources.getSoundPath("DIAL_SIX");
        DIAL_SEVEN = resources.getSoundPath("DIAL_SEVEN");
        DIAL_EIGHT = resources.getSoundPath("DIAL_EIGHT");
        DIAL_NINE = resources.getSoundPath("DIAL_NINE");
        DIAL_DIEZ = resources.getSoundPath("DIAL_DIEZ");
        DIAL_STAR = resources.getSoundPath("DIAL_STAR");
    }

    private SoundProperties() {
    }
}
