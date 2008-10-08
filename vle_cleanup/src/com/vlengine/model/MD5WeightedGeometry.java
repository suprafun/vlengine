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

package com.vlengine.model;

import com.vlengine.bounding.BoundingBox;
import com.vlengine.math.Matrix3f;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import com.vlengine.scene.animation.MD5.MD5AnimationFrame;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.Weight;
import java.nio.FloatBuffer;

/**
 * Geometry composed of weights (aka MD5 format mesh),
 * note that it is valid for such a mesh not to have
 * position vertex data (ouch!).
 * @author vear (Arpad Vekas)
 */
public class MD5WeightedGeometry extends Geometry {
    // the current joints of the mesh
    protected MD5AnimationFrame bindPose;
    // the weights of the mesh
    // TODO: this may need to be converted to buffer
    protected Weight[] weights;
    
    // TODO: convert this to attribute buffer, once known how GPU
    // is best to handle it
    // the start weights for each vertex
    protected int startWeight[];
    // the number of weights for each vertex
    protected int numWeights[];
    
    // runtime data
    // TODO: put it into thread context?
    // the actual joint transformation matrices
    protected MD5AnimationFrame transformed = new MD5AnimationFrame();
    //protected Quaternion[] jointRotation;
    //protected Vector3f[] jointTranslation;
    // the transformed weight positions
    protected Vector3f[] transformedWeight;
    
    protected Vector3f tempStore = new Vector3f();

    public void setBindPose(MD5AnimationFrame joints) {
        this.bindPose = joints;
    }
    
    public void setStartWeightsArray(int[] startWeights) {
        this.startWeight = startWeights;
    }
    
    public void setNumVertexWeightsArray(int[] numWeights) {
        this.numWeights = numWeights;
    }
    
    public void setWeightsArray(Weight[] weightArray) {
        this.weights = weightArray;
    }
    
    /**
     * Creates a geometry object that can represent an animated frame
     * of this weighted geometry. This basicaly copyes
     * static data in this geometry to the other. After calling this method
     * applyJoints can be called on target.
     * @param target
     */
    public Geometry createFrameGeom(Geometry target) {
        if(target== null || target==this)
            target = new Geometry();
        // copy over all the attributes
        target.getBuffers().clear();
        FastList<VertexBuffer> buffers = getBuffers();
        IntList startvert = getBufferStartVertex();
        for(int i=0; i<buffers.size(); i++) {
            target.addAttribBuffer(buffers.get(i), startvert.get(i));
        }
        // dont copy start vertex, it
        //target.setStartVertex(this.getStartVertex());
        // copy over pointers
        target.setNumVertex(this.getNumVertex());
        target.setIndexBuffer(this.getIndexBuffer());
        target.setStartIndex(this.getStartIndex());
        target.setNumIndex(this.getNumIndex());
        if(target.getModelBound()==null) {
            target.setModelBound(new BoundingBox());
        }
        return target;
    }

    /**
     * This method transforms joints and weights using the
     * bindPose transform.
     */
    public void jointTransform() {
        if (transformed.rotation==null) {
            transformed.rotation = new Quaternion[bindPose.rotation.length];
            for(int i=0; i<bindPose.rotation.length; i++) {
                transformed.rotation[i] = new Quaternion();
            }
        }
        if(transformed.translation==null) {
            transformed.translation = new Vector3f[bindPose.translation.length];
            for(int i=0; i<bindPose.translation.length; i++) {
                transformed.translation[i]= new Vector3f();
            }
        }
        
        // bindpose is already transformaed, only copy
        for(int i=0; i<bindPose.translation.length; i++) {
            transformed.rotation[i].set(bindPose.rotation[i]);
            transformed.translation[i].set(bindPose.translation[i]);
        }
         
        /*
        // go trough all the joints tree, and transform child joints
        for(int i=0; i<joints.length; i++) {
            if(jointRotation[i]==null)
                jointRotation[i] = new Quaternion();
            if(jointTranslation[i]==null)
                jointTranslation[i]= new Vector3f();
            if( joints[i].parentIndex >= 0) {
                jointRotation[i].set(jointRotation[joints[i].parentIndex]);
                jointTranslation[i].set(jointTranslation[joints[i].parentIndex]);
            } else {
                jointRotation[i].set(0, 0, 0, 1);
                jointTranslation[i].set(0, 0, 0);
            }
            jointTranslation[i].addLocal(jointRotation[i].mult(joints[i].translation,tempStore));
            jointRotation[i].multLocal(joints[i].rotation);

        }
         */
        // transform weights
        if(transformedWeight==null)
            transformedWeight = new Vector3f[weights.length];
        for(int i=0; i<weights.length; i++) {
            if(transformedWeight[i]==null)
                transformedWeight[i] = new Vector3f();
            transformed.rotation[weights[i].jointIndex].mult(weights[i].translation,transformedWeight[i]);
            transformedWeight[i].addLocal(transformed.translation[weights[i].jointIndex]);
        }
    }

    /**
     * Transforms weights based on an animation frames joint transforms, after this
     * a geometry holding positions can be constructed using apply method.
     * @param jointParent       the array holding indices of joints parents
     * @param frameTranslation  the array of translations for each joint
     * @param frameRotation     the array of rotations for each joints
     */
    public void frameTransformJoints(int[] jointParent, Vector3f[] frameTranslation, Quaternion[] frameRotation) {
        // 
        if (transformed.rotation==null || jointParent.length != transformed.rotation.length) {
            transformed.rotation = new Quaternion[jointParent.length];
        }
        if(transformed.translation==null || jointParent.length != transformed.translation.length) {
            transformed.translation = new Vector3f[jointParent.length];
        }
        // go trough all the joints tree, and transform child joints
        for(int i=0; i<jointParent.length; i++) {
            if(transformed.rotation[i]==null)
                transformed.rotation[i] = new Quaternion();
            if(transformed.translation[i]==null)
                transformed.translation[i]= new Vector3f();
            if( jointParent[i] >= 0) {
                transformed.rotation[i].set(transformed.rotation[jointParent[i]]);
                transformed.translation[i].set(transformed.translation[jointParent[i]]);
            } else {
                transformed.rotation[i].set(0, 0, 0, 1);
                transformed.translation[i].set(0, 0, 0);
            }
            transformed.translation[i].addLocal(transformed.rotation[i].mult(frameTranslation[i],tempStore));
            transformed.rotation[i].multLocal(frameRotation[i]);
        }
        // transform weights
        if(transformedWeight==null)
            transformedWeight = new Vector3f[weights.length];
        for(int i=0; i<weights.length; i++) {
            if(transformedWeight[i]==null)
                transformedWeight[i] = new Vector3f();
            transformed.rotation[weights[i].jointIndex].mult(weights[i].translation,transformedWeight[i]);
            transformedWeight[i].addLocal(transformed.translation[weights[i].jointIndex]);
        }
    }
    
    /**
     * Applyes transforms in the weights. This method created a
     * vertex position buffer if it doesnt already exists in target.
     * This method is used to create a keyframe of the animated mesh.
     * @param target    Where to put the final geometry
     *                  createFrameGeom should be called once before this method
     */
    public void apply(Geometry target) {
        // save position buffer
       VertexBuffer vb = target.getAttribBuffer(VertexAttribute.USAGE_POSITION);

        // transform vertices
        // get vertex buffer from target, it must be a single buffer (not interleaved)
        if(vb == null ) {
            // normaly this should not happen, animated geomtry should be set up using
            // mapped buffers (BaseGeometry.VBO_PRELOAD) before the buffers are to be filled here 
            vb = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_POSITION, this.getNumVertex());
            target.addAttribBuffer(vb, 0);
            target.setNumVertex(this.getNumVertex());
        }
        // get the data buffer
        FloatBuffer fb = vb.getDataBuffer();
        fb.clear();
        // calculate min and max extents
        float minX=0;
        float maxX=0;
        float minY=0;
        float maxY=0;
        float minZ=0;
        float maxZ=0;

        // go over vertices and calculate final position
        for(int i=0; i<startWeight.length; i++) {
            // clear position
            tempStore.zero();
            // go over all influencing weights
            int wpos = 0;
            for(int j=0; j<numWeights[i]; j++) {
                // add (scaled) all transformed weight positions
                wpos = startWeight[i]+j;
                tempStore.addScaledLocal(transformedWeight[wpos], weights[wpos].bias);
            }
            // put into buffer
            fb.put(tempStore.x);
            fb.put(tempStore.y);
            fb.put(tempStore.z);
            if(i==0) {
                minX=maxX=tempStore.x;
                minY=maxY=tempStore.y;
                minZ=maxZ=tempStore.z;
            } else {
                if(tempStore.x<minX)
                    minX=tempStore.x;
                if(tempStore.x>maxX)
                    maxX=tempStore.x;
                if(tempStore.y<minY)
                    minY=tempStore.y;
                if(tempStore.y>maxY)
                    maxY=tempStore.y;
                if(tempStore.z<minZ)
                    minZ=tempStore.z;
                if(tempStore.z>maxZ)
                    maxZ=tempStore.z;
            }
        }
        // finished creating positions
        fb.rewind();
        // set the bounding box for the geom
        BoundingBox bb = (BoundingBox) target.getModelBound();
        bb.getCenter().set((maxX+minX)/2, (maxY+minY)/2, (maxZ+minZ)/2);
        bb.xExtent = (maxX-minX)/2;
        bb.yExtent = (maxY-minY)/2;
        bb.zExtent = (maxZ-minZ)/2;
    }
}
