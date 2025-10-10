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
package io.jeamlit.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.jeamlit.core.utils.Preconditions.checkState;

/**
 * In streamlit, userState (free key-value storage in session_state) and
 * componentsState (widget states) are made available via the same map.
 * This can cause confusion because in most cases the components state should not be edited
 * via the map.
 * In Jeamlit, the user state is made available in {@link Jt#sessionState()}.
 * The components state is made available in {@link Jt#componentsState()}.
 */
final class InternalSessionState {
    // readable/writable by users
    private final Map<String, Object> userState = new ConcurrentHashMap<>();

    // {componentKey: value}  - NOT EXPOSED TO USERS
    // not using a ConcurrentHashMap because need to support null values
    // concurrency on this map is an edge case, not the normal case so should be fine
    private final Map<String, Object> componentsState = Collections.synchronizedMap(new HashMap<>());

    // {componentUserKey: value} - exposed to users but should not be modifiable directly when exposed
    // it's the responsibility of the internal logic (non public API) that gets this map to perform an immutable map wrapping or even consider a deep copy
    // not using a ConcurrentHashMap because need to support null values
    // concurrency on this map is an edge case, not the normal case so should be fine
    private final Map<String, Object> userVisibleComponentsState = Collections.synchronizedMap(new HashMap<>());

    private final Map<@NotNull String, @NotNull String> internalKeyToUserKey = new ConcurrentHashMap<>();

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

    private JtPage lastExecutionPage;

    InternalSessionState() {
    }

    Map<String, Object> getUserState() {
        return userState;
    }

    Map<String, Object> getUserVisibleComponentsState() {
        return userVisibleComponentsState;
    }

    void removeComponentState(@Nonnull String componentKey) {
        componentsState.remove(componentKey);
        if (internalKeyToUserKey.containsKey(componentKey)) {
            userVisibleComponentsState.remove(internalKeyToUserKey.get(componentKey));
            internalKeyToUserKey.remove(componentKey);
        }
    }

    void removeAllComponentsWithPrefix(@Nonnull String prefix) {
        componentsState.keySet().removeIf(key -> key.startsWith(prefix));
        userVisibleComponentsState.keySet().removeIf(key -> key.startsWith(prefix));
        internalKeyToUserKey.keySet().removeIf(key -> key.startsWith(prefix));
    }

    Object getComponentState(final @Nonnull String componentKey) {
        return componentsState.get(componentKey);
    }

    void upsertComponentsState(final @Nonnull JtComponent component) {
        componentsState.put(component.getKey(), component.returnValue());
        if (component.getUserKey() != null) {
            final String prefixedUserKey = StateManager.pagePrefix() + component.getUserKey();
            internalKeyToUserKey.put(component.getKey(), prefixedUserKey);
            userVisibleComponentsState.put(prefixedUserKey, component.returnValue());
        }
    }

    // corresponds to a frontend update
    void updateComponentsState(final @Nonnull String componentKey, final Object updatedValue) {
        // this is a precondition - caller already ensures (and must), but keeping it here for safer refactorings and early catching of bugs
        checkState(componentsState.containsKey(componentKey), "Implementation error. Please reach out to support.");
        componentsState.put(componentKey, updatedValue);
        if (internalKeyToUserKey.containsKey(componentKey)) {
            final String prefixedUserKey = internalKeyToUserKey.get(componentKey);
            userVisibleComponentsState.put(prefixedUserKey, updatedValue);
        }
    }

    void updateAllComponentsState(final Map<@NotNull String, @Nullable Object> componentKeyToUpdatedValue) {
        for (final Map.Entry<String, Object> entry : componentKeyToUpdatedValue.entrySet()) {
            updateComponentsState(entry.getKey(), entry.getValue());
        }
    }

    String getCallbackComponentKey() {
        return callbackComponentKey;
    }

    void setCallbackComponentKey(String callbackComponentKey) {
        this.callbackComponentKey = callbackComponentKey;
    }

    Map<String, Map<String, Object>> pendingInFormComponentsState() {
        return pendingInFormComponentsState;
    }

    Set<String> formComponentsToReset() {
        return formComponentsToReset;
    }

    void setUrlContext(UrlContext urlContext) {
        this.urlContext = urlContext;
    }

    UrlContext getUrlContext() {
        return urlContext;
    }

    void setLastExecutionPage(JtPage lastExecutionPage) {
        this.lastExecutionPage = lastExecutionPage;
    }

    JtPage getLastExecutionPage() {
        return lastExecutionPage;
    }
}
