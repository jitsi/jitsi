/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.util.Hashtable;
import net.java.sip.communicator.service.resources.*;

import net.java.sip.communicator.util.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author damencho
 */
public class DefaultResourcePackActivator
    implements BundleActivator
{

    private Logger logger =
        Logger.getLogger(DefaultResourcePackActivator.class);
    private static BundleContext bundleContext;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        DefaultColorPackImpl colPackImpl = 
            new DefaultColorPackImpl();
        
        Hashtable props = new Hashtable();
        props.put(ResourcePack.RESOURCE_NAME, 
                  ColorPack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  ColorPack.class.getName(),
                                        colPackImpl,
                                        props);
        
        DefaultImagePackImpl imgPackImpl = 
            new DefaultImagePackImpl();
        
        Hashtable imgProps = new Hashtable();
        imgProps.put(ResourcePack.RESOURCE_NAME, 
                    ImagePack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  ImagePack.class.getName(),
                                        imgPackImpl,
                                        imgProps);
        
        DefaultLanguagePackImpl langPackImpl = 
            new DefaultLanguagePackImpl();
        
        Hashtable langProps = new Hashtable();
        langProps.put(ResourcePack.RESOURCE_NAME, 
                    LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  LanguagePack.class.getName(),
                                        langPackImpl,
                                        langProps);
        
        DefaultSettingsPackImpl setPackImpl = 
            new DefaultSettingsPackImpl();
        
        Hashtable setProps = new Hashtable();
        langProps.put(ResourcePack.RESOURCE_NAME, 
                      SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  SettingsPack.class.getName(),
                                        setPackImpl,
                                        setProps);
        
        DefaultSoundPackImpl sndPackImpl = 
            new DefaultSoundPackImpl();
        
        Hashtable sndProps = new Hashtable();
        langProps.put(ResourcePack.RESOURCE_NAME, 
                      SoundPack.RESOURCE_NAME_DEFAULT_VALUE);
        
        bundleContext.registerService(  SoundPack.class.getName(),
                                        sndPackImpl,
                                        sndProps);

        logger.info("Default resources ... [REGISTERED]");
    }

    public void stop(BundleContext bc) throws Exception
    {

    }
}
