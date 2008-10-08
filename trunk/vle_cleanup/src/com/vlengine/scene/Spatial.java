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

import com.vlengine.bounding.CollisionVolume;
import com.vlengine.scene.control.Controller;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.BitSet;
import com.vlengine.util.FastList;
import java.util.EnumSet;

/**
 *
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public abstract class Spatial extends SceneElement {
    
    public static final int LOCKED_NONE = 0;
    public static final int LOCKED_BOUNDS = 1;
    public static final int LOCKED_MESH_DATA = 2;
    public static final int LOCKED_TRANSFORMS = 4;
    public static final int LOCKED_SHADOWS = 8;
    public static final int LOCKED_BRANCH = 16;
    // locked effects (no effects propagation)
    public static final int LOCKED_EFFECTS = 32;
    
    protected int lockedMode = LOCKED_NONE;
    
    protected boolean isCollidable = true;
    
    /** Spatial's rotation relative to its parent. */
    protected Quaternion localRotation;

    /** Spatial's world absolute rotation. */
    protected Quaternion worldRotation;

    /** Spatial's translation relative to its parent. */
    protected Vector3f localTranslation;

    /** Spatial's world absolute translation. */
    protected Vector3f worldTranslation;

    /** Spatial's scale relative to its parent. */
    protected Vector3f localScale;

    /** Spatial's world absolute scale. */
    protected Vector3f worldScale;

    /** ArrayList of controllers for this spatial. */
    protected FastList<Controller> controllers;
    
    protected String name;
    
    protected CollisionVolume collVolume;
    
    protected boolean changed = true;
    
    // managing attributes of the spatial
    public static enum Flag {
        // dinamic means it can be affected by physics, it moves
        Dynamic(0);
        
        public final int bit;
        Flag(int b) {
            bit = b;
        }
    }

    protected EnumSet<Flag> spatialFlags;
    
    public Spatial() {
        super();
        localRotation = new Quaternion();
        worldRotation = new Quaternion();
        localTranslation = new Vector3f();
        worldTranslation = new Vector3f();
        localScale = new Vector3f(1.0f, 1.0f, 1.0f);
        worldScale = new Vector3f(1.0f, 1.0f, 1.0f);
    }
    
    /**
     * Constructor instantiates a new <code>Spatial</code> object setting the
     * rotation, translation and scale value to defaults.
     *
     * @param name
     *            the name of the scene element. This is required for
     *            identification and comparision purposes.
     */
    public Spatial(String name) {
        this();
        this.name = name;
    }
    
    /**
     * Sets the name of this spatial.
     * 
     * @param name
     *            The spatial's new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this spatial.
     * 
     * @return This spatial's name.
     */
    public String getName() {
        return name;
    } 
    
    public void setFlag(Flag f, boolean state) {
        // no need to set anything
        if(spatialFlags==null && state == false) 
            return;
        if(spatialFlags==null)
            spatialFlags = EnumSet.noneOf(Flag.class);
        if(state) {
            spatialFlags.add(f);
        } else {
            spatialFlags.remove(f);
        }
    }

    public boolean getFlag(Flag f) {
        if(spatialFlags == null)
            return false;
        return spatialFlags.contains(f);
    }

    public void setIsCollidable(boolean isCollidable) {
        this.isCollidable = isCollidable;
    }
    
    public boolean isCollidable() {
        return this.isCollidable;
    }
    
    @Override
    protected void setParent(Spatial parent) {
        super.setParent(parent);
        changed = true;
    }
    
    public void updateGeometricState(UpdateContext ctx, boolean initiator) {
        if ((lockedMode & Spatial.LOCKED_BRANCH) != 0 && !changed) return;
        updateWorldData(ctx);
        if ((lockedMode & Spatial.LOCKED_BOUNDS) == 0 && !changed) {
            updateWorldBound();
            if (initiator) {
                propagateBoundToRoot();
            }
        }
        // update child counts
        updateCounts(initiator);
        changed = false;
    }

    public void updateGeometricState() {
        if (!isLockedTransforms() && !changed) {
            updateWorldScale();
            updateWorldRotation();
            updateWorldTranslation();
        }
        updateWorldBound();
    }

    /**
     * Calling this method tells the scenegraph that it is not necessary to
     * update bounds from this point in the scenegraph on down to the leaves.
     * This is useful for performance gains where you have scene items that do
     * not move (at all) or change shape and thus do not need constant
     * re-calculation of boundaries. When you call lock, the bounds are first
     * updated to ensure current bounds are accurate.
     * 
     * @see #unlockBounds()
     */
    public void lockBounds() {
        lockedMode |= LOCKED_BOUNDS;
        changed = true;
    }

    public boolean isLockedBounds() {
        return (!changed && (lockedMode & Spatial.LOCKED_BOUNDS) != 0);
    }
    
    /**
     * Calling this method tells the scenegraph that it is not necessary to
     * update Shadow volumes that may be associated with this SceneElement. This
     * is useful for skipping various checks for spatial transformation when
     * deciding whether or not to recalc a shadow volume for a SceneElement.
     * 
     * @see #unlockShadows()
     */
    public void lockShadows() {
        lockedMode |= LOCKED_SHADOWS;
        changed = true;
    }

    /**
     * Calling this method tells the scenegraph that it is not necessary to
     * traverse this SceneElement or any below it during the update phase. This
     * should be called *after* any other lock call to ensure they are able to
     * update any bounds or vectors they might need to update.
     * 
     * @see #unlockBranch()
     */
    public void lockBranch() {
        lockedMode |= LOCKED_BRANCH;
        changed = true;
    }

    /**
     * Flags this spatial and those below it in the scenegraph to not
     * recalculate world transforms such as translation, rotation and scale on
     * every update. This is useful for efficiency when you have scene items
     * that stay in one place all the time as it avoids needless recalculation
     * of transforms.
     * 
     * @see #unlockTransforms()
     */
    public void lockTransforms() {
        lockedMode |= LOCKED_TRANSFORMS;
        changed = true;
    }

    public boolean isLockedTransforms() {
        return ((lockedMode & Spatial.LOCKED_TRANSFORMS) != 0);
    }
    
    /**
     * Flags this spatial and those below it that any meshes in the specified
     * scenegraph location or lower will not have changes in vertex, texcoord,
     * normal or color data. 
     * 
     */
    public void lockMeshes() {
        lockedMode |= LOCKED_MESH_DATA;
        changed = true;
    }

    public void lockEffects() {
        lockedMode |= LOCKED_EFFECTS;
        changed = true;
    }
    
    /**
     * Convienence function for locking all aspects of a SceneElement. For
     * lockMeshes it calls:
     * <code>lockMeshes(DisplaySystem.getDisplaySystem().getRenderer());</code>
     * 
     * @see #lockBounds()
     * @see #lockTransforms()
     * @see #lockMeshes()
     * @see #lockShadows()
     */
    public void lock() {
        lockBounds();
        lockTransforms();
        lockMeshes();
        lockShadows();
    }
    
    public void unlockBounds() {
        lockedMode &= ~LOCKED_BOUNDS;
        changed = true;
    }

    /**
     * Flags this spatial and those below it to allow for shadow volume updates
     * (the default).
     * 
     * @see #lockShadows()
     */
    public void unlockShadows() {
        lockedMode &= ~LOCKED_SHADOWS;
        changed = true;
    }

    /**
     * Flags this SceneElement and any below it as being traversable during the
     * update phase.
     * 
     * @see #lockBranch()
     */
    public void unlockBranch() {
        lockedMode &= ~LOCKED_BRANCH;
        changed = true;
    }

    /**
     * Flags this spatial and those below it to allow for transform updating
     * (the default).
     * 
     * @see #lockTransforms()
     */
    public void unlockTransforms() {
        lockedMode &= ~LOCKED_TRANSFORMS;
        changed = true;
    }

    /**
     * Flags this spatial and those below it to allow for mesh updating (the
     * default). Generally this means that any display lists setup will be
     * erased and released. Calls unlockMeshes(Renderer) with the current
     * display system's renderer.
     * 
     * @see #unlockMeshes(Renderer)
     */
    public void unlockMeshes() {
        lockedMode &= ~LOCKED_MESH_DATA;
        changed = true;
    }

    /**
     * Convienence function for unlocking all aspects of a SceneElement. For
     * unlockMeshes it calls:
     * <code>unlockMeshes(DisplaySystem.getDisplaySystem().getRenderer());</code>
     * 
     * @see #unlockBounds()
     * @see #unlockTransforms()
     * @see #unlockMeshes()
     * @see #unlockShadows()
     * @see #unlockBranch()
     */
    public void unlock() {
        unlockBounds();
        unlockTransforms();
        unlockMeshes();
        unlockShadows();
        unlockBranch();
    }
    
    public void addController(Controller controller) {
        if (controllers == null) {
            controllers = new FastList<Controller>(1);
        }
        controllers.add(controller);
        changed = true;
    }
    
    public Controller getController(String name) {
        if (controllers == null) {
            return null;
        }
        for(int i=0, mx=controllers.size(); i<mx; i++) {
            Controller c = controllers.get(i);
            if( c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }
    
    public void removeController(Controller controller) {
        if (controllers == null) {
            return;
        }
        controllers.remove(controller);
        changed = true;
    }
    
    /**
     *
     * <code>getWorldRotation</code> retrieves the absolute rotation of the
     * Spatial.
     *
     * @return the Spatial's world rotation matrix.
     */
    public Quaternion getWorldRotation() {
        return worldRotation;
    }

    /**
     *
     * <code>getWorldTranslation</code> retrieves the absolute translation of
     * the spatial.
     *
     * @return the world's tranlsation vector.
     */
    public Vector3f getWorldTranslation() {
        return worldTranslation;
    }

    /**
     *
     * <code>getWorldScale</code> retrieves the absolute scale factor of the
     * spatial.
     *
     * @return the world's scale factor.
     */
    public Vector3f getWorldScale() {
        return worldScale;
    }

    /**
     * <code>rotateUpTo</code> is a util function that alters the
     * localrotation to point the Y axis in the direction given by newUp.
     *
     * @param newUp the up vector to use - assumed to be a unit vector.
     */
    public void rotateUpTo(Vector3f newUp) {
        Context tmp = LocalContext.getContext();
        
        //First figure out the current up vector.
        Vector3f upY = tmp.compVecA.set(Vector3f.UNIT_Y);
        localRotation.multLocal(upY);

        // get angle between vectors
        float angle = upY.angleBetween(newUp);

        //figure out rotation axis by taking cross product
        Vector3f rotAxis = upY.crossLocal(newUp).normalizeLocal();

        // Build a rotation quat and apply current local rotation.
        Quaternion q = tmp.compQuat;
        q.fromAngleNormalAxis(angle, rotAxis);
        q.mult(localRotation, localRotation);
        changed = true;
    }


    /**
     * <code>lookAt</code> is a convienence method for auto-setting the
     * local rotation based on a position and an up vector. It computes
     * the rotation to transform the z-axis to point onto 'position'
     * and the y-axis to 'up'. Unlike {@link Quaternion#lookAt} this method
     * takes a world position to look at not a relative direction.
     *
     * @param position
     *            where to look at in terms of world coordinates
     * @param upVector
     *            a vector indicating the (local) up direction.
     *            (typically {0, 1, 0} in jME.)
     */
    public void lookAt(Vector3f position, Vector3f upVector) {
        Context tmp = LocalContext.getContext();
        tmp.compVecA.set(position).subtractLocal(getWorldTranslation());
        getLocalRotation().lookAt( tmp.compVecA, upVector );
        changed = true;
    }

    /**
     *
     * <code>updateGeometricState</code> updates all the geometry information
     * for the node.
     *
     * @param time
     *            the frame time.
     * @param initiator
     *            true if this node started the update process.
     */

    public void updateWorldData(UpdateContext ctx) {
        // update spatial state via controllers
        if(controllers != null) {
            for (int i = 0, gSize = controllers.size(); i < gSize; i++) {
                Controller controller = controllers.get( i );
                if ( controller != null ) {
                    if (controller.isActive()) {
                            controller.update( ctx );
                    }
                }
            }
        }
        updateWorldVectors(ctx);
    }
    
    public void updateWorldVectors(UpdateContext ctx) {
        if (isLockedTransforms() && !changed) return;
        updateWorldScale();
        updateWorldRotation();
        updateWorldTranslation();
    }
    
    protected void updateWorldTranslation() {
        if (parent != null) {
            worldTranslation = parent.localToWorld( localTranslation, worldTranslation );
        } else {
            worldTranslation.set(localTranslation);
        }
    }
    
    protected void updateWorldRotation() {
        if (parent != null) {
            parent.getWorldRotation().mult(localRotation, worldRotation);
        } else {
            worldRotation.set(localRotation);
        }
    }
    
    protected void updateWorldScale() {
        if (parent != null) {
            worldScale.set(parent.getWorldScale()).multLocal(localScale);
        } else {
            worldScale.set(localScale);
        }
    }
    
    public Vector3f localToWorld( final Vector3f in, Vector3f store ) {
        if ( store == null ) store = new Vector3f();
        // multiply with scale first, then rotate, finally translate (cf. Eberly)
        return getWorldRotation().mult(store.set( in ).multLocal( getWorldScale() ),
                store ).addLocal( getWorldTranslation());
    }
    
    public Vector3f worldToLocal(final Vector3f in, final Vector3f store) {
        in.subtract(getWorldTranslation(), store).divideLocal(getWorldScale());
        Quaternion q = LocalContext.getContext().tSpatialq1;
        q.loadIdentity();
        q.set(getWorldRotation());
        q.inverseLocal().mult(store, store);
        return store;
    }
    
    public boolean removeFromParent() {
        if ( parent != null 
                && parent instanceof Node ) {
            ((Node)parent).detachChild(this);
            return true;
        }
        return false;
    }
    
    public Quaternion getLocalRotation() {
        return localRotation;
    }
    
    public Vector3f getLocalScale() {
        return localScale;
    }
    
    public Vector3f getLocalTranslation() {
        return localTranslation;
    }
    
    public void propagateBoundToRoot() {
        if (parent != null) {
            parent.updateWorldBound();
            parent.propagateBoundToRoot();
        }
    }
    
    public void setCollisionVolume(CollisionVolume cvol) {
        collVolume = cvol;
        changed = true;
    }
    
    public CollisionVolume getCollisionVolume() {
        return collVolume;
    }
}
