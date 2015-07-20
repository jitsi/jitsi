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

package net.java.sip.communicator.plugin.notificationconfiguration;

import java.io.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.util.*;

/**
 * Filter to display only the sound files in the filechooser.
 *
 * @author Alexandre Maillard
 * @author Vincent Lucas
 */
public class SoundFilter
    extends SipCommFileFilter
{
    /**
     * All acceptable sound formats. If null, then this sound filter will accept
     * all sound formats available in SoundFileUtils.
     */
    private String[] soundFormats = null;

    /**
     * Creates a new sound filter which accepts all sound format available in
     * SoundFileUtils.
     */
    public SoundFilter()
    {
        super();
    }

    /**
     * Creates a new sound filter which accepts only sound format corresponding
     * to the list given in parameter.
     *
     * @param soundFormats The list of sound format to accept.
     */
    public SoundFilter(String[] soundFormats)
    {
        super();
        if(soundFormats != null)
        {
            this.soundFormats = new String[soundFormats.length];
            System.arraycopy(
                    soundFormats,
                    0,
                    this.soundFormats,
                    0,
                    soundFormats.length);
        }
    }

    /**
     * Method which describes differents permits extensions and defines which
     * file or directory will be displayed in the filechoser.
     *
     * @param f file for the test
     *
     * @return boolean true if the File is a Directory or a sound file. And
     * return false in the other cases.
     */
    @Override
    public boolean accept(File f)
    {
        // Tests if the file passed in argument is a directory.
        if (f.isDirectory())
        {
            return true;
        }
        // Else, tests if the exension is correct.
        else
        {
            return SoundFileUtils.isSoundFile(f, this.soundFormats);
        }
    }

    /**
     * Method which describes, in the file chooser, the text representing the
     * permit extension files.
     *
     * @return String which is displayed in the sound file chooser.
     */
    @Override
    public String getDescription()
    {
        String desc = "Sound File (";
        if(this.soundFormats != null)
        {
            for(int i = 0; i < this.soundFormats.length; ++i)
            {
                if(i != 0)
                {
                    desc += ", ";
                }
                desc += "*." + this.soundFormats[i];
            }
        }
        else
        {
            desc += "*.au, *.mid, *.mod, *.mp2, *.mp3, *.ogg, *.ram, *.wav, "
                + "*.wma";
        }
        desc += ")";

        return desc;
    }
}
