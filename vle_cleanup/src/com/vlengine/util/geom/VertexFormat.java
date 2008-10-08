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

import com.vlengine.system.VleException;
import com.vlengine.util.FastList;
import java.util.EnumSet;
import java.util.HashMap;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class VertexFormat {

    //TODO: not threadsafe
    protected static final HashMap<Long,VertexFormat> formatCache = new HashMap<Long,VertexFormat>();

    // the list of attributes
    private final FastList<VertexAttribute> attributes = new FastList<VertexAttribute>();
    
    // the index of attributes by type
    private final VertexAttribute[] attrindex = new VertexAttribute[VertexAttribute.Usage.getQuantity()];
    
    //length in bytes
    private int lenght = 0;
    // lenght in floats
    private int floats = 0;
    private boolean locked = false;
    //private EnumSet<VertexAttribute.Usage> attrSignature = EnumSet.noneOf(VertexAttribute.Usage.class);
    private long binarySignature = 0;
    private String name;
    
    // precreate common formats
    static {
        createFormats();
    }
    
    protected VertexFormat( String name ) {
        this.name = name;
    }
    
    protected VertexFormat(  ) {
        
    }
    
    protected static void createFormats() {
        long bformat = (1<<VertexAttribute.Usage.Position.id) 
                | (1<<VertexAttribute.Usage.Normal.id) 
                | (1<<VertexAttribute.Usage.Texture0uv.id);
        VertexFormat vtxFormat = VertexFormat.getFormat(bformat);
        if(!vtxFormat.isLocked()) {
            vtxFormat.addAttribute(VertexAttribute.Usage.Position);
            vtxFormat.addAttribute(VertexAttribute.Usage.Normal);
            vtxFormat.addAttribute(VertexAttribute.Usage.Texture0uv);
            vtxFormat.name = generateName(vtxFormat.binarySignature);
            vtxFormat.lock();
        }
    }
    
    protected static VertexFormat getFormat( long name ) {
        VertexFormat vform = null;
        if(name != 0 ) {
            vform = formatCache.get(name);
            if( vform != null ) {
                return vform;
            }
        }
        vform = new VertexFormat();
        return vform;
    }
    
    public static VertexFormat getDefaultFormat(long format) {
        //String fname = VertexFormat.generateName(format);
        VertexFormat vtxFormat = VertexFormat.getFormat(format);
        if(!vtxFormat.isLocked()) {
            // we need to create the format
            for(int i=0;i<VertexAttribute.USAGE_MAX; i++) {
                if((format & (1<<i))!=0) {
                    vtxFormat.addAttribute(VertexAttribute.Usage.getById(i));
                }
            }
            vtxFormat.lock();
        }
        return vtxFormat;
    }
    
    public static VertexFormat getDefaultFormat(EnumSet<VertexAttribute.Usage> format) {
        long form = 0;
        for(VertexAttribute.Usage a : format) {
            form |= (1<<a.id);
        }
        VertexFormat vtxFormat = VertexFormat.getFormat(form);
        if(!vtxFormat.isLocked()) {
            // we need to create the format
            for(VertexAttribute.Usage a: format ) {
                vtxFormat.addAttribute(a);
            }
            /*
            for(int i=0;i<VertexAttribute.DEFAULT_ATTRIB_BYTES.length; i++) {
                if(VertexFormat.isRequested(format, i)) {
                    vtxFormat.addAttribute(i);
                }
            }
             */
            vtxFormat.lock();
        }
        return vtxFormat;
    }
    
    /*
    public void addAttribute( VertexAttribute.Usage attr) {
        if( attType < VertexAttribute.DEFAULT_ATTRIB_BYTES.length ) 
            addAttribute(attType, VertexAttribute.DEFAULT_ATTRIB_BYTES[attType]);
        else
            throw new VleException("No default size for vertex attribute type "+attType);
    }
    
    protected void addAttribute( VertexAttribute.Usage, int len ) {
        addAttribute( attType, len/4, len);
    }
     */

    protected void addAttribute( VertexAttribute.Usage attType) {
        if(locked)
            return;
        VertexAttribute att = new VertexAttribute(attType, lenght/4);
        // put it into attributes
        attributes.add(att);
        // increase the length
        lenght += attType.bytes;
        // increase the float length
        floats += attType.floats;
        // mark that we hold a given attribute type
        binarySignature = setRequested(binarySignature, attType);
        // put it into index array also (for faster access)
        
        attrindex[attType.id] = att;
    }

    public boolean isLocked() {
        return locked;
    }
    
    public void lock() {
        if(locked)
            return;
        locked = true;
        if( binarySignature != 0 ) {
            formatCache.put(binarySignature, this);
        }
    }
    
    public boolean hasAttribute(VertexAttribute.Usage type) {
        return isRequested(binarySignature, type);
    }
    
    public FastList<VertexAttribute> getAttributes() {
        return attributes;
    }
    
    public VertexAttribute getAttribute(VertexAttribute.Usage attType) {
        return attrindex[attType.id];
    }

    public int getBytes() {
        return floats * 4;
    }

    public int getSize() {
        return this.floats;
    }
    
    public long getBinarySignature() {
        return binarySignature;
    }
    
    public static boolean isRequested(long format, VertexAttribute.Usage type) {
        return (format & (1<<type.id)) != 0;
    }

    public static long setRequested(VertexAttribute.Usage type) {
        return (1<<type.id);
    }
    
    public static long setRequested(long format, VertexAttribute.Usage type) {
        format |= (1<<type.id);
        return format;
    }
    
    public static String generateName(EnumSet<VertexAttribute.Usage> format) {
        StringBuffer sb = new StringBuffer();
        for(VertexAttribute.Usage a:format) {
            sb.append(a.name);
            sb.append(a.bytes);
        }
        /*
        for(int i=0, mx=VertexAttribute.DEFAULT_ATTRIB_NAMES.length; i<mx; i++) {
            if(isRequested(format, i)) {
                sb.append(VertexAttribute.DEFAULT_ATTRIB_NAMES[i]);
                sb.append(VertexAttribute.DEFAULT_ATTRIB_BYTES[i]);
            }
        }
         */
        return sb.toString();
    }
    
    public static String generateName(long format) {
        StringBuffer sb = new StringBuffer();
        for(int i=0, mx=VertexAttribute.USAGE_MAX; i<mx; i++) {
            if((format & (1<<i))!=0) {
                sb.append(VertexAttribute.Usage.getById(i).name);
                sb.append(VertexAttribute.Usage.getById(i).bytes);
            }
        }
        return sb.toString();
    }   

    @Override
    public String toString() {
        return generateName(binarySignature);
    }
}
