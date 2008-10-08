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
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class XMLReader {
        private char[] buffer=new char[255];
        private StringBuffer acum=new StringBuffer();
        private Reader ch;
        private int prevstart=-1, prevend=-1;
        private boolean finishread=false;
        private String currentValue;
        private String nextValue;
        private int start=0;
                
    /** Creates a new instance of XMLReader */
    public XMLReader(Reader ch) {
        this.ch=ch;
    }
       
    public String remove() {
        String next=currentValue;
        if(prevstart>-1) {
            //acum.delete(prevstart,prevend);
            start=prevend;
            prevstart=-1;
            prevend=-1;
            currentValue=null;
        }
        return next;
    }
    
    public void skip() throws IOException {
        String val=getNext();
        if(val!=null && val.startsWith("<")) {
            back();
        }
    }
    
    public void back() {
        nextValue=currentValue;
    }
    
    public String getNext() throws IOException {
        if(nextValue!=null) {
            currentValue=nextValue;
            nextValue=null;
            return currentValue;
        }
        remove();
        
        int read=0;
        while((!finishread || acum.length()>0) && prevstart==-1) {
            
            if(acum.length()>0) {
                if(start<acum.length()) {
                    // do we have a tag element
                    if(acum.charAt(start)=='<') {
                        prevend=acum.indexOf(">",start);
                        if(prevend>-1) {
                            prevstart=start;
                            prevend++;
                        }
                    }
                    if(prevstart==-1) {
                        // do we have a text element
                        prevend=acum.indexOf("<",start);
                        if(prevend>-1 && prevend>start) {
                            prevstart=start;
                            //prevend;
                        }
                    }
                }
                if(prevstart==-1 && finishread) {
                    prevstart=start;
                    prevend=acum.length();
                }
            }
            // no new elements found, read the input some more
            if(prevstart==-1 && !finishread) {
                if( (read=ch.read(buffer)) > -1 ) {
                    // remove already processed
                    acum.delete(0,start);
                    start=0;
                    acum.append(buffer, 0, read);
                } else {
                    finishread=true;
                }
            }
        }
        if(prevstart>-1) {
            currentValue=acum.substring(prevstart, prevend);
        }
        return currentValue;
    }
    
    public String skipRootElement() throws IOException {
        String se;
        se=getNext(); skip();
        if(se.startsWith("<?xml")) {
            se=getNext(); se=se.substring(1,se.length()-1); skip();
        }
        return se;
    }
    
    public void close() throws IOException {
        if(ch!=null) {
            ch.close();
            ch=null;
            finishread=true;
        }
    }
    
    public Element getNextChildElement() throws IOException {
        Element e=null;
        String ne=getNext();
        if(ne.startsWith("</")) {
            skip();
        } else if(ne.startsWith("<") && ne.endsWith("/>")) {
            ne=ne.substring(1,ne.length()-2).trim();
            e=new Element(ne);
            skip();
        } else if(ne.startsWith("<")) {
            ne=ne.substring(1,ne.length()-1);
            e=new Element(ne);
            e.fromXML(this);
        } else back();
        return e;
    }
}
