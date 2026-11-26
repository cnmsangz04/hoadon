package vn.hoadon.ai.knowledge.service;

import org.springframework.stereotype.Component;
import vn.hoadon.ai.knowledge.config.KnowledgeProperties;
import vn.hoadon.ai.knowledge.model.KnowledgeChunkDraft;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KnowledgeChunker {
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern TABLE_SEPARATOR = Pattern.compile("^\\s*\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*:?-{3,}:?\\s*\\|?\\s*$");

    private final KnowledgeProperties properties;

    public KnowledgeChunker(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public List<KnowledgeChunkDraft> chunk(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<Block> blocks = parseBlocks(content.replace("\r\n", "\n").replace('\r', '\n'));
        List<KnowledgeChunkDraft> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentHeading = null;
        int startLine = 1;
        int endLine = 1;

        for (Block block : blocks) {
            if (block.text().length() > properties.getChunkMaxCharacters()) {
                if (block.atomic()) {
                    throw new IllegalArgumentException("Bảng hoặc code block quá lớn, vui lòng chia nhỏ tài liệu");
                }
                for (Block split : splitLargeBlock(block)) {
                    if (wouldOverflow(current, split.text())) {
                        addChunk(result, current, currentHeading, startLine, endLine);
                        currentHeading = null;
                    }
                    if (current.isEmpty()) {
                        startLine = split.startLine();
                        currentHeading = split.heading();
                    }
                    append(current, split.text());
                    endLine = split.endLine();
                }
                continue;
            }
            boolean newSection = block.heading() != null && currentHeading != null
                    && !block.heading().equals(currentHeading);
            if (!current.isEmpty() && (wouldOverflow(current, block.text())
                    || (newSection && current.length() >= properties.getChunkTargetCharacters() / 2))) {
                addChunk(result, current, currentHeading, startLine, endLine);
                currentHeading = null;
            }
            if (current.isEmpty()) {
                startLine = block.startLine();
                currentHeading = block.heading();
            }
            append(current, block.text());
            endLine = block.endLine();
        }
        addChunk(result, current, currentHeading, startLine, endLine);
        return result;
    }

    private List<Block> parseBlocks(String content) {
        String[] lines = content.split("\n", -1);
        List<Block> blocks = new ArrayList<>();
        String heading = null;
        int index = 0;
        while (index < lines.length) {
            if (lines[index].isBlank()) { index++; continue; }
            Matcher headingMatcher = HEADING.matcher(lines[index]);
            if (headingMatcher.matches()) {
                heading = headingMatcher.group(2).trim();
                blocks.add(new Block(lines[index], heading, index + 1, index + 1, true));
                index++;
                continue;
            }
            if (lines[index].stripLeading().startsWith("```") || lines[index].stripLeading().startsWith("~~~")) {
                int start = index;
                String marker = lines[index].stripLeading().substring(0, 3);
                index++;
                while (index < lines.length && !lines[index].stripLeading().startsWith(marker)) index++;
                if (index < lines.length) index++;
                blocks.add(block(lines, start, index, heading, true));
                continue;
            }
            if (isTableStart(lines, index)) {
                int start = index;
                index += 2;
                while (index < lines.length && lines[index].contains("|") && !lines[index].isBlank()) index++;
                blocks.add(block(lines, start, index, heading, true));
                continue;
            }
            int start = index;
            index++;
            while (index < lines.length && !lines[index].isBlank() && !HEADING.matcher(lines[index]).matches()
                    && !lines[index].stripLeading().startsWith("```")
                    && !lines[index].stripLeading().startsWith("~~~") && !isTableStart(lines, index)) index++;
            blocks.add(block(lines, start, index, heading, false));
        }
        return blocks;
    }

    private boolean isTableStart(String[] lines, int index) {
        return index + 1 < lines.length && lines[index].contains("|") && TABLE_SEPARATOR.matcher(lines[index + 1]).matches();
    }

    private Block block(String[] lines, int start, int endExclusive, String heading, boolean atomic) {
        StringBuilder value = new StringBuilder();
        for (int index = start; index < endExclusive; index++) {
            if (!value.isEmpty()) value.append('\n');
            value.append(lines[index]);
        }
        return new Block(value.toString().trim(), heading, start + 1, Math.max(start + 1, endExclusive), atomic);
    }

    private List<Block> splitLargeBlock(Block block) {
        List<Block> result = new ArrayList<>();
        String[] lines = block.text().split("\n");
        StringBuilder current = new StringBuilder();
        int currentStart = block.startLine();
        for (int index = 0; index < lines.length; index++) {
            if (!current.isEmpty() && current.length() + lines[index].length() + 1 > properties.getChunkMaxCharacters()) {
                result.add(new Block(current.toString(), block.heading(), currentStart,
                        block.startLine() + index - 1, false));
                current.setLength(0);
                currentStart = block.startLine() + index;
            }
            append(current, lines[index]);
        }
        if (!current.isEmpty()) result.add(new Block(current.toString(), block.heading(), currentStart, block.endLine(), false));
        return result;
    }

    private boolean wouldOverflow(StringBuilder current, String next) {
        return !current.isEmpty() && current.length() + next.length() + 2 > properties.getChunkTargetCharacters();
    }

    private void addChunk(List<KnowledgeChunkDraft> result, StringBuilder content, String heading,
                          int startLine, int endLine) {
        if (content.isEmpty()) return;
        result.add(new KnowledgeChunkDraft(result.size(), heading,
                "Dòng " + startLine + "-" + endLine, content.toString().trim()));
        content.setLength(0);
    }

    private void append(StringBuilder target, String value) {
        if (!target.isEmpty()) target.append("\n\n");
        target.append(value);
    }

    private record Block(String text, String heading, int startLine, int endLine, boolean atomic) {
    }
}
