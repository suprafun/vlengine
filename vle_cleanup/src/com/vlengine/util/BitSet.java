/*
 * Copyright (c)2003-2007 Terence Parr, 2008 VL Engine
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
 * * Neither the name 'Terence Parr' nor the names of other contributors 
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

/**
 *
 * @author vear (Arpad Vekas)
 */
public class BitSet {

/* Extracted from ANTLR and modifyed for own use
*/

//package antlr.collections.impl;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id: //depot/code/org.antlr/release/antlr-2.7.5/antlr/collections/impl/BitSet.java#1 $
 */

/**A BitSet to replace java.util.BitSet.
 * Primary differences are that most set operators return new sets
 * as opposed to oring and anding "in place".  Further, a number of
 * operations were added.  I cannot contain a BitSet because there
 * is no way to access the internal bits (which I need for speed)
 * and, because it is final, I cannot subclass to add functionality.
 * Consider defining set degree.  Without access to the bits, I must
 * call a method n times to test the ith bit...ack!
 *
 * Also seems like or() from util is wrong when size of incoming set is bigger
 * than this.bits.length.
 *
 * @author Terence Parr
 * @author <br><a href="mailto:pete@yamuna.demon.co.uk">Pete Wells</a>
 */
    
    protected final static int BITS = 64;    // number of bits / long
    protected final static int NIBBLE = 4;
    protected final static int LOG_BITS = 6; // 2^6 == 64

    /* We will often need to do a mod operator (i mod nbits).  Its
     * turns out that, for powers of two, this mod operation is
     * same as (i & (nbits-1)).  Since mod is slow, we use a
     * precomputed mod mask to do the mod instead.
     */
    protected final static int MOD_MASK = BITS - 1;

    /** The actual data bits */
    protected long bits[];

    /** Construct a bitset of size one word (64 bits) */
    public BitSet() {
        this(BITS);
    }

    /** Construction from a static array of longs */
    public BitSet(long[] bits_) {
        bits = bits_;
    }

    // construct from an other bitset
    public BitSet(BitSet oth) {
        bits = new long[oth.bits.length];
        System.arraycopy(oth.bits, 0, bits, 0, bits.length);
    }
    
    /** Construct a bitset given the size
     * @param nbits The size of the bitset in bits
     */
    public BitSet(int nbits) {
        bits = new long[((nbits - 1) >> LOG_BITS) + 1];
    }

    /** or this element into this set (grow as necessary to accommodate) */
    public void add(int el) {
        //System.out.println("add("+el+")");
        int n = wordNumber(el);
        //System.out.println("word number is "+n);
        //System.out.println("bits.length "+bits.length);
        if (n >= bits.length) {
            growToInclude(el);
        }
        bits[n] |= bitMask(el);
    }

    public BitSet and(BitSet a) {
        BitSet s = (BitSet)this.clone();
        s.andInPlace(a);
        return s;
    }

    public void addAll(IntList cl) {
        for(int i=0, mx=cl.size(); i<mx; i++) {
            add(cl.get(i));
        }
    }
    
    public void andInPlace(BitSet a) {
        int min = Math.min(bits.length, a.bits.length);
        for (int i = min - 1; i >= 0; i--) {
            bits[i] &= a.bits[i];
        }
        // clear all bits in this not present in a (if this bigger than a).
        for (int i = min; i < bits.length; i++) {
            bits[i] = 0;
        }
    }

    private final static long bitMask(int bitNumber) {
        int bitPosition = bitNumber & MOD_MASK; // bitNumber mod BITS
        return 1L << bitPosition;
    }

    public void clear() {
        for (int i = bits.length - 1; i >= 0; i--) {
            bits[i] = 0;
        }
    }

    public void clear(int el) {
        int n = wordNumber(el);
        if (n >= bits.length) {	// grow as necessary to accommodate
            growToInclude(el);
        }
        bits[n] &= ~bitMask(el);
    }

    public void set(int el, boolean state) {
        int n = wordNumber(el);
        if (n >= bits.length) {	// grow as necessary to accommodate
            growToInclude(el);
        }
        if(state)
            bits[n] |= bitMask(el);
        else
            bits[n] &= ~bitMask(el);
    }
    
    @Override
    public Object clone() {
        BitSet s;
        try {
            s = (BitSet)super.clone();
            s.bits = new long[bits.length];
            System.arraycopy(bits, 0, s.bits, 0, bits.length);
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        return s;
    }

    public int degree() {
        int deg = 0;
        for (int i = bits.length - 1; i >= 0; i--) {
            long word = bits[i];
            if (word != 0L) {
                for (int bit = BITS - 1; bit >= 0; bit--) {
                    if ((word & (1L << bit)) != 0) {
                        deg++;
                    }
                }
            }
        }
        return deg;
    }

    /** code "inherited" from java.util.BitSet */
    @Override
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof BitSet)) {
            BitSet set = (BitSet)obj;

            int n = Math.min(bits.length, set.bits.length);
            for (int i = n; i-- > 0;) {
                if (bits[i] != set.bits[i]) {
                    return false;
                }
            }
            if (bits.length > n) {
                for (int i = bits.length; i-- > n;) {
                    if (bits[i] != 0) {
                        return false;
                    }
                }
            }
            else if (set.bits.length > n) {
                for (int i = set.bits.length; i-- > n;) {
                    if (set.bits[i] != 0) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /** Find ranges in a set element array.  @param elems The array of
     * elements representing the set, usually from Bit Set.toArray().
     * @return Vector of ranges.
     */
    /*
    public static FastList getRanges(int[] elems) {
        if (elems.length == 0) {
            return null;
        }
        int begin = elems[0];
        int end = elems[elems.length - 1];
        if (elems.length <= 2) {
            // Not enough elements for a range expression
            return null;
        }

        FastList ranges = new FastList(5);
        // look for ranges
        for (int i = 0; i < elems.length - 2; i++) {
            int lastInRange;
            lastInRange = elems.length - 1;
            for (int j = i + 1; j < elems.length; j++) {
                if (elems[j] != elems[j - 1] + 1) {
                    lastInRange = j - 1;
                    break;
                }
            }
            // found a range
            if (lastInRange - i > 2) {
                ranges.add(new IntRange(elems[i], elems[lastInRange]));
            }
        }
        return ranges;
    }
     */

    /**
     * Grows the set to a larger number of bits.
     * @param bit element that must fit in set
     */
    public void growToInclude(int bit) {
        int newSize = Math.max(bits.length << 1, numWordsToHold(bit));
        long newbits[] = new long[newSize];
        System.arraycopy(bits, 0, newbits, 0, bits.length);
        bits = newbits;
    }

    /**
     * Returns if the given element is contained in this bitset
     * @param el
     * @return
     */
    public boolean member(int el) {
        int n = wordNumber(el);
        if (n >= bits.length) return false;
        return (bits[n] & bitMask(el)) != 0;
    }
    
    /**
     * Returns if the given element is contained in this bitset
     * @param el
     * @return
     */
    public boolean contains(int el) {
        return member(el);
    }
    
    /**
     * Returns if the given element is contained in this bitset
     * @param el
     * @return
     */
    public boolean get(int el) {
        return member(el);
    }
    
    public boolean nil() {
        for (int i = bits.length - 1; i >= 0; i--) {
            if (bits[i] != 0) return false;
        }
        return true;
    }

    public BitSet not() {
        BitSet s = (BitSet)this.clone();
        s.notInPlace();
        return s;
    }

    public void notInPlace() {
        for (int i = bits.length - 1; i >= 0; i--) {
            bits[i] = ~bits[i];
        }
    }

    /** complement bits in the range 0..maxBit. */
    public void notInPlace(int maxBit) {
        notInPlace(0, maxBit);
    }

    /** complement bits in the range minBit..maxBit.*/
    public void notInPlace(int minBit, int maxBit) {
        // make sure that we have room for maxBit
        growToInclude(maxBit);
        for (int i = minBit; i <= maxBit; i++) {
            int n = wordNumber(i);
            bits[n] ^= bitMask(i);
        }
    }

    private final int numWordsToHold(int el) {
        return (el >> LOG_BITS) + 1;
    }

    public static BitSet of(int el) {
        BitSet s = new BitSet(el + 1);
        s.add(el);
        return s;
    }

    /** return this | a in a new set */
    public BitSet or(BitSet a) {
        BitSet s = (BitSet)this.clone();
        s.orInPlace(a);
        return s;
    }

    public void orInPlace(BitSet a) {
        // If this is smaller than a, grow this first
        if (a.bits.length > bits.length) {
            setSize(a.bits.length);
        }
        int min = Math.min(bits.length, a.bits.length);
        for (int i = min - 1; i >= 0; i--) {
            bits[i] |= a.bits[i];
        }
    }

    // remove this element from this set
    public void remove(int el) {
        int n = wordNumber(el);
        if (n >= bits.length) {
            growToInclude(el);
        }
        bits[n] &= ~bitMask(el);
    }

    /**
     * Sets the size of a set.
     * @param nwords how many words the new set should be
     */
    private void setSize(int nwords) {
        long newbits[] = new long[nwords];
        int n = Math.min(nwords, bits.length);
        System.arraycopy(bits, 0, newbits, 0, n);
        bits = newbits;
    }

    public int size() {
        return bits.length << LOG_BITS; // num words * bits per word
    }

    /** return how much space is being used by the bits array not
     *  how many actually have member bits on.
     */
    public int lengthInLongWords() {
        return bits.length;
    }

    /**Is this contained within a? */
    public boolean subset(BitSet a) {
        if (a == null || !(a instanceof BitSet)) return false;
        return this.and(a).equals(this);
    }

    /**Subtract the elements of 'a' from 'this' in-place.
     * Basically, just turn off all bits of 'this' that are in 'a'.
     */
    public void subtractInPlace(BitSet a) {
        if (a == null) return;
        // for all words of 'a', turn off corresponding bits of 'this'
        for (int i = 0; i < bits.length && i < a.bits.length; i++) {
            bits[i] &= ~a.bits[i];
        }
    }

    public int[] toArray() {
        int[] elems = new int[degree()];
        int en = 0;
        for (int i = 0; i < (bits.length << LOG_BITS); i++) {
            if (member(i)) {
                elems[en++] = i;
            }
        }
        return elems;
    }

    public void addAllint(int[] array) {
        for (int i = 0; i < array.length ; i++) {
            add(array[i]);
        }
    }
    
    public long[] toPackedArray() {
        return bits;
    }

    @Override
    public String toString() {
        return toString(",");
    }

    /** Transform a bit set into a string by formatting each element as an integer
     * @separator The string to put in between elements
     * @return A commma-separated list of values
     */
    public String toString(String separator) {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < (bits.length << LOG_BITS); i++) {
            if (member(i)) {
                if (str.length() > 0) {
                    str.append( separator );
                }
                str.append(i);
            }
        }
        return str.toString();
    }

    /** Transform a bit set into a string of characters.
     * @separator The string to put in between elements
     * @param formatter An object implementing the CharFormatter interface.
     * @return A commma-separated list of character constants.
     */

/**    
    public String toString(String separator, CharFormatter formatter) {
        String str = "";

        for (int i = 0; i < (bits.length << LOG_BITS); i++) {
            if (member(i)) {
                if (str.length() > 0) {
                    str += separator;
                }
                str = str + formatter.literalChar(i);
            }
        }
        return str;
    }
*/
    
    /**Create a string representation where instead of integer elements, the
     * ith element of vocabulary is displayed instead.  Vocabulary is a Vector
     * of Strings.
     * @separator The string to put in between elements
     * @return A commma-separated list of character constants.
     */
    public String toString(String separator, FastList vocabulary) {
        if (vocabulary == null) {
            return toString(separator);
        }
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < (bits.length << LOG_BITS); i++) {
            if (member(i)) {
                if (str.length() > 0) {
                    str.append(separator);
                }
                if (i >= vocabulary.size()) {
// changed, we dont want rubish
                    str.append(i);
                }
                else {
                    Object o = vocabulary.get(i);
                    if ( o == null) {
// changed, we dont want rubish                    
                        str.append(i);
                    }
                    else {
                        str.append(o);
                    }
                }
            }
        }
        return str.toString();
    }

    /**
     * Dump a comma-separated list of the words making up the bit set.
     * Split each 64 bit number into two more manageable 32 bit numbers.
     * This generates a comma-separated list of C++-like unsigned long constants.
     */
    public String toStringOfHalfWords() {
        String s = new String();
        for (int i = 0; i < bits.length; i++) {
            if (i != 0) s += ", ";
            long tmp = bits[i];
            tmp &= 0xFFFFFFFFL;
            s += (tmp + "UL");
            s += ", ";
            tmp = bits[i] >>> 32;
            tmp &= 0xFFFFFFFFL;
            s += (tmp + "UL");
        }
        return s;
    }

    /**
     * Dump a comma-separated list of the words making up the bit set.
     * This generates a comma-separated list of Java-like long int constants.
     */
    public String toStringOfWords() {
        String s = new String();
        for (int i = 0; i < bits.length; i++) {
            if (i != 0) s += ", ";
            s += (bits[i] + "L");
        }
        return s;
    }

    /** Print out the bit set but collapse char ranges. */
    
/**    
    public String toStringWithRanges(String separator, CharFormatter formatter) {
        String str = "";
        int[] elems = this.toArray();
        if (elems.length == 0) {
            return "";
        }
        // look for ranges
        int i = 0;
        while (i < elems.length) {
            int lastInRange;
            lastInRange = 0;
            for (int j = i + 1; j < elems.length; j++) {
                if (elems[j] != elems[j - 1] + 1) {
                    break;
                }
                lastInRange = j;
            }
            // found a range
            if (str.length() > 0) {
                str += separator;
            }
            if (lastInRange - i >= 2) {
                str += formatter.literalChar(elems[i]);
                str += "..";
                str += formatter.literalChar(elems[lastInRange]);
                i = lastInRange;	// skip past end of range for next range
            }
            else {	// no range, just print current char and move on
                str += formatter.literalChar(elems[i]);
            }
            i++;
        }
        return str;
    }
*/
    private final static int wordNumber(int bit) {
        return bit >> LOG_BITS; // bit / BITS
    }
    
    // added function to decode a string
    public static BitSet fromString(String str, String separator, FastList vocabulary) {
        BitSet bs = new BitSet();
        FastList<String> seti = new FastList<String>(str.split(separator));
        for(int i=0, mx = seti.size(); i < mx; i++){
            String sc=seti.get(i);
            if(!sc.equals("")) {
                int vn=vocabulary.indexOf(sc);
                if(vn==-1) {
                    // not found, try the numeric representation
                    try {
                        vn=Integer.parseInt(sc);
                    } catch(Exception e) {
                        // not found throw again
                        throw new InternalError();
                    }
                }
                bs.add(vn);
            }
        }
        return bs;
    }

        /* ANTLR Translator Generator
     * Project led by Terence Parr at http://www.jGuru.com
     * Software rights: http://www.antlr.org/license.html
     *
     * $Id: //depot/code/org.antlr/release/antlr-2.7.5/antlr/collections/impl/IntRange.java#1 $
     */
    /*
    public static class IntRange {
        int begin, end;

        public IntRange(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        public String toString() {
            return begin + ".." + end;
        }
    }
     */
}
