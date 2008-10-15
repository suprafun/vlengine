/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 * 
 * * Neither the name of 'VL Engine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.vlengine.input;

import com.vlengine.image.Image;
import java.net.URL;

/**
 * <code>MouseInput</code> defines an interface to communicate with the mouse
 * input device.
 * The status of spcific buttons can be queried via the {@link #isButtonDown}
 * method. Position data can be queried by various get methods.
 * For each button that is pressed or released as well as for movement of
 * mouse or wheel an event is generated which
 * can be received by InputListener and dispatched to {@link InputListener}, these are subsribed via
 * adding a KeyBindingSet to the InputSystem.
 * @author Mark Powell
 * @version $Id: MouseInput.java,v 1.25 2007/08/16 13:19:09 rherlitz Exp $
 */
public abstract class MouseInput {

    //private static MouseInput instance;
    
    protected InputSystem input;
    
    public static final int MOUSE_MOVE = 256;
    
    public static final int MOUSE_X = 257;
    
    public static final int MOUSE_Y = 258;

    public static final int MOUSE_WHEEL = 259;
            
    public static final int MOUSE_WHEELUP = 260;
    
    public static final int MOUSE_WHEELDOWN = 261;
    
    public static final int MOUSE_1 = 262;
    
    public static final int MOUSE_2 = 263;
    
    public static final int MOUSE_3 = 264;
            
    public void setInputSystem(InputSystem input) {
        this.input = input;
    }
    
    /**
     *
     * <code>getButtonIndex</code> gets the button code for a given button
     * name.
     * @param buttonName the name to get the code for.
     * @return the code for the given button name.
     */
    public abstract int getButtonIndex(String buttonName);

    /**
     *
     * <code>isButtonDown</code> returns true if a given button is pressed,
     * false if it is not pressed.
     * @param buttonCode the button code to check.
     * @return true if the button is pressed, false otherwise.
     */
    public abstract boolean isButtonDown(int buttonCode);

    /**
     *
     * <code>getButtonName</code> gets the button name for a given button
     * code.
     * @param buttonIndex the code to get the name for.
     * @return the name for the given button code.
     */
    public abstract String getButtonName(int buttonIndex);

    /**
     *
     * <code>getWheelDelta</code> gets the change in the mouse wheel.
     * @return the change in the mouse wheel.
     */
    public abstract int getWheelDelta();

    /**
     *
     * <code>getXDelta</code> gets the change along the x axis.
     * @return the change along the x axis.
     */
    public abstract int getXDelta();

    /**
     *
     * <code>getYDelta</code> gets the change along the y axis.
     * @return the change along the y axis.
     */
    public abstract int getYDelta();

    /**
     *
     * <code>getXAbsolute</code> gets the absolute x axis value.
     * @return the absolute x axis value.
     */
    public abstract int getXAbsolute();

    /**
     *
     * <code>getYAbsolute</code> gets the absolute y axis value.
     * @return the absolute y axis value.
     */
    public abstract int getYAbsolute();

    /**
     * Updates the state of the mouse (position and button states). Invokes event listeners synchronously.
     */
    public abstract void update();

    /**
     * <code>setCursorVisible</code> sets the visiblity of the hardware cursor.
     * @param v true turns the cursor on false turns it off
     */
    public abstract void setCursorVisible(boolean v);

    /**
     * <code>isCursorVisible</code>
     * @return the visibility of the hardware cursor
     */
    public abstract boolean isCursorVisible();

    /**
     * <code>setHardwareCursor</code> sets the image to use for the hardware cursor.
     * @param file URL to cursor image
     */
    public abstract void setHardwareCursor(URL file);

    /**
     * <code>setHardwareCursor</code> sets the image and hotspot position to use for the hardware cursor.
     * @param file URL to cursor image
     * @param xHotspot Cursor X hotspot position
     * @param yHotspot Cursor Y hotspot position
     */
    public abstract void setHardwareCursor(URL file, int xHotspot, int yHotspot);

    /**
     * This method will set an animated harware cursor.
     * 
     * @param file
     *            in this method file is only used as a key for cursor cashing
     * @param images
     *            the animation frames
     * @param delays
     *            delays between changing each frame
     * @param xHotspot
     *            from image left
     * @param yHotspot
     *            from image bottom
     */
    public abstract void setHardwareCursor(URL file, Image[] images,
            int[] delays, int xHotspot, int yHotspot);

	/**
     * Destroy the input if it was initialized.
     */
    public abstract void destroyIfInitialized();

    /**
     * @return absolte wheel rotation
     */
    public abstract int getWheelRotation();

    /**
     * @return number of mouse buttons
     */
    public abstract int getButtonCount();
    
    public abstract void setCursorPosition( int x, int y);
    
    public String getName(int id) {
        String name;
        switch(id) {
            case MOUSE_X : name = "MOUSE_X"; break;
            case MOUSE_Y : name = "MOUSE_Y"; break;
            case MOUSE_WHEELUP : name = "MOUSE_WHEELUP"; break;
            case MOUSE_WHEELDOWN : name = "MOUSE_WHEELDOWN"; break;
            case MOUSE_1 : name = "MOUSE_1"; break;
            case MOUSE_2 : name = "MOUSE_2"; break;
            case MOUSE_3 : name = "MOUSE_3"; break;
        default: name = "UNKNOWN"; break;
        }
        return name;
    }
    
    public int getKeyIndex(String name) {
        int id=-1;
        if( name.equals("MOUSE_MOVE")) {
            id = MOUSE_MOVE;
        } else if( name.equals("MOUSE_X")) {
            id = MOUSE_X;
        } else if( name.equals("MOUSE_Y")) {
            id = MOUSE_Y;
        } else if( name.equals("MOUSE_WHEEL")) {
            id = MOUSE_WHEEL;
        } else if( name.equals("MOUSE_WHEELUP")) {
            id = MOUSE_WHEELUP;
        } else if( name.equals("MOUSE_WHEELDOWN")) {
            id = MOUSE_WHEELDOWN;
        } else if( name.equals("MOUSE_1")) {
            id = MOUSE_1;
        } else if( name.equals("MOUSE_2")) {
            id = MOUSE_2;
        } else if( name.equals("MOUSE_3")) {
            id = MOUSE_3;
        }
        return id;
    }
    
    public abstract void setGrabbed(boolean grabbed);
}
