package com.arenastrike.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OAuthIdentityVerifier {
    private final RestClient restClient;
    private final String googleClientId;
    private final String facebookAppId;
    private final String facebookAppSecret;
    private final String facebookApiVersion;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public OAuthIdentityVerifier(
            RestClient.Builder restClientBuilder,
            @Value("${arenastrike.oauth.google-client-id:}") String googleClientId,
            @Value("${arenastrike.oauth.facebook-app-id:}") String facebookAppId,
            @Value("${arenastrike.oauth.facebook-app-secret:}") String facebookAppSecret,
            @Value("${arenastrike.oauth.facebook-api-version:v26.0}") String facebookApiVersion) {
        this.restClient = restClientBuilder.build();
        this.googleClientId = googleClientId;
        this.facebookAppId = facebookAppId;
        this.facebookAppSecret = facebookAppSecret;
        this.facebookApiVersion = facebookApiVersion;
        this.googleIdTokenVerifier = googleClientId.isBlank() || googleClientId.startsWith("YOUR_")
                ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(List.of(googleClientId))
                        .build();
    }

    public VerifiedIdentity verify(String rawProvider, String credential) {
        String provider = rawProvider == null ? "" : rawProvider.trim().toLowerCase(Locale.ROOT);
        if (credential == null || credential.isBlank()) {
            throw unauthorized("OAuth credential is missing");
        }
        return switch (provider) {
            case "google" -> verifyGoogle(credential);
            case "facebook" -> verifyFacebook(credential);
            default -> throw unauthorized("Unsupported OAuth provider");
        };
    }

    private VerifiedIdentity verifyGoogle(String idToken) {
        if (googleIdTokenVerifier == null) {
            ResponseStatusException e = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google OAuth is not configured on the server");
            e.printStackTrace();
            System.err.println("OAUTH ERROR: " + e.getMessage());
            throw e;
        }

        try {
            GoogleIdToken token = googleIdTokenVerifier.verify(idToken);
            if (token == null) {
                throw unauthorized("Invalid Google credential");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            String subject = payload.getSubject();
            if (subject == null || subject.isBlank()) {
                throw unauthorized("Invalid Google credential");
            }

            String email = value(payload.getEmail());
            Object emailVerifiedClaim = payload.get("email_verified");
            if (!email.isBlank() && Boolean.FALSE.equals(emailVerifiedClaim)) {
                throw unauthorized("Google email is not verified");
            }
            return new VerifiedIdentity("google", subject, email, value(payload.get("name")));
        } catch (ResponseStatusException e) {
            e.printStackTrace();
            System.err.println("OAUTH ERROR: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("OAUTH ERROR: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google credential", e);
        }
    }

    @SuppressWarnings("unchecked")
    private VerifiedIdentity verifyFacebook(String userAccessToken) {
        if (facebookAppId.isBlank() || facebookAppId.startsWith("YOUR_") || facebookAppSecret.isBlank()) {
            ResponseStatusException e = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Facebook OAuth is not configured on the server");
            e.printStackTrace();
            System.err.println("OAUTH ERROR: " + e.getMessage());
            throw e;
        }

        try {
            String appAccessToken = facebookAppId + "|" + facebookAppSecret;
            Map<String, Object> debugResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder.scheme("https").host("graph.facebook.com")
                            .pathSegment(facebookApiVersion, "debug_token")
                            .queryParam("input_token", userAccessToken)
                            .queryParam("access_token", appAccessToken).build())
                    .retrieve().body(Map.class);
            Map<String, Object> debugData = debugResponse == null
                    ? null : (Map<String, Object>) debugResponse.get("data");
            String subject = stringValue(debugData, "user_id");
            if (!Boolean.TRUE.equals(debugData == null ? null : debugData.get("is_valid"))
                    || !facebookAppId.equals(stringValue(debugData, "app_id"))
                    || subject.isBlank()) {
                throw unauthorized("Invalid Facebook credential");
            }

            Map<String, Object> profile = restClient.get()
                    .uri(uriBuilder -> uriBuilder.scheme("https").host("graph.facebook.com")
                            .pathSegment(facebookApiVersion, "me")
                            .queryParam("fields", "id,name,email")
                            .queryParam("access_token", userAccessToken).build())
                    .retrieve().body(Map.class);
            if (profile == null || !subject.equals(stringValue(profile, "id"))) {
                throw unauthorized("Invalid Facebook profile");
            }
            String name = stringValue(profile, "name");
            if (name.isBlank()) {
                throw unauthorized("Facebook profile has no name");
            }
            return new VerifiedIdentity("facebook", subject, stringValue(profile, "email"), name);
        } catch (ResponseStatusException e) {
            e.printStackTrace();
            System.err.println("OAUTH ERROR: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("OAUTH ERROR: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Facebook credential", e);
        }
    }

    private static String stringValue(Map<String, Object> values, String key) {
        return value(values == null ? null : values.get(key));
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    public record VerifiedIdentity(String provider, String subject, String email, String name) {}
}
