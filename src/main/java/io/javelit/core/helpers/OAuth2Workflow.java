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

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.javelit.core.Jt;
import io.javelit.core.JtPage;
import io.javelit.core.JtRunnable;
import jakarta.annotation.Nonnull;

public class OAuth2Workflow {

    private static final String EXPIRES_IN = "expires_in";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String ID_TOKEN = "id_token";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String STATE2 = "state";
    private static final String SCOPE = "scope";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String CODE = "code";
    private static final String RESPONSE_TYPE = "response_type";
    private static final String CLIENT_ID = "client_id";
    private static final String SESSION_TOKEN_KEY = "token";
    @Nonnull private OAuth2Configuration oAuth2Configuration;

    public OAuth2Workflow(@Nonnull OAuth2Configuration oAuth2Configuration) {
        this.oAuth2Configuration = oAuth2Configuration;
    }

    public JtRunnable loginPageRenderer = () -> {
        Jt.text(String.format("Please authenticate with your %s account",oAuth2Configuration.getiDPName())).use();

        // Generate a state parameter for CSRF protection
        String state = UUID.randomUUID().toString();

        // Get the authorization URL
        String authUrl = getAuthorizationUrl(state);

        Jt.pageLink(authUrl, "Login with "+ oAuth2Configuration.getiDPName()).target("_self").use();
    };

    public JtRunnable callbackPageRenderer = () -> {
        Map<String, List<String>> params = Jt.urlQueryParameters();

        // Get the authorization code and state from the query parameters
        String code = Optional.ofNullable(params.get("code")).map(l -> l.get(0)).orElse(null);
        List<String> error = Optional.ofNullable(params.get("error")).orElse(List.of());
        List<String> errorDescription = Optional.ofNullable(params.get("error_description")).orElse(List.of());
        
        // Check for errors from Microsoft
        if (!error.isEmpty()) {
            renderErrorPage(error.get(0),
                    !errorDescription.isEmpty() ? errorDescription.get(0) : "");
            return;
        }

        try {
            // Process the authorization code and get tokens
            OAuth2TokenResponse tokenResponse = getTokenFromAuthCode(code);
            OAuth2UserClaims userClaims = extractUserClaimsFromToken(tokenResponse.idToken());
            // Store user info and tokens in session
            Jt.sessionState().put(Jt.SESSION_USER_KEY, userClaims);
            Jt.sessionState().put(SESSION_TOKEN_KEY, tokenResponse.accessToken());
            Jt.sessionState().put(Jt.SESSION_LOGGED_IN_KEY, true);
        } catch (SecurityException e) {
            // State validation failed - potential CSRF attack
            renderErrorPage("Security Error", "Invalid state parameter - possible CSRF attack");
        } catch (Exception e) {
            // Token exchange failed
            renderErrorPage("Authentication Failed", e.getMessage());
        }

        Jt.rerun(true);
    };

    public String getAuthorizationUrl(String state) throws UnsupportedEncodingException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(CLIENT_ID, oAuth2Configuration.getClientId());
        params.put(RESPONSE_TYPE, CODE);
        params.put(REDIRECT_URI, oAuth2Configuration.getRedirectUri());
        params.put(SCOPE, oAuth2Configuration.getScope());
        params.put(STATE2, state);

        try {
            return new URIBuilder(oAuth2Configuration.getAuthorizationUrl())
                .addParameter(CLIENT_ID, oAuth2Configuration.getClientId())
                .addParameter(RESPONSE_TYPE, CODE)
                .addParameter(REDIRECT_URI, oAuth2Configuration.getRedirectUri())
                .addParameter(SCOPE, oAuth2Configuration.getScope())
                .addParameter(STATE2, state).build().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid Authorization URL", e);
        }

        //return oAuth2Configuration.getAuthorizationUrl() + "?" + buildQueryString(params);
    }

    private String buildQueryString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return sb.toString();
    }

    public List<JtPage.Builder> getLoginPages() {
        List<JtPage.Builder> loginPages = new ArrayList<>();
        JtPage.Builder loginPageBuilder = JtPage.builder("/login", this.loginPageRenderer);
        loginPages.add(loginPageBuilder);
        JtPage.Builder callbackPageBuilder = JtPage.builder("/auth/callback", this.callbackPageRenderer);
        loginPages.add(callbackPageBuilder);
        return loginPages;
    }

    private void renderErrorPage(String errorTitle, String errorMessage) {

        var container = Jt.container().key("error").use();
        Jt.title("Authentication Error").use(container);
        Jt.header("Authentication Failed").use(container);
        Jt.text("Error: " + errorTitle).use(container);
        Jt.text("Details: " + errorMessage).use(container);

        // Retry button
        if (Jt.button("Try Again").use(container)) {
            Jt.switchPage("/");
        }
    }

    private OAuth2TokenResponse getTokenFromAuthCode(String authCode) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(oAuth2Configuration.getTokenUrl());

            Map<String, String> params = new LinkedHashMap<>();
            params.put(CLIENT_ID, oAuth2Configuration.getClientId());
            params.put(CLIENT_SECRET, oAuth2Configuration.getClientSecret());
            params.put(CODE, authCode);
            params.put(REDIRECT_URI, oAuth2Configuration.getRedirectUri());
            params.put(GRANT_TYPE, AUTHORIZATION_CODE);
            params.put(SCOPE, "openid profile email");

            List<NameValuePair> paramsv2 = List.of(
                new org.apache.http.message.BasicNameValuePair(CLIENT_ID, oAuth2Configuration.getClientId()),
                new org.apache.http.message.BasicNameValuePair(CLIENT_SECRET, oAuth2Configuration.getClientSecret()),
                new org.apache.http.message.BasicNameValuePair(CODE, authCode),
                new org.apache.http.message.BasicNameValuePair(REDIRECT_URI, oAuth2Configuration.getRedirectUri()),
                new org.apache.http.message.BasicNameValuePair(GRANT_TYPE, AUTHORIZATION_CODE),
                new org.apache.http.message.BasicNameValuePair(SCOPE, "openid profile email")
            );

            httpPost.setEntity(new UrlEncodedFormEntity(paramsv2, StandardCharsets.UTF_8));
            //httpPost.setEntity(new StringEntity(buildQueryString(params), StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            return httpClient.execute(httpPost, response -> {
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                return parseTokenResponse(responseBody);
            });
        }
    }

    private OAuth2TokenResponse parseTokenResponse(String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

        return new OAuth2TokenResponse(
            jsonObject.get(ACCESS_TOKEN).getAsString(),
            jsonObject.get(ID_TOKEN).getAsString(),
            jsonObject.has(REFRESH_TOKEN) ? jsonObject.get(REFRESH_TOKEN).getAsString() : null,
            jsonObject.get(EXPIRES_IN).getAsInt()
        );
    }

    private OAuth2UserClaims extractUserClaimsFromToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();


        // Extract roles from the token
        Object rolesClaim = claimsSet.getClaim("roles");
        List<String> roles = new ArrayList<>();
        if (rolesClaim != null) {
            if (rolesClaim instanceof List) {
                roles = (List<String>) rolesClaim;
            } else {
               roles.add((String) rolesClaim);
            }
        } 

        // Parse custom claims if present
        Object groups = claimsSet.getClaim("groups");

        return new OAuth2UserClaims(
            claimsSet.getSubject(),
            (String) claimsSet.getClaim("name"),
            (String) claimsSet.getClaim("email"),
            (String) claimsSet.getClaim("oid"),
            roles,
            groups != null ? (List<String>) groups : null

        );
    }

    public static OAuth2UserClaims getCurrentUser() {
        return (OAuth2UserClaims) Jt.sessionState().get(Jt.SESSION_USER_KEY);
    }

}
