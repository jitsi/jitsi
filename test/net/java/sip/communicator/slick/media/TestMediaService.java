/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.media;

import junit.framework.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.media.event.MediaEvent;
import net.java.sip.communicator.service.media.event.MediaListener;

/**
 * Tests basic MediaService behaviour.
 *
 * @author Martin Andre
 */
public class TestMediaService extends TestCase
{
    /**
     * The MediaService that we will be testing.
     */
    private MediaService mediaService = null;

    /**
     * The MediaEvent that our test listeners will capture for testing.
     * Make sure we null that upon tear down
     */
    private MediaEvent mediaEvent = null;

    /**
     * A Media listener impl.
     */
    private MediaListener mediaListener = new MediaListener()
    {
        public void receivedMediaStream(MediaEvent evt) {
            // TODO Auto-generated method stub
            mediaEvent = evt;
        }

        public void mediaServiceStatusChanged() {
            // TODO Auto-generated method stub
        }
    };


    /**
     * Generic JUnit Constructor.
     * @param name the name of the test
     */
    public TestMediaService(String name)
    {
        super(name);
        BundleContext context = MediaServiceLick.bc;
        ServiceReference ref = context.getServiceReference(
            MediaService.class.getName());
        this.mediaService = (MediaService)context.getService(ref);
    }

    /**
     * Generic JUnit setUp method.
     * @throws Exception if anything goes wrong.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Generic JUnit tearDown method.
     * @throws Exception if anything goes wrong.
     */
    protected void tearDown() throws Exception
    {
        //first remove any remaining listeners
        mediaService.removeMediaListener(mediaListener);

        mediaEvent = null;

        super.tearDown();
    }

    /**
     * Test initialisation check.
     */
    public void testIsInitialized()
    {
        // Initial check
        assertFalse(mediaService.isInitialized());

        // Initialize service
//emcho: breaks the build on proxenet cause there's no X server there.
// I wonder whether who's to blame here - proexenet for not having an X server
// or initialize for requiring one ?
//        mediaService.initialize();
//        assertTrue(mediaService.isInitialized());

        // XXX Should also check that Media service is really initialized

        // Shutdown service
        mediaService.shutdown();
        assertFalse(mediaService.isInitialized());
    }

    /**
     * Tests media event notification through listeners.
     */
    public void testMediaEventNotification()
    {
        mediaEvent = null;

        mediaService.addMediaListener(mediaListener);

        // test the initial set of a property.
//emcho: breaks the build on proxenet cause there's no X server there.
// I wonder whether who's to blame here - proexenet for not having an X server
// or initialize for requiring one ?
//        mediaService.initialize();
//        assertNotNull("A MediaEvent with a registered listener", mediaEvent);

        mediaService.shutdown();

        //test remove
        mediaEvent = null;
        mediaService.removeMediaListener(mediaListener);

//emcho: breaks the build on proxenet cause there's no X server there.
// I wonder whether who's to blame here - proexenet for not having an X server
// or initialize for requiring one ?
//        mediaService.initialize();
//        assertNull("A MediaEvent after unregistering a listener.",
//                mediaEvent);

        mediaService.shutdown();
    }

}
