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
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.Renderable;
import com.vlengine.system.VleException;
import com.vlengine.util.geom.VertexAttribute;

/**
 *
 * @author vear (Arpad Vekas)
 */
public abstract class BaseGeometry {
    
    public static final int NM_USE_PROVIDED = 1;
    public static final int NM_GL_NORMALIZE_PROVIDED = 2;
    public static final int NM_GL_NORMALIZE_IF_SCALED = 3;
    public static final int NM_OFF = 4;

    public static final int TRIANGLE_SOUP = 0;
    public static final int TRIANGLES = 1;
    public static final int TRIANGLE_STRIP = 2;
    public static final int TRIANGLE_FAN = 3;
    
    public static final int LINE_SEGMENTS = 4;
    public static final int LINE_CONNECTED = 5;
    public static final int LINE_LOOP = 6;
    
    // strategy for generating display lists
    // do not generate display list
    public static final int LIST_NO = 0;
    // generate display list
    public static final int LIST_YES = 1;
    // generate display list with locked position
    // this list is used for only one object with no moving
    public static final int LIST_LOCKED = 2;
    
    // strategy for generating VBOs
    // this geometry is used only once in the scene
    public static final int VBO_NO = 0;
    // this geometry is fixed in one place in the scene
    public static final int VBO_YES = 1;
    // this geometry can change during frames, but try to make VBOs
    // in update thread before rendering
    public static final int VBO_PRELOAD = 2;
    // this geometry is used in more frames, preserve VBO among frames
    // this assumes that the geometry buffers do not change
    public static final int VBO_LONGLIVED = 3;
    
    // do not combine this geometry
    public static final int COMBINE_NONE = 0;
    // pack geometry data into a single VBO
    public static final int COMBINE_PACK = 2;
    // make a single interleaved array for this geometry
    public static final int COMBINE_INTERLEAVE = 3;
    // combine packed array with other geometry for large VBO
    public static final int COMBINE_PACK_LARGE = 4;
    // combine interleaved array with other geometry for large VBO
    public static final int COMBINE_INTERLEAVE_LARGE = 5;

    /** The local bounds of this Geometry object. */
    protected BoundingVolume bound;

    /**
     * A flag indicating how normals should be treated by the renderer.
     */
    protected int normalsMode = NM_USE_PROVIDED;
    
    protected boolean castsShadows = true;

    protected int mode = TRIANGLES;
    
    protected boolean isCollidable = true;
    
    protected CollisionTree collisionTree=null;
    
    protected CollisionVolume collisionVolume=null;

    // the default is no display lists
    protected int listMode = LIST_NO;
    
    // the default is no VBO
    protected int vboMode = VBO_NO;
    
    // the default is no buffer combining
    protected int combineMode = COMBINE_NONE;
    
    /**
     * Non -1 values signal that drawing this scene should use the provided
     * display list instead of drawing from the buffers.
     */
    protected int displayListID = -1;
    // display list id with position only
    protected int posListID = -1;

    // how many vertices belong to this geometry
    int numVertex = 0;
    
    // where this batch's indices begin in the index buffer
    int startIndex = 0;
    // how many indices belong to this geometry
    int numIndex = 0;
    

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }

    public int getDisplayListID() {
            return displayListID;
    }

    public void setDisplayListID(int displayListID) {
            this.displayListID = displayListID;
    }

    public int getPosOnlyDisplayListID() {
            return posListID;
    }

    public void setPosOnlyDisplayListID(int displayListID) {
            this.posListID = displayListID;
    }

    public boolean isCastsShadows() {
        return castsShadows;
    }
    
    /**
     * Sets if this Spatial is to be used in intersection (collision and
     * picking) calculations. By default this is true.
     * 
     * @param isCollidable
     *            true if this Spatial is to be used in intersection
     *            calculations, false otherwise.
     */
    public void setCollidable(boolean isCollidable) {
        this.isCollidable = isCollidable;
    }
    
    /**
     * Defines if this Spatial is to be used in intersection (collision and
     * picking) calculations. By default this is true.
     * 
     * @return true if this Spatial is to be used in intersection calculations,
     *         false otherwise.
     */
    public boolean isCollidable() {
        return this.isCollidable;
    }

    public void setCastsShadows(boolean castsShadows) {
        this.castsShadows = castsShadows;
    }

    /**
     * <code>getModelBound</code> retrieves the bounding object that contains
     * the batch vertices.
     * 
     * @return the bounding object for this geometry.
     */
    public BoundingVolume getModelBound() {
        return bound;
    }

    public void setModelBound(BoundingVolume bound) {
        this.bound = bound;
    }

    public CollisionTree getCollisionTree() {
        return collisionTree;
    }
    
    public void setCollisionTree(CollisionTree tree) {
        this.collisionTree = tree;
    }
    
    public CollisionVolume getCollisionVolume() {
        return collisionVolume;
    }
    
    public void setCollisionVolume(CollisionVolume tree) {
        this.collisionVolume = tree;
    }

    public int getTriangleCount() {
        switch(mode) {
            case TRIANGLE_SOUP : return numVertex /3;
            case TRIANGLES : return numIndex / 3;
            case TRIANGLE_STRIP:
            case TRIANGLE_FAN:
                return numIndex - 2;
            default:
                // possibly we have a line
                return 0;
        }
    }
    
    protected int getVertIndex(int triangle, int point) {
        int index = 0, i = (triangle * 3) + point;
        switch (mode) {
            case TRIANGLE_SOUP:
            case TRIANGLES:
                index = i;
                break;
            case TRIANGLE_STRIP:
                index = (i/3)+(i%3);
                break;
            case TRIANGLE_FAN:
                if (i%3 == 0) index = 0;
                else {
                    index = (i%3);
                    index = ((i - index) / 3) + index;
                }
                break;
            default:
                throw new VleException("mode is set to invalid type: "+mode);
        }
        return startIndex + index;
    }   
    
    public void setNumVertex(int numVertex) {
        this.numVertex = numVertex;
    }
    
    public int getNumVertex() {
        return this.numVertex;
    }
    
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getStartIndex() {
        return this.startIndex;
    }
    
    public void setNumIndex(int numIndex) {
        this.numIndex = numIndex;
    }

    public int getNumIndex() {
        return this.numIndex;
    }
    
    public int getMaxIndex() {
        return startIndex + numIndex;
    }
    
    public int getNormalsMode() {
        return normalsMode;
    }
    
    public int getDisplayListMode() {
        return listMode;
    }
    
    public void setDisplayListMode(int gmode) {
        listMode = gmode;
    }
    
    public int getVBOMode() {
        return vboMode;
    }
    
    public void setVBOMode(int vbomode) {
        vboMode = vbomode;
    }
    
    public int getCombineMode() {
        return combineMode;
    }
    
    public void setCombineMode(int cmode) {
        combineMode = cmode;
    }

    // called every time this geomerty is drawn
    // if false, this geometry is not drawn
    public boolean preDraw(RenderContext ctx, Renderable r) {return true;}
    
    // called every time this geometry has finished drawing
    public void postDraw(RenderContext ctx, Renderable r) {};

    public abstract void getTriangle(int i, Vector3f[] vertices);
}
