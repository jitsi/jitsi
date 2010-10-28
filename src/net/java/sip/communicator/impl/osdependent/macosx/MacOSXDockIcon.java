package net.java.sip.communicator.impl.osdependent.macosx;

import net.java.sip.communicator.impl.osdependent.*;

import com.apple.eawt.*;

/**
 * MacOSX specific dock icon, which will add a dock icon listener in order to
 * show the application each time user clicks on the dock icon.
 *
 * @author Yana Stamcheva
 */
@SuppressWarnings("deprecation")
public class MacOSXDockIcon
{
    /**
     * Adds a dock icon listener in order to show the application each time user
     * clicks on the dock icon.
     */
    public static void addDockIconListener()
    {
        Application application = Application.getApplication();
        if (application != null)
        {
            application.addApplicationListener(new ApplicationAdapter()
            {
                /**
                 * Handles re-open application event. Shows the contact list
                 * window.
                 */
                public void handleReOpenApplication(
                    ApplicationEvent applicationevent)
                {
                    OsDependentActivator.getUIService().setVisible(true);
                }
            });
        }
    }
}
