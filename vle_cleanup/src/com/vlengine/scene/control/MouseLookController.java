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
import com.vlengine.input.MouseInput;
import com.vlengine.math.FastMath;
import com.vlengine.math.Matrix3f;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.scene.Spatial;
import com.vlengine.system.DisplaySystem;
import com.vlengine.system.VleException;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class MouseLookController extends Controller {
    
    private Spatial control;
    protected KeyBindingSet mousekeys;
    private InputSystem input;
    
    private boolean hasPrev = false;
    private final Quaternion prevrot=new Quaternion();
    private final Quaternion orig=new Quaternion();
    private final Quaternion crot=new Quaternion();
    
    private final Matrix3f incr = new Matrix3f();
    private final Matrix3f incrx = new Matrix3f();
    private final Matrix3f tempMa = new Matrix3f();
    private final Matrix3f tempMb = new Matrix3f();
    private final Vector3f tempVa = new Vector3f();
    private final float angles[]=new float[3];
    private final Vector3f upVect = new Vector3f(0,1,0);
    private final Vector3f leftVect = new Vector3f(1,0,0);
    private final Vector3f tmpVect = new Vector3f(-1,0,0);
    
    float xturn = 0;
    float yturn = 0;
    
    float fxturn = 0;
    float fyturn = 0;
    
    float xspeed = 1.0f;
    float yspeed = 1.0f;
    
    protected MouseInput mouseInput;
    
    protected boolean isdragonly = false;
    protected int dragbutton = MouseInput.MOUSE_3;
    
    public MouseLookController(MouseInput mouseInput ) {
        super("mouse_look");
        this.mouseInput = mouseInput;
    }
    
    public void setDragOnly(boolean dragOnly) {
        isdragonly = dragOnly;
    }
    
    public void setDragButton(int button) {
        this.dragbutton = button;
    }
    
    public void setXSpeed(float xspeed) {
        this.xspeed = xspeed;
    }

    public void setYSpeed(float yspeed) {
        this.yspeed = yspeed;
    }
    
    public void setControlledNode(Spatial control) {
        this.control = control;
    }
    
    public void setInputSystem( InputSystem is ) {
        if( input == null ) {
            this.input = is;
        } else {
            throw new VleException("InputSystem already set");
        }
    }
    
    public void setUpCommands( ) {
        if( mousekeys != null ) return;
        mousekeys = new KeyBindingSet( "mouse_look", "mouse" );
        mousekeys.set( "mouse_look", MouseInput.MOUSE_MOVE, new InputListener() {
            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                mouse_look(parm, c, keyCode, pressed);
            }
        } );
        mousekeys.setEnabled( true );
        input.addKeybindingSet( mousekeys );
    }

    // handle input event
    public void mouse_look( String parm, char c, int keyCode, boolean pressed ) {
        // if drag only, and right mouse button is not down
        if(this.isdragonly && !mouseInput.isButtonDown(dragbutton))
                return;
        int x = mouseInput.getXDelta();
        int y = mouseInput.getYDelta();
        int dispx = DisplaySystem.getDisplaySystem().getWidth();
        int dispy = DisplaySystem.getDisplaySystem().getHeight();
        if(dispx==0 || dispy==0)
            return;
        xturn += - ( ((float)x) / ((float)dispx) )*speed*xspeed;
        yturn += ( ((float)y) / ((float)dispy) )*speed*yspeed;

        //System.out.println("x="+xturn+" y="+yturn);
    }
    
    @Override
    public void update( UpdateContext ctx ) {
        //if( xturn == 0 && yturn == 0 )
        //    return;
        
        if(!prevrot.equals(control.getLocalRotation())) {
            orig.set(control.getLocalRotation());
            float[] rangles = new float[3];
            orig.toAngles(rangles);
            fxturn = rangles[1];
            //orig.mult(leftVect,tmpVect);
            //yturn = orig.toAngleAxis(tmpVect);
            fyturn = rangles[0];
        }

        fxturn += xturn; xturn = 0;
        fyturn += yturn; yturn = 0;

        fxturn=FastMath.mod(fxturn, 2*FastMath.PI);
        //yturn=FastMath.mod(yturn, 2*FastMath.PI);
        if(fyturn>FastMath.HALF_PI-0.1f)
            fyturn = FastMath.HALF_PI-0.1f;
        if(fyturn<-FastMath.HALF_PI+0.1f)
            fyturn = -FastMath.HALF_PI+0.1f;

        /*
        else
            orig.set(control.getLocalRotation());
         */
        
        //control.getLocalRotation().getRotationColumn(0, tempVa);
        //tempVa.normalizeLocal();
        //if(xturn>FastMath.HALF_PI || xturn<-FastMath.HALF_PI)
        //    incr.fromAngleNormalAxis(yturn, rightVect);//*time
        //else
            //*time
                
        incrx.fromAngleNormalAxis(fxturn, upVect);
        incrx.mult(leftVect,tmpVect);
        incr.fromAngleNormalAxis(fyturn, tmpVect);
        
        incr.mult(incrx, incr);
        
        //control.getLocalRotation().fromRotationMatrix(
        //        incr.mult(control.getLocalRotation().toRotationMatrix(tempMa), tempMb));
        control.getLocalRotation().fromRotationMatrix(incr);
        control.getLocalRotation().normalize();

        
        //control.getLocalRotation().toAngles(angles);
        //if( (angles[0]>1.4f) || (angles[0]<-1.4f) ) {
        //    control.getLocalRotation().set(orig);
        //}
        //xturn = 0;
        //yturn = 0;
        prevrot.set(control.getLocalRotation());
        //this.hasPrev = true;
        //pxturn = xturn;
        //pyturn = yturn;
        //xturn = 0;
        //yturn = 0;
    }
}
