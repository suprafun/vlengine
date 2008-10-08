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

import com.vlengine.bounding.BoundingVolume;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.util.FastList;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class SetNode extends Node {

    /** This node's children. */
    protected FastList<Spatial> children=new FastList<Spatial>(4);

    //protected FastList<SceneEffect> effects;
    
    // the list of passes this node removed from active passes
    protected FastList<RenderPass> nopasses = new FastList<RenderPass>();

    public SetNode(String name ) {
        super(name);
    }
    
    /**
     * 
     * <code>getQuantity</code> returns the number of children this node
     * maintains.
     * 
     * @return the number of children this node maintains.
     */
    public int getQuantity() {
        if(children == null) {
            return 0;
        } 
        return children.size();
    }
    
    /**
     * 
     * <code>attachChild</code> attaches a child to this node. This node
     * becomes the child's parent. The current number of children maintained is
     * returned.
     * <br>
     * If the child already had a parent it is detached from that former parent.
     * 
     * @param child
     *            the child to attach to this node.
     */
    protected void attachChild(Spatial child, FastList children) {
        if (child != null) {
            if (child.getParent() != this) {
                if (child.getParent() != null) {
                    ((Node)child.getParent()).detachChild(child);
                }
                child.setParent(this);
                // put effects in the begginning of our children
                children.add(child);
                changed = true;
            }
        }
    }

    public void attachChild(Spatial child) {
        if(children == null) {
            children = new FastList<Spatial>(8);
        }
        attachChild(child, children);
        // TODO: compile child
        // add child count to parent
        // recalculate renderqueuemode
        
    }

    protected void detachChild(Spatial child, FastList<Spatial> children) {
        int index = children.indexOf(child);
        if (index != -1) {
            detachChildAt(index, children);
        }
    }

    /**
     * <code>detachChild</code> removes a given child from the node's list.
     * This child will no longe be maintained.
     * 
     * @param child
     *            the child to remove.
     */
    public void detachChild(Spatial child) {
        detachChild(child, children);
    }

    /**
     * 
     * <code>detachChildAt</code> removes a child at a given index. That child
     * is returned for saving purposes.
     * 
     * @param index
     *            the index of the child to be removed.
     */
    protected void detachChildAt(int index, FastList<Spatial> children) {
        children.get(index).setParent(null);
        children.remove(index);
        changed = true;
    }

    /**
     * 
     * <code>detachAllChildren</code> removes all children attached to this
     * node.
     */
    public void detachAllChildren() {
        children.clear();
        //TODO: remove child count from parent
        changed = true;
    }

    /**
     * 
     * <code>getChild</code> returns a child at a given index.
     * 
     * @param i
     *            the index to retrieve the child from.
     * @return the child at a specified index.
     */

    public Spatial getChild(int i) {
        return children.get(i);
    }

    protected Spatial getChild(String name, FastList<Spatial> children) {
        for (int x = 0, cSize = children.size(); x < cSize; x++) {
            Spatial child = children.get(x);
            if (name.equals(child.getName())) {
                return child;
            } else if(child instanceof Node) {
                Spatial out = ((Node)child).getChild(name);
                if(out != null) {
                    return out;
                }
            }
        }
        return null;
    }

    /**
     * 
     * <code>getChild</code> returns the first child found with exactly the
     * given name (case sensitive.)
     * 
     * @param name
     *            the name of the child to retrieve.
     * @return the child if found, or null.
     */
    public Spatial getChild(String name) {
        return getChild(name, children);
    }
    
    /**
     * determines if the provide Spatial is contained in the children list of
     * this node.
     * 
     * @param spat
     *            the spatial object to check.
     * @return true if the object is contained, false otherwise.
     */
    public boolean hasChild(Spatial spat) {
        if(children == null) {
            return false;
        }
        if (children.contains(spat))
            return true;

        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child instanceof Node && ((Node) child).hasChild(spat))
                return true;
        }
        return false;
    }

    @Override
    public void updateGeometricState() {
        // update world transforms
        if (!isLockedTransforms() || changed) {
            updateWorldScale();
            updateWorldRotation();
            updateWorldTranslation();
        }
        // update children
        Spatial child;
        for (int i = 0, n = children.size(); i < n; i++) {
            try {
                child = children.get(i);
            } catch (IndexOutOfBoundsException e) {
                // a child was removed in updateGeometricState (note: this may
                // skip one child)
                break;
            }
            if (child != null) {
                child.updateGeometricState();
            }
        }
        // update world bound
        updateWorldBound();
    }

    /**
     * <code>updateWorldData</code> updates all the children maintained by
     * this node.
     * 
     * @param time
     *            the frame time.
     */
    @Override
    public void updateWorldData(UpdateContext ctx) {
        super.updateWorldData(ctx);
        /*
        // update effects
        if(effects!=null) {
            SceneEffect ef;
            for (int i = 0, n = effects.size(); i < n; i++) {
                try {
                    ef = effects.get(i);
                } catch (IndexOutOfBoundsException e) {
                    // a child was removed in updateGeometricState (note: this may
                    // skip one child)
                    break;
                }
                if (ef != null) {
                    ef.updateGeometricState(ctx, false);
                }
            }
        }
         */
        // update children
        Spatial child;
        for (int i = 0, n = children.size(); i < n; i++) {
            try {
                child = children.get(i);
            } catch (IndexOutOfBoundsException e) {
                // a child was removed in updateGeometricState (note: this may
                // skip one child)
                break;
            }
            if (child != null) {
                child.updateGeometricState(ctx, false);
            }
        }
    }

    // inheritted docs
    @Override
    public void lockBounds() {
        super.lockBounds();
        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                child.lockBounds();
            }
        }
    }

    //  inheritted docs
    @Override
    public void lockShadows() {
        super.lockShadows();
        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                child.lockShadows();
            }
        }
    }
    
    //  inheritted docs
    @Override
    public void lockTransforms() {
        super.lockTransforms();
        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                child.lockTransforms();
            }
        }
    }

    //  inheritted docs
    @Override
    public void lockMeshes() {
        super.lockMeshes();
        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                child.lockMeshes();
            }
        }
    }

    //  inheritted docs
    @Override
    public void unlockBounds() {
        super.unlockBounds();
        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                child.unlockBounds();
            }
        }
    }
    
    //  inheritted docs
    @Override
    public void unlockShadows() {
        super.unlockShadows();
        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                child.unlockShadows();
            }
        }
    }
    
    //  inheritted docs
    @Override
    public void unlockTransforms() {
        super.unlockTransforms();
        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                child.unlockTransforms();
            }
        }
    }

    //  inheritted docs
    @Override
    public void unlockMeshes() {
        super.unlockMeshes();
        for (int i = 0, max = children.size(); i < max; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                child.unlockMeshes();
            }
        }
    }

    @Override
    public boolean queue(CullContext ctx) {
        if(children == null) {
            return false;
        }
        // check the cull mode
        //long cm = getRenderQueueMode();
        if ( renderQueueMode != RenderQueue.QueueFilter.None.value ) {
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
//                long pcm = p.getQueueFilter();
//                if( ( cm & pcm ) == 0 ) {
                    // the node did not pass for this one, remove from passes
                    ctx.removePass(p);
                    nopasses.add(p);
                    removed = true;
                    // we removed one element, so reset increment
                    i--;
                }
            }
            if( found ) {
                /*
                // apply the effects on this node
                if( effects!= null && effects.size() > 0 ) {
                    for (int i = 0, mx=effects.size() ; i < mx; i++) {
                        SceneEffect e = effects.get(i);
                        if(e.isEnabled())
                            e.doEffect(ctx);
                    }
                }
                 */

                // check and cull children
                Spatial child;
                int work = maxelements;
                // save the camera plane state
                int state = ctx.getCullCamera().getPlaneState();
                for (int i = 0, cSize = children.size(); i < cSize; i++) {
                    child =  children.get(i);
                    // TODO: if child is a node, process it in other free thread
                    if (child != null ) {
                        boolean check = false;
                        if( ctx.getFrame().getApp().isMultithreaded()) {
                            if( work > 16
                                    && child instanceof Node 
                                    && child.maxelements > work /4 
                                    && child.maxelements < work /2 
                                    ) {
                                // if the elements needs to be processed is large enugh
                                // and it is big chunk of all the work
                                // but it is not all the work
                                // this formula allows for creation of 2 to 4 culler threads

                                // then try to delegate the work to another thread
                                CullContext cctx = ctx.getFrame().cullNode(ctx, (Node)child, ctx.getViewCamera());
                                if( cctx != null ) {
                                    // start the child culler context
                                    cctx.start();
                                    check = true;
                                }
                            }
                        }
                        
                        // if the child is not a (big) node or no other thread is ready
                        // do the culling and queueing
                        if( !check && child.docull(ctx) )
                            child.queue(ctx);
                        
                        // restore the camera plane state
                        ctx.getCullCamera().setPlaneState(state);
                        // calculate elements still needs to be done
                        work -= child.maxelements;
                    }
                }
                
                /*
                 // undo the effects
                if( effects!= null && effects.size() > 0 ) {
                    for (int i = effects.size() - 1; i >= 0; i--) {
                        SceneEffect e = effects.get(i);
                        e.undoEffect(ctx);
                    }
                }
                 */
                
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

    /**
     * <code>updateWorldBound</code> merges the bounds of all the children
     * maintained by this node. This will allow for faster culling operations.
     * 
     * @see com.jme.scene.Spatial#updateWorldBound()
     */
    public void updateWorldBound() {
        if ((lockedMode & Spatial.LOCKED_BOUNDS ) != 0 && !changed) return;
        if (children == null) {
            return;
        }
        BoundingVolume worldBound = null;
        for (int i = 0, cSize = children.size(); i < cSize; i++) {
            Spatial child =  children.get(i);
            if (child != null) {
                if (worldBound != null) {
                    // merge current world bound with child world bound
                    worldBound.mergeLocal(child.getWorldBound());
                } else {
                    // set world bound to first non-null child world bound
                    if (child.getWorldBound() != null) {
                        worldBound = child.getWorldBound().clone(this.worldBound);
                    }
                }
            }
        }
        this.worldBound = worldBound;
    }

    /**
     * Returns all children to this node.
     *
     * @return an array containing all children to this node
     */
    public FastList<Spatial> getChildren() {
        return children;
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

        if(children!=null) {
            // compile the children
            for(int i = children.size() - 1; i >= 0; i--) {
                Spatial s = children.get(i);
                s.updateCounts(false);
            }
        }

        // pass our values up
        if( parent != null) {
            // compile elements into parent
            parent.maxelements += maxelements;
            // compile renderQueueMode into parent
            parent.renderQueueMode |= renderQueueMode;
        }
    }

    /*
    public void addEffect(SceneEffect child) {
        if (child != null) {
            if(effects == null) {
                effects = new FastList<SceneEffect>(8);
            }
            if (! effects.contains(child)) {
                // set that we are the new parent
                child.setParent(this);
                // put effects in the begginning of our children
                effects.add(child);
            }
        }
        changed = true;
    }
    
    public void removeEffect(SceneEffect child) {
        int index = effects.indexOf(child);
        if (index != -1) {
            effects.remove(index);
            child.setParent(null);
        }
        changed = true;
    }
    
    public FastList<SceneEffect> getEffects() {
        return effects;
    }
     */
}
