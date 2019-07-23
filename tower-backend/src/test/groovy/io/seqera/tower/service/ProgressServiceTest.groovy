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

import grails.gorm.transactions.Transactional
import io.micronaut.test.annotation.MicronautTest
import io.seqera.tower.Application
import io.seqera.tower.domain.ProcessProgress
import io.seqera.tower.domain.Task
import io.seqera.tower.domain.Workflow
import io.seqera.tower.enums.TaskStatus
import io.seqera.tower.exchange.progress.ProgressGet
import io.seqera.tower.util.AbstractContainerBaseTest
import io.seqera.tower.util.DomainCreator

import javax.inject.Inject

@MicronautTest(application = Application.class)
@Transactional
class ProgressServiceTest extends AbstractContainerBaseTest {

    @Inject
    ProgressService progressService


    void "compute the progress info of a workflow"() {
        given: 'create a pending task of a process and associated with a workflow'
        DomainCreator domainCreator = new DomainCreator()
        String process1 = 'process1'
        Task task1 = domainCreator.createTask(status: TaskStatus.NEW, process: process1, duration: 1)
        Workflow workflow = task1.workflow

        and: 'a task for the previous process in each status'
        [TaskStatus.SUBMITTED, TaskStatus.CACHED, TaskStatus.RUNNING, TaskStatus.FAILED].each { TaskStatus status ->
            domainCreator.createTask(status: status, workflow: workflow, process: process1, duration: 1)
        }

        and: 'one more completed task of the same process'
        domainCreator.createTask(status: TaskStatus.COMPLETED, workflow: workflow, process: process1, duration: 1, hash: "lastHash")

        and: 'a pending task of another process'
        String process2 = 'process2'
        domainCreator.createTask(status: TaskStatus.NEW, workflow: workflow, process: process2, duration: 1)

        and: 'a task for the previous process in each status'
        [TaskStatus.SUBMITTED, TaskStatus.CACHED, TaskStatus.RUNNING, TaskStatus.FAILED].each { TaskStatus status ->
            domainCreator.createTask(status: status, workflow: workflow, process: process2, duration: 1)
        }

        and: 'two more completed tasks'
        2.times {
            domainCreator.createTask(status: TaskStatus.COMPLETED, workflow: workflow, process: process2, duration: 1, hash: "lastHash${it}")
        }

        when: "compute the progress of the workflow"
        ProgressGet progress = progressService.computeWorkflowProgress(workflow.id)

        then: "the tasks has been successfully computed"
        progress.workflowTasksProgress.progress.pending == 2
        progress.workflowTasksProgress.progress.submitted == 2
        progress.workflowTasksProgress.progress.running == 2
        progress.workflowTasksProgress.progress.cached == 2
        progress.workflowTasksProgress.progress.failed == 2
        progress.workflowTasksProgress.progress.succeeded == 3

        then: "the processes progress has been successfully computed"
        progress.processesProgress.size() == 2
        ProcessProgress progress1 = progress.processesProgress.find { it.process == process1 }
        progress1.progress.running == 1
        progress1.progress.submitted == 1
        progress1.progress.failed == 1
        progress1.progress.pending == 1
        progress1.progress.succeeded == 1
        progress1.progress.cached == 1
        progress1.progress.total == 6

        ProcessProgress progress2 = progress.processesProgress.find { it.process == process2 }
        progress2.progress.running == 1
        progress2.progress.submitted == 1
        progress2.progress.failed == 1
        progress2.progress.pending == 1
        progress2.progress.succeeded == 2
        progress2.progress.cached == 1
        progress2.progress.total == 7
    }

}
