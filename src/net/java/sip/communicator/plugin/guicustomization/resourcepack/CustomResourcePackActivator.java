/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.guicustomization.resourcepack;

import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.startlevel.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 *
 * @author damencho
 */
public class CustomResourcePackActivator
    implements BundleActivator
{

    private Logger logger =
        Logger.getLogger(CustomResourcePackActivator.class);

    private static BundleContext bundleContext;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        ServiceReference serviceReference = bundleContext
                .getServiceReference(StartLevel.class.getName());

        StartLevel startLevelService = (StartLevel) bundleContext
            .getService(serviceReference);
        
        startLevelService.setBundleStartLevel(bc.getBundle(), 39);

        CustomColorPackImpl colPackImpl = 
            new CustomColorPackImpl();

        Hashtable props = new Hashtable();
        props.put(ColorPack.RESOURCE_NAME, 
                  ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  ColorPack.class.getName(),
                                        colPackImpl,
                                        props);

        CustomImagePackImpl imgPackImpl = 
            new CustomImagePackImpl();

        Hashtable imgProps = new Hashtable();
        imgProps.put(ImagePack.RESOURCE_NAME, 
                    ImagePack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  ImagePack.class.getName(),
                                        imgPackImpl,
                                        imgProps);
        
        CustomLanguagePackImpl langPackImpl = 
            new CustomLanguagePackImpl();
        
        Hashtable langProps = new Hashtable();
        langProps.put(LanguagePack.RESOURCE_NAME, 
                    LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  LanguagePack.class.getName(),
                                        langPackImpl,
                                        langProps);
        
        CustomSettingsPackImpl setPackImpl = 
            new CustomSettingsPackImpl();
        
        Hashtable setProps = new Hashtable();
        langProps.put(SettingsPack.RESOURCE_NAME, 
                      SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  SettingsPack.class.getName(),
                                        setPackImpl,
                                        setProps);
        
        CustomSoundPackImpl sndPackImpl = 
            new CustomSoundPackImpl();
        
        Hashtable sndProps = new Hashtable();
        langProps.put(SoundPack.RESOURCE_NAME, 
                      SoundPack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  SoundPack.class.getName(),
                                        sndPackImpl,
                                        sndProps);

        logger.info("Custom resources ... [REGISTERED]");
    }

    public void stop(BundleContext bc) throws Exception
    {

    }
}
