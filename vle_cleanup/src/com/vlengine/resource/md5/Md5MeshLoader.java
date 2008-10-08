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
import com.vlengine.model.MD5WeightedGeometry;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelMaterial;
import com.vlengine.resource.model.ModelMaterialPart;
import com.vlengine.resource.model.ModelPart;
import com.vlengine.resource.obj.Tokens;
import com.vlengine.scene.animation.Bone;
import com.vlengine.scene.animation.MD5.MD5BoneAnimation;
import com.vlengine.scene.animation.MD5.MD5BoneAnimationPack;
import com.vlengine.util.BufferInputStream;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.scene.animation.MD5.MD5AnimationFrame;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.Weight;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for loading MD5 mesh files.
 * TODO: Not checked
 * 
 * @author vear (Arpad Vekas)
 */
public class Md5MeshLoader {
    private static final Logger log = Logger.getLogger(Md5MeshLoader.class.getName());
    private static Quaternion baseRot = new Quaternion(-0.5f, -0.5f, -0.5f, 0.5f);
    
    private BufferedReader inFile;
    private Model obj;
    
    // what type of section we are working on curretly
    // 0-root
    // 1-joints
    // 2-mesh
    private int section = 0;
    
    //private Joint[] joints;
    private MD5BoneAnimationPack bindPoseAnim;
    private MD5AnimationFrame bindPoseTransform;

    // the current joint we are working on
    private int currentJoint = 0;
    
    // the current mesh we are working on
    private ModelPart currentModel;
    private ModelMaterial currentMaterial;
    private int startWeight[];
    private int numWeights[];
    private MD5WeightedGeometry currentGeom;
    private FloatBuffer currentTex0Buffer;
    private IndexBuffer currentIndices;
    protected Weight[] currentWeights;
    
    public Md5MeshLoader() {}
    
    public Model convert(ByteBuffer modelData, ParameterMap params) {
        
        try {
            // get a stream to our buffer
            InputStream format = new BufferInputStream(modelData);

            obj = new Model();

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
        Model mdl = obj;
        obj = null;
        section = 0;
        currentJoint = 0;
        bindPoseAnim = null;
        bindPoseTransform = null;
        return mdl;
    }
    
    protected void processLine(String line) {
        if (line == null || line.length() == 0) return;
        
        Tokens tokens = new Tokens(line, " ()\"");
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
            } else if("numJoints".equals(token)) {
                // number of joints to allocate
                if (tokens.isEmpty()) {
                    log.log(Level.SEVERE, "Unexpected end of line.");
                    return;
                }
                int numJoints = Integer.parseInt(tokens.pop());
                bindPoseAnim = new MD5BoneAnimationPack();
                MD5BoneAnimation bindPoseAnimOne = new MD5BoneAnimation();
                bindPoseAnim.addAnimation("bindPose", bindPoseAnimOne);
                // create transform frame
                bindPoseTransform=new MD5AnimationFrame();
                //bindPose.parent = new int[numJoints];
                //bindPose.name = new String[numJoints];
                bindPoseTransform.rotation = new Quaternion[numJoints];
                bindPoseTransform.translation = new Vector3f[numJoints];
                //joints = new Joint[numJoints];
                return;
            } else if("joints".equals(token)) {
                // we enter the joints section
                section = 1;
                return;
            } else if("mesh".equals(token)) {
                // enter section 2 for meshes
                currentModel = new ModelPart();
                section = 2;
                return;
            }
            return;
        } else if(section == 1) {
            if("}".equals(token)) {
                // exit this section
                section = 0;
                return;
            }
            // tokens for the joints section
            // example:
            //"origin"	-1 ( 0 0 0 ) ( -0.5 -0.5 -0.5 )		//comment
            Bone b = new Bone();
            b.name = token;
            //j.name = j.name.substring(1, j.name.length()-1);
            int par = Integer.parseInt(tokens.pop());
            if(par>-1)
                b.parent = bindPoseAnim.getBones().get(par);
            b.id = currentJoint;
            //bindPose.parent[currentJoint] = ;
            bindPoseAnim.getBones().set(currentJoint, b);
            // skip bracket
            //tokens.pop();
            // translation
            bindPoseTransform.translation[currentJoint] = new Vector3f();
            bindPoseTransform.translation[currentJoint].x = Float.parseFloat(tokens.pop());
            bindPoseTransform.translation[currentJoint].y = Float.parseFloat(tokens.pop());
            bindPoseTransform.translation[currentJoint].z = Float.parseFloat(tokens.pop());
            // skip bracket
            //tokens.pop();
            //tokens.pop();
            // rotation
            bindPoseTransform.rotation[currentJoint] = new Quaternion();
            bindPoseTransform.rotation[currentJoint].x = Float.parseFloat(tokens.pop());
            bindPoseTransform.rotation[currentJoint].y = Float.parseFloat(tokens.pop());
            bindPoseTransform.rotation[currentJoint].z = Float.parseFloat(tokens.pop());
            bindPoseTransform.rotation[currentJoint].computeW();
            // fix for rotation?
            //if(currentJoint==0)
            //    j.rotation.multLocal(baseRot);

            // add the joint to the list
            //joints[currentJoint] = j;
            currentJoint++;
            return;
        } else if(section == 2) {
            if("}".equals(token)) {
                // close the mesh section
                // add the mesh to finished
                obj.addPart(0, currentModel);
                currentModel = null;
                currentIndices = null;
                currentMaterial = null;
                currentGeom = null;
                currentTex0Buffer = null;
                startWeight = null;
                numWeights = null;
                currentWeights =null;
                section = 0;
                return;
            } else if("shader".equals(token)) {
                // handle it as submaterial
                if(currentMaterial==null) {
                    currentMaterial = new ModelMaterial();
                    currentModel.setMaterial(currentMaterial);
                }
                // shader "models/characters/male_npc/marine/marine"
                
                // create new texture as tga
                String tex = tokens.pop();
                tex = tex.substring(tex.lastIndexOf("/")+1);
                if(!tex.endsWith(".tga"))
                    tex+=".tga";
                ModelMaterialPart mpart = new ModelMaterialPart();
                mpart.setTextureName(tex);
                currentMaterial.addTexture(mpart);
                return;
            } else if("numverts".equals(token)) {
                int numVerts = Integer.parseInt(tokens.pop());
                // create geometry if not yet created
                currentGeom = new MD5WeightedGeometry();
                //currentGeom.setStartVertex(0);
                currentGeom.setNumVertex(numVerts);
                // create arrays
                this.startWeight = new int[numVerts];
                this.numWeights = new int[numVerts];
                // set the arrays into geom
                currentGeom.setStartWeightsArray(startWeight);
                currentGeom.setNumVertexWeightsArray(numWeights);
                currentGeom.setBindPose(bindPoseTransform);
                // create float buffer for texture coords 0
                VertexBuffer vb = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_TEXTURE0, numVerts);
                // add it to geom
                currentGeom.addAttribBuffer(vb, 0);
                currentTex0Buffer = vb.getDataBuffer();
                currentModel.setGeometry(currentGeom);
                return;
            } else if("vert".equals(token)) {
                // process a vertice
                int index = Integer.parseInt(tokens.pop());
                // skip bracket
                //tokens.pop();
                // texture coords
                currentTex0Buffer.position(index*2);
                currentTex0Buffer.put(Float.parseFloat(tokens.pop()));
                // invert v texcoord?
                currentTex0Buffer.put(1.0f-Float.parseFloat(tokens.pop()));
                // start weight, num weight
                startWeight[index] = Integer.parseInt(tokens.pop());
                numWeights[index] = Integer.parseInt(tokens.pop());
                return;
            } else if("numtris".equals(token)) {
                int numTris = Integer.parseInt(tokens.pop());
                // create index buffer
                currentIndices = IndexBuffer.createBuffer(numTris*3, currentGeom.getNumVertex(), null);
                // set index buffer into geometry
                currentGeom.setIndexBuffer(currentIndices);
                return;
            } else if("tri".equals(token)) {
                int index = Integer.parseInt(tokens.pop());
                currentIndices.position(index*3);
                // load triangles in proper OpenGL winding
                currentIndices.put(Integer.parseInt(tokens.pop()));
                int idx1 = Integer.parseInt(tokens.pop());
                currentIndices.put(Integer.parseInt(tokens.pop()));
                currentIndices.put(idx1);
                return;
            } else if("numweights".equals(token)) {
                // allocate weights arrays
                currentWeights = new Weight[Integer.parseInt(tokens.pop())];
                currentGeom.setWeightsArray(currentWeights);
                return;
            } else if("weight".equals(token)) {
                // weight 0 3 0.2993961573 ( 2.698390007 -3.0038146973 5.225025177 )
                int index = Integer.parseInt(tokens.pop());
                Weight w = new Weight();
                w.jointIndex = Integer.parseInt(tokens.pop());
                w.bias = Float.parseFloat(tokens.pop());
                // skip bracket
                //tokens.pop();
                w.translation.x = Float.parseFloat(tokens.pop());
                w.translation.y = Float.parseFloat(tokens.pop());
                w.translation.z = Float.parseFloat(tokens.pop());
                currentWeights[index] = w;
                return;
            }
            
        }
        
    }
}
