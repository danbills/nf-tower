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

package io.seqera.tower.filter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.filters.SecurityFilter
import org.reactivestreams.Publisher

@Slf4j
@CompileStatic
@Filter("/**")
class DefaultAuthenticationFilter implements HttpServerFilter {

    @Override
    Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        log.debug("DefaultAuthenticationFilter invoked for: ${request.path}")
        // Create a default authentication object for all requests
        def authentication = new UserDetails('admin', ['ROLE_USER'])
        def req = request.setAttribute(SecurityFilter.AUTHENTICATION, authentication)
        log.debug("Authentication set: ${authentication.username}")
        return chain.proceed(req)
    }
}