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

import com.vlengine.intersection.CollisionData;
import com.vlengine.intersection.CollisionResults;
import com.vlengine.intersection.PickData;
import com.vlengine.intersection.PickResults;
import com.vlengine.math.Ray;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Geometry;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Node;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;

/**
 *
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class CollisionTreeManager {
    /**
	 * defines the default maximum number of triangles in a tree leaf.
	 */
	public static final int DEFAULT_MAX_TRIS_PER_LEAF = 16;

	//the singleton instance of the manager
	private static CollisionTreeManager instance;
        
        private boolean doSort=true;

	private int treeType = CollisionTree.AABB_TREE;
	
	private int maxTrisPerLeaf = DEFAULT_MAX_TRIS_PER_LEAF;
	
        public static CollisionTreeManager getInstance() {
		if (instance == null) {
			instance = new CollisionTreeManager();
		}
		return instance;
	}
        
	/**
	 * getCollisionTree obtains a collision tree that is assigned to a supplied
	 * TriangleBatch. The cache is checked for a pre-existing tree, if none is
	 * available and generateTrees is true, a new tree is created and returned.
	 *  
	 * @param batch the batch to use as the key for the tree to obtain.
	 * @return the tree assocated with a triangle batch
	 */
	public CollisionTree createCollisionTree(Geometry batch) {
            if (batch == null) {
			return null;
		}
            CollisionTree tree=batch.getCollisionTree();
            if(tree == null) {
                tree = new CollisionTree(treeType);
                tree.construct(batch, doSort);
                batch.setCollisionTree(tree);
            }
            return tree;
	}
        
	/**
	 * returns true if the manager is set to sort new generated trees. False
	 * otherwise.
	 * @return true to sort tree, false otherwise.
	 */
	public boolean isDoSort() {
		return doSort;
	}

	/**
	 * set if this manager should have newly generated trees sort triangles.
	 * @param doSort true to sort trees, false otherwise.
	 */
	public void setDoSort(boolean doSort) {
		this.doSort = doSort;
	}
        
	/**
	 * returns the type of collision trees this manager will create: AABB_TREE,
	 * OBB_TREE or SPHERE_TREE.
	 * @return the type of tree the manager will create.
	 */
	public int getTreeType() {
		return treeType;
	}

	/**
	 * set the type of collision tree this manager will create: AABB_TREE, 
	 * OBB_TREE or SPHERE_TREE.
	 * @param treeType the type of tree to create.
	 */
	public void setTreeType(int treeType) {
		this.treeType = treeType;
	}

	/**
	 * returns the maximum number of triangles a leaf of the collision tree
	 * will contain.
	 * @return the maximum number of triangles a leaf will contain.
	 */
	public int getMaxTrisPerLeaf() {
		return maxTrisPerLeaf;
	}

	/**
	 * set the maximum number of triangles a leaf of the collision tree will
	 * contain.
	 * @param maxTrisPerLeaf the maximum number of triangles a leaf will contain.
	 */
	public void setMaxTrisPerLeaf(int maxTrisPerLeaf) {
		this.maxTrisPerLeaf = maxTrisPerLeaf;
	}
        
        protected void findTriangleCollision(Spatial firstMesh, Spatial secondMesh, BaseGeometry batch1, BaseGeometry batch2,
            IntList thisIndex, IntList otherIndex, boolean checktriangles) {
            
            if( batch1 == null || batch2 == null || !batch1.isCollidable() || !batch2.isCollidable() )
                return;
            
            
            CollisionTree thisCT = batch1.getCollisionTree();
            CollisionTree checkCT = batch2.getCollisionTree();
            
            if(thisCT == null || checkCT == null ) {
                return;
            }
            // clear temp object stack
            Context tmp = LocalContext.getContext();
            tmp.ctbstack.clear();
            
            if(checktriangles) {
                thisCT.intersect(checkCT, thisIndex, otherIndex, firstMesh, secondMesh, null);
            } else {
                thisCT.triInsideBound(checkCT, thisIndex, otherIndex, firstMesh, secondMesh, null);
            }
        }    
        
        protected void findTriangleCollision(LodMesh firstMesh, LodMesh secondMesh, int firstBatch, int secondBatch,
            IntList thisIndex, IntList otherIndex, boolean checkTriangle) {
            BaseGeometry batch1 = firstMesh.getBatch(0, firstBatch).getModel();
            BaseGeometry batch2 = secondMesh.getBatch(0, secondBatch).getModel();
            
            findTriangleCollision(firstMesh, secondMesh, batch1, batch2, thisIndex, otherIndex, checkTriangle);
        }
        
        protected boolean findVolumeCollision(Spatial firstMesh, Spatial secondMesh, BaseGeometry batch1, BaseGeometry batch2,
            FastList<Vector3f> colPoints) {
            
            if( batch1 == null || batch2 == null || !batch1.isCollidable() || !batch2.isCollidable() )
                return false;
            
            
            CollisionVolume thisCT = batch1.getCollisionVolume();
            CollisionVolume checkCT = batch2.getCollisionVolume();
            
            if(thisCT == null || checkCT == null ) {
                return false;
            }
            
            return thisCT.getCollisions(firstMesh, secondMesh, checkCT, colPoints);
        }
        
        protected void findVolumeCollision(Spatial s, Spatial t, CollisionResults res) {

            CollisionVolume thisCT = s.getCollisionVolume();
            CollisionVolume checkCT = t.getCollisionVolume();
            
            if(thisCT == null || checkCT == null ) {
                return;
            }

            FastList<Vector3f> colPoints = null;
            if(res.isVolumeGetCollisionPoints()) {
                colPoints = new FastList<Vector3f>();
            }
                    
            if(thisCT.getCollisions(s, t, checkCT, colPoints)) {
                CollisionData data = new CollisionData(s, t, -1, -1, colPoints);
                res.addCollision(data);
            }
        }
                
        protected void findVolumeCollision(LodMesh s, LodMesh t, CollisionResults res) {
            //find the triangle that is being hit.
            //add this node and the triangle to the CollisionResults list.
            TriBatch a,b;
            // checking is done with the most detailed batches
            for (int x = 0, mx=s.getBatchCount(0); x < mx; x++) {
                a = s.getBatch(0, x);
                if (a == null ) continue;
                for (int y = 0, my=t.getBatchCount(0); y < my; y++) {
                    b = t.getBatch(0, y);
                    if (b == null ) continue;

                    FastList<Vector3f> colPoints = null;
                    if(res.isVolumeGetCollisionPoints()) {
                        colPoints = new FastList<Vector3f>();
                    }
                    
                    if(findVolumeCollision(s, t, a.getModel(), b.getModel(), colPoints)) {
                        CollisionData data = new CollisionData(s, t, x, y, colPoints);
                        res.addCollision(data);
                    }
                }
            }
        }
        
        protected void findVolumeCollision(Mesh s, LodMesh t, CollisionResults res) {
            //find the triangle that is being hit.
            //add this node and the triangle to the CollisionResults list.
            TriBatch a,b;
            // checking is done with the most detailed batches
            //for (int x = 0, mx=s.getBatchCount(0); x < mx; x++) {
                a = s.getBatch();
                if (a == null ) return;
                for (int y = 0, my=t.getBatchCount(0); y < my; y++) {
                    b = t.getBatch(0, y);
                    if (b == null ) continue;

                    FastList<Vector3f> colPoints = null;
                    if(res.isVolumeGetCollisionPoints()) {
                        colPoints = new FastList<Vector3f>();
                    }
                    
                    if(findVolumeCollision(s, t, a.getModel(), b.getModel(), colPoints)) {
                        CollisionData data = new CollisionData(s, t, -1, y, colPoints);
                        res.addCollision(data);
                    }
                }
            //}
        }
        
        protected void findVolumeCollision(LodMesh s, Mesh t, CollisionResults res) {
            //find the triangle that is being hit.
            //add this node and the triangle to the CollisionResults list.
            TriBatch a,b;
            b = t.getBatch();
            if (b == null ) return;

            // checking is done with the most detailed batches
            for (int x = 0, mx=s.getBatchCount(0); x < mx; x++) {
                a = s.getBatch(0, x);
                if (a == null ) continue;
                //for (int y = 0, my=t.getBatchCount(0); y < my; y++) {
                FastList<Vector3f> colPoints = null;
                if(res.isVolumeGetCollisionPoints()) {
                    colPoints = new FastList<Vector3f>();
                }

                if(findVolumeCollision(s, t, a.getModel(), b.getModel(), colPoints)) {
                    CollisionData data = new CollisionData(s, t, x, -1, colPoints);
                    res.addCollision(data);
                }
                //}
            }
        }
        
        protected void findVolumeCollision(Mesh s, Mesh t, CollisionResults res) {
            //find the triangle that is being hit.
            //add this node and the triangle to the CollisionResults list.
            TriBatch a,b;
            // checking is done with the most detailed batches
            //for (int x = 0, mx=s.getBatchCount(0); x < mx; x++) {
            a = s.getBatch();
            if (a == null ) return;
            //for (int y = 0, my=t.getBatchCount(0); y < my; y++) {
            b = t.getBatch();
            if (b == null ) return;

            FastList<Vector3f> colPoints = null;
            if(res.isVolumeGetCollisionPoints()) {
                colPoints = new FastList<Vector3f>();
            }

            if(findVolumeCollision(s, t, a.getModel(), b.getModel(), colPoints)) {
                CollisionData data = new CollisionData(s, t, -1, -1, colPoints);
                res.addCollision(data);
            }
                //}
            //}
        }
        
        protected void findTriangleCollision(LodMesh s, LodMesh t, CollisionResults res) {
            //find the triangle that is being hit.
            //add this node and the triangle to the CollisionResults list.
            TriBatch a,b;
            // checking is done with the most detailed batches
            for (int x = 0, mx=s.getBatchCount(0); x < mx; x++) {
                a = s.getBatch(0, x);
                if (a == null ) continue;
                for (int y = 0, my=t.getBatchCount(0); y < my; y++) {
                    b = t.getBatch(0, y);
                    
                    // TODO: allocate from stack
                    IntList aList = new IntList();
                    IntList bList = new IntList();
                    if (b == null ) continue;
                    findTriangleCollision(s, t, a.getModel(), b.getModel(), aList, bList, res.isCheckTriangles());
                    // TODO: allocate from loacal stack
                    if(aList.size() >0 || bList.size()>0) {
                        CollisionData data = new CollisionData(s, t, x, y, aList, bList);
                        res.addCollision(data);
                    }
                }
            }
        }
        
        protected void findTriangleCollision(LodMesh s, Mesh t, CollisionResults res) {
            //find the triangle that is being hit.
            //add this node and the triangle to the CollisionResults list.
            TriBatch a,b;
            // checking is done with the most detailed batches
            for (int x = 0, mx=s.getBatchCount(0); x < mx; x++) {
                a = s.getBatch(0, x);
                if (a == null ) continue;
                b = t.getBatch();
                if (b == null ) continue;
                // TODO: allocate from stack
                IntList aList = new IntList();
                IntList bList = new IntList();
                
                findTriangleCollision(s, t, a.getModel(), b.getModel(), aList, bList, res.isCheckTriangles());
                // TODO: allocate from loacal stack
                if(aList.size() >0 || bList.size()>0) {
                    CollisionData data = new CollisionData(s, t, x, -1, aList, bList);
                    res.addCollision(data);
                }
            }
        }
        
        protected void findTriangleCollision(Mesh s, LodMesh t, CollisionResults res) {
            //find the triangle that is being hit.
            //add this node and the triangle to the CollisionResults list.
            TriBatch a,b;
            // checking is done with the most detailed batches
            
            a = s.getBatch();
            if (a == null ) return;
            for (int y = 0, my=t.getBatchCount(0); y < my; y++) {
                b = t.getBatch(0, y);

                // TODO: allocate from stack
                IntList aList = new IntList();
                IntList bList = new IntList();
                if (b == null ) continue;
                findTriangleCollision(s, t, a.getModel(), b.getModel(), aList, bList, res.isCheckTriangles());
                // TODO: allocate from loacal stack
                if(aList.size() >0 || bList.size()>0) {
                    CollisionData data = new CollisionData(s, t, -1, y, aList, bList);
                    res.addCollision(data);
                }
            }
        }
        
        protected void findTriangleCollision(Mesh s, Mesh t, CollisionResults res) {
            //find the triangle that is being hit.
            //add this node and the triangle to the CollisionResults list.
            TriBatch a,b;
            // checking is done with the most detailed batches
            
            a = s.getBatch();
            if (a == null ) return;
            b = t.getBatch();
            if (b == null ) return;
            
            // TODO: allocate from stack
            IntList aList = new IntList();
            IntList bList = new IntList();
            
            findTriangleCollision(s, t, a.getModel(), b.getModel(), aList, bList, res.isCheckTriangles());
            // TODO: allocate from loacal stack
            if(aList.size() >0 || bList.size()>0) {
                CollisionData data = new CollisionData(s, t, -1, -1, aList, bList);
                res.addCollision(data);
            }
        }
        
        public void findCollisions(Spatial spat, Spatial scene, CollisionResults results) {
            // clear temp object stack
            Context tmp = LocalContext.getContext();
            tmp.ctbstack.clear();
            findBoundCollisions(spat, scene, results);
        }
        
        protected void findBoundCollisions(Spatial spat, Spatial scene, CollisionResults results) {
            if (spat == scene || !spat.isCollidable() || !scene.isCollidable()) {
                return;
            }

            if (spat.getWorldBound().intersects(scene.getWorldBound())) {
                // if spat is Node, process all its children
                if(spat instanceof Node && (spat.getCollisionVolume() == null || !results.isCheckCollisionVolume() )) {
                    Node sparent = (Node) spat;
                    for (int i = 0; i < sparent.getQuantity(); i++) {
                        findBoundCollisions(sparent.getChild(i), scene, results);
                    }
                } else {
                    if( results.isCheckCollisionVolume() && scene.getCollisionVolume() != null  
                        && spat.getCollisionVolume() != null ) {
                        findVolumeCollision(spat, scene, results);
                    } else if ( scene instanceof Node ) {
                        Node sparent = (Node) scene;
                        for (int i = 0; i < sparent.getQuantity(); i++) {
                            findBoundCollisions(spat, sparent.getChild(i), results);
                        }
                    } /*else if(results.isCheckCollisionTree()) {
                        if(spat instanceof LodMesh) {
                            if(scene instanceof LodMesh ) {
                                findTriangleCollision((LodMesh)spat, (LodMesh)scene, results);
                            } else if( scene instanceof Mesh ) {
                                findTriangleCollision((LodMesh)spat, (Mesh)scene, results);
                            }
                        } else if(spat instanceof Mesh) {
                            if(scene instanceof LodMesh ) {
                                findTriangleCollision((Mesh)spat, (LodMesh)scene, results);
                            } else if( scene instanceof Mesh ) {
                                findTriangleCollision((Mesh)spat, (Mesh)scene, results);
                            }
                        }
                        // TODO: other types of meshes
                    } else if(results.isCheckCollisionVolume()) {
                        if(spat instanceof LodMesh) {
                            if(scene instanceof LodMesh ) {
                                findVolumeCollision((LodMesh)spat, (LodMesh)scene, results);
                            } else if( scene instanceof Mesh ) {
                                findVolumeCollision((LodMesh)spat, (Mesh)scene, results);
                            }
                        } else if(spat instanceof Mesh) {
                            if(scene instanceof LodMesh ) {
                                findVolumeCollision((Mesh)spat, (LodMesh)scene, results);
                            } else if( scene instanceof Mesh ) {
                                findVolumeCollision((Mesh)spat, (Mesh)scene, results);
                            }
                        }
                    }*/
                    else {
                        // create data
                        // TODO: allocate from local stack
                        CollisionData data = new CollisionData(spat, scene);
                        results.addCollision(data);
                    }
                }
            }
        }
        
        protected void findBoundPick(LodMesh spat, PickResults results) {
            if (spat.getWorldBound() != null && spat.isCollidable()) {
                if (spat.getWorldBound().intersects(results.getRay())) {
                    int frameid = getFrameId();
                    // further checking needed.
                    // use the most detailed batch for picking
                    for (int i = 0, mi=spat.getBatchCount(0); i < mi; i++) {
                        TriBatch gb = spat.getBatch(0, i);
                        // this is needed so that not only viewed batches can be picked
                        gb.updateWorldBound(frameid);
                        BoundingVolume bv = gb.getWorldBound(frameid);
                        BaseGeometry bg = gb.getModel();
                        if ( bv == null || !bg.isCollidable()) {
                            return;
                        }
                        if ( bv.intersects(results.getRay()) ) {
                            // find the triangle that is being hit.
                            // add this node and the triangle to the PickResults list.
                            if(results.isCheckTriangles()) {
                                IntList aList = new IntList();
                                findTrianglePick(spat, bg, results.getRay(), aList);
                                if(aList.size()>0) {
                                    PickData data = new PickData(spat, i, aList);
                                    results.addPick(data);
                                }
                            } else {
                                PickData data = new PickData(spat, i);
                                results.addPick(data);
                            }
                        }
                    }
                }
            }
        }
        
        protected void findBoundPick(Mesh spat, PickResults results) {
            if (spat.getWorldBound() != null && spat.isCollidable()) {
                if (spat.getWorldBound().intersects(results.getRay())) {
                    int frameid = getFrameId();
                    // further checking needed.
                    // use the most detailed batch for picking
                    TriBatch gb = spat.getBatch();
                    // this is needed so that not only viewed batches can be picked
                    gb.updateWorldBound(frameid);
                    BoundingVolume bv = gb.getWorldBound(frameid);
                    BaseGeometry bg = gb.getModel();
                    if ( bv == null || !bg.isCollidable()) {
                        return;
                    }
                    if ( bv.intersects(results.getRay()) ) {
                        // find the triangle that is being hit.
                        // add this node and the triangle to the PickResults list.
                        if(results.isCheckTriangles()) {
                            IntList aList = new IntList();
                            findTrianglePick(spat, bg, results.getRay(), aList);
                            if(aList.size()>0) {
                                PickData data = new PickData(spat, -1, aList);
                                results.addPick(data);
                            }
                        } else {
                            PickData data = new PickData(spat);
                            results.addPick(data);
                        }
                    }
                }
            }
        }
        
        public void findPick(Spatial spat, PickResults results) {
            Context tmp = LocalContext.getContext();
            tmp.ctbstack.clear();
            findBoundPick(spat, results);
        }
        
        protected void findBoundPick(Spatial spat, PickResults results) {
            if(spat == null) {
                return;
            }
            
            if (spat.getWorldBound() != null && spat.isCollidable() 
                    && spat.getWorldBound().intersects(results.getRay())
                    ) {
                if(results.isCheckVolume() && spat.getCollisionVolume()!=null) {
                    // we got a spat with collision volume, check it
                    spat.getCollisionVolume().getPick(spat, results);
                } else if(spat instanceof Node) {
                    Node sparent = (Node) spat;
                    for (int i = 0; i < sparent.getQuantity(); i++) {
                        findBoundPick(sparent.getChild(i), results);
                    }
                } /*else {
                    if(results.isCheckTriangles()) {
                        if(spat instanceof LodMesh) {
                            findBoundPick((LodMesh)spat, results);
                        } else if(spat instanceof Mesh) {
                            findBoundPick((Mesh)spat, results);
                        }
                        // TODO: other types of meshes
                    } else if(!results.isCheckVolume()) {
                        // create data
                        // TODO: allocate from local stack
                        PickData data = new PickData(spat);
                        results.addPick(data);
                    }
                }
                   */
            }
        }
        
        protected int getFrameId() {
            Context ctx = LocalContext.getContext();
            return ctx.scene.getFrameId();
        }
        
        /**
         * <code>findTrianglePick</code> determines the triangles of this trimesh
         * that are being touched by the ray. The indices of the triangles are
         * stored in the provided ArrayList.
         * 
         * @param toTest
         *            the ray to test. The direction of the ray must be normalized (length 1). 
         * @param results
         *            the indices to the triangles.
         */
        protected void findTrianglePick(Spatial parent, BaseGeometry target, Ray toTest, IntList results) {
            if (parent == null || target == null || !target.isCollidable()) {
                return;
            }

            CollisionTree ct = target.getCollisionTree();
            if(ct != null) {
                ct.intersect(toTest, results, parent, parent.getWorldBound());
            }
        }

}
