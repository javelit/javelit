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

import jakarta.annotation.Nonnull;


/**
 * Configuration holder for OAuth2 authentication parameters.
 * <p>
 * Use the {@link Builder} to construct an instance.
 */
public class OAuth2Configuration {

    @Nonnull private final String clientId;
    @Nonnull private final String clientSecret;
    @Nonnull private final String tokenUrl;
    @Nonnull private final String authorizationUrl;
    @Nonnull private final String redirectUri;
    private final String scope;
    private final String iDPName;

    private OAuth2Configuration(@Nonnull Builder builder) {
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.tokenUrl = builder.tokenUrl;
        this.authorizationUrl = builder.authorizationUrl;
        this.redirectUri = builder.redirectUri;
        this.scope = builder.scope;
        this.iDPName = builder.iDPName;
    }


    /**
     * Creates a new {@link Builder} for {@link OAuth2Configuration}.
     *
     * @param clientId OAuth2 client ID
     * @param clientSecret OAuth2 client secret
     * @param tokenUrl OAuth2 token endpoint URL
     * @param authorizationUrl OAuth2 authorization endpoint URL
     * @param redirectUri OAuth2 redirect URI
     * @return a new Builder instance
     */
    public static Builder builder(final @Nonnull String clientId, final @Nonnull String clientSecret,
                                  final @Nonnull String tokenUrl, final @Nonnull String authorizationUrl,
                                  final @Nonnull String redirectUri) {
        return new Builder(clientId, clientSecret, tokenUrl, authorizationUrl, redirectUri);
    }


    /**
     * @return the OAuth2 client ID
     */
    public String getClientId() {
        return clientId;
    }


    /**
     * @return the OAuth2 client secret
     */
    public String getClientSecret() {
        return clientSecret;
    }


    /**
     * @return the OAuth2 token endpoint URL
     */
    public String getTokenUrl() {
        return tokenUrl;
    }


    /**
     * @return the OAuth2 authorization endpoint URL
     */
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }


    /**
     * @return the OAuth2 redirect URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }


    /**
     * @return the identity provider name, or null if not set
     */
    public String getiDPName() {
        return iDPName;
    }

    public String getScope() {
        return scope;
    }


    /**
     * Builder for {@link OAuth2Configuration}.
     * <p>
     * Use this class to fluently construct an OAuth2Configuration instance.
     */
    public static class Builder {
        @Nonnull private String clientId;
        @Nonnull private String clientSecret;
        @Nonnull private String tokenUrl;
        @Nonnull private String authorizationUrl;
        @Nonnull private String redirectUri;
        private String scope;
        private String iDPName;

        private Builder(final @Nonnull String clientId, final @Nonnull String clientSecret,
                                  final @Nonnull String tokenUrl, final @Nonnull String authorizationUrl,
                                  final @Nonnull String redirectUri) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.tokenUrl = tokenUrl;
            this.authorizationUrl = authorizationUrl;
            this.redirectUri = redirectUri;
        }

        /**
         * Sets the OAuth2 scope.
         * @param scope the scope
         * @return this builder
         */
        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }


        /**
         * Sets the OAuth2 client ID.
         * @param clientId the client ID
         * @return this builder
         */
        public Builder clientId(@Nonnull String clientId) {
            this.clientId = clientId;
            return this;
        }


        /**
         * Sets the OAuth2 client secret.
         * @param clientSecret the client secret
         * @return this builder
         */
        public Builder clientSecret(@Nonnull String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }


        /**
         * Sets the OAuth2 token endpoint URL.
         * @param tokenUrl the token endpoint URL
         * @return this builder
         */
        public Builder tokenUrl(@Nonnull String tokenUrl) {
            this.tokenUrl = tokenUrl;
            return this;
        }


        /**
         * Sets the OAuth2 authorization endpoint URL.
         * @param authorizationUrl the authorization endpoint URL
         * @return this builder
         */
        public Builder authorizationUrl(@Nonnull String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
            return this;
        }


        /**
         * Sets the OAuth2 redirect URI.
         * @param redirectUri the redirect URI
         * @return this builder
         */
        public Builder redirectUri(@Nonnull String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }


        /**
         * Sets the identity provider name (optional).
         * @param iDPName the identity provider name
         * @return this builder
         */
        public Builder iDPName(String iDPName) {
            this.iDPName = iDPName;
            return this;
        }


        /**
         * Builds the {@link OAuth2Configuration} instance.
         * @return a new OAuth2Configuration
         */
        public OAuth2Configuration build() {
            return new OAuth2Configuration(this);
        }
    }
}
