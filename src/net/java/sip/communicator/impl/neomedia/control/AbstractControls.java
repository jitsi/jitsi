/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.control;

import javax.media.*;

import net.java.sip.communicator.util.*;

/**
 * Provides an abstract implementation of <tt>Controls</tt> which facilitates
 * implementers by requiring them to only implement
 * {@link Controls#getControls()}.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractControls
    implements Controls
{

    /**
     * The <tt>Logger</tt> used by the <tt>AbstractControls</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractControls.class);

    /**
     * Implements {@link Controls#getControl(String)}. Invokes
     * {@link #getControls()} and then looks for a control of the specified type
     * in the returned array of controls.
     *
     * @param controlType a <tt>String</tt> value naming the type of the control
     * of this instance to be retrieved
     * @return an <tt>Object</tt> which represents the control of this instance
     * with the specified type
     */
    public Object getControl(String controlType)
    {
        return getControl(this, controlType);
    }

    /**
     * Gets the control of a specific <tt>Controls</tt> implementation of a
     * specific type if such a control is made available through
     * {@link Controls#getControls()}; otherwise, returns <tt>null</tt>.
     *
     * @param controlsImpl the implementation of <tt>Controls</tt> which is to
     * be queried for its list of controls so that the control of the specified
     * type can be looked for
     * @param controlType a <tt>String</tt> value which names the type of the
     * control to be retrieved
     * @return an <tt>Object</tt> which represents the control of
     * <tt>controlsImpl</tt> of the specified <tt>controlType</tt> if such a
     * control is made available through <tt>Controls#getControls()</tt>;
     * otherwise, <tt>null</tt>
     */
    public static Object getControl(Controls controlsImpl, String controlType)
    {
        Object[] controls = controlsImpl.getControls();

        if ((controls != null) && (controls.length > 0))
        {
            Class<?> controlClass;

            try
            {
                controlClass = Class.forName(controlType);
            }
            catch (ClassNotFoundException cnfe)
            {
                controlClass = null;
                logger
                    .warn(
                        "Failed to find control class " + controlType,
                        cnfe);
            }
            if (controlClass != null)
                for (Object control : controls)
                    if (controlClass.isInstance(control))
                        return control;
        }
        return null;
    }
}
