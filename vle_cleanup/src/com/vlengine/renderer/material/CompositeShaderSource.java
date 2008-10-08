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

package com.vlengine.renderer.material;

import com.vlengine.util.FastList;
import java.util.HashMap;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class CompositeShaderSource extends ShaderSource {

    FastList<ShaderSource> sources = new FastList<ShaderSource>();
    
    public void clear() {
        sources.clear();
    }
    
    public void addSource(ShaderSource src) {
        sources.add(src);
    }
    
    @Override
    public String getVertDeclarations(ShaderKey key, HashMap<String, String> variables) {
        // go from first to last, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<sources.size(); i++) {
            sb.append(sources.get(i).getVertDeclarations(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getVertBody1(ShaderKey key, HashMap<String, String> variables) {
        // go from first to last, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<sources.size(); i++) {
            sb.append(sources.get(i).getVertBody1(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getVertBody2(ShaderKey key, HashMap<String, String> variables) {
        // go from first to last, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<sources.size(); i++) {
            sb.append(sources.get(i).getVertBody2(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getVertBody3(ShaderKey key, HashMap<String, String> variables) {
        // go from first to last, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<sources.size(); i++) {
            sb.append(sources.get(i).getVertBody3(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getVertBodyEnd(ShaderKey key, HashMap<String, String> variables) {
        // go from last to first, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=sources.size()-1; i>=0; i--) {
            sb.append(sources.get(i).getVertBodyEnd(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getFragDeclarations(ShaderKey key, HashMap<String, String> variables) {
        // go from first to last, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<sources.size(); i++) {
            sb.append(sources.get(i).getFragDeclarations(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getFragFunctions(ShaderKey key, HashMap<String, String> variables) {
        // go from first to last, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<sources.size(); i++) {
            sb.append(sources.get(i).getFragFunctions(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getFragBody1(ShaderKey key, HashMap<String, String> variables) {
        // go from first to last, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<sources.size(); i++) {
            sb.append(sources.get(i).getFragBody1(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getFragBody2(ShaderKey key, HashMap<String, String> variables) {
        // go from first to last, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<sources.size(); i++) {
            sb.append(sources.get(i).getFragBody2(key, variables));
        }
        return sb.toString();
    }

    @Override
    public String getFragEnd(ShaderKey key, HashMap<String, String> variables) {
        // go from last to first, and concatenate
        StringBuffer sb = new StringBuffer();
        for(int i=sources.size()-1; i>=0; i--) {
            sb.append(sources.get(i).getFragEnd(key, variables));
        }
        return sb.toString();
    }

}
