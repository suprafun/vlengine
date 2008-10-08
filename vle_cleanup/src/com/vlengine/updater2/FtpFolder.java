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

package com.vlengine.updater2;

import com.vlengine.system.VleException;
import com.vlengine.util.FastList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.logging.Level;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class FtpFolder extends Folder {
    
    protected FTPClient ftp = new FTPClient();
    protected URL ftpURL;
    protected String folder;
    protected HashSet<String> dirs = new HashSet<String>();
    protected String user;
    protected String password;
    
    @Override
    public boolean connect(String connectString, String user, String password) {
        try {
            ftpURL = new URL(connectString);
        } catch (Exception e) {
            return false;
        }
        this.user = user;
        this.password = password;
        return connect();
    }

    protected boolean connect() {
        try {
            int port = ftpURL.getPort();
            if (port < 0) port = 21;
            folder = ftpURL.getFile();
            
            ftp.connect(ftpURL.getHost(), port);

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                log.log(Level.SEVERE, "FTP server refused connection.");
                ftp.disconnect();
                return false;
            }

            ftp.login(user, password);

            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                log.log(Level.SEVERE, "Incorrect FTP login information.");
                ftp.disconnect();
                return false;
            }

            //ftp.makeDirectory(serverDirUrl.getFile());
            /*
            if (!ftp.changeWorkingDirectory(folder)) {
                log.log(Level.SEVERE, "Unable to change working directory.");
                ftp.disconnect();
                return false;
            }
             */

            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Unable to establish connection to the FTP server.", e);
            return false;
        }
        return true;
    }
    
    @Override
    public void disconnect() {
        try {
            if (ftp.isConnected()) {
                ftp.logout();
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                    log.log(Level.WARNING, "Logout attempt did not succeed.");
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Error while logging out from the ftp.", e);
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Error while disconnecting from the ftp.");
            }
        }
    }

    @Override
    public InputStream getInputStream(Resource r) {
        try {
            if(ftp.isConnected()) {
                disconnect();
            }
            if(!connect()) {
                return null;
            }

            String target = folder;
            String tgp = r.getTargetFilePath();
            if(!tgp.startsWith("/") && !target.endsWith("/")) {
                target = target + "/" + tgp;
            } else {
                target = target + tgp;
            }
            return ftp.retrieveFileStream(target);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Cannot open input stream to file "+r.getName(), ex);
            if(!ftp.isConnected()) {
                connect();
            }
        }
        return null;
    }

    @Override
    public OutputStream getOutputStream(Resource r) {
        try {
            
            if(ftp.isConnected()) {
                disconnect();
            }
            connect();
            
            mkDirs(r);
            
            String target = folder;
            String tgp = r.getTargetFilePath();
            if(!tgp.startsWith("/") && !target.endsWith("/")) {
                target = target + "/" + tgp;
            } else {
                target = target + tgp;
            }
            
            // remove file
            ftp.deleteFile(target);

            // open connection to it
            return ftp.storeFileStream(target);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Cannot open output stream to file "+r.getName(), ex);
        }
        return null;
    }

    @Override
    public void getFileList(FastList<Resource> store) {
        throw new VleException("Listing not supported on FTP folders");
    }
    
    @Override
    public void getFileList(HashSet<String> includes, FastList<Resource> store) {
        throw new VleException("Listing not supported on FTP folders");
    }

    @Override
    public void deleteFile(Resource r) {
        try {
            String fullFile = folder + "/" + r.getTargetFilePath();
            ftp.deleteFile(fullFile);
        } catch (IOException ex) {
        }
    }

    @Override
    public void mkDirs(Resource r) {
        try {
            String target = folder;
            String tgp = r.getTargetFilePath();
            if(!tgp.startsWith("/") && !target.endsWith("/")) {
                target = target + "/" + tgp;
            } else {
                target = target + tgp;
            }
            int index = target.indexOf('/', 0);
            while (index != -1) {
                String dir = target.substring(0, index);
                if (!dirs.contains(dir) && !"".equals(dir)) {
                    ftp.makeDirectory(dir);
                    dirs.add(dir);
                }
                index = target.indexOf('/', index + 1);
            }
        } catch(Exception e) {
        }
    }

    @Override
    public void setFileDate(Resource r) {
        // ignore
    }


}
