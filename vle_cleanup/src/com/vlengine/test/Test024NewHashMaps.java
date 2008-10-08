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

package com.vlengine.test;

import com.vlengine.util.IntList;
import com.vlengine.util.ShortShortMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test024NewHashMaps {

    long start;
    
    int num = 10000;
    long checksum = 0;
    
    IntList randoms = new IntList();
    short[] state;
        
    public void createData() {
        Random rnd = new Random(52342131);
        randoms.ensureCapacity(num);
        // create random numbers
        for(int i=0; i<num; i++) {
            int val = 0;
            // value of 0 has special handling, dont use it
            while(val==0)
                val=rnd.nextInt()>>16;
            randoms.add(val);
        }
        
        // simulate a map with simple array
        state = new short[Short.MAX_VALUE*2];
    }
    
    public void TestShortMap() {
        
        
        
        // create the map
        ShortShortMap map = new ShortShortMap(num);
        //HashMap map = new HashMap();
        // put values into map, remove half of them immediately
        for(short i=0; i<num; i++) {
            // key 0 has special meaning, so dont use it as a key
            short key = (short)randoms.get(i);
            short val = (short)(i+1);
            map.put(key, val);
            //map.put(new Short(key), val);
            // remove every other key
            if((i&1) == 0) {
                map.remove(key);
                state[key+Short.MAX_VALUE] = 0;
            } else {
                state[key+Short.MAX_VALUE] = val;
            }
        }
        /*
        for(int i=0; i<num; i++) {
            short key = (short)(i+1);
            short val = (short)randoms.get(i);
            short valm=map.get(key);
            if((i&1)==0) {
                if(valm!=0)
                    checksum++;
            } else {
                if(val != valm)
                    checksum++;
            }
        }
         */
        
        // check the contents of the map
        
        for(int i=0; i<state.length; i++) {
            short key = (short)(i-Short.MAX_VALUE);
            short val;
            
                //val = (Short)map.get(new Short(key));
            
                val = map.get(key);
            if(state[i]!=val) {
                checksum++;
            }
        }

    }

    public void TestHashMap() {
        
        // create the map
        //ShortShortMap map = new ShortShortMap();
        HashMap<Short,Short> map = new HashMap<Short,Short>(num);
        // put values into map, remove half of them immediately
        for(short i=0; i<num; i++) {
            // key 0 has special meaning, so dont use it as a key
            short key = (short)randoms.get(i);
            short val = (short)(i+1);
            map.put(key, val);
            // remove every other key
            if((i&1) == 0) {
                map.remove(key);
                state[key+Short.MAX_VALUE] = 0;
            } else {
                state[key+Short.MAX_VALUE] = val;
            }
        }
        /*
        for(int i=0; i<num; i++) {
            short key = (short)(i+1);
            short val = (short)randoms.get(i);
            short valm=map.get(key);
            if((i&1)==0) {
                if(valm!=0)
                    checksum++;
            } else {
                if(val != valm)
                    checksum++;
            }
        }
         */
        
        // check the contents of the map
        
        for(int i=0; i<state.length; i++) {
            short key = (short)(i-Short.MAX_VALUE);
            short val = 0;
            
             Short vals = map.get(key);
             if(vals!=null)
                 val = vals.shortValue();
            
            //    val = map.get(key);
            if(state[i]!=val) {
                checksum++;
            }
        }

    }
    
    protected long doTest(int test) {
        checksum = 0;
        
        Arrays.fill(state, (short)0);

        start = System.currentTimeMillis();
        switch(test) {
            case 0 : {
                TestShortMap();
            } break;
            case 1 : {
                TestHashMap();
            } break;
        
        }        
        return System.currentTimeMillis()-start;
    }
    
    public static void main( String[] args ) {
        
        Test024NewHashMaps test = new Test024NewHashMaps();
        
        test.createData();

        long shortTime = 0;
        long shortChecksum = 0;
        long hashTime = 0;
        long hashChecksum = 0;
        
        // prime the JVM with 100 runs
        for(int i=0; i<100; i++) {
            shortTime += test.doTest(0);
            shortChecksum += test.checksum;
            hashTime += test.doTest(1);
            hashChecksum += test.checksum;
        }
        
        // reset values
        shortTime = 0;
        shortChecksum = 0;
        hashTime = 0;
        hashChecksum = 0;
        
        // do the calculated 1000 runs
        for(int i=0; i<1000; i++) {
            shortTime += test.doTest(0);
            shortChecksum += test.checksum;
            hashTime += test.doTest(1);
            hashChecksum += test.checksum;
        }        
                
        float shortAll = shortTime/1000f;
        float hashAll = hashTime/1000f;
        
        System.out.println("ShortMap runtime: "+shortAll);
        System.out.println("ShortMap checksum: "+shortChecksum);
        System.out.println("HashMap runtime: "+hashAll);
        System.out.println("HashMap checksum: "+hashChecksum);
    }
}
