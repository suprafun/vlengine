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

package com.vlengine.light;

import com.vlengine.math.FastMath;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ShadowUtils {
    
    public static float m_fSplitLambda = 0.6f;

    /**
     * Calculates number of frustrum splits.
     * Source: http://hax.fi/asko/PSSM.html#OpenGL
     *   "The demo and its source code are public domain. Feel free use them in any imaginable way. Crediting is appreciated but not necessary. "
     * @param iNumOfSplits
     * @param fNear
     * @param fFar
     * @param m_fSplitDistances
     * @return
     */
    public static float[] calculateSplitDistances(int iNumOfSplits, float fNear, float fFar, float[] m_fSplitDistances) {
        if(m_fSplitDistances==null || m_fSplitDistances.length != iNumOfSplits+1)
            m_fSplitDistances = new float[iNumOfSplits+1];

	// The lambda must me between 0.0f and 1.0f
	float fLambda = m_fSplitLambda;
	
	for(int i = 0; i < iNumOfSplits; i++) {
		float fIDM = i / (float)iNumOfSplits;
		float fLog = fNear * FastMath.pow((fFar/fNear), fIDM);
		float fUniform = fNear + (fFar - fNear)*fIDM;
		m_fSplitDistances[i] = fLog * fLambda + fUniform*(1-fLambda);
    }

	// This is used to improve the correctness of the calculations. Our main near- and farplane
	// of the camera always stay the same, no matter what happens.
	m_fSplitDistances[0] = fNear;
	m_fSplitDistances[iNumOfSplits] = fFar;
        
        return m_fSplitDistances;
    }
}
