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

package com.vlengine.util.geom;

import com.vlengine.math.Vector2f;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import java.nio.FloatBuffer;

/**
 * Provides one type of data from a VertexBuffer,
 * handles proper stride of the vertex format.
 * 
 * @author vear (Arpad Vekas)
 */
public class VertexIterator {
    
    // the data buffer we are working on
    protected final FloatBuffer data;
    // first vertex position
    protected final int startVertex;
    // the number of vertices we have
    protected final int numVertex;
    // the length of one vertex
    protected final int vertexfloats;
    // the start position of the data inside the one vertexs data
    protected final int datastart;
    // the length (in floats) of the data
    protected final int datafloats;
    
    public VertexIterator(FloatBuffer data, 
            int startVertex, int numVertex, 
            int vertexfloats,
            int datastart, int datafloats)
    {
        this.data = data;
        this.startVertex = startVertex;
        this.numVertex = numVertex;
        this.vertexfloats = vertexfloats;
        this.datastart = datastart;
        this.datafloats = datafloats;
    }
    
    protected int getDataPos(int index) {
        return (startVertex+index) * vertexfloats + datastart;
    }

    public Vector2f get(int index, Vector2f store) {
        if(store==null)
            store=new Vector2f();
        int datapos = getDataPos(index);
        store.x = data.get(datapos);
        store.y = data.get(datapos+1);
        return store;
    }

    public Vector3f get(int index, Vector3f store) {
        if(store==null)
            store=new Vector3f();
        int datapos = getDataPos(index);
        store.x = data.get(datapos);
        store.y = data.get(datapos+1);
        store.z = data.get(datapos+2);
        return store;
    }
    
    public void put(int index, VertexIterator src, int srcindex) {
        int datapos = getDataPos(index);
        int srcpos = src.getDataPos(srcindex);
        
        for (int i = 0; i < datafloats; i++) {
            data.put(datapos + i, src.data.get(srcpos + i));
        }
    }

    public void put(int index, ColorRGBA value) {
        int datapos = getDataPos(index);
        data.put(datapos, value.r);
        data.put(datapos+1, value.g);
        data.put(datapos+2, value.b);
        data.put(datapos+3, value.a);
    }
    
    public void put(int index, Vector3f value) {
        int datapos = getDataPos(index);
        data.put(datapos, value.x);
        data.put(datapos+1, value.y);
        data.put(datapos+2, value.z);
    }
}
