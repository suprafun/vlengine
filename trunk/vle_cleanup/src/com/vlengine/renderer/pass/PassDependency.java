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

package com.vlengine.renderer.pass;

import com.vlengine.util.FastList;
import com.vlengine.util.IntList;

/**
 * Represents a pass depending on other pass
 * 
 * @author vear (Arpad Vekas)
 */
public class PassDependency {

    //protected FastList<Dependency> deps = new FastList<Dependency>();
    protected FastList<RenderPass> passById = new FastList<RenderPass>();
    protected IntList second = new IntList();
    protected IntList first = new IntList();
    
    /*
    public class Dependency {
        
        protected RenderPass pass;
        protected RenderPass dependsPass;
        
        public Dependency( RenderPass pass, RenderPass dependsPass ) {
            this.pass = pass;
            this.dependsPass = dependsPass;
        }
    }
     */
    
    public void clear() {
        //deps.clear();
        passById.clear();
        second.clear();
        first.clear();
    }
    
    public void addPassDependency( RenderPass pass, RenderPass dependson ) {
        // check if the dependency already exists
        if( isPassDependant( pass, dependson ) )
            return;
        synchronized( this ) {
            int pid = pass.getId();
            int did = dependson.getId();
            if(passById.size()<=pid) {
                passById.ensureCapacity(pid+1);
            }
            if(passById.size()<=did) {
                passById.ensureCapacity(did+1);
            }
            passById.set(pid, pass);
            passById.set(did, dependson);
            second.add(pid);
            first.add(did);
        }
    }
    
    public void addPassDependency( int firstPass, int lastPass) {
        this.first.add(firstPass);
        this.second.add(lastPass);
    }

    public void addPassDependency( int prevPass, int middlePass, int lastPass) {
        // middle depends on first
        first.add(prevPass);
        second.add(middlePass);
        
        // last depends on middle
        first.add(middlePass);
        second.add(lastPass);
    }
    
    
    
    // check if a pass depends on other pass
    public boolean isPassDependant( RenderPass pass, RenderPass dependson ) {
        int pid = pass.getId();
        int did = dependson.getId();
        for( int i=0; i < second.size(); i++ ) {
            if(second.get(i) == pid
                && first.get(i)==did)
                return true;
        }
        return false;
    }
    
    public int getQuantity() {
        return second.size();
    }
    
    public RenderPass getDependent( int index ) {
        return passById.get(second.get(index));
    }
    
    public RenderPass getDependson( int index ) {
        return passById.get(first.get(index));
    }
    
    public FastList<RenderPass> getPassOrder( FastList<RenderPass> pass, FastList<RenderPass> store ) {
        if( store == null )
            store = new FastList<RenderPass>();
        // get first pass
        store.clear();
        // get dependency for all passes requested
        for(int i = 0; i < pass.size(); i++) {
            // do we need to process this pass?
            RenderPass p = pass.get(i);
            getPassOrder( p, store );
        }
        return store;
    }
    
    private void getPassOrder( RenderPass pass, FastList<RenderPass> store ) {
        if( ! store.contains( pass ) ) {
            int pid = pass.getId();
            // get all the passes this pass depends on
            for( int i=0; i < second.size(); i++ ) {
                if( second.get(i) == pid ) {
                    // we found a pass we depend on, get dependency for it
                    // recursively
                    getPassOrder( passById.get(first.get(i)), store );
                }
            }
            // we resolved all dependencyes, add this pass to store
            store.add( pass );
        }
    }
    
}
