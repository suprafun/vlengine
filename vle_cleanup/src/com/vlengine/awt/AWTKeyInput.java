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

package com.vlengine.awt;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Logger;

import com.vlengine.input.KeyInput;
import com.vlengine.util.BitSet;
import com.vlengine.util.FastList;
import java.awt.Canvas;

/**
 * <code>AWTKeyInput</code>
 *
 * @author Joshua Slack
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class AWTKeyInput extends KeyInput implements KeyListener {
    private static final Logger logger = Logger.getLogger(AWTKeyInput.class.getName());

    FastList<KeyEvent> events = new FastList<KeyEvent>();
    BitSet keyDown = new BitSet( 256 );
    private boolean enabled = true;

    public AWTKeyInput() {
    }

    public boolean isKeyDown( int key ) {
        return keyDown.contains( key );
    }

    public String getKeyName( int key ) {
        return KeyEvent.getKeyText( toAWTCode( key ) );
    }

    public int getKeyIndex( String name ) {
        throw new UnsupportedOperationException( "getKeyIndex is not supported by AWTKeyInput." );
    }

    public void update() {
        if ( input != null ) {
            for ( int x = 0; x < events.size(); x++ ) {
                KeyEvent e = events.get( x );
                char c = e.getKeyChar();
                int keyCode = toInputCode( e.getKeyCode() );
                boolean pressed = e.getID() == KeyEvent.KEY_PRESSED;
                input.onKey( c, keyCode, pressed );
            }
        }

        events.clear();
    }

    public void destroyIfInitialized() {
        ; // nothing to do
    }

    /**
     * @return Returns the enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled The enabled to set.
     */
    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

    // **********************************
    // java.awt.event.KeyListener methods
    // **********************************

    public void keyTyped( KeyEvent arg0 ) {
        ; // ignore this one
    }

    public void keyPressed( KeyEvent arg0 ) {
        if ( !enabled ) {
            return;
        }

        events.add( arg0 );
        keyDown.set( toInputCode( arg0.getKeyCode() ), true );
    }

    public void keyReleased( KeyEvent arg0 ) {
        if ( !enabled ) {
            return;
        }

        events.add( arg0 );
        keyDown.set( toInputCode( arg0.getKeyCode() ), false );
    }

    /**
     * <code>toAWTCode</code> converts KeyInput key codes to AWT key codes.
     *
     * @param key jme KeyInput key code
     * @return awt KeyEvent key code
     */
    public int toAWTCode( int key ) {
        switch ( key ) {
            case KEY_ESCAPE:
                return KeyEvent.VK_ESCAPE;
            case KEY_1:
                return KeyEvent.VK_1;
            case KEY_2:
                return KeyEvent.VK_2;
            case KEY_3:
                return KeyEvent.VK_3;
            case KEY_4:
                return KeyEvent.VK_4;
            case KEY_5:
                return KeyEvent.VK_5;
            case KEY_6:
                return KeyEvent.VK_6;
            case KEY_7:
                return KeyEvent.VK_7;
            case KEY_8:
                return KeyEvent.VK_8;
            case KEY_9:
                return KeyEvent.VK_9;
            case KEY_0:
                return KeyEvent.VK_0;
            case KEY_MINUS:
                return KeyEvent.VK_MINUS;
            case KEY_EQUALS:
                return KeyEvent.VK_EQUALS;
            case KEY_BACK:
                return KeyEvent.VK_BACK_SPACE;
            case KEY_TAB:
                return KeyEvent.VK_TAB;
            case KEY_Q:
                return KeyEvent.VK_Q;
            case KEY_W:
                return KeyEvent.VK_W;
            case KEY_E:
                return KeyEvent.VK_E;
            case KEY_R:
                return KeyEvent.VK_R;
            case KEY_T:
                return KeyEvent.VK_T;
            case KEY_Y:
                return KeyEvent.VK_Y;
            case KEY_U:
                return KeyEvent.VK_U;
            case KEY_I:
                return KeyEvent.VK_I;
            case KEY_O:
                return KeyEvent.VK_O;
            case KEY_P:
                return KeyEvent.VK_P;
            case KEY_LBRACKET:
                return KeyEvent.VK_OPEN_BRACKET;
            case KEY_RBRACKET:
                return KeyEvent.VK_CLOSE_BRACKET;
            case KEY_RETURN:
                return KeyEvent.VK_ENTER;
            case KEY_LCONTROL:
                return KeyEvent.VK_CONTROL;
            case KEY_A:
                return KeyEvent.VK_A;
            case KEY_S:
                return KeyEvent.VK_S;
            case KEY_D:
                return KeyEvent.VK_D;
            case KEY_F:
                return KeyEvent.VK_F;
            case KEY_G:
                return KeyEvent.VK_G;
            case KEY_H:
                return KeyEvent.VK_H;
            case KEY_J:
                return KeyEvent.VK_J;
            case KEY_K:
                return KeyEvent.VK_K;
            case KEY_L:
                return KeyEvent.VK_L;
            case KEY_SEMICOLON:
                return KeyEvent.VK_SEMICOLON;
            case KEY_APOSTROPHE:
                return KeyEvent.VK_QUOTE;
            case KEY_GRAVE:
                return KeyEvent.VK_DEAD_GRAVE;
            case KEY_LSHIFT:
                return KeyEvent.VK_SHIFT;
            case KEY_BACKSLASH:
                return KeyEvent.VK_BACK_SLASH;
            case KEY_Z:
                return KeyEvent.VK_Z;
            case KEY_X:
                return KeyEvent.VK_X;
            case KEY_C:
                return KeyEvent.VK_C;
            case KEY_V:
                return KeyEvent.VK_V;
            case KEY_B:
                return KeyEvent.VK_B;
            case KEY_N:
                return KeyEvent.VK_N;
            case KEY_M:
                return KeyEvent.VK_M;
            case KEY_COMMA:
                return KeyEvent.VK_COMMA;
            case KEY_PERIOD:
                return KeyEvent.VK_PERIOD;
            case KEY_SLASH:
                return KeyEvent.VK_SLASH;
            case KEY_RSHIFT:
                return KeyEvent.VK_SHIFT;
            case KEY_MULTIPLY:
                return KeyEvent.VK_MULTIPLY;
            case KEY_SPACE:
                return KeyEvent.VK_SPACE;
            case KEY_CAPITAL:
                return KeyEvent.VK_CAPS_LOCK;
            case KEY_F1:
                return KeyEvent.VK_F1;
            case KEY_F2:
                return KeyEvent.VK_F2;
            case KEY_F3:
                return KeyEvent.VK_F3;
            case KEY_F4:
                return KeyEvent.VK_F4;
            case KEY_F5:
                return KeyEvent.VK_F5;
            case KEY_F6:
                return KeyEvent.VK_F6;
            case KEY_F7:
                return KeyEvent.VK_F7;
            case KEY_F8:
                return KeyEvent.VK_F8;
            case KEY_F9:
                return KeyEvent.VK_F9;
            case KEY_F10:
                return KeyEvent.VK_F10;
            case KEY_NUMLOCK:
                return KeyEvent.VK_NUM_LOCK;
            case KEY_SCROLL:
                return KeyEvent.VK_SCROLL_LOCK;
            case KEY_NUMPAD7:
                return KeyEvent.VK_NUMPAD7;
            case KEY_NUMPAD8:
                return KeyEvent.VK_NUMPAD8;
            case KEY_NUMPAD9:
                return KeyEvent.VK_NUMPAD9;
            case KEY_SUBTRACT:
                return KeyEvent.VK_SUBTRACT;
            case KEY_NUMPAD4:
                return KeyEvent.VK_NUMPAD4;
            case KEY_NUMPAD5:
                return KeyEvent.VK_NUMPAD5;
            case KEY_NUMPAD6:
                return KeyEvent.VK_NUMPAD6;
            case KEY_ADD:
                return KeyEvent.VK_ADD;
            case KEY_NUMPAD1:
                return KeyEvent.VK_NUMPAD1;
            case KEY_NUMPAD2:
                return KeyEvent.VK_NUMPAD2;
            case KEY_NUMPAD3:
                return KeyEvent.VK_NUMPAD3;
            case KEY_NUMPAD0:
                return KeyEvent.VK_NUMPAD0;
            case KEY_DECIMAL:
                return KeyEvent.VK_DECIMAL;
            case KEY_F11:
                return KeyEvent.VK_F11;
            case KEY_F12:
                return KeyEvent.VK_F12;
            case KEY_F13:
                return KeyEvent.VK_F13;
            case KEY_F14:
                return KeyEvent.VK_F14;
            case KEY_F15:
                return KeyEvent.VK_F15;
            case KEY_KANA:
                return KeyEvent.VK_KANA;
            case KEY_CONVERT:
                return KeyEvent.VK_CONVERT;
            case KEY_NOCONVERT:
                return KeyEvent.VK_NONCONVERT;
            case KEY_NUMPADEQUALS:
                return KeyEvent.VK_EQUALS;
            case KEY_CIRCUMFLEX:
                return KeyEvent.VK_CIRCUMFLEX;
            case KEY_AT:
                return KeyEvent.VK_AT;
            case KEY_COLON:
                return KeyEvent.VK_COLON;
            case KEY_UNDERLINE:
                return KeyEvent.VK_UNDERSCORE;
            case KEY_STOP:
                return KeyEvent.VK_STOP;
            case KEY_NUMPADENTER:
                return KeyEvent.VK_ENTER;
            case KEY_RCONTROL:
                return KeyEvent.VK_CONTROL;
            case KEY_NUMPADCOMMA:
                return KeyEvent.VK_COMMA;
            case KEY_DIVIDE:
                return KeyEvent.VK_DIVIDE;
            case KEY_PAUSE:
                return KeyEvent.VK_PAUSE;
            case KEY_HOME:
                return KeyEvent.VK_HOME;
            case KEY_UP:
                return KeyEvent.VK_UP;
            case KEY_PRIOR:
                return KeyEvent.VK_PAGE_UP;
            case KEY_LEFT:
                return KeyEvent.VK_LEFT;
            case KEY_RIGHT:
                return KeyEvent.VK_RIGHT;
            case KEY_END:
                return KeyEvent.VK_END;
            case KEY_DOWN:
                return KeyEvent.VK_DOWN;
            case KEY_NEXT:
                return KeyEvent.VK_PAGE_DOWN;
            case KEY_INSERT:
                return KeyEvent.VK_INSERT;
            case KEY_DELETE:
                return KeyEvent.VK_DELETE;
            case KEY_LMENU:
                return KeyEvent.VK_ALT; //todo: location left
            case KEY_RMENU:
                return KeyEvent.VK_ALT; //todo: location right
        }
        logger.warning("unsupported key:" + key);
        return 0x10000 + key;
    }

    /**
     * <code>getKeyParam</code> returns the KEY_XXXXX param related to the code
     * given.
     *
     * @param key jme KeyInput key code
     * @return param name for given code.
     */
    public String getKeyParam( int key ) {
        switch ( key ) {
            case KEY_ESCAPE:
                return "KEY_ESCAPE";
            case KEY_1:
                return "KEY_1";
            case KEY_2:
                return "KEY_2";
            case KEY_3:
                return "KEY_3";
            case KEY_4:
                return "KEY_4";
            case KEY_5:
                return "KEY_5";
            case KEY_6:
                return "KEY_6";
            case KEY_7:
                return "KEY_7";
            case KEY_8:
                return "KEY_8";
            case KEY_9:
                return "KEY_9";
            case KEY_0:
                return "KEY_0";
            case KEY_MINUS:
                return "KEY_MINUS";
            case KEY_EQUALS:
                return "KEY_EQUALS";
            case KEY_BACK:
                return "KEY_BACK";
            case KEY_TAB:
                return "KEY_TAB";
            case KEY_Q:
                return "KEY_Q";
            case KEY_W:
                return "KEY_W";
            case KEY_E:
                return "KEY_E";
            case KEY_R:
                return "KEY_R";
            case KEY_T:
                return "KEY_T";
            case KEY_Y:
                return "KEY_Y";
            case KEY_U:
                return "KEY_U";
            case KEY_I:
                return "KEY_I";
            case KEY_O:
                return "KEY_O";
            case KEY_P:
                return "KEY_P";
            case KEY_LBRACKET:
                return "KEY_LBRACKET";
            case KEY_RBRACKET:
                return "KEY_RBRACKET";
            case KEY_RETURN:
                return "KEY_RETURN";
            case KEY_LCONTROL:
                return "KEY_LCONTROL";
            case KEY_A:
                return "KEY_A";
            case KEY_S:
                return "KEY_S";
            case KEY_D:
                return "KEY_D";
            case KEY_F:
                return "KEY_F";
            case KEY_G:
                return "KEY_G";
            case KEY_H:
                return "KEY_H";
            case KEY_J:
                return "KEY_J";
            case KEY_K:
                return "KEY_K";
            case KEY_L:
                return "KEY_L";
            case KEY_SEMICOLON:
                return "KEY_SEMICOLON";
            case KEY_APOSTROPHE:
                return "KEY_APOSTROPHE";
            case KEY_GRAVE:
                return "KEY_GRAVE";
            case KEY_LSHIFT:
                return "KEY_LSHIFT";
            case KEY_BACKSLASH:
                return "KEY_BACKSLASH";
            case KEY_Z:
                return "KEY_Z";
            case KEY_X:
                return "KEY_X";
            case KEY_C:
                return "KEY_C";
            case KEY_V:
                return "KEY_V";
            case KEY_B:
                return "KEY_B";
            case KEY_N:
                return "KEY_N";
            case KEY_M:
                return "KEY_M";
            case KEY_COMMA:
                return "KEY_COMMA";
            case KEY_PERIOD:
                return "KEY_PERIOD";
            case KEY_SLASH:
                return "KEY_SLASH";
            case KEY_RSHIFT:
                return "KEY_RSHIFT";
            case KEY_MULTIPLY:
                return "KEY_MULTIPLY";
            case KEY_SPACE:
                return "KEY_SPACE";
            case KEY_CAPITAL:
                return "KEY_CAPITAL";
            case KEY_F1:
                return "KEY_F1";
            case KEY_F2:
                return "KEY_F2";
            case KEY_F3:
                return "KEY_F3";
            case KEY_F4:
                return "KEY_F4";
            case KEY_F5:
                return "KEY_F5";
            case KEY_F6:
                return "KEY_F6";
            case KEY_F7:
                return "KEY_F7";
            case KEY_F8:
                return "KEY_F8";
            case KEY_F9:
                return "KEY_F9";
            case KEY_F10:
                return "KEY_F10";
            case KEY_NUMLOCK:
                return "KEY_NUMLOCK";
            case KEY_SCROLL:
                return "KEY_SCROLL";
            case KEY_NUMPAD7:
                return "KEY_NUMPAD7";
            case KEY_NUMPAD8:
                return "KEY_NUMPAD8";
            case KEY_NUMPAD9:
                return "KEY_NUMPAD9";
            case KEY_SUBTRACT:
                return "KEY_SUBTRACT";
            case KEY_NUMPAD4:
                return "KEY_NUMPAD4";
            case KEY_NUMPAD5:
                return "KEY_NUMPAD5";
            case KEY_NUMPAD6:
                return "KEY_NUMPAD6";
            case KEY_ADD:
                return "KEY_ADD";
            case KEY_NUMPAD1:
                return "KEY_NUMPAD1";
            case KEY_NUMPAD2:
                return "VK_NUMPAD2";
            case KEY_NUMPAD3:
                return "KEY_NUMPAD2";
            case KEY_NUMPAD0:
                return "KEY_NUMPAD0";
            case KEY_DECIMAL:
                return "KEY_DECIMAL";
            case KEY_F11:
                return "KEY_F11";
            case KEY_F12:
                return "KEY_F12";
            case KEY_F13:
                return "KEY_F13";
            case KEY_F14:
                return "KEY_F14";
            case KEY_F15:
                return "KEY_F15";
            case KEY_KANA:
                return "KEY_KANA";
            case KEY_CONVERT:
                return "KEY_CONVERT";
            case KEY_NOCONVERT:
                return "KEY_NOCONVERT";
            case KEY_NUMPADEQUALS:
                return "KEY_NUMPADEQUALS";
            case KEY_CIRCUMFLEX:
                return "KEY_CIRCUMFLEX";
            case KEY_AT:
                return "KEY_AT";
            case KEY_COLON:
                return "KEY_COLON";
            case KEY_UNDERLINE:
                return "KEY_UNDERLINE";
            case KEY_STOP:
                return "KEY_STOP";
            case KEY_NUMPADENTER:
                return "KEY_NUMPADENTER";
            case KEY_RCONTROL:
                return "KEY_RCONTROL";
            case KEY_NUMPADCOMMA:
                return "KEY_NUMPADCOMMA";
            case KEY_DIVIDE:
                return "KEY_DIVIDE";
            case KEY_PAUSE:
                return "KEY_PAUSE";
            case KEY_HOME:
                return "KEY_HOME";
            case KEY_UP:
                return "KEY_UP";
            case KEY_PRIOR:
                return "KEY_PRIOR";
            case KEY_LEFT:
                return "KEY_LEFT";
            case KEY_RIGHT:
                return "KEY_RIGHT";
            case KEY_END:
                return "KEY_END";
            case KEY_DOWN:
                return "KEY_DOWN";
            case KEY_NEXT:
                return "KEY_NEXT";
            case KEY_INSERT:
                return "KEY_INSERT";
            case KEY_DELETE:
                return "KEY_DELETE";
            case KEY_LMENU:
                return "KEY_LMENU"; //todo: location left
            case KEY_RMENU:
                return "KEY_RMENU"; //todo: location right
        }
        logger.warning( "unsupported key:" + key );
        return "unknown (" + key + ")";
    }

    /**
     * <code>toInputCode</code> converts AWT key codes to KeyInput key codes.
     *
     * @param key awt KeyEvent key code
     * @return jme KeyInput key code
     */
    public int toInputCode( int key ) {
        switch ( key ) {
            case KeyEvent.VK_ESCAPE:
                return KEY_ESCAPE;
            case KeyEvent.VK_1:
                return KEY_1;
            case KeyEvent.VK_2:
                return KEY_2;
            case KeyEvent.VK_3:
                return KEY_3;
            case KeyEvent.VK_4:
                return KEY_4;
            case KeyEvent.VK_5:
                return KEY_5;
            case KeyEvent.VK_6:
                return KEY_6;
            case KeyEvent.VK_7:
                return KEY_7;
            case KeyEvent.VK_8:
                return KEY_8;
            case KeyEvent.VK_9:
                return KEY_9;
            case KeyEvent.VK_0:
                return KEY_0;
            case KeyEvent.VK_MINUS:
                return KEY_MINUS;
            case KeyEvent.VK_EQUALS:
                return KEY_EQUALS;
            case KeyEvent.VK_BACK_SPACE:
                return KEY_BACK;
            case KeyEvent.VK_TAB:
                return KEY_TAB;
            case KeyEvent.VK_Q:
                return KEY_Q;
            case KeyEvent.VK_W:
                return KEY_W;
            case KeyEvent.VK_E:
                return KEY_E;
            case KeyEvent.VK_R:
                return KEY_R;
            case KeyEvent.VK_T:
                return KEY_T;
            case KeyEvent.VK_Y:
                return KEY_Y;
            case KeyEvent.VK_U:
                return KEY_U;
            case KeyEvent.VK_I:
                return KEY_I;
            case KeyEvent.VK_O:
                return KEY_O;
            case KeyEvent.VK_P:
                return KEY_P;
            case KeyEvent.VK_OPEN_BRACKET:
                return KEY_LBRACKET;
            case KeyEvent.VK_CLOSE_BRACKET:
                return KEY_RBRACKET;
            case KeyEvent.VK_ENTER:
                return KEY_RETURN;
            case KeyEvent.VK_CONTROL:
                return KEY_LCONTROL;
            case KeyEvent.VK_A:
                return KEY_A;
            case KeyEvent.VK_S:
                return KEY_S;
            case KeyEvent.VK_D:
                return KEY_D;
            case KeyEvent.VK_F:
                return KEY_F;
            case KeyEvent.VK_G:
                return KEY_G;
            case KeyEvent.VK_H:
                return KEY_H;
            case KeyEvent.VK_J:
                return KEY_J;
            case KeyEvent.VK_K:
                return KEY_K;
            case KeyEvent.VK_L:
                return KEY_L;
            case KeyEvent.VK_SEMICOLON:
                return KEY_SEMICOLON;
            case KeyEvent.VK_QUOTE:
                return KEY_APOSTROPHE;
            case KeyEvent.VK_DEAD_GRAVE:
                return KEY_GRAVE;
            case KeyEvent.VK_SHIFT:
                return KEY_LSHIFT;
            case KeyEvent.VK_BACK_SLASH:
                return KEY_BACKSLASH;
            case KeyEvent.VK_Z:
                return KEY_Z;
            case KeyEvent.VK_X:
                return KEY_X;
            case KeyEvent.VK_C:
                return KEY_C;
            case KeyEvent.VK_V:
                return KEY_V;
            case KeyEvent.VK_B:
                return KEY_B;
            case KeyEvent.VK_N:
                return KEY_N;
            case KeyEvent.VK_M:
                return KEY_M;
            case KeyEvent.VK_COMMA:
                return KEY_COMMA;
            case KeyEvent.VK_PERIOD:
                return KEY_PERIOD;
            case KeyEvent.VK_SLASH:
                return KEY_SLASH;
            case KeyEvent.VK_MULTIPLY:
                return KEY_MULTIPLY;
            case KeyEvent.VK_SPACE:
                return KEY_SPACE;
            case KeyEvent.VK_CAPS_LOCK:
                return KEY_CAPITAL;
            case KeyEvent.VK_F1:
                return KEY_F1;
            case KeyEvent.VK_F2:
                return KEY_F2;
            case KeyEvent.VK_F3:
                return KEY_F3;
            case KeyEvent.VK_F4:
                return KEY_F4;
            case KeyEvent.VK_F5:
                return KEY_F5;
            case KeyEvent.VK_F6:
                return KEY_F6;
            case KeyEvent.VK_F7:
                return KEY_F7;
            case KeyEvent.VK_F8:
                return KEY_F8;
            case KeyEvent.VK_F9:
                return KEY_F9;
            case KeyEvent.VK_F10:
                return KEY_F10;
            case KeyEvent.VK_NUM_LOCK:
                return KEY_NUMLOCK;
            case KeyEvent.VK_SCROLL_LOCK:
                return KEY_SCROLL;
            case KeyEvent.VK_NUMPAD7:
                return KEY_NUMPAD7;
            case KeyEvent.VK_NUMPAD8:
                return KEY_NUMPAD8;
            case KeyEvent.VK_NUMPAD9:
                return KEY_NUMPAD9;
            case KeyEvent.VK_SUBTRACT:
                return KEY_SUBTRACT;
            case KeyEvent.VK_NUMPAD4:
                return KEY_NUMPAD4;
            case KeyEvent.VK_NUMPAD5:
                return KEY_NUMPAD5;
            case KeyEvent.VK_NUMPAD6:
                return KEY_NUMPAD6;
            case KeyEvent.VK_ADD:
                return KEY_ADD;
            case KeyEvent.VK_NUMPAD1:
                return KEY_NUMPAD1;
            case KeyEvent.VK_NUMPAD2:
                return KEY_NUMPAD2;
            case KeyEvent.VK_NUMPAD3:
                return KEY_NUMPAD3;
            case KeyEvent.VK_NUMPAD0:
                return KEY_NUMPAD0;
            case KeyEvent.VK_DECIMAL:
                return KEY_DECIMAL;
            case KeyEvent.VK_F11:
                return KEY_F11;
            case KeyEvent.VK_F12:
                return KEY_F12;
            case KeyEvent.VK_F13:
                return KEY_F13;
            case KeyEvent.VK_F14:
                return KEY_F14;
            case KeyEvent.VK_F15:
                return KEY_F15;
            case KeyEvent.VK_KANA:
                return KEY_KANA;
            case KeyEvent.VK_CONVERT:
                return KEY_CONVERT;
            case KeyEvent.VK_NONCONVERT:
                return KEY_NOCONVERT;
            case KeyEvent.VK_CIRCUMFLEX:
                return KEY_CIRCUMFLEX;
            case KeyEvent.VK_AT:
                return KEY_AT;
            case KeyEvent.VK_COLON:
                return KEY_COLON;
            case KeyEvent.VK_UNDERSCORE:
                return KEY_UNDERLINE;
            case KeyEvent.VK_STOP:
                return KEY_STOP;
            case KeyEvent.VK_DIVIDE:
                return KEY_DIVIDE;
            case KeyEvent.VK_PAUSE:
                return KEY_PAUSE;
            case KeyEvent.VK_HOME:
                return KEY_HOME;
            case KeyEvent.VK_UP:
                return KEY_UP;
            case KeyEvent.VK_PAGE_UP:
                return KEY_PRIOR;
            case KeyEvent.VK_LEFT:
                return KEY_LEFT;
            case KeyEvent.VK_RIGHT:
                return KEY_RIGHT;
            case KeyEvent.VK_END:
                return KEY_END;
            case KeyEvent.VK_DOWN:
                return KEY_DOWN;
            case KeyEvent.VK_PAGE_DOWN:
                return KEY_NEXT;
            case KeyEvent.VK_INSERT:
                return KEY_INSERT;
            case KeyEvent.VK_DELETE:
                return KEY_DELETE;
            case KeyEvent.VK_ALT:
                return KEY_LMENU; //Left vs. Right need to improve

        }
        logger.warning( "unsupported key:" + key );
        if ( key >= 0x10000 ) {
            return key - 0x10000;
        }
        
        return 0;        
    }
    
    public void setup( Canvas glCanvas) {
        glCanvas.addKeyListener(this);
    }
}
