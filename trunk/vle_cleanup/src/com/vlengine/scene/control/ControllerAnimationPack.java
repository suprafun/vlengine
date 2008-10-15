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

import com.vlengine.math.FastMath;
import com.vlengine.math.Vector3f;
import com.vlengine.scene.animation.Action;
import com.vlengine.scene.animation.x.XBoneAnimationController;
import com.vlengine.util.FastList;

/**
 * Animation managing class for CharacterController and other Controller classes
 * One such class is used for one controller
 * @author vear (Arpad Vekas)
 */
public class ControllerAnimationPack {

    /**
     * Animation type definitions
     */
    public static enum AnimType {
        None(0),
        Idle(1),
        Walk(2),
        Run(3),
        StandingJump(4),
        RuningJump(5),
        StrafeLeft(6),
        StrafeRight(7),
        Falling(8),
        Landing(9)
        ;
        
        public final int animId;
        AnimType(int id) {
            animId = id;
        }
    }
    
    public static enum AnimLoopType {
        RandomLoop;
    }
    
    // the animations class
    public class Animation {
        public AnimType animType;
        public AnimLoopType loopType;
        
        public FastList<Action> animVersions = new FastList<Action>();
    }
    
    // animations, indexed by animtype
    protected FastList<Animation> animations = new FastList<Animation>();
    
    // the current active animation type
    protected AnimType animIndex = AnimType.None;
    
    protected XBoneAnimationController animController;
    
    protected Action currentAction = new Action();
    protected Action nextAction = new Action();
    protected Action continueAction = new Action();
    
    // the movement directions
    //protected Vector3f[] moveDirs;
    
    public ControllerAnimationPack() {
        
    }

    public void setAnimationController(XBoneAnimationController c) {
        animController = c;
    }
    
    //public void setMoveDirs(Vector3f[] dirs) {
    //    this.moveDirs = dirs;
    //}

    // TODO: remove this to load from XML definition file
    public void setupPC1Animations() {
        // this is hard-coded animation data of a character model
        
            // setup the animation actions
        Animation as;
        
        // idle animation
        as = new Animation();
        as.animType = AnimType.Idle;
        as.loopType = AnimLoopType.RandomLoop;
        
        // idle actions
        Action anim;
        
        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.ContinueNext;
        anim.id = 0;
        anim.rangeStart = 54;
        anim.rangeEnd = 113;
        anim.startFrame = 54;
        as.animVersions.add(anim);

        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.ContinueNext;
        anim.id = 0;
        anim.rangeStart = 117;
        anim.rangeEnd = 176;
        anim.startFrame = 117;
        as.animVersions.add(anim);
        animations.set(as.animType.animId, as);
        
        // walk forward
        as = new Animation();
        as.animType = AnimType.Walk;
        //as.loopType = AnimLoopType.RandomLoop;
        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.RestartAtEnd;
        anim.id = 0;
        anim.rangeStart = 0;
        anim.rangeEnd = 31;
        anim.startFrame = 0;
        as.animVersions.add(anim);
        animations.set(as.animType.animId, as);

        // run forward
        as = new Animation();
        as.animType = AnimType.Run;
        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.RestartAtEnd;
        anim.id = 0;
        anim.rangeStart = 32;
        anim.rangeEnd = 52;
        anim.startFrame = 32;
        as.animVersions.add(anim);
        animations.set(as.animType.animId, as);

        // falling
        as = new Animation();
        as.animType = AnimType.Falling;
        Action fallanim = new Action();
        fallanim.exclusiveGroup = 1;
        fallanim.loop = Action.LoopMode.StopAtEnd;
        fallanim.id = 0;
        fallanim.rangeStart = 200;
        fallanim.rangeEnd = 205;
        fallanim.startFrame = 200;
        as.animVersions.add(fallanim);
        animations.set(as.animType.animId, as);

        // landing
        as = new Animation();
        as.animType = AnimType.StandingJump;
        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.StopAtEnd;
        anim.id = 0;
        anim.rangeStart = 206;
        anim.rangeEnd = 221;
        anim.startFrame = 206;
        as.animVersions.add(anim);
        animations.set(as.animType.animId, as);
        
        // standing jump
        as = new Animation();
        as.animType = AnimType.StandingJump;
        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.ContinueNext;
        anim.id = 0;
        anim.rangeStart = 180;
        anim.rangeEnd = 200;
        anim.startFrame = 180;
        anim.nextAction = fallanim;
        as.animVersions.add(anim);
        animations.set(as.animType.animId, as);
        
        // runing jump
        as = new Animation();
        as.animType = AnimType.RuningJump;
        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.ContinueNext;
        anim.id = 0;
        anim.rangeStart = 224;
        anim.rangeEnd = 261;
        anim.startFrame = 224;
        anim.nextAction = fallanim;
        as.animVersions.add(anim);
        animations.set(as.animType.animId, as);
        
        // strafe left
        as = new Animation();
        as.animType = AnimType.StrafeLeft;
        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.RestartAtEnd;
        anim.id = 0;
        anim.rangeStart = 264;
        anim.rangeEnd = 293;
        anim.startFrame = 264;
        as.animVersions.add(anim);
        animations.set(as.animType.animId, as);

        // strafe right
        as = new Animation();
        as.animType = AnimType.StrafeRight;
        anim = new Action();
        anim.exclusiveGroup = 1;
        anim.loop = Action.LoopMode.RestartAtEnd;
        anim.id = 0;
        anim.rangeStart = 296;
        anim.rangeEnd = 325;
        anim.startFrame = 296;
        as.animVersions.add(anim);
        animations.set(as.animType.animId, as);

    }
    
    /**
     * Update the animation based on characters local space movement
     * @param speed
     */
    public void updateAnimation(Vector3f localMoveDir, boolean run, boolean jump, boolean fall, boolean land) {
        // determine the movement direction
        boolean forward = localMoveDir.z < -FastMath.ZERO_TOLERANCE;
        boolean backward = localMoveDir.z > FastMath.ZERO_TOLERANCE;
        boolean left = localMoveDir.x < -FastMath.ZERO_TOLERANCE;
        boolean right = localMoveDir.x > FastMath.ZERO_TOLERANCE;
        
        // determine the new move direction
        // choose animation
        // choose animation for the controller
        //int newAnimIndex = 0;
        AnimType newAnimType = AnimType.None;
        
        if(fall) {
            newAnimType = AnimType.Falling;
        } else if(land) {
            newAnimType = AnimType.Landing;
        } else
        // forward or backward?
        if(forward && !backward) {
            // forward
            if(run ) {
                if(jump) {
                    newAnimType = AnimType.RuningJump;
                } else {
                    newAnimType = AnimType.Run;
                }
            } else {
                if(jump) {
                    newAnimType = AnimType.StandingJump;
                } else {
                    newAnimType = AnimType.Walk;
                }
            }
        } else if(!forward && backward) {
            newAnimType = AnimType.Walk;
            // TODO: implement backward as playing the animation backward?
            // backward
            /*
            if(left && !right) {
                // back left
                newAnimIndex = 6;
            } else if(!left && right) {
                // back right
                newAnimIndex = 4;
            } else {
                // back
                newAnimIndex = 5;
            }
             */
        } else if(left && !right) {
            // left
            newAnimType = AnimType.StrafeLeft;
        } else if(!left && right) {
            // right
            newAnimType = AnimType.StrafeRight;
        } else if(jump) {
          // standing jump
            newAnimType = AnimType.StandingJump;
        } else {
            // idle
            //newAnimIndex = 0;
            newAnimType = AnimType.Idle;
        }
        // if new anim index does not match the old one
        if(newAnimType != animIndex) {
            // we need to change animation
            // stop he old animation
            if(currentAction.id != -1) {
                // stop the current action
                currentAction.nextAction = null;
                currentAction.loop = Action.LoopMode.StopAtEnd;
            }
            
            // TODO: maybe fix this so that old animation finishes?
            // get 
            Animation an = animations.get(newAnimType.animId);
            if(an != null) {
                // choose an action
                Action a = null;
                if(an.animVersions.size()>1 && an.loopType == AnimLoopType.RandomLoop) {
                    a = an.animVersions.get(FastMath.rand.nextInt(an.animVersions.size()));
                } else if(an.animVersions.size()>0) {
                    a = an.animVersions.get(0);
                }
                // create a copy from the model action to the next action
                if(a!=null) {
                    a.copy(nextAction);
                    // switch next an current
                    Action tmp = currentAction;
                    currentAction = nextAction;
                    nextAction = tmp;
                    animController.scheduleAction(currentAction);
                }
            }
            animIndex = newAnimType;
        }
        // check if we need to provide continuous random actions
        Animation an = animations.get(animIndex.animId);
        if(an != null && an.animVersions.size()>1 && an.loopType == AnimLoopType.RandomLoop) {
            // check if the current animation has no next set
            if(currentAction.nextAction==null) {

                // fill the next action with a new one
                int idx = FastMath.rand.nextInt(an.animVersions.size());
                an.animVersions.get(idx).copy(continueAction);
                currentAction.nextAction = continueAction;

                // swap current and continue
                //Action tmp = currentAction;
                //currentAction = continueAction;
                //continueAction = tmp;
            }
        }
    }
}
