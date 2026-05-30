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
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final ContentTaskRepository taskRepository;
    private final MediaResourceRepository mediaRepository;
    private final PlatformPublishRecordRepository recordRepository;
    private final JsonContentMapper jsonMapper;
    private final Parser markdownParser;
    private final TextCollectingVisitor textCollector = new TextCollectingVisitor();
    private final Path storageRoot;

    public TaskService(
            ContentTaskRepository taskRepository,
            MediaResourceRepository mediaRepository,
            PlatformPublishRecordRepository recordRepository,
            JsonContentMapper jsonMapper,
            @Value("${pulse.media.storage-root:data/media}") String storageRoot
    ) {
        this.taskRepository = taskRepository;
        this.mediaRepository = mediaRepository;
        this.recordRepository = recordRepository;
        this.jsonMapper = jsonMapper;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.markdownParser = Parser.builder()
                .extensions(List.of(StrikethroughExtension.create()))
                .build();
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

    @Transactional
    public void deleteTask(String taskId) {
        ContentTask task = getTask(taskId);
        recordRepository.deleteByTaskId(taskId);
        mediaRepository.deleteByTaskId(taskId);
        taskRepository.delete(task);
        Path taskMediaDirectory = taskMediaDirectory(taskId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    deleteDirectoryQuietly(taskMediaDirectory);
                } catch (RuntimeException e) {
                    log.warn("failed to delete task media directory after database commit: {}", taskMediaDirectory, e);
                }
            }
        });
    }

    private ContentTask getTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "task not found"));
    }

    private NormalizedContent parseMarkdown(String title, String rawContent) {
        List<ContentBlock> blocks = new ArrayList<>();
        Node document = markdownParser.parse(rawContent == null ? "" : rawContent);
        for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
            appendBlock(blocks, node, 0);
        }

        String summary = blocks.stream()
                .filter(block -> "paragraph".equals(block.type()))
                .map(ContentBlock::text)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");

        return new NormalizedContent(title, summary, List.copyOf(blocks));
    }

    private void appendBlock(List<ContentBlock> blocks, Node node, int depth) {
        if (node instanceof Heading heading) {
            String text = collectText(heading);
            if (StringUtils.hasText(text)) {
                blocks.add(new ContentBlock("heading", heading.getLevel(), text, null));
            }
            return;
        }

        if (node instanceof BulletList || node instanceof OrderedList) {
            appendListItems(blocks, node, node instanceof OrderedList, depth);
            return;
        }

        if (node instanceof Paragraph paragraph) {
            appendParagraph(blocks, paragraph);
            return;
        }

        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            appendBlock(blocks, child, depth);
        }
    }

    private void appendListItems(List<ContentBlock> blocks, Node listNode, boolean ordered, int depth) {
        for (Node child = listNode.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof ListItem) {
                ListItemParts parts = collectListItemLead(child);
                if (StringUtils.hasText(parts.text())) {
                    blocks.add(new ContentBlock("list", null, parts.text(), null, ordered, depth));
                }
                parts.images().forEach(image -> blocks.add(new ContentBlock("image", null, null, image)));
                boolean leadConsumed = false;
                for (Node itemChild = child.getFirstChild(); itemChild != null; itemChild = itemChild.getNext()) {
                    if (itemChild instanceof BulletList || itemChild instanceof OrderedList) {
                        appendListItems(blocks, itemChild, itemChild instanceof OrderedList, depth + 1);
                    } else if (!leadConsumed) {
                        appendContinuationAfterFirstLine(blocks, itemChild);
                        leadConsumed = true;
                    } else {
                        appendBlock(blocks, itemChild, depth);
                    }
                }
            }
        }
    }

    private void appendParagraph(List<ContentBlock> blocks, Paragraph paragraph) {
        StringBuilder text = new StringBuilder();
        for (Node child = paragraph.getFirstChild(); child != null; child = child.getNext()) {
            appendParagraphInline(blocks, child, text);
        }
        flushParagraph(blocks, text);
    }

    private void flushParagraph(List<ContentBlock> blocks, StringBuilder text) {
        String value = text.toString().trim();
        if (StringUtils.hasText(value)) {
            blocks.add(new ContentBlock("paragraph", null, value, null));
        }
        text.setLength(0);
    }

    private ListItemParts collectListItemLead(Node listItem) {
        StringBuilder text = new StringBuilder();
        List<MediaRef> images = new ArrayList<>();
        boolean[] stopped = {false};
        for (Node child = listItem.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof BulletList || child instanceof OrderedList) {
                continue;
            }
            appendListInline(child, text, images, stopped);
            break;
        }
        return new ListItemParts(text.toString().trim(), List.copyOf(images));
    }

    private void appendParagraphInline(List<ContentBlock> blocks, Node node, StringBuilder text) {
        if (node instanceof Image image) {
            flushParagraph(blocks, text);
            blocks.add(new ContentBlock("image", null, null, mediaRef(image)));
            return;
        }
        appendMarkdownText(node, text, (mediaRef) -> {
            flushParagraph(blocks, text);
            blocks.add(new ContentBlock("image", null, null, mediaRef));
        });
    }

    private void appendListInline(Node node, StringBuilder text, List<MediaRef> images, boolean[] stopped) {
        appendMarkdownTextUntilLineBreak(node, text, images, stopped);
    }

    private void appendContinuationAfterFirstLine(List<ContentBlock> blocks, Node node) {
        StringBuilder text = new StringBuilder();
        boolean[] afterFirstLine = {false};
        appendAfterFirstLine(blocks, node, text, afterFirstLine);
        flushParagraph(blocks, text);
    }

    private String markdownText(Node node) {
        StringBuilder text = new StringBuilder();
        appendMarkdownText(node, text, ignored -> {
        });
        return text.toString();
    }

    private void appendMarkdownText(Node node, StringBuilder text, java.util.function.Consumer<MediaRef> imageConsumer) {
        if (node instanceof Image) {
            imageConsumer.accept(mediaRef((Image) node));
            return;
        }
        if (node instanceof Text textNode) {
            text.append(textNode.getChars().toString());
            return;
        }
        if (node instanceof Code code) {
            text.append("`").append(code.getText()).append("`");
            return;
        }
        if (node instanceof StrongEmphasis) {
            appendWrappedChildren(node, text, imageConsumer, "**");
            return;
        }
        if (node instanceof Emphasis) {
            appendWrappedChildren(node, text, imageConsumer, "*");
            return;
        }
        if (node instanceof Strikethrough) {
            appendWrappedChildren(node, text, imageConsumer, "~~");
            return;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            appendMarkdownText(child, text, imageConsumer);
        }
    }

    private void appendMarkdownTextUntilLineBreak(
            Node node,
            StringBuilder text,
            List<MediaRef> images,
            boolean[] stopped
    ) {
        if (stopped[0]) {
            return;
        }
        if (node instanceof SoftLineBreak) {
            stopped[0] = true;
            return;
        }
        if (node instanceof Image image) {
            images.add(mediaRef(image));
            return;
        }
        if (node instanceof Text textNode) {
            text.append(textNode.getChars().toString());
            return;
        }
        if (node instanceof Code code) {
            text.append("`").append(code.getText()).append("`");
            return;
        }
        if (node instanceof StrongEmphasis) {
            appendWrappedChildrenUntilLineBreak(node, text, images, stopped, "**");
            return;
        }
        if (node instanceof Emphasis) {
            appendWrappedChildrenUntilLineBreak(node, text, images, stopped, "*");
            return;
        }
        if (node instanceof Strikethrough) {
            appendWrappedChildrenUntilLineBreak(node, text, images, stopped, "~~");
            return;
        }
        for (Node child = node.getFirstChild(); child != null && !stopped[0]; child = child.getNext()) {
            appendMarkdownTextUntilLineBreak(child, text, images, stopped);
        }
    }

    private void appendAfterFirstLine(
            List<ContentBlock> blocks,
            Node node,
            StringBuilder text,
            boolean[] afterFirstLine
    ) {
        if (node instanceof SoftLineBreak) {
            afterFirstLine[0] = true;
            return;
        }
        if (afterFirstLine[0]) {
            appendParagraphInline(blocks, node, text);
            return;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            appendAfterFirstLine(blocks, child, text, afterFirstLine);
        }
    }

    private void appendWrappedChildrenUntilLineBreak(
            Node node,
            StringBuilder text,
            List<MediaRef> images,
            boolean[] stopped,
            String marker
    ) {
        text.append(marker);
        for (Node child = node.getFirstChild(); child != null && !stopped[0]; child = child.getNext()) {
            appendMarkdownTextUntilLineBreak(child, text, images, stopped);
        }
        text.append(marker);
    }

    private void appendWrappedChildren(
            Node node,
            StringBuilder text,
            java.util.function.Consumer<MediaRef> imageConsumer,
            String marker
    ) {
        text.append(marker);
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            appendMarkdownText(child, text, imageConsumer);
        }
        text.append(marker);
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
        return new ContentBlock(block.type(), block.level(), block.text(), mediaRef, block.ordered(), block.depth());
    }

    private Path taskMediaDirectory(String taskId) {
        Path target = storageRoot.resolve(taskId).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new BusinessException(400, "invalid storage path");
        }
        return target;
    }

    private void deleteDirectoryQuietly(Path target) {
        if (!Files.exists(target)) {
            return;
        }
        try (var paths = Files.walk(target)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private record ListItemParts(String text, List<MediaRef> images) {
    }
}
