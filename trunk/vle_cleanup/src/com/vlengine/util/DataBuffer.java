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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class DataBuffer {

    public static DataBuffer allocate(int bytes) {
        ByteBuffer data = ByteBuffer.allocate(bytes);
        DataBuffer db = new DataBuffer();
        db.setData(data);
        return db;
    }
    
    public static DataBuffer allocateDirect(int bytes) {
        ByteBuffer data = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        DataBuffer db = new DataBuffer();
        db.setData(data);
        return db;
    }
    
    private ByteBuffer data;

    public void clear() {
        data.clear();
    }

    public int getInt() {
        return data.getInt();
    }

    public int remaining() {
        return data.remaining();
    }

    public void rewind() {
        data.rewind();
    }
    
    public void put(int type) {
        data.putInt(type);
    }

    public void put(ByteBuffer toput) {
        data.put(toput);
    }
    
    public void put(String str) {
        data.put(str.getBytes());
    }
    
    public String getChars(int len) {
        byte b[]=new byte[len];
        data.get(b, data.position(), len);
        String hdr=new String(b);
        return hdr;
    }
    
    public void setData(ByteBuffer data) {
        this.data = data;
    }
    
    public ByteBuffer getData() {
        return data;
    }
}
