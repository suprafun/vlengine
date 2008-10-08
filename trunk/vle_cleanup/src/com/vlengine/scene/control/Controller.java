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

package com.vlengine.scene.control;

/**
 *
 * @author vear (Arpad Vekas)
 */
public abstract class Controller {

    protected String name;
    
    /**
     * The 'speed' of this Controller. Genericly, less than 1 is slower, more
     * than 1 is faster, and 1 represents the base speed
     */
    protected float speed = 1;

    /**
     * True if this controller is active, false otherwise
     */
    protected boolean active = true;

    
    public Controller( String name ) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the speed of this controller. Speed is 1 by default.
     * 
     * @return
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Sets the speed of this controller
     * 
     * @param speed
     *            The new speed
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Sets the active flag of this controller. Note: updates on controllers are
     * still called even if this flag is set to false. It is the responsibility
     * of the extending class to check isActive if it wishes to be
     * turn-off-able.
     * 
     * @param active
     *            The new active state.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns if this Controller is active or not.
     * 
     * @return True if this controller is set to active, false if not.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Defined by extending classes, <code>update</code> is a signal to
     * Controller that it should update whatever object(s) it is controlling.
     */
    public abstract void update(UpdateContext ctx);
    
}
