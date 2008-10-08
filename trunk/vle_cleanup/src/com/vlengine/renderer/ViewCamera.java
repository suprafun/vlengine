/*
 * Camera.java
 * 
 * Created on 2007.10.23., 22:58:41
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vlengine.renderer;

import com.vlengine.math.FastMath;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Plane;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector2f;
import com.vlengine.math.Vector3f;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import java.util.logging.Logger;

/**
 *
 * @author vear
 */

public class ViewCamera extends Camera {

    private static final Logger logger = Logger.getLogger(ViewCamera.class
            .getName());
    

    //the location and orientation of the camera.
    /**
     * Camera's location
     */
    protected Vector3f location;

    /**
     * Direction of camera's 'left'
     */
    protected Vector3f left;

    /**
     * Direction of 'up' for camera.
     */
    protected Vector3f up;

    /**
     * Direction the camera is facing.
     */
    protected Vector3f direction;

    // focal point of the view, used when zooming
    protected Vector3f focalPoint;
    
    /**
     * Distance from camera to near frustum plane.
     */
    protected float frustumNear;

    /**
     * Distance from camera to far frustum plane.
     */
    protected float frustumFar;

    /**
     * Distance from camera to left frustum plane.
     */
    protected float frustumLeft;

    /**
     * Distance from camera to right frustum plane.
     */
    protected float frustumRight;

    /**
     * Distance from camera to top frustum plane.
     */
    protected float frustumTop;

    /**
     * Distance from camera to bottom frustum plane.
     */
    protected float frustumBottom;

    /**
     * Crop matrix for zooming in, it is only used for shadow maps
     */
    //protected Matrix4f cropMatrix;
    
    //Temporary values computed in onFrustumChange that are needed if a
    //call is made to onFrameChange.
    protected float coeffLeft[];

    protected float coeffRight[];

    protected float coeffBottom[];

    protected float coeffTop[];

    /* Frustum planes always processed for culling. Seems to simply always be 6. */
    protected int planeQuantity;

    //view port coordinates
    /**
     * Percent value on display where horizontal viewing starts for this camera.
     * Default is 0.
     */
    protected float viewPortLeft;

    /**
     * Percent value on display where horizontal viewing ends for this camera.
     * Default is 1.
     */
    protected float viewPortRight;

    /**
     * Percent value on display where vertical viewing ends for this camera.
     * Default is 1.
     */
    protected float viewPortTop;

    /**
     * Percent value on display where vertical viewing begins for this camera.
     * Default is 0.
     */
    protected float viewPortBottom;



    /**
     * Temporary computation vector
     */
    protected Vector3f tmp_vec = new Vector3f();

    
    protected int width;
    protected int height;
    //protected transient Object parent;

    /**
     * store the value for field parallelProjection
     */
    protected boolean parallelProjection;

    private static final Quaternion tmp_quat = new Quaternion(); 
    private final Matrix4f tmp_mat = new Matrix4f();

    private boolean dataOnly;
    
    protected boolean updateModelViewMatrix = true;
    protected Matrix4f modelView = new Matrix4f();
    protected boolean updateProjectionMatrix = true;
    protected Matrix4f projection = new Matrix4f();
    protected boolean updateCalculatedMatrices = true;
    private final Matrix4f modelViewProjectionInverse = new Matrix4f();
    private final Matrix4f modelViewProjection = new Matrix4f();

    
    
    /**
     * Constructor instantiates a new <code>AbstractCamera</code> object. All
     * values of the camera are set to default.
     */
    public ViewCamera() {
        this(false);
    }
    
    public ViewCamera(int height, int width) {
        this(false);
        this.height = height;
        this.width = width;
        //this.parent = parent;
    }
    
    /**
     * Constructor instantiates a new <code>Camera</code> object. All
     * values of the camera are set to default.
     */
    public ViewCamera(boolean dataOnly) {
        setDataOnly(dataOnly);
        location = new Vector3f();
        left = new Vector3f( -1, 0, 0 );
        up = new Vector3f( 0, 1, 0 );
        direction = new Vector3f( 0, 0, -1 );
        focalPoint = new Vector3f();
        
        frustumNear = 1.0f;
        frustumFar = 2.0f;
        frustumLeft = -0.5f;
        frustumRight = 0.5f;
        frustumTop = 0.5f;
        frustumBottom = -0.5f;

        coeffLeft = new float[2];
        coeffRight = new float[2];
        coeffBottom = new float[2];
        coeffTop = new float[2];

        viewPortLeft = 0.0f;
        viewPortRight = 1.0f;
        viewPortTop = 1.0f;
        viewPortBottom = 0.0f;

        planeQuantity = 6;

        worldPlane = new Plane[MAX_WORLD_PLANES];
        for ( int i = 0; i < MAX_WORLD_PLANES; i++ ) {
            worldPlane[i] = new Plane();
        }
        updateModelViewMatrix = true;
        updateProjectionMatrix = true;
    }

    /**
     * <code>getFrustumBottom</code> returns the value of the bottom frustum
     * plane.
     *
     * @return the value of the bottom frustum plane.
     */
    public float getFrustumBottom() {
        return frustumBottom;
    }

    /**
     * <code>setFrustumBottom</code> sets the value of the bottom frustum
     * plane.
     *
     * @param frustumBottom the value of the bottom frustum plane.
     */
    public void setFrustumBottom( float frustumBottom ) {
        this.frustumBottom = frustumBottom;
        updateProjectionMatrix = true;
    }

    /**
     * <code>getFrustumFar</code> gets the value of the far frustum plane.
     *
     * @return the value of the far frustum plane.
     */
    public float getFrustumFar() {
        return frustumFar;
    }

    /**
     * <code>setFrustumFar</code> sets the value of the far frustum plane.
     *
     * @param frustumFar the value of the far frustum plane.
     */
    public void setFrustumFar( float frustumFar ) {
        this.frustumFar = frustumFar;
        updateProjectionMatrix = true;
    }

    /**
     * <code>getFrustumLeft</code> gets the value of the left frustum plane.
     *
     * @return the value of the left frustum plane.
     */
    public float getFrustumLeft() {
        return frustumLeft;
    }

    /**
     * <code>setFrustumLeft</code> sets the value of the left frustum plane.
     *
     * @param frustumLeft the value of the left frustum plane.
     */
    public void setFrustumLeft( float frustumLeft ) {
        this.frustumLeft = frustumLeft;
        updateProjectionMatrix = true;
    }

    /**
     * <code>getFrustumNear</code> gets the value of the near frustum plane.
     *
     * @return the value of the near frustum plane.
     */
    public float getFrustumNear() {
        return frustumNear;
    }

    /**
     * <code>setFrustumNear</code> sets the value of the near frustum plane.
     *
     * @param frustumNear the value of the near frustum plane.
     */
    public void setFrustumNear( float frustumNear ) {
        this.frustumNear = frustumNear;
        updateProjectionMatrix = true;
    }

    /**
     * <code>getFrustumRight</code> gets the value of the right frustum plane.
     *
     * @return frustumRight the value of the right frustum plane.
     */
    public float getFrustumRight() {
        return frustumRight;
    }

    /**
     * <code>setFrustumRight</code> sets the value of the right frustum plane.
     *
     * @param frustumRight the value of the right frustum plane.
     */
    public void setFrustumRight( float frustumRight ) {
        this.frustumRight = frustumRight;
        updateProjectionMatrix = true;
    }

    /**
     * <code>getFrustumTop</code> gets the value of the top frustum plane.
     *
     * @return the value of the top frustum plane.
     */
    public float getFrustumTop() {
        return frustumTop;
    }

    /**
     * <code>setFrustumTop</code> sets the value of the top frustum plane.
     *
     * @param frustumTop the value of the top frustum plane.
     */
    public void setFrustumTop( float frustumTop ) {
        this.frustumTop = frustumTop;
        updateProjectionMatrix = true;
    }

    /**
     * <code>getLocation</code> retrieves the location vector of the camera.
     *
     * @return the position of the camera.
     * @see Camera#getLocation()
     */
    public Vector3f getLocation() {
        return location;
    }

    /**
     * <code>getDirection</code> retrieves the direction vector the camera is
     * facing.
     *
     * @return the direction the camera is facing.
     * @see Camera#getDirection()
     */
    public Vector3f getDirection() {
        return direction;
    }

    public Vector3f getFocalPoint() {
        return focalPoint;
    }
    
    /**
     * <code>getLeft</code> retrieves the left axis of the camera.
     *
     * @return the left axis of the camera.
     * @see Camera#getLeft()
     */
    public Vector3f getLeft() {
        return left;
    }

    /**
     * <code>getUp</code> retrieves the up axis of the camera.
     *
     * @return the up axis of the camera.
     * @see Camera#getUp()
     */
    public Vector3f getUp() {
        return up;
    }

    /**
     * <code>setLocation</code> sets the position of the camera.
     *
     * @param location the position of the camera.
     * @see Camera#setLocation(com.jme.math.Vector3f)
     */
    public void setLocation( Vector3f location ) {
        this.location = location;
        updateModelViewMatrix = true;
    }

    /**
     * <code>setDirection</code> sets the direction this camera is facing. In
     * most cases, this changes the up and left vectors of the camera. If your
     * left or up vectors change, you must updates those as well for correct
     * culling.
     *
     * @param direction the direction this camera is facing.
     * @see Camera#setDirection(com.jme.math.Vector3f)
     */
    public void setDirection( Vector3f direction ) {
        this.direction = direction;
        updateModelViewMatrix = true;
    }

    /**
     * <code>setLeft</code> sets the left axis of this camera. In most cases,
     * this changes the up and direction vectors of the camera. If your
     * direction or up vectors change, you must updates those as well for
     * correct culling.
     *
     * @param left the left axis of this camera.
     * @see Camera#setLeft(com.jme.math.Vector3f)
     */
    public void setLeft( Vector3f left ) {
        this.left = left;
        updateModelViewMatrix = true;
    }

    /**
     * <code>setUp</code> sets the up axis of this camera. In most cases, this
     * changes the direction and left vectors of the camera. If your left or up
     * vectors change, you must updates those as well for correct culling.
     *
     * @param up the up axis of this camera.
     * @see Camera#setUp(com.jme.math.Vector3f)
     */
    public void setUp( Vector3f up ) {
        this.up = up;
        updateModelViewMatrix = true;
    }

    /**
     * <code>setAxes</code> sets the axes (left, up and direction) for this
     * camera.
     *
     * @param left      the left axis of the camera.
     * @param up        the up axis of the camera.
     * @param direction the direction the camera is facing.
     * @see Camera#setAxes(com.jme.math.Vector3f,com.jme.math.Vector3f,com.jme.math.Vector3f)
     */
    public void setAxes( Vector3f left, Vector3f up, Vector3f direction ) {
        this.left = left;
        this.up = up;
        this.direction = direction;
        updateModelViewMatrix = true;
    }

    /**
     * <code>setAxes</code> uses a rotational matrix to set the axes of the
     * camera.
     *
     * @param axes the matrix that defines the orientation of the camera.
     */
    public void setAxes( Quaternion axes ) {
        left = axes.getRotationColumn( 0, left );
        up = axes.getRotationColumn( 1, up );
        direction = axes.getRotationColumn( 2, direction );
        updateModelViewMatrix = true;
    }

    /**
     * normalize normalizes the camera vectors.
     */
    public void normalize() {
        left.normalizeLocal();
        up.normalizeLocal();
        direction.normalizeLocal();
        updateModelViewMatrix = true;
    }

    /**
     * <code>setFrustum</code> sets the frustum of this camera object.
     *
     * @param near   the near plane.
     * @param far    the far plane.
     * @param left   the left plane.
     * @param right  the right plane.
     * @param top    the top plane.
     * @param bottom the bottom plane.
     * @see Camera#setFrustum(float, float, float, float,
     *      float, float)
     */
    public void setFrustum( float near, float far, float left, float right,
                            float top, float bottom ) {

        frustumNear = near;
        frustumFar = far;
        frustumLeft = left;
        frustumRight = right;
        frustumTop = top;
        frustumBottom = bottom;
        this.updateProjectionMatrix = true;
    }

    public void setFrustumPerspective( float fovY, float aspect, float near,
                                       float far ) {
        // fixed
        float h = FastMath.tan( fovY * FastMath.DEG_TO_RAD *.5f ) * near;
        float w = h * aspect;
        frustumLeft = -w;
        frustumRight = w;
        frustumBottom = -h;
        frustumTop = h;
        frustumNear = near;
        frustumFar = far;

        this.updateProjectionMatrix = true;
    }

    /**
     * <code>setFrame</code> sets the orientation and location of the camera.
     *
     * @param location  the point position of the camera.
     * @param left      the left axis of the camera.
     * @param up        the up axis of the camera.
     * @param direction the facing of the camera.
     * @see Camera#setFrame(com.jme.math.Vector3f,
     *      com.jme.math.Vector3f, com.jme.math.Vector3f, com.jme.math.Vector3f)
     */
    public void setFrame( Vector3f location, Vector3f left, Vector3f up,
                          Vector3f direction ) {

        this.location = location;
        this.left = left;
        this.up = up;
        this.direction = direction;
        this.updateModelViewMatrix = true;
    }

    /**
     * <code>lookAt</code> is a convienence method for auto-setting the frame
     * based on a world position the user desires the camera to look at. It
     * repoints the camera towards the given position using the difference
     * between the position and the current camera location as a direction
     * vector and the worldUpVector to compute up and left camera vectors.
     *
     * @param pos           where to look at in terms of world coordinates
     * @param worldUpVector a normalized vector indicating the up direction of the world.
     *                      (typically {0, 1, 0} in jME.)
     */
    public void lookAt( Vector3f pos, Vector3f worldUpVector ) {
        tmp_vec.set( pos ).subtractLocal( location );
        lookDirection(tmp_vec, worldUpVector);
    }

    public void lookDirection( Vector3f dir, Vector3f worldUpVector ) {
        tmp_vec.set( dir ).normalizeLocal();
        // check to see if we haven't really updated camera -- no need to call
        // sets.
        if ( tmp_vec.equals( direction ) ) {
            return;
        }
        direction.set( tmp_vec  );

        up.set(worldUpVector).normalizeLocal();
        if (up.equals(Vector3f.ZERO))
            up.set(Vector3f.UNIT_Y);
        left.set(up).crossLocal(direction).normalizeLocal();
        if (left.equals(Vector3f.ZERO)) {
            if (direction.x != 0) {
                left.set(direction.y, -direction.x, 0f);
            } else {
                left.set(0f, direction.z, -direction.y);
            }
        }
        up.set(direction).crossLocal(left).normalizeLocal();
        updateModelViewMatrix = true;
    }
    
    /**
     * <code>setFrame</code> sets the orientation and location of the camera.
     * 
     * @param location
     *            the point position of the camera.
     * @param axes
     *            the orientation of the camera.
     */
    public void setFrame( Vector3f location, Quaternion axes ) {
        this.location = location;
        left = axes.getRotationColumn( 0, left );
        up = axes.getRotationColumn( 1, up );
        direction = axes.getRotationColumn( 2, direction );
        updateModelViewMatrix = true;
    }

    /**
     * <code>update</code> updates the camera parameters by calling
     * <code>onFrustumChange</code>,<code>onViewPortChange</code> and
     * <code>onFrameChange</code>.
     *
     * @see Camera#update()
     */
    public void update() {
        onFrustumChange();
        //onViewPortChange();
        onFrameChange();
        // recalculate focal point
        focalPoint.set(this.direction).normalizeLocal().multLocal(this.frustumNear).addLocal(this.location);
    }


    /**
     * <code>getViewPortLeft</code> gets the left boundary of the viewport
     *
     * @return the left boundary of the viewport
     */
    public float getViewPortLeft() {
        return viewPortLeft;
    }

    /**
     * <code>setViewPortLeft</code> sets the left boundary of the viewport
     *
     * @param left the left boundary of the viewport
     */
    public void setViewPortLeft( float left ) {
        viewPortLeft = left;
    }

    /**
     * <code>getViewPortRight</code> gets the right boundary of the viewport
     *
     * @return the right boundary of the viewport
     */
    public float getViewPortRight() {
        return viewPortRight;
    }

    /**
     * <code>setViewPortRight</code> sets the right boundary of the viewport
     *
     * @param right the right boundary of the viewport
     */
    public void setViewPortRight( float right ) {
        viewPortRight = right;
    }

    /**
     * <code>getViewPortTop</code> gets the top boundary of the viewport
     *
     * @return the top boundary of the viewport
     */
    public float getViewPortTop() {
        return viewPortTop;
    }

    /**
     * <code>setViewPortTop</code> sets the top boundary of the viewport
     *
     * @param top the top boundary of the viewport
     */
    public void setViewPortTop( float top ) {
        viewPortTop = top;
    }

    /**
     * <code>getViewPortBottom</code> gets the bottom boundary of the viewport
     *
     * @return the bottom boundary of the viewport
     */
    public float getViewPortBottom() {
        return viewPortBottom;
    }

    /**
     * <code>setViewPortBottom</code> sets the bottom boundary of the viewport
     *
     * @param bottom the bottom boundary of the viewport
     */
    public void setViewPortBottom( float bottom ) {
        viewPortBottom = bottom;
    }

    /**
     * <code>setViewPort</code> sets the boundaries of the viewport
     *
     * @param left   the left boundary of the viewport
     * @param right  the right boundary of the viewport
     * @param bottom the bottom boundary of the viewport
     * @param top    the top boundary of the viewport
     */
    public void setViewPort( float left, float right, float bottom, float top ) {
        setViewPortLeft( left );
        setViewPortRight( right );
        setViewPortBottom( bottom );
        setViewPortTop( top );
        
    }


    /**
     * <code>onFrustumChange</code> updates the frustum to reflect any changes
     * made to the planes. The new frustum values are kept in a temporary
     * location for use when calculating the new frame. It should be noted that
     * the abstract implementation of this class only updates the data, and does
     * not make any rendering calls. As such, any impelmenting subclass should
     * insure to override this method call it with super and then call the
     * rendering specific code.
     */
    public void onFrustumChange() {
        if ( !isParallelProjection() ) {
            float nearSquared = frustumNear * frustumNear;
            float leftSquared = frustumLeft * frustumLeft;
            float rightSquared = frustumRight * frustumRight;
            float bottomSquared = frustumBottom * frustumBottom;
            float topSquared = frustumTop * frustumTop;

            float inverseLength = FastMath.invSqrt( nearSquared + leftSquared );
            coeffLeft[0] = frustumNear * inverseLength;
            coeffLeft[1] = -frustumLeft * inverseLength;

            inverseLength = FastMath.invSqrt( nearSquared + rightSquared );
            coeffRight[0] = -frustumNear * inverseLength;
            coeffRight[1] = frustumRight * inverseLength;

            inverseLength = FastMath.invSqrt( nearSquared + bottomSquared );
            coeffBottom[0] = frustumNear * inverseLength;
            coeffBottom[1] = -frustumBottom * inverseLength;

            inverseLength = FastMath.invSqrt( nearSquared + topSquared );
            coeffTop[0] = -frustumNear * inverseLength;
            coeffTop[1] = frustumTop * inverseLength;
        }
        else {
            coeffLeft[0] = 1;
            coeffLeft[1] = 0;

            coeffRight[0] = -1;
            coeffRight[1] = 0;

            coeffBottom[0] = 1;
            coeffBottom[1] = 0;

            coeffTop[0] = -1;
            coeffTop[1] = 0;
        }

        this.updateProjectionMatrix = true;
    }

    /**
     * <code>onFrameChange</code> updates the view frame of the camera. It
     * should be noted that the abstract implementation of this class only
     * updates the data, and does not make any rendering calls. As such, any
     * implementing subclass should insure to override this method call it with
     * super and then call the rendering specific code.
     */
    public void onFrameChange() {
        float dirDotLocation = direction.dot( location );

        // left plane
        Vector3f leftPlaneNormal = worldPlane[LEFT_PLANE].normal;
        leftPlaneNormal.x = left.x * coeffLeft[0];
        leftPlaneNormal.y = left.y * coeffLeft[0];
        leftPlaneNormal.z = left.z * coeffLeft[0];
        leftPlaneNormal.addLocal( direction.x * coeffLeft[1], direction.y
                * coeffLeft[1], direction.z * coeffLeft[1] );
        worldPlane[LEFT_PLANE].setConstant( location.dot( leftPlaneNormal ) );

        // right plane
        Vector3f rightPlaneNormal = worldPlane[RIGHT_PLANE].normal;
        rightPlaneNormal.x = left.x * coeffRight[0];
        rightPlaneNormal.y = left.y * coeffRight[0];
        rightPlaneNormal.z = left.z * coeffRight[0];
        rightPlaneNormal.addLocal( direction.x * coeffRight[1], direction.y
                * coeffRight[1], direction.z * coeffRight[1] );
        worldPlane[RIGHT_PLANE].setConstant( location.dot( rightPlaneNormal ) );

        // bottom plane
        Vector3f bottomPlaneNormal = worldPlane[BOTTOM_PLANE].normal;
        bottomPlaneNormal.x = up.x * coeffBottom[0];
        bottomPlaneNormal.y = up.y * coeffBottom[0];
        bottomPlaneNormal.z = up.z * coeffBottom[0];
        bottomPlaneNormal.addLocal( direction.x * coeffBottom[1], direction.y
                * coeffBottom[1], direction.z * coeffBottom[1] );
        worldPlane[BOTTOM_PLANE].setConstant( location.dot( bottomPlaneNormal ) );

        // top plane
        Vector3f topPlaneNormal = worldPlane[TOP_PLANE].normal;
        topPlaneNormal.x = up.x * coeffTop[0];
        topPlaneNormal.y = up.y * coeffTop[0];
        topPlaneNormal.z = up.z * coeffTop[0];
        topPlaneNormal.addLocal( direction.x * coeffTop[1], direction.y
                * coeffTop[1], direction.z * coeffTop[1] );
        worldPlane[TOP_PLANE].setConstant( location.dot( topPlaneNormal ) );

        if ( isParallelProjection() ) {
            worldPlane[LEFT_PLANE].setConstant( worldPlane[LEFT_PLANE].getConstant() + frustumLeft );
            worldPlane[RIGHT_PLANE].setConstant( worldPlane[RIGHT_PLANE].getConstant() - frustumRight );
            worldPlane[TOP_PLANE].setConstant( worldPlane[TOP_PLANE].getConstant() + frustumTop );
            worldPlane[BOTTOM_PLANE].setConstant( worldPlane[BOTTOM_PLANE].getConstant() - frustumBottom );
        }

        // far plane
        worldPlane[FAR_PLANE].normal.set( -direction.x, -direction.y,
                -direction.z );
        worldPlane[FAR_PLANE].setConstant( -( dirDotLocation + frustumFar ) );

        // near plane
        worldPlane[NEAR_PLANE].normal
                .set( direction.x, direction.y, direction.z );
        worldPlane[NEAR_PLANE].setConstant( dirDotLocation + frustumNear );

        updateModelViewMatrix = true;
    }

    /**
     * @return true if parallel projection is enable, false if in normal perspective mode
     * @see #setParallelProjection(boolean)
     */
    public boolean isParallelProjection() {
        return this.parallelProjection;
    }

    /**
     * Enable/disable parallel projection.
     *
     * @param value true to set up this camera for parallel projection is enable, false to enter normal perspective mode
     */
    public void setParallelProjection( final boolean value ) {
        this.parallelProjection = value;
    }

    /* @see Camera#getWorldCoordinates */
    public Vector3f getWorldCoordinates( Vector2f screenPos, float zPos ) {
        return getWorldCoordinates( screenPos, zPos, null );
    }



    /* @see Camera#getWorldCoordinates */
    public Vector3f getWorldCoordinates( Vector2f screenPosition,
                                         float zPos, Vector3f store ) {
        if ( store == null ) {
            store = new Vector3f();
        }
        checkViewProjection();

        tmp_quat.set(
                ( screenPosition.x / getWidth() - viewPortLeft ) / ( viewPortRight - viewPortLeft ) * 2 - 1,
                ( screenPosition.y / getHeight() - viewPortBottom ) / ( viewPortTop - viewPortBottom ) * 2 - 1,
                zPos * 2 - 1, 1 );
        modelViewProjectionInverse.mult( tmp_quat, tmp_quat );
        tmp_quat.multLocal( 1.0f / tmp_quat.w );
        store.x = tmp_quat.x;
        store.y = tmp_quat.y;
        store.z = tmp_quat.z;
        return store;
    }

    /* @see Camera#getScreenCoordinates */
    public Vector3f getScreenCoordinates( Vector3f worldPos ) {
        return getScreenCoordinates( worldPos, null );
    }

    public Matrix4f getProjectionMatrix() {
        checkViewProjection();
        return this.projection;
    }

    public Matrix4f getModelViewMatrix() {
        checkViewProjection();
        return this.modelView;
    }

    /**
     * Implementation contributed by Zbyl.
     *
     * @see Camera#getScreenCoordinates(Vector3f, Vector3f)
     */
    public Vector3f getScreenCoordinates( Vector3f worldPosition, Vector3f store ) {
        if ( store == null ) {
            store = new Vector3f();
        }
        checkViewProjection();
        tmp_quat.set( worldPosition.x, worldPosition.y, worldPosition.z, 1 );
        modelViewProjection.mult( tmp_quat, tmp_quat );
        tmp_quat.multLocal( 1.0f / tmp_quat.w );
        store.x = ( ( tmp_quat.x + 1 ) * ( viewPortRight - viewPortLeft ) / 2 + viewPortLeft ) * getWidth();
        store.y = ( ( tmp_quat.y + 1 ) * ( viewPortTop - viewPortBottom ) / 2 + viewPortBottom ) * getHeight();
        store.z = ( tmp_quat.z + 1 ) / 2;

        return store;
    }

    /**
     * update modelViewProjection if necessary.
     */
    protected void checkViewProjection() {
        if(updateModelViewMatrix) {
            // calculate modelview
            tmp_vec.set(location).addLocal(direction);
            modelView.lookAt(location, tmp_vec, up);
            modelView.transposeLocal();

            updateCalculatedMatrices = true;
            updateModelViewMatrix = false;
        }
        if ( updateProjectionMatrix ) {
            // calculateprojection matrix
            
            // use as tmp variable
            //if(cropMatrix!=null) {
            //    projection.set(cropMatrix);
            //    tmp_mat.setFrustum(frustumLeft, frustumRight, frustumBottom, frustumTop, frustumNear, frustumFar);
            //    projection.multLocal(tmp_mat);
            //} else {
                projection.setFrustum(frustumLeft, frustumRight, frustumBottom, frustumTop, frustumNear, frustumFar);
                projection.transposeLocal();
            //}
            updateCalculatedMatrices = true;
            updateProjectionMatrix = false;
        }
        if(updateCalculatedMatrices) {
            
            modelViewProjection.set( modelView ).multLocal( projection );
            modelViewProjection.invert( modelViewProjectionInverse );
            updateCalculatedMatrices = false;
        }
    }

    /**
     * @return the width/resolution of the display.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the height/resolution of the display.
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * <code>resize</code> resizes this cameras view with the given width/height.
     * This is similar to constructing a new camera, but reusing the same
     * Object.
     * @param width int
     * @param height int
     */
    public void resize(int width, int height) {
      this.width = width;
      this.height = height;
    }
    
    public void setDataOnly(boolean dataOnly) {
        this.dataOnly = dataOnly;
    }
    
    public boolean isDataOnly() {
        return dataOnly;
    }
    
    /**
     * Returns the 8 points comprising the bound of the camera frustum.
     * @param points    an Vector3f[8] array that will hold the frustum corners
     * @param center    the center of the frustum, the points are relative to this point
     */
    public void getFrustumCorners(Vector3f[] points, Vector3f center) {
        
        float distAspect = frustumFar / frustumNear;
        
        float frustumTopFar =  frustumTop * distAspect;
	float frustumRightFar = frustumRight * distAspect;

        Vector3f vNearPlaneCenter = new Vector3f();
        vNearPlaneCenter.set(direction).multLocal(frustumNear).addLocal(location);
                
	Vector3f vFarPlaneCenter = new Vector3f();
        vFarPlaneCenter.set(direction).multLocal(frustumFar).addLocal(location);

        // create vectors in the array if not yet exists
        for(int i=0; i<8; i++)
            if(points[i]==null)
                points[i] = new Vector3f();

	points[0].set(vNearPlaneCenter)
                .addScaledLocal(left, -frustumRight)
                .addScaledLocal(up, -frustumTop);
	points[1].set(vNearPlaneCenter)
                .addScaledLocal(left, -frustumRight)
                .addScaledLocal(up, +frustumTop);
	points[2].set(vNearPlaneCenter)
                .addScaledLocal(left, +frustumRight)
                .addScaledLocal(up, +frustumTop);
	points[3].set(vNearPlaneCenter)
                .addScaledLocal(left, +frustumRight)
                .addScaledLocal(up, -frustumTop);
	
	points[4].set(vFarPlaneCenter)
                .addScaledLocal(left, -frustumRightFar)
                .addScaledLocal(up, -frustumTopFar);
	points[5].set(vFarPlaneCenter)
                .addScaledLocal(left, -frustumRightFar)
                .addScaledLocal(up, +frustumTopFar);
	points[6].set(vFarPlaneCenter)
                .addScaledLocal(left, +frustumRightFar)
                .addScaledLocal(up, +frustumTopFar);
	points[7].set(vFarPlaneCenter)
                .addScaledLocal(left, +frustumRightFar)
                .addScaledLocal(up, -frustumTopFar);

	center.set(0.0f, 0.0f, 0.0f);
	for(int i = 0; i < 8; i++) 
		center.addLocal(points[i]);

        center.multLocal(1f/8f);

        // reposition points arond the new center
	for(int i = 0; i < 8; i++)
            points[i].subtractLocal(center);
    }

    /**
     * Zooms in the camera frustum to zoom on a given set of points.
     * It is required to call lookAt first on the location of the object,
     * because this method supposes that camera already looks at the object.
     * 
     * @param points    The points in local coordinates
     */
    public void setZoom(Vector3f[] points) {
        float fMaxX = 0;
        float fMaxY = 0;
        float fMinX =  width;
        float fMinY =  height;
	float fMaxZ =  -1.0f;
        
        /*
        if(cropMatrix==null)
            cropMatrix = new Matrix4f();
        else
            cropMatrix.loadIdentity();
         */
        
        // ensure that matrices are up to date
        checkViewProjection();
        
        // construct a dummy modelview matrix
        Matrix4f dmodelviewproj = new Matrix4f();
        //dmodelviewproj.set(projection).multLocal(modelView);
        
        //dmodelviewproj.lookAt(location, Vector3f.ZERO, up);
        dmodelviewproj.set(modelViewProjection);
        //dmodelviewproj.multLocal(this.projection);

        // find the min and max of transformed points
        for(int i = 0; i < points.length; i++) {
            tmp_quat.set(points[i].x, points[i].y, points[i].z, 1.0f);
            
            dmodelviewproj.mult(tmp_quat, tmp_quat);

            //tmp_quat.set( worldPosition.x, worldPosition.y, worldPosition.z, 1 );
            //modelViewProjection.mult( tmp_quat, tmp_quat );
            // We project the x and y values prior to determining the max values
            tmp_quat.x /= tmp_quat.w;
            tmp_quat.y /= tmp_quat.w;
            
            //tmp_quat.multLocal( 1.0f / tmp_quat.w );
            tmp_quat.x = ( ( tmp_quat.x + 1 ) * ( viewPortRight - viewPortLeft ) / 2 + viewPortLeft ) * width;
            tmp_quat.y = ( ( tmp_quat.y + 1 ) * ( viewPortTop - viewPortBottom ) / 2 + viewPortBottom ) * height;
            //tmp_quat.z = ( tmp_quat.z + 1 ) / 2;

            // We find the min and max values for X, Y and Z
            if(tmp_quat.x > fMaxX) fMaxX = tmp_quat.x;
            if(tmp_quat.y > fMaxY) fMaxY = tmp_quat.y;
            if(tmp_quat.y < fMinY) fMinY = tmp_quat.y;
            if(tmp_quat.x < fMinX) fMinX = tmp_quat.x;
            if(tmp_quat.z > fMaxZ) fMaxZ = tmp_quat.z;
	}

        /*
        fMaxX = FastMath.clamp(fMaxX, -1.0f, 1.0f);
        fMaxY = FastMath.clamp(fMaxY, -1.0f, 1.0f);
        fMinX = FastMath.clamp(fMinX, -1.0f, 1.0f);
        fMinY = FastMath.clamp(fMinY, -1.0f, 1.0f);
         */

        // clamp and scale to -1 1
        fMaxX = FastMath.clamp(fMaxX, 0, width);
        fMaxY = FastMath.clamp(fMaxY, 0, height);
        fMinX = FastMath.clamp(fMinX, 0, width);
        fMinY = FastMath.clamp(fMinY, 0, height);

        // the new far plane
        frustumFar = fMaxZ + 1.0f + 1.5f;

        /*
	float fScaleX = 2.0f  / (fMaxX-fMinX);
	float fScaleY = 2.0f  / (fMaxY-fMinY);
	float fOffsetX = -0.5f * (fMaxX+fMinX) * fScaleX;
	float fOffsetY = -0.5f * (fMaxY+fMinY) * fScaleY;
         */

        // the biggest extent from the center of the screen
        // in the X directon
        float wh = width/2;
        float mextx = 0;
        if(fMinX<wh) {
            mextx=Math.max(mextx, wh-fMinX);
        }
        if(fMaxX>wh) {
            mextx=Math.max(mextx, fMaxX-wh);
        }

        // in the Y direction
        float hh = height/2;
        float mexty = 0;
        if(fMinY<hh) {
            mexty=Math.max(mexty, hh-fMinY);
        }
        if(fMaxY>hh) {
            mexty=Math.max(mexty, fMaxY-hh);
        }

        // get how much we could relatively increase in X direction
        float xscale = wh/mextx;
        float yscale = hh/mexty;
        
        // get the less scale as the one that is safe to apply
        // scale the frustum
        xscale = 1f / xscale;
        yscale = 1f / yscale;
        
        float scale = xscale;
        if(scale < yscale)
            scale = yscale;

        frustumRight = frustumRight*scale;
        frustumLeft = -frustumRight;
        frustumTop = frustumTop*scale;
        frustumBottom = -frustumTop;

        this.updateProjectionMatrix = true;
    }

    /*
    public void clearZoom() {
        cropMatrix.loadIdentity();
    }
     */

    public ViewCamera copy(ViewCamera other) {
        other.coeffBottom[0] = this.coeffBottom[0];
        other.coeffBottom[1] = this.coeffBottom[1];
        other.coeffLeft[0] = this.coeffLeft[0];
        other.coeffLeft[1] = this.coeffLeft[1];
        other.coeffRight[0] = this.coeffRight[0];
        other.coeffRight[1] = this.coeffRight[1];
        other.coeffTop[0] = this.coeffTop[0];
        other.coeffTop[1] = this.coeffTop[1];
        
        other.direction.set(this.direction);
        other.left.set(this.left);
        other.up.set(this.up);
        other.location.set(this.location);
        other.focalPoint.set(this.focalPoint);
        
        other.frustumBottom = this.frustumBottom;
        other.frustumFar = this.frustumFar;
        other.frustumNear = this.frustumNear;
        other.frustumRight = this.frustumRight;
        other.frustumLeft = this.frustumLeft;
        other.frustumTop = this.frustumTop;
        
        other.height = this.height;
        other.width = this.width;
        
        other.modelView.set(this.modelView);
        other.modelViewProjection.set(this.modelViewProjection);
        other.modelViewProjectionInverse.set(this.modelViewProjectionInverse);
        other.projection.set(this.projection);
        /*
        // copy the Zoom-in matrix
        if(this.cropMatrix!=null) {
            if(other.cropMatrix==null)
                other.cropMatrix = new Matrix4f();
            other.cropMatrix.set(cropMatrix);
        } else {
            if(other.cropMatrix!=null)
                other.cropMatrix.loadIdentity();
        }
         */
        
        other.parallelProjection = this.parallelProjection;
        other.planeQuantity = this.planeQuantity;
        other.planeState = this.planeState;
        
        other.updateModelViewMatrix = this.updateModelViewMatrix;
        other.updateProjectionMatrix = this.updateProjectionMatrix;
        other.updateCalculatedMatrices = this.updateCalculatedMatrices;
        
        other.viewPortBottom = this.viewPortBottom;
        other.viewPortLeft = this.viewPortLeft;
        other.viewPortRight = this.viewPortRight;
        other.viewPortTop = this.viewPortTop;
        other.viewId = this.viewId;
        for ( int i = 0; i < MAX_WORLD_PLANES; i++ ) {
            other.worldPlane[i].set(worldPlane[i]);
        }
        return other;
    }
    
    public void setViewId( int id ) {
        this.viewId = id;
    }
}
