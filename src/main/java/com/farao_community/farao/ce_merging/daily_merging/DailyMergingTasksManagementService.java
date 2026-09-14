/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotFoundException;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class DailyMergingTasksManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyMergingTasksManagementService.class);

    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;

    public DailyMergingTasksManagementService(final CeMergingConfiguration configuration,
                                              final DailyMergingRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public DailyMergingTask createDailyTask(final List<Long> ids, final MultipartFile mergingRequest, final int mergingVersion) {
        //TODO
        return null;
    }

    public DailyMergingTask runDailyMergingTask(final long taskId) {
        // TODO implements and change return type
        return null;
    }

    public List<DailyMergingTask> getAllTasks() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .toList();
    }

    public DailyMergingTask getTask(final long taskId) {
        return getTaskById(taskId);
    }

    public void deleteAllTasks() {
        repository.findAll().forEach(task -> deleteTask(task.getId()));
    }

    public void deleteTask(final long taskId) {
        try {
            final DailyMergingTask task = getTaskById(taskId);
            FileSystemUtils.deleteRecursively(Paths.get(configuration.getDailyDirectoryPath(task)));
            repository.deleteById(taskId);
        } catch (IOException e) {
            throw new CeMergingException("Error during daily merging task delete", e);
        }
    }

    public SavedFile getMergingRequest(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyRefProgOutput(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyMergingResponse(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyMergingLogs(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyCgmZip(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyGlskReport(final long taskId) {
        //TODO
        return null;
    }

    public byte[] getDailyResultPackage(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getMergingReport(final long taskId) {
        //TODO
        return null;
    }

    public DailyMergingTask getTaskById(final long taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(String.format("Task %d not available", taskId)));
    }

}
