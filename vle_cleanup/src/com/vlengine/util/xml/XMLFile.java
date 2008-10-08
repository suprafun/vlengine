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
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger; 

/**
 *
 * @author vear (Arpad Vekas)
 */
public class XMLFile {
    
    private static final Logger _log = Logger.getLogger(XMLFile.class.getName());

    /** Creates a new instance of MyXML */
    public XMLFile() { }
    
    public static Element fromXML(String filename, boolean dozip) {
        Element npj=null;
        try {
            // feldolgozzuk az XML-t
            XMLReader xr=readXML(filename, dozip);
            // skip the header
            String se=xr.skipRootElement();
            npj=new Element(se);
            npj.fromXML(xr);
            xr.close();
        } catch(Exception e) {
            //_log.log(Level.SEVERE, "Cannot load XML file "+filename, e);
        }
        return npj;
    }
    
    public static Element fromXML(InputStream is, boolean dozip) {
        Element npj=null;
        try {
            // feldolgozzuk az XML-t
            XMLReader xr=readXML(is, dozip);
            // skip the header
            String se=xr.skipRootElement();
            npj=new Element(se);
            npj.fromXML(xr);
            xr.close();
        } catch(Exception e) {
            //_log.log(Level.SEVERE, "Cannot load XML file "+filename, e);
        }
        return npj;
    }
    
    public static Element fromXML(Reader r) {
        Element npj=null;

        try {
            XMLReader xr=new XMLReader(r);
            String se;
            se = xr.skipRootElement();
            npj=new Element(se);
            npj.fromXML(xr);
            xr.close();
        } catch (IOException ex) {
            _log.log(Level.SEVERE, "Cannot read XML stream", ex);
        }
        return npj;
    }
    
    protected static XMLReader readXML(String filename, boolean dozip) throws FileNotFoundException, IOException {
        return readXML(new FileInputStream(filename), dozip);
    }
    
    protected static XMLReader readXML(InputStream is, boolean dozip) {
        try {
            BufferedReader rdr = null;
            if(dozip) {
                rdr = new BufferedReader(new InputStreamReader(new java.util.zip.GZIPInputStream(is)));
            } else {
                rdr=new BufferedReader(new InputStreamReader(is));
            }
            XMLReader xr=new XMLReader(rdr);
            return xr;
        } catch (Exception e) {
        }
        return null;
    }
    
    public static void toXML(String filename, Element doc, boolean dozip) {
        try {
            OutputStream out;
            out = new FileOutputStream(filename);
            
            toXML(out, doc, dozip);
            
        } catch(Exception e) {
        }
    }
    
    public static boolean toXML(OutputStream outS, Element doc, boolean dozip) {
        try {
            OutputStream out;
            if(dozip) {
                out = new BufferedOutputStream(new java.util.zip.GZIPOutputStream( outS));
            } else {
                 out = new BufferedOutputStream(outS);
            }
            
            // print out the header
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
            WrappedByteChannel bc=new WrappedByteChannel(out);
            doc.toXML(bc);
            out.close();
        } catch(Exception e) {
            return false;
        }
        return true;
    }
}
