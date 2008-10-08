/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
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
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
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

package com.vlengine.bounding;

import com.vlengine.intersection.IntersectionRecord;
import com.vlengine.math.FastMath;
import com.vlengine.math.Matrix3f;
import com.vlengine.math.Plane;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Ray;
import com.vlengine.math.Triangle;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Geometry;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import java.nio.FloatBuffer;

/**
 *
 * @author vear (Arpad Vekas) reworked to VL engine
 */
public class BoundingBox extends BoundingVolume {

    public float xExtent, yExtent, zExtent;


    /**
     * Default contstructor instantiates a new <code>BoundingBox</code>
     * object.
     */
    public BoundingBox() {
    }

    /**
     * Contstructor instantiates a new <code>BoundingBox</code> object with
     * given specs.
     */
    public BoundingBox(Vector3f c, float x, float y, float z) {
        this.center.set(c);
        this.xExtent = x;
        this.yExtent = y;
        this.zExtent = z;
    }

    public int getType() {
        return BoundingVolume.BOUNDING_BOX;
    }

    public void computeFromPoints(VertexBuffer attribBuffer, int startVertex, int numVertex) {
        containAABB(attribBuffer, startVertex, numVertex);
    }
    
    /**
     * <code>computeFromBatches</code> creates a new Bounding Box from a given
     * set of batches which contain a list of points. It uses the
     * <code>containAABB</code> method as default.
     * 
     * @param batches
     *            the batches to contain.
     */
    public void computeFromBatches(FastList<Geometry> batches) {
        if (batches == null || batches.size() == 0) {
            return;
        }
        
        Context tmp=LocalContext.getContext();
        
        BoundingBox temp = tmp.tempBoundingBox;

        Geometry cg = batches.get(0);
        temp.containAABB(cg.getAttribBuffer(VertexAttribute.USAGE_POSITION), 
                cg.getStartVertex(VertexAttribute.USAGE_POSITION),
                cg.getNumVertex());

        for (int i = 1; i < batches.size(); i++) {
            BoundingBox bb = tmp.tempBoundingBox2;
            cg = batches.get(i);
            bb.containAABB(cg.getAttribBuffer(VertexAttribute.USAGE_POSITION), 
                cg.getStartVertex(VertexAttribute.USAGE_POSITION),
                cg.getNumVertex());
            temp.mergeLocal(bb);
        }

        this.center = temp.getCenter();
        this.xExtent = temp.xExtent;
        this.yExtent = temp.yExtent;
        this.zExtent = temp.zExtent;
    }

    /**
     * <code>computeFromTris</code> creates a new Bounding Box from a given
     * set of triangles. It is used in OBBTree calculations.
     * 
     * @param tris
     * @param start
     * @param end
     */
    public void computeFromTris(Triangle[] tris, int start, int end) {
        if (end - start <= 0) {
            return;
        }
        Context tmp = LocalContext.getContext();
        
        Vector3f min = tmp._compVect1.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f max = tmp._compVect2.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        
        Vector3f point;
        for (int i = start; i < end; i++) {
            point = tris[i].get(0);
            checkMinMax(min, max, point);
            point = tris[i].get(1);
            checkMinMax(min, max, point);
            point = tris[i].get(2);
            checkMinMax(min, max, point);
        }
        
        center.set(min.addLocal(max));
        center.multLocal(0.5f);

        xExtent = max.x - center.x;
        yExtent = max.y - center.y;
        zExtent = max.z - center.z;
    }
    
    public void computeFromTris(Geometry batch, int start, int end) {
    	if (end - start <= 0) {
            return;
        }
    	Context tmp = LocalContext.getContext();
        final Vector3f[] verts = tmp.bbverts;
                
    	Vector3f min = tmp._compVect1.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f max = tmp._compVect2.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        Vector3f point;
        
        for (int i = start; i < end; i++) {
        	batch.getTriangle(i, verts);
        	point = verts[0];
            checkMinMax(min, max, point);
            point = verts[1];
            checkMinMax(min, max, point);
            point = verts[2];
            checkMinMax(min, max, point);
        }
        
        center.set(min.addLocal(max));
        center.multLocal(0.5f);

        xExtent = max.x - center.x;
        yExtent = max.y - center.y;
        zExtent = max.z - center.z;
    }

    private void checkMinMax(Vector3f min, Vector3f max, Vector3f point) {
        if (point.x < min.x)
            min.x = point.x;
        else if (point.x > max.x)
            max.x = point.x;
        if (point.y < min.y)
            min.y = point.y;
        else if (point.y > max.y)
            max.y = point.y;
        if (point.z < min.z)
            min.z = point.z;
        else if (point.z > max.z)
            max.z = point.z;
    }

    /**
     * <code>containAABB</code> creates a minimum-volume axis-aligned bounding
     * box of the points, then selects the smallest enclosing sphere of the box
     * with the sphere centered at the boxes center.
     * 
     * @param points
     *            the list of points.
     */
    public void containAABB(FloatBuffer points) {
        
        if (points == null)
            return;

        points.rewind();
        if (points.remaining() <= 2) // we need at least a 3 float vector
            return;

        Context tmp=LocalContext.getContext();
        
        BufferUtils.populateFromBuffer(tmp._compVect1, points, 0);
        float minX = tmp._compVect1.x, minY = tmp._compVect1.y, minZ = tmp._compVect1.z;
        float maxX = tmp._compVect1.x, maxY = tmp._compVect1.y, maxZ = tmp._compVect1.z;

        for (int i = 1, len = points.remaining() / 3; i < len; i++) {
            BufferUtils.populateFromBuffer(tmp._compVect1, points, i);

            if (tmp._compVect1.x < minX)
                minX = tmp._compVect1.x;
            else if (tmp._compVect1.x > maxX)
                maxX = tmp._compVect1.x;

            if (tmp._compVect1.y < minY)
                minY = tmp._compVect1.y;
            else if (tmp._compVect1.y > maxY)
                maxY = tmp._compVect1.y;

            if (tmp._compVect1.z < minZ)
                minZ = tmp._compVect1.z;
            else if (tmp._compVect1.z > maxZ)
                maxZ = tmp._compVect1.z;
        }

        center.set(minX + maxX, minY + maxY, minZ + maxZ);
        center.multLocal(0.5f);

        xExtent = maxX - center.x;
        yExtent = maxY - center.y;
        zExtent = maxZ - center.z;
    }

    public void containAABB(VertexBuffer vb, int startVertex, int numVertex) {
        
        if (vb == null)
            return;

        // get the position data in the buffer
        VertexAttribute posAtt = vb.getFormat().getAttribute(VertexAttribute.USAGE_POSITION);
        // if the buffer does not contain position data, return
        if( posAtt == null)
            return;
        // get the data buffer
        FloatBuffer fb = vb.getDataBuffer();
        if( fb == null)
            return;
        fb.rewind();
        
        // determine the postion and stride, in floats
        int stride = vb.getFormat().getSize();
        // the start, in floats
        int start = posAtt.startfloat;
        
        fb.position(startVertex*stride + start);
        
        if (fb.remaining() <= posAtt.floats) // we need at least a 3 float vector
            return;

        int len = fb.remaining() / stride;
        if( len > numVertex)
            len = numVertex;

        Context tmp=LocalContext.getContext();

        tmp._compVect1.x = fb.get(startVertex*stride + start);
        tmp._compVect1.y = fb.get(startVertex*stride + start+1);
        tmp._compVect1.z = fb.get(startVertex*stride + start+2);
        float minX = tmp._compVect1.x, minY = tmp._compVect1.y, minZ = tmp._compVect1.z;
        float maxX = tmp._compVect1.x, maxY = tmp._compVect1.y, maxZ = tmp._compVect1.z;
        
        for (int i = 1; i < len; i++) {
            int index = (startVertex+ i)*stride + start;
            tmp._compVect1.x = fb.get(index);
            tmp._compVect1.y = fb.get(index+1);
            tmp._compVect1.z = fb.get(index+2);

            if (tmp._compVect1.x < minX)
                minX = tmp._compVect1.x;
            else if (tmp._compVect1.x > maxX)
                maxX = tmp._compVect1.x;

            if (tmp._compVect1.y < minY)
                minY = tmp._compVect1.y;
            else if (tmp._compVect1.y > maxY)
                maxY = tmp._compVect1.y;

            if (tmp._compVect1.z < minZ)
                minZ = tmp._compVect1.z;
            else if (tmp._compVect1.z > maxZ)
                maxZ = tmp._compVect1.z;
        }

        center.set(minX + maxX, minY + maxY, minZ + maxZ);
        center.multLocal(0.5f);

        xExtent = maxX - center.x;
        yExtent = maxY - center.y;
        zExtent = maxZ - center.z;
    }
    
    /**
     * <code>transform</code> modifies the center of the box to reflect the
     * change made via a rotation, translation and scale.
     * 
     * @param rotate
     *            the rotation change.
     * @param translate
     *            the translation change.
     * @param scale
     *            the size change.
     * @param store
     *            box to store result in
     */
    public BoundingVolume transform(Quaternion rotate, Vector3f translate,
            Vector3f scale, BoundingVolume store) {

        BoundingBox box;
        if (store == null || ! ( store instanceof BoundingBox ) ) {
            box = new BoundingBox();
        } else {
            box = (BoundingBox) store;
        }

        center.mult(scale, box.center);
        rotate.mult(box.center, box.center);
        box.center.addLocal(translate);

        Context tmp=LocalContext.getContext();
        
        Matrix3f transMatrix = tmp._compMat;
        transMatrix.set(rotate);
        // Make the rotation matrix all positive to get the maximum x/y/z extent
        transMatrix.m00 = FastMath.abs(transMatrix.m00);
        transMatrix.m01 = FastMath.abs(transMatrix.m01);
        transMatrix.m02 = FastMath.abs(transMatrix.m02);
        transMatrix.m10 = FastMath.abs(transMatrix.m10);
        transMatrix.m11 = FastMath.abs(transMatrix.m11);
        transMatrix.m12 = FastMath.abs(transMatrix.m12);
        transMatrix.m20 = FastMath.abs(transMatrix.m20);
        transMatrix.m21 = FastMath.abs(transMatrix.m21);
        transMatrix.m22 = FastMath.abs(transMatrix.m22);

        tmp._compVect1.set(xExtent * scale.x, yExtent * scale.y, zExtent * scale.z);
        transMatrix.mult(tmp._compVect1, tmp._compVect2);
        // Assign the biggest rotations after scales.
        box.xExtent = FastMath.abs(tmp._compVect2.x);
        box.yExtent = FastMath.abs(tmp._compVect2.y);
        box.zExtent = FastMath.abs(tmp._compVect2.z);

        return box;
    }

    /**
     * <code>whichSide</code> takes a plane (typically provided by a view
     * frustum) to determine which side this bound is on.
     * 
     * @param plane
     *            the plane to check against.
     */
    public int whichSide(Plane plane) {
        float radius = FastMath.abs(xExtent * plane.normal.x)
                + FastMath.abs(yExtent * plane.normal.y)
                + FastMath.abs(zExtent * plane.normal.z);

        float distance = plane.pseudoDistance(center);

        //changed to < and > to prevent floating point precision problems
        if (distance < -radius) {
            return Plane.NEGATIVE_SIDE;
        } else if (distance > radius) {
            return Plane.POSITIVE_SIDE;
        } else {
            return Plane.NO_SIDE;
        }
    }

    /**
     * <code>merge</code> combines this sphere with a second bounding sphere.
     * This new sphere contains both bounding spheres and is returned.
     * 
     * @param volume
     *            the sphere to combine with this sphere.
     * @return the new sphere
     */
    public BoundingVolume merge(BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {
            case BoundingVolume.BOUNDING_BOX: {
                BoundingBox vBox = (BoundingBox) volume;
                return merge(vBox.center, vBox.xExtent, vBox.yExtent,
                        vBox.zExtent, new BoundingBox(new Vector3f(0, 0, 0), 0,
                                0, 0));
            }

            case BoundingVolume.BOUNDING_SPHERE: {
                BoundingSphere vSphere = (BoundingSphere) volume;
                return merge(vSphere.center, vSphere.radius, vSphere.radius,
                        vSphere.radius, new BoundingBox(new Vector3f(0, 0, 0),
                                0, 0, 0));
            }
/* TODO: impelement       
            //Treating Capsule like sphere, inefficient
            case BoundingVolume.BOUNDING_CAPSULE: {
                BoundingCapsule capsule = (BoundingCapsule) volume;
                float totalRadius = capsule.getRadius() + capsule.getLineSegment().getExtent();
                return merge(capsule.center, totalRadius, totalRadius,
                        totalRadius, new BoundingBox(new Vector3f(0, 0, 0),
                                0, 0, 0));
            }

            case BoundingVolume.BOUNDING_OBB: {
                OrientedBoundingBox box = (OrientedBoundingBox) volume;
                BoundingBox rVal = (BoundingBox) this.clone(null);
                return rVal.mergeOBB(box);
            }
*/
            default:
                return null;
        }
    }

    /**
     * <code>mergeLocal</code> combines this sphere with a second bounding
     * sphere locally. Altering this sphere to contain both the original and the
     * additional sphere volumes;
     * 
     * @param volume
     *            the sphere to combine with this sphere.
     * @return this
     */
    public BoundingVolume mergeLocal(BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {
            case BoundingVolume.BOUNDING_BOX: {
                BoundingBox vBox = (BoundingBox) volume;
                return merge(vBox.center, vBox.xExtent, vBox.yExtent,
                        vBox.zExtent, this);
            }

            case BoundingVolume.BOUNDING_SPHERE: {
                BoundingSphere vSphere = (BoundingSphere) volume;
                return merge(vSphere.center, vSphere.radius, vSphere.radius,
                        vSphere.radius, this);
            }
/* TODO: impelement
            //Treating capsule like sphere, inefficient
            case BoundingVolume.BOUNDING_CAPSULE: {
                BoundingCapsule capsule = (BoundingCapsule) volume;
                float totalRadius = capsule.getRadius() + capsule.getLineSegment().getExtent();
                return merge(capsule.center, totalRadius, totalRadius,
                		totalRadius, this);
            }

            case BoundingVolume.BOUNDING_OBB: {
                return mergeOBB((OrientedBoundingBox) volume);
            }
*/
            default:
                return null;
        }
    }

    /**
     * Merges this AABB with the given OBB.
     * 
     * @param volume
     *            the OBB to merge this AABB with.
     * @return This AABB extended to fit the given OBB.
     */
    /* TODO: implement
    private BoundingBox mergeOBB(OrientedBoundingBox volume) {
        if (!volume.correctCorners)
            volume.computeCorners();

        Vector3f min = _compVect1.set(center.x - xExtent, center.y - yExtent,
                center.z - zExtent);
        Vector3f max = _compVect2.set(center.x + xExtent, center.y + yExtent,
                center.z + zExtent);

        for (int i = 1; i < volume.vectorStore.length; i++) {
            Vector3f temp = volume.vectorStore[i];
            if (temp.x < min.x)
                min.x = temp.x;
            else if (temp.x > max.x)
                max.x = temp.x;

            if (temp.y < min.y)
                min.y = temp.y;
            else if (temp.y > max.y)
                max.y = temp.y;

            if (temp.z < min.z)
                min.z = temp.z;
            else if (temp.z > max.z)
                max.z = temp.z;
        }

        center.set(min.addLocal(max));
        center.multLocal(0.5f);

        xExtent = max.x - center.x;
        yExtent = max.y - center.y;
        zExtent = max.z - center.z;
        return this;
    }
    */
    
    /**
     * <code>merge</code> combines this bounding box with another box which is
     * defined by the center, x, y, z extents.
     * 
     * @param boxCenter
     *            the center of the box to merge with
     * @param boxX
     *            the x extent of the box to merge with.
     * @param boxY
     *            the y extent of the box to merge with.
     * @param boxZ
     *            the z extent of the box to merge with.
     * @param rVal
     *            the resulting merged box.
     * @return the resulting merged box.
     */
    private BoundingBox merge(Vector3f boxCenter, float boxX, float boxY,
            float boxZ, BoundingBox rVal) {

        Context tmp = LocalContext.getContext();
        
        tmp._compVect1.x = center.x - xExtent;
        if (tmp._compVect1.x > boxCenter.x - boxX)
            tmp._compVect1.x = boxCenter.x - boxX;
        tmp._compVect1.y = center.y - yExtent;
        if (tmp._compVect1.y > boxCenter.y - boxY)
            tmp._compVect1.y = boxCenter.y - boxY;
        tmp._compVect1.z = center.z - zExtent;
        if (tmp._compVect1.z > boxCenter.z - boxZ)
            tmp._compVect1.z = boxCenter.z - boxZ;

        tmp._compVect2.x = center.x + xExtent;
        if (tmp._compVect2.x < boxCenter.x + boxX)
            tmp._compVect2.x = boxCenter.x + boxX;
        tmp._compVect2.y = center.y + yExtent;
        if (tmp._compVect2.y < boxCenter.y + boxY)
            tmp._compVect2.y = boxCenter.y + boxY;
        tmp._compVect2.z = center.z + zExtent;
        if (tmp._compVect2.z < boxCenter.z + boxZ)
            tmp._compVect2.z = boxCenter.z + boxZ;

        center.set(tmp._compVect2).addLocal(tmp._compVect1).multLocal(0.5f);

        xExtent = tmp._compVect2.x - center.x;
        yExtent = tmp._compVect2.y - center.y;
        zExtent = tmp._compVect2.z - center.z;

        return rVal;
    }

    /**
     * <code>clone</code> creates a new BoundingBox object containing the same
     * data as this one.
     * 
     * @param store
     *            where to store the cloned information. if null or wrong class,
     *            a new store is created.
     * @return the new BoundingBox
     */
    public BoundingVolume clone(BoundingVolume store) {
        if (store != null && store instanceof BoundingBox) {
            BoundingBox rVal = (BoundingBox) store;
            rVal.center.set(center);
            rVal.xExtent = xExtent;
            rVal.yExtent = yExtent;
            rVal.zExtent = zExtent;
            rVal.checkPlane = checkPlane;
            return rVal;
        }
        
        BoundingBox rVal = new BoundingBox(
                (center != null ? (Vector3f) center.clone() : null),
                xExtent, yExtent, zExtent);
        return rVal;
    }

    /**
     * <code>toString</code> returns the string representation of this object.
     * The form is: "Radius: RRR.SSSS Center: <Vector>".
     * 
     * @return the string representation of this.
     */
    @Override
    public String toString() {
        return "BoundingBox [Center: " + center + "  xExtent: "
                + xExtent + "  yExtent: " + yExtent + "  zExtent: " + zExtent
                + "]";
    }

    /**
     * intersects determines if this Bounding Box intersects with another given
     * bounding volume. If so, true is returned, otherwise, false is returned.
     * 
     * @see com.jme.bounding.BoundingVolume#intersects(com.jme.bounding.BoundingVolume)
     */
    public boolean intersects(BoundingVolume bv) {
        if (bv == null)
            return false;
       
        return bv.intersectsBoundingBox(this);
    }

    /**
     * determines if this bounding box intersects a given bounding sphere.
     * 
     * @see com.vlengine.bounding.BoundingVolume#intersectsSphere(com.vlengine.bounding.BoundingSphere)
     */
    public boolean intersectsSphere(BoundingSphere bs) {
        if (FastMath.abs(center.x - bs.getCenter().x) < bs.getRadius()
                + xExtent
                && FastMath.abs(center.y - bs.getCenter().y) < bs.getRadius()
                        + yExtent
                && FastMath.abs(center.z - bs.getCenter().z) < bs.getRadius()
                        + zExtent)
            return true;

        return false;
    }

    /**
     * determines if this bounding box intersects a given bounding box. If the
     * two boxes intersect in any way, true is returned. Otherwise, false is
     * returned.
     * 
     */
    public boolean intersectsBoundingBox(BoundingBox bb) {
        if (center.x + xExtent < bb.center.x - bb.xExtent
                || center.x - xExtent > bb.center.x + bb.xExtent)
            return false;
        else if (center.y + yExtent < bb.center.y - bb.yExtent
                || center.y - yExtent > bb.center.y + bb.yExtent)
            return false;
        else if (center.z + zExtent < bb.center.z - bb.zExtent
                || center.z - zExtent > bb.center.z + bb.zExtent)
            return false;
        else
            return true;
    }

    /**
     * determines if this bounding box intersects with a given oriented bounding
     * box.
     * 
     * @see com.jme.bounding.BoundingVolume#intersectsOrientedBoundingBox(com.jme.bounding.OrientedBoundingBox)
     */
/* TODO: implement
    public boolean intersectsOrientedBoundingBox(OrientedBoundingBox obb) {
        return obb.intersectsBoundingBox(this);
    }
*/
    /**
     * determines if this bounding box intersects with a given bounding capsule.
     * 
     * @see com.jme.bounding.BoundingVolume#intersectsCapsule(BoundingCapsule)
     */
/* TODO: impelement
    public boolean intersectsCapsule(BoundingCapsule bc) {
    	return bc.intersectsBoundingBox(this);
    }
*/
    /**
     * determines if this bounding box intersects with a given ray object. If an
     * intersection has occurred, true is returned, otherwise false is returned.
     * 
     * @see com.jme.bounding.BoundingVolume#intersects(com.jme.math.Ray)
     */
    public boolean intersects(Ray ray) {
        float rhs;

        Context tmp = LocalContext.getContext();
        
        Vector3f diff = ray.origin.subtract(getCenter(tmp._compVect2), tmp._compVect1);

        tmp.fWdU[0] = ray.getDirection().dot(Vector3f.UNIT_X);
        tmp.fAWdU[0] = FastMath.abs(tmp.fWdU[0]);
        tmp.fDdU[0] = diff.dot(Vector3f.UNIT_X);
        tmp.fADdU[0] = FastMath.abs(tmp.fDdU[0]);
        if (tmp.fADdU[0] > xExtent && tmp.fDdU[0] * tmp.fWdU[0] >= 0.0) {
            return false;
        }

        tmp.fWdU[1] = ray.getDirection().dot(Vector3f.UNIT_Y);
        tmp.fAWdU[1] = FastMath.abs(tmp.fWdU[1]);
        tmp.fDdU[1] = diff.dot(Vector3f.UNIT_Y);
        tmp.fADdU[1] = FastMath.abs(tmp.fDdU[1]);
        if (tmp.fADdU[1] > yExtent && tmp.fDdU[1] * tmp.fWdU[1] >= 0.0) {
            return false;
        }

        tmp.fWdU[2] = ray.getDirection().dot(Vector3f.UNIT_Z);
        tmp.fAWdU[2] = FastMath.abs(tmp.fWdU[2]);
        tmp.fDdU[2] = diff.dot(Vector3f.UNIT_Z);
        tmp.fADdU[2] = FastMath.abs(tmp.fDdU[2]);
        if (tmp.fADdU[2] > zExtent && tmp.fDdU[2] * tmp.fWdU[2] >= 0.0) {
            return false;
        }

        Vector3f wCrossD = ray.getDirection().cross(diff, tmp._compVect2);

        tmp.fAWxDdU[0] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_X));
        rhs = yExtent * tmp.fAWdU[2] + zExtent * tmp.fAWdU[1];
        if (tmp.fAWxDdU[0] > rhs) {
            return false;
        }

        tmp.fAWxDdU[1] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_Y));
        rhs = xExtent * tmp.fAWdU[2] + zExtent * tmp.fAWdU[0];
        if (tmp.fAWxDdU[1] > rhs) {
            return false;
        }

        tmp.fAWxDdU[2] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_Z));
        rhs = xExtent * tmp.fAWdU[1] + yExtent * tmp.fAWdU[0];
        if (tmp.fAWxDdU[2] > rhs) {
            return false;

        }

        return true;
    }

    /**
     * @see com.vlengine.bounding.BoundingVolume#intersectsWhere(com.vlengine.math.Ray)
     */
    public IntersectionRecord intersectsWhere(Ray ray) {
        Context tmp=LocalContext.getContext();
        
        Vector3f diff = tmp._compVect1.set(ray.origin).subtractLocal(center);
        // convert ray to box coordinates
        Vector3f direction = tmp._compVect2.set(ray.direction);
        
        // Warning: the context record is used as return parameter
        // which means this method can handle one call at a time (per thread)
        // later invocations overwrite previouse return values
        IntersectionRecord record=tmp.parIntersectionRecord;
        
        float[] t = record.getDistances(2);
        t[0] = 0f; t[1] = Float.POSITIVE_INFINITY;
        
        float saveT0 = t[0], saveT1 = t[1];
        boolean notEntirelyClipped = clip(+direction.x, -diff.x - xExtent, t)
                && clip(-direction.x, +diff.x - xExtent, t)
                && clip(+direction.y, -diff.y - yExtent, t)
                && clip(-direction.y, +diff.y - yExtent, t)
                && clip(+direction.z, -diff.z - zExtent, t)
                && clip(-direction.z, +diff.z - zExtent, t);
        
        if (notEntirelyClipped && (t[0] != saveT0 || t[1] != saveT1)) {
            if (t[1] > t[0]) {
                Vector3f[] points = record.getPoints(2);
                points[0].set(ray.direction).multLocal(t[0]).addLocal(ray.origin);
                points[1].set(ray.direction).multLocal(t[1]).addLocal(ray.origin);
                return record;
            } 
            
            float[] distances = record.getDistances(1);
            distances[0] = t[0];
            Vector3f[] points = record.getPoints(1);
            points[0].set(ray.direction).multLocal(distances[0]).addLocal(ray.origin);
            return record;            
        } 
        record.getDistances(0);
        return record;
       
    }

    @Override
    public boolean contains(Vector3f point) {
        return FastMath.abs(center.x - point.x) < xExtent
                && FastMath.abs(center.y - point.y) < yExtent
                && FastMath.abs(center.z - point.z) < zExtent;
    }

    public float distanceToEdge(Vector3f point) {
        // compute coordinates of point in box coordinate system
        // TODO: get this vector from tmp
        Vector3f closest = point.subtract(center);

        // project test point onto box
        float sqrDistance = 0.0f;
        float delta;

        if (closest.x < -xExtent) {
            delta = closest.x + xExtent;
            sqrDistance += delta * delta;
            closest.x = -xExtent;
        } else if (closest.x > xExtent) {
            delta = closest.x - xExtent;
            sqrDistance += delta * delta;
            closest.x = xExtent;
        }

        if (closest.y < -yExtent) {
            delta = closest.y + yExtent;
            sqrDistance += delta * delta;
            closest.y = -yExtent;
        } else if (closest.y > yExtent) {
            delta = closest.y - yExtent;
            sqrDistance += delta * delta;
            closest.y = yExtent;
        }

        if (closest.z < -zExtent) {
            delta = closest.z + zExtent;
            sqrDistance += delta * delta;
            closest.z = -zExtent;
        } else if (closest.z > zExtent) {
            delta = closest.z - zExtent;
            sqrDistance += delta * delta;
            closest.z = zExtent;
        }

        return FastMath.sqrt(sqrDistance);
    }

    /**
     * Calculates the distance the other bounding box penetrates this
     * bounding box
     * @param bb
     * @param store
     * @return Returns null if the other bound is not inside our bound
     */
    public Vector3f distanceInside(BoundingBox other, Vector3f store) {
        store.zero();
        Vector3f centerdist = other.getCenter().subtract(center);
        // is it inside ton the x extent
        if(centerdist.x > 0) {
            if(centerdist.x - other.xExtent < xExtent) {
                store.x = xExtent - (centerdist.x - other.xExtent);
            } else 
                return null;
        } else {
            if(centerdist.x + other.xExtent > -xExtent) {
                store.x = (centerdist.x + other.xExtent) - xExtent;
            } else 
                return null;
        }
        if(centerdist.y > 0) {
            if(centerdist.y - other.yExtent < yExtent) {
                store.y = yExtent - (centerdist.y - other.yExtent);
            } else 
                return null;
        } else {
            if(centerdist.y + other.yExtent > -yExtent) {
                store.y = (centerdist.y + other.yExtent) - yExtent;
            } else 
                return null;
        }
        if(centerdist.z > 0) {
            if(centerdist.z - other.zExtent < zExtent) {
                store.z = zExtent - (centerdist.z - other.zExtent);
            } else 
                return null;
        } else {
            if(centerdist.z + other.zExtent > -zExtent) {
                store.z = (centerdist.z + other.zExtent) - zExtent;
            } else 
                return null;
        }
        return store;
    }

    public Vector3f distanceInside(Vector3f point, Vector3f store) {
        store.zero();
        Vector3f centerdist = point.subtract(center);
        // is it inside ton the x extent
        if(centerdist.x > 0) {
            if(centerdist.x < xExtent) {
                store.x = xExtent - centerdist.x ;
            } else 
                return null;
        } else {
            if(centerdist.x > -xExtent) {
                store.x = centerdist.x - xExtent;
            } else 
                return null;
        }
        if(centerdist.y > 0) {
            if(centerdist.y < yExtent) {
                store.y = yExtent - centerdist.y;
            } else 
                return null;
        } else {
            if(centerdist.y > -yExtent) {
                store.y = centerdist.y - yExtent;
            } else 
                return null;
        }
        if(centerdist.z > 0) {
            if(centerdist.z < zExtent) {
                store.z = zExtent - centerdist.z;
            } else 
                return null;
        } else {
            if(centerdist.z > -zExtent) {
                store.z = centerdist.z - zExtent;
            } else 
                return null;
        }
        return store;
    }
    
    /**
     * <code>clip</code> determines if a line segment intersects the current
     * test plane.
     * 
     * @param denom
     *            the denominator of the line segment.
     * @param numer
     *            the numerator of the line segment.
     * @param t
     *            test values of the plane.
     * @return true if the line segment intersects the plane, false otherwise.
     */
    private boolean clip(float denom, float numer, float[] t) {
        // Return value is 'true' if line segment intersects the current test
        // plane. Otherwise 'false' is returned in which case the line segment
        // is entirely clipped.
        if (denom > 0.0f) {
            if (numer > denom * t[1])
                return false;
            if (numer > denom * t[0])
                t[0] = numer / denom;
            return true;
        } else if (denom < 0.0f) {
            if (numer > denom * t[0])
                return false;
            if (numer > denom * t[1])
                t[1] = numer / denom;
            return true;
        } else {
            return numer <= 0.0;
        }
    }

    /**
     * Query extent.
     * 
     * @param store
     *            where extent gets stored - null to return a new vector
     * @return store / new vector
     */
    public Vector3f getExtent(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(xExtent, yExtent, zExtent);
        return store;
    }
    
    @Override
    public float getVolume() {
        return (8*xExtent*yExtent*zExtent);
    }
    
    public boolean contains(BoundingBox other) {
        if( other.center.x - other.xExtent < center.x - xExtent )
            return false;
        if( other.center.x + other.xExtent > center.x + xExtent )
            return false;
        if( other.center.y - other.yExtent < center.y - yExtent )
            return false;
        if( other.center.y + other.yExtent > center.y + yExtent )
            return false;
        if( other.center.z - other.zExtent < center.z - zExtent )
            return false;
        if( other.center.z + other.zExtent > center.z + zExtent )
            return false;
        return true;
    }
}
