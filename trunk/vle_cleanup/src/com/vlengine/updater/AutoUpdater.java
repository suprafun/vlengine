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
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vlengine.updater.UpdateStatus.Status;

/**
 * 
 * This class is not thread safe. However UpdateStatus retrieved from this class
 * can be safely read in other threads.
 * 
 * @author lex (Aleksey Nikiforov)
 *
 */
public abstract class AutoUpdater extends BaseAutoUpdater {

    private static final Logger log = Logger.getLogger(
            AutoUpdater.class.getName());
    protected static final String BACKUP_SUFFIX = ".restore";
    protected static final String PATCH_SUFFIX = ".patch";
    protected UpdateStatus status;
    protected File backupRecords;
    protected File patchRecords;
    protected FileRecord partialFile;

    public AutoUpdater() {
        super();
        status = new UpdateStatus(recordManager.getFileProgress());
    }
    
    public AutoUpdater(File localDir, File localDescriptor, URL serverDir,
            URL serverDescriptorUrl)
    {
        super(localDir, localDescriptor, serverDir, serverDescriptorUrl);
        status = new UpdateStatus(recordManager.getFileProgress());

        backupRecords = new File(
                this.localDescriptor.getAbsolutePath() + BACKUP_SUFFIX);
        patchRecords = new File(
                this.localDescriptor.getAbsolutePath() + PATCH_SUFFIX);
    }
    
    @Override
    public void setLocalDescriptor(File desc) {
        this.localDescriptor = desc;
        
        backupRecords = new File(
                this.localDescriptor.getAbsolutePath() + BACKUP_SUFFIX);
        patchRecords = new File(
                this.localDescriptor.getAbsolutePath() + PATCH_SUFFIX);
    }

    public UpdateStatus getUpdateStatus() {
        return status;
    }

    /**
     * This method manages file records and generates
     * three lists: files to be added, files to be replaced and files
     * to be removed.
     * 
     * @return the array with add, replace and remove lists in that order
     * @throws UpdateException
     */
    protected abstract RecordList[] manageFileRecords() throws UpdateException;

    protected abstract void manageUpdatedRecords() throws UpdateException;

    protected abstract void preUpdate() throws UpdateException;

    protected abstract void postUpdate();

    /**
     * @param record
     * @return true is copy operation succeeded, false if cancelled
     * @throws UpdateException
     */
    protected abstract boolean copy(FileRecord record) throws UpdateException;

    protected abstract void remove(FileRecord record) throws UpdateException;

    protected abstract void removeEmptyDirs() throws UpdateException;

    protected RecordList loadLocalRecords(boolean fallBackOnDescriptorErrors)
            throws UpdateException {
        RecordList localRecords = null;

        boolean primaryDescriptorCorrupted = false;
        boolean backupLoaded = false;
        boolean patchLoaded = false;

        if (localDescriptor.exists()) {

            try {
                localRecords = loadLocalDescriptor(localDescriptor);
                return localRecords;

            } catch (UpdateException e) {
                if (!fallBackOnDescriptorErrors) {
                    throw e;
                }

                primaryDescriptorCorrupted = true;
                log.log(Level.WARNING,
                        "Error reading primary descriptor file, using backup.",
                        e);
            }

        }

        localRecords = new RecordList();

        if (backupRecords.exists()) {

            try {
                localRecords = loadLocalDescriptor(backupRecords);
                backupLoaded = true;

            } catch (UpdateException e) {
                if (!fallBackOnDescriptorErrors) {
                    throw e;
                }

                log.log(Level.WARNING,
                        "Error reading backup descriptor file, " +
                        "using patch-backup.", e);
            }
        }

        if (patchRecords.exists()) {

            try {
                FileRecord[] last = new FileRecord[1];
                RecordList tempList = RecordList.fromFile(
                        new FileInputStream(patchRecords),
                        last);
                FileRecord lastFile = last[0];

                if (lastFile != null && !primaryDescriptorCorrupted) {
                    tempList.removeRecord(lastFile);
                    partialFile = lastFile;
                }

                localRecords.applyPatch(tempList);
                patchLoaded = true;
                saveLocalRecords(localRecords);

            } catch (IOException e) {
                if (!fallBackOnDescriptorErrors) {
                    throw new UpdateException(
                            "Error while reading the local patch file.", e);
                }

                log.log(Level.WARNING,
                        "Error reading patch-backup file, generating new list.",
                        e);
            }
        }

        if (backupLoaded || patchLoaded) {
            return localRecords;
        }
        return null;
    }

    public boolean update() throws UpdateException {
        try {
            status.reset();
            preUpdate();
            return updateFiles();
        } finally {
            postUpdate();
        }
    }

    private boolean updateFiles() throws UpdateException {
        RecordList[] diff;
        PersistentRecordList patchList;
        try {

            diff = manageFileRecords();
            if (diff == null) {
                status.setStatus(Status.COMPLETE);
                return true;
            }

            // if the incomplete file will not be updated or re-added,
            // then it should be removed.
            if (partialFile != null) {
                if (!diff[0].getRecords().containsKey(partialFile.getFile()) &&
                    !diff[1].getRecords().containsKey(partialFile.getFile())) {
                    remove(partialFile);
                }
            }

            if (localDescriptor.exists()) {
                if (backupRecords.exists()) {
                    backupRecords.delete();
                }

                if (!localDescriptor.renameTo(backupRecords)) {
                    throw new UpdateException("Unable to save the local " +
                        "descriptor file '" + backupRecords.getAbsolutePath() +
                        "'.");
                }
            }

            // create persistent record list
            if (patchRecords.exists()) {
                patchRecords.delete();
            } else {
                File dir = localDescriptor.getAbsoluteFile().getParentFile();
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new UpdateException("Unable to save the local " +
                        "descriptor file '" + localDescriptor.getAbsolutePath()+
                        "' - could not create the directory.");
                }
            }
            try {
                patchList = new PersistentRecordList(
                        new FileOutputStream(patchRecords));
            } catch (IOException e) {
                throw new UpdateException("Unable to save the local " +
                    "descriptor file '" + patchRecords.getAbsolutePath() +
                    "'.");
            }

        } catch (UpdateException e) {
            status.setStatus(Status.ERROR);
            throw e;
        }

        status.setJobSize(diff[0].getTotalSize() + diff[1].getTotalSize());

        boolean updateException = false;
        boolean fileCopied = false;
        FileRecord lastRecord = null;
        try {

            for (int i = 0; i < 2; i++) {
                if (i == 0) {
                    status.setStatus(Status.ADDING);
                }
                if (i == 1) {
                    status.setStatus(Status.UPDATING);
                }

                for (FileRecord record : diff[i].getRecords().values()) {
                    lastRecord = record;
                    status.nextFile(record);

                    try {

                        patchList.addRecord(record);
                        fileCopied = false;

                    } catch (PersistentRecordException e) {
                        throw new UpdateException("Unable to save the local " +
                                "descriptor file '"
                                + patchRecords.getAbsolutePath() + "'.",
                                e.getCause());
                    }

                    fileCopied = copy(record);

                    if (status.isCancelled()) {
                        log.log(Level.INFO, "Update is cancelled.");
                        status.setStatus(Status.CANCELLED);
                        return false;
                    }

                }
            }

            status.setStatus(Status.CLEANING_UP);
            for (FileRecord record : diff[2].getRecords().values()) {
                remove(record);
                destRecords.removeRecord(record);
            }

            removeEmptyDirs();

        } catch (UpdateException e) {
            status.setStatus(Status.ERROR);
            updateException = true;
            throw e;

        } finally {

            try {
                patchList.disconnect();
            } catch (IOException e) {
                log.log(Level.SEVERE, "File socket lost.");
            }

            if (lastRecord != null && !fileCopied) {
                patchList.removeRecordFromMemory(lastRecord);
            }

            destRecords.applyPatch(patchList);

            try {
                saveLocalRecords(destRecords);

            } catch (UpdateException e) {
                status.setStatus(Status.ERROR);

                if (updateException) {
                    log.log(Level.SEVERE, "Exception while saving " +
                            "local records.", e);
                } else {
                    throw e;
                }
            }
        }

        try {
            manageUpdatedRecords();
        } catch (UpdateException e) {
            status.setStatus(Status.ERROR);
            throw e;
        }

        status.setStatus(Status.COMPLETE);
        return true;
    }
}
