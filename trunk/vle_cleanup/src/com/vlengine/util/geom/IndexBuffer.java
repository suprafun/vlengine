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

import com.vlengine.renderer.VBOAttributeInfo;
import com.vlengine.util.IntList;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author vear (Arpad Vekas)
 */
public abstract class IndexBuffer {
        
    protected VBOAttributeInfo vboInfo;
    protected int numIndices;
    
    public static IndexBufferInt createBuffer(IntBuffer ib, IndexBuffer store) {
        IndexBufferInt ret;
        if( store != null && store instanceof IndexBufferInt ) {
            ret = (IndexBufferInt) store;
            ret.setData(ib);
        } else
            ret = new IndexBufferInt(ib);
        return ret;
    }
    
    public static IndexBufferShort createBuffer(ShortBuffer ib, IndexBuffer store) {
        IndexBufferShort ret;
        if( store != null && store instanceof IndexBufferShort ) {
            ret = (IndexBufferShort) store;
            ret.setData(ib);
        } else
            ret = new IndexBufferShort(ib);
        return ret;
    }

    public static IndexBuffer createEmptyBuffer(int numIndex, int numVertex, IndexBuffer store) {
        if( isShortBufferPossible(numVertex) )
            return createEmptyShortBuffer(numIndex, store);
        else
            return createEmptyIntBuffer(numIndex, store);
    }
    
    public static IndexBuffer createBuffer(int numIndex, int numVertex, IndexBuffer store) {
        if( isShortBufferPossible(numVertex) )
            return createShortBuffer(numIndex, store);
        else
            return createIntBuffer(numIndex, store);
    }

    public static boolean isShortBufferPossible(int numVertex) {
        return numVertex< (2<<15);
    }
    
    public static IndexBufferInt createIntBuffer(int numIndex, IndexBuffer store) {
        IndexBufferInt ret;
        IntBuffer ib = BufferUtils.createIntBuffer(numIndex);
        if( store != null && store instanceof IndexBufferInt ) {
            ret = (IndexBufferInt) store;
            ret.setData(ib);
        } else
            ret = new IndexBufferInt(ib);
        return ret;
    }

    public static IndexBufferShort createShortBuffer(int numIndex, IndexBuffer store) {
        IndexBufferShort ret;
        ShortBuffer ib = BufferUtils.createShortBuffer(numIndex);
        if( store != null && store instanceof IndexBufferShort ) {
            ret = (IndexBufferShort) store;
            ret.setData(ib);
        } else
            ret = new IndexBufferShort(ib);
        return ret;
    }
    
    public static IndexBufferInt createEmptyIntBuffer(int numIndex, IndexBuffer store) {
        if( store != null 
                && store instanceof IndexBufferInt ) {
            if( store.getNumIndices() == numIndex) {
                return (IndexBufferInt) store;
            } else {
                store.removeBuffer();
                store.setNumIndices(numIndex);
                return (IndexBufferInt) store;
            }
        }
        IndexBufferInt ret = new IndexBufferInt();
        ret.setNumIndices(numIndex);
        return ret;
    }

    public static IndexBufferShort createEmptyShortBuffer(int numIndex, IndexBuffer store) {
        if( store != null 
                && store instanceof IndexBufferShort ) {
            if( store.getNumIndices() == numIndex ) {
                return (IndexBufferShort) store;
            } else {
                store.removeBuffer();
                store.setNumIndices(numIndex);
                return (IndexBufferShort) store;
            }
        }
        IndexBufferShort ret = new IndexBufferShort();
        ret.setNumIndices(numIndex);
        return ret;
    }

    public static IndexBuffer createBuffer(int[] indices, int numVertex, IndexBuffer store) {
        store = createBuffer(indices.length, numVertex, store);
        store.put(indices);
        return store;
    }

    
    public abstract void clear();

    public abstract int get(int index);
    
    public abstract int get();
    
    public abstract int limit();
    
    public abstract Buffer limit(int n);

    public abstract int position();
    
    public abstract void position(int p);

    public abstract void put( int i );
    
    public abstract void put(int index, int i);

    public abstract void rewind();
    
    public abstract Buffer getBuffer();
    
    public abstract void removeBuffer();
    
    public abstract void setData(ByteBuffer bb);
    
    public void setVBOInfo( VBOAttributeInfo info ) {
        vboInfo = info;
    }
    
    public VBOAttributeInfo getVBOInfo() {
            return vboInfo;
    }

    public void put( int[] indices) {
        for(int i=0; i<indices.length; i++) {
            put(indices[i]);
        }
    }
    
    public void put( IndexBuffer idx, int numIndices) {
        for(int i=0; i<numIndices; i++) {
            put(idx.get());
        }
    }
    
    public void put(IntList indices) {
        for(int i=0; i<indices.size(); i++) {
            put(indices.get(i));
        }
    }
    
    public void setNumIndices(int numIndex) {
        this.numIndices = numIndex;
    }
    
    public int getNumIndices() {
        return this.numIndices;
    }
    
    
}
