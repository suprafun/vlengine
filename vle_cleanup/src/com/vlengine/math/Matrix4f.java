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

package com.vlengine.math;

import com.vlengine.system.VleException;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.geom.BufferUtils;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

/**
 * <code>Matrix4f</code> defines and maintains a 4x4 matrix in row major order.
 * This matrix is intended for use in a translation and rotational capacity. 
 * It provides convenience methods for creating the matrix from a multitude 
 * of sources.
 * 
 * Matrices are stored assuming column vectors on the right, with the translation
 * in the rightmost column. Element numbering is row,column, so m03 is the zeroth
 * row, third column, which is the "x" translation part. This means that the implicit
 * storage order is column major. However, the get() and set() functions on float
 * arrays default to row major order!
 *
 * @author Mark Powell
 * @author Joshua Slack (revamp and various methods)
 * @author Arpad Vekas updated for VL Engine
 */
public class Matrix4f {
    private static final Logger logger = Logger.getLogger(Matrix4f.class.getName());
    
    public float m00, m01, m02, m03;
    public float m10, m11, m12, m13;
    public float m20, m21, m22, m23;
    public float m30, m31, m32, m33;
    
    /**
     * Constructor instantiates a new <code>Matrix</code> that is set to the
     * identity matrix.
     *  
     */
    public Matrix4f() {
        loadIdentity();
    }

    /**
     * constructs a matrix with the given values.
     */
    public Matrix4f(float m00, float m01, float m02, float m03, 
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
    }

    /**
     * Create a new Matrix4f, given data in column-major format.
     *
     * @param array
	 *		An array of 16 floats in column-major format (translation in elements 12, 13 and 14).
     */
    public Matrix4f(float[] array) {
    	set(array, false);
    }

    /**
     * Constructor instantiates a new <code>Matrix</code> that is set to the
     * provided matrix. This constructor copies a given Matrix. If the provided
     * matrix is null, the constructor sets the matrix to the identity.
     * 
     * @param mat
     *            the matrix to copy.
     */
    public Matrix4f(Matrix4f mat) {
        set(mat);
    }

    /**
     * <code>get</code> retrieves the values of this object into
     * a float array in row-major order.
     * 
     * @param matrix
     *            the matrix to set the values into.
     */
    public void get(float[] matrix) {
        get(matrix, true);
    }

    /**
     * <code>set</code> retrieves the values of this object into
     * a float array.
     * 
     * @param matrix
     *            the matrix to set the values into.
     * @param rowMajor
     *            whether the outgoing data is in row or column major order.
     */
    public void get(float[] matrix, boolean rowMajor) {
        if (matrix.length != 16) throw new VleException(
                "Array must be of size 16.");

        if (rowMajor) {
            matrix[0] = m00;
            matrix[1] = m01;
            matrix[2] = m02;
            matrix[3] = m03;
            matrix[4] = m10;
            matrix[5] = m11;
            matrix[6] = m12;
            matrix[7] = m13;
            matrix[8] = m20;
            matrix[9] = m21;
            matrix[10] = m22;
            matrix[11] = m23;
            matrix[12] = m30;
            matrix[13] = m31;
            matrix[14] = m32;
            matrix[15] = m33;
        } else {
            matrix[0] = m00;
            matrix[4] = m01;
            matrix[8] = m02;
            matrix[12] = m03;
            matrix[1] = m10;
            matrix[5] = m11;
            matrix[9] = m12;
            matrix[13] = m13;
            matrix[2] = m20;
            matrix[6] = m21;
            matrix[10] = m22;
            matrix[14] = m23;
            matrix[3] = m30;
            matrix[7] = m31;
            matrix[11] = m32;
            matrix[15] = m33;
        }
    }

    /**
     * <code>get</code> retrieves a value from the matrix at the given
     * position. If the position is invalid a <code>JmeException</code> is
     * thrown.
     * 
     * @param i
     *            the row index.
     * @param j
     *            the colum index.
     * @return the value at (i, j).
     */
    public float get(int i, int j) {
        switch (i) {
        case 0:
            switch (j) {
            case 0: return m00;
            case 1: return m01;
            case 2: return m02;
            case 3: return m03;
            }
        case 1:
            switch (j) {
            case 0: return m10;
            case 1: return m11;
            case 2: return m12;
            case 3: return m13;
            }
        case 2:
            switch (j) {
            case 0: return m20;
            case 1: return m21;
            case 2: return m22;
            case 3: return m23;
            }
        case 3:
            switch (j) {
            case 0: return m30;
            case 1: return m31;
            case 2: return m32;
            case 3: return m33;
            }
        }

        logger.warning("Invalid matrix index.");
        throw new VleException("Invalid indices into matrix.");
    }

    /**
     * <code>getColumn</code> returns one of three columns specified by the
     * parameter. This column is returned as a float array of length 4.
     * 
     * @param i
     *            the column to retrieve. Must be between 0 and 3.
     * @return the column specified by the index.
     */
    public float[] getColumn(int i) {
        return getColumn(i, null);
    }

    /**
     * <code>getColumn</code> returns one of three columns specified by the
     * parameter. This column is returned as a float[4].
     * 
     * @param i
     *            the column to retrieve. Must be between 0 and 3.
     * @param store
     *            the float array to store the result in. if null, a new one
     *            is created.
     * @return the column specified by the index.
     */
    public float[] getColumn(int i, float[] store) {
        if (store == null) store = new float[4];
        switch (i) {
        case 0:
            store[0] = m00;
            store[1] = m10;
            store[2] = m20;
            store[3] = m30;
            break;
        case 1:
            store[0] = m01;
            store[1] = m11;
            store[2] = m21;
            store[3] = m31;
            break;
        case 2:
            store[0] = m02;
            store[1] = m12;
            store[2] = m22;
            store[3] = m32;
            break;
        case 3:
            store[0] = m03;
            store[1] = m13;
            store[2] = m23;
            store[3] = m33;
            break;
        default:
            logger.warning("Invalid column index.");
            throw new VleException("Invalid column index. " + i);
        }
        return store;
    }

    /**
     * 
     * <code>setColumn</code> sets a particular column of this matrix to that
     * represented by the provided vector.
     * 
     * @param i
     *            the column to set.
     * @param column
     *            the data to set.
     */
    public void setColumn(int i, float[] column) {

        if (column == null) {
            logger.warning("Column is null. Ignoring.");
            return;
        }
        switch (i) {
        case 0:
            m00 = column[0];
            m10 = column[1];
            m20 = column[2];
            m30 = column[3];
            break;
        case 1:
            m01 = column[0];
            m11 = column[1];
            m21 = column[2];
            m31 = column[3];
            break;
        case 2:
            m02 = column[0];
            m12 = column[1];
            m22 = column[2];
            m32 = column[3];
            break;
        case 3:
            m03 = column[0];
            m13 = column[1];
            m23 = column[2];
            m33 = column[3];
            break;
        default:
            logger.warning("Invalid column index.");
            throw new VleException("Invalid column index. " + i);
        }    }

    /**
     * <code>set</code> places a given value into the matrix at the given
     * position. If the position is invalid a <code>JmeException</code> is
     * thrown.
     * 
     * @param i
     *            the row index.
     * @param j
     *            the colum index.
     * @param value
     *            the value for (i, j).
     */
    public void set(int i, int j, float value) {
        switch (i) {
        case 0:
            switch (j) {
            case 0: m00 = value; return;
            case 1: m01 = value; return;
            case 2: m02 = value; return;
            case 3: m03 = value; return;
            }
        case 1:
            switch (j) {
            case 0: m10 = value; return;
            case 1: m11 = value; return;
            case 2: m12 = value; return;
            case 3: m13 = value; return;
            }
        case 2:
            switch (j) {
            case 0: m20 = value; return;
            case 1: m21 = value; return;
            case 2: m22 = value; return;
            case 3: m23 = value; return;
            }
        case 3:
            switch (j) {
            case 0: m30 = value; return;
            case 1: m31 = value; return;
            case 2: m32 = value; return;
            case 3: m33 = value; return;
            }
        }

        logger.warning("Invalid matrix index.");
        throw new VleException("Invalid indices into matrix.");
    }

    /**
     * <code>set</code> sets the values of this matrix from an array of
     * values.
     * 
     * @param matrix
     *            the matrix to set the value to.
     * @throws JmeException
     *             if the array is not of size 16.
     */
    public void set(float[][] matrix) {
        if (matrix.length != 4 || matrix[0].length != 4) { throw new VleException(
                "Array must be of size 16."); }

        m00 = matrix[0][0];
        m01 = matrix[0][1];
        m02 = matrix[0][2];
        m03 = matrix[0][3];
        m10 = matrix[1][0];
        m11 = matrix[1][1];
        m12 = matrix[1][2];
        m13 = matrix[1][3];
        m20 = matrix[2][0];
        m21 = matrix[2][1];
        m22 = matrix[2][2];
        m23 = matrix[2][3];
        m30 = matrix[3][0];
        m31 = matrix[3][1];
        m32 = matrix[3][2];
        m33 = matrix[3][3];
    }

    /**
     * <code>set</code> sets the values of this matrix from another matrix.
     *
     * @param matrix
     *            the matrix to read the value from.
     */
    public Matrix4f set(Matrix4f matrix) {
        m00 = matrix.m00; m01 = matrix.m01; m02 = matrix.m02; m03 = matrix.m03;
        m10 = matrix.m10; m11 = matrix.m11; m12 = matrix.m12; m13 = matrix.m13;
        m20 = matrix.m20; m21 = matrix.m21; m22 = matrix.m22; m23 = matrix.m23;
        m30 = matrix.m30; m31 = matrix.m31; m32 = matrix.m32; m33 = matrix.m33;
        return this;
    }

    /**
     * <code>set</code> sets the values of this matrix from an array of
     * values assuming that the data is rowMajor order;
     * 
     * @param matrix
     *            the matrix to set the value to.
     */
    public void set(float[] matrix) {
        set(matrix, true);
    }

    /**
     * <code>set</code> sets the values of this matrix from an array of
     * values;
     * 
     * @param matrix
     *            the matrix to set the value to.
     * @param rowMajor
     *            whether the incoming data is in row or column major order.
     */
    public void set(float[] matrix, boolean rowMajor) {
        if (matrix.length != 16) throw new VleException(
                "Array must be of size 16.");

        if (rowMajor) {
            m00 = matrix[0];
            m01 = matrix[1];
            m02 = matrix[2];
            m03 = matrix[3];
            m10 = matrix[4];
            m11 = matrix[5];
            m12 = matrix[6];
            m13 = matrix[7];
            m20 = matrix[8];
            m21 = matrix[9];
            m22 = matrix[10];
            m23 = matrix[11];
            m30 = matrix[12];
            m31 = matrix[13];
            m32 = matrix[14];
            m33 = matrix[15];
        } else {
            m00 = matrix[0];
            m01 = matrix[4];
            m02 = matrix[8];
            m03 = matrix[12];
            m10 = matrix[1];
            m11 = matrix[5];
            m12 = matrix[9];
            m13 = matrix[13];
            m20 = matrix[2];
            m21 = matrix[6];
            m22 = matrix[10];
            m23 = matrix[14];
            m30 = matrix[3];
            m31 = matrix[7];
            m32 = matrix[11];
            m33 = matrix[15];
        }
    }

    public Matrix4f transpose() {
        float[] tmp = new float[16];
        get(tmp, true);
        Matrix4f mat = new Matrix4f(tmp);
    	return mat;
    }

    /**
     * <code>transpose</code> locally transposes this Matrix.
     * 
     * @return this object for chaining.
     */
    public Matrix4f transposeLocal() {
        float[] tmp = LocalContext.getContext().tmpMatrix4fArr16;
        get(tmp, true);
        set(tmp, false);
        return this;
    }
    
    
    /**
     * <code>toFloatBuffer</code> returns a FloatBuffer object that contains
     * the matrix data.
     * 
     * @return matrix data as a FloatBuffer.
     */
    public FloatBuffer toFloatBuffer() {
    	return toFloatBuffer(false);
    }
    
    /**
     * <code>toFloatBuffer</code> returns a FloatBuffer object that contains
     * the matrix data.
     * 
     * @param columnMajor if true, this buffer should be filled with column
     * 		major data, otherwise it will be filled row major.
     * @return matrix data as a FloatBuffer.
     */
    public FloatBuffer toFloatBuffer(boolean columnMajor) {
    	FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        
    	if(columnMajor) {
                fb.put(m00).put(m10).put(m20).put(m30);
	        fb.put(m01).put(m11).put(m21).put(m31);
	        fb.put(m02).put(m12).put(m22).put(m32);
	        fb.put(m03).put(m13).put(m23).put(m33);
	    } else {
	        fb.put(m00).put(m01).put(m02).put(m03);
	        fb.put(m10).put(m11).put(m12).put(m13);
	        fb.put(m20).put(m21).put(m22).put(m23);
	        fb.put(m30).put(m31).put(m32).put(m33);
	    }
        
    	fb.rewind();
    	return fb;
    }
       
    /**
     * <code>fillFloatBuffer</code> fills a FloatBuffer object with
     * the matrix data.
     * @param fb the buffer to fill, must be correct size
     * @return matrix data as a FloatBuffer.
     */
    public FloatBuffer fillFloatBuffer(FloatBuffer fb) {
    	return fillFloatBuffer(fb, false);
    }

    /**
     * <code>fillFloatBuffer</code> fills a FloatBuffer object with
     * the matrix data.
     * @param fb the buffer to fill, must be correct size
     * @param columnMajor if true, this buffer should be filled with column
     * 		major data, otherwise it will be filled row major.
     * @return matrix data as a FloatBuffer.
     */
    public FloatBuffer fillFloatBuffer(FloatBuffer fb, boolean columnMajor) {
        fb.clear();
        if(columnMajor) {
            putFloatBufferColumnMajor(fb);
        } else {
            putFloatBufferRowMajor(fb);
        }
        fb.rewind();
        return fb;
    }

    public FloatBuffer putFloatBufferColumnMajor(FloatBuffer fb) {
        fb.put(m00).put(m10).put(m20).put(m30);
        fb.put(m01).put(m11).put(m21).put(m31);
        fb.put(m02).put(m12).put(m22).put(m32);
        fb.put(m03).put(m13).put(m23).put(m33);
        return fb;
    }

    public FloatBuffer putFloatBufferRowMajor(FloatBuffer fb) {
        fb.put(m00).put(m01).put(m02).put(m03);
        fb.put(m10).put(m11).put(m12).put(m13);
        fb.put(m20).put(m21).put(m22).put(m23);
        fb.put(m30).put(m31).put(m32).put(m33);
        return fb;
    }

    /**
     * <code>readFloatBuffer</code> reads value for this matrix from a FloatBuffer.
     * @param fb the buffer to read from, must be correct size
     * @return this data as a FloatBuffer.
     */
    public Matrix4f readFloatBuffer(FloatBuffer fb) {
    	return readFloatBuffer(fb, false);
    }

    /**
     * <code>readFloatBuffer</code> reads value for this matrix from a FloatBuffer.
     * @param fb the buffer to read from, must be correct size
     * @param columnMajor if true, this buffer should be filled with column
     * 		major data, otherwise it will be filled row major.
     * @return this data as a FloatBuffer.
     */
    public Matrix4f readFloatBuffer(FloatBuffer fb, boolean columnMajor) {
    	
    	if(columnMajor) {
    		m00 = fb.get(); m10 = fb.get(); m20 = fb.get(); m30 = fb.get();
    		m01 = fb.get(); m11 = fb.get(); m21 = fb.get(); m31 = fb.get();
    		m02 = fb.get(); m12 = fb.get(); m22 = fb.get(); m32 = fb.get();
    		m03 = fb.get(); m13 = fb.get(); m23 = fb.get(); m33 = fb.get();
    	} else {
    		m00 = fb.get(); m01 = fb.get(); m02 = fb.get(); m03 = fb.get();
    		m10 = fb.get(); m11 = fb.get(); m12 = fb.get(); m13 = fb.get();
    		m20 = fb.get(); m21 = fb.get(); m22 = fb.get(); m23 = fb.get();
    		m30 = fb.get(); m31 = fb.get(); m32 = fb.get(); m33 = fb.get();
    	}
        return this;
    }

    /**
     * <code>loadIdentity</code> sets this matrix to the identity matrix,
     * namely all zeros with ones along the diagonal.
     *  
     */
    public void loadIdentity() {
        zero();
        m00 = m11 = m22 = m33 = 1;
    }

    /**
     * <code>fromAngleAxis</code> sets this matrix4f to the values specified
     * by an angle and an axis of rotation.  This method creates an object, so
     * use fromAngleNormalAxis if your axis is already normalized.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation.
     */
    public void fromAngleAxis(float angle, Vector3f axis) {
        Vector3f normAxis = axis.normalize();
        fromAngleNormalAxis(angle, normAxis);
    }

    /**
     * <code>fromAngleNormalAxis</code> sets this matrix4f to the values
     * specified by an angle and a normalized axis of rotation.
     * 
     * @param angle
     *            the angle to rotate (in radians).
     * @param axis
     *            the axis of rotation (already normalized).
     */
    public void fromAngleNormalAxis(float angle, Vector3f axis) {
        zero();
        m33 = 1;

        float fCos = FastMath.cos(angle);
        float fSin = FastMath.sin(angle);
        float fOneMinusCos = ((float)1.0)-fCos;
        float fX2 = axis.x*axis.x;
        float fY2 = axis.y*axis.y;
        float fZ2 = axis.z*axis.z;
        float fXYM = axis.x*axis.y*fOneMinusCos;
        float fXZM = axis.x*axis.z*fOneMinusCos;
        float fYZM = axis.y*axis.z*fOneMinusCos;
        float fXSin = axis.x*fSin;
        float fYSin = axis.y*fSin;
        float fZSin = axis.z*fSin;
        
        m00 = fX2*fOneMinusCos+fCos;
        m01 = fXYM-fZSin;
        m02 = fXZM+fYSin;
        m10 = fXYM+fZSin;
        m11 = fY2*fOneMinusCos+fCos;
        m12 = fYZM-fXSin;
        m20 = fXZM-fYSin;
        m21 = fYZM+fXSin;
        m22 = fZ2*fOneMinusCos+fCos;
    }

    /**
     * <code>mult</code> multiplies this matrix by a scalar.
     * 
     * @param scalar
     *            the scalar to multiply this matrix by.
     */
    public void multLocal(float scalar) {
        m00 *= scalar;
        m01 *= scalar;
        m02 *= scalar;
        m03 *= scalar;
        m10 *= scalar;
        m11 *= scalar;
        m12 *= scalar;
        m13 *= scalar;
        m20 *= scalar;
        m21 *= scalar;
        m22 *= scalar;
        m23 *= scalar;
        m30 *= scalar;
        m31 *= scalar;
        m32 *= scalar;
        m33 *= scalar;
    }
    
    public Matrix4f mult(float scalar) {
    	Matrix4f out = new Matrix4f();
    	out.set(this);
    	out.multLocal(scalar);
    	return out;
    }
    
    public Matrix4f mult(float scalar, Matrix4f store) {
    	store.set(this);
    	store.multLocal(scalar);
    	return store;
    }

    /**
     * <code>mult</code> multiplies this matrix with another matrix. The
     * result matrix will then be returned. This matrix will be on the left hand
     * side, while the parameter matrix will be on the right.
     * 
     * @param in2
     *            the matrix to multiply this matrix by.
     * @return the resultant matrix
     */
    public Matrix4f mult(Matrix4f in2) {
        return mult(in2, null);
    }

    /**
     * <code>mult</code> multiplies this matrix with another matrix. The
     * result matrix will then be returned. This matrix will be on the left hand
     * side, while the parameter matrix will be on the right.
     * 
     * @param in2
     *            the matrix to multiply this matrix by.
     * @param store
     *            where to store the result. It is safe for in2 and store to be
     *            the same object.
     * @return the resultant matrix
     */
    public Matrix4f mult(Matrix4f in2, Matrix4f store) {
        if (store == null) store = new Matrix4f();

        float temp00, temp01, temp02, temp03;
        float temp10, temp11, temp12, temp13;
        float temp20, temp21, temp22, temp23;
        float temp30, temp31, temp32, temp33;

        temp00 = m00 * in2.m00 + 
                m01 * in2.m10 + 
                m02 * in2.m20 + 
                m03 * in2.m30;
        temp01 = m00 * in2.m01 + 
                m01 * in2.m11 + 
                m02 * in2.m21 +
                m03 * in2.m31;
        temp02 = m00 * in2.m02 + 
                m01 * in2.m12 + 
                m02 * in2.m22 +
                m03 * in2.m32;
        temp03 = m00 * in2.m03 + 
                m01 * in2.m13 + 
                m02 * in2.m23 + 
                m03 * in2.m33;
        
        temp10 = m10 * in2.m00 + 
                m11 * in2.m10 + 
                m12 * in2.m20 +
                m13 * in2.m30;
        temp11 = m10 * in2.m01 +
                m11 * in2.m11 +
                m12 * in2.m21 +
                m13 * in2.m31;
        temp12 = m10 * in2.m02 +
                m11 * in2.m12 + 
                m12 * in2.m22 +
                m13 * in2.m32;
        temp13 = m10 * in2.m03 +
                m11 * in2.m13 +
                m12 * in2.m23 + 
                m13 * in2.m33;

        temp20 = m20 * in2.m00 + 
                m21 * in2.m10 + 
                m22 * in2.m20 +
                m23 * in2.m30;
        temp21 = m20 * in2.m01 + 
                m21 * in2.m11 + 
                m22 * in2.m21 +
                m23 * in2.m31;
        temp22 = m20 * in2.m02 + 
                m21 * in2.m12 + 
                m22 * in2.m22 +
                m23 * in2.m32;
        temp23 = m20 * in2.m03 + 
                m21 * in2.m13 + 
                m22 * in2.m23 +
                m23 * in2.m33;

        temp30 = m30 * in2.m00 + 
                m31 * in2.m10 + 
                m32 * in2.m20 +
                m33 * in2.m30;
        temp31 = m30 * in2.m01 + 
                m31 * in2.m11 + 
                m32 * in2.m21 +
                m33 * in2.m31;
        temp32 = m30 * in2.m02 + 
                m31 * in2.m12 + 
                m32 * in2.m22 +
                m33 * in2.m32;
        temp33 = m30 * in2.m03 + 
                m31 * in2.m13 + 
                m32 * in2.m23 +
                m33 * in2.m33;
        
        store.m00 = temp00;  store.m01 = temp01;  store.m02 = temp02;  store.m03 = temp03;
        store.m10 = temp10;  store.m11 = temp11;  store.m12 = temp12;  store.m13 = temp13;
        store.m20 = temp20;  store.m21 = temp21;  store.m22 = temp22;  store.m23 = temp23;
        store.m30 = temp30;  store.m31 = temp31;  store.m32 = temp32;  store.m33 = temp33;
        
        return store;
    }

    /**
     * <code>mult</code> multiplies this matrix with another matrix. The
     * results are stored internally and a handle to this matrix will 
     * then be returned. This matrix will be on the left hand
     * side, while the parameter matrix will be on the right.
     * 
     * @param in2
     *            the matrix to multiply this matrix by.
     * @return the resultant matrix
     */
    public Matrix4f multLocal(Matrix4f in2) {
        
        return mult(in2, this);
    }

    /**
     * <code>mult</code> multiplies a vector about a rotation matrix. The
     * resulting vector is returned as a new Vector3f.
     * 
     * @param vec
     *            vec to multiply against.
     * @return the rotated vector.
     */
    public Vector3f mult(Vector3f vec) {
        return mult(vec, null);
    }

    /**
     * <code>mult</code> multiplies a vector with this transformation matrix
     * 
     * @param vec
     *            vec to multiply against.
     * @param store
     *            a vector to store the result in. Created if null is passed.
     * @return the rotated vector.
     */
    public Vector3f mult(Vector3f vec, Vector3f store) {
        if (store == null) store = new Vector3f();
        
        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = m00 * vx + m01 * vy + m02 * vz + m03;
        store.y = m10 * vx + m11 * vy + m12 * vz + m13;
        store.z = m20 * vx + m21 * vy + m22 * vz + m23;

        return store;
    }

    @Deprecated
    public Vector4f mult(Vector4f vec, Vector4f store) {
        if (store == null) store = new Vector4f();
        
        float vx = vec.x, vy = vec.y, vz = vec.z, vw = vec.w;
        store.x = m00 * vx + m01 * vy + m02 * vz + m03 * vw;
        store.y = m10 * vx + m11 * vy + m12 * vz + m13 * vw;
        store.z = m20 * vx + m21 * vy + m22 * vz + m23 * vw;
        store.w = m30 * vx + m31 * vy + m32 * vz + m33 * vw;

        return store;
    }
    
    /**
     * <code>mult</code> multiplies a vector with this matrix
     * 
     * @param vec
     *            vec to multiply against.
     * @param store
     *            a vector to store the result in.  created if null is passed.
     * @return the rotated vector.
     */
    public Vector3f multAcross(Vector3f vec, Vector3f store) {
        if (null == vec) {
            logger.info("Source vector is null, null result returned.");
            return null;
        }
        if (store == null) store = new Vector3f();
        
        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = m00 * vx + m10 * vy + m20 * vz + m30 * 1;
        store.y = m01 * vx + m11 * vy + m21 * vz + m31 * 1;
        store.z = m02 * vx + m12 * vy + m22 * vz + m32 * 1;

        return store;
    }

    /**
     * <code>mult</code> multiplies a quaternion about a matrix. The
     * resulting vector is returned.
     *
     * @param vec
     *            vec to multiply against.
     * @param store
     *            a quaternion to store the result in.  created if null is passed.
     * @return store = this * vec
     */
    public Quaternion mult(Quaternion vec, Quaternion store) {

        if (null == vec) {
            logger.warning("Source vector is null, null result returned.");
            return null;
        }
        if (store == null) store = new Quaternion();

        float x = m00 * vec.x + m10 * vec.y + m20 * vec.z + m30 * vec.w;
        float y = m01 * vec.x + m11 * vec.y + m21 * vec.z + m31 * vec.w;
        float z = m02 * vec.x + m12 * vec.y + m22 * vec.z + m32 * vec.w;
        float w = m03 * vec.x + m13 * vec.y + m23 * vec.z + m33 * vec.w;
        store.x = x;
        store.y = y;
        store.z = z;
        store.w = w;

        return store;
    }
    
    /**
     * <code>mult</code> multiplies an array of 4 floats against this rotation 
     * matrix. The results are stored directly in the array. (vec4f x mat4f)
     * 
     * @param vec4f
     *            float array (size 4) to multiply against the matrix.
     * @return the vec4f for chaining.
     */
    public float[] mult(float[] vec4f) {
        if (null == vec4f || vec4f.length != 4) {
            logger.warning("invalid array given, must be nonnull and length 4");
            return null;
        }

        float x = vec4f[0], y = vec4f[1], z = vec4f[2], w = vec4f[3];
        
        vec4f[0] = m00 * x + m01 * y + m02 * z + m03 * w;
        vec4f[1] = m10 * x + m11 * y + m12 * z + m13 * w;
        vec4f[2] = m20 * x + m21 * y + m22 * z + m23 * w;
        vec4f[3] = m30 * x + m31 * y + m32 * z + m33 * w;

        return vec4f;
    }

    /**
     * <code>mult</code> multiplies an array of 4 floats against this rotation 
     * matrix. The results are stored directly in the array. (vec4f x mat4f)
     * 
     * @param vec4f
     *            float array (size 4) to multiply against the matrix.
     * @return the vec4f for chaining.
     */
    public float[] multAcross(float[] vec4f) {
        if (null == vec4f || vec4f.length != 4) {
            logger.warning("invalid array given, must be nonnull and length 4");
            return null;
        }

        float x = vec4f[0], y = vec4f[1], z = vec4f[2], w = vec4f[3];
        
        vec4f[0] = m00 * x + m10 * y + m20 * z + m30 * w;
        vec4f[1] = m01 * x + m11 * y + m21 * z + m31 * w;
        vec4f[2] = m02 * x + m12 * y + m22 * z + m32 * w;
        vec4f[3] = m03 * x + m13 * y + m23 * z + m33 * w;

        return vec4f;
    }

    /**
     * Inverts this matrix as a new Matrix4f.
     * 
     * @return The new inverse matrix
     */
    public Matrix4f invert() {
        return invert(null);
    }

    /**
     * Inverts this matrix and stores it in the given store.
     * 
     * @return The store
     */
    public Matrix4f invert(Matrix4f store) {
        if (store == null) store = new Matrix4f();

        float fA0 = m00*m11 - m01*m10;
        float fA1 = m00*m12 - m02*m10;
        float fA2 = m00*m13 - m03*m10;
        float fA3 = m01*m12 - m02*m11;
        float fA4 = m01*m13 - m03*m11;
        float fA5 = m02*m13 - m03*m12;
        float fB0 = m20*m31 - m21*m30;
        float fB1 = m20*m32 - m22*m30;
        float fB2 = m20*m33 - m23*m30;
        float fB3 = m21*m32 - m22*m31;
        float fB4 = m21*m33 - m23*m31;
        float fB5 = m22*m33 - m23*m32;
        float fDet = fA0*fB5-fA1*fB4+fA2*fB3+fA3*fB2-fA4*fB1+fA5*fB0;

        if ( FastMath.abs(fDet) <= FastMath.FLT_EPSILON )
            throw new ArithmeticException("This matrix cannot be inverted");

        store.m00 = + m11*fB5 - m12*fB4 + m13*fB3;
        store.m10 = - m10*fB5 + m12*fB2 - m13*fB1;
        store.m20 = + m10*fB4 - m11*fB2 + m13*fB0;
        store.m30 = - m10*fB3 + m11*fB1 - m12*fB0;
        store.m01 = - m01*fB5 + m02*fB4 - m03*fB3;
        store.m11 = + m00*fB5 - m02*fB2 + m03*fB1;
        store.m21 = - m00*fB4 + m01*fB2 - m03*fB0;
        store.m31 = + m00*fB3 - m01*fB1 + m02*fB0;
        store.m02 = + m31*fA5 - m32*fA4 + m33*fA3;
        store.m12 = - m30*fA5 + m32*fA2 - m33*fA1;
        store.m22 = + m30*fA4 - m31*fA2 + m33*fA0;
        store.m32 = - m30*fA3 + m31*fA1 - m32*fA0;
        store.m03 = - m21*fA5 + m22*fA4 - m23*fA3;
        store.m13 = + m20*fA5 - m22*fA2 + m23*fA1;
        store.m23 = - m20*fA4 + m21*fA2 - m23*fA0;
        store.m33 = + m20*fA3 - m21*fA1 + m22*fA0;

        float fInvDet = 1.0f/fDet;
        store.multLocal(fInvDet);

        return store;
    }

    /**
     * Inverts this matrix locally.
     * 
     * @return this
     */
    public Matrix4f invertLocal() {

        float fA0 = m00*m11 - m01*m10;
        float fA1 = m00*m12 - m02*m10;
        float fA2 = m00*m13 - m03*m10;
        float fA3 = m01*m12 - m02*m11;
        float fA4 = m01*m13 - m03*m11;
        float fA5 = m02*m13 - m03*m12;
        float fB0 = m20*m31 - m21*m30;
        float fB1 = m20*m32 - m22*m30;
        float fB2 = m20*m33 - m23*m30;
        float fB3 = m21*m32 - m22*m31;
        float fB4 = m21*m33 - m23*m31;
        float fB5 = m22*m33 - m23*m32;
        float fDet = fA0*fB5-fA1*fB4+fA2*fB3+fA3*fB2-fA4*fB1+fA5*fB0;

        if ( FastMath.abs(fDet) <= FastMath.FLT_EPSILON )
            return zero();

        float f00 = + m11*fB5 - m12*fB4 + m13*fB3;
        float f10 = - m10*fB5 + m12*fB2 - m13*fB1;
        float f20 = + m10*fB4 - m11*fB2 + m13*fB0;
        float f30 = - m10*fB3 + m11*fB1 - m12*fB0;
        float f01 = - m01*fB5 + m02*fB4 - m03*fB3;
        float f11 = + m00*fB5 - m02*fB2 + m03*fB1;
        float f21 = - m00*fB4 + m01*fB2 - m03*fB0;
        float f31 = + m00*fB3 - m01*fB1 + m02*fB0;
        float f02 = + m31*fA5 - m32*fA4 + m33*fA3;
        float f12 = - m30*fA5 + m32*fA2 - m33*fA1;
        float f22 = + m30*fA4 - m31*fA2 + m33*fA0;
        float f32 = - m30*fA3 + m31*fA1 - m32*fA0;
        float f03 = - m21*fA5 + m22*fA4 - m23*fA3;
        float f13 = + m20*fA5 - m22*fA2 + m23*fA1;
        float f23 = - m20*fA4 + m21*fA2 - m23*fA0;
        float f33 = + m20*fA3 - m21*fA1 + m22*fA0;
        
        m00 = f00;
        m01 = f01;
        m02 = f02;
        m03 = f03;
        m10 = f10;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m20 = f20;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m30 = f30;
        m31 = f31;
        m32 = f32;
        m33 = f33;

        float fInvDet = 1.0f/fDet;
        multLocal(fInvDet);

        return this;
    }
    
    /**
     * Returns a new matrix representing the adjoint of this matrix.
     * 
     * @return The adjoint matrix
     */
    public Matrix4f adjoint() {
        return adjoint(null);
    }
     
    
    /**
     * Places the adjoint of this matrix in store (creates store if null.)
     * 
     * @param store
     *            The matrix to store the result in.  If null, a new matrix is created.
     * @return store
     */
    public Matrix4f adjoint(Matrix4f store) {
        if (store == null) store = new Matrix4f();

        float fA0 = m00*m11 - m01*m10;
        float fA1 = m00*m12 - m02*m10;
        float fA2 = m00*m13 - m03*m10;
        float fA3 = m01*m12 - m02*m11;
        float fA4 = m01*m13 - m03*m11;
        float fA5 = m02*m13 - m03*m12;
        float fB0 = m20*m31 - m21*m30;
        float fB1 = m20*m32 - m22*m30;
        float fB2 = m20*m33 - m23*m30;
        float fB3 = m21*m32 - m22*m31;
        float fB4 = m21*m33 - m23*m31;
        float fB5 = m22*m33 - m23*m32;

        store.m00 = + m11*fB5 - m12*fB4 + m13*fB3;
        store.m10 = - m10*fB5 + m12*fB2 - m13*fB1;
        store.m20 = + m10*fB4 - m11*fB2 + m13*fB0;
        store.m30 = - m10*fB3 + m11*fB1 - m12*fB0;
        store.m01 = - m01*fB5 + m02*fB4 - m03*fB3;
        store.m11 = + m00*fB5 - m02*fB2 + m03*fB1;
        store.m21 = - m00*fB4 + m01*fB2 - m03*fB0;
        store.m31 = + m00*fB3 - m01*fB1 + m02*fB0;
        store.m02 = + m31*fA5 - m32*fA4 + m33*fA3;
        store.m12 = - m30*fA5 + m32*fA2 - m33*fA1;
        store.m22 = + m30*fA4 - m31*fA2 + m33*fA0;
        store.m32 = - m30*fA3 + m31*fA1 - m32*fA0;
        store.m03 = - m21*fA5 + m22*fA4 - m23*fA3;
        store.m13 = + m20*fA5 - m22*fA2 + m23*fA1;
        store.m23 = - m20*fA4 + m21*fA2 - m23*fA0;
        store.m33 = + m20*fA3 - m21*fA1 + m22*fA0;

        return store;
    }

    /**
     * <code>determinant</code> generates the determinate of this matrix.
     * 
     * @return the determinate
     */
    public float determinant() {
        float fA0 = m00*m11 - m01*m10;
        float fA1 = m00*m12 - m02*m10;
        float fA2 = m00*m13 - m03*m10;
        float fA3 = m01*m12 - m02*m11;
        float fA4 = m01*m13 - m03*m11;
        float fA5 = m02*m13 - m03*m12;
        float fB0 = m20*m31 - m21*m30;
        float fB1 = m20*m32 - m22*m30;
        float fB2 = m20*m33 - m23*m30;
        float fB3 = m21*m32 - m22*m31;
        float fB4 = m21*m33 - m23*m31;
        float fB5 = m22*m33 - m23*m32;
        float fDet = fA0*fB5-fA1*fB4+fA2*fB3+fA3*fB2-fA4*fB1+fA5*fB0;
        return fDet;
    }

    /**
     * Sets all of the values in this matrix to zero.
     * 
     * @return this matrix
     */
    public Matrix4f zero() {
        m00 = m01 = m02 = m03 = 0.0f;
        m10 = m11 = m12 = m13 = 0.0f;
        m20 = m21 = m22 = m23 = 0.0f;
        m30 = m31 = m32 = m33 = 0.0f;
        return this;
    }
    
    public Matrix4f add(Matrix4f mat) {
    	Matrix4f result = new Matrix4f();
    	result.m00 = this.m00 + mat.m00;
    	result.m01 = this.m01 + mat.m01;
    	result.m02 = this.m02 + mat.m02;
    	result.m03 = this.m03 + mat.m03;
    	result.m10 = this.m10 + mat.m10;
    	result.m11 = this.m11 + mat.m11;
    	result.m12 = this.m12 + mat.m12;
    	result.m13 = this.m13 + mat.m13;
    	result.m20 = this.m20 + mat.m20;
    	result.m21 = this.m21 + mat.m21;
    	result.m22 = this.m22 + mat.m22;
    	result.m23 = this.m23 + mat.m23;
    	result.m30 = this.m30 + mat.m30;
    	result.m31 = this.m31 + mat.m31;
    	result.m32 = this.m32 + mat.m32;
    	result.m33 = this.m33 + mat.m33;
    	return result;
    }

    /**
     * <code>add</code> adds the values of a parameter matrix to this matrix.
     * 
     * @param mat
     *            the matrix to add to this.
     */
    public void addLocal(Matrix4f mat) {
        m00 += mat.m00;
        m01 += mat.m01;
        m02 += mat.m02;
        m03 += mat.m03;
        m10 += mat.m10;
        m11 += mat.m11;
        m12 += mat.m12;
        m13 += mat.m13;
        m20 += mat.m20;
        m21 += mat.m21;
        m22 += mat.m22;
        m23 += mat.m23;
        m30 += mat.m30;
        m31 += mat.m31;
        m32 += mat.m32;
        m33 += mat.m33;
    }
    
    public Vector3f toTranslationVector() {
        return new Vector3f(m03, m13, m23);
    }
    
    public void toTranslationVector(Vector3f vector) {
        vector.set(m03, m13, m23);
    }
    
    public Quaternion toRotationQuat() {
        Quaternion quat = new Quaternion();
        quat.fromRotationMatrix(toRotationMatrix());
        return quat;
    }
    
    public void toRotationQuat(Quaternion q) {
        q.fromRotationMatrix(toRotationMatrix());
    }
    
    public Matrix3f toRotationMatrix() {
        return new Matrix3f(m00, m01, m02, m10, m11, m12, m20, m21, m22);
        
    }
    
    public void toRotationMatrix(Matrix3f mat) {
        mat.m00 = m00;
        mat.m01 = m01;
        mat.m02 = m02;
        mat.m10 = m10;
        mat.m11 = m11;
        mat.m12 = m12;
        mat.m20 = m20;
        mat.m21 = m21;
        mat.m22 = m22;
        
    }

    /**
     * <code>setTranslation</code> will set the matrix's translation values.
     * 
     * @param translation
     *            the new values for the translation.
     * @throws JmeException
     *             if translation is not size 3.
     */
    public void setTranslation(float[] translation) {
        if (translation.length != 3) { throw new VleException(
                "Translation size must be 3."); }
        m03 = translation[0];
        m13 = translation[1];
        m23 = translation[2];
    }

    /**
     * <code>setTranslation</code> will set the matrix's translation values.
     * 
     * @param x
     *            value of the translation on the x axis
     * @param y
     *            value of the translation on the y axis
     * @param z
     *            value of the translation on the z axis
     */
    public void setTranslation(float x, float y, float z) {
        m03 = x;
        m13 = y;
        m23 = z;
    }

    /**
     * <code>setTranslation</code> will set the matrix's translation values.
     *
     * @param translation
     *            the new values for the translation.
     */
    public void setTranslation(Vector3f translation) {
        m03 = translation.x;
        m13 = translation.y;
        m23 = translation.z;
    }

    /**
     * <code>setInverseTranslation</code> will set the matrix's inverse
     * translation values.
     * 
     * @param translation
     *            the new values for the inverse translation.
     * @throws JmeException
     *             if translation is not size 3.
     */
    public void setInverseTranslation(float[] translation) {
        if (translation.length != 3) { throw new VleException(
                "Translation size must be 3."); }
        m03 = -translation[0];
        m13 = -translation[1];
        m23 = -translation[2];
    }

    /**
     * <code>angleRotation</code> sets this matrix to that of a rotation about
     * three axes (x, y, z). Where each axis has a specified rotation in
     * degrees. These rotations are expressed in a single <code>Vector3f</code>
     * object.
     * 
     * @param angles
     *            the angles to rotate.
     */
    public void angleRotation(Vector3f angles) {
        float angle;
        float sr, sp, sy, cr, cp, cy;

        angle = (angles.z * FastMath.DEG_TO_RAD);
        sy = FastMath.sin(angle);
        cy = FastMath.cos(angle);
        angle = (angles.y * FastMath.DEG_TO_RAD);
        sp = FastMath.sin(angle);
        cp = FastMath.cos(angle);
        angle = (angles.x * FastMath.DEG_TO_RAD);
        sr = FastMath.sin(angle);
        cr = FastMath.cos(angle);

        // matrix = (Z * Y) * X
        m00 = cp * cy;
        m10 = cp * sy;
        m20 = -sp;
        m01 = sr * sp * cy + cr * -sy;
        m11 = sr * sp * sy + cr * cy;
        m21 = sr * cp;
        m02 = (cr * sp * cy + -sr * -sy);
        m12 = (cr * sp * sy + -sr * cy);
        m22 = cr * cp;
        m03 = 0.0f;
        m13 = 0.0f;
        m23 = 0.0f;
    }

    /**
     * <code>setRotationQuaternion</code> builds a rotation from a
     * <code>Quaternion</code>.
     * 
     * @param quat
     *            the quaternion to build the rotation from.
     * @throws NullPointerException
     *             if quat is null.
     */
    public void setRotationQuaternion(Quaternion quat) {
        quat.toRotationMatrix(this);
    }

    /**
     * <code>setInverseRotationRadians</code> builds an inverted rotation from
     * Euler angles that are in radians.
     * 
     * @param angles
     *            the Euler angles in radians.
     * @throws JmeException
     *             if angles is not size 3.
     */
    public void setInverseRotationRadians(float[] angles) {
        if (angles.length != 3) { throw new VleException(
                "Angles must be of size 3."); }
        double cr = FastMath.cos(angles[0]);
        double sr = FastMath.sin(angles[0]);
        double cp = FastMath.cos(angles[1]);
        double sp = FastMath.sin(angles[1]);
        double cy = FastMath.cos(angles[2]);
        double sy = FastMath.sin(angles[2]);

        m00 = (float) (cp * cy);
        m10 = (float) (cp * sy);
        m20 = (float) (-sp);

        double srsp = sr * sp;
        double crsp = cr * sp;

        m01 = (float) (srsp * cy - cr * sy);
        m11 = (float) (srsp * sy + cr * cy);
        m21 = (float) (sr * cp);

        m02 = (float) (crsp * cy + sr * sy);
        m12 = (float) (crsp * sy - sr * cy);
        m22 = (float) (cr * cp);
    }

    /**
     * <code>setInverseRotationDegrees</code> builds an inverted rotation from
     * Euler angles that are in degrees.
     * 
     * @param angles
     *            the Euler angles in degrees.
     * @throws JmeException
     *             if angles is not size 3.
     */
    public void setInverseRotationDegrees(float[] angles) {
        if (angles.length != 3) { throw new VleException(
                "Angles must be of size 3."); }
        float vec[] = new float[3];
        vec[0] = (angles[0] * FastMath.RAD_TO_DEG);
        vec[1] = (angles[1] * FastMath.RAD_TO_DEG);
        vec[2] = (angles[2] * FastMath.RAD_TO_DEG);
        setInverseRotationRadians(vec);
    }

    /**
     * 
     * <code>inverseTranslateVect</code> translates a given Vector3f by the
     * translation part of this matrix.
     * 
     * @param vec
     *            the Vector3f data to be translated.
     * @throws JmeException
     *             if the size of the Vector3f is not 3.
     */
    public void inverseTranslateVect(float[] vec) {
        if (vec.length != 3) { throw new VleException(
                "vec must be of size 3."); }

        vec[0] = vec[0] - m03;
        vec[1] = vec[1] - m13;
        vec[2] = vec[2] - m23;
    }

    /**
     * 
     * <code>inverseTranslateVect</code> translates a given Vector3f by the
     * translation part of this matrix.
     * 
     * @param data
     *            the Vector3f to be translated.
     * @throws JmeException
     *             if the size of the Vector3f is not 3.
     */
    public void inverseTranslateVect(Vector3f data) {
        data.x -= m03;
        data.y -= m13;
        data.z -= m23;
    }

    /**
     * 
     * <code>inverseTranslateVect</code> translates a given Vector3f by the
     * translation part of this matrix.
     * 
     * @param data
     *            the Vector3f to be translated.
     * @throws JmeException
     *             if the size of the Vector3f is not 3.
     */
    public void translateVect(Vector3f data) {
        data.x += m03;
        data.y += m13;
        data.z += m23;
    }

    /**
     * 
     * <code>inverseRotateVect</code> rotates a given Vector3f by the rotation
     * part of this matrix.
     * 
     * @param vec
     *            the Vector3f to be rotated.
     */
    public void inverseRotateVect(Vector3f vec) {
        float vx = vec.x, vy = vec.y, vz = vec.z;

        vec.x = vx * m00 + vy * m10 + vz * m20;
        vec.y = vx * m01 + vy * m11 + vz * m21;
        vec.z = vx * m02 + vy * m12 + vz * m22;
    }
    
    public void rotateVect(Vector3f vec) {
        float vx = vec.x, vy = vec.y, vz = vec.z;

        vec.x = vx * m00 + vy * m01 + vz * m02;
        vec.y = vx * m10 + vy * m11 + vz * m12;
        vec.z = vx * m20 + vy * m21 + vz * m22;
    }

    /**
     * <code>toString</code> returns the string representation of this object.
     * It is in a format of a 4x4 matrix. For example, an identity matrix would
     * be represented by the following string. com.jme.math.Matrix3f <br>[<br>
     * 1.0  0.0  0.0  0.0 <br>
     * 0.0  1.0  0.0  0.0 <br>
     * 0.0  0.0  1.0  0.0 <br>
     * 0.0  0.0  0.0  1.0 <br>]<br>
     * 
     * @return the string representation of this object.
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer("com.vtte.math.Matrix4f\n[\n");
        result.append(" ");
        result.append(m00);
        result.append("  ");
        result.append(m01);
        result.append("  ");
        result.append(m02);
        result.append("  ");
        result.append(m03);
        result.append(" \n");
        result.append(" ");
        result.append(m10);
        result.append("  ");
        result.append(m11);
        result.append("  ");
        result.append(m12);
        result.append("  ");
        result.append(m13);
        result.append(" \n");
        result.append(" ");
        result.append(m20);
        result.append("  ");
        result.append(m21);
        result.append("  ");
        result.append(m22);
        result.append("  ");
        result.append(m23);
        result.append(" \n");
        result.append(" ");
        result.append(m30);
        result.append("  ");
        result.append(m31);
        result.append("  ");
        result.append(m32);
        result.append("  ");
        result.append(m33);
        result.append(" \n]");
        return result.toString();
    }
    
    /**
     * 
     * <code>hashCode</code> returns the hash code value as an integer and is
     * supported for the benefit of hashing based collection classes such as
     * Hashtable, HashMap, HashSet etc.
     * 
     * @return the hashcode for this instance of Matrix4f.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = 37;
        hash = 37 * hash + Float.floatToIntBits(m00);
        hash = 37 * hash + Float.floatToIntBits(m01);
        hash = 37 * hash + Float.floatToIntBits(m02);
        hash = 37 * hash + Float.floatToIntBits(m03);

        hash = 37 * hash + Float.floatToIntBits(m10);
        hash = 37 * hash + Float.floatToIntBits(m11);
        hash = 37 * hash + Float.floatToIntBits(m12);
        hash = 37 * hash + Float.floatToIntBits(m13);

        hash = 37 * hash + Float.floatToIntBits(m20);
        hash = 37 * hash + Float.floatToIntBits(m21);
        hash = 37 * hash + Float.floatToIntBits(m22);
        hash = 37 * hash + Float.floatToIntBits(m23);

        hash = 37 * hash + Float.floatToIntBits(m30);
        hash = 37 * hash + Float.floatToIntBits(m31);
        hash = 37 * hash + Float.floatToIntBits(m32);
        hash = 37 * hash + Float.floatToIntBits(m33);

        return hash;
    }
    
    /**
     * are these two matrices the same? they are is they both have the same mXX values.
     *
     * @param o
     *            the object to compare for equality
     * @return true if they are equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Matrix4f) || o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        Matrix4f comp = (Matrix4f) o;
        if (Float.compare(m00,comp.m00) != 0) return false;
        if (Float.compare(m01,comp.m01) != 0) return false;
        if (Float.compare(m02,comp.m02) != 0) return false;
        if (Float.compare(m03,comp.m03) != 0) return false;

        if (Float.compare(m10,comp.m10) != 0) return false;
        if (Float.compare(m11,comp.m11) != 0) return false;
        if (Float.compare(m12,comp.m12) != 0) return false;
        if (Float.compare(m13,comp.m13) != 0) return false;

        if (Float.compare(m20,comp.m20) != 0) return false;
        if (Float.compare(m21,comp.m21) != 0) return false;
        if (Float.compare(m22,comp.m22) != 0) return false;
        if (Float.compare(m23,comp.m23) != 0) return false;

        if (Float.compare(m30,comp.m30) != 0) return false;
        if (Float.compare(m31,comp.m31) != 0) return false;
        if (Float.compare(m32,comp.m32) != 0) return false;
        if (Float.compare(m33,comp.m33) != 0) return false;

        return true;
    }
    
    /**
     * @return true if this matrix is identity
     */
    public boolean isIdentity() {
        return 
        (m00 == 1 && m01 == 0 && m02 == 0 && m03 == 0) &&
        (m10 == 0 && m11 == 1 && m12 == 0 && m13 == 0) &&
        (m20 == 0 && m21 == 0 && m22 == 1 && m23 == 0) &&
        (m30 == 0 && m31 == 0 && m32 == 0 && m33 == 1);
    }

    /**
     * Apply a scale to this matrix.
     * 
     * @param scale
     *            the scale to apply
     */
    public void scale(Vector3f scale) {
        m00 *= scale.x;
        m10 *= scale.x;
        m20 *= scale.x;
        m30 *= scale.x;
        m01 *= scale.y;
        m11 *= scale.y;
        m21 *= scale.y;
        m31 *= scale.y;
        m02 *= scale.z;
        m12 *= scale.z;
        m22 *= scale.z;
        m32 *= scale.z;
    }

    public void lookAt(Vector3f eye, Vector3f center, Vector3f up) {
        Context tmp = LocalContext.getContext();
                
        Vector3f d = tmp.tmpMatrix4f_f;
        Vector3f r = tmp.tmpMatrix4f_s;
        Vector3f u = tmp.tmpMatrix4f_u;
        d.set(center).subtractLocal(eye);
        d.normalizeLocal();
        r.set(d).crossLocal(up);
        r.normalizeLocal();
        u.set(r).crossLocal(d);
        u.normalizeLocal();

        m00 = r.x; m01 = r.y; m02 = r.z; m03 = 0.0f;
        m10 = u.x; m11 = u.y; m12 = u.z; m13 = 0.0f;
        m20 = -d.x; m21 = -d.y; m22 = -d.z; m23 = 0.0f;
        m30 = 0; m31 = 0; m32 = 0; m33 = 1.0f;
        
        Matrix4f trans = tmp.tmpMatrix4f_m;
        trans.loadIdentity();
        trans.m03 = -eye.x;
        trans.m13 = -eye.y;
        trans.m23 = -eye.z;
        this.multLocal(trans);

    }

    public void setPerspective(float fovy,  float aspectRatio,
                                float zNear, float zFar) {
        // calculate the appropriate left, right etc.
        float tan_fovy = FastMath.tan(FastMath.DEG_TO_RAD*fovy*0.5f);
        float right  =  tan_fovy * aspectRatio * zNear;
        float left   = -right;
        float top    =  tan_fovy * zNear;
        float bottom =  -top;
        setFrustum(left,right,bottom,top,zNear,zFar);
    }

    public void setFrustum(float left,   float right,
                            float bottom, float top,
                            float zNear,  float zFar) {
        float A = (right+left)/(right-left);
        float B = (top+bottom)/(top-bottom);
        float C = -(zFar+zNear)/(zFar-zNear);
        float D = -2.0f*zFar*zNear/(zFar-zNear);
        m00 = 2.0f * zNear / (right - left); m01 = 0.0f; m02 = A; m03 = 0.0f; 
        m10 = 0.0f; m11 = 2.0f * zNear / (top - bottom); m12 = B; m13 = 0.0f; 
        m20 = 0.0f; m21 = 0.0f; m22 = C; m23 = D; 
        m30 = 0.0f; m31 = 0.0f; m32 = -1.0f; m33 = 0.0f;
        
    }
    
    // taken from jME 2.0 AbstractCamera
    public void setFrustumParallel(float frustumLeft, float frustumRight,
                                   float frustumBottom, float frustumTop,
                                   float frustumNear, float frustumFar) {
        loadIdentity();
        m00 = 2.0f / (frustumRight - frustumLeft);
        m11 = 2.0f / (frustumBottom - frustumTop);
        m22 = -2.0f / (frustumFar - frustumNear);
        m33 = 1f;
        m30 = -(frustumRight + frustumLeft) / (frustumRight - frustumLeft);
        m31 = -(frustumBottom + frustumTop) / (frustumBottom - frustumTop);
        m32 = -(frustumFar + frustumNear) / (frustumFar - frustumNear);
    }

    /**
     * Interpolate the matrix with another matrix as:
     * this*(1-interpolation) + other*(interpolation)
     * @param other
     * @param interpolation
     */
    public void interpolate(Matrix4f other, float interpolation) {
        // this matrixs contribution scale
        float oneminus = 1f - interpolation;
        
        /*
        Context tmp = LocalContext.getContext();
        
        // translation: m03, m13, m23
        // rotation this
        Quaternion thisRot = tmp.tmpMatrix4f_q;
        Quaternion otherRot = tmp.tmpMatrix4f_q2;
        
        Vector3f thisTrans = tmp.tmpMatrix4f_f;
        Vector3f otherTrans = tmp.tmpMatrix4f_s;
        
        // set in translation
        this.toTranslationVector(thisTrans);
        other.toTranslationVector(otherTrans);
        
        // set in rotations
        thisRot.fromRotationMatrix(this);
        otherRot.fromRotationMatrix(other);
        
        // interpolate translation
        thisTrans.interpolate(otherTrans, interpolation);
        
        // interpolate rotation
        thisRot.slerp(otherRot, interpolation);
        
        // set this matrix from rotation and translation
        thisRot.toRotationMatrix(this);
        
        // set in traslation
        this.setTranslation(thisTrans);

     */
        // lineary inerpolating matrices is ok if the matrices
        // do not differ much, eg: in bone animation
        m00 = m00*oneminus + other.m00*interpolation;
        m01 = m01*oneminus + other.m01*interpolation;
        m02 = m02*oneminus + other.m02*interpolation;
        m03 = m03*oneminus + other.m03*interpolation;
        m10 = m10*oneminus + other.m10*interpolation;
        m11 = m11*oneminus + other.m11*interpolation;
        m12 = m12*oneminus + other.m12*interpolation;
        m13 = m13*oneminus + other.m13*interpolation;
        m20 = m20*oneminus + other.m20*interpolation;
        m21 = m21*oneminus + other.m21*interpolation;
        m22 = m22*oneminus + other.m22*interpolation;
        m23 = m23*oneminus + other.m23*interpolation;
        m30 = m30*oneminus + other.m30*interpolation;
        m31 = m31*oneminus + other.m31*interpolation;
        m32 = m32*oneminus + other.m32*interpolation;
        m33 = m33*oneminus + other.m33*interpolation;

    }
}
