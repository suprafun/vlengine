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

import com.vlengine.intersection.Intersection;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Ray;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Geometry;
import com.vlengine.scene.Spatial;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.IntList;
import com.vlengine.util.SortUtil;

/**
 * TODO:There is bug in triangle collision calculation
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class CollisionTree {


	/**
	 * defines a CollisionTree as using Oriented Bounding Boxes.
	 */
	public static final int OBB_TREE = 0;

	/**
	 * defines a CollisionTree as using Axis-Aligned Bounding Boxes.
	 */
	public static final int AABB_TREE = 1;

	/**
	 * defines a CollisionTree as using Bounding Spheres.
	 */
	public static final int SPHERE_TREE = 2;
	
	//Default tree is axis-aligned
	private int type = AABB_TREE;

	//children trees
	private CollisionTree left;
	private CollisionTree right;

	//bounding volumes that contain the triangles that the node is 
	//handling
	private BoundingVolume bounds;
	//private BoundingVolume worldBounds;

	//the list of triangle indices that compose the tree. This list
	//contains all the triangles of the batch and is shared between
	//all nodes of this tree.
	private int[] triIndex;

	//Defines the pointers into the triIndex array that this node is
	//directly responsible for.
	private int start, end;

	//Required Spatial information
	//private Geometry parent;
	private Geometry batch;
	
	/**
	 * Constructor creates a new instance of CollisionTree. The type of tree
	 * is provided as a parameter with valid options being:
	 * AABB_TREE, OBB_TREE, SPHERE_TREE.
	 * @param type the type of collision tree to make
	 */
	public CollisionTree(int type) {
		this.type = type;
	}

	/**
	 * Recreate this Collision Tree for the given TriMesh and batch
	 * index.
	 * 
	 * @param batchIndex the index of the batch to generate the tree for.
	 * @param parent
	 *            The Geometry that this OBBTree should represent.
	 * @param doSort true to sort triangles during creation, false otherwise
	 */
	public void construct(Geometry batch, boolean doSort) {
                this.batch = batch;
                int triCount = batch.getTriangleCount();
                triIndex = new int[triCount];
                for(int i=0; i<triCount; i++) {
                    triIndex[i] = i;
                }
                createTree(0, triCount, doSort);
	}

	/**
	 * Creates a Collision Tree by recursively creating children nodes, splitting
	 * the triangles this node is responsible for in half until the desired
	 * triangle count is reached.
	 * 
	 * @param start
	 *            The start index of the tris, inclusive.
	 * @param end
	 *            The end index of the tris, exclusive.
	 * @param doSort
	 * 			  True if the triangles should be sorted at each level,
	 * 			false otherwise.
	 */
	protected void createTree(int start, int end, boolean doSort) {
		this.start = start;
		this.end = end;
		
		if (triIndex == null) {
			return;
		}

		createBounds();

		// the bounds at this level should contain all the triangles this level
		// is reponsible for.
		bounds.computeFromTris(batch, start, end);

		// check to see if we are a leaf, if the number of triangles we
		// reference is less than or equal to the maximum defined by the
		// CollisionTreeManager we are done.
		if (end - start + 1 <= CollisionTreeManager.getInstance()
				.getMaxTrisPerLeaf()) {
			return;
		}

		// if doSort is set we need to attempt to optimize the referenced
		// triangles.
		// optimizing the sorting of the triangles will help group them
		// spatially
		// in the left/right children better.
		if (doSort) {
			sortTris();
		}

		// create the left child, it will reference many of our values (index,
		// parent, batch)
		if (left == null) {
			left = new CollisionTree(type);
		}

		left.triIndex = this.triIndex;
		//left.parent = this.parent;
		left.batch = this.batch;
		left.createTree(start, (start + end) / 2, doSort);

		// create the right child, it will reference many of our values (index,
		// parent, batch)
		if (right == null) {
			right = new CollisionTree(type);
		}
		right.triIndex = this.triIndex;
		//right.parent = this.parent;
		right.batch = this.batch;
		right.createTree((start + end) / 2, end, doSort);
	}
	
	/**
	 * Tests if the world bounds of the node at this level interesects a 
	 * provided bounding volume. If an intersection occurs, true is 
	 * returned, otherwise false is returned. If the provided volume is
	 * invalid, false is returned.
	 * @param volume the volume to intersect with.
	 * @return true if there is an intersect, false otherwise.
	 */
	public boolean intersectsBounding(BoundingVolume myBV, BoundingVolume volume) {
		switch(volume.getType()) {
		case BoundingVolume.BOUNDING_BOX:
			return myBV.intersectsBoundingBox((BoundingBox)volume);
/* TODO: implement
		case BoundingVolume.BOUNDING_OBB:
			return worldBounds.intersectsOrientedBoundingBox((OrientedBoundingBox)volume);
  */
		case BoundingVolume.BOUNDING_SPHERE:
			return myBV.intersectsSphere((BoundingSphere)volume);

		default:
			return false;
		}
		
	}
        
	/**
	 * Determines if this Collision Tree intersects the given CollisionTree. If
	 * a collision occurs, true is returned, otherwise false is returned. If
	 * the provided collisionTree is invalid, false is returned.
	 * 
	 * @param collisionTree
	 *            The Tree to test.
	 * @return True if they intersect, false otherwise.
	 */
	public boolean intersect(CollisionTree collisionTree, Spatial myParent, Spatial otherParent, BoundingVolume worldBound) {
		if (collisionTree == null) {
			return false;
		}
                
                
                
                // get worldBound from temp, we have to carefully operate here, not to overwrite
                if( worldBound == null) {
                    worldBound = getTempWorldBound( myParent );
                }
                 
                BoundingVolume othWB = collisionTree.getTempWorldBound( otherParent );
                
		// our two collision bounds do not intersect, therefore, our triangles must
		// not intersect. Return false.
		if (!intersectsBounding(worldBound, othWB)) {
                    // release allocated othWB
                    //tmp.ctbstack.release(othWB.getType());
			return false;
		}

		// check children
		if (left != null) { // This is not a leaf
			if (collisionTree.intersect(left, otherParent, myParent, othWB)) {
				return true;
			}
			if (collisionTree.intersect(right,  otherParent, myParent, othWB)) {
				return true;
			}
			return false;
		}

		// This is a leaf
		if (collisionTree.left != null) { // but collision isn't
			if (intersect(collisionTree.left, myParent, otherParent, worldBound)) {
				return true;
			}
			if (intersect(collisionTree.right, myParent, otherParent, worldBound)) {
				return true;
			}
			return false;
		}

		// both are leaves
		Quaternion roti = myParent.getWorldRotation();
		Vector3f scalei = myParent.getWorldScale();
		Vector3f transi = myParent.getWorldTranslation();

		Quaternion rotj = otherParent.getWorldRotation();
		Vector3f scalej = otherParent.getWorldScale();
		Vector3f transj = otherParent.getWorldTranslation();

                Context tmp = LocalContext.getContext();
                final Vector3f[] verts = tmp.ctverts;
                final Vector3f[] target = tmp.cttarget;
                
		//for every triangle to compare, put them into world space and check
		//for intersections
		for (int i = start; i < end; i++) {
			batch.getTriangle(triIndex[i], verts);
                        roti.mult(tmp.tempVa.set(verts[0]).multLocal(scalei), tmp.tempVa).addLocal(transi);
			roti.mult(tmp.tempVb.set(verts[1]).multLocal(scalei), tmp.tempVb).addLocal(transi);
			roti.mult(tmp.tempVc.set(verts[2]).multLocal(scalei), tmp.tempVc).addLocal(transi);
			for (int j = collisionTree.start; j < collisionTree.end; j++) {
				collisionTree.batch.getTriangle(collisionTree.triIndex[j],
						target);
                                rotj.mult(tmp.tempVd.set(target[0]).multLocal(scalej), tmp.tempVd).addLocal(transj);
				rotj.mult(tmp.tempVe.set(target[1]).multLocal(scalej), tmp.tempVe).addLocal(transj);
				rotj.mult(tmp.tempVf.set(target[2]).multLocal(scalej), tmp.tempVf).addLocal(transj);
				if (Intersection.intersection(tmp.tempVa, tmp.tempVb, tmp.tempVc, tmp.tempVd,
						tmp.tempVe, tmp.tempVf))
					return true;
			}
		}
		return false;
	}

        private BoundingVolume getTempWorldBound( Spatial parent ) {
            Context tmp = LocalContext.getContext();
                // create by transform from our own bound
            return bounds.transform(parent.getWorldRotation(),
                                            parent.getWorldTranslation(),
                                            parent.getWorldScale(),
                                            tmp.ctbstack.fetchBound(bounds.getType()));
        }
        
	/**
	 * Determines if this Collision Tree intersects the given CollisionTree. If
	 * a collision occurs, true is returned, otherwise false is returned. If
	 * the provided collisionTree is invalid, false is returned. All collisions
	 * that occur are stored in lists as an integer index into the mesh's 
	 * triangle buffer. where aList is the triangles for this mesh and bList 
	 * is the triangles for the test tree.
	 * 
	 * @param collisionTree
	 *            The Tree to test.
	 * @param aList a list to contain the colliding triangles of this mesh.
	 * @param bList a list to contain the colliding triangles of the testing mesh.
	 * @return True if they intersect, false otherwise.
	 */
	public boolean intersect(CollisionTree collisionTree, IntList aList,
			IntList bList, Spatial myParent, Spatial otherParent, BoundingVolume worldBound) {
		
		if (collisionTree == null) {
			return false;
		}
		
                // get worldBound from temp, we have to carefully operate here, not to overwrite
                if( worldBound == null) {
                    worldBound = getTempWorldBound( myParent );
                }
                 
                BoundingVolume othWB = collisionTree.getTempWorldBound( otherParent );
                
		// our two collision bounds do not intersect, therefore, our triangles must
		// not intersect. Return false.		
		if (!intersectsBounding(worldBound, othWB)) {
			return false;
		}
		
		//if our node is not a leaf send the children (both left and right) to
		// the test tree.
		if (left != null) { // This is not a leaf
			boolean test = collisionTree.intersect(left, bList, aList, myParent, otherParent, othWB);
			test = collisionTree.intersect(right, bList, aList, myParent, otherParent, othWB) || test;
			return test;
		}

		// This node is a leaf, but the testing tree node is not. Therefore,
		// continue processing the testing tree until we find its leaves.
		if (collisionTree.left != null) {
			boolean test = intersect(collisionTree.left, aList, bList, myParent, otherParent, worldBound);
			test = intersect(collisionTree.right, aList, bList, myParent, otherParent, worldBound) || test;
			return test;
		}

		// both this node and the testing node are leaves. Therefore, we can
		// switch to checking the contained triangles with each other. Any 
		// that are found to intersect are placed in the appropriate list.
                /*
		Quaternion roti = myParent.getWorldRotation();
		Vector3f scalei = myParent.getWorldScale();
		Vector3f transi = myParent.getWorldTranslation();

		Quaternion rotj = otherParent.getWorldRotation();
		Vector3f scalej = otherParent.getWorldScale();
		Vector3f transj = otherParent.getWorldTranslation();
                 */
		
		boolean test = false;
		Context tmp = LocalContext.getContext();
                final Vector3f[] verts = tmp.ctverts;
                final Vector3f[] target = tmp.cttarget;
                
		for (int i = start; i < end; i++) {
			batch.getTriangle(triIndex[i], verts);
                        myParent.localToWorld( verts[0], tmp.tempVa );
                        myParent.localToWorld( verts[1], tmp.tempVb );
                        myParent.localToWorld( verts[2], tmp.tempVc );
                        
                        //roti.mult(tmp.tempVa.set(verts[0]).multLocal(scalei), tmp.tempVa).addLocal(transi);
			//roti.mult(tmp.tempVb.set(verts[1]).multLocal(scalei), tmp.tempVb).addLocal(transi);
			//roti.mult(tmp.tempVc.set(verts[2]).multLocal(scalei), tmp.tempVc).addLocal(transi);
			for (int j = collisionTree.start; j < collisionTree.end; j++) {
				collisionTree.batch.getTriangle(collisionTree.triIndex[j],
						target);
                                otherParent.localToWorld( target[0], tmp.tempVd );
                                otherParent.localToWorld( target[1], tmp.tempVe );
                                otherParent.localToWorld( target[2], tmp.tempVf );
                                //rotj.mult(tmp.tempVd.set(target[0]).multLocal(scalej), tmp.tempVd).addLocal(transj);
				//rotj.mult(tmp.tempVe.set(target[1]).multLocal(scalej), tmp.tempVe).addLocal(transj);
				//rotj.mult(tmp.tempVf.set(target[2]).multLocal(scalej), tmp.tempVf).addLocal(transj);
				if (Intersection.intersection(tmp.tempVa, tmp.tempVb, tmp.tempVc, tmp.tempVd,
						tmp.tempVe, tmp.tempVf)) {
					test = true;
					aList.add(triIndex[i]);
					bList.add(collisionTree.triIndex[j]);
				}
			}
		}
		return test;

	}

	public boolean triInsideBound(CollisionTree collisionTree, IntList aList,
			IntList bList, Spatial myParent, Spatial otherParent, BoundingVolume worldBound) {
		
		if (collisionTree == null) {
			return false;
		}
		
                // get worldBound from temp, we have to carefully operate here, not to overwrite
                if( worldBound == null) {
                    worldBound = getTempWorldBound( myParent );
                }
                 
                BoundingVolume othWB = collisionTree.getTempWorldBound( otherParent );
                
		// our two collision bounds do not intersect, therefore, our triangles must
		// not intersect. Return false.		
		if (!intersectsBounding(worldBound, othWB)) {
			return false;
		}
		
		//if our node is not a leaf send the children (both left and right) to
		// the test tree.
		if (left != null) { // This is not a leaf
			boolean test = collisionTree.triInsideBound(left, bList, aList, myParent, otherParent, othWB);
			test = collisionTree.triInsideBound(right, bList, aList, myParent, otherParent, othWB) || test;
			return test;
		}

		// This node is a leaf, but the testing tree node is not. Therefore,
		// continue processing the testing tree until we find its leaves.
		if (collisionTree.left != null) {
			boolean test = triInsideBound(collisionTree.left, aList, bList, myParent, otherParent, worldBound);
			test = triInsideBound(collisionTree.right, aList, bList, myParent, otherParent, worldBound) || test;
			return test;
		}

		// both this node and the testing node are leaves. Therefore, we can
		// switch to checking the contained triangles with each other. Any 
		// that are found to intersect are placed in the appropriate list.
		
		boolean test = false;
		Context tmp = LocalContext.getContext();
                //final Vector3f[] verts = tmp.ctverts;
                //final Vector3f[] target = tmp.cttarget;
                
		for (int i = start; i < end; i++) {
			//batch.getTriangle(triIndex[i], verts);
                        
                        // just put into the list of possibly colliding triangles
                        //we'll sort it out later
                        /*
                        myParent.localToWorld( verts[0], tmp.tempVa );
                        myParent.localToWorld( verts[1], tmp.tempVb );
                        myParent.localToWorld( verts[2], tmp.tempVc );
                        */
			for (int j = collisionTree.start; j < collisionTree.end; j++) {
				/*
                                collisionTree.batch.getTriangle(collisionTree.triIndex[j],target);
                                otherParent.localToWorld( target[0], tmp.tempVd );
                                otherParent.localToWorld( target[1], tmp.tempVe );
                                otherParent.localToWorld( target[2], tmp.tempVf );
				if (Intersection.intersection(tmp.tempVa, tmp.tempVb, tmp.tempVc, tmp.tempVd,
						tmp.tempVe, tmp.tempVf)) {
                                 */
					test = true;
					aList.add(triIndex[i]);
					bList.add(collisionTree.triIndex[j]);
				//}
			}
		}
		return test;

	}
        
	/**
	 * intersect checks for collisions between this collision tree and a 
	 * provided Ray. Any collisions are stored in a provided list. The ray
	 * is assumed to have a normalized direction for accurate calculations.
	 * @param ray the ray to test for intersections.
	 * @param triList the list to store instersections with.
	 */
	public void intersect(Ray ray, IntList triList, Spatial myParent, BoundingVolume worldBound) {
                //Context tmp = LocalContext.getContext();
                
                if(worldBound == null) 
                    worldBound = getTempWorldBound( myParent );
                
		// if our ray doesn't hit the bounds, then it must not hit a triangle.
		if (!worldBound.intersects(ray)) {
			return;
		}

		//This is not a leaf node, therefore, check each child (left/right) for
		//intersection with the ray.
		if (left != null) {
			left.intersect(ray, triList, myParent, null);
		}
		if (right != null) {
			right.intersect(ray, triList, myParent, null);
		} else if (left == null) {
			//This is a leaf node. We can therfore, check each triangle this
			//node contains. If an intersection occurs, place it in the 
			//list.
                        Context tmp = LocalContext.getContext();
                        final Vector3f[] verts = tmp.ctverts;
			for (int i = start; i < end; i++) {
				batch.getTriangle(triIndex[i], verts);
                                myParent.localToWorld( verts[0], tmp.tempVa );
                                myParent.localToWorld( verts[1], tmp.tempVb );
                                myParent.localToWorld( verts[2], tmp.tempVc );
				if (ray.intersect(tmp.tempVa, tmp.tempVb, tmp.tempVc)) {
					triList.add(triIndex[i]);
                                }
			}
		}
	}

	/**
	 * Returns the bounding volume for this tree node in local space.
	 * @return the bounding volume for this tree node in local space.
	 */
	public BoundingVolume getBounds() {
		return bounds;
	}

	/**
	 * Returns the bounding volume for this tree node in world space.
	 * @return the bounding volume for this tree node in world space.
	 */
/*
	public BoundingVolume getWorldBounds() {
		return worldBounds;
	}
*/
	/**
	 * creates the appropriate bounding volume based on the type set
	 * during construction.
	 */
	private void createBounds() {
		switch(type) {
		case AABB_TREE:
			bounds = new BoundingBox();
//			worldBounds = new BoundingBox();
			break;
/* TODO: implement
		case OBB_TREE:
			bounds = new OrientedBoundingBox();
			worldBounds = new OrientedBoundingBox();
			break;
  */
		case SPHERE_TREE:
			bounds = new BoundingSphere();
//			worldBounds = new BoundingSphere();
			break;
		default:
			break;
		}
	}
	
	/**
	 * sortTris attempts to optimize the ordering of the subsection of the array
	 * of triangles this node is responsible for. The sorting is based on the
	 * most efficient method along an axis. Using the TreeComparator and quick
	 * sort, the subsection of the array is sorted.
	 */
	public void sortTris() {
                Context tmp=LocalContext.getContext();
                TreeComparator comparator = tmp.comparator;
                
		switch (type) {
		case AABB_TREE:
			//determine the longest length of the box, this axis will be best
			//for sorting.
			if (((BoundingBox) bounds).xExtent > ((BoundingBox) bounds).yExtent) {
				if (((BoundingBox) bounds).xExtent > ((BoundingBox) bounds).zExtent) {
					comparator.setAxis(TreeComparator.X_AXIS);
				} else {
					comparator.setAxis(TreeComparator.Z_AXIS);
				}
			} else {
				if (((BoundingBox) bounds).yExtent > ((BoundingBox) bounds).zExtent) {
					comparator.setAxis(TreeComparator.Y_AXIS);
				} else {
					comparator.setAxis(TreeComparator.Z_AXIS);
				}
			}
			break;
/* TODO: impelement
		case OBB_TREE:
			//determine the longest length of the box, this axis will be best
			//for sorting.
			if (((OrientedBoundingBox) bounds).extent.x > ((OrientedBoundingBox) bounds).extent.y) {
				if (((OrientedBoundingBox) bounds).extent.x > ((OrientedBoundingBox) bounds).extent.z) {
					comparator.setAxis(TreeComparator.X_AXIS);
				} else {
					comparator.setAxis(TreeComparator.Z_AXIS);
				}
			} else {
				if (((OrientedBoundingBox) bounds).extent.y > ((OrientedBoundingBox) bounds).extent.z) {
					comparator.setAxis(TreeComparator.Y_AXIS);
				} else {
					comparator.setAxis(TreeComparator.Z_AXIS);
				}
			}
			break;
 */
		case SPHERE_TREE:
			//sort any axis, X is fine.
			comparator.setAxis(TreeComparator.X_AXIS);
			break;
		default:
			break;
		}

		comparator.setCenter(bounds.center);
		comparator.setBatch(batch);
		SortUtil.qsort(triIndex, start, end - 1, comparator);
	}
}
