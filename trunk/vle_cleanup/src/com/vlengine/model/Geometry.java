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

import com.vlengine.bounding.BoundingVolume;
import com.vlengine.bounding.CollisionTree;
import com.vlengine.bounding.CollisionVolume;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.VBOAttributeInfo;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import com.vlengine.util.geom.GeometryIterator;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;
import com.vlengine.util.geom.VertexIterator;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

/**
 * Base geometry class that hold triangulated surface data.
 * 
 * @author vear (Arpad Vekas)
 */
public class Geometry extends BaseGeometry {
    private static final Logger logger = Logger.getLogger(Geometry.class.getName());
    
    // vertex attributes as an array
    protected FastList<VertexBuffer> attribArrays = new FastList<VertexBuffer>();
    // vertex buffer by type
    protected VertexBuffer[] bfrIndex = new VertexBuffer[VertexAttribute.USAGE_MAX];
    // where vertex data start for buffer of a given buffer
    protected IntList startVertex = new IntList();
    // start vertex by type
    protected int[] startVertexIndex = new int[VertexAttribute.USAGE_MAX];
    
    // the indexbuffer
    protected IndexBuffer indices;
    
    public void addAttribBuffer(VertexBuffer attribArray, int startVertex) {
        // add it to the arrays list
        attribArrays.add(attribArray);
        // set startvertex
        this.startVertex.add(startVertex);
        // get all attribute types, and set each index to the array
        FastList<VertexAttribute> vattrs = attribArray.getFormat().getAttributes();
        for(int i=0, mx=vattrs.size(); i<mx; i++) {
            VertexAttribute vatt = vattrs.get(i);
            bfrIndex[vatt.type.id] = attribArray;
            // set startvertex into index
            startVertexIndex[vatt.type.id] = startVertex;
        }
        setNumVertex(attribArray.getVertexCount());
    }

    public VertexBuffer getAttribBuffer(VertexAttribute.Usage vertexdatatype) {
        VertexBuffer vb = bfrIndex[vertexdatatype.id];
        if(vb!=null && vb.getFormat().getAttribute(vertexdatatype)==null)
            vb = null;
        if(vb==null) {
            // try to find
            for(int i=0; i<attribArrays.size(); i++) {
                VertexAttribute vatt = attribArrays.get(i).getFormat().getAttribute(vertexdatatype);
                if(vatt!=null) {
                    bfrIndex[vatt.type.id] = attribArrays.get(i);
                }
            }
        }
        return vb;
    }

    public FastList<VertexBuffer> getBuffers() {
        return attribArrays;
    }
    
    public void setIndexBuffer(IndexBuffer indices) {
        this.indices = indices;
        setNumIndex(indices.limit());
    }

    public IndexBuffer getIndexBuffer() {
        return indices;
    }

    public void updateModelBound() {
        if (bound != null && getAttribBuffer(VertexAttribute.USAGE_POSITION) != null) {
            bound.computeFromPoints(getAttribBuffer(VertexAttribute.USAGE_POSITION), startVertexIndex[VertexAttribute.USAGE_POSITION.id], numVertex);
        }
    }

    public FastList<VertexAttribute> getAllAttributes(FastList<VertexAttribute> store) {
        if(store==null)
            store = new FastList<VertexAttribute>();
        // got trough all the vertex buffers
        for(int i=0; i<attribArrays.size(); i++) {
            FastList<VertexAttribute> vfa = attribArrays.get(i).getFormat().getAttributes();
            store.addAll(vfa);
        }
        return store;
    }
    
    public VertexIterator getVertexIterator(VertexAttribute.Usage vertexdatatype) {
        VertexBuffer vb = getAttribBuffer(vertexdatatype);
        
        if(vb==null || vb.getDataBuffer()==null)
            return null;
        VertexAttribute vba = vb.getFormat().getAttribute(vertexdatatype);
        // construct the iterator
        VertexIterator vit = new VertexIterator(vb.getDataBuffer(), 
                startVertexIndex[vertexdatatype.id], numVertex, 
                vb.getFormat().getSize(),
                vba.startfloat, vba.floats);        
        return vit;
    }

    public void fillBuffer(VertexAttribute.Usage attribType, ColorRGBA color) {
        // create color buffer, it we dont have it
        VertexBuffer vb = getAttribBuffer(attribType);
        if(vb == null) {
            // create color buffer
            vb = VertexBuffer.createSingleBuffer(attribType, this.getNumVertex());
            this.addAttribBuffer(vb, 0);
        }
        VertexIterator vi = this.getVertexIterator(attribType);
        for(int i=0; i<numVertex; i++) {
            vi.put(i, color);
        }
    }

    /**
     * Returns the start vertex array corresponding to attribute buffers.
     * Index position inside the array corresponds to attribute array number.
     * Eg. getBuffers(i) is the buffer, and getBufferStartVertex().get(i) is
     * the start position inside the buffer.
     * @return
     */
    public IntList getBufferStartVertex() {
        return this.startVertex;
    }
    
    /**
     * Returns the start vertex for this geom inside the vertex buffer
     * containing the given attribute type.
     * @param attribType    The attribute type we request the start
     * @return              The start vertex of this geom inside the vertex buffer
     */
    public int getStartVertex(VertexAttribute.Usage attribType) {
        return startVertexIndex[attribType.id];
    }

    /**
     * Stores in the <code>vertices</code> array the vertex values of triangle
     * <code>i</code>. If <code>i</code> is an invalid triangle index,
     * nothing happens.
     * 
     * @param i
     * @param vertices
     */
    public void getTriangle(int i, Vector3f[] vertices) {
    	if(vertices == null) {
            vertices = new Vector3f[3];
    	}
        if (i < getTriangleCount() && i >= 0) {
            VertexBuffer vb = this.getAttribBuffer(VertexAttribute.USAGE_POSITION);
            VertexFormat vf = vb.getFormat();
            VertexAttribute pa = vf.getAttribute(VertexAttribute.USAGE_POSITION);
            FloatBuffer fb = vb.getDataBuffer();
            int posStart = startVertexIndex[VertexAttribute.USAGE_POSITION.id];
            
            for (int x = 0; x < 3; x++) {
                if (vertices[x] == null) {
                	vertices[x] = new Vector3f();
                }
                // calculate position
                fb.position(((posStart+indices.get(getVertIndex(i, x)))  // start of geom in buffer
                        *vf.getSize())  // start of vertex
                        +pa.startfloat  // start of position data inside vertex
                        );
                vertices[x].x = fb.get();
                vertices[x].y = fb.get();
                vertices[x].z = fb.get();
            }
        }
    }

    public void createCollisionTree() {
        if(bound!=null ) {
            int ctype = 0;
            switch(bound.getType()) {
                case BoundingVolume.BOUNDING_BOX:
                    ctype = CollisionTree.AABB_TREE;
                    break;
                case BoundingVolume.BOUNDING_OBB:
                    ctype = CollisionTree.OBB_TREE;
                    break;
                case BoundingVolume.BOUNDING_SPHERE:
                    ctype = CollisionTree.SPHERE_TREE;
                    break;
                default:
                    ctype = CollisionTree.AABB_TREE;
            }
            collisionTree = new CollisionTree(ctype);
            collisionTree.construct(this, true);
        }
    }
    
    public void createCollisionVolume(Vector3f cellSize) {
        collisionVolume = new CollisionVolume();
        collisionVolume.buildVolume(this, cellSize);
    }
    
    /**
     * Force create VBO info for every vertex buffer
     */
    public void createVBOInfos() {
        for(int i=0; i<attribArrays.size(); i++) {
            VertexBuffer vtx = attribArrays.get(i);
            if(vtx.getVBOInfo()==null) {
                vtx.setVBOInfo(new VBOAttributeInfo());
            }
        }
        // create VBO info on the index buffer too
        if(indices.getVBOInfo()==null) {
           indices.setVBOInfo(new VBOAttributeInfo());
        }
    }
    
    public GeometryIterator createIterator() {
        return new GeometryIterator(this);
    }
}
