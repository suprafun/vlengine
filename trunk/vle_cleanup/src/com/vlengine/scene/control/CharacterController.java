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

import com.vlengine.app.AppContext;
import com.vlengine.bounding.BoundingBox;
import com.vlengine.bounding.BoundingSphere;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.bounding.CollisionTreeManager;
import com.vlengine.input.InputListener;
import com.vlengine.input.InputSystem;
import com.vlengine.input.KeyBindingSet;
import com.vlengine.input.KeyInput;
import com.vlengine.input.MouseInput;
import com.vlengine.intersection.CollisionData;
import com.vlengine.intersection.CollisionResults;
import com.vlengine.intersection.PickData;
import com.vlengine.intersection.PickResults;
import com.vlengine.math.FastMath;
import com.vlengine.math.Matrix3f;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Ray;
import com.vlengine.math.Vector3f;
import com.vlengine.scene.CameraNode;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.Spatial;
import com.vlengine.util.FastList;

/**
 * A third-person camera and character controller
 * TODO: remove the sub-optimal collision detection stuff
 * and do collision detection with a proper physics library
 * @author vear (Arpad Vekas)
 */
public class CharacterController extends Controller {

    // the application context
    protected AppContext app;

    // the keybindings we handle
    // character moving
    protected KeyBindingSet movekeys;
    // which of the directions is pressed
    protected boolean[] press = new boolean[4];
    // is the run key (shift pressed)
    protected boolean runpressed = false;
    protected boolean running = false;
    // is jump pressed
    protected boolean jumppressed = false;
    protected boolean jumping = false;
    protected boolean falling = false;
    protected boolean landed = false;
    
    // moving directions
    private Vector3f[] movedir = new Vector3f[4];
    // the active move vector
    private Vector3f move = new Vector3f();
    // the rotated move
    private Vector3f rotmove = new Vector3f();
    private Vector3f origrotmove = new Vector3f();
    // the original position, returned to if cullision is detected
    private Vector3f origPos = new Vector3f();
    
    // the controlled node
    protected Spatial controlledNode;

    // the controlled camera node
    protected CameraNode controlledCamera;

    // the animation controller
    //protected XBoneAnimationController animController;
    // the animation actions for different move directions
    // idle + 8 directions
    //protected Action[] anim = new Action[9];
    // current directional animation index
    //protected int animIndex = -1;

    // the starting speed
    //protected float startSpeed = 40f;
    // the acceleration of the move (as multiplier)
    //protected float acceleration = 2f;
    // the current speed
    protected float currentSpeed = 0f;
    // the maximum speed allowed
    protected float walkSpeed = 60f;
    // the decceleration (as multiplier)
    //protected float decceleration = 0.5f;
    // max run speed
    protected float runSpeed = 110f;
    // the run acceleration
    //protected float runaccel = 4f;
    //protected float rundeccel = 0.8f;

    
    // mouse look controller stuff
    protected KeyBindingSet mousekeys;
    // is left pressed
    protected boolean leftMouse = false;
    protected boolean rightMouse = false;
    protected boolean forwardPressed = false;
    protected int wheelState = 0;
    
    // horizontal and vertical mouse look position
    float xturn = 0;
    float yturn = 0;
    
    float fxturn = 0;
    float fyturn = 0;
    
    float reffxturn = 0;
    
    float xspeed = 1.0f;
    float yspeed = 1.0f;

    private final Quaternion prevrot=new Quaternion();
    private final Quaternion orig=new Quaternion();
    private final Matrix3f incr = new Matrix3f();
    private final Matrix3f incrx = new Matrix3f();
    private final Vector3f upVect = new Vector3f(0,1,0);
    private final Vector3f leftVect = new Vector3f(1,0,0);
    private final Vector3f tmpVect = new Vector3f(-1,0,0);
    
    
    // the angle stepping used to equalize set rotation with character back rotation
    protected float equalizeStepAngle = 60f;
    // the prefered distance from character to camera
    protected float preferredDistance = 80f;
    // the current distance from character to the camera
    protected float currentDistance = 80f;
    protected float targetDistance = 80f;
    
    // the final rotation of the camera relative from back of the character
    protected Quaternion controlRot = new Quaternion();
    float[] angles = new float[3];
    float[] angles1 = new float[3];
    private final Quaternion camRot=new Quaternion();
    private final Quaternion camRot1=new Quaternion();
    protected Vector3f charHotspot = new Vector3f();
    protected Vector3f lookVect = new Vector3f();
    
    // mouse curso control
    protected int mouseX, mouseY;
    
    protected CollisionResults colres = new CollisionResults();
    
    // the animating class
    protected ControllerAnimationPack animPack;
    
    // the sound playing class
    protected ControllerSoundPack soundPack;
    
    // data for left right movement
    // original rotation
    protected Quaternion origRot;
    // left direction rotation
    protected Quaternion leftRot;
    // right direction rotation
    protected Quaternion rightRot;
    // left direction rotation
    protected Quaternion leftForwardRot;
    // right direction rotation
    protected Quaternion rightForwardRot;
    // backward
    protected Quaternion backRot;
    protected Quaternion leftBackRot;
    // right direction rotation
    protected Quaternion rightBackRot;
    
    // force applyed when jumping
    protected Vector3f jumpForce = new Vector3f(0,100,0);
    // the current jump force, sum of movement force and
    // and jump force, is decreased by frame time
    protected Vector3f currJumpForce = new Vector3f();
    protected Vector3f jumpStart = new Vector3f();

    Vector3f currForce = new Vector3f();
    float jumptime = 0.7f;
    float currjumptime = 0f;

    protected boolean hascollision = false;
    protected boolean hasfeetcollision = false;
    
    public CharacterController() {
        this("CharacterController");
    }

    public CharacterController(String name) {
        super(name);
        // setup move directions
        // forward
        movedir[0] = new Vector3f(0,0,-1);
        // backward
        movedir[1] = new Vector3f(0,0,1);
        // left
        movedir[2] = new Vector3f(-1,0,0);
        // right
        movedir[3] = new Vector3f(1,0,0);
    }

    public void setAppContext(AppContext app) {
        this.app = app;
    }

    public void setControlledNode(Spatial s) {
        controlledNode = s;
    }
    
    public void setControlledCamera(CameraNode camn) {
        controlledCamera = camn;
    }
    
    public void setAnimPack(ControllerAnimationPack animPack) {
        this.animPack = animPack;
    }
    
    public void setSoundPack(ControllerSoundPack soundPack) {
        this.soundPack = soundPack;
    }

    public void setupController() {
        // setup keybindings
        InputSystem input = app.inputSystem;
        // right mouse button
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
        movekeys.set( "press_run", KeyInput.KEY_LSHIFT, new InputListener() {
            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                press_run(parm, c, keyCode, pressed);
            }
        } );
        movekeys.set( "press_jump", KeyInput.KEY_SPACE, new InputListener() {
            public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                press_jump(parm, c, keyCode, pressed);
            }
        } );
        input.disableKeyBindingSetGroup("move");
        movekeys.setEnabled( true );
        input.addKeybindingSet( movekeys );
        
        // setup the camera move keys
        mousekeys = new KeyBindingSet( "mouse_look", "mouse" );
        // mouse move
        mousekeys.set( "mouse_look", MouseInput.MOUSE_MOVE, new InputListener() {
        public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                mouse_look(parm, c, keyCode, pressed);
            }
        } );
        // left click
        mousekeys.set( "mousebutton_left", MouseInput.MOUSE_1, new InputListener() {
        public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                mouse_left(parm, c, keyCode, pressed);
            }
        } );
        // right click
        mousekeys.set( "mousebutton_right", MouseInput.MOUSE_2, new InputListener() {
        public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                mouse_right(parm, c, keyCode, pressed);
            }
        } );
        // mouse wheel
        mousekeys.set( "mouse_wheeldown", MouseInput.MOUSE_WHEEL, new InputListener() {
        public void handleInput(String parm, char c, int keyCode, boolean pressed) {
                mouse_wheel(parm, c, keyCode, pressed);
            }
        } );

        input.disableKeyBindingSetGroup("mouse");
        mousekeys.setEnabled( true );
        input.addKeybindingSet( mousekeys );
    }

    public void removeController() {
        // remove our keybindings
        InputSystem input = app.inputSystem;
        input.removeKeybindingSet(movekeys);
        input.removeKeybindingSet(mousekeys);
    }
    
    protected void mouse_wheel( String parm, char c, int keyCode, boolean pressed ) {
        MouseInput mouseInput = app.mouseInput;
        wheelState -= mouseInput.getWheelDelta()/120f;
    }

    protected void mouse_left( String parm, char c, int keyCode, boolean pressed ) {
        leftMouse = pressed;
        MouseInput mouseInput = app.mouseInput;
        if(!rightMouse) {
            if(pressed) {
                // store current position
                mouseX = mouseInput.getXAbsolute();
                mouseY = mouseInput.getYAbsolute();
                mouseInput.setGrabbed(true);
            } else {
                // restore current postiion
                mouseInput.setGrabbed(false);
                mouseInput.setCursorPosition(mouseX, mouseY);
                //mouseInput.setCursorVisible(true);
            }
        }
        checkForward();
    }

    protected void mouse_right( String parm, char c, int keyCode, boolean pressed ) {
        rightMouse = pressed;
        MouseInput mouseInput = app.mouseInput;
        if(!leftMouse) {
            if(pressed) {
                // store current position
                mouseX = mouseInput.getXAbsolute();
                mouseY = mouseInput.getYAbsolute();
                //mouseInput.setCursorVisible(false);
                mouseInput.setGrabbed(true);
            } else {
                // restore current postiion
                mouseInput.setGrabbed(false);
                mouseInput.setCursorPosition(mouseX, mouseY);
                //mouseInput.setCursorVisible(true);
            }
        }
        checkForward();
    }

    protected void checkForward() {
        press[0] = forwardPressed || (leftMouse && rightMouse);
        updateMove();
    }
    
    protected void move_forward( String parm, char c, int keyCode, boolean pressed ) {
        forwardPressed = pressed;
        checkForward();
    }
    
    protected void move_backward( String parm, char c, int keyCode, boolean pressed ) {
        press[1] = pressed;
        updateMove();
    }
    
    protected void move_left( String parm, char c, int keyCode, boolean pressed ) {
        press[2] = pressed;
        updateMove();
    }
    
    protected void move_right( String parm, char c, int keyCode, boolean pressed ) {
        press[3] = pressed;
        updateMove();
    }
    
    protected void press_run( String parm, char c, int keyCode, boolean pressed ) {
        this.runpressed = pressed;
        updateMove();
    }
    
    protected void press_jump( String parm, char c, int keyCode, boolean pressed ) {
        this.jumppressed = pressed;
    }
    
    protected void updateMove() {
        if(jumping)
            return;
        move.set(Vector3f.ZERO);
        float dirs = 0f;
        
        for(int i=0; i<4; i++) {
            if(press[i]) {
                move.scaleAdd(speed, movedir[i], move);
                dirs ++;
            }
        }
        if(dirs > 1) {
            move.divideLocal(dirs);
        }
        if(runpressed && press[0] && !(press[1] || press[2] || press[3])) {
            running = true;
        } else {
            running = false;
        }
    }
    
    protected void mouse_look( String parm, char c, int keyCode, boolean pressed ) {
        // either left or right must be pressed
        if(!leftMouse && !rightMouse)
            return;
        MouseInput mouseInput = app.mouseInput;

        int x = mouseInput.getXDelta();
        int y = mouseInput.getYDelta();
        int dispx = app.display.getWidth();
        int dispy = app.display.getHeight();
        if(dispx==0 || dispy==0)
            return;
        xturn += - ( ((float)x) / ((float)dispx) )*speed*xspeed;
        yturn += ( ((float)y) / ((float)dispy) )*speed*yspeed;
    }

    protected boolean hasPressed() {
        return press[0] || press[1] || press[2] || press[3];
    }
    
    protected void updateCamera(UpdateContext ctx) {
        if(!prevrot.equals(controlRot)) {
            orig.set(controlRot);
            float[] rangles = new float[3];
            orig.toAngles(rangles);
            fxturn = rangles[1];
            //orig.mult(leftVect,tmpVect);
            //yturn = orig.toAngleAxis(tmpVect);
            fyturn = rangles[0];
        }

        fxturn -= xturn; xturn = 0;
        fyturn -= yturn; yturn = 0;

        fxturn=FastMath.mod(fxturn, 2*FastMath.PI);
        if(fxturn>180f*FastMath.DEG_TO_RAD)
            fxturn -= 360f*FastMath.DEG_TO_RAD;
        if(fxturn<-180f*FastMath.DEG_TO_RAD)
            fxturn += 360f*FastMath.DEG_TO_RAD;
        if(fyturn>FastMath.HALF_PI-0.1f)
            fyturn = FastMath.HALF_PI-0.1f;
        if(fyturn<-FastMath.HALF_PI+0.1f)
            fyturn = -FastMath.HALF_PI+0.1f;
                
        // if no mouse is pressed and a movement is pressed apply equalization on the rotation
        if(!leftMouse 
          && !rightMouse
          && !move.equals(Vector3f.ZERO)) {
            
            if(fxturn>reffxturn+FastMath.DEG_TO_RAD*180f)
                fxturn -= FastMath.DEG_TO_RAD*360f;
            if(fxturn<reffxturn) {
                fxturn += this.equalizeStepAngle*ctx.time*FastMath.DEG_TO_RAD;
                if(fxturn>reffxturn)
                    fxturn = reffxturn;
            } else if(fxturn>reffxturn) {
                fxturn -= this.equalizeStepAngle*ctx.time*FastMath.DEG_TO_RAD;
                if(fxturn<reffxturn)
                    fxturn = reffxturn;
            }
            if(fyturn<0f) {
                fyturn += this.equalizeStepAngle*ctx.time*FastMath.DEG_TO_RAD;
            }
        }
        
        
        /*
        incrx.fromAngleNormalAxis(fxturn, upVect);
        incrx.mult(leftVect,tmpVect);
        incr.fromAngleNormalAxis(fyturn, tmpVect);
        
        incr.mult(incrx, incr);
        
        controlRot.fromRotationMatrix(incr);
        controlRot.normalize();

        
        
        // rotate camera from relative from the character
        controlRot.toAngles(angles);
        // set angles back into rotation
        controlRot.fromAngles(angles);

        // store into prevrot
        prevrot.set(controlRot);
        // save it back into 
        
        
        
        // add 180 to y rotation
        //angles[1]= (angles[1]+FastMath.DEG_TO_RAD*180f)%(FastMath.DEG_TO_RAD*360f);
        // the rotation used to position the camera
        */
        angles[0] = FastMath.cos(fxturn);
        angles[1] = FastMath.sin(fyturn);
        angles[2] = FastMath.sin(fxturn);
        
        //camRot1.fromAngles(angles);
        //camRot.set(controlledNode.getLocalRotation()).multLocal(camRot1);
        //camRot.toAngles(angles);
         
        // has the camera dist been changed
        if(wheelState != 0) {
            if(wheelState>0) {
                if(preferredDistance<5f) {
                   preferredDistance = 5f; 
                }
            }
            preferredDistance *= (FastMath.pow(1.1f, wheelState));
            if(preferredDistance<5f) {
                preferredDistance = 5f;
            }
            if(preferredDistance>300f) {
                preferredDistance = 300f;
            }
            wheelState = 0;
        }
        
        charHotspot.set(controlledNode.getWorldTranslation());
        // also add the height of the character
        BoundingVolume bv = controlledNode.getWorldBound();
        if(bv!=null) {
            charHotspot.y = bv.getCenter().y;
            if(bv instanceof BoundingSphere) {
                charHotspot.y += ((BoundingSphere)bv).radius/2f;
            } else if(bv instanceof BoundingBox) {
                charHotspot.y += ((BoundingBox)bv).yExtent;
            }
        }
        
        // update camera distance based on collision
        updateCameraDistance(ctx);
        // the distance from character to camera
        float camDist = currentDistance;
        // TODO: check that camera can see the character, only consider static geometry for visibility
        // if not, then reduce the camera range
        
        // calculate the camera position based on character position
        // character rotation * controlRotation * UnitZ vector
        //camRot.multLocal(controlledNode.getLocalRotation());
        Vector3f camTrans = controlledCamera.getLocalTranslation();

        camTrans.x = camDist * angles[0];
        camTrans.y = camDist * angles[1];
        camTrans.z = camDist * angles[2];
        camTrans.addLocal(charHotspot);
         
        //camRot.multLocal(camTrans);
        
        // look the camera on the character
        lookVect.set(camTrans).subtractLocal(charHotspot);
        controlledCamera.getLocalRotation().lookAt(lookVect, Vector3f.UNIT_Y);
        
        // if we are looking up, then fix that
        if(camTrans.y < controlledNode.getWorldTranslation().y+5f) {
            camTrans.y = controlledNode.getWorldTranslation().y+5f;
            // reduce elevation towards 0
        }

        // if right mouse if pressed, then character rotation is the control rotation
        if(rightMouse) {
            //float charAngle = FastMath.mod((fxturn+(180f*FastMath.DEG_TO_RAD)), 2*FastMath.PI);
            //controlledNode.getLocalRotation().fromAngleAxis(charAngle, Vector3f.UNIT_Y);
            //fxturn = 0;
            lookVect.y = 0;
            controlledNode.getLocalRotation().lookAt(lookVect, Vector3f.UNIT_Y);
            /*
            controlledNode.getLocalRotation().toAngles(angles1);
            angles1[1] = fxturn;
            controlledNode.getLocalRotation().fromAngles(angles1);
             */
            reffxturn = fxturn;
        }
    }

    protected void updateSpeed(UpdateContext ctx) {
        if(jumping)
            return;
         if( ! move.equals(Vector3f.ZERO)) {
            if(press[0]) {
                //if(currentSpeed < startSpeed)
                 //   currentSpeed = startSpeed;
                if(running) {
                    currentSpeed = this.runSpeed;
                    //currentSpeed += currentSpeed*this.runaccel*ctx.time;
                } else {
                    currentSpeed = this.walkSpeed;
                    //currentSpeed += currentSpeed*acceleration*ctx.time;
                }
            } else if(press[1] || press[2] || press[3]) {
                currentSpeed = this.walkSpeed;
            } else {
                currentSpeed = 0;
                //currentSpeed = startSpeed;
            }
            /*
            if(running) {
                if(currentSpeed>this.runSpeed)
                    currentSpeed = runSpeed;
            } else {
                if(currentSpeed>walkSpeed) {
                    currentSpeed *= this.rundeccel;
                }
            }
             */
        } else {
             /*
            if(currentSpeed>walkSpeed) {
                currentSpeed *= this.rundeccel;
            } else {
                currentSpeed *= decceleration;
            }
              */
           currentSpeed = 0;
        }
    }

    Vector3f animMove = new Vector3f();
    Quaternion tmpQuat1 = new Quaternion();
    
    protected void updateAnimation() {
        
        boolean walk = press[0];
                
        // update animation
        if(animPack!=null) {
            if(!rotmove.equals(Vector3f.ZERO)) {
                animMove.set(move);
            } else {
                animMove.set(Vector3f.ZERO);
            }
            animPack.updateAnimation(animMove, running, jumping, ((currJumpForce.y < 0f) && jumping)|| falling || (!jumping && !hasfeetcollision), landed);
            
            boolean forward = animMove.z < -FastMath.ZERO_TOLERANCE;
            boolean backward = animMove.z > FastMath.ZERO_TOLERANCE;
            boolean left = animMove.x < -FastMath.ZERO_TOLERANCE;
            boolean right = animMove.x > FastMath.ZERO_TOLERANCE;
            
            walk = (forward != backward ) || (left != right );
            
            Quaternion spQuat = ((SetNode)controlledNode).getChild(0).getLocalRotation();
            
            if(!forward && !backward ) {
                if(left && ! right) {
                    // rotate the character left
                    spQuat.slerp(spQuat, leftRot, 0.5f);
                } else if(!left && right) {
                    // rotate the character right
                    spQuat.slerp(spQuat, rightRot, 0.5f);
                }
            } else if(forward && !backward && left != right) {
                if(left) {
                    // rotate the character left
                    spQuat.slerp(spQuat, leftForwardRot, 0.5f);
                } else if(right) {
                    // rotate the character right
                    spQuat.slerp(spQuat, rightForwardRot, 0.5f);
                }
            } else if(backward && !forward) {
                if(left != right) {
                    if(left) {
                        // rotate the character left
                        spQuat.slerp(spQuat, leftBackRot, 0.5f);
                    } else if(right) {
                        // rotate the character right
                        spQuat.slerp(spQuat, rightBackRot, 0.5f);
                    }
                } else {
                    spQuat.slerp(spQuat, backRot, 0.5f);
                }
            } else {
                // rotate the character back front
                spQuat.slerp(spQuat, origRot, 0.5f);// = spQuat.slerp(spQuat, origRot, 0.5f);
            }
        }
        
        
        // update sounds
        if(soundPack!=null) {
            soundPack.update(controlledNode, walk && !running, walk && running, landed);
        }
    }

    Vector3f tmpVec1 = new Vector3f();
    Vector3f tmpVec2 = new Vector3f();
    Vector3f tmpVec3 = new Vector3f();

    protected float terrainHeight = 0;
    protected float collisionHeight = 0;
    float ascended = 0;
    
    protected void updateCollision3(UpdateContext ctx) {
        // store old position
        origPos.set(controlledNode.getLocalTranslation());

        
        controlledNode.getLocalTranslation().addLocal(rotmove);
        /*
        float posy = controlledNode.getLocalTranslation().y;
        if(posy - ctx.time * 9.12f > terrainHeight) {
            controlledNode.getLocalTranslation().y -= ctx.time * 9.12f;
        } else if(posy < terrainHeight) {
            controlledNode.getLocalTranslation().y = terrainHeight;
        }
         */
        //controlledNode.getLocalTranslation().y = terrainHeight - 1;
        
        boolean recheck = true;
        boolean recheckedDescend = false;
        boolean recheckedAscend = false;
        boolean recheckedSlideX = false;
        boolean recheckedSlideZ = false;
        // does it have collision under the feet, use to check for jumping and landing
        hasfeetcollision = false;
        hascollision = false;

        // steep of a slope one can go up
        float stepHeight = 10f;
        int checkCount = 10;
        float offset = 0f;

        controlledNode.getLocalTranslation().y += offset;
        origrotmove.set(rotmove);

        while(recheck && checkCount > 0) {
            recheck = false;
            checkCount--;
            
            // since we check based on the world bound we will need to update this node before we can
            // check for collision
            controlledNode.updateGeometricState();

            // get collided objects
            colres.clear();
            colres.setCheckCollisionTree(false);
            colres.setCheckTriangles(false);
            colres.setCheckCollisionVolume(true);
            colres.setVolumeGetCollisionPoints(true);
            
            CollisionTreeManager ctm = CollisionTreeManager.getInstance();
            ctm.findCollisions(controlledNode, ctx.frame.getApp().getRootNode(), colres);

            // do we have a triangle collision
            boolean hasTriangleCollision = false;
            boolean hasDynamicCollision = false;
            // the height, calculated from the bottom of the model, where the collision occured
            float highestHeight = -1000;
            tmpVec1.zero();
            tmpVec2.zero();
            int numCols = 0;
            
            Spatial collidedSpat = null;
            Spatial collidedDynamic = null;
            
            float closestDynamic = Float.MAX_VALUE;
            
            // find collision with actual triangles, which is the closest
            FastList<CollisionData> cdat = colres.getResults();
            if(cdat!=null && cdat.size()>0) {
                // we got some collision, check the closest triagle collision
                for(int i=0, mi=cdat.size(); i<mi; i++) {
                    CollisionData cd = cdat.get(i);
                    Spatial dynSpat = cd.getTargetMesh();
                    if(dynSpat.getFlag(Spatial.Flag.Dynamic)) {
                        // check its distance to our center
                        float dist = dynSpat.getWorldBound().distanceTo(controlledNode.getWorldBound().getCenter());
                        if(dist < closestDynamic || collidedDynamic == null) {
                            hasDynamicCollision = true;
                            closestDynamic = dist;
                            collidedDynamic = dynSpat;
                        }
                    } else {
                        FastList<Vector3f> colpoints = cd.getCollisionPoints();
                        if(colpoints!= null && colpoints.size()>0) {
                            hasTriangleCollision = true;
                            // extract the heighest collision point
                            for(int j=0; j<colpoints.size(); j++) {
                                Vector3f colPoint = colpoints.get(j);
                                tmpVec2.addLocal(colPoint);
                                numCols++;
                                if(colPoint.y > highestHeight) {
                                    highestHeight = colPoint.y;
                                    tmpVec1.set(colPoint);
                                    collidedSpat = cd.getSourceMesh();
                                }
                            }
                        }
                    }
                }
                // get average collision position
                if(numCols>0) {
                    tmpVec2.divideLocal(numCols);
                }
            }

            if(hasDynamicCollision) {
                
                // get world bounds
                BoundingBox bb = (BoundingBox) controlledNode.getWorldBound();
                BoundingBox otherBB = (BoundingBox) collidedDynamic.getWorldBound();
                // get the vector between collided bounds
                if(bb.distanceInside(otherBB, tmpVec3)!=null) {
                    if(running) {
                        tmpVec3.multLocal(0.1f);
                    } else {
                        tmpVec3.multLocal(ctx.time);
                    }
                    // offset our spatial with the given distance
                    controlledNode.getParent().worldToLocal(tmpVec3, tmpVec3);
                    controlledNode.getLocalTranslation().subtractLocal(tmpVec3);
                }
            }
            
            if(hasTriangleCollision) {
                
                if(highestHeight-terrainHeight<stepHeight*2) {
                    hasfeetcollision = true;
                } else {
                    hascollision = true;
                }
                
                // get 1/3 of model height
                // TODO: maybe this is influenced by skill, on how
                if(!recheckedDescend) {
                    // if collision higher than a value, we cannot step on it, return
                    if(highestHeight-terrainHeight>stepHeight) {
                        if(!recheckedSlideX) {
                            // just return it to the previous position it was
                            // calculate vector from collision point to object
                            //float ml = rotmove.length();
                            //tmpVec2.subtractLocal(controlledNode.getWorldTranslation());
                            /*
                            if(FastMath.abs(tmpVec2.x) < FastMath.abs(tmpVec2.z)) {
                                rotmove.z = 0;
                            } else {
                                rotmove.x = 0;
                            }
                            //float angle = rotmove.angleBetween(tmpVec2);
                            //if(angle<0) {
                                
                            //} else {
                                
                            //}
                             
                            //rotmove.set(tmpVec2);
                            rotmove.y = 0;
                            //controlledNode.getLocalTranslation().set(origPos);
                             */
                            rotmove.set(origrotmove);
                            rotmove.x = 0;
                            controlledNode.getLocalTranslation().set(origPos).addLocal(rotmove);
                            recheck = true;
                            recheckedSlideX=true;
                        } else if(!recheckedSlideZ) {
                            rotmove.set(origrotmove);
                            rotmove.z = 0;
                            controlledNode.getLocalTranslation().set(origPos).addLocal(rotmove);
                            recheck = true;
                            recheckedSlideZ=true;
                        } else {
                            controlledNode.getLocalTranslation().set(origPos);
                            
                            // push the entity out from direction of the collision
                            BoundingBox bb = (BoundingBox) controlledNode.getWorldBound();
                            if(bb.distanceInside(tmpVec2, tmpVec3)!=null) {
                                if(running) {
                                    tmpVec3.multLocal(0.1f);
                                } else {
                                    tmpVec3.multLocal(ctx.time);
                                }
                                // offset our spatial with the given distance
                                controlledNode.getParent().worldToLocal(tmpVec3, tmpVec3);
                                controlledNode.getLocalTranslation().subtractLocal(tmpVec3);
                            }
                        }
                    } else {
                        //if(ascended <= stepHeight) {
                            //if(!recheckedAscend) {
                                controlledNode.getLocalTranslation().y += 1f;
                                        //(controlledNode.getLocalTranslation().y + highestHeight)/2f;
                                recheck = true;
                                recheckedAscend = true;
                                
                            //} else {
                            //    terrainHeight = controlledNode.getLocalTranslation().y;
                             //   ascended += 1;
                            //}
                        //}
                                //;
                        //collisionHeight = highestHeight - terrainHeight;
                        /*
                        if(highestHeight>terrainHeight+stepHeight) {
                            // if collision is below the value we can step onto
                            // change the height, as we step on

                            float elevation = stepHeight - highestHeight;
                            // add it to the position, and recheck
                            controlledNode.getLocalTranslation().y += 5;
                            collidedSpat.getWorldTranslation().y += 5;
                            //collidedSpat.updateWorldBound();
                            //recheck = true;
                        }
                         */
                    }
              }
                
                /*
                // get vector from center of object to the collision point
                
                // add it to the position
                // substract the bound (signed), from this vector

                BoundingVolume bound = controlledNode.getWorldBound();
                if(bound instanceof BoundingBox) {
                    BoundingBox bb = (BoundingBox) bound;
                    tmpVec2.set(tmpVec1).subtractLocal(bb.getCenter());
                    // substract the extents (signed)
                    if(tmpVec2.x < 0) {
                        tmpVec2.x += bb.xExtent;
                    } else if(tmpVec2.x > 0) {
                        tmpVec2.x -= bb.xExtent;
                    }
                    if(tmpVec2.y < 0) {
                        tmpVec2.y += bb.yExtent;
                    } else if(tmpVec2.y > 0) {
                        tmpVec2.y -= bb.yExtent;
                    }
                    if(tmpVec2.z < 0) {
                        tmpVec2.z += bb.zExtent;
                    } else if(tmpVec2.z > 0) {
                        tmpVec2.z -= bb.zExtent;
                    }
                    tmpVec2.normalizeLocal();
                    controlledNode.getLocalTranslation().addLocal(tmpVec2);
                }
                 */
                
                //controlledNode.getLocalTranslation().set(origPos);

            } else {
                if(recheckedAscend) {
                    terrainHeight = controlledNode.getLocalTranslation().y;
                }
                //ascended = 0;
                // if there is nothing below our legs, get the first collision with ray, cast below
                // fall to that height if something found
                // fall to height 0 if nothing found
                //controlledNode.getLocalTranslation().y = terrainHeight;
                // recheck with fall down
                if(!recheckedAscend || !recheckedDescend) {
                    if(!recheckedDescend) {
                        float desc = Math.max(Math.min(ctx.frame.getTimer().getTimePerFrame() * 10f, 4f),2f);
                        if(falling)
                            desc *= 2;
                        controlledNode.getLocalTranslation().y -= desc;
                        recheck=true;
                        recheckedDescend = true;
                    } else {
                        terrainHeight = controlledNode.getLocalTranslation().y;
                    }
                }// else {
                  //  terrainHeight = controlledNode.getLocalTranslation().y;
                //}
            }
        }
        controlledNode.getLocalTranslation().y = terrainHeight - offset;
        
        
    }

    PickResults pr = null;
    
    float lastclosetime = 0f;
            
    protected void updateCameraDistance(UpdateContext ctx) {
        // checks if view from camera is occluded
        // and reduces camera distance, so that the character is visible from camera
        
        Ray r = null;
        // cast a ray from character to the camera
        if(pr==null) {
            r = new Ray();
            pr = new PickResults(r);
        } else {
            r = pr.getRay();
        }
        pr.clear();
        
        // set origin to controleld node
        r.origin.set(charHotspot);
        //r.origin.addLocal(charHotspot);
        // set direction to look at camera
        r.direction.set(controlledCamera.getWorldTranslation()).subtractLocal(r.origin).normalizeLocal();
        //r.origin.addLocal(r.direction);
        //r.direction.normalizeLocal();
        
        // find bound pick
        pr.setCheckTriangles(false);
        pr.setCheckVolume(true);
        // calculate real distance
        tmpVec1.set(angles[0]*preferredDistance, angles[1]*preferredDistance, angles[2]*preferredDistance);
        float reallength = tmpVec1.length();
        pr.setRayLenght(reallength);
        
        CollisionTreeManager ctm = CollisionTreeManager.getInstance();
        ctm.findPick(ctx.frame.getApp().getRootNode(), pr);
        FastList<PickData> pd = pr.getPickData();
        float mindist = Float.MAX_VALUE;
        // find the closest pick
        for(int i=0; i<pd.size(); i++) {
            PickData targ = pd.get(i);
            if(targ.getTargetMesh() == controlledNode)
                continue;
            // ignore dinamic nodes
            if(targ.getTargetMesh().getFlag(Spatial.Flag.Dynamic))
                continue;
            float dist = targ.getDistance();
            if(dist < mindist) {
                mindist = dist;
            }
        }
        
        // multiply got distance with distance ratio
        mindist *= (preferredDistance/reallength);
        mindist -= 8f;
        
        if(mindist < preferredDistance) {
            targetDistance = mindist;
        } else {
            targetDistance = preferredDistance;
        }
        // calculate distance change per frame
        //float dispf = (currentDistance-targetDistance)*ctx.time*20f;
        if(currentDistance > targetDistance) {
                currentDistance = targetDistance;
        } /*else if(currentDistance < targetDistance - 20f) {
            currentDistance = targetDistance;
        } */else {
            currentDistance += (targetDistance-currentDistance)*ctx.time*20f;
        }
        /*
        // if its less, then lessen the current distance
        if(mindist < currentDistance) {
            targetDistance = mindist;
            if(targetDistance<5f) {
                targetDistance = 5f;
            }
            //lastclosetime = 0.2f;
        } else if(mindist < preferredDistance) {
            //if(lastclosetime<=0) {
                // interpolate to mindist
                targetDistance += currentDistance*200f*ctx.time;
                if(currentDistance>mindist) {
                    currentDistance = mindist;
                }
            //} else {
                //lastclosetime -= ctx.time;
            //}
        } else if(currentDistance < preferredDistance) {
            // interpolate to preferred distance
            currentDistance += currentDistance*200f*ctx.time;
        }
         */
        if(currentDistance < 5f) {
            currentDistance = 5f;
        }
        if(currentDistance > preferredDistance) {
            currentDistance = preferredDistance;
        }
    }
    
    protected void updateJump(UpdateContext ctx) {
        
        // check for jumping and landing
        if((falling || jumping) && ((hascollision && !hasfeetcollision)   
                || (hasfeetcollision && currJumpForce.y < FastMath.ZERO_TOLERANCE))
                ) {
            // we are jumping be we had a collision
            landed = true;
            jumping = false;
            falling = false;
            updateMove();
        }
        // do we initiat jump
        // we are not jumping already, jump is initiated, and we have something under our feet
        if(!jumping && jumppressed && hasfeetcollision) {
            currJumpForce.zero();
            if( currentSpeed > 0) {
                // transform move by the rotation
                controlledNode.getLocalRotation().mult(move, currJumpForce);
                currJumpForce.multLocal(1+currentSpeed);
            }
            // create the jump force
            currJumpForce.addLocal(this.jumpForce);
            //if(running) {
            //    currJumpForce.multLocal(3f);
            //} else {
            //    currJumpForce.multLocal(2f);
            //}
            jumpStart.set(controlledNode.getLocalTranslation());
            currjumptime = 0;
            jumping = true;
        }
        
        if(jumping && currjumptime<jumptime) {
            currjumptime += ctx.time;
            if(currjumptime>jumptime) {
                currjumptime = jumptime;
            }
            // we have a jump to apply
            currForce.set(currJumpForce).multLocal(currjumptime/jumptime);
            //currForce.set(jumpDestination).mult();
            controlledNode.getLocalTranslation().addLocal(currForce);
            
            // falling
            //if(!hasfeetcollision) {
                // substract gravity from force
            //    controlledNode.getLocalTranslation().y -= 9.12f * ctx.time;
            //}
            
            if(FastMath.sign(currJumpForce.x) == FastMath.sign(currJumpForce.x-currForce.x)) {
                currJumpForce.x -= currForce.x;
            } else {
                currJumpForce.x = 0;
            }
            //
            if(FastMath.sign(currJumpForce.y) == FastMath.sign(currJumpForce.y-currForce.y)) {
                currJumpForce.y -= currForce.y;
            } else {
                currJumpForce.y = 0;
            }
             
            if(FastMath.sign(currJumpForce.z) == FastMath.sign(currJumpForce.z-currForce.z)) {
                currJumpForce.z -= currForce.z;
            } else {
                currJumpForce.z = 0;
            }
             
        } else if(jumping) {
            //currjumptime = 0f;
            //jumping = false;
            falling = true;
        }
        
    }

    Vector3f origPos1 = new Vector3f();
    
    @Override
    public void update(UpdateContext ctx) {
        

        // check for original rotation
        if(origRot==null) {
            origRot = new Quaternion();
            origRot.set(((SetNode)controlledNode).getChild(0).getLocalRotation());
            Vector3f tmpax = new Vector3f(0,1,0);
            float angle = origRot.toAngleAxis(tmpax);
            
            // create left by rotation CW
            leftRot = new Quaternion();
            leftRot.fromAngleAxis(angle+FastMath.HALF_PI, tmpax);
            
            // create right by rotation CCW
            rightRot = new Quaternion();
            rightRot.fromAngleAxis(angle-FastMath.HALF_PI, tmpax);
            
            // create left by rotation CW
            leftForwardRot = new Quaternion();
            leftForwardRot.fromAngleAxis(angle+(FastMath.HALF_PI/2f), tmpax);
            
            // create right by rotation CCW
            rightForwardRot = new Quaternion();
            rightForwardRot.fromAngleAxis(angle-(FastMath.HALF_PI/2f), tmpax);
            
            // back
            backRot = new Quaternion();
            backRot.fromAngleAxis(angle+FastMath.PI, tmpax);
            
            leftBackRot = new Quaternion();
            leftBackRot.fromAngleAxis(angle+(FastMath.HALF_PI/2f)*3f, tmpax);
            
            // create right by rotation CCW
            rightBackRot = new Quaternion();
            rightBackRot.fromAngleAxis(angle-(FastMath.HALF_PI/2f)*3f, tmpax);
            
        }

        updateCamera(ctx);
        
        boolean wasjumping = jumping;
        
        //origPos1.set(controlledNode.getLocalTranslation());
        
        updateSpeed(ctx);
        
        
        
        if(!jumping) {
            
            // update location
            if( currentSpeed > 0) {
                // transform move by the rotation
                controlledNode.getLocalRotation().mult(move, rotmove);

                rotmove.multLocal(ctx.time*(1+currentSpeed));

            } else {
                rotmove.zero();
            }
        }
        
        
        updateAnimation();
        
        updateCollision3(ctx);
        
        // reset the landed flag
        landed =false;
        updateJump(ctx);
        
        //updateCameraDistance(ctx);
        //if(jumping && !wasjumping) {
        //    controlledNode.getLocalTranslation().set(origPos1);
        //    updateJump(ctx);
        //}
        
        updateCamera(ctx);
    }

}
