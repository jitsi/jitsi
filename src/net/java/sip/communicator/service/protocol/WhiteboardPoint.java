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
package net.java.sip.communicator.service.protocol;

/**
 * A point representing a location in {@code (x,y)} coordinate space,
 * specified in integer precision.
 * <p>
 * This class has been inspired by the java.awt.Point class.
 * <p>
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardPoint implements Cloneable
{
    /**
     * The X coordinate of this WhiteboadPoint.
     */
    private double x;

    /**
     * The Y coordinate of this <code>Point</code>.
     */
    private double y;

    /**
     * Constructs and initializes a point with the same location as
     * the specified <tt>Point</tt> object.
     * @param p a point
     */
    public WhiteboardPoint(WhiteboardPoint p)
    {
        this(p.x, p.y);
    }

    /**
     * Constructs and initializes a point at the specified <tt>(x,y)</tt>
     * location in the coordinate space.
     *
     * @param x the X coordinate of the newly constructed <code>Point</code>
     * @param y the Y coordinate of the newly constructed <code>Point</code>
     * @since 1.0
     */
    public WhiteboardPoint(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the X coordinate of this <tt>WhiteboardPoint</tt>.
     *
     * @return the x coordinate of this <tt>WhiteboardPoint</tt>.
     */
    public double getX()
    {
        return x;
    }

    /**
     * Returns the Y coordinate of this <tt>WhiteboardPoint</tt>.
     *
     * @return the y coordinate of this <tt>WhiteboardPoint</tt>.
     */
    public double getY()
    {
        return y;
    }

    /**
     * Sets a new value to the x coordinate.
     *
     * @param x the new value of the x coordinate
     */
    public void setX(double x)
    {
        this.x = x;
    }

    /**
     * Sets a new value to the y coordinate.
     *
     * @param y the new value of the y coordinate
     */
    public void setY(double y)
    {
        this.y = y;
    }

    /**
     * Determines whether or not two points are equal. Two instances of
     * <tt>WhiteboardPoint</tt> are equal if the values of their
     * <tt>x</tt> and <tt>y</tt> member fields, representing
     * their position in the coordinate space, are the same.
     *
     * @param obj an object to be compared with this <tt>WhiteboardPoint</tt>
     *
     * @return <tt>true</tt> if the object to be compared is an instance of
     * <tt>WhiteboardPoint</tt> and has the same values; <tt>false</tt>
     * otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof WhiteboardPoint)
        {
            WhiteboardPoint pt = (WhiteboardPoint)obj;
            return (x == pt.x) && (y == pt.y);
        }

        return false;
    }

    /**
     * Returns a string representation of this point and its location
     * in the {@code (x,y)} coordinate space. This method is intended to be
     * used only for debugging purposes, and the content  and format of the
     * returned string may vary between implementations.
     *
     * The returned string may be empty but may not be <code>null</code>.
     *
     * @return a string representation of this point
     */
    @Override
    public String toString()
    {
        return getClass().getName() + "[x=" + x + ",y=" + y + "]";
    }

    /**
     * Creates and returns a copy of this <tt>WhiteboardPoint</tt>.
     *
     * @return     a clone of this <tt>WhiteboardPoint</tt> instance.
     */
    @Override
    protected Object clone()
    {
        return new WhiteboardPoint(this);
    }

    /**
     * Calculates the distance from this point the given point.
     *
     * @param p the point to which to calculate the distance
     * @return the distance between this point and the given point
     */
    public double distance(WhiteboardPoint p)
    {
        double PX = p.getX() - this.getX();
        double PY = p.getY() - this.getY();

        return Math.sqrt(PX * PX + PY * PY);
    }
}
