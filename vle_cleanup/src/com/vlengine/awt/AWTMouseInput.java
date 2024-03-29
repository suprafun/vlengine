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

package com.vlengine.awt;

import java.awt.Canvas;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import com.vlengine.image.Image;
import com.vlengine.input.MouseInput;
import com.vlengine.util.BitSet;

/**
 * <code>AWTMouseInput</code>
 * 
 * @author Joshua Slack
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class AWTMouseInput extends MouseInput implements MouseListener, MouseWheelListener, MouseMotionListener {

    public static int WHEEL_AMP = 40;   // arbitrary...  Java's mouse wheel seems to report something a lot lower than lwjgl's

    private int currentWheelDelta;
    private int wheelDelta;
    private int wheelRotation;
    private boolean enabled = true;
    private boolean dragOnly = false;
    private BitSet buttons = new BitSet(3);

    private boolean virgin = true;
    private int dx, dy;
    private int lastX, lastY;

    /**
     * Swing events are put in here in the swing thread and removed from it in the update method.
     * To flatline memory usage the LinkedList could be replaced by two ArrayLists but then one
     * would need to synchronize insertions.
     */
    private List<MouseEvent> swingEvents = new LinkedList<MouseEvent>();

    public AWTMouseInput() {
        ;
    }

    public void destroyIfInitialized() {
        ; // ignore
    }

    public int getButtonIndex(String buttonName) {
        if ("MOUSE0".equalsIgnoreCase(buttonName)) {
                return 0;
        }
        else if ("MOUSE1".equalsIgnoreCase(buttonName)) {
                return 1;
        }
        else if ("MOUSE2".equalsIgnoreCase(buttonName)) {
                return 2;
        }

        throw new IllegalArgumentException("invalid buttonName: "+buttonName);
    }

    public boolean isButtonDown(int buttonCode) {
        return buttons.get(buttonCode-MouseInput.MOUSE_1);
    }

    public String getButtonName(int buttonIndex) {
        switch (buttonIndex) {
        case 0:
            return "MOUSE0";
        case 1:
            return "MOUSE1";
        case 2:
            return "MOUSE2";
        }
        throw new IllegalArgumentException("invalid buttonIndex: "+buttonIndex);
    }

    public int getWheelDelta() {
        return wheelDelta;
    }

    public int getXDelta() {
        return dx;
    }

    public int getYDelta() {
        return dy;
    }

    public int getXAbsolute() {
        return lastX;
    }

    public int getYAbsolute() {
        return lastY;
    }

    public void update() {
        int x = lastX;
        int y = lastY;

        if ( input != null ) {
            while ( !swingEvents.isEmpty() ) {
                MouseEvent event = swingEvents.remove( 0 );

                switch ( event.getID() ) {
                    case MouseEvent.MOUSE_DRAGGED:
                    case MouseEvent.MOUSE_MOVED:
                        x = event.getX();
                        y = event.getY();
                        dx = x - lastX;
                        dy = -(y - lastY);
                        lastX = x;
                        lastY = y;
                        if (virgin && (dx != 0 || dy != 0)) {
                            dx = dy = 0;
                            virgin = false;
                        }
                        input.onMove( dx, dy, x, y );
                        //x = event.getX();
                        //y = event.getY();
                        break;
                    case MouseEvent.MOUSE_PRESSED:
                    case MouseEvent.MOUSE_RELEASED:
                        input.onButton( getJMEButtonIndex( event ), event.getID() == MouseEvent.MOUSE_PRESSED, event.getX(), event.getY() );
                        break;
                    case MouseEvent.MOUSE_WHEEL:
                        input.onWheel( ((MouseWheelEvent)event).getUnitsToScroll()*WHEEL_AMP, event.getX(), event.getY() );
                        break;
                    default:
                }
            }
        } else {
            swingEvents.clear();
        }

        wheelDelta = currentWheelDelta;
        currentWheelDelta = 0;
    }

    public void setCursorVisible(boolean v) {
        ; // ignore
    }

    public boolean isCursorVisible() {
        // always true
        return true;
    }

    @Override
    public void setHardwareCursor(URL file) {
            ; // ignore
    }

    @Override
    public void setHardwareCursor(URL file, int xHotspot, int yHotspot) {
            ; // ignore
    }

    @Override
    public void setHardwareCursor(URL file, Image[] images, int[] delays, int xHotspot, int yHotspot) {
        ; // ignore
    }

    public int getWheelRotation() {
        return wheelRotation;
    }

    public int getButtonCount() {
        return 3;
    }

    /**
     * @return Returns the enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled The enabled to set.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return Returns the dragOnly.
     */
    public boolean isDragOnly() {
        return dragOnly;
    }

    /**
     * @param dragOnly The dragOnly to set.
     */
    public void setDragOnly(boolean dragOnly) {
        this.dragOnly = dragOnly;
    }

    // **********************************
    // java.awt.event.MouseListener methods
    // **********************************

    public void mouseClicked(MouseEvent arg0) {
        ; // ignore
    }

    public void mousePressed(MouseEvent arg0) {
        if (!enabled) {
                return;
        }
        //lastPoint.setLocation(arg0.getPoint());
        buttons.set( getJMEButtonIndex( arg0 ), true);
        swingEvents.add( arg0 );
    }

    private int getJMEButtonIndex( MouseEvent arg0 ) {
        int index;
        switch (arg0.getButton()) {
            default:
            case MouseEvent.BUTTON1: //left
                index = 0;
                break;
            case MouseEvent.BUTTON2: //middle
                index = 1;
                break;
            case MouseEvent.BUTTON3: //right
                index = 2;
                break;
        }
        return index;
    }

    public void mouseReleased(MouseEvent arg0) {
        if (!enabled) {
                return;
        }
        buttons.set(getJMEButtonIndex( arg0 ), false);
        swingEvents.add( arg0 );
    }

    public void mouseEntered(MouseEvent arg0) {
        ; // ignore for now
    }

    public void mouseExited(MouseEvent arg0) {
        ; // ignore for now
    }


    // **********************************
    // java.awt.event.MouseWheelListener methods
    // **********************************

    public void mouseWheelMoved(MouseWheelEvent arg0) {
        if (!enabled) {
                return;
        }

        final int delta = arg0.getUnitsToScroll() * WHEEL_AMP;
        currentWheelDelta -= delta;
        wheelRotation -= delta;

        swingEvents.add( arg0 );
    }


    // **********************************
    // java.awt.event.MouseMotionListener methods
    // **********************************

    public void mouseDragged(MouseEvent arg0) {
        if (!enabled) {
                return;
        }
        swingEvents.add( arg0 );
    }

    public void mouseMoved(MouseEvent arg0) {
        if (enabled && !dragOnly) {
                mouseDragged(arg0);
        }
    }

    @Override
    public void setCursorPosition(int x, int y) {
        lastX = x;
        lastY = y;
    	//absPoint.setLocation( x,y);
    }

    public void setGrabbed(boolean drag) {
        
    }
    
    /**
     * Set up a canvas to fire mouse events via the input system.
     * @param glCanvas canvas that should be listened to
     * @param dragOnly true to enable mouse input to jME only when the mouse is dragged
     */
    public void setup( Canvas glCanvas, boolean dragOnly ) {
        setEnabled( !dragOnly );
        setDragOnly( dragOnly );
        //setRelativeDelta( glCanvas );
        glCanvas.addMouseListener(this);
        glCanvas.addMouseWheelListener(this);
        glCanvas.addMouseMotionListener(this);
    }
}
