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

import com.vlengine.math.Vector3f;
import com.vlengine.system.VleException;
import com.vlengine.util.FastList;



/**
  * Manager class for reusing objects
 * 
 * @author vear (Arpad Vekas)
 */
public class ReuseManager {
    
    public static final int TYPE_BoundingBox = 0;
    public static final int TYPE_BoundingSphere = 1;
    public static final int TYPE_Vector3f = 2;
    
    public static final int MAX_TYPES = 3;
    
    protected Class[] classtypes = {
        BoundingBox.class,
        BoundingSphere.class,
        Vector3f.class
        };
    
    protected final static int INITIAL_STACK_SIZE = 10;

    FastList<ReuseStack> objectStacks = new FastList<ReuseStack>(MAX_TYPES);
    
    public ReuseManager() {

    }
    
    public BoundingVolume fetchBound(int type) {
        switch(type) {
        case BoundingVolume.BOUNDING_BOX : 
            return (BoundingVolume) fetch(TYPE_BoundingBox);
/* TODO: implement others
 */
        case BoundingVolume.BOUNDING_SPHERE :
            return (BoundingVolume) fetch(TYPE_BoundingSphere);
        default:
            return null;
        }
    }
    
    public Object fetch(int type) {
        // get the proper stack
        ReuseStack ostack = objectStacks.get(type);
        if(ostack==null) {
            // create the stack
            ostack = new ReuseStack(classtypes[type]);
            objectStacks.set(type, ostack);
        }
        return ostack.get();
    }
    
    // resets the usable objects
    public void clear() {
        // go over all stacks and clear
        for(int i=0; i<objectStacks.size(); i++) {
            ReuseStack ostack = objectStacks.get(i);
            if(ostack!=null) {
                ostack.clear();
            }
        }
    }
    
    // class for handling stacks of reused objects
    // clearing does not clear out objects
    // but simply marks them as reusable
    protected class ReuseStack {
        protected FastList objects;
        protected Class clazz;
        protected int _pos = 0;
        public ReuseStack(Class clazz) {
            objects = new FastList(INITIAL_STACK_SIZE);
            this.clazz = clazz;
        }
        
        public void clear() {
            _pos = 0;
        }
        
        public Object get() {
            Object obj = null;
            if(_pos < objects.size()) {
                obj = objects.get(_pos);
            } else {
                try {
                    obj = clazz.newInstance();
                } catch (Exception ex) {
                    throw new VleException("No proper constructor for reusable class");
                }
                objects.add(obj);
            }
            _pos++;
            return obj;
        }
    }
}
