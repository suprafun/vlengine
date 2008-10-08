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

package com.vlengine.scene.batch;

import com.vlengine.app.frame.Frame;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.bounding.CollisionTree;
import com.vlengine.intersection.PickResults;
import com.vlengine.math.Ray;
import com.vlengine.model.BaseGeometry;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.CullContext;
import com.vlengine.scene.Renderable;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;

/**
 * 
 * @author vear (Arpad Vekas)
 */
public class TriBatch extends Renderable {
    
    protected BaseGeometry target;
    
    public TriBatch() {
        super();
        worldBound = new BoundingVolume[Frame.MAX_FRAMES];
    }
    
    public TriBatch(BaseGeometry target) {
        super();
        worldBound = new BoundingVolume[Frame.MAX_FRAMES];
        this.target = target;
    }

    /**
     * <code>setTarget</code> sets the shared data mesh.
     * 
     * @param target
     *            the TriMesh to share the data.
     */
    public void setModel(BaseGeometry target) {
            this.target = target;
    }

    /**
     * <code>getTarget</code> returns the mesh that is being shared by this
     * object.
     * 
     * @return the mesh being shared.
     */
    public BaseGeometry getModel() {
            return target;
    }
    
    public void updateWorldBound( int frameId ) {
        if(worldBound[ frameId ] != null && parent.isLockedBounds())
            return;
        if( target!= null && target.getModelBound() != null ) {
            worldBound[ frameId ] = target.getModelBound().clone(worldBound[ frameId ]).transform(
                                    worldTransform[ frameId ].getRotation(),
                                    worldTransform[ frameId ].getTranslation(),
                                    worldTransform[ frameId ].getScale(), worldBound[ frameId ]);
        }
    }

    public BoundingVolume getLocalBound() {
        return target != null ? target.getModelBound() : null;
    }

    @Override
    public BoundingVolume getWorldBound() {
        return worldBound[ getFrameId() ];
    }

    @Override
    public String toString() {
            if (parent != null)
                    return parent.getName() + ": Batch ";

            return "orphaned batch";
    }

    @Override
    public boolean docull( CullContext ctx ) {
        if(target==null)
            return false;
        updateWorldBound( ctx.getFrameId() );
        return super.docull(ctx);
    }
    
    public void update(CullContext ctx) {}
    
    @Override
    public void draw(RenderContext ctx) {
        ctx.getRenderer().draw(this);
    }
    
}
