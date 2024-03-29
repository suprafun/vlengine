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

package com.vlengine.util;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>LittleEndien</code> is a class to read littleendien stored data
 * via a InputStream.  All functions work as defined in DataInput, but
 * assume they come from a LittleEndien input stream.  Currently used to read .ms3d and .3ds files.
 * @author Jack Lindamood
 */
public class LittleEndien implements DataInput{

    private InputStream in;
    //private BufferedReader inRead;

    /**
     * Creates a new LittleEndien reader from the given input stream.  The
     * stream is wrapped in a BufferedReader automatically.
     * @param in The input stream to read from.
     */
    public LittleEndien(InputStream in){
        this.in = in;
        //inRead=new BufferedReader(new InputStreamReader(in));
    }

    public final int readUnsignedShort() throws IOException{
        return (in.read()&0xff) | ((in.read()&0xff) << 8);
    }
    
    /**
     * read an unsigned int as a long
     */
    public final long readUInt() throws IOException{
        return ((in.read()&0xff) |
            ((in.read()&0xff) << 8) |
            ((in.read()&0xff) << 16) |
            (((long)(in.read()&0xff)) << 24)
        );
    }
    
    public final boolean readBoolean() throws IOException{
        return (in.read()!=0);
    }

    public final byte readByte() throws IOException{
        return (byte) in.read();
    }

    public final int readUnsignedByte() throws IOException{
        return in.read();
    }

    public final short readShort() throws IOException{
        return (short) this.readUnsignedShort();
    }

    public final char readChar() throws IOException{
        return (char) this.readUnsignedShort();
    }
    public final int readInt() throws IOException{
        return (
            (in.read()&0xff) |
            ((in.read()&0xff) << 8) |
            ((in.read()&0xff) << 16) |
            ((in.read()&0xff) << 24)
        );
    }
    
    public final long readLong() throws IOException{
        return (
            (in.read()&0xff) |
            ((long)(in.read()&0xff) << 8) |
            ((long)(in.read()&0xff) << 16) |
            ((long)(in.read()&0xff) << 24) |
            ((long)(in.read()&0xff) << 32) |
            ((long)(in.read()&0xff) << 40) |
            ((long)(in.read()&0xff) << 48) |
            ((long)(in.read()&0xff) << 56)
        );
    }

    public final float readFloat() throws IOException{
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException{
        return Double.longBitsToDouble(readLong());
    }

    public final void readFully(byte b[]) throws IOException{
        in.read(b, 0, b.length);
    }

    public final void readFully(byte b[], int off, int len) throws IOException{
        in.read(b, off, len);
    }

    public int skipBytes(int num) throws IOException {        
        int skipped=0;
        do {
            skipped+=in.skip(num-skipped);
        } while(skipped<num);
        return skipped;
    }
    
    /*
    public final int skipBytes(int n) throws IOException{
        return (int) in.skip(n);
    }
     */

    public final String readLine() throws IOException{
        StringBuffer s = new StringBuffer();
        s.setLength(0);
        char c=0xa;
        do {
            c=(char)readUnsignedByte();
            if(c!=0xa && c!=0xd) s.append(c);
        } while(c!=0xa);
        return s.toString();
    }
 

    public final String readUTF() throws IOException{
        throw new IOException("Unsupported operation");
    }

    public final  void close() throws IOException{
        in.close();
    }
    
    public final int available() throws IOException{
        return in.available();
    }
    
    // read the given number of characters
    public String readChars(int len) throws IOException {
        byte b[]=new byte[len];
        in.read(b);
        String hdr=new String(b);
        return hdr;
    }
    
    // read 3 bytes (24 bits)
    public int read3ByteLen() throws IOException {
        return (in.read()&0xff) |
        ((in.read()&0xff) << 8) |
        ((in.read()&0xff) << 16);
    }
    
    private StringBuffer readString(char endmark) throws IOException {
        StringBuffer s = new StringBuffer();
        s.setLength(0);
        char c=endmark;
        do {
            c=(char)readUnsignedByte();
            if(c!=endmark) s.append(c);
        } while(c!=endmark);
        return s;
    }
    
    /*
     * Reads in a zero-terminated string
     */
    public StringBuffer readStringZ() throws IOException {
        return readString((char)0);
    }
}