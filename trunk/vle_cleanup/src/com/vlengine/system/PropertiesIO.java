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

package com.vlengine.system;

import com.vlengine.app.Config;
import com.vlengine.math.Vector3f;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class PropertiesIO {
    private static final Logger logger = Logger.getLogger(PropertiesIO.class
            .getName());
    //property object
    private Properties prop;
    //the file that contains our properties.
    private String filename;
    
    public PropertiesIO(String filename) {
        if (null == filename) {
            throw new VleException("Must give a valid filename");
        }

        this.filename = filename;
        prop = new Properties();

        logger.info("PropertiesIO created");
    }
    
    public boolean load(Config conf) {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            logger.warning("Could not load properties. Creating a new one.");
            return false;
        }

        try {
            if (fin != null) {
                prop.load(fin);
                fin.close();
                Class clazz=conf.getClass();
                // go over all of them
                while(clazz.getSuperclass() != null && !clazz.getName().equals("Object")) {
                    Field[] fields = clazz.getDeclaredFields();
                    for(int i=0, mx=fields.length; i<mx; i++) {
                        Field f = fields[i];
                        String name = f.getName();
                        // not exported config values start with p_
                        if(name.startsWith("p_"))
                            continue;
                        String val = prop.getProperty(name);
                        if( val != null ) {
                            try {
                                String tn = f.getType().getName();
                                if( tn.equals("boolean")) {
                                    f.setBoolean(conf, Boolean.parseBoolean(val));
                                } else if( tn.equals("int") ) {
                                    f.setInt(conf, Integer.parseInt(val));
                                } else if( tn.equals("float") ) {
                                    f.setFloat(conf, Float.parseFloat(val));
                                } else if(tn.endsWith("Vector3f")) {
                                    String[] vals = val.split(",");
                                    if( vals!= null) {
                                        ((Vector3f)f.get(conf)).set(Float.parseFloat(vals[0]), 
                                                Float.parseFloat(vals[1]),
                                                Float.parseFloat(vals[2]));
                                    }
                                } else {
                                    f.set(conf, val);
                                }
                            } catch (IllegalAccessException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(PropertiesIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            logger.warning("Could not load properties. Creating a new one.");
            return false;
        }

        // store the values into config class
        

        logger.info("Read properties");
        return true;
    }
    
    public boolean save(Config conf) {
        FileOutputStream fout;
        try {
            fout = new FileOutputStream(filename);

            prop.clear();

            // get all implementing classes
            Class clazz=conf.getClass();
            // go over all of the superclasses, until we reach Object, which does not have superclass
            while(clazz.getSuperclass() != null) {
                Field[] fields = clazz.getDeclaredFields();
                for(int i=0, mx=fields.length; i<mx; i++) {
                    Field f = fields[i];
                    String name = f.getName();
                    if(name.startsWith("p_"))
                        continue;
                    Object val = f.get(conf);
                    if(val instanceof Vector3f) {
                        prop.put(name, ((Vector3f)val).x+","+((Vector3f)val).y+","+((Vector3f)val).z);
                    } else {
                        prop.put(name, val.toString());
                    }
                }
                clazz = clazz.getSuperclass();
            }
            prop.store(fout, "Properties");

            fout.close();
        } catch (IllegalAccessException ex) {
            Logger.getLogger(PropertiesIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            logger.warning("Could not save properties: " + e.toString());
            return false;
        }
        logger.info("Saved properties");
        return true;
    }
    
}
