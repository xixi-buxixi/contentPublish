package com.example.pulsedistro.service;

import com.example.pulsedistro.domain.ContentTask;
import com.example.pulsedistro.domain.MediaResource;
import com.example.pulsedistro.dto.task.CreateTaskRequest;
import com.example.pulsedistro.dto.task.TaskSummaryResponse;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.ContentBlock;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.model.NormalizedContent;
import com.example.pulsedistro.repository.ContentTaskRepository;
import com.example.pulsedistro.repository.MediaResourceRepository;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TaskService {

    private final ContentTaskRepository taskRepository;
    private final MediaResourceRepository mediaRepository;
    private final JsonContentMapper jsonMapper;
    private final Parser markdownParser;
    private final TextCollectingVisitor textCollector = new TextCollectingVisitor();

    public TaskService(
            ContentTaskRepository taskRepository,
            MediaResourceRepository mediaRepository,
            JsonContentMapper jsonMapper
    ) {
        this.taskRepository = taskRepository;
        this.mediaRepository = mediaRepository;
        this.jsonMapper = jsonMapper;
        this.markdownParser = Parser.builder().build();
    }

    @Transactional
    public TaskSummaryResponse createTask(CreateTaskRequest request) {
        String title = requireText(request.title(), "title is required");
        String sourceType = normalizeSourceType(request.sourceType());
        String rawContent = request.rawContent() == null ? "" : request.rawContent();
        NormalizedContent normalized = parseMarkdown(title, rawContent);
        ContentTask task = new ContentTask(title, sourceType, rawContent, jsonMapper.write(normalized));

        return toSummary(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskSummaryResponse getTaskSummary(String taskId) {
        return toSummary(getTask(taskId));
    }

    @Transactional(readOnly = true)
    public NormalizedContent getNormalizedContent(String taskId) {
        ContentTask task = getTask(taskId);
        return jsonMapper.readNormalized(task.getNormalizedContentJson());
    }

    @Transactional
    public NormalizedContent updateNormalizedContent(String taskId, NormalizedContent normalizedContent) {
        ContentTask task = getTask(taskId);
        NormalizedContent enriched = enrichMediaRefs(taskId, normalizedContent);
        task.setTitle(requireText(enriched.title(), "title is required"));
        task.setNormalizedContentJson(jsonMapper.write(enriched));
        return enriched;
    }

    private ContentTask getTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "task not found"));
    }

    private NormalizedContent parseMarkdown(String title, String rawContent) {
        List<ContentBlock> blocks = new ArrayList<>();
        Node document = markdownParser.parse(rawContent == null ? "" : rawContent);
        for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
            appendBlock(blocks, node);
        }

        String summary = blocks.stream()
                .filter(block -> "paragraph".equals(block.type()))
                .map(ContentBlock::text)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");

        return new NormalizedContent(title, summary, List.copyOf(blocks));
    }

    private void appendBlock(List<ContentBlock> blocks, Node node) {
        if (node instanceof Heading heading) {
            String text = collectText(heading);
            if (StringUtils.hasText(text)) {
                blocks.add(new ContentBlock("heading", heading.getLevel(), text, null));
            }
            return;
        }

        if (node instanceof BulletList || node instanceof OrderedList) {
            appendListItems(blocks, node);
            return;
        }

        if (node instanceof Paragraph paragraph) {
            appendParagraph(blocks, paragraph);
            return;
        }

        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            appendBlock(blocks, child);
        }
    }

    private void appendListItems(List<ContentBlock> blocks, Node listNode) {
        for (Node child = listNode.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof ListItem) {
                String text = collectText(child);
                if (StringUtils.hasText(text)) {
                    blocks.add(new ContentBlock("list", null, text, null));
                }
            }
        }
    }

    private void appendParagraph(List<ContentBlock> blocks, Paragraph paragraph) {
        List<Image> images = imagesIn(paragraph);
        if (!images.isEmpty()) {
            String paragraphText = collectText(paragraph);
            for (Image image : images) {
                blocks.add(new ContentBlock("image", null, null, mediaRef(image)));
            }
            boolean paragraphOnlyDescribesImage = images.stream()
                    .map(image -> image.getText().toString())
                    .anyMatch(alt -> alt.equals(paragraphText));
            if (paragraphOnlyDescribesImage) {
                return;
            }
            if (StringUtils.hasText(paragraphText)) {
                blocks.add(new ContentBlock("paragraph", null, paragraphText, null));
            }
            return;
        }

        String text = collectText(paragraph);
        if (StringUtils.hasText(text)) {
            blocks.add(new ContentBlock("paragraph", null, text, null));
        }
    }

    private List<Image> imagesIn(Node node) {
        List<Image> images = new ArrayList<>();
        collectImages(node, images);
        return images;
    }

    private void collectImages(Node node, List<Image> images) {
        if (node instanceof Image image) {
            images.add(image);
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            collectImages(child, images);
        }
    }

    private MediaRef mediaRef(Image image) {
        String url = image.getUrl().toString();
        return new MediaRef(mediaIdFromUrl(url), url, image.getText().toString(), null, null);
    }

    private String mediaIdFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        int marker = url.indexOf("/media/");
        if (marker < 0) {
            return null;
        }
        String value = url.substring(marker + "/media/".length());
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        return StringUtils.hasText(value) ? value : null;
    }

    private String collectText(Node node) {
        return textCollector.collectAndGetText(node).trim();
    }

    private NormalizedContent enrichMediaRefs(String taskId, NormalizedContent normalizedContent) {
        List<ContentBlock> blocks = normalizedContent.blocks() == null
                ? List.of()
                : normalizedContent.blocks().stream()
                .map(block -> enrichBlock(taskId, block))
                .toList();
        String summary = normalizedContent.summary() == null ? "" : normalizedContent.summary();

        return new NormalizedContent(normalizedContent.title(), summary, blocks);
    }

    private ContentBlock enrichBlock(String taskId, ContentBlock block) {
        if (!"image".equals(block.type()) || block.media() == null || !StringUtils.hasText(block.media().mediaId())) {
            return block;
        }

        MediaResource media = mediaRepository.findByIdAndTaskId(block.media().mediaId(), taskId)
                .orElseThrow(() -> new BusinessException(400, "media does not belong to task"));
        MediaRef mediaRef = new MediaRef(
                media.getId(),
                media.getPublicUrl(),
                block.media().alt(),
                media.getWidth(),
                media.getHeight()
        );
        return new ContentBlock(block.type(), block.level(), block.text(), mediaRef);
    }

    private String normalizeSourceType(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            return "MARKDOWN";
        }
        return sourceType.trim().toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private TaskSummaryResponse toSummary(ContentTask task) {
        return new TaskSummaryResponse(
                task.getId(),
                task.getTitle(),
                task.getSourceType(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
