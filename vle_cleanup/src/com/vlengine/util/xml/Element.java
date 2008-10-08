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

package com.vlengine.util.xml;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Element {
    
    protected static final int VALUE_NULL=0;
    protected static final int VALUE_NODE=1;
    protected static final int VALUE_OBJ=2;   // data stored in objValue
    protected static final int VALUE_STR=4;
    protected static final int VALUE_INT=8;   // data stored in intValue
    protected static final int VALUE_SHORT=16;
    protected static final int VALUE_FLOAT=32;
    protected static final int VALUE_BOOLEAN=64;
    protected static final int VALUE_BYTE=128;
    protected static final int VALUE_BITSET=256;
    protected static final int VALUE_ARRAY=512;   // array stored in objValue
    protected static final int VALUE_LONG=1024;   // data stored in longValue
    
    protected FastList<Element> children;
    protected String name;
    protected int dtype=0;
    protected Object objValue;
    protected int intValue;
    protected long longValue;
    protected float floatValue;
    protected Element parent;
    
    /** Creates a new instance of Element */
    public Element(String name) {
        this.name=name;
    }
    
    public String getName() {
        return name;
    }
    
    public Element setText(String string) {
        objValue=string;
        dtype=VALUE_STR|VALUE_OBJ;
        return this;
    }

    public Element setText(Integer integer) {
        intValue=integer.intValue();
        dtype=VALUE_INT;
        return this;
    }
    
    public Element setText(int integer) {
        intValue=integer;
        dtype=VALUE_INT;
        return this;
    }

    public Element setText(long longval) {
        longValue=longval;
        dtype=VALUE_LONG;
        return this;
    }
    
    public long getTextlong() {
        if((dtype&VALUE_LONG)!=0)
            return longValue;
        else if((dtype&VALUE_FLOAT)!=0) 
            return (long)floatValue;
        else if((dtype&VALUE_INT)!=0) 
            return (int)intValue;
        else
            return Long.parseLong(getText());
    }
    
    public Element setText(float floatvalue) {
        floatValue=floatvalue;
        dtype=VALUE_FLOAT;
        return this;
    }
    
    public Element setText(short shortvalue) {
        intValue=shortvalue;
        dtype=VALUE_SHORT|VALUE_INT;
        return this;
    }
    
    public Element setText(byte bytevalue) {
        intValue=bytevalue;
        dtype=VALUE_BYTE|VALUE_INT;
        return this;
    }
    
    public Element setText(boolean booleanvalue) {
        intValue=booleanvalue?1:0;
        dtype=VALUE_BOOLEAN|VALUE_INT;
        return this;
    }
    
    public Element setText(boolean[] bitset) {
        intValue=0;
        for(int i=0;i<bitset.length;i++) {
            intValue|=(bitset[i]?1:0)<<i;
        }
        dtype=VALUE_BITSET|VALUE_INT;
        return this;
    }
    
    public short getTextshort() {
        return (short)getTextint();
    }
    
    public Integer getTextInteger() {
        return new Integer(getTextint());
    }
    
    public int getTextint() {
        if((dtype&VALUE_INT)!=0)
            return intValue;
        else if((dtype&VALUE_FLOAT)!=0) 
            return (int)floatValue;
        else
            return Integer.parseInt(getText());
    }

    public byte getTextbyte() {
        return (byte)getTextint();
    }
    
    public boolean getTextboolean() {
        if((dtype&VALUE_BOOLEAN)!=0)
            return intValue!=0;
        else
            return getText().equals("true");
    }
    
    public float getTextfloat() {
        if((dtype&VALUE_FLOAT)!=0) 
            return floatValue;
        else if((dtype&VALUE_INT)!=0)
            return intValue;
        else
            return Float.parseFloat(getText());
    }
    
    public boolean[] getTextbitset(boolean[] store) {
        int val=0;
        if((dtype&VALUE_INT)!=0) {
            val=intValue;
        } else {
            val=Integer.parseInt(getText());
        }
        int bits=Integer.highestOneBit(val);
        if(store==null) store=new boolean[bits+1];
        else
            bits=store.length<32?store.length-1:31;
        for(int i=0;i<=bits;i++)
            store[i]=(val&(1<<i))!=0;
        return store;
    }

    public Element setText(float[] array) {
        // stored in obj, as float, array
        dtype = VALUE_OBJ|VALUE_FLOAT|VALUE_ARRAY;
        float[] farr = Arrays.copyOf(array, array.length);
        objValue = farr;
        return this;
    }
    
    public Element setText(Vector3f vec) {
        // stored in obj, as float, array
        dtype = VALUE_OBJ|VALUE_FLOAT|VALUE_ARRAY;
        float[] farr = new float[3];
        objValue = farr;
        vec.toArray(farr);
        return this;
    }
    
    public Element setText(Quaternion q) {
        // stored in obj, as float, array
        dtype = VALUE_OBJ|VALUE_FLOAT|VALUE_ARRAY;
        float[] farr = new float[4];
        objValue = farr;
        farr[0] = q.x;
        farr[1] = q.y;
        farr[2] = q.z;
        farr[3] = q.w;
        return this;
    }

    public Element setText(ColorRGBA c) {
        // stored in obj, as float, array
        dtype = VALUE_OBJ|VALUE_FLOAT|VALUE_ARRAY;
        float[] farr = new float[4];
        objValue = farr;
        farr[0] = c.r;
        farr[1] = c.g;
        farr[2] = c.b;
        farr[3] = c.a;
        return this;
    }

    public Vector3f getText(Vector3f store) {
        if(store==null) 
            store = new Vector3f();
        if((dtype&VALUE_ARRAY)==0
           || (dtype&VALUE_FLOAT)==0) {
            // parse from text
            String val = getText();
            String[] vals = val.split(" ");
            if(vals==null || vals.length != 3)
                return null;
            store.x = Float.parseFloat(vals[0]);
            store.y = Float.parseFloat(vals[1]);
            store.z = Float.parseFloat(vals[2]);
        } else {
            float[] vala = (float[]) objValue;
            if(vala.length != 3)
                return null;
            store.x = vala[0];
            store.y = vala[1];
            store.z = vala[2];
        }
        return store;
    }
    
    public Quaternion getText(Quaternion store) {
        if(store==null) 
            store = new Quaternion();
        if((dtype&VALUE_ARRAY)==0
           || (dtype&VALUE_FLOAT)==0) {
            // parse from text
            String val = getText();
            String[] vals = val.split(" ");
            if(vals==null || vals.length != 4)
                return null;
            store.x = Float.parseFloat(vals[0]);
            store.y = Float.parseFloat(vals[1]);
            store.z = Float.parseFloat(vals[2]);
            store.w = Float.parseFloat(vals[3]);
        } else {
            float[] vala = (float[]) objValue;
            if(vala.length != 4)
                return null;
            store.x = vala[0];
            store.y = vala[1];
            store.z = vala[2];
            store.w = vala[3];
        }
        return store;
    }

    public ColorRGBA getText(ColorRGBA store) {
        if(store==null) 
            store = new ColorRGBA();
        if((dtype&VALUE_ARRAY)==0
           || (dtype&VALUE_FLOAT)==0) {
            // parse from text
            String val = getText();
            String[] vals = val.split(" ");
            if(vals==null || vals.length != 4)
                return null;
            store.r = Float.parseFloat(vals[0]);
            store.g = Float.parseFloat(vals[1]);
            store.b = Float.parseFloat(vals[2]);
            store.a = Float.parseFloat(vals[3]);
        } else {
            float[] vala = (float[]) objValue;
            if(vala.length != 4)
                return null;
            store.r = vala[0];
            store.g = vala[1];
            store.b = vala[2];
            store.a = vala[3];
        }
        return store;
    }

    public float[] getText(float[] store) {
        float[] valf;
        if((dtype&VALUE_ARRAY)==0
           || (dtype&VALUE_FLOAT)==0) {
            // parse from text
            String val = getText();
            String[] vals = val.split(" ");
            if(vals==null)
                return null;
            if(store != null && store.length == vals.length)
                valf = store;
            else
                valf = new float[vals.length];
            for(int i=0; i<vals.length; i++)
                valf[i] = Float.parseFloat(vals[i]);
        } else {
            float[] vala = (float[]) objValue;
            if(store!=null && store.length == vala.length)
                valf = store;
            else
                valf = new float[vala.length];
            System.arraycopy(vala, 0, valf, 0, vala.length);
        }
        return valf;
    }

    public float[] getChildfloat(String child, float[] store) {
        Element ce=getChild(child);
        return ce==null?null:ce.getText(store);
    }

    public String getText() {
        if((dtype&VALUE_STR)!=0)
            return (String) objValue;
        else if((dtype&VALUE_ARRAY)!=0) {
            // array, check type
            if((dtype&VALUE_FLOAT)!=0) {
                float[] vala = (float[]) objValue;
                StringBuffer sb = new StringBuffer();
                for(int i=0; i<vala.length; i++) {
                    sb.append(vala[i]);
                    sb.append(" ");
                }
                return sb.toString();
            }
            //TODO: handle other array types
        }
        else if((dtype&VALUE_OBJ)!=0)
            return objValue.toString();
        else if((dtype&VALUE_BOOLEAN)!=0)
            return intValue!=0?"true":"false";
        else if((dtype&VALUE_INT)!=0)
            return String.valueOf(intValue);
        else if((dtype&VALUE_LONG)!=0)
            return String.valueOf(longValue);
        else if((dtype&VALUE_FLOAT)!=0)
            return String.valueOf(floatValue);
        return null;
    }

    private void assureChildren() {
        if(children==null) {
            children=new FastList();
            dtype=VALUE_NODE;
        }
    }
    
    public Element addContent(Element child) {
        assureChildren();
        children.add(child);
        child.parent=this;
        return this;
    }
    
    public Element addContent(String string) {
        Element child=new Element(string);
        addContent(child);
        return child;
    }
    
    public void removeContent() {
        dtype=VALUE_NULL;
        if(children!=null) children.clear();
    }
    
    public void removeChild(Element child) {
      children.remove(child);
    }
    
    public Element getChild(String child) {
        if(children==null || dtype!=VALUE_NODE) return null;
        Element ch=null;
        Element che=null;
        for(int i=0, j=children.size(); i<j && ch==null;i++) {
            che=children.get(i);
            if(che.getName().equals(child)) ch=che;
        }
        return ch;
    }
    
    public FastList getChildren() {
        if(dtype!=VALUE_NODE)
            return null;
        return children;
    }

    public FastList getChildren(String child) {
        if(dtype!=VALUE_NODE)
            return null;
        FastList sch=new FastList();
        Element che=null;
        for(int i=0, j=children.size(); i<j;i++) {
            che=children.get(i);
            if(che.getName().equals(child)) sch.add(che);
        }
        return sch;
    }
    
    public Element setChild(String child) {
        Element che=getChild(child);
        if(che==null) {
            che=new Element(child);
            addContent(che);
        }
        return che;
    }
    
    public String getChildText(String child) {
        Element che=getChild(child);
        return che==null?null:che.getText();
    }
    
    public Integer getChildInteger(String child) {
        Element ce=getChild(child);
        return ce==null?null:ce.getTextInteger();
    }
    
    public int getChildint(String child) {
        Element ce=getChild(child);
        return ce==null?0:ce.getTextint();
    }
    
    public short getChildshort(String child) {
        Element ce=getChild(child);
        return ce==null?0:ce.getTextshort();
    }
    
    public boolean getChildboolean(String child) {
        Element ce=getChild(child);
        return ce==null?false:ce.getTextboolean();
    }

    public float getChildfloat(String child) {
        Element ce=getChild(child);
        return ce==null?0:ce.getTextfloat();
    }

    public byte getChildbyte(String child) {
        Element ce=getChild(child);
        return ce==null?0:ce.getTextbyte();
    }
    
    public boolean[] getChildbitset(String child, boolean[] store) {
        Element ce=getChild(child);
        return ce==null?null:ce.getTextbitset(store);
    }

    public Element setText(IntList intList) {
        dtype = VALUE_OBJ|VALUE_INT|VALUE_ARRAY;
        if(intList.getArray().length == intList.size()) {
            objValue = intList.getArray();
        } else {
            objValue = Arrays.copyOf(intList.getArray(), intList.size());
        }
        return this;
    }
    
    public Element setText(int[] array) {
        // stored in obj, as float, array
        dtype = VALUE_OBJ|VALUE_INT|VALUE_ARRAY;
        objValue = array;
        return this;
    }

    public IntList getTextintlist(IntList store) {
        IntList vali;
        if((dtype&VALUE_ARRAY)==0
           || (dtype&VALUE_INT)==0) {
            // parse from text
            String val = getText();
            String[] vals = val.split(" ");
            if(vals==null)
                return null;
            if(store!=null) {
                vali = store;
                vali.ensureCapacity(vals.length);
                vali.clear();
            } else {
                vali = new IntList(vals.length);
            }
            for(int i=0; i<vals.length; i++)
                vali.set(i, Integer.parseInt(vals[i]));
        } else {
            int[] vala = (int[]) objValue;
            if(store!=null) {
                vali = store;
                vali.setArray(vala);
            } else {
                vali = new IntList(vala);
            }
        }
        return vali;
    }
    
    public Element setText(Matrix4f mat) {
        float[] array = new float[16];
        mat.get(array);
        setText(array);
        return this;
    }

    public Matrix4f getTextMatrix4f(Matrix4f store) {
        if(store == null) {
            store = new Matrix4f();
        }
        float[] array = getText(new float[16]);
        if(array==null)
            return null;
        store.set(array);
        return store;
    }

    public void toXML(WritableByteChannel ch) throws IOException {
        ByteBuffer startTag=ByteBuffer.allocate(name.length()+2);
        startTag.put("<".getBytes());
        startTag.put(name.getBytes());
        startTag.put(">".getBytes());
        ch.write(startTag);
        String val=null;
        if(dtype==VALUE_NODE) {
            if(children!=null) {
                for(int i=0,j=children.size();i<j;i++) {
                    children.get(i).toXML(ch);
                }
            }
        } else {
            val=getText();
            if(val!=null)
                ch.write(ByteBuffer.wrap((val).getBytes()));
        }
        ByteBuffer endTag=ByteBuffer.allocate(name.length()+3);
        endTag.put("</".getBytes());
        endTag.put(name.getBytes());
        endTag.put(">".getBytes());
        ch.write(endTag);
    }

    public void fromXML(XMLReader r) throws IOException {
        // start reading
        boolean cont=true;
        String ne;
        while(cont) {
            ne=r.getNext();
            if(ne==null) {
                cont=false;
            } else if(ne.startsWith("</")) {
                cont=false;
                r.skip();
            } else if(ne.startsWith("<") && ne.endsWith("/>")) {
                ne=ne.substring(1,ne.length()-2).trim();
                Element sube=new Element(ne);
                addContent(sube);
                r.skip();
            } else if(ne.startsWith("<")) {
                // new child
                ne=ne.substring(1,ne.length()-1);
                Element sube=new Element(ne);
                sube.fromXML(r);
                addContent(sube);
            } else {
                String close=r.getNext();
                if(close.length()>2 && close.substring(2,close.length()-1).equals(name)) {
                    // fixed value
                    setText(ne);
                    cont=false;
                    r.skip();
                } else
                    r.back();
            }
        }
    }
    
    protected void toString(StringBuffer app) {
        app.append("[").append(name);
        if(dtype==VALUE_NODE) {
            if(children!=null) {
                for(int i=0,j=children.size();i<j;i++) {
                    children.get(i).toString(app);
                }
            }
        }
        app.append("]");
    }
    
    @Override
    public String toString() {
        StringBuffer apb=new StringBuffer();
        toString(apb);
        return apb.toString();
    }

    /**
     * Clone this element and all its children
     * @return
     */
    public Element deepClone() {
        Element copy = new Element(name);
        copy.dtype = dtype;
        copy.objValue = objValue;
        copy.intValue = intValue;
        copy.floatValue = floatValue;
        copy.longValue = longValue;

        if((dtype&VALUE_NODE)!=0 && children!=null) {
            // make a depp copy of all the children, and add them to the element
            for(int i=0; i<children.size(); i++) {
                copy.addContent(children.get(i).deepClone());
            }
        }
        return copy;
    }
}
