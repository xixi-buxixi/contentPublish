package com.example.pulsedistro.stage1;

import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.dto.media.MediaUploadResponse;
import com.example.pulsedistro.dto.task.CreateTaskRequest;
import com.example.pulsedistro.dto.task.TaskSummaryResponse;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.ContentBlock;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.model.NormalizedContent;
import com.example.pulsedistro.repository.ContentTaskRepository;
import com.example.pulsedistro.repository.MediaResourceRepository;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import com.example.pulsedistro.service.MediaService;
import com.example.pulsedistro.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class TaskMediaStageOneIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContentTaskRepository taskRepository;

    @Autowired
    private MediaResourceRepository mediaRepository;

    @Autowired
    private PlatformPublishRecordRepository recordRepository;

    @Value("${pulse.media.storage-root}")
    private String storageRoot;

    @Test
    void createTaskPreservesMarkdownListShapeAndSplitsInlineImages() {
        TaskSummaryResponse created = taskService.createTask(new CreateTaskRequest(
                "多平台内容分发",
                "MARKDOWN",
                "# 核心观点\n\n开头 ![内联图](https://example.com/inline.png) 结尾。\n\n- 小红书要轻量\n  1. 子项一\n  2. 子项二\n* 知乎要结构化\n1. 先生成适配稿\n\n![架构图](https://example.com/arch.png)"
        ));

        NormalizedContent normalized = taskService.getNormalizedContent(created.taskId());

        assertThat(created.status()).isEqualTo("PENDING");
        assertThat(normalized.title()).isEqualTo("多平台内容分发");
        assertThat(normalized.summary()).isEqualTo("开头");
        assertThat(normalized.blocks())
                .extracting(ContentBlock::type)
                .containsExactly("heading", "paragraph", "image", "paragraph", "list", "list", "list", "list", "list", "image");
        assertThat(normalized.blocks().stream()
                .filter(block -> "paragraph".equals(block.type()))
                .map(ContentBlock::text))
                .containsExactly("开头", "结尾。")
                .noneMatch(text -> text.contains("内联图"));
        assertThat(normalized.blocks().stream()
                .filter(block -> "list".equals(block.type())))
                .extracting(ContentBlock::text, ContentBlock::ordered, ContentBlock::depth)
                .containsExactly(
                        tuple("小红书要轻量", false, 0),
                        tuple("子项一", true, 1),
                        tuple("子项二", true, 1),
                        tuple("知乎要结构化", false, 0),
                        tuple("先生成适配稿", true, 0)
                );
        assertThat(normalized.blocks().stream()
                .filter(block -> "image".equals(block.type())))
                .extracting(block -> block.media().publicUrl(), block -> block.media().alt())
                .containsExactly(
                        tuple("https://example.com/inline.png", "内联图"),
                        tuple("https://example.com/arch.png", "架构图")
                );
    }

    @Test
    void mediaUploadStoresPublicUrlAndDeleteRejectsNormalizedReferences() {
        TaskSummaryResponse created = taskService.createTask(new CreateTaskRequest(
                "带图任务",
                "MARKDOWN",
                "正文"
        ));
        MockMultipartFile image = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                tinyPng()
        );

        MediaUploadResponse uploaded = mediaService.store(created.taskId(), image, "封面");
        taskService.updateNormalizedContent(created.taskId(), new NormalizedContent(
                "带图任务",
                "正文",
                List.of(new ContentBlock(
                        "image",
                        null,
                        null,
                        new MediaRef(uploaded.mediaId(), "http://client/ignored.png", "封面", null, null)
                ))
        ));

        assertThat(uploaded.publicUrl()).isEqualTo("http://localhost:8080/media/" + uploaded.mediaId());
        assertThat(mediaService.listByTask(created.taskId())).hasSize(1);
        assertThatThrownBy(() -> mediaService.deleteMedia(created.taskId(), uploaded.mediaId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409);
    }

    @Test
    void deleteTaskApiRemovesTaskRecordsMediaRowsAndPhysicalDirectory() throws Exception {
        TaskSummaryResponse created = taskService.createTask(new CreateTaskRequest(
                "可删除任务",
                "MARKDOWN",
                "正文"
        ));
        MediaUploadResponse uploaded = mediaService.store(created.taskId(), new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                tinyPng()
        ), "封面");
        Path taskMediaDir = Path.of(storageRoot).toAbsolutePath().normalize()
                .resolve(created.taskId())
                .normalize();
        assertThat(Files.isDirectory(taskMediaDir)).isTrue();

        PlatformPublishRecord record = new PlatformPublishRecord(created.taskId(), "xiaohongshu");
        record.markReady("标题", "正文", "[]", "[]", "ready");
        recordRepository.save(record);

        mockMvc.perform(delete("/api/tasks/{taskId}", created.taskId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(taskRepository.existsById(created.taskId())).isFalse();
        assertThat(mediaRepository.findByTaskIdOrderByCreatedAtAsc(created.taskId())).isEmpty();
        assertThat(recordRepository.findByTaskIdOrderByCreatedAtAsc(created.taskId())).isEmpty();
        assertThat(Files.exists(taskMediaDir)).isFalse();
        assertThat(uploaded.mediaId()).isNotBlank();
    }

    private byte[] tinyPng() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
