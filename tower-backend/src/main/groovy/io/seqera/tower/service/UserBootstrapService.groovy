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

import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.inject.Singleton

import groovy.util.logging.Slf4j
import grails.gorm.transactions.Transactional
import io.seqera.tower.domain.User

/**
 * Bootstrap service to create default user for single-user mode
 */
@Slf4j
@Singleton
class UserBootstrapService {

    public static final String DEFAULT_USER_EMAIL = 'admin@example.com'

    @Inject
    UserService userService

    @PostConstruct
    @Transactional
    void init() {
        log.info "Initializing default user for single-user mode..."
        User defaultUser = userService.getByEmail(DEFAULT_USER_EMAIL)

        if (!defaultUser) {
            log.info "Creating default user: ${DEFAULT_USER_EMAIL}"
            defaultUser = userService.create(DEFAULT_USER_EMAIL)
            defaultUser.userName = 'tower-user'
            defaultUser.firstName = 'Tower'
            defaultUser.lastName = 'User'
            defaultUser.trusted = true
            defaultUser.save(failOnError: true)
            log.info "Default user created successfully with ID: ${defaultUser.id}"
        } else {
            log.info "Default user already exists with ID: ${defaultUser.id}"
        }
    }

    User getDefaultUser() {
        return userService.getByEmail(DEFAULT_USER_EMAIL)
    }
}
