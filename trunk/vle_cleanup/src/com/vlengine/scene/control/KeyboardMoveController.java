/*
 * Copyright (c) 2008 VL Engine
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

import com.vlengine.input.InputListener;
import com.vlengine.input.InputSystem;
import com.vlengine.input.KeyBindingSet;
import com.vlengine.input.KeyInput;
import com.vlengine.math.Vector3f;
import com.vlengine.scene.Spatial;
import com.vlengine.system.VleException;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class KeyboardMoveController extends Controller {
    
    private Spatial source;
    private Spatial control;
    protected KeyBindingSet movekeys;
    private InputSystem input;

    private boolean[] press = new boolean[4];
    
    private Vector3f move = new Vector3f();
    private Vector3f rotmove = new Vector3f();
    
    private Vector3f[] movedir = new Vector3f[4];
    
    private float acceleration = 0;
    private float currentAcceleration = 0;
    
    public KeyboardMoveController( ) {
        super("key_move");
        // forward
        movedir[0] = new Vector3f(0,0,-1);
        // backward
        movedir[1] = new Vector3f(0,0,1);
        // left
        movedir[2] = new Vector3f(-1,0,0);
        // right
        movedir[3] = new Vector3f(1,0,0);
    }
    
    public void setAcceleration(float acceleration) {
        this.acceleration = acceleration;
    }
    
    public void setControlledNode(Spatial control) {
        this.control = control;
    }
    
    public void setRotationSource(Spatial source) {
        this.source = source;
    }
    
    public void setForward(Vector3f forward) {
        movedir[0].set(forward);
    }
    
    public void setBackward(Vector3f back) {
        movedir[1].set(back);
    }
    
    public void setLeft(Vector3f left) {
        movedir[2].set(left);
    }
    
    public void setRight(Vector3f right) {
        movedir[3].set(right);
    }
    
    public void setInputSystem( InputSystem is ) {
        if( input == null ) {
            this.input = is;
        } else {
            throw new VleException("InputSystem already set");
        }
    }
    
    public void setUpCommands( ) {
        if( movekeys != null ) return;
        movekeys = new KeyBindingSet( "move", "move" );
        movekeys.set( "move_forward", KeyInput.KEY_W, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                move_forward(parm, c, keyCode, pressed);
            }
        } );
        movekeys.set( "move_backward", KeyInput.KEY_S, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                move_backward(parm, c, keyCode, pressed);
            }
        } );
        movekeys.set( "move_left", KeyInput.KEY_A, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                move_left(parm, c, keyCode, pressed);
            }
        } );
        movekeys.set( "move_right", KeyInput.KEY_D, new InputListener() {

            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                move_right(parm, c, keyCode, pressed);
            }
        } );
        movekeys.setEnabled( true );
        input.addKeybindingSet( movekeys );
    }
    
    public void move_forward( String parm, char c, int keyCode, boolean pressed ) {
        press[0] = pressed;
        updateMove();
    }
    
    public void move_backward( String parm, char c, int keyCode, boolean pressed ) {
        press[1] = pressed;
        updateMove();
    }
    
    public void move_left( String parm, char c, int keyCode, boolean pressed ) {
        press[2] = pressed;
        updateMove();
    }
    
    public void move_right( String parm, char c, int keyCode, boolean pressed ) {
        press[3] = pressed;
        updateMove();
    }
    
    private void updateMove() {
        move.set(Vector3f.ZERO);
        for(int i=0; i<4; i++) {
            if(press[i]) {
                move.scaleAdd(speed, movedir[i], move);
            }
        }
    }
    
    @Override
    public void update(UpdateContext ctx) {
        if( ! move.equals(Vector3f.ZERO)) {
            // transform move by the rotation
            source.getLocalRotation().mult(move, rotmove);
            currentAcceleration += acceleration*ctx.time;
            rotmove.multLocal(ctx.time*(1+currentAcceleration));
            control.getLocalTranslation().addLocal(rotmove);
        } else {
            currentAcceleration = 0;
        }
    }
    
}
