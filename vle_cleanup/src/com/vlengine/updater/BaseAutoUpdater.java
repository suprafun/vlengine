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

package com.vlengine.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.security.DigestException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This auto updater works with with an application descriptor file
 * deployed on the server.
 * 
 * @author lex (Aleksey Nikiforov)
 *
 */
public class BaseAutoUpdater {

    private static final Logger log = Logger.getLogger(
            BaseAutoUpdater.class.getName());
    private static final String TEMP_SUFFIX = ".temp";
    
    /** application directory */
    protected File localDir;
    protected File localDescriptor;
    
    /** a url pointing to a file that contains application descriptor */
    protected URL serverDescriptorUrl;
    protected URL serverDirUrl;
    
    protected RecordList destRecords;
    protected RecordList sourceRecords;
    protected String user;
    protected String password;
    protected RecordManager recordManager;

    public BaseAutoUpdater() {
        recordManager = new RecordManager(10 * 1024);
    }
    
    public BaseAutoUpdater(File localDir, File localDescriptor,
            URL serverDirUrl, URL serverDescriptorUrl)
    {
        this.localDir = localDir;
        if (localDir == null) {
            this.localDir = localDescriptor.getAbsoluteFile().getParentFile();
        }
        this.localDescriptor = localDescriptor;

        this.serverDescriptorUrl = serverDescriptorUrl;
        if (serverDirUrl == null) {
            try {
                String file = serverDescriptorUrl.getFile();
                this.serverDirUrl = new URL(
                        serverDescriptorUrl.getProtocol(),
                        serverDescriptorUrl.getHost(),
                        serverDescriptorUrl.getPort(),
                        file.substring(0, file.lastIndexOf("/") + 1));
            } catch (MalformedURLException e) {
                throw new NullPointerException("Server dir is not set.");
            }
        } else {
            this.serverDirUrl = serverDirUrl;
        }

        recordManager = new RecordManager(10 * 1024);
    }

    public File getLocalDescriptor() {
        return localDescriptor;
    }

    public void setLocalDescriptor(File localDescriptor) {
        this.localDescriptor = localDescriptor;
    }

    public File getLocalDir() {
        return localDir;
    }

    public void setLocalDir(File localDir) {
        this.localDir = localDir;
    }

    public URL getServerDescriptorUrl() {
        return serverDescriptorUrl;
    }

    public void setServerDescriptorUrl(URL serverDescriptorUrl) {
        this.serverDescriptorUrl = serverDescriptorUrl;
    }

    public URL getServerDirUrl() {
        return serverDirUrl;
    }

    public void setServerDirUrl(URL serverDirUrl) {
        this.serverDirUrl = serverDirUrl;
    }

    public RecordList loadLocalDescriptor(File descriptor)
            throws UpdateException {
        try {
            return RecordList.fromFile(
                    new GZIPInputStream(new FileInputStream(descriptor)),
                    null);

        } catch (IOException e) {
            throw new UpdateException(
                    "Error while reading local descriptor file.", e);
        }
    }

    public void setAuthentication(final String user, final String password) {
        this.user = user;
        this.password = password;
    }

    protected RecordList loadServerDescriptor(URL descriptorUrl)
            throws UpdateException {
        try {

            if (user != null && password != null) {
                Authenticator.setDefault(new Authenticator() {

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        return new PasswordAuthentication(
                                user, password.toCharArray());
                    }
                });
            }

            return RecordList.fromFile(
                    new GZIPInputStream(descriptorUrl.openStream()),
                    null);

        } catch (IOException e) {
            throw new UpdateException(
                    "Error while reading remote descriptor file.", e);
        } finally {
            Authenticator.setDefault(null);
        }
    }

    public URL getRemoteUrl(String file) throws MalformedURLException {
        try {
            String spec = URLEncoder.encode(file, "UTF-8");
            spec = spec.replaceAll("%2F", "/");
            spec = spec.replaceAll("\\+", "%20");
            return new URL(serverDirUrl, spec);
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, "UTF-8 encoding is not supported.");
            return null;
        }
    }

    public File getLocalFile(String file) {
        return new File(localDir, file);
    }

    /**
     * If the file has the same md5 as the record, null is returned. Otherwise
     * the new record generated from the file is returned.
     * 
     * @param record
     * @return
     * @throws UpdateException
     */
    protected FileRecord verifyLocalFile(FileRecord record)
            throws UpdateException {
        try {
            File file = getLocalFile(record.getFile());
            String md5 = "0";
            if (file.exists()) {
                md5 = recordManager.copyWithMd5(
                        new FileInputStream(file), null);
            }

            if (md5.equals(record.getMd5())) {
                return null;
            }

            return new FileRecord(md5,
                    file.lastModified(),
                    file.length(),
                    record.getFile());

        } catch (DigestException e) {
            throw new UpdateException("Unable to generate md5 for '" +
                    record.getFile() + "'.");
        } catch (IOException e) {
            throw new UpdateException("Read error while reading file '" +
                    record.getFile() + "'.");
        }
    }

    public void saveLocalRecords(RecordList records) throws UpdateException {
        try {
            File dir = localDescriptor.getAbsoluteFile().getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new UpdateException("Unable to save the local " +
                        "descriptor file '" + localDescriptor.getAbsolutePath()+
                        "' - could not create the directory.");
            }

            File temp = new File(localDescriptor.getAbsoluteFile()+TEMP_SUFFIX);
            records.save(new GZIPOutputStream(
                    new FileOutputStream(temp)));
            if (localDescriptor.exists()) {
                localDescriptor.delete();
            }
            if (!temp.renameTo(localDescriptor)) {
                throw new UpdateException("Unable to save the local " +
                        "descriptor file '" + localDescriptor.getAbsolutePath()+
                        "' - could not rename the temp file.");
            }

        } catch (IOException e) {
            throw new UpdateException("Unable to save the local " +
                    "descriptor file '" + localDescriptor.getAbsolutePath() +
                    "' - write error.",
                    e);
        }
    }
}
