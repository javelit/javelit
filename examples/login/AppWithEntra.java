//DEPS com.google.code.gson:gson:2.10.1
//DEPS com.nimbusds:nimbus-jose-jwt:10.7
//DEPS org.apache.httpcomponents.client5:httpclient5:5.3.1


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;


import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import io.javelit.core.Jt;

public class AppWithEntra {

    private static final String SESSION_USER = "current_user";
    private static final String SESSION_TOKEN = "auth_token";
    private static final String clientId = System.getenv("ENTRA_CLIENT_ID");
    private static final String clientSecret = System.getenv("ENTRA_CLIENT_SECRET");
    private static final String tenantId = System.getenv("ENTRA_TENANT_ID");
    private static final String redirectUri = System.getenv("ENTRA_REDIRECT_URL");
    private static final String tokenEndpoint = String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenantId);
    private static final String authorizationEndpoint = String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize", tenantId);

    public static void main(String[] args) throws UnsupportedEncodingException {
        Jt.title("Welcome to Javelit! \uD83D\uDEA1").use();
        
        boolean loggedIn = Jt.sessionState().computeIfAbsentBoolean("logged_in", k -> false);

        if (!loggedIn) {
            var currentPage = Jt.navigation(Jt.page("/login", () -> {
                loginPage();
            }), Jt.page("/auth/callback", () -> {
                renderCallbackPage();
                Jt.rerun(true);
            })).hidden().use();
            currentPage.run();
        } else {
            var currentPage = Jt.navigation(Jt.page("/dashboard", () -> {
                renderDashboardPage();
            }), Jt.page("/logout", () -> {
                Jt.sessionState().clear();
                Jt.rerun(true);
            }).title("Logout")).use();
            currentPage.run();
        }
    }

    private static void loginPage() throws UnsupportedEncodingException {
        Jt.text("Please authenticate with your Microsoft 365 account").use();

        // Generate a state parameter for CSRF protection
        String state = UUID.randomUUID().toString();

        // Get the authorization URL
        String authUrl = getAuthorizationUrl(state);

        Jt.pageLink(authUrl, "Login with Microsoft").target("_self").use();
    }

    private static void renderDashboardPage() {
        Jt.header("Dashboard").use();
        UserClaims userClaims = (UserClaims) Jt.sessionState().get(SESSION_USER);
        Jt.markdown("You are logged in: " + userClaims.getName()).use();
        userClaims.getRoles().forEach(role -> {
            Jt.markdown("Role: " + role).use();
        });
    }

    public static String getAuthorizationUrl(String state) throws UnsupportedEncodingException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", clientId);
        params.put("response_type", "code");
        params.put("redirect_uri", redirectUri);
        params.put("scope", "openid profile email Directory.Read.All");
        params.put("state", state);

        return authorizationEndpoint + "?" + buildQueryString(params);
    }

    private static String buildQueryString(Map<String, String> params) throws UnsupportedEncodingException {
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

    private static void renderCallbackPage() {

        Map<String, List<String>> params = Jt.urlQueryParameters();

        // Get the authorization code and state from the query parameters
        String code = Optional.ofNullable(params.get("code")).map(l -> l.get(0)).orElse(null);
        String state = Optional.ofNullable(params.get("state")).map(l -> l.get(0)).orElse(null);
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
            TokenResponse tokenResponse = getTokenFromAuthCode(code);
            UserClaims userClaims = parseToken(tokenResponse.getIdToken());
            // Store user info and tokens in session
            Jt.sessionState().put(SESSION_USER, userClaims);
            Jt.sessionState().put(SESSION_TOKEN, tokenResponse.getAccessToken());
            Jt.sessionState().put("logged_in", true);
        } catch (SecurityException e) {
            // State validation failed - potential CSRF attack
            renderErrorPage("Security Error", "Invalid state parameter - possible CSRF attack");
        } catch (Exception e) {
            // Token exchange failed
            renderErrorPage("Authentication Failed", e.getMessage());
        }
    }

    public static TokenResponse getTokenFromAuthCode(String authCode) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(tokenEndpoint);

            Map<String, String> params = new LinkedHashMap<>();
            params.put("client_id", clientId);
            params.put("client_secret", clientSecret);
            params.put("code", authCode);
            params.put("redirect_uri", redirectUri);
            params.put("grant_type", "authorization_code");
            params.put("scope", "openid profile email");

            httpPost.setEntity(new StringEntity(buildQueryString(params), StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            return httpClient.execute(httpPost, response -> {
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                return parseTokenResponse(responseBody);
            });
        }
    }

    private static TokenResponse parseTokenResponse(String responseBody) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(jsonObject.get("access_token").getAsString());
        tokenResponse.setIdToken(jsonObject.get("id_token").getAsString());
        tokenResponse.setExpiresIn(jsonObject.get("expires_in").getAsInt());

        if (jsonObject.has("refresh_token")) {
            tokenResponse.setRefreshToken(jsonObject.get("refresh_token").getAsString());
        }

        return tokenResponse;
    }

    public static UserClaims parseToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

        UserClaims userClaims = new UserClaims();
        userClaims.setSubject(claimsSet.getSubject());
        userClaims.setName((String) claimsSet.getClaim("name"));
        userClaims.setEmail((String) claimsSet.getClaim("email"));
        userClaims.setOid((String) claimsSet.getClaim("oid")); // Object ID

        // Extract roles from the token
        Object rolesClaim = claimsSet.getClaim("roles");
        if (rolesClaim != null) {
            if (rolesClaim instanceof List) {
                userClaims.setRoles((List<String>) rolesClaim);
            } else {
                userClaims.setRoles(Collections.singletonList((String) rolesClaim));
            }
        } else {
            userClaims.setRoles(Collections.emptyList());
        }

        // Parse custom claims if present
        Object groups = claimsSet.getClaim("groups");
        if (groups != null) {
            userClaims.setGroups((List<String>) groups);
        }

        return userClaims;
    }

    private static void renderErrorPage(String errorTitle, String errorMessage) {

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
    /**
     * Model class for token response
     */
    public static class TokenResponse {
        private String accessToken;
        private String idToken;
        private String refreshToken;
        private int expiresIn;

        // Getters and setters
        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getIdToken() {
            return idToken;
        }

        public void setIdToken(String idToken) {
            this.idToken = idToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(int expiresIn) {
            this.expiresIn = expiresIn;
        }
    }

    public static class UserClaims {
        private String subject;
        private String name;
        private String email;
        private String oid;
        private List<String> roles;
        private List<String> groups;

        // Getters and setters
        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getOid() {
            return oid;
        }

        public void setOid(String oid) {
            this.oid = oid;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public List<String> getGroups() {
            return groups;
        }

        public void setGroups(List<String> groups) {
            this.groups = groups;
        }

        public boolean hasRole(String role) {
            return roles != null && roles.contains(role);
        }
    }

}
