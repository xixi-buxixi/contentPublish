package com.example.pulsedistro.api;

import com.example.pulsedistro.dto.adapt.RecordSummaryResponse;
import com.example.pulsedistro.service.AdaptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "LANGCHAIN4J_API_KEY=")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PulseDistroApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdaptService adaptService;

    @Test
    void httpApiSupportsCreateUploadAdaptPublishAndMockData() throws Exception {
        String sessionJson = mockMvc.perform(post("/api/session/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("""
                                {"clientType":"web","nickname":"smoke"}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String userToken = extract(sessionJson, "userToken");
        String traceId = extract(sessionJson, "traceId");

        String taskJson = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("""
                                {
                                  "title": "HTTP 联调任务",
                                  "sourceType": "MARKDOWN",
                                  "rawContent": "# 开场\\n\\n一条接口烟测内容。\\n\\n- 上传图片\\n- 模拟发布"
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String taskId = extract(taskJson, "taskId");

        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("HTTP 联调任务"));

        String mediaJson = mockMvc.perform(multipart("/api/tasks/{taskId}/media", taskId)
                        .file(new MockMultipartFile("file", "cover.png", "image/png", tinyPng()))
                .param("alt", "封面"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.publicUrl").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String mediaId = extract(mediaJson, "mediaId");

        mockMvc.perform(get("/media/{mediaId}", mediaId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        mockMvc.perform(get("/api/configs/platforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].platform").value("xiaohongshu"));

        String adaptJson = mockMvc.perform(post("/api/tasks/{taskId}/adapt", taskId)
                        .header("X-User-Token", userToken)
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("""
                                {"platforms":["xiaohongshu"],"forceRegenerate":false}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.data.records[0].status").value("ADAPTING"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String recordId = extract(adaptJson, "recordId");

        await().untilAsserted(() -> assertThat(adaptService.listRecords(taskId))
                .extracting(RecordSummaryResponse::status)
                .containsOnly("READY"));

        mockMvc.perform(post("/api/tasks/{taskId}/publish", taskId)
                        .header("X-User-Token", userToken)
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("""
                                {"mode":"mock","platforms":["xiaohongshu"],"clientSessionId":"web-smoke"}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mode").value("mock"))
                .andExpect(jsonPath("$.data.results[0].status").value("SUCCESS"));

        mockMvc.perform(get("/api/mock/xiaohongshu/{recordId}", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.containsString("一条接口烟测内容")));
    }

    private String extract(String json, String fieldName) {
        String needle = "\"" + fieldName + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) {
            throw new AssertionError("field not found: " + fieldName + " in " + json);
        }
        int valueStart = start + needle.length();
        int valueEnd = json.indexOf('"', valueStart);
        return json.substring(valueStart, valueEnd);
    }

    private byte[] json(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] tinyPng() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
