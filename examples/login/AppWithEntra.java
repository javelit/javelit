
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
import io.javelit.core.helpers.OAuth2Configuration;
import io.javelit.core.helpers.OAuth2UserClaims;

public class AppWithEntra {


    private static final String CLIENT_ID = System.getenv("ENTRA_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("ENTRA_CLIENT_SECRET");
    private static final String TENANT_ID = System.getenv("ENTRA_TENANT_ID");
    private static final String REDIRECT_URI = System.getenv("ENTRA_REDIRECT_URL");
    private static final String SCOPE = System.getenv("ENTRA_SCOPE");
    private static final String TOKEN_ENDPOINT = String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/token", TENANT_ID);
    private static final String AUTHORIZATION_ENDPOINT = String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize", TENANT_ID);

    public static void main(String[] args) throws UnsupportedEncodingException {
        Jt.title("Welcome to Javelit! \uD83D\uDEA1").use();
        
            var currentPage = Jt.navigation(Jt.page("/dashboard", () -> {
                renderDashboardPage();
            })).withOauth2(OAuth2Configuration.builder(CLIENT_ID, CLIENT_SECRET, TOKEN_ENDPOINT, AUTHORIZATION_ENDPOINT, REDIRECT_URI).scope(SCOPE).iDPName("Microsoft")).use();
            currentPage.run();
    }

    private static void renderDashboardPage() {
        Jt.header("Dashboard").use();
        OAuth2UserClaims userClaims = (OAuth2UserClaims) Jt.sessionState().get(Jt.SESSION_USER_KEY);
        Jt.markdown("You are logged in: " + userClaims.name()).use();
        userClaims.roles().forEach(role -> {
            Jt.markdown("Role: " + role).use();
        });
    }


}
