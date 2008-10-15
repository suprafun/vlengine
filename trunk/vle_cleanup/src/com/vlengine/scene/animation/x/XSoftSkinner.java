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

package com.vlengine.scene.animation.x;

import com.vlengine.bounding.BoundingVolume;
import com.vlengine.bounding.CollisionVolume;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Geometry;
import com.vlengine.model.XWeightedGeometry;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.Renderable;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Class that perform software skinning on a single batch
 * @author vear (Arpad Vekas)
 */
public class XSoftSkinner extends Geometry implements XAnimatedItem {
    // the geometry has to have all the attributes in different
    // buffers
    protected XWeightedGeometry model;
    
    // the current transformation matrices for the bones
    protected Matrix4f[] boneMats;

    protected boolean needsRefresh = true;
    
    ByteBuffer mappedBuffer = null;
    
    public void setMatrixValues(Matrix4f[] origBoneMats) {
        IntList boneMapping = model.getBoneMapping();
        if(boneMats == null || boneMats.length != boneMapping.size()) {
            boneMats = new Matrix4f[boneMapping.size()];
            for(int i=0; i<boneMats.length; i++) {
                boneMats[i] = new Matrix4f();
            }
        }
        for(int l=0, ml=boneMapping.size(); l<ml; l++) {
            // the bone-s mapped id for this animated item
            int origBone = boneMapping.get(l);
            //if(origBone!=0) {
                // TODO: just for testing
                //skinTransforms[origBone].loadIdentity();
                // if this bone is actualy required in the item
                boneMats[l].set(origBoneMats[origBone]);
                boneMats[l].transposeLocal();
            //}
        }
        needsRefresh = true;
    }

    public IntList getBoneMapping() {
        return model.getBoneMapping();
    }

    public XWeightedGeometry getModel() {
        return model;
    }

    public void setModel(XWeightedGeometry model) {
        this.model = model;
        // copy over stuff from the model
        // we need to transfer everything from the XWeightedGeometry, and need to create
        // an empty position VBO, with no VBO
        // go over the buffers of the model
        // TODO: this way of copying is not working if the attributes are interleaved!
        FastList<VertexAttribute> attribs = model.getAllAttributes(null);
        for(int i=0; i<attribs.size(); i++) {
            VertexAttribute va = attribs.get(i);
            if(va.type != VertexAttribute.USAGE_POSITION
               && va.type != VertexAttribute.USAGE_WEIGHTINDICES
               && va.type != VertexAttribute.USAGE_WEIGHTS
               && va.type != VertexAttribute.USAGE_NUMWEIGHTS
               && va.type != VertexAttribute.USAGE_NORMAL
               ) {
                VertexBuffer vbm = model.getAttribBuffer(va.type);
                // add the buffer as ours
                this.addAttribBuffer(vbm, model.getStartVertex(va.type));
            }
        }

        // copy over the index buffer from the model too
        this.setIndexBuffer(model.getIndexBuffer());
        this.setStartIndex(model.getStartIndex());
        this.setNumIndex(model.getNumIndex());
    }

    /**
     * Setup this skinner, this will require access to a render context,
     * since it is doing VBO mapping
     */
    protected void mapBuffers(RenderContext ctx) {
        // get the VBO id
        VertexBuffer vb = this.getAttribBuffer(VertexAttribute.USAGE_POSITION);
        VertexBuffer nb = this.getAttribBuffer(VertexAttribute.USAGE_NORMAL);
        if(vb==null) {
            
            // create vertex buffer for position data, and map it
            // TODO: this should be done in two steps, using deferredloader
            vb = ctx.getRenderer().allocVertexBuffer(
                    VertexFormat.getDefaultFormat(VertexFormat.setRequested(0, VertexAttribute.USAGE_POSITION))
                    , model.getNumVertex(), null);
            this.addAttribBuffer(vb, 0);
            
            // create vertex buffer for normal data
            nb = ctx.getRenderer().allocVertexBuffer(
                    VertexFormat.getDefaultFormat(VertexFormat.setRequested(0, VertexAttribute.USAGE_NORMAL))
                    , model.getNumVertex(), null);
            this.addAttribBuffer(nb, 0);
            
        }
        // map the ogl memory
        // TODO: in multithreading environment we need to maintain two sets of the buffer
        // switching to new one with each update
        ctx.getRenderer().mapVertexBuffer(vb);
        ctx.getRenderer().mapVertexBuffer(nb);
    }

    protected void unMapBuffers(RenderContext ctx) {
        VertexBuffer vb = this.getAttribBuffer(VertexAttribute.USAGE_POSITION);
        VertexBuffer nb = this.getAttribBuffer(VertexAttribute.USAGE_NORMAL);
        ctx.getRenderer().unMapVertexBuffer(vb);
        ctx.getRenderer().unMapVertexBuffer(nb);
    }
    
    public void skinMesh() {
        // get hte position buffer
        FloatBuffer pb = model.getAttribBuffer(VertexAttribute.USAGE_POSITION).getDataBuffer();
        pb.rewind();
        // get the normal buffer
        FloatBuffer nb = model.getAttribBuffer(VertexAttribute.USAGE_NORMAL).getDataBuffer();
        nb.rewind();
        
        // TODO: this may need to be reworked to vertexiterators
        // get matrix weights
        FloatBuffer wb = model.getAttribBuffer(VertexAttribute.USAGE_WEIGHTS).getDataBuffer();
        wb.rewind();
        // get matrix indices
        FloatBuffer ib = model.getAttribBuffer(VertexAttribute.USAGE_WEIGHTINDICES).getDataBuffer();
        ib.rewind();
        // get num indices
        FloatBuffer inb = model.getAttribBuffer(VertexAttribute.USAGE_NUMWEIGHTS).getDataBuffer();
        inb.rewind();
        
        // get the target position buffer
        FloatBuffer tpb = getAttribBuffer(VertexAttribute.USAGE_POSITION).getDataBuffer();
        tpb.rewind();

        // get the target normal buffer
        FloatBuffer tnb = getAttribBuffer(VertexAttribute.USAGE_NORMAL).getDataBuffer();
        tnb.rewind();

        // source vectors
        Vector3f sPosition = new Vector3f();
        Vector3f sNormal = new Vector3f();
        // matrix weights
        float[] matWeights = new float[4];
        
        Vector3f tvec = new Vector3f();

        // target vectors
        Vector3f tPosition = new Vector3f();
        Vector3f tNormal = new Vector3f();

        // go over every vertex
        for(int i=0, mi=this.getNumVertex(); i<mi; i++) {
            // rerieve source vectors
            sPosition.x = pb.get(); sPosition.y = pb.get(); sPosition.z = pb.get();
            sNormal.x = nb.get(); sNormal.y = nb.get(); sNormal.z = nb.get();
            
            
            // number of indices
            int numIndex = Float.floatToIntBits(inb.get(i)) & 0xff;
            // the matrix indices
            int matIndex = Float.floatToIntBits(ib.get(i));
            // the matrix weights
            wb.position(i*4);
            wb.get(matWeights);

            // clear vectors holding result values
            tPosition.set(0,0,0);
            tNormal.set(0, 0, 0);

            // go over each of the weights
            for(int j=0; j<numIndex; j++) {
                // get the matrix index
                int mati = matIndex & 0xff;
                // shift the index
                matIndex = matIndex >> 8;
                
                // fetch the matrix
                Matrix4f mat = boneMats[mati];
                
                // transform position
                tvec.set(sPosition);
                mat.mult(tvec, tvec);
                // scale the vector by weight
                tvec.scale(matWeights[j]);
                // sum up into final position
                tPosition.addLocal(tvec);
                
                // rotate normal
                tvec.set(sNormal);
                mat.rotateVect(tvec);
                tvec.scale(matWeights[j]);
                tNormal.addLocal(tvec);
            }
             
            //tPosition.set(sPosition);
            //tNormal.set(sNormal);
            
            // we got the final position and normal, put them into the buffers
            tpb.put(tPosition.x).put(tPosition.y).put(tPosition.z);
            tnb.put(tNormal.x).put(tNormal.y).put(tNormal.z);
        }
        
        // rewind the buffers
        pb.rewind();
        nb.rewind();
        wb.rewind();
        ib.rewind();
        inb.rewind();
        tpb.rewind();
        tnb.rewind();
    }

    @Override
    public boolean preDraw(RenderContext ctx, Renderable r) {
        // if the mesh needs to be updated, update it
        if(needsRefresh) {
            // allocate ogl memory if its not yet allocated
            // map the buffer to java memory too
            mapBuffers(ctx);
            
            // do the actual skinning (generate vertices)
            skinMesh();
            
            // release the buffers, so OGL can use them during rendering
            unMapBuffers(ctx);

            // map a buffer from OpenGL
            needsRefresh = false;
        }
        return true;
    }
    
    @Override
    public void createCollisionVolume(Vector3f cellSize) {
        CollisionVolume cvm = model.getCollisionVolume();
        if(cvm!=null && cvm.getCellSize().equals(cellSize)) {
            this.collisionVolume = cvm;
            return;
        }
        collisionVolume = new CollisionVolume();
        collisionVolume.buildVolume(model, cellSize);
    }
    
    @Override
    public BoundingVolume getModelBound() {
        return model.getModelBound();
    }
}
