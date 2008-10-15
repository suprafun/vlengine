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

package com.vlengine.bounding;

import com.vlengine.intersection.PickData;
import com.vlengine.intersection.PickResults;
import com.vlengine.math.FastMath;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Geometry;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelPart;
import com.vlengine.scene.Spatial;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.BitSet;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.VertexAttribute;

/**
 * Experimental collision handling class, representing a mesh.
 * 
 * @author vear (Arpad Vekas)
 */
public class CollisionVolume {

    // default cell size for dinamic models (characters)
    public static final Vector3f DEFAULT_CELLSIZE_ANIMATED = new Vector3f(3f,3f,3f);
    // default cell size for static models (buildings)
    public static final Vector3f DEFAULT_CELLSIZE_STATIC = new Vector3f(5,5,5);
    
    // the mesh always has a BoundingBox
    protected BoundingBox localBound;
    
    // the occupiing of data
    // for GPU processing this need to be a bytebuffer?
    //protected boolean[] cells;
    protected BitSet cells;
    
    protected int sizeX;
    protected int sizeY;
    protected int sizeZ;
    
    // the geometry we have
    //protected Geometry parent;
    
    protected Vector3f cellSize = new Vector3f();
    protected Vector3f cellScale = new Vector3f();
    
    protected Vector3f translate;
    // the cell position in rasterizing
    protected Vector3f linestart = new Vector3f();
    protected Vector3f lineend = new Vector3f();
    
    // cellsize is the size of cells
    public CollisionVolume() {
        
    }
    
    public Vector3f getCellSize() {
        return cellSize;
    }

    /**
     * Build a cell map for the whole model
     * @param mdl
     */
    public void buildVolume(Model mdl, Vector3f csize) {
        
        cellSize = csize;
        cellScale.x = 1f/cellSize.x; cellScale.y = 1f/cellSize.y; cellScale.z = 1f/cellSize.z;
        
        localBound = new BoundingBox();
        
        FastList<FastList<ModelPart>> lods = mdl.getLods();
        if(lods == null) 
            return;
        FastList<ModelPart> mparts = lods.get(0);
        if(mparts==null)
            return;
        
        // go over the parts, and create the local bound
        localBound = new BoundingBox();
        
        for(int i=0, mi=mparts.size(); i<mi; i++) {
            ModelPart mp = mparts.get(i);
            Geometry geom = mp.getGeometry();
            localBound.mergeLocal(geom.getModelBound());
        }
        
        // calculate translate vector which brings vertices to cell space
        translate = new Vector3f(localBound.xExtent, localBound.yExtent, localBound.zExtent);
        translate.subtractLocal(localBound.getCenter());
        
        // calculate the grid size
        sizeX = (int) (localBound.xExtent*2f * cellScale.x)+1;
        sizeY = (int) (localBound.yExtent*2f * cellScale.y)+1;
        sizeZ = (int) (localBound.zExtent*2f * cellScale.z)+1;
        
        /*
        // let there be a minimum of 4 cells
        if(sizeX<4) {
            sizeX = 4;
            cellSize.x = localBound.xExtent*2f / ((float)sizeX);
            cellScale.x = 1f/cellSize.x;
        }
        if(sizeY<4) {
            sizeY = 4;
            cellSize.y = localBound.yExtent*2f / ((float)sizeY);
            cellScale.y = 1f/cellSize.y;
        }
        if(sizeZ<4) {
            sizeZ = 4;
            cellSize.z = localBound.zExtent*2f / ((float)sizeZ);
            cellScale.z = 1f/cellSize.z;
        }
        sizeX++;
        sizeY++;
        sizeZ++;
         */
        
        // create the cell map
        //cells = new boolean[sizeX*sizeY*sizeZ];
        cells = new BitSet(sizeX*sizeY*sizeZ);
        
        // fill cells for each geom
        for(int i=0, mi=mparts.size(); i<mi; i++) {
            ModelPart mp = mparts.get(i);
            Geometry geom = mp.getGeometry();
            buildCellMap(geom);
        }
    }

    public void buildVolume(Geometry parent, float cellsize) {
        cellSize.x = cellSize.y = cellSize.z = cellsize;
        buildVolume(parent, cellSize);
    }
    
    public void buildVolume(Geometry parent, Vector3f cellsize) {
        this.cellSize = cellsize;
        cellScale.x = 1f/cellSize.x; cellScale.y = 1f/cellSize.y; cellScale.z = 1f/cellSize.z;
        buildVolume(parent);
    }

    protected void buildVolume(Geometry parent) {
        
        //this.parent = parent;
        // calculate the bounding box
        if (parent.getAttribBuffer(VertexAttribute.USAGE_POSITION) == null) {
            // no position data, exit
            return;
        }
        localBound = new BoundingBox();
        localBound.computeFromPoints(parent.getAttribBuffer(VertexAttribute.USAGE_POSITION), parent.getStartVertex(VertexAttribute.USAGE_POSITION), parent.getNumVertex());
        // calculate translate vector which brings vertices to cell space
        translate = new Vector3f(localBound.xExtent, localBound.yExtent, localBound.zExtent);
        translate.subtractLocal(localBound.getCenter());
        
        // calculate the grid size
        sizeX = (int) (localBound.xExtent*2f / cellSize.x)+1;
        sizeY = (int) (localBound.yExtent*2f / cellSize.y)+1;
        sizeZ = (int) (localBound.zExtent*2f / cellSize.z)+1;
        // create the cell map
        //cells = new boolean[sizeX*sizeY*sizeZ];
        cells = new BitSet(sizeX*sizeY*sizeZ);
        buildCellMap(parent);
    }
    
    protected void buildCellMap(Geometry parent) {
        Vector3f[] verts = LocalContext.getContext().tCollisionVolumeVerts;
        // go over and resterize the triangles into the cells
        
        int[] p = new int[3];
        
        int triCount = parent.getTriangleCount();
        for(int i=0; i<triCount; i++) {
            parent.getTriangle(i, verts);
            // translate vector
            verts[0].addLocal(translate);
            verts[1].addLocal(translate);
            verts[2].addLocal(translate);

            // get the longest extent
            int[] extent = getLongestExtent(verts);
            
            // TODO: find the biggest extent of the triangle
            // and do the stepping with that extent
            // find the top vertice, sort by Y
            sortVerts(verts, extent[0]);
            // go from bottom to top
            for(linestart.set(extent[0], verts[0].get(extent[0])); 
                linestart.get(extent[0])<=verts[2].get(extent[0]); 
                linestart.set(extent[0], linestart.get(extent[0])+cellSize.get(extent[0]))) {
                // calculate the interpolation
                float interp1 = (linestart.get(extent[0])-verts[0].get(extent[0]))/(verts[2].get(extent[0])-verts[0].get(extent[0]));
                // calculate start x and z by interpolating
                linestart.set(extent[1], FastMath.LERP(interp1, verts[0].get(extent[1]), verts[2].get(extent[1])));
                linestart.set(extent[2], FastMath.LERP(interp1, verts[0].get(extent[2]), verts[2].get(extent[2])));
                
                if(linestart.get(extent[0])< verts[1].get(extent[0])) {
                    // interpolation between v0 and v1
                    float interp2 = (linestart.get(extent[0])-verts[0].get(extent[0]))/(verts[1].get(extent[0])-verts[0].get(extent[0]));
                    // we scann on y, so y is not changed here
                    lineend.set(extent[0], linestart.get(extent[0]));
                    lineend.set(extent[1], FastMath.LERP(interp2, verts[0].get(extent[1]), verts[1].get(extent[1])));
                    lineend.set(extent[2], FastMath.LERP(interp2, verts[0].get(extent[2]), verts[1].get(extent[2])));
                } else {
                    // interpolation between v1 and v2
                    float interp2 = (linestart.get(extent[0])-verts[1].get(extent[0]))/(verts[2].get(extent[0])-verts[1].get(extent[0]));
                    lineend.set(extent[0], linestart.get(extent[0]));
                    lineend.set(extent[1], FastMath.LERP(interp2, verts[1].get(extent[1]), verts[2].get(extent[1])));
                    lineend.set(extent[2], FastMath.LERP(interp2, verts[1].get(extent[2]), verts[2].get(extent[2])));
                }
                

                // rasterize on the longer axis
                if(FastMath.abs(lineend.get(extent[1]) - linestart.get(extent[1])) < FastMath.abs(lineend.get(extent[2]) - linestart.get(extent[2]))) {
                    int ext1 = extent[1];
                    extent[1] = extent[2];
                    extent[2] = ext1;
                }
                // go from start to end
                if(linestart.get(extent[1]) > lineend.get(extent[1])) {
                    Vector3f tmp = lineend;
                    lineend = linestart;
                    linestart = tmp;
                }
                for(float ex1 = linestart.get(extent[1]); ex1<=lineend.get(extent[1]); ex1+=cellSize.get(extent[1])) {
                    // calculate Z as interpolated between start and end
                    float interp3 = (ex1-linestart.get(extent[1]))/(lineend.get(extent[1])-linestart.get(extent[1]));
                    float ex2 = FastMath.LERP(interp3, linestart.get(extent[2]), lineend.get(extent[2]));

                    p[extent[0]] = (int) (linestart.get(extent[0]) * cellScale.get(extent[0]));
                    p[extent[1]] = (int) (ex1 * cellScale.get(extent[1]));
                    p[extent[2]] = (int) (ex2 * cellScale.get(extent[2]));
                    
                    if(p[0]<0 || p[1]<0 || p[2]<0) {
                        System.out.println("Bad cell coordinate");
                    }
                    // switch on the cell at the given coordinate
                    //cells[getCellIndex(p[0], p[1], p[2])] = true;
                    setCell(p[0], p[1], p[2]);
                }
            }
        }
    }

    protected int[] getLongestExtent(Vector3f[] verts) {
        
        int[] extent = new int[3];
        
        float myext = FastMath.abs(verts[0].y-verts[1].y);
        float mxext = FastMath.abs(verts[0].x-verts[1].x);
        float mzext = FastMath.abs(verts[0].z-verts[1].z);
        
        float yext = FastMath.abs(verts[1].y-verts[2].y);
        float xext = FastMath.abs(verts[1].x-verts[2].x);
        float zext = FastMath.abs(verts[1].z-verts[2].z);
        
        if(yext>myext)
            myext = yext;
        if(xext>mxext)
            mxext = xext;
        if(zext>mzext)
            mzext = zext;

        yext = FastMath.abs(verts[0].y-verts[2].y);
        xext = FastMath.abs(verts[0].x-verts[2].x);
        zext = FastMath.abs(verts[0].z-verts[2].z);

        if(yext>myext)
            myext = yext;
        if(xext>mxext)
            mxext = xext;
        if(zext>mzext)
            mzext = zext;

        // check which extent is the longest
        if(mxext>=myext && mxext >= mzext) {
            extent[0] = 0;
            if(myext >= mzext) {
                extent[1] = 1;
                extent[2] = 2;
                return extent;
            } else {
                extent[1] = 2;
                extent[2] = 1;
                return extent;
            }
        } else if(myext >= mxext && myext >= mzext) {
            extent[0] = 1;
            if(mxext >= mzext) {
                extent[1] = 0;
                extent[2] = 2;
                return extent;
            } else {
                extent[1] = 2;
                extent[2] = 0;
                return extent;
            }
        }
            
        extent[0] = 2;
        if(mxext >= myext) {
            extent[1] = 0;
            extent[2] = 1;
        } else {
            extent[1] = 1;
            extent[2] = 0;
        }

        return extent;
    }
    
    protected boolean getCell(int x, int y, int z) {
        return cells.get(getCellIndex(x, y, z));
    }
    
    protected void setCell(int x, int y, int z) {
        cells.set(getCellIndex(x, y, z), true);
    }
    
    protected int getCellIndex(int x, int y, int z) {
        return x*(sizeY*sizeZ) + y*sizeZ + z;
    }
    
    protected void sortVerts(Vector3f[] verts, int extent) {
        if(verts[0].get(extent)> verts[1].get(extent)) {
            Vector3f sv = verts[1];
            verts[1] = verts[0];
            verts[0] = sv;
        }
        if(verts[1].get(extent) > verts[2].get(extent)) {
            Vector3f sv = verts[2];
            verts[2] = verts[1];
            verts[1] = sv;
        }
        if(verts[0].get(extent) > verts[1].get(extent)) {
            Vector3f sv = verts[1];
            verts[1] = verts[0];
            verts[0] = sv;
        }
    }
    
    Matrix4f finalMat = new Matrix4f();
    Matrix4f localToWorld = new Matrix4f();
    Matrix4f tmpMat = new Matrix4f();
    Matrix4f worldToLocal = new Matrix4f();
    Vector3f tmpVec = new Vector3f();
    Quaternion tmpQ = new Quaternion();
    
    public boolean getCollisions(Spatial myParent, Spatial otherParent, CollisionVolume otherVolume, FastList<Vector3f> points) {
        // create a matrix to transform from this volumes coordinate system to world coordinate system
        localToWorld.loadIdentity();
        tmpVec.set(translate);
        tmpVec.negateLocal();
        localToWorld.setTranslation(tmpVec);
        localToWorld.scale(myParent.getWorldScale());
        localToWorld.multLocal(myParent.getWorldRotation().toRotationMatrix(tmpMat));
        tmpMat.loadIdentity(); tmpMat.setTranslation(myParent.getWorldTranslation());
        localToWorld.multLocal(tmpMat);
        
        // create world to local on the other matrix
        worldToLocal.loadIdentity();
        tmpVec.set(otherParent.getWorldTranslation());
        tmpVec.negateLocal();
        worldToLocal.setTranslation(tmpVec);
        tmpVec.set(otherParent.getWorldScale());
        tmpVec.x = 1f/tmpVec.x;
        tmpVec.y = 1f/tmpVec.y;
        tmpVec.z = 1f/tmpVec.z;
        worldToLocal.scale(tmpVec);
        tmpMat.loadIdentity();
        tmpQ.set(otherParent.getWorldRotation()).inverseLocal();
        tmpQ.toRotationMatrix(tmpMat);
        worldToLocal.multLocal(tmpMat);
        tmpVec.set(otherVolume.translate);
        tmpMat.loadIdentity();
        tmpMat.setTranslation(tmpVec);
        worldToLocal.multLocal(tmpMat);
        
        // the final transform matrix
        finalMat.set(localToWorld);
        finalMat.multLocal(worldToLocal);
        
        //float otherScale = 1f/otherVolume.cellSize;
        
        boolean found = false;
        
        // go over the cells
        for(int y=0; y<sizeY; y++) {
            for(int x=0; x<sizeX; x++) {
                for(int z=0; z<sizeZ; z++) {
                    // check if it is set
                    if(getCell(x, y, z)) {
                        // this cell is occupyed
                        // set temp vector
                        tmpVec.set(x*cellSize.x, y*cellSize.y, z*cellSize.z);
                        // transform into world space
                        tmpVec.subtractLocal(translate);
                        myParent.localToWorld(tmpVec, tmpVec);
                        // transform it to targets cell space
                        //finalMat.mult(tmpVec, tmpVec);
                        otherParent.worldToLocal(tmpVec, tmpVec);
                        tmpVec.addLocal(otherVolume.translate);
                        
                        int tx = (int) (tmpVec.x * otherVolume.cellScale.x);
                        int ty = (int) (tmpVec.y * otherVolume.cellScale.y);
                        int tz = (int) (tmpVec.z * otherVolume.cellScale.z);
                        // check if its inside the other volume, 
                        // check if its set in the other volume
                        if(tx>=0 && tx<otherVolume.sizeX
                                && ty>=0 && ty<otherVolume.sizeY
                                && tz>=0 && tz<otherVolume.sizeZ
                                && otherVolume.getCell(tx, ty, tz)) {
                            // we got a collision
                            if(points!=null) {
                                // create a new vector to store
                                Vector3f cv = (Vector3f) LocalContext.getContext().ctbstack.fetch(ReuseManager.TYPE_Vector3f);
                                cv.set(x*cellSize.x, y*cellSize.y, z*cellSize.z);
                                // transform it into world space
                                cv.subtractLocal(translate);
                                myParent.localToWorld(cv, cv);
                                
                                //localToWorld.mult(cv, cv);
                                // add the vector
                                points.add(cv);
                                found = true;
                            } else {
                                // just return that we collided
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return found;
    }
    
    Vector3f tmpVec1 = new Vector3f();
    Vector3f tmpVec2 = new Vector3f();
    Vector3f tmpVec3 = new Vector3f();
    
    public boolean getPick(Spatial myParent, PickResults res) {
        
        // get direction, multiply with or cell size, this will be the stepping
        tmpVec1.set(res.getRay().getDirection()).normalizeLocal().multLocal(cellSize);
        
        boolean entered = false;
        boolean exited = false;
        
        float rayLenght = res.getRayLength();
        PickData pickdata = null;
        FastList<Vector3f> points = null;
        
        boolean found = false;
        tmpVec2.zero();
        
        Vector3f rcent = res.getRay().origin;
        
        float raysquared = rayLenght * rayLenght;

        // go util until reached rayLenght, or exited the area of the volume
        for(;!exited&&tmpVec2.lengthSquared()<raysquared;tmpVec2.addLocal(tmpVec1)) {
            // transform the position into cell space
            tmpVec.set(tmpVec2).addLocal(rcent);
            myParent.worldToLocal(tmpVec, tmpVec);
            tmpVec.addLocal(translate);

            int tx = (int) (tmpVec.x * cellScale.x);
            int ty = (int) (tmpVec.y * cellScale.y);
            int tz = (int) (tmpVec.z * cellScale.z);

            /*
            // check multiple cells around the cell
            int px = (int) (tmpVec.x * cellScale.x);
            int py = (int) (tmpVec.y * cellScale.y);
            int pz = (int) (tmpVec.z * cellScale.z);

            for(int tx=px-1; tx<=px+1; tx++)
                for(int ty=py-1; ty<=py+1; ty++)
                    for(int tz=pz-1; tz<=pz+1; tz++) {
             */
                        // check if its set in the other volume
                        if(tx>=0 && tx<sizeX
                                && ty>=0 && ty<sizeY
                                && tz>=0 && tz<sizeZ) {
                            entered = true;
                            if(getCell(tx, ty, tz)) {
                                if(pickdata == null) {
                                    pickdata = new PickData(myParent);
                                    if(res.isVolumeGetPickPoints()) {
                                        points = new FastList<Vector3f>();
                                        pickdata.setCollisionPoints(points);
                                    }
                                    res.addPick(pickdata);
                                }

                                // we got a collision
                                if(points != null) {
                                    // create a new vector to store
                                    Vector3f cv = (Vector3f) LocalContext.getContext().ctbstack.fetch(ReuseManager.TYPE_Vector3f);
                                    cv.set(tmpVec2);
                                    // add the vector
                                    points.add(cv);
                                    found = true;
                                } else {
                                    // this is actualy the closeset one
                                    pickdata.setDistance(tmpVec2.length());
                                    // just return that we collided
                                    return true;
                                }
                            }
                        } else {
                            if(entered) {
                                // we exited, finish
                                exited = true;
                            }
                        }
                    //}
        }
        return found;
    }
}
