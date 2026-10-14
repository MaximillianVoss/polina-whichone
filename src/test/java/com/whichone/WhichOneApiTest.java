package com.whichone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whichone.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WhichOneApiTest {
    private static final AtomicInteger SERIAL = new AtomicInteger();
    private static final String PASSWORD = "Study-pass-2026";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired Flyway flyway;
    private String username;
    private String token;

    @BeforeEach
    void registerThroughRealHttp() throws Exception {
        username = "learner_" + SERIAL.incrementAndGet();
        token = register(username).path("accessToken").asText();
    }

    @Test
    void registrationStoresBcryptAndReturnsSafeUserAndWorkingToken() throws Exception {
        var stored = users.findByUsername(username).orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("$2a$");
        assertThat(passwords.matches(PASSWORD, stored.getPasswordHash())).isTrue();
        var me = call(get("/api/me").header("Authorization", bearer(token)), 200);
        assertThat(me.path("username").asText()).isEqualTo(username);
        assertThat(me.toString()).doesNotContain("password", "hash", "role");
    }

    @Test
    void duplicateNamesAreCaseInsensitiveAndReturnConflict() throws Exception {
        call(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username.toUpperCase(), "password", PASSWORD))), 409);
    }

    @Test
    void loginRejectsWrongPasswordAndAuthenticatesCorrectPassword() throws Exception {
        call(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", "Wrong-pass-2026"))), 401);
        var login = call(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", PASSWORD))), 200);
        call(get("/api/me").header("Authorization", bearer(login.path("accessToken").asText())), 200);
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        call(get("/api/me"), 401);
        call(get("/api/quizzes/mine"), 401);
        call(get("/api/me/statistics"), 401);
    }

    @Test
    void draftIsVisibleOnlyToOwnerAndPublishedQuizIsInPublicFeed() throws Exception {
        var quiz = createEducational(token);
        long id = quiz.path("id").asLong();
        call(get("/api/quizzes/" + id), 404);
        call(get("/api/quizzes/" + id).header("Authorization", bearer(token)), 200);
        String foreign = register("foreign_" + SERIAL.incrementAndGet()).path("accessToken").asText();
        call(get("/api/quizzes/" + id).header("Authorization", bearer(foreign)), 404);
        publish(id, token);
        var publicQuiz = call(get("/api/quizzes/" + id), 200);
        assertThat(publicQuiz.path("published").asBoolean()).isTrue();
        var feed = call(get("/api/quizzes"), 200);
        assertThat(feed.toString()).contains(quiz.path("title").asText());
    }

    @Test
    void foreignUserCannotUpdatePublishOrDeleteQuiz() throws Exception {
        var quiz = createEducational(token);
        long id = quiz.path("id").asLong();
        String foreign = register("foreign_" + SERIAL.incrementAndGet()).path("accessToken").asText();
        call(put("/api/quizzes/" + id).header("Authorization", bearer(foreign))
                .contentType(MediaType.APPLICATION_JSON).content(educationalJson()), 403);
        call(post("/api/quizzes/" + id + "/publish").header("Authorization", bearer(foreign)), 403);
        call(delete("/api/quizzes/" + id).header("Authorization", bearer(foreign)), 403);
        assertThat(call(get("/api/quizzes/" + id).header("Authorization", bearer(token)), 200)
                .path("published").asBoolean()).isFalse();
    }

    @Test
    void ownerCanReplaceOrDeleteDraft() throws Exception {
        var quiz = createEducational(token);
        long id = quiz.path("id").asLong();
        var updated = call(put("/api/quizzes/" + id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(educationalJson()), 200);
        assertThat(updated.path("id").asLong()).isEqualTo(id);
        mvc.perform(delete("/api/quizzes/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        call(get("/api/quizzes/" + id).header("Authorization", bearer(token)), 404);
    }

    @Test
    void publishedQuizCannotBeChangedOrDeletedAndPublishIsIdempotent() throws Exception {
        var quiz = createEducational(token);
        long id = quiz.path("id").asLong();
        publish(id, token);
        publish(id, token);
        call(put("/api/quizzes/" + id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(educationalJson()), 409);
        call(delete("/api/quizzes/" + id).header("Authorization", bearer(token)), 409);
    }

    @Test
    void unpublishedQuizCannotBeAttemptedEvenByAuthor() throws Exception {
        var quiz = createEducational(token);
        submit(quiz, token, answers(quiz, 0, 0), 409);
    }

    @Test
    void educationalScoreIsComputedFromServerAnswersAndQuestionPoints() throws Exception {
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var attempt = submit(quiz, token, answers(quiz, 0, 1), 201);
        assertThat(attempt.path("score").asInt()).isEqualTo(3);
        assertThat(attempt.path("maxScore").asInt()).isEqualTo(5);
        assertThat(attempt.path("answers").get(0).path("correct").asBoolean()).isTrue();
        assertThat(attempt.path("answers").get(1).path("correct").asBoolean()).isFalse();
    }

    @Test
    void foreignQuizOptionIsRejectedWithoutSavingAttempt() throws Exception {
        var quiz = createEducational(token);
        var other = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var invalid = answers(quiz, 0, 0);
        invalid.get(0).put("optionId", other.path("questions").get(0).path("options").get(0).path("id").asLong());
        submit(quiz, token, invalid, 400);
        assertThat(call(get("/api/me/attempts").header("Authorization", bearer(token)), 200).size()).isZero();
    }

    @Test
    void optionBelongingToAnotherQuestionIsRejected() throws Exception {
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var invalid = answers(quiz, 0, 0);
        invalid.get(0).put("optionId", quiz.path("questions").get(1).path("options").get(0).path("id").asLong());
        submit(quiz, token, invalid, 400);
    }

    @Test
    void missingOrDuplicateAnswersAreRejected() throws Exception {
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var two = answers(quiz, 0, 0);
        submit(quiz, token, List.of(two.get(0)), 400);
        submit(quiz, token, List.of(two.get(0), two.get(0)), 400);
    }

    @Test
    void personalityTieReturnsFirstDeclaredOutcome() throws Exception {
        var quiz = call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(personalityJson()), 201);
        publish(quiz.path("id").asLong(), token);
        var result = submit(quiz, token, answers(quiz, 0, 1), 201);
        assertThat(result.path("outcomeCode").asText()).isEqualTo("ZEBRA");
        assertThat(result.path("outcomeLabel").asText()).isEqualTo("Планировщик");
        assertThat(result.path("score").isNull()).isTrue();
    }

    @Test
    void statisticsUseSavedAttemptsAndAveragePercentIsNullUntilEducationalAttempt() throws Exception {
        var before = call(get("/api/me/statistics").header("Authorization", bearer(token)), 200);
        assertThat(before.path("totalAttempts").asLong()).isZero();
        assertThat(before.path("averagePercent").isNull()).isTrue();
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        submit(quiz, token, answers(quiz, 0, 1), 201); // 60%
        submit(quiz, token, answers(quiz, 0, 0), 201); // 100%
        var personal = call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(personalityJson()), 201);
        publish(personal.path("id").asLong(), token);
        submit(personal, token, answers(personal, 0, 0), 201);
        var after = call(get("/api/me/statistics").header("Authorization", bearer(token)), 200);
        assertThat(after.path("totalAttempts").asLong()).isEqualTo(3);
        assertThat(after.path("educationalAttempts").asLong()).isEqualTo(2);
        assertThat(after.path("personalityAttempts").asLong()).isEqualTo(1);
        assertThat(after.path("averagePercent").asDouble()).isEqualTo(80.0);
        assertThat(after.path("bestPercent").asDouble()).isEqualTo(100.0);
    }

    @Test
    void attemptHistoryBelongsToAuthenticatedUserOnly() throws Exception {
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var result = submit(quiz, token, answers(quiz, 0, 0), 201);
        long id = result.path("id").asLong();
        String foreign = register("foreign_" + SERIAL.incrementAndGet()).path("accessToken").asText();
        call(get("/api/me/attempts/" + id).header("Authorization", bearer(foreign)), 404);
        assertThat(call(get("/api/me/attempts").header("Authorization", bearer(foreign)), 200).size()).isZero();
        call(get("/api/me/attempts/" + id).header("Authorization", bearer(token)), 200);
    }

    @Test
    void quizReadDtosNeverLeakCorrectFlagsOutcomeWeightsOrHashes() throws Exception {
        var quiz = createEducational(token);
        assertThat(quiz.toString()).doesNotContain("correct", "weight", "password", "outcomeCode");
        publish(quiz.path("id").asLong(), token);
        assertThat(call(get("/api/quizzes/" + quiz.path("id").asLong()), 200).toString())
                .doesNotContain("correct", "weight", "password", "outcomeCode");
        var personal = call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(personalityJson()), 201);
        publish(personal.path("id").asLong(), token);
        assertThat(call(get("/api/quizzes/" + personal.path("id").asLong()), 200).toString())
                .doesNotContain("correct", "weight", "outcomeCode");
    }

    @Test
    void invalidInputAndUnknownFieldsReturn400() throws Exception {
        call(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ok_name\",\"password\":\"short\"}"), 400);
        call(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin_try\",\"password\":\"Study-pass-2026\",\"role\":\"ADMIN\"}"), 400);
        var quiz = json.readTree(educationalJson());
        ((com.fasterxml.jackson.databind.node.ObjectNode) quiz).put("authorId", 999);
        call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(quiz.toString()), 400);
        var valid = createEducational(token);
        publish(valid.path("id").asLong(), token);
        call(post("/api/quizzes/" + valid.path("id").asLong() + "/attempts")
                .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[],\"score\":999}"), 400);
    }

    @Test
    void quizSemanticValidationRejectsAmbiguousCorrectAnswersAndUnknownOutcomes() throws Exception {
        String badEducational = educationalJson().replace("\"correct\":true", "\"correct\":false");
        call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(badEducational), 400);
        String badPersonality = personalityJson().replace("\"outcomeCode\":\"ZEBRA\"", "\"outcomeCode\":\"FOREIGN\"");
        call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(badPersonality), 400);
    }

    @Test
    void forgedExpiredAndWrongIssuerTokensAreRejected() throws Exception {
        call(get("/api/me").header("Authorization", "Bearer definitely.not.signed"), 401);
        String expired = issueToken(Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600), "whichone", "whichone-api");
        call(get("/api/me").header("Authorization", bearer(expired)), 401);
        String wrongIssuer = issueToken(Instant.now(), Instant.now().plusSeconds(3600), "foreign", "whichone-api");
        call(get("/api/me").header("Authorization", bearer(wrongIssuer)), 401);
        String wrongAudience = issueToken(Instant.now(), Instant.now().plusSeconds(3600), "whichone", "foreign-api");
        call(get("/api/me").header("Authorization", bearer(wrongAudience)), 401);
    }

    @Test
    void flywaySecondMigrationIsNoOpAndRetainsUser() {
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(users.findByUsername(username)).isPresent();
    }

    @Test
    void homepageDemoAndOpenApiAreAccessible() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("WhichOne")));
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        var docs = call(get("/v3/api-docs"), 200);
        assertThat(docs.path("paths").has("/api/auth/login")).isTrue();
        assertThat(docs.path("components").path("securitySchemes").has("BearerAuth")).isTrue();
    }

    private String issueToken(Instant issued, Instant expires, String issuer, String audience) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(username)
                .audience(List.of(audience)).issuedAt(issued).expiresAt(expires).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private JsonNode register(String name) throws Exception {
        return call(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", name, "password", PASSWORD))), 201);
    }

    private JsonNode createEducational(String accessToken) throws Exception {
        return call(post("/api/quizzes").header("Authorization", bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON).content(educationalJson()), 201);
    }

    private void publish(long id, String accessToken) throws Exception {
        call(post("/api/quizzes/" + id + "/publish").header("Authorization", bearer(accessToken)), 200);
    }

    private JsonNode submit(JsonNode quiz, String accessToken, List<Map<String, Long>> answers, int expectedStatus) throws Exception {
        return call(post("/api/quizzes/" + quiz.path("id").asLong() + "/attempts")
                .header("Authorization", bearer(accessToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("answers", answers))), expectedStatus);
    }

    private List<Map<String, Long>> answers(JsonNode quiz, int firstOption, int secondOption) {
        var first = quiz.path("questions").get(0);
        var second = quiz.path("questions").get(1);
        return List.of(
                new java.util.HashMap<>(Map.of("questionId", first.path("id").asLong(),
                        "optionId", first.path("options").get(firstOption).path("id").asLong())),
                new java.util.HashMap<>(Map.of("questionId", second.path("id").asLong(),
                        "optionId", second.path("options").get(secondOption).path("id").asLong())));
    }

    private JsonNode call(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String response = mvc.perform(request).andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }

    private static String bearer(String accessToken) { return "Bearer " + accessToken; }

    private String educationalJson() {
        return """
                {"title":"Java basics %s","description":"Two questions, five points","type":"EDUCATIONAL","outcomes":[],
                 "questions":[
                   {"text":"How much is 2 + 2?","points":3,"options":[
                     {"text":"4","correct":true,"weight":0},{"text":"5","correct":false,"weight":0}]},
                   {"text":"Which keyword creates a class?","points":2,"options":[
                     {"text":"class","correct":true,"weight":0},{"text":"table","correct":false,"weight":0}]}]}
                """.formatted(SERIAL.incrementAndGet());
    }

    private String personalityJson() {
        return """
                {"title":"Learning style","description":"A demonstration, not a medical assessment","type":"PERSONALITY",
                 "outcomes":[
                   {"code":"ZEBRA","label":"Планировщик","description":"You prefer a plan."},
                   {"code":"ALPHA","label":"Исследователь","description":"You prefer experimenting."}],
                 "questions":[
                   {"text":"How do you start?","points":0,"options":[
                     {"text":"Make a plan","correct":false,"outcomeCode":"ZEBRA","weight":2},
                     {"text":"Try something","correct":false,"outcomeCode":"ALPHA","weight":2}]},
                   {"text":"What helps you learn?","points":0,"options":[
                     {"text":"A schedule","correct":false,"outcomeCode":"ZEBRA","weight":2},
                     {"text":"Experiments","correct":false,"outcomeCode":"ALPHA","weight":2}]}]}
                """;
    }
}
