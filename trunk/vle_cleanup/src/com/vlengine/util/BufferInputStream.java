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

package com.vlengine.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class BufferInputStream extends InputStream {

    private final ByteBuffer buff;
    private boolean finished = false;
    
    public BufferInputStream( ByteBuffer buffer ) {
        this.buff = buffer;
    }
    
    @Override
    /** Reads a single byte from the buffer. */
    public int read() throws IOException {
        if( finished )
            return -1;

        // must &, otherwise implicit cast can change value.
        // (for example, reading the byte -1 is very different than
        //  reading the int -1, which means EOF.)
        return buff.get() & 0xFF;
    }
    
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (len == 0)
            return 0;
            
        if(finished)
            return -1;

        int available = Math.min(buff.remaining(), len);
        if(available>0)
            buff.get(buf, off, available);
        else {
            finished = true;
            return -1;
        }

        return available; // the amount we read.
    }
    
    @Override
    public long skip(long n) throws IOException {
        // not default behavior! allow skipping back
        buff.position(buff.position() + (int)n);
	return n;
    }
    
    @Override
    public int available() throws IOException {
	return buff.remaining();
    }
    
    
}
