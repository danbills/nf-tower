/*
 * Copyright (c) 2019, Seqera Labs.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 */

package io.seqera.tower.service

import javax.inject.Inject
import javax.inject.Singleton

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import io.seqera.tower.domain.ProcessProgress
import io.seqera.tower.domain.WorkflowMetrics
import io.seqera.tower.domain.Task
import io.seqera.tower.domain.User
import io.seqera.tower.domain.Workflow
import io.seqera.tower.exceptions.NonExistingWorkflowException
import io.seqera.tower.exchange.progress.ProgressGet
import io.seqera.tower.exchange.trace.TraceWorkflowRequest

@Transactional
@Singleton
class WorkflowServiceImpl implements WorkflowService {

    ProgressService progressService

    @Inject
    WorkflowServiceImpl(ProgressService progressService) {
        this.progressService = progressService
    }

    @CompileDynamic
    Workflow get(Serializable id) {
        Workflow.findById(id, [fetch: [tasksProgress: 'join', processesProgress: 'join']])
    }

    @CompileDynamic
    List<Workflow> listByOwner(User owner) {
        Workflow.findAllByOwner(owner, [sort: 'start', order: 'desc'])
    }

    Workflow processTraceWorkflowRequest(TraceWorkflowRequest request, User owner) {
        request.workflow.checkIsStarted() ? saveWorkflow(request.workflow, owner) : updateWorkflow(request.workflow, request.summary)
    }

    private Workflow saveWorkflow(Workflow workflow, User owner) {
        workflow.submit = workflow.start

        workflow.owner = owner

        // invoke validation explicitly due to gorm bug
        // https://github.com/grails/gorm-hibernate5/issues/110
        if (workflow.validate())
            workflow.save()

        return workflow
    }

    @CompileDynamic
    private Workflow updateWorkflow(Workflow workflow, List<WorkflowMetrics> metrics) {
        Workflow existingWorkflow = Workflow.get(workflow.workflowId)
        if (!existingWorkflow) {
            throw new NonExistingWorkflowException("Can't update a non-existing workflow")
        }

        updateChangeableFields(existingWorkflow, workflow)
        associateMetrics(existingWorkflow, metrics)
        associateProgress(existingWorkflow)

        existingWorkflow.save()
        existingWorkflow
    }

    private void updateChangeableFields(Workflow workflowToUpdate, Workflow originalWorkflow) {
        workflowToUpdate.resume = originalWorkflow.resume
        workflowToUpdate.success = originalWorkflow.success
        workflowToUpdate.complete = originalWorkflow.complete
        workflowToUpdate.duration = originalWorkflow.duration

        workflowToUpdate.exitStatus = originalWorkflow.exitStatus
        workflowToUpdate.errorMessage = originalWorkflow.errorMessage
        workflowToUpdate.errorReport = originalWorkflow.errorReport

        workflowToUpdate.stats = originalWorkflow.stats
    }

    private void associateMetrics(Workflow workflow, List<WorkflowMetrics> allMetrics) {
        for( WorkflowMetrics metrics : allMetrics ) {
            metrics.workflow = workflow
            metrics.save()
        }
    }

    private void associateProgress(Workflow workflow) {
        if (!workflow.checkIsStarted()) {
            ProgressGet progress = progressService.computeWorkflowProgress(workflow.id)
            workflow.tasksProgress = progress.tasksProgress
            progress.processesProgress.each { ProcessProgress processProgress ->
                workflow.addToProcessesProgress(processProgress)
            }
        }
    }

    void delete(Workflow workflowToDelete) {
        WorkflowMetrics.where { workflow == workflowToDelete }.deleteAll()
        
        workflowToDelete.tasks?.each { Task task ->
            task.delete()
        }

        workflowToDelete.delete()
    }

    void deleteById(Serializable workflowId) {
        delete( get(workflowId) )
    }

    @CompileDynamic
    List<WorkflowMetrics> findMetrics(Workflow workflow) {
        WorkflowMetrics.findAllByWorkflow(workflow)
    }

}
