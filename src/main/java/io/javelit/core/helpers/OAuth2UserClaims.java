
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
package io.javelit.core.helpers;


import java.util.List;

/**
 * Represents OAuth2 user claims.
 *
 * @param subject the subject (user identifier)
 * @param name the user's name
 * @param email the user's email
 * @param oid the user's OID
 * @param roles the user's roles
 * @param groups the user's groups
 */
public record OAuth2UserClaims(
    String subject,
    String name,
    String email,
    String oid,
    List<String> roles,
    List<String> groups
) {
    /**
     * Checks if the user has the specified role.
     * @param role the role to check
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
