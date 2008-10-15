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

package com.vlengine.scene;

import com.vlengine.app.frame.Frame;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.math.Transform;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.util.FastList;

/**
 *
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class LodMesh extends Spatial {
       
    // the list of all batchlists, for every lod
    // lodList.get(0) is the default LOD, the most detailed one
    protected FastList<FastList<TriBatch>> lodList;
    
    // the local bound calculated from batches
    protected BoundingVolume localBound;
    
    // the transforms for child batches
    // for each of the frames
    protected Transform[] childTransforms;
    
    // the list of passes this node removed from active passes
    protected FastList<RenderPass> nopasses = new FastList<RenderPass>();
    
    
    public LodMesh() {
        super();
        setupBatchList();
    }

    /**
     * Constructor instantiates a new <code>Geometry</code> object. This is
     * the default object which has an empty vertex array. All other data is
     * null.
     * 
     * @param name
     *            the name of the scene element. This is required for
     *            identification and comparision purposes.
     */
    public LodMesh(String name) {
        super(name);
        setupBatchList();
    }

    protected void setupBatchList() {
        lodList = new FastList<FastList<TriBatch>>(1);
        childTransforms = new Transform[Frame.MAX_FRAMES];
        for( int i=0, mx=childTransforms.length; i<mx; i++ )
            childTransforms[i] = new Transform();
    }
    
    /**
     * adds a batch to the batch list of the geometry.
     * 
     * @param batch
     *            the batch to add.
     */
    public void addBatch(int lod, TriBatch batch) {
        // set the parent of the batch to be us
        batch.setParent(this);
        // lock the transforms of the batch to us
        batch.setWorldTransform(childTransforms);
        
        // get the batch list for the selected lod
        FastList<TriBatch> batchList = lodList.size() > lod ? lodList.get(lod) : null;
        if( batchList == null ) {
            // create the batchlist for the lod if it does not exists
            batchList = new FastList<TriBatch>();
            lodList.ensureCapacity(lod + 1);
            lodList.set(lod, batchList);
        }
        // add the batch to the list
        batchList.add(batch);
    }

    /**
     * Retrieves the batch at the supplied index. If the index is invalid, this
     * could throw an exception.
     * 
     * @param index
     *            the index to retrieve the batch from.
     * @return the selected batch.
     */
    public TriBatch getBatch(int lod, int index) {
        return lodList.get(lod).get(index);
    }

    /**
     * returns the number of batches contained in this geometry.
     * 
     * @return the number of batches in this geometry.
     */
    public int getBatchCount( int lod ) {
        return lodList.get(lod).size();
    }

    public int getLodCount() {
        return lodList.size();
    }
    
    /**
     * returns the number of vertices contained in this geometry. This is a
     * summation of the vertex count for each enabled batch that is contained in
     * this geometry.
     */
    public int getVertexCount( int lod ) {
        int count = 0;

        for (int i = 0; i < getBatchCount(lod); i++) {
            TriBatch gb = getBatch(lod, i); 
            count += gb.getModel().getNumVertex();
        }
        return count;
    }

    @Override
    public void updateGeometricState() {
        if (!isLockedTransforms()) {
            updateWorldScale();
            updateWorldRotation();
            updateWorldTranslation();
        }
        if ((lockedMode & Spatial.LOCKED_BOUNDS) == 0) {
            // transform the local bound, dont calculate it from batches
            if( localBound!= null ) {
                worldBound = localBound.clone(worldBound).transform(
                                            getWorldRotation(),
                                            getWorldTranslation(),
                                            getWorldScale(), worldBound);
            } else {
                // process full bound
                updateWorldBound();
            }
        }
    }

    /**
     * <code>updateWorldBound</code> updates the bounding volume that contains
     * this geometry. The location of the geometry is based on the location of
     * all this node's parents.
     * 
     * @see com.jme.scene.Spatial#updateWorldBound()
     */
    @Override
    public void updateWorldBound() {
        if ((lockedMode & Spatial.LOCKED_BOUNDS) != 0 && !changed) return;

        //if( localBound == null ) {
            // calculate the local bound
            
            // bound is always calculated the most detailed batch-set
            FastList<TriBatch> batchList = lodList.get(0);
            boolean foundFirstBound = false;
            for (int i = 0, cSize = batchList.size(); i < cSize; i++) {
                TriBatch child =  batchList.get(i);
                if (child != null) {
                    if (foundFirstBound) {
                        // merge current local bound with child local bound
                        localBound.mergeLocal(child.getLocalBound());
                    } else {
                        // set local bound to first non-null child local bound
                        if (child.getLocalBound() != null) {
                            localBound = child.getLocalBound()
                                    .clone(localBound);
                            foundFirstBound = true;
                        }
                    }
                }
            }
        //}
        // transform the local bound
        if( localBound!= null )
            worldBound = localBound.clone(worldBound).transform(
                                        getWorldRotation(),
                                        getWorldTranslation(),
                                        getWorldScale(), worldBound);
    }
    
    @Override
    public void updateWorldVectors(UpdateContext ctx) {
        super.updateWorldVectors(ctx);
        
        // copy transforms to those used in child Batches
        Transform t = childTransforms[ctx.frameId];
        t.getRotation().set(worldRotation);
        t.getScale().set(worldScale);
        t.getTranslation().set(worldTranslation);
    }

    @Override
    public boolean queue(CullContext ctx) {
        if(lodList == null) {
            return false;
        }
        // check the cull mode
        long cm = getRenderQueueMode();
        if ( cm != 0 ) {
            // prepare the list of passes, that dont need to be processed on children
            nopasses.clear();
            boolean found = false;
            boolean removed = false;
            // check for every our pass
            for(int i = 0; i < ctx.getPassQuantity(); i++) {
                RenderPass p = ctx.getPass(i);
                if(isUsePass(ctx, p)) {
                    found = true;
                } else {
                    // the mesh did not pass for this one, remove from passes
                    ctx.removePass(p);
                    nopasses.add(p);
                    removed = true;
                    i--;
                }
            }
            if( found ) {
                // determine the LOD level to use
                int lodindex = lodList.size() > 1 ? this.getLodIndex(ctx.getFrame().getCamera()) : 0;
                FastList<TriBatch> children = lodList.get(lodindex);
                TriBatch child;
                // save the camera plane state
                int state = ctx.getCullCamera().getPlaneState();
                for (int i = 0, cSize = children.size(); i < cSize; i++) {
                    child =  children.get(i);
                    if (child != null) {
                        if( child.docull(ctx) )
                            child.queue(ctx);
                        // restore the camera plane state
                        ctx.getCullCamera().setPlaneState(state);
                    }
                }
            }
            // after culling the children, restore previous passes
            if( removed ) {
                ctx.addPass(nopasses);
                nopasses.clear();
            }
            return found;
        }
        return false;
    }
    
    public int getLodIndex( ViewCamera cam ) {
        // get camera range (far - near)
        float range = cam.getFrustumFar() - cam.getFrustumNear();
        Vector3f cmp = cam.getLocation();
        float dst = cmp.distance(this.getWorldTranslation());
        return (int) Math.min((dst/range)*lodList.size(), lodList.size()-1);
    }
    
    @Override
    public void updateCounts(boolean initiator) {
        if(initiator && parent!=null)
            parent.updateCounts(true);
        
        // calcualte depth now, so children get proper value
        if(parent!=null)
            depth = parent.depth + 1;
        else
            depth = 0;
        
        // we are 1 element
        maxelements = 1;
        
        // clear the queue mode
        renderQueueMode = RenderQueue.QueueFilter.None.value;

        // find the lod with max elements
        int mxel = 0;
        // the max elements if our biggest lod
        for( int i = 0, mxi = lodList.size(); i < mxi; i++) {
            // reset maxelements 
            maxelements = 0;
            FastList<TriBatch> tl = lodList.get(i);
            for( int j = 0, mxj = tl.size(); j < mxj; j++ ) {
                TriBatch tb = tl.get(j);
                if( tb != null ) {
                    tb.updateCounts(false);
                }
            }
            // store it if its more
            if( mxel < maxelements )
                mxel = maxelements;
        }
        // write maxelements as the biggest list
        maxelements = mxel;

        // pass our values up
        if( parent != null) {
            // compile elements into parent
            parent.maxelements += maxelements;
            // compile renderQueueMode into parent
            parent.renderQueueMode |= renderQueueMode;
        }
    }
}
