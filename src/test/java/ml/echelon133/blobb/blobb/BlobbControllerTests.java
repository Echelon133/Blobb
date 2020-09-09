package ml.echelon133.blobb.blobb;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.blobb.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@ExtendWith(MockitoExtension.class)
public class BlobbControllerTests {

    static User testUser;

    private MockMvc mockMvc;

    @Mock
    private BlobbService blobbService;

    @InjectMocks
    private BlobbController blobbController;

    @InjectMocks
    private BlobbExceptionHandler blobbExceptionHandler;

    private JacksonTester<FeedBlobb> jsonFeedBlobb;

    private JacksonTester<BlobbInfo> jsonBlobbInfo;

    private JacksonTester<List<FeedBlobb>> jsonFeedBlobbList;

    private JacksonTester<BlobbDto> jsonBlobbDto;

    private JacksonTester<ResponseDto> jsonResponseDto;

    private JacksonTester<ReblobbDto> jsonReblobbDto;

    private JacksonTester<Map<String, String>> jsonBlobbResult;

    @BeforeAll
    public static void beforeAll() {
        testUser = new User("user1", "", "","");
        testUser.setUuid(UUID.randomUUID());
    }

    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());

        // custom filter that lets us use
        // SecurityMockMvcRequestPostProcessors.user(UserDetails user)
        // while doing test request
        SecurityContextPersistenceFilter filter;
        filter = new SecurityContextPersistenceFilter();

        mockMvc = MockMvcBuilders
                .standaloneSetup(blobbController)
                .setControllerAdvice(blobbExceptionHandler)
                .addFilter(filter)
                .build();
    }

    @Test
    public void getBlobbWithUuid_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "asdf";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + invalidUuid)
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void getBlobbWithUuid_DoesntExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.getByUuid(uuid))
                .willThrow(new BlobbDoesntExistException(uuid));

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid)
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getContentAsString())
                .contains(String.format("Blobb with UUID %s doesn't exist", uuid.toString()));
    }

    @Test
    public void getBlobbWithUuid_ReturnsBlobb() throws Exception {
        UUID uuid = UUID.randomUUID();
        FeedBlobb blobb = new FeedBlobb();
        blobb.setUuid(uuid);
        blobb.setAuthor(testUser);

        // expected json
        JsonContent<FeedBlobb> json = jsonFeedBlobb.write(blobb);

        // given
        given(blobbService.getByUuid(uuid)).willReturn(blobb);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid)
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(json.getJson());
    }

    @Test
    public void getInfoAboutBlobbWithUuid_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "asdf";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + invalidUuid + "/info")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void getInfoAboutBlobbWithUuid_DoesntExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.getBlobbInfo(uuid))
                .willThrow(new BlobbDoesntExistException(uuid));

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/info")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getContentAsString())
                .contains(String.format("Blobb with UUID %s doesn't exist", uuid.toString()));
    }

    @Test
    public void getInfoAboutBlobbWithUuid_ReturnsBlobbInfo() throws Exception {
        UUID uuid = UUID.randomUUID();
        BlobbInfo info = new BlobbInfo();
        info.setUuid(uuid);
        info.setLikes(10L);
        info.setReblobbs(20L);
        info.setResponses(5L);

        // expected json
        JsonContent<BlobbInfo> json = jsonBlobbInfo.write(info);

        // given
        given(blobbService.getBlobbInfo(uuid)).willReturn(info);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/info")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(json.getJson());
    }

    @Test
    public void getResponsesToBlobb_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "asdf";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + invalidUuid + "/responses")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void getResponsesToBlobb_DoesntExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.getAllResponsesTo(uuid, 0L, 5L))
                .willThrow(new BlobbDoesntExistException(uuid));

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/responses")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getContentAsString())
                .contains(String.format("Blobb with UUID %s doesn't exist", uuid.toString()));
    }

    @Test
    public void getResponsesToBlobb_NoSkipSetsSkipValueToDefault() throws Exception {
        List<FeedBlobb> responses = List.of(new FeedBlobb(), new FeedBlobb());

        UUID uuid = UUID.randomUUID();

        // expected json
        JsonContent<List<FeedBlobb>> json = jsonFeedBlobbList.write(responses);

        // given
        given(blobbService.getAllResponsesTo(uuid, 0L, 20L))
                .willReturn(responses);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/responses")
                        .accept(APPLICATION_JSON)
                        .param("limit", "20")
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains(json.getJson());
    }

    @Test
    public void getResponsesToBlobb_NoLimitSetsLimitValueToDefault() throws Exception {
        List<FeedBlobb> responses = List.of(new FeedBlobb(), new FeedBlobb());

        UUID uuid = UUID.randomUUID();

        // expected json
        JsonContent<List<FeedBlobb>> json = jsonFeedBlobbList.write(responses);

        // given
        given(blobbService.getAllResponsesTo(uuid, 10L, 5L))
                .willReturn(responses);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/responses")
                        .accept(APPLICATION_JSON)
                        .param("skip", "10")
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains(json.getJson());
    }

    @Test
    public void getResponsesToBlobb_DefaultSkipAndLimitIsCorrect() throws Exception {
        List<FeedBlobb> responses = List.of(new FeedBlobb(), new FeedBlobb());

        UUID uuid = UUID.randomUUID();

        // expected json
        JsonContent<List<FeedBlobb>> json = jsonFeedBlobbList.write(responses);

        // given
        given(blobbService.getAllResponsesTo(uuid, 0L, 5L))
                .willReturn(responses);

        // when
        // don't give skip & limit parameters to see if they are set to 0 & 5
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/responses")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains(json.getJson());
    }

    @Test
    public void getResponsesToBlobb_ProvidedSkipAndLimitAreUsed() throws Exception {
        List<FeedBlobb> responses = List.of(new FeedBlobb(), new FeedBlobb());

        UUID uuid = UUID.randomUUID();

        // expected json
        JsonContent<List<FeedBlobb>> json = jsonFeedBlobbList.write(responses);

        // given
        given(blobbService.getAllResponsesTo(uuid, 10L, 20L))
                .willReturn(responses);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/responses")
                        .param("skip", "10")
                        .param("limit", "20")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains(json.getJson());
    }

    @Test
    public void getReblobbsOfBlobb_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "asdf";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + invalidUuid + "/reblobbs")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void getReblobbsOfBlobb_DoesntExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.getAllReblobbsOf(uuid, 0L, 5L))
                .willThrow(new BlobbDoesntExistException(uuid));

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/reblobbs")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getContentAsString())
                .contains(String.format("Blobb with UUID %s doesn't exist", uuid.toString()));
    }

    @Test
    public void getReblobbsOfBlobb_NoSkipSetsSkipValueToDefault() throws Exception {
        List<FeedBlobb> reblobbs = List.of(new FeedBlobb());

        UUID uuid = UUID.randomUUID();

        // expected json
        JsonContent<List<FeedBlobb>> json = jsonFeedBlobbList.write(reblobbs);

        // given
        given(blobbService.getAllReblobbsOf(uuid, 0L, 20L))
                .willReturn(reblobbs);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/reblobbs")
                        .accept(APPLICATION_JSON)
                        .param("limit", "20")
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains(json.getJson());
    }

    @Test
    public void getReblobbsOfBlobb_NoLimitSetsLimitValueToDefault() throws Exception {
        List<FeedBlobb> reblobbs = List.of(new FeedBlobb());

        UUID uuid = UUID.randomUUID();

        // expected json
        JsonContent<List<FeedBlobb>> json = jsonFeedBlobbList.write(reblobbs);

        // given
        given(blobbService.getAllReblobbsOf(uuid, 10L, 5L))
                .willReturn(reblobbs);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/reblobbs")
                        .accept(APPLICATION_JSON)
                        .param("skip", "10")
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains(json.getJson());
    }

    @Test
    public void getReblobbsOfBlobb_DefaultSkipAndLimitIsCorrect() throws Exception {
        List<FeedBlobb> reblobbs = List.of(new FeedBlobb());

        UUID uuid = UUID.randomUUID();

        // expected json
        JsonContent<List<FeedBlobb>> json = jsonFeedBlobbList.write(reblobbs);

        // given
        given(blobbService.getAllReblobbsOf(uuid, 0L, 5L))
                .willReturn(reblobbs);

        // when
        // don't give skip & limit parameters to see if they are set to 0 & 5
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/reblobbs")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains(json.getJson());
    }

    @Test
    public void getReblobbsOfBlobb_ProvidedSkipAndLimitAreUsed() throws Exception {
        List<FeedBlobb> reblobbs = List.of(new FeedBlobb());

        UUID uuid = UUID.randomUUID();

        // expected json
        JsonContent<List<FeedBlobb>> json = jsonFeedBlobbList.write(reblobbs);

        // given
        given(blobbService.getAllReblobbsOf(uuid, 10L, 20L))
                .willReturn(reblobbs);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/reblobbs")
                        .param("skip", "10")
                        .param("limit", "20")
                        .accept(APPLICATION_JSON)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains(json.getJson());
    }

    @Test
    public void checkIfLikes_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "asdf";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + invalidUuid + "/like")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void checkIfLikes_DoesntExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.checkIfUserWithUuidLikes(testUser, uuid))
                .willThrow(new BlobbDoesntExistException(uuid));

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/like")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getContentAsString())
                .contains(String.format("Blobb with UUID %s doesn't exist", uuid.toString()));
    }

    @Test
    public void checkIfLikes_UserDoesntLike() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.checkIfUserWithUuidLikes(testUser, uuid))
                .willReturn(false);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/like")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains("{\"liked\":false}");
    }

    @Test
    public void checkIfLikes_UserLikes() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.checkIfUserWithUuidLikes(testUser, uuid))
                .willReturn(true);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                get("/api/blobbs/" + uuid + "/like")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains("{\"liked\":true}");
    }

    @Test
    public void likeBlobb_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "asdf";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + invalidUuid + "/like")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void likeBlobb_DoesntExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.likeBlobb(testUser, uuid))
                .willThrow(new BlobbDoesntExistException(uuid));

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + uuid + "/like")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getContentAsString())
                .contains(String.format("Blobb with UUID %s doesn't exist", uuid.toString()));
    }

    @Test
    public void likeBlobb_Failure() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.likeBlobb(testUser, uuid))
                .willReturn(false);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + uuid + "/like")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains("{\"liked\":false}");
    }

    @Test
    public void likeBlobb_Success() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.likeBlobb(testUser, uuid))
                .willReturn(true);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + uuid + "/like")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains("{\"liked\":true}");
    }

    @Test
    public void unlikeBlobb_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "asdf";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + invalidUuid + "/unlike")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void unlikeBlobb_DoesntExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.unlikeBlobb(testUser, uuid))
                .willThrow(new BlobbDoesntExistException(uuid));

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + uuid + "/unlike")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getContentAsString())
                .contains(String.format("Blobb with UUID %s doesn't exist", uuid.toString()));
    }

    @Test
    public void unlikeBlobb_Failure() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.unlikeBlobb(testUser, uuid))
                .willReturn(false);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + uuid + "/unlike")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains("{\"unliked\":false}");
    }

    @Test
    public void unlikeBlobb_Success() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.unlikeBlobb(testUser, uuid))
                .willReturn(true);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + uuid + "/unlike")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .contains("{\"unliked\":true}");
    }

    @Test
    public void postBlobb_RejectsNullContent() throws Exception {
        // json
        String json = "{}";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Field 'content' must be provided");
    }

    @Test
    public void postBlobb_RejectsInvalidBlobbLength() throws Exception {
        BlobbDto dto1 = new BlobbDto();
        BlobbDto dto2 = new BlobbDto();
        dto1.setContent(""); // length 0
        dto2.setContent(
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        ); // length 301

        // json
        JsonContent<BlobbDto> json1 = jsonBlobbDto.write(dto1);
        JsonContent<BlobbDto> json2 = jsonBlobbDto.write(dto2);

        // when
        MockHttpServletResponse response1 = mockMvc.perform(
                post("/api/blobbs")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json1.getJson())
        ).andReturn().getResponse();

        MockHttpServletResponse response2 = mockMvc.perform(
                post("/api/blobbs")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json2.getJson())
        ).andReturn().getResponse();

        // then
        assertThat(response1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response1.getContentAsString())
                .contains("Blobb length is invalid");

        assertThat(response2.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response2.getContentAsString())
                .contains("Blobb length is invalid");
    }

    @Test
    public void postBlobb_AcceptsValidBlobbLength() throws Exception {
        BlobbDto dto1 = new BlobbDto();
        dto1.setContent("This is a test blobb");

        Blobb b = new Blobb(testUser, dto1.getContent());
        b.setUuid(UUID.randomUUID());

        // json
        JsonContent<BlobbDto> json = jsonBlobbDto.write(dto1);
        JsonContent<Map<String, String>> expected = jsonBlobbResult.write(
                Map.of("blobbUUID", b.getUuid().toString(),
                       "content", b.getContent(),
                       "author", testUser.getUsername())
        );

        // given
        given(blobbService.postBlobb(testUser, dto1.getContent())).willReturn(b);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json.getJson())
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(expected.getJson());
    }

    @Test
    public void respondToBlobb_RejectsNullContent() throws Exception {
        UUID postUuid = UUID.randomUUID();

        // json
        String json = "{}";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + postUuid + "/respond")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Field 'content' must be provided");
    }

    @Test
    public void respondToBlobb_RejectsInvalidBlobbLength() throws Exception {
        UUID postUuid = UUID.randomUUID();
        ResponseDto dto1 = new ResponseDto();
        ResponseDto dto2 = new ResponseDto();
        dto1.setContent(""); // length 0
        dto2.setContent(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        ); // length 301

        // json
        JsonContent<ResponseDto> json1 = jsonResponseDto.write(dto1);
        JsonContent<ResponseDto> json2 = jsonResponseDto.write(dto2);

        // when
        MockHttpServletResponse response1 = mockMvc.perform(
                post("/api/blobbs/" + postUuid + "/respond")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json1.getJson())
        ).andReturn().getResponse();

        MockHttpServletResponse response2 = mockMvc.perform(
                post("/api/blobbs/" + postUuid + "/respond")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json2.getJson())
        ).andReturn().getResponse();

        // then
        assertThat(response1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response1.getContentAsString())
                .contains("Response length is invalid");

        assertThat(response2.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response2.getContentAsString())
                .contains("Response length is invalid");
    }

    @Test
    public void respondToBlobb_AcceptsValidBlobbLength() throws Exception {
        UUID postUuid = UUID.randomUUID();
        ResponseDto dto1 = new ResponseDto();
        dto1.setContent("This is a test response");

        Blobb b = new ResponseBlobb(testUser, dto1.getContent(), new Blobb());
        b.setUuid(UUID.randomUUID());

        // json
        JsonContent<ResponseDto> json = jsonResponseDto.write(dto1);
        JsonContent<Map<String, String>> expected = jsonBlobbResult.write(
                Map.of("blobbUUID", b.getUuid().toString(),
                        "content", b.getContent(),
                        "author", testUser.getUsername())
        );

        // given
        given(blobbService.postResponse(testUser, dto1.getContent(), postUuid)).willReturn(b);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + postUuid + "/respond")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json.getJson())
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(expected.getJson());
    }

    @Test
    public void respondToBlobb_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "test";

        ResponseDto dto1 = new ResponseDto();
        dto1.setContent("This is a test response");

        // json
        JsonContent<ResponseDto> json = jsonResponseDto.write(dto1);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + invalidUuid + "/respond")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json.getJson())
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void reblobbOfBlobb_RejectsNullContent() throws Exception {
        UUID postUuid = UUID.randomUUID();

        // json
        String json = "{}";

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + postUuid + "/reblobb")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json)
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Field 'content' must be provided");
    }

    @Test
    public void reblobbOfBlobb_RejectsInvalidBlobbLength() throws Exception {
        UUID postUuid = UUID.randomUUID();
        ReblobbDto dto1 = new ReblobbDto();
        dto1.setContent(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        ); // length 301

        // json
        JsonContent<ReblobbDto> json1 = jsonReblobbDto.write(dto1);

        // when
        MockHttpServletResponse response1 = mockMvc.perform(
                post("/api/blobbs/" + postUuid + "/reblobb")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json1.getJson())
        ).andReturn().getResponse();

        // then
        assertThat(response1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response1.getContentAsString())
                .contains("Reblobb length is invalid");
    }

    @Test
    public void reblobbOfBlobb_AcceptsValidBlobbLength() throws Exception {
        UUID postUuid = UUID.randomUUID();
        ReblobbDto dto1 = new ReblobbDto();
        ReblobbDto dto2 = new ReblobbDto();
        dto1.setContent(""); // reblobb should accept empty content
        dto2.setContent("Test reblobb content");

        Blobb b1 = new Reblobb(testUser, dto1.getContent(), new Blobb());
        b1.setUuid(UUID.randomUUID());
        Blobb b2 = new Reblobb(testUser, dto2.getContent(), new Blobb());
        b2.setUuid(UUID.randomUUID());

        // json
        JsonContent<ReblobbDto> json1 = jsonReblobbDto.write(dto1);
        JsonContent<ReblobbDto> json2 = jsonReblobbDto.write(dto2);
        JsonContent<Map<String, String>> expected1 = jsonBlobbResult.write(
                Map.of("blobbUUID", b1.getUuid().toString(),
                        "content", b1.getContent(),
                        "author", testUser.getUsername())
        );
        JsonContent<Map<String, String>> expected2 = jsonBlobbResult.write(
                Map.of("blobbUUID", b2.getUuid().toString(),
                        "content", b2.getContent(),
                        "author", testUser.getUsername())
        );

        // given
        given(blobbService.postReblobb(testUser, dto1.getContent(), postUuid)).willReturn(b1);
        given(blobbService.postReblobb(testUser, dto2.getContent(), postUuid)).willReturn(b2);

        // when
        MockHttpServletResponse response1 = mockMvc.perform(
                post("/api/blobbs/" + postUuid + "/reblobb")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json1.getJson())
        ).andReturn().getResponse();

        MockHttpServletResponse response2 = mockMvc.perform(
                post("/api/blobbs/" + postUuid + "/reblobb")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json2.getJson())
        ).andReturn().getResponse();

        // then
        assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response1.getContentAsString()).isEqualTo(expected1.getJson());

        assertThat(response2.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response2.getContentAsString()).isEqualTo(expected2.getJson());
    }

    @Test
    public void reblobbOfBlobb_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "test";

        ReblobbDto dto1 = new ReblobbDto();
        dto1.setContent("This is a test reblobb");

        // json
        JsonContent<ReblobbDto> json = jsonReblobbDto.write(dto1);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                post("/api/blobbs/" + invalidUuid + "/reblobb")
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
                        .contentType(APPLICATION_JSON)
                        .content(json.getJson())
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void deleteBlobb_HandlesInvalidUuid() throws Exception {
        String invalidUuid = "test";


        // when
        MockHttpServletResponse response = mockMvc.perform(
                delete("/api/blobbs/" + invalidUuid)
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains("Invalid UUID string");
    }

    @Test
    public void deleteBlobb_CorrectResponseWhenNotAuthorized() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.markBlobbAsDeleted(testUser, uuid))
                .willThrow(new UserCannotDeleteBlobbException(testUser, uuid));

        // when
        MockHttpServletResponse response = mockMvc.perform(
                delete("/api/blobbs/" + uuid)
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString())
                .contains(String.format("User %s cannot delete blobb with %s uuid",
                        testUser.getUsername(), uuid));
    }

    @Test
    public void deleteBlobb_CorrectResponseWhenDeleted() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.markBlobbAsDeleted(testUser, uuid))
                .willReturn(true);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                delete("/api/blobbs/" + uuid)
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .isEqualTo("{\"deleted\":true}");
    }

    @Test
    public void deleteBlobb_CorrectResponseWhenNotDeleted() throws Exception {
        UUID uuid = UUID.randomUUID();

        // given
        given(blobbService.markBlobbAsDeleted(testUser, uuid))
                .willReturn(false);

        // when
        MockHttpServletResponse response = mockMvc.perform(
                delete("/api/blobbs/" + uuid)
                        .accept(APPLICATION_JSON)
                        .with(user(testUser))
        ).andReturn().getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString())
                .isEqualTo("{\"deleted\":false}");
    }
}
