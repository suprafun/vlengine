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
 * The passmanager stores information on passes in the scene, there
 * should be one such object for a scene
 * 
 * @author vear (Arpad Vekas)
 */
public class PassManager {
    
    // the passes gathered from the scene
    private FastList<RenderPass> pass = new FastList<RenderPass>();
    protected FastList<RenderPass> passById = new FastList<RenderPass>();
    protected IntList second = new IntList();
    protected IntList first = new IntList();
    
    // the dependencyes for passes
    //private PassDependency passd = new PassDependency();
    
    // the passes ordered for rendering
    private FastList<RenderPass> prepass = new FastList<RenderPass>();
   
    public void clear() {
        pass.clear();
        //passd.clear();
        passById.clear();
        first.clear();
        second.clear();
        prepass.clear();
    }
    
    public void addPass( RenderPass p ) {
        int id = p.getId();
        passById.ensureCapacity(id+1);
        if(passById.get(id)==null) {
            pass.add(p);
            passById.set(id, p);
        }
    }
    
    public void addAll( FastList<RenderPass> pa) {
        for(int i=0, pq = pa.size(); i< pq; i++) {
            RenderPass p = pa.get(i);
            // add this pass to our list
            // (if its not already there)
            addPass(p);
        }
    }
    
    public void remove(RenderPass p) {
        int id = p.getId();
        pass.remove(p);
        passById.set(id, null);
    }
    
    public void remove(int passId) {
        for(int i=0; i<pass.size(); i++) {
            if(pass.get(i).getId()==passId) {
                pass.remove(i);
            }
        }
        passById.set(passId, null);
        // remove all dependency
        for(int i=0; i<first.size(); i++) {
            if(first.get(i)==passId
               || second.get(i)==passId) {
                first.removeElementAt(i);
                second.removeElementAt(i);
                i--;
            }
        }
    }
    
    public int getQuantity() {
        return pass.size();
    }
        
    public RenderPass getPassByIndex( int index ) {
        return pass.get(index);
    }
    
    public RenderPass getPassById( int id ) {
        passById.ensureCapacity(id+1);
        return passById.get(id);
    }
    
    public void setPassOrder(int firstPass, int secondPass) {
        first.add(firstPass);
        second.add(secondPass);
    }

    public void setPassOrder( int prevPass, int middlePass, int lastPass) {
        // middle depends on first
        first.add(prevPass);
        second.add(middlePass);
        
        // last depends on middle
        first.add(middlePass);
        second.add(lastPass);
    }
    
    private void getPassOrder( RenderPass pass, FastList<RenderPass> store ) {
        if(pass==null)
            return;
        // do we need to process this pass?
        if( ! store.contains( pass ) ) {
            int pid = pass.getId();
            // get all the passes this pass depends on
            for( int i=0; i < second.size(); i++ ) {
                if( second.get(i) == pid ) {
                    // we found a pass we depend on, get dependency for it
                    // recursively
                    int fid = first.get(i);
                    if(fid<passById.size())
                        getPassOrder( passById.get(fid), store );
                }
            }
            // we resolved all dependencyes, add this pass to store
            store.add( pass );
        }
    }
    
    public FastList<RenderPass> getPasses() {
        return pass;
    }
    
    public FastList<RenderPass> getSortedPasses() {
        // get first pass
        prepass.clear();
        // get dependency for all passes requested
        for(int i = 0; i < pass.size(); i++) {
            RenderPass p = pass.get(i);
            getPassOrder( p, prepass );
        }
        return prepass;
    }
    
    // merges all the passes in the other manager with ours
    // also transfering dependecyes
    public void merge(PassManager other  ) {
        // add all passes
        addAll(other.pass);
        
        // add all dependecies
        for(int i=0, pq = other.first.size(); i<pq; i++) {
            first.add(other.first.get(i));
            second.add(other.second.get(i));
        }
    }
}
