/*
 * Copyright (c) 2008 ChaosDeathFish and MD5 Reader 2 Team, 2008 VL Engine
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
 * * Neither the name of 'MD5 Reader 2 Team' nor the names of its contributors 
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

package com.vlengine.resource.md5;

import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.animation.MD5.MD5BoneAnimation;
import com.vlengine.resource.obj.Tokens;
import com.vlengine.util.BufferInputStream;
import com.vlengine.scene.animation.MD5.MD5AnimationFrame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads an Md5 animation file into BoneAnimation class
 * TODO: Animations are loaded, but the animation is not implemented
 * @author vear (Arpad Vekas)
 */
public class Md5AnimLoader {
    private static final Logger log = Logger.getLogger(Md5AnimLoader.class
            .getName());
    
    private BufferedReader inFile;    
    MD5BoneAnimation obj;
    
    // 0-root
    // 1-hierarchy
    // 2-bounds
    // 3-baseframe
    // 4-frame
    private int section = 0;
    
    // the current joint we are working on
    private int currentJoint = 0;
    
    // the current bound
    private int currentBound = 0;
    // the current transform for base frame
    private int currentTransform = 0;
    // the current frame
    private int currentFrame = 0;
    // the current transform data index for a frame
    private int currentFrameData = 0;

    public MD5BoneAnimation convert(ByteBuffer modelData, ParameterMap params) {
        
        try {
            // get a stream to our buffer
            InputStream format = new BufferInputStream(modelData);

            obj = new MD5BoneAnimation();

            inFile = new BufferedReader(new InputStreamReader(format));
            String in;

            while ((in = inFile.readLine()) != null) {
                processLine(in);
            }
            inFile.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
            obj = null;
        }
        inFile = null;
        MD5BoneAnimation mdl = obj;
        obj = null;
        section = 0;
        return mdl;
    }
    
    protected void processLine(String line) {
        if (line == null || line.length() == 0) return;
        
        Tokens tokens = new Tokens(line);
        if (tokens.isEmpty()) return;
        String token = tokens.pop();
        
        if(token.startsWith("//"))
            return; // comment
        // root level tokens
        if(section == 0) {
            if("MD5Version".equals(token)) {
                // should be 10
                if (tokens.isEmpty()) {
                    log.log(Level.SEVERE, "Unexpected end of line.");
                    return;
                }
                if(!tokens.pop().equals("10")) {
                    log.log(Level.SEVERE, "Unknown file version");
                    return;
                }
                return;
            } else if("commandline".equals(token)) {
                // command line, ignore
                return;
            } else if("numFrames".equals(token)) {
                obj.numFrames = Integer.parseInt(tokens.pop());
                // initialize transforms array for the frames
                obj.frame = new MD5AnimationFrame[obj.numFrames];
                return;
            } else if("numJoints".equals(token)) {
                obj.numJoints = Integer.parseInt(tokens.pop());
                // initialize arrays for holding joint data
                obj.jointName = new String[obj.numJoints];
                obj.jointParent = new int[obj.numJoints];
                obj.jointTransformMask = new int[obj.numJoints];
                obj.jointStartIndex = new int[obj.numJoints];
                return;
            } else if("frameRate".equals(token)) {
                obj.frameRate = Integer.parseInt(tokens.pop());
                return;
            } else if("numAnimatedComponents".equals(token)) {
                obj.numAnimatedComponents = Integer.parseInt(tokens.pop());
                return;
            } else if("hierarchy".equals(token)) {
                // enter section 1
                section = 1;
                // start counting joints
                currentJoint = 0;
                return;
            } else if("bounds".equals(token)) {
                // enter section 2
                section = 2;
                // start counting bounds
                currentBound = 0;
                // create bounds array to hold bound for each frame
                obj.boundMin = new Vector3f[obj.numFrames];
                obj.boundMax = new Vector3f[obj.numFrames];
                return;
            } else if("baseframe".equals(token)) {
                // enter section 3
                section = 3;
                // start counting bounds
                currentTransform = 0;
                // allocate transform arrays
                obj.baseFrame = new MD5AnimationFrame();
                //obj.baseTranslation = new Vector3f[obj.numJoints];
                //obj.baseRotatition = new Quaternion[obj.numJoints];
                return;
            } else if("frame".equals(token)) {
                // enter section 4
                section = 4;
                // start counting data
                currentFrameData = 0;
                // get index
                currentFrame = Integer.parseInt(tokens.pop());
                // allocate transform arrays
                obj.frame[currentFrame].translation = new Vector3f[obj.numJoints];
                obj.frame[currentFrame].rotation = new Quaternion[obj.numJoints];
                // fill in default data from baseframe
                for(int i=0; i<obj.numJoints;i++) {
                    obj.frame[currentFrame].translation[i] = new Vector3f().set(obj.baseFrame.translation[i]);
                    obj.frame[currentFrame].rotation[i] = new Quaternion().set(obj.baseFrame.rotation[i]);
                }
                return;
            }
            
        } else if(section == 1) {
            // hierarchy section
            if("}".equals(token)) {
                // exit section
                section = 0;
                return;
            }
            // create new joint
            //"origin"	-1 3 0	// ( Tx Ty )
            String jname = token;
            obj.jointName[currentJoint] = jname.substring(1, jname.length()-1);
            obj.jointParent[currentJoint] = Integer.parseInt(tokens.pop());
            obj.jointTransformMask[currentJoint] = Integer.parseInt(tokens.pop());
            obj.jointStartIndex[currentJoint] = Integer.parseInt(tokens.pop());
            if(obj.jointTransformMask[currentJoint]!=0) {
                // store joint number by data start index
                obj.transformJoint.ensureCapacity(obj.jointStartIndex[currentJoint]+1);
                obj.transformJoint.set(obj.jointStartIndex[currentJoint], currentJoint);
            }

            // increase joint number
            currentJoint++;
        } else if(section == 2) {
            // bound section
            if("}".equals(token)) {
                // exit section
                section = 0;
                return;
            }
            // create new bound (BoundingBox)
            //( -20.5229568481 -17.552526474 -2.1456007957 ) ( 26.4583339691 16.6007442474 68.7835159302 )
            // skip bracket
            // create min and max vector
            obj.boundMin[currentBound] = new Vector3f();
            obj.boundMax[currentBound] = new Vector3f();
            // read data
            // bound min
            obj.boundMin[currentBound].x = Float.parseFloat(tokens.pop());
            obj.boundMin[currentBound].y = Float.parseFloat(tokens.pop());
            obj.boundMin[currentBound].z = Float.parseFloat(tokens.pop());
            // bound max
            obj.boundMax[currentBound].x = Float.parseFloat(tokens.pop());
            obj.boundMax[currentBound].y = Float.parseFloat(tokens.pop());
            obj.boundMax[currentBound].z = Float.parseFloat(tokens.pop());
            // increment bound counter
            
            currentBound++;
        } else if(section == 3) {
            // baseframe section
            if("}".equals(token)) {
                // exit section
                section = 0;
                return;
            }
            // allocate translation and rotation
            obj.baseFrame.translation[currentTransform] = new Vector3f();
            obj.baseFrame.rotation[currentTransform] = new Quaternion();
            
            // ( 0 0 0 ) ( -0.5 -0.5 -0.5 )
            // skipped bracket
            // translation
            obj.baseFrame.translation[currentTransform].x = Float.parseFloat(tokens.pop());
            obj.baseFrame.translation[currentTransform].y = Float.parseFloat(tokens.pop());
            obj.baseFrame.translation[currentTransform].z = Float.parseFloat(tokens.pop());
            // skip backets
            //tokens.pop();
            //tokens.pop();
            // rotation
            obj.baseFrame.rotation[currentTransform].x = Float.parseFloat(tokens.pop());
            obj.baseFrame.rotation[currentTransform].y = Float.parseFloat(tokens.pop());
            obj.baseFrame.rotation[currentTransform].z = Float.parseFloat(tokens.pop());
            // fill-in missing w
            obj.baseFrame.rotation[currentTransform].computeW();
            
            // increment transform counter
            currentTransform++;
            return;
        } else if(section == 4) {
            // frame section
            if("}".equals(token)) {
                // exit section
                section = 0;
                return;
            }
            // get which joint we are putting data into
            int joint = obj.transformJoint.get(currentFrameData);
            // based on bitmask get data into proper places
            if((obj.jointTransformMask[joint]&(1))!=0) {
                // translation x
                obj.frame[currentFrame].translation[joint].x = Float.parseFloat(token);
                if(!tokens.isEmpty())
                    token = tokens.pop();
                currentFrameData++;
            }
            if((obj.jointTransformMask[joint]&(1<<1))!=0) {
                // translation y
                obj.frame[currentFrame].translation[joint].y = Float.parseFloat(token);
                if(!tokens.isEmpty())
                    token = tokens.pop();
                currentFrameData++;
            }
            if((obj.jointTransformMask[joint]&(1<<2))!=0) {
                // translation z
                obj.frame[currentFrame].translation[joint].z = Float.parseFloat(token);
                if(!tokens.isEmpty())
                    token = tokens.pop();
                currentFrameData++;
            }
            boolean computew = false;
            if((obj.jointTransformMask[joint]&(1<<3))!=0) {
                // rotation x
                obj.frame[currentFrame].rotation[joint].x = Float.parseFloat(token);
                computew = true;
                if(!tokens.isEmpty())
                    token = tokens.pop();
                currentFrameData++;
            }
            if((obj.jointTransformMask[joint]&(1<<4))!=0) {
                // rotation y
                obj.frame[currentFrame].rotation[joint].y = Float.parseFloat(token);
                computew = true;
                if(!tokens.isEmpty())
                    token = tokens.pop();
                currentFrameData++;
            }
            if((obj.jointTransformMask[joint]&(1<<5))!=0) {
                // rotation x
                obj.frame[currentFrame].rotation[joint].y = Float.parseFloat(token);
                computew = true;
                if(!tokens.isEmpty())
                    token = tokens.pop();
                currentFrameData++;
            }
            if(computew) {
                obj.frame[currentFrame].rotation[joint].computeW();
            }
        }
    }
}
