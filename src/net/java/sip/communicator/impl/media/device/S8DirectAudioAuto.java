package net.java.sip.communicator.impl.media.device;

import java.util.*;

import javax.media.*;

import com.sun.media.*;

/**
 * Probes for JMF Solaris 8 direct audio.
 *
 * @author Emil Ivov
 * @author Ken Larson
 */
public class S8DirectAudioAuto
{
    @SuppressWarnings("unchecked") //legacy JMF code.
    public S8DirectAudioAuto() throws Exception
    {
        Class<?> cls;
        int plType = PlugInManager.RENDERER;
        String dar = "com.sun.media.renderer.audio.DirectAudioRenderer";

        // Check if this is the solaris Performance Pack - hack
        cls = Class.forName(
            "net.java.sip.communicator.impl.media.device.SunVideoAuto");

        // Find the renderer class and instantiate it.
        cls = Class.forName(dar);

        Renderer rend = (Renderer) cls.newInstance();

        if (rend instanceof ExclusiveUse &&
            ! ( (ExclusiveUse) rend).isExclusive())
        {
            // sol8+, DAR supports mixing
            Vector<String> rendList = PlugInManager.getPlugInList(null, null,
                plType);
            int listSize = rendList.size();
            boolean found = false;
            String rname = null;

            for (int i = 0; i < listSize; i++)
            {
                rname = rendList.elementAt(i);
                if (rname.equals(dar))
                { // DAR is in the registry
                    found = true;
                    rendList.removeElementAt(i);
                    break;
                }
            }

            if (found)
            {
                rendList.insertElementAt(dar, 0);
                PlugInManager.setPlugInList(rendList, plType);
                PlugInManager.commit();
            }
        }
    }
}
