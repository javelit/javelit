/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.catheu.jeamlit.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;

/**
 * In streamlit, userState (free key-value storage in session_state) and
 * componentsState (widget states) are made available via the same map.
 * This can cause confusion because in most cases the components state should not be edited
 * via the map.
 * In Jeamlit, the user state is made available in {@link Jt#sessionState()}.
 * The components state is made available in {@link Jt#componentsState()}.
 */
class InternalSessionState {
    // readable/writable by users
    private final Map<String, Object> userState = new ConcurrentHashMap<>();
    // componentKey  -> value (not writable by users)
    // not using a ConcurrentHashMap because need to support null values
    // concurrency on this map is an edge case, not the normal case so should be fine
    private final Map<String, Object> componentsState = Collections.synchronizedMap(new HashMap<>());

    // (formComponentKey -> (componentKey -> value) (internal only - not visible to users)
    // values that are not applied yet - they are pending because controlled by a form
    private final Map<String, Map<String, Object>> pendingInFormComponentsState = new ConcurrentHashMap<>();

    // set of component keys to reset after the run of the script - the update has to be sent to the frontend.
    private final Set<String> formComponentsToReset = new HashSet<>();

    private String callbackComponentKey;

    record UrlContext(@Nonnull String currentPath,
                             @Nonnull Map<String, List<String>> queryParameters) {
    }

    private UrlContext urlContext;

    protected InternalSessionState() {
    }

    Map<String, Object> getUserState() {
        return userState;
    }

    Map<String, Object> getComponentsState() {
        return componentsState;
    }

    String getCallbackComponentKey() {
        return callbackComponentKey;
    }

    void setCallbackComponentKey(String callbackComponentKey) {
        this.callbackComponentKey = callbackComponentKey;
    }

    protected Map<String, Map<String, Object>> pendingInFormComponentsState() {
        return pendingInFormComponentsState;
    }

    public Set<String> formComponentsToReset() {
        return formComponentsToReset;
    }

    public void setUrlContext(UrlContext urlContext) {
        this.urlContext = urlContext;
    }

    public UrlContext getUrlContext() {
        return urlContext;
    }
}
