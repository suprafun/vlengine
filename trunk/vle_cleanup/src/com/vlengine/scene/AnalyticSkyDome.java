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

package com.vlengine.scene;

import com.vlengine.image.Image;
import com.vlengine.image.Texture;
import com.vlengine.light.DirectionalLight;
import com.vlengine.light.Light;
import com.vlengine.light.PointLight;
import com.vlengine.math.FastMath;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Cylinder;
import com.vlengine.model.Dome;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.batch.LightBatch;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.util.TextureManager;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.VertexAttribute;
import java.nio.FloatBuffer;

/**
 * sky gradient based on "A practical analytic model for daylight"
 * by A. J. Preetham, Peter Shirley, Brian Smits (University of Utah)
 * 
 * The skydome is sensitive to the change in vertex order in Dome and Cylinder
 * classes. So the local copy of those classes is used in case their original
 * implementation is altered and vertex order is changed.
 * 
 * @author Highnik
 * @author lex (Aleksey Nikiforov) speed improvements, documentation
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class AnalyticSkyDome extends SetNode {
    private static final float INFINITY = 3.3e+38f;
    private static final float EPSILON  = 0.000001f;
    
    public static final float DEFAULT_BRIGHTNESS = 1;
	public static final float BLOOM_BRIGHTNESS = 0.65f;
    
    private Dome dome;
    private TriBatch domeBatch;
    private Mesh domeMesh;
    private Material domeMaterial;
    private Cylinder cylinder;
    private TriBatch cylinderBatch;
    private Material cylinderMaterial;
    private Mesh cylinderMesh;
    private int radialSamples;
    private ColorRGBA fogColor;
    
    private Vector3f cameraPos = new Vector3f();
    
    // shading parameters
    private float thetaSun;
    private float phiSun;
    private float turbidity = 2.0f;
    private boolean isLinearExpControl;
    private float exposure = 18.0f;
    private float overcast;
    private float gammaCorrection = 2.5f;
    private float brightness = DEFAULT_BRIGHTNESS;
    
    // time parameters
    private float timeOfDay = 0.0f;
    private float julianDay = 0.0f;
    private float latitude  = 0.0f;
    private float longitude = 0.0f;
    private float stdMeridian = 0.0f;
    private float sunnyTime = 12.0f;
    private float solarDeclination = 0.0f;
    private float latitudeInRadian = 0.0f;
    private boolean isNight = false;
    // timer control.
    private float timeCount = 0;
    private float updateTime = 0.0f;
    private float timeWarp = 180.0f;
    private float nightWarp = 1800.0f;
    
    // used at update color
    private float chi;
    private float zenithLuminance;
    private float zenithX;
    private float zenithY;
    private float[] perezLuminance;
    private float[] perezX;
    private float[] perezY;
    private Vector3f sunDirection = new Vector3f();
    private Vector3f sunPosition = new Vector3f();
    private ColorXYZ colorTemp = new ColorXYZ();
    private ColorRGBA tempRGBA = new ColorRGBA();
    //private TriBatch batch;
    private FloatBuffer colorBuf;
    private FloatBuffer normalBuf;
    private Vector3f vertex = new Vector3f();
    private float gamma;
    private float cosTheta;
    private float cosGamma2;
    private float x_value;
    private float y_value;
    private float yClear;
    private float yOver;
    private float _Y;
    private float _X;
    private float _Z;
    
    private final boolean directionalsunlight = false;
    private Light sunLight;
    //private LightBatch sunBatch;
    private Vector3f sunDir = new Vector3f();
    private LightNode sun;
    
    private float sunDistance;
    //private LensFlare flare;
    private boolean sunEnabled = true;
    
    
    /** Distribution coefficients for the luminance(Y) distribution function */
    private float distributionLuminance[][] = {	// Perez distributions
        {  0.17872f , -1.46303f },		// a = darkening or brightening of the horizon
        { -0.35540f ,  0.42749f },		// b = luminance gradient near the horizon,
        { -0.02266f ,  5.32505f },		// c = relative intensity of the circumsolar region
        {  0.12064f , -2.57705f },		// d = width of the circumsolar region
        { -0.06696f ,  0.37027f }};		// e = relative backscattered light
    
    /** Distribution coefficients for the x distribution function */
    private float distributionXcomp[][] = {
        { -0.01925f , -0.25922f },
        { -0.06651f ,  0.00081f },
        { -0.00041f ,  0.21247f },
        { -0.06409f , -0.89887f },
        { -0.00325f ,  0.04517f }};
    
    /** Distribution coefficients for the y distribution function */
    private float distributionYcomp[][] = {
        { -0.01669f , -0.26078f },
        { -0.09495f ,  0.00921f },
        { -0.00792f ,  0.21023f },
        { -0.04405f , -1.65369f },
        { -0.01092f ,  0.05291f }};
    
    /** Zenith x value */
    private float zenithXmatrix[][] = {
        {  0.00165f, -0.00375f,  0.00209f,  0.00000f },
        { -0.02903f,  0.06377f, -0.03202f,  0.00394f },
        {  0.11693f, -0.21196f,  0.06052f,  0.25886f }};
    
    /** Zenith y value */
    private float zenithYmatrix[][] = {
        {  0.00275f, -0.00610f,  0.00317f,  0.00000f },
        { -0.04214f,  0.08970f, -0.04153f,  0.00516f },
        {  0.15346f, -0.26756f,  0.06670f,  0.26688f }};
/** Creates a new instance of SkyDome */
    public AnalyticSkyDome() {
        this("SkyDome", 11, 18, 100f, 100f, 20);
    }
    
    public AnalyticSkyDome(String name) {
        this(name, 11, 18, 100f, 100f, 20);
    }
    
    public AnalyticSkyDome(String name, int planes, int radialSamples, float radius,
    		float sunDistance, float fadeHeight) {
        super(name);

    	this.sunDistance = sunDistance;
    	this.radialSamples = radialSamples;
        	
        cylinder = new Cylinder(2, radialSamples, radius, fadeHeight);
        cylinder.fillBuffer(VertexAttribute.USAGE_COLOR, ColorRGBA.black);
        cylinder.setDisplayListMode(BaseGeometry.LIST_NO);
        cylinder.setVBOMode(BaseGeometry.VBO_NO);

        cylinderBatch = new TriBatch();
        cylinderMaterial = new Material();
        cylinderBatch.setModel(cylinder);
        cylinderBatch.setMaterial(cylinderMaterial);
        cylinderBatch.setRenderQueueMode(RenderQueue.QueueFilter.BackGround.value);
        cylinderBatch.setRenderPassMode(RenderPass.PassFilter.BackGround.value);
        

        cylinderMaterial.setLightCombineMode(LightState.OFF);
        
        cylinderMesh = new Mesh("cylinder");
        cylinderMesh.setBatch(cylinderBatch);
        
        cylinderMesh.getLocalRotation().fromAngles(90*FastMath.DEG_TO_RAD, 0, 0);
        cylinderMesh.getLocalTranslation().set(0, -fadeHeight/2 + 0.01f, 0);
        cylinderMesh.setRenderQueueMode(RenderQueue.QueueFilter.BackGround.value);
        cylinderMesh.setRenderPassMode(RenderPass.PassFilter.BackGround.value);
        
        attachChild(cylinderMesh);
        
        dome = new Dome(new Vector3f(), planes, radialSamples, radius, true);
        dome.fillBuffer(VertexAttribute.USAGE_COLOR, ColorRGBA.black);
        dome.setDisplayListMode(BaseGeometry.LIST_NO);
        dome.setVBOMode(BaseGeometry.VBO_NO);
        

        domeBatch = new TriBatch();
        domeMaterial = new Material();
        domeMaterial.setLightCombineMode(LightState.OFF);
        
        domeBatch.setModel(dome);
        domeBatch.setMaterial(domeMaterial);
        domeBatch.setRenderQueueMode(RenderQueue.QueueFilter.BackGround.value);
        domeBatch.setRenderPassMode(RenderPass.PassFilter.BackGround.value);
        domeMesh = new Mesh("dome");
        domeMesh.setBatch(domeBatch);
        domeMesh.setRenderQueueMode(RenderQueue.QueueFilter.BackGround.value);
        domeMesh.setRenderPassMode(RenderPass.PassFilter.BackGround.value);
        
        attachChild(domeMesh);
        
        solarDeclination = calc_solar_declination(julianDay);
        sunnyTime = calc_sunny_time(latitude,  solarDeclination);
        
        /*
        ZBufferState zbuff = new ZBufferState();
        zbuff.setWritable(false);
        zbuff.setEnabled(false);
        zbuff.setFunction(ZBufferState.CF_ALWAYS);
        //zbuff.setFunction(ZBufferState.CF_LEQUAL);
        cylinderMaterial.setRenderState(zbuff);
        domeMaterial.setRenderState(zbuff);
         */
        
        /*
        CullState cs = new CullState();
        cs.setCullMode(CullState.CS_NONE);
        cs.setEnabled(true);
        cylinderMaterial.setRenderState(cs);
         domeMaterial.setRenderState(cs);
         */
        
        setupSun();
        //setupLensFlare();
        
        setCullMode(SceneElement.CullMode.NEVER);
        setLightCombineMode(LightState.OFF);
        //setTextureCombineMode(TextureState.OFF);
        setRenderQueueMode(RenderQueue.QueueFilter.BackGround.value);
    }
    
    public void setFadeColor(ColorRGBA fogColor) {
    	this.fogColor = fogColor;
    }
    
    public ColorRGBA getFadeToFogColor() {
    	return fogColor;
    }
    
    /**
     * Set Sun's positon
     */
    public void setSunPosition(Vector3f sunPos) {
        Vector3f pos = new Vector3f();
        pos = FastMath.cartesianToSpherical(sunPos, pos);
        thetaSun = pos.z;
        phiSun = pos.y;
    }
    
    /**
     * Return Sun's position
     */
    public Vector3f getSunPosition() {
        return sunPosition;
    }
    
    /**
     * Convert time to sun position
     * @param time
     *              Sets a time of day between 0 to 24 (6,25 = 6:15 hs)
     */
    public void setSunPosition(float time) {
        float solarTime, solarAltitude, opp, adj, solarAzimuth, cosSolarDeclination, sinSolarDeclination, sinLatitude, cosLatitude;
        this.timeOfDay = time;
        
        sinLatitude = FastMath.sin(latitudeInRadian);
        cosLatitude = FastMath.cos(latitudeInRadian);
        sinSolarDeclination = FastMath.sin(solarDeclination);
        cosSolarDeclination = FastMath.cos(solarDeclination);
        
        // real time
        solarTime = time + (0.170f * FastMath.sin(4f * FastMath.PI * (julianDay - 80f) / 373f) -
                0.129f * FastMath.sin(FastMath.TWO_PI * (julianDay - 8f) / 355f)) +
                (stdMeridian - longitude) / 15;
        
        solarAltitude = FastMath.asin(sinLatitude * sinSolarDeclination -
                cosLatitude * cosSolarDeclination *
                FastMath.cos(FastMath.PI * solarTime / sunnyTime));
        
        opp = -cosSolarDeclination * FastMath.sin(FastMath.PI * solarTime / sunnyTime);
        
        adj = -(cosLatitude * sinSolarDeclination + sinLatitude * cosSolarDeclination *
                FastMath.cos(FastMath.PI * solarTime / sunnyTime));
        
        solarAzimuth = FastMath.atan(opp / adj);

        if (solarAltitude > 0f) { //-0.05f
            
            isNight = false;
            if ((opp < 0.0f && solarAzimuth < 0.0f) || (opp > 0.0f && solarAzimuth > 0.0f)) {
                solarAzimuth = FastMath.HALF_PI + solarAzimuth;
            } else {
                solarAzimuth = FastMath.HALF_PI - solarAzimuth;
            }
            phiSun = FastMath.TWO_PI - solarAzimuth;
            thetaSun = FastMath.HALF_PI - solarAltitude;
            
            //System.out.println("h: " + phiSun*FastMath.RAD_TO_DEG + ", v: " + thetaSun*FastMath.RAD_TO_DEG);
            
            sunDirection.x = sunDistance;
            sunDirection.y = phiSun;
            sunDirection.z = solarAltitude;
            sunPosition = FastMath.sphericalToCartesian(sunDirection, sunPosition);
            if (solarAzimuth < 0.0f) {
                sunPosition.x *= -1;
            }
            
            if (this.isSunEnabled()) {
                ((LightBatch)sun.getBatch()).getLight().setEnabled(true);
                if(this.directionalsunlight) {
                    sun.getLocalTranslation().set(sunPosition);
                    sunDir.set(sunPosition).negateLocal();
                    sun.getLocalRotation().lookAt(sunDir, Vector3f.UNIT_Y);
                } else {
                    sun.getLocalTranslation().set(sunPosition).multLocal(1000);
                }
            } else {
                ((LightBatch)sun.getBatch()).getLight().setEnabled(false);
            }
        } else {
            ((LightBatch)sun.getBatch()).getLight().setEnabled(false);
            isNight = true;
        }
    }
    
    /**
     * Return if now is night
     */
    public boolean isNight() {
        return isNight;
    }
    
    /**
     * Set Day of year between 0 to 364
     */
    public void setDay(float julianDay) {
        this.julianDay = clamp(julianDay, 0.0f, 365.0f);
        // Solar declination
        solarDeclination = calc_solar_declination(julianDay);
        sunnyTime = calc_sunny_time(latitude, solarDeclination);
    }
    
    /**
     * Get Day of year
     */
    public float getDay() {
        return julianDay;
    }
    
    /**
     * Set latitude
     */
    public void setLatitude(float latitude) {
        this.latitude = clamp(latitude, -90.0f, 90.0f);
        latitudeInRadian = FastMath.DEG_TO_RAD * latitude;
        sunnyTime = calc_sunny_time(latitudeInRadian, solarDeclination);
    }
    
    /**
     * Get latitude
     */
    public float getLatitude() {
        return latitude;
    }
    
    /**
     * Set longitude
     */
    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }
    
    /**
     * Get longitude
     */
    public float getLongitude() {
        return longitude;
    }
    
    /**
     * Set standar meridian
     * @param stdMeridian
     *                      TimeZone * 15
     */
    public void setStandardMeridian(float stdMeridian) {
        this.stdMeridian = stdMeridian;
    }
    
    /**
     * Get standar meridian
     */
    public float getStandardMeridian() {
        return stdMeridian;
    }
    
    /**
     * Between 1.0f and 10.0f, where value of 1.0 would be super
     * clean air and 10.0 would be very dusty (or full of water vapor) air.
     * The crystal clear day values is 2.0f.
     * 
     * give earth-like results.
     * @param turbidity
     */
    public void setTurbidity(float turbidity ) {
        this.turbidity = clamp(turbidity, 0.0f, 10.0f);
    }
    
    /**
     * Set Exposure factor 1.0 to infinity.
     * Lower - brighter colors, Heigher - darker colors.
     */
    public void setExposure(boolean isLinearExpControl, float exposure) {
        this.isLinearExpControl = isLinearExpControl;
        this.exposure = 1.0f / clamp(exposure, 1.0f, INFINITY );
    }
    
    /**
     * Set Over Cast factor between 0.0f and 1.0f. 0 beang clear sky
     * and 1 being cloudy sky.
     */
    public void setOvercastFactor(float overcast) {
        this.overcast = clamp(overcast, 0.0f, 1.0f );
    }
    
    /**
     * Set gamma correction factor 1.0 to infinity.
     * Lower - stronger colors, Heigher - weaker grayish colors. 
     */
    public void setGammaCorrection(float gamma) {
        this.gammaCorrection = 1.0f / clamp(gamma, EPSILON, INFINITY );
    }
    
    /**
     * Seconds to update
     */
    public void setUpdateTime(float seconds) {
        this.updateTime = seconds;
    }
    
    public float getUpdateTime() {
        return updateTime;
    }
    
    /**
     * if updateTime = 1 and timeWarp = 1, every seconds will be updated
     */
    public void setTimeWarp(float timeWarp) {
        this.timeWarp = timeWarp;
    }
    
    public float getTimeWarp() {
        return timeWarp;
    }
    
    public float getBrightness() {
    	return this.brightness;
    }
    
    public void setBrightness(float brightness) {
    	this.brightness = brightness;
    }
    
    @Override
    public void updateGeometricState(UpdateContext ctx, boolean initiator) {
        if (updateTime > 0.0f) {
        	timeCount += ctx.time;
            if (timeCount > updateTime) {
                float warp = timeWarp;
                if( isNight ) {
                    // warp time fast at night
                    warp = nightWarp;
                }
            	float timeDiff = (int) (timeCount/updateTime) * updateTime;
            	timeCount -= timeDiff;
                timeOfDay += timeDiff * warp / 3600f;

                if (timeOfDay > 24) timeOfDay -= 24f;
                
                setSunPosition(timeOfDay);
                updateColors();
                
                
            }
        }
        
        cameraPos = ctx.frame.getCamera().getLocation();
        this.getLocalTranslation().set(cameraPos);
        super.updateGeometricState(ctx, initiator);
    }

    public void setNightTimeWarp(float nightWarp) {
        this.nightWarp = nightWarp;
    }
    
    /**
     * update Sky color
     */
    private void updateColors() {
        // reset ambient and diffuse colors in sun
        Light l = ((LightBatch)sun.getBatch()).getLight();
        ColorRGBA ambient = l.getAmbient();
        ColorRGBA diffuse = l.getDiffuse();
        ambient.set(ColorRGBA.black);
        diffuse.set(ColorRGBA.black);
        
        if (isNight) {
            dome.fillBuffer(VertexAttribute.USAGE_COLOR, ColorRGBA.black);
            cylinder.fillBuffer(VertexAttribute.USAGE_COLOR, ColorRGBA.black);
            
            return;
        }
        
        // get zenith luminance
        chi = ( (4.0f / 9.0f) - (turbidity / 120.0f) ) * ( FastMath.PI - (2.0f * thetaSun) );
        zenithLuminance = ( (4.0453f * turbidity) - 4.9710f ) * FastMath.tan(chi) - (0.2155f * turbidity) + 2.4192f;
        if (zenithLuminance < 0.0f)
            zenithLuminance = -zenithLuminance;
        
        // get x / y zenith
        zenithX = getZenith( zenithXmatrix, thetaSun, turbidity );
        zenithY = getZenith( zenithYmatrix, thetaSun, turbidity );
        
        // get perez function parameters
        perezLuminance = getPerez(distributionLuminance, turbidity );
        perezX = getPerez(distributionXcomp, turbidity );
        perezY = getPerez(distributionYcomp, turbidity );
        
        // make some precalculation
        zenithX = perezFunctionO1( perezX, thetaSun, zenithX );
        zenithY = perezFunctionO1( perezY, thetaSun, zenithY );
        zenithLuminance = perezFunctionO1( perezLuminance, thetaSun, zenithLuminance );
        
        // build sun direction vector
        sunDirection.x = FastMath.cos(FastMath.HALF_PI - thetaSun) * FastMath.cos(phiSun);
        sunDirection.y = FastMath.sin(FastMath.HALF_PI - thetaSun);
        sunDirection.z = FastMath.cos(FastMath.HALF_PI - thetaSun) * FastMath.sin(phiSun);
        sunDirection.normalize();
                
        normalBuf = dome.getAttribBuffer(VertexAttribute.USAGE_NORMAL).getDataBuffer();
        colorBuf = dome.getAttribBuffer(VertexAttribute.USAGE_COLOR).getDataBuffer();
        
        
        int diffuseSamples = 0;
        
        for (int i = 0; i < dome.getNumVertex(); i++) {
            
            BufferUtils.populateFromBuffer(vertex, normalBuf, i);
            
            // angle between sun and vertex
            gamma = FastMath.acos(vertex.dot(sunDirection));
            
            if (vertex.y < 0.05f) {
                vertex.y = 0.05f;
            }
            
            cosTheta = 1.0f / vertex.y;
            cosGamma2 = FastMath.sqr(FastMath.cos(gamma));

            // Compute x,y values
            x_value = perezFunctionO2( perezX, cosTheta, gamma, cosGamma2, zenithX );
            y_value = perezFunctionO2( perezY, cosTheta, gamma, cosGamma2, zenithY );

            // luminance(Y) for clear & overcast sky
            yClear = perezFunctionO2( perezLuminance, cosTheta, gamma, cosGamma2, zenithLuminance );
            yOver = (1.0f + 2.0f * vertex.y) / 3.0f;
            
            _Y = FastMath.LERP(overcast, yClear, yOver);
            _X = (x_value / y_value) * _Y;
            _Z = ((1.0f - x_value - y_value) / y_value) * _Y;
            
            colorTemp.setXYZ(_X, _Y, _Z);
            colorTemp.convertXYZtoRGB();
            colorTemp.convertRGBtoHSV();
            
            if (isLinearExpControl) {                                       // linear scale
                colorTemp.setValue(colorTemp.getValue() * exposure);
            } else {                                                        // exp scale
                colorTemp.setValue(1.0f - FastMath.exp(-exposure * colorTemp.getValue()));
            }
            colorTemp.convertHSVtoRGB();
            
            // gamma control
            colorTemp.setGammaCorrection(gammaCorrection);
            
            ColorRGBA finalColor = colorTemp.getRGBA(tempRGBA);
            finalColor.multLocal(brightness);
            finalColor.clamp();
            
            // change the color
            BufferUtils.setInBuffer(finalColor, colorBuf, i);
            
            if (i < radialSamples) {
            	setCylinderVertexColor(finalColor, i);
            	if (i == 0) setCylinderVertexColor(finalColor, radialSamples);
            }
            
            // add it to the ambient color
            ambient.r += finalColor.r;
            ambient.g += finalColor.g;
            ambient.b += finalColor.b;
            
            // if inside sun disk, addd it to the diffuse
            if(gamma<0.1f) {
                diffuse.r += finalColor.r;
                diffuse.g += finalColor.g;
                diffuse.b += finalColor.b;
                diffuseSamples++;
            }
        }
        
        // divide ambient by samples
        float samples = 1f/dome.getNumVertex()*2f;
        ambient.r *= samples;
        ambient.g *= samples;
        ambient.b *= samples;
        ambient.a = 1f;
        
        samples = 1f/((float)diffuseSamples);
        if(diffuseSamples>0) {
            diffuse.r *= samples;
            diffuse.g *= samples;
            diffuse.b *= samples;
            diffuse.a = 1.0f;
        } else {
            diffuse.set(ambient);
        }
        
        float lerp = FastMath.sin(thetaSun*2);
        /*
        ambient.r = FastMath.LERP(lerp, ambient.r, diffuse.r);
        ambient.g = FastMath.LERP(lerp, ambient.g, diffuse.g);
        ambient.b = FastMath.LERP(lerp, ambient.b, diffuse.b);
         */
        ambient.r *= lerp;
        ambient.g *= lerp;
        ambient.b *= lerp;
    }

    private void setCylinderVertexColor(ColorRGBA color, int vertex) {
    	FloatBuffer buffer = cylinder.getAttribBuffer(VertexAttribute.USAGE_COLOR).getDataBuffer();
    	BufferUtils.setInBuffer(color, buffer, vertex);
    	
    	if (fogColor == null) {
            BufferUtils.setInBuffer(color, buffer, vertex + radialSamples + 1);
    	} else {
            BufferUtils.setInBuffer(fogColor, buffer, vertex + radialSamples + 1);
    	}
    }

    public Light getSunLight() {
        return sunLight;
    }
    
    /**
     * Set the rootNode to flare
     */
    /*
    public void setRootNode(Node value) {
        if (flare != null) {
            flare.setRootNode(value);
        }
    }
     */
    
    /**
     * Set a intensity to Flare
     */
    /*
    public void setIntensity(float value) {
        if (flare != null) {
            flare.setIntensity(value);
        }
    }
     */

    /**
     * Set a target to LightNode
     */
    /*
    public void setTarget(Spatial node) {
        if (sun != null) {
            sun.setTarget(node);
        }
    }
     */

    public void setSunEnabled(boolean enable) {
        this.sunEnabled = enable;
        ((LightBatch)sun.getBatch()).getLight().setEnabled(enable);
    }
    
    public boolean isSunEnabled() {
        return sunEnabled;
    }
    
    private float calc_solar_declination(float jDay) {
        return (0.4093f * FastMath.sin(FastMath.TWO_PI * (284f + jDay) / 365f));
    }
    
    private float calc_sunny_time(float lat, float solarDeclin) {
        // Time of hours over horizon
        float sunnyTime;
        sunnyTime = (2.0f * FastMath.acos(-FastMath.tan(lat) * FastMath.tan(solarDeclin)));
        sunnyTime = (sunnyTime * FastMath.RAD_TO_DEG) / 15;
        return sunnyTime;
    }
    
    private void setupSun() {
        if(this.directionalsunlight) {
            sunLight = new DirectionalLight();
        } else {
            sunLight = new PointLight();
        }
        sunLight.setEnabled(true);
        sunLight.setDiffuse(ColorRGBA.white.clone());
        sunLight.setAmbient(ColorRGBA.gray.clone());
        sunLight.setSpecular(ColorRGBA.white.clone());
        sunLight.setAttenuate(false);
        //sunLight.setDirection(new Vector3f(0.0f, 0.0f, 0.0f));
        sunLight.setShadowCaster(true);
        
        sun = new LightNode("SunNode", sunLight);
        
        attachChild(sun);
    }
    
    /**
     * Create Lens flare effect
     */
    @SuppressWarnings("unused")
	private void setupLensFlare() {
        // Setup the lensflare textures.
        TextureState[] tex = new TextureState[4];
        tex[0] = new TextureState();
        tex[0].setTexture(
                TextureManager.loadTexture("flare1.png",
                Texture.MM_LINEAR_LINEAR,
                Texture.FM_LINEAR,
                Image.GUESS_FORMAT,
                1.0f,
                true));
        tex[0].setEnabled(true);
        
        tex[1] = new TextureState();
        tex[1].setTexture(
                TextureManager.loadTexture("flare2.png",
                        Texture.MM_LINEAR_LINEAR,
                        Texture.FM_LINEAR,
                        Image.GUESS_FORMAT,
                        1.0f,
                        true));
        tex[1].setEnabled(true);
        
        tex[2] = new TextureState();
        tex[2].setTexture(
                TextureManager.loadTexture("flare3.png",
                        Texture.MM_LINEAR_LINEAR,
                        Texture.FM_LINEAR,
                        Image.GUESS_FORMAT,
                        1.0f,
                        true));
        tex[2].setEnabled(true);
        
        tex[3] = new TextureState();
        tex[3].setTexture(
                TextureManager.loadTexture("flare4.png",
                        Texture.MM_LINEAR_LINEAR,
                        Texture.FM_LINEAR,
                        Image.GUESS_FORMAT,
                        1.0f,
                        true));
        tex[3].setEnabled(true);
        
        /*
        flare = LensFlareFactory.createBasicLensFlare("flare", tex);
        
        flare.setIntensity(0.5f);
        
        sun.attachChild(flare);
         */
    }
    
    private float[] getPerez(float[][] distribution, float turbidity ) {
        float[] perez = new float[5];
        perez[0] = distribution[0][0] * turbidity + distribution[0][1];
        perez[1] = distribution[1][0] * turbidity + distribution[1][1];
        perez[2] = distribution[2][0] * turbidity + distribution[2][1];
        perez[3] = distribution[3][0] * turbidity + distribution[3][1];
        perez[4] = distribution[4][0] * turbidity + distribution[4][1];
        return perez;
    }
    
    private float getZenith(float[][] zenithMatrix, float theta, float turbidity) {
        float theta2 = theta * theta;
        float theta3 = theta * theta2;
        
        return	(zenithMatrix[0][0] * theta3 + zenithMatrix[0][1] * theta2 + zenithMatrix[0][2] * theta + zenithMatrix[0][3]) * turbidity * turbidity +
                (zenithMatrix[1][0] * theta3 + zenithMatrix[1][1] * theta2 + zenithMatrix[1][2] * theta + zenithMatrix[1][3]) * turbidity +
                (zenithMatrix[2][0] * theta3 + zenithMatrix[2][1] * theta2 + zenithMatrix[2][2] * theta + zenithMatrix[2][3]);
    }
    
    private float perezFunctionO1(float[] perezCoeffs, float thetaSun, float zenithValue ) {
        float val = (1.0f + perezCoeffs[0] * FastMath.exp(perezCoeffs[1])) *
                (1.0f + perezCoeffs[2] * FastMath.exp(perezCoeffs[3] * thetaSun ) + perezCoeffs[4] * FastMath.sqr(FastMath.cos(thetaSun)));
        return zenithValue / val;
    }
    
    private float perezFunctionO2(float[] perezCoeffs, float cosTheta, float gamma, float cosGamma2, float zenithValue ) {
        return zenithValue * (1.0f + perezCoeffs[0] * FastMath.exp(perezCoeffs[1] * cosTheta )) *
                (1.0f + perezCoeffs[2] * FastMath.exp(perezCoeffs[3] * gamma) + perezCoeffs[4] * cosGamma2);
    }
    
    /**
     * clamp the value between min and max values
     */
    private float clamp(float value, float min, float max) {
        if (value < min)
            return min;
        else if (value > max)
            return max;
        else
            return value;
    }
    
    private static class ColorXYZ {
        private float x = 0.0f;
        private float y = 0.0f;
        private float z = 0.0f;
        private float r = 0.0f;
        private float g = 0.0f;
        private float b = 0.0f;
        private float a = 1.0f;
        private float hue = 0.0f;
        private float saturation = 0.0f;
        private float value = 0.0f;
        
        public ColorXYZ() { }
        
        public ColorXYZ(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public void setXYZ(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public void setValue(float value) {
            this.value = value;
        }
        
        public float getValue() {
            return this.value;
        }
        
        public void setGammaCorrection(float gammaCorrection) {
            r = FastMath.pow(r, gammaCorrection);
            g = FastMath.pow(g, gammaCorrection);
            b = FastMath.pow(b, gammaCorrection);
        }
        
        /**
         * Retorna o RGBA color
         */
        public ColorRGBA getRGBA(ColorRGBA store) {
        	store.set(r, g, b, a);
            return store;
        }
        
        /**
         * Converte XYZ to RGB color
         */
        public void convertXYZtoRGB() {
            this.r =  3.240479f * x - 1.537150f * y - 0.498535f * z;
            this.g = -0.969256f * x + 1.875992f * y + 0.041556f * z;
            this.b =  0.055648f * x - 0.204043f * y + 1.057311f * z;
        }
        
        /**
         * Converte RGB to HSV
         */
        public void convertRGBtoHSV() {
            float minColor = Math.min(Math.min(r,g),b);
            float maxColor = Math.max(Math.max(r,g),b);
            float delta = maxColor - minColor;
            
            this.value = maxColor;                                              // Value
            if ( ! (FastMath.abs(maxColor) < EPSILON)) {
                this.saturation = delta / maxColor;                             // Saturation
            } else {                                                            // r = g = b = 0
                this.saturation = 0.0f;                                         // Saturation = 0
                this.hue = -1;                                                  // Hue = undefined
                return;
            }
            
            if (FastMath.abs(r - maxColor) < EPSILON)
                this.hue = (g - b) / delta;                                     // between yellow & magenta
            else if (FastMath.abs(g - maxColor) < EPSILON)
                this.hue = 2.0f + (b-r) / delta;                                // between cyan & yellow
            else
                this.hue = 4.0f + (r-g) / delta;                                // between magenta & cyan
            
            this.hue *= 60.0f;                                                  // degrees
            
            if (this.hue < 0.0f )
                this.hue += 360.0f;                                             // positive
        }
        
        /**
         * Converte HSV to RGB
         */
        public void convertHSVtoRGB() {
            if (FastMath.abs(saturation) < EPSILON) {                           // achromatic (grey)
                this.r = value;
                this.g = value;
                this.b = value;
                this.a = value;
            }
            
            hue /= 60.0f;							// sector 0 to 5
            int sector = (int) FastMath.floor(hue);
            
            float f = hue - sector;                                             // factorial part of hue
            float p = value * (1.0f - saturation);
            float q = value * (1.0f - saturation * f );
            float t = value * (1.0f - saturation * (1.0f - f));
            switch (sector) {
                case 0:
                    this.r = value;
                    this.g = t;
                    this.b = p;
                    break;
                case 1:
                    this.r = q;
                    this.g = value;
                    this.b = p;
                    break;
                case 2:
                    this.r = p;
                    this.g = value;
                    this.b = t;
                    break;
                case 3:
                    this.r = p;
                    this.g = q;
                    this.b = value;
                    break;
                case 4:
                    this.r = t;
                    this.g = p;
                    this.b = value;
                    break;
                default:                                                        // case 5:
                    this.r = value;
                    this.g = p;
                    this.b = q;
                    break;
            }
        }
    }
}
