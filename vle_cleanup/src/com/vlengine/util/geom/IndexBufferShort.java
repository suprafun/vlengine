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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class IndexBufferShort extends IndexBuffer {

    ShortBuffer data;
    
    IndexBufferShort() {
    }
    
    IndexBufferShort(ShortBuffer data) {
        this.data = data;
    }

    protected void setData(ShortBuffer ib) {
        data = ib;
        numIndices = data.limit();
    }

    public ShortBuffer getBuffer() {
        return data;
    }
    
    @Override
    public int limit() {
        return data.limit();
    }
    
    public int get() {
        return data.get();
    }
    
    @Override
    public int get(int index) {
        return data.get(index);
    }

    @Override
    public void rewind() {
        data.rewind();
    }

    @Override
    public void put(int i) {
        data.put((short)i);
    }

    @Override
    public Buffer limit(int n) {
        return data.limit(n);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public void put(int index, int i) {
        data.put(index, (short)i);
    }

    @Override
    public void position(int p) {
        data.position(p);
    }

    @Override
    public int position() {
        return data.position();
    }
    
    @Override
    public void put(IndexBuffer idx, int numIndices) {
        if( idx instanceof IndexBufferShort ) {
            ShortBuffer bfr = (ShortBuffer) idx.getBuffer();
            int prevlimit = bfr.limit();
            bfr.limit(bfr.position() + numIndices);
            data.put(bfr);
            bfr.limit(prevlimit);
        } else
            super.put(idx, numIndices);
    }

    @Override
    public void removeBuffer() {
        data = null;
    }

    @Override
    public void setData(ByteBuffer bb) {
        setData(bb.asShortBuffer());
    }
}
