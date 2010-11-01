package net.java.sip.communicator.impl.osdependent.macosx;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.service.gui.*;

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
                    UIService uiService = OsDependentActivator.getUIService();

                    if (uiService != null && !uiService.isVisible())
                        uiService.setVisible(true);
                }
            });
        }
    }
}
