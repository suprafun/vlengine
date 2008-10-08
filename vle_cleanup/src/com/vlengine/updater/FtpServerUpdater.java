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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * 
 * @author lex (Aleksey Nikiforov)
 */
public class FtpServerUpdater extends AutoUpdater {

    private static final Logger log = Logger.getLogger(
            FtpServerUpdater.class.getName());
    
    protected static final String UPDATED_SUFFIX = ".updated";
    
    protected File updatedRecords;
    protected boolean updatingDescriptor = true;
    protected FTPClient ftp;
    protected HashMap<String, String> dirs;

    public FtpServerUpdater() {
        super();
        ftp = new FTPClient();
    }
    
    public FtpServerUpdater(File localDir, File localDescriptor,
            URL serverDir, URL serverDescriptorUrl)
    {
        super(localDir, localDescriptor, serverDir, serverDescriptorUrl);
        ftp = new FTPClient();
        updatedRecords = new File(
                localDescriptor.getAbsolutePath() + UPDATED_SUFFIX);
    }
    
    @Override
    public void setLocalDescriptor(File desc) {
        super.setLocalDescriptor(desc);
        updatedRecords = new File(
                localDescriptor.getAbsolutePath() + UPDATED_SUFFIX);
    }

    public boolean isUpdatingDescriptor() {
        return updatingDescriptor;
    }

    public void setUpdatingDescriptor(boolean updatingDescriptor) {
        this.updatingDescriptor = updatingDescriptor;
    }

    @Override
    protected boolean copy(FileRecord record) throws UpdateException {
        InputStream in = null;
        try {
            in = new FileInputStream(getLocalFile(record.getFile()));
        } catch (FileNotFoundException e) {
            throw new UpdateException("Unable to locate local file '" +
                    record.getFile() + "'.", e);
        }

        OutputStream out;
        try {
            ftp.deleteFile(record.getFile());
            makeDirectories(record.getFile());

            out = ftp.storeFileStream(record.getFile());

            if (out == null) {
                log.log(Level.SEVERE,
                        "Error while initiating ftp transfer for file '" +
                        record.getFile() + "', " +
                        "ftp server response: '" + ftp.getReplyString() + "'.");
                throw new UpdateException(
                        "Error while initiating ftp transfer for file '" +
                        record.getFile() + "'.");
            }
        } catch (IOException e) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    log.log(Level.SEVERE, "File socket lost.", e1);
                }
            }

            throw new UpdateException(
                    "Error while initiating ftp transfer for file '" +
                    record.getFile() + "'.", e);
        }

        try {
            boolean copied = recordManager.copy(in, out);
            if (!ftp.completePendingCommand()) {
                log.log(Level.SEVERE,
                        "File upload for '" + record.getFile() + "' " +
                        "was not successfull, " +
                        "ftp server response: '" + ftp.getReplyString() + "'.");
                throw new UpdateException(
                        "File upload for '" + record.getFile() +
                        "' was not successfull.");
            }
            return copied;

        } catch (IOException e) {
            throw new UpdateException(
                    "Error while uploading file '" + record.getFile() + "'.", e);
        }
    }

    private void makeDirectories(String target) throws IOException {
        int index = target.indexOf('/', 0);
        while (index != -1) {
            String dir = target.substring(0, index);
            if (!dirs.containsKey(dir)) {
                ftp.makeDirectory(dir);
                dirs.put(dir, null);
            }
            index = target.indexOf('/', index + 1);
        }
    }

    @Override
    protected RecordList[] manageFileRecords() throws UpdateException {
        File updated = new File(
                localDescriptor.getAbsolutePath() + UPDATED_SUFFIX);

        if (updated.exists()) {
            destRecords = loadLocalDescriptor(updated);
        } else {
            try {
                destRecords = loadServerDescriptor(serverDescriptorUrl);
            } catch (UpdateException e) {
                destRecords = new RecordList();
            }

            RecordList recoveryRecords = loadLocalRecords(false);
            if (recoveryRecords != null) {
                destRecords.applyPatch(recoveryRecords);
            }
        }

        HashMap<String, String> exclude = new HashMap<String, String>();
        String baseDir = localDir.getAbsolutePath();
        if (localDescriptor.getAbsolutePath().startsWith(baseDir)) {
            int offset = baseDir.length() + 1;
            String desc = localDescriptor.getAbsolutePath().substring(offset);
            exclude.put(desc, null);
            exclude.put(desc + UPDATED_SUFFIX, null);
            exclude.put(desc + AutoUpdater.BACKUP_SUFFIX, null);
            exclude.put(desc + AutoUpdater.PATCH_SUFFIX, null);
        }

        // generate source records
        try {
            sourceRecords = recordManager.generateLocalList(localDir, exclude);
        } catch (IOException e) {
            throw new UpdateException(
                    "Unable to generate local file list.", e);
        }

        return RecordManager.findDifference(destRecords, sourceRecords);
    }

    @Override
    protected void manageUpdatedRecords() throws UpdateException {
        if (updatingDescriptor) {
            if (updatedRecords.exists()) {
                updatedRecords.delete();
            }

            if (!localDescriptor.renameTo(updatedRecords)) {
                throw new UpdateException("Unable to rename local descriptor.");
            }

            FileInputStream is = null;
            try {
                is = new FileInputStream(updatedRecords);
                ftp.deleteFile(serverDescriptorUrl.getFile());
                makeDirectories(serverDescriptorUrl.getFile());

                if (!ftp.storeFile(serverDescriptorUrl.getFile(), is)) {
                    throw new UpdateException(
                            "Unable to upload the new server descriptor.");
                }

            } catch (IOException e) {
                throw new UpdateException(
                        "Error while uploading the new server descriptor.", e);

            } finally {
                if (is != null) {

                    try {
                        is.close();
                    } catch (IOException e) {
                        log.log(Level.SEVERE, "Lost file socket.", e);
                    }
                }
            }
        }

        updatedRecords.delete();
        localDescriptor.delete();
        backupRecords.delete();
        patchRecords.delete();

        if (updatedRecords.exists() || localDescriptor.exists()
                || backupRecords.exists() || patchRecords.exists())
        {
            log.log(Level.WARNING,
                    "Unable to cleanup generated records.");
        }
    }

    @Override
    protected void postUpdate() {
        try {
            if (ftp.isConnected()) {
                ftp.logout();
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                    throw new IOException("Logout attempt did not succeed.");
                }
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Error while logging out from the ftp.", e);
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Error while disconnecting from " +
                        "the ftp.");
            }
        }
    }

    @Override
    protected void preUpdate() throws UpdateException {
        try {
            int port = this.serverDescriptorUrl.getPort();
            if (port < 0) port = 21;
            ftp.connect(this.serverDescriptorUrl.getHost(), port);

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                throw new UpdateException("FTP server refused connection.");
            }

            ftp.login(user, password);

            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                throw new UpdateException("Incorrect login information.");
            }

            ftp.makeDirectory(serverDirUrl.getFile());
            if (!ftp.changeWorkingDirectory(serverDirUrl.getFile())) {
                throw new UpdateException("Unable to make a directory or " +
                        "change working directory.");
            }

            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            dirs = new HashMap<String, String>();
        } catch (IOException e) {
            throw new UpdateException("Unable to establish connection to " +
                    "the FTP server.", e);
        }
    }

    @Override
    protected void remove(FileRecord record) throws UpdateException {
        try {
            if (!ftp.deleteFile(record.getFile())) {
                log.log(Level.WARNING,
                        "File '" + record.getFile() + "' " +
                        "could not be deleted, " +
                        "ftp server response: '" + ftp.getReplyString() + "'.");
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Error while deleting file '" +
                    record.getFile() + "'.");
        }
    }

    @Override
    protected void removeEmptyDirs() throws UpdateException {
        // do nothing
    }
    
    public boolean willResume() {
        return (updatedRecords.exists() || localDescriptor.exists()
                || backupRecords.exists() || patchRecords.exists());
    }
}
