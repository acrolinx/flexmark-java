package com.vladsch.flexmark.ext.jekyll.front.matter.internal;

import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterBlock;
import com.vladsch.flexmark.formatter.*;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JekyllFrontMatterNodeFormatter implements PhasedNodeFormatter {
    public JekyllFrontMatterNodeFormatter(DataHolder options) {
    }

    @Nullable
    @Override
    public Set<FormattingPhase> getFormattingPhases() {
        return new HashSet<>(Collections.singleton(FormattingPhase.DOCUMENT_FIRST));
    }

    @Nullable
    @Override
    public Set<Class<?>> getNodeClasses() {
        return null;
    }

    @Override
    public void renderDocument(@NotNull NodeFormatterContext context, @NotNull MarkdownWriter markdown, @NotNull Document document, @NotNull FormattingPhase phase) {
        if (phase == FormattingPhase.DOCUMENT_FIRST) {
            Node node = document.getFirstChild();
            if (node instanceof JekyllFrontMatterBlock) {
                markdown.openPreFormatted(false);
                markdown.append(node.getChars()).blankLine();
                markdown.closePreFormatted();
            }
        }
    }

    @Nullable
    @Override
    public Set<NodeFormattingHandler<?>> getNodeFormattingHandlers() {
        return new HashSet<>(Collections.singletonList(
                new NodeFormattingHandler<>(JekyllFrontMatterBlock.class, JekyllFrontMatterNodeFormatter.this::render)
        ));
    }

    private void render(JekyllFrontMatterBlock node, NodeFormatterContext context, MarkdownWriter markdown) {
    }

    public static class Factory implements NodeFormatterFactory {
        @NotNull
        @Override
        public NodeFormatter create(@NotNull DataHolder options) {
            return new JekyllFrontMatterNodeFormatter(options);
        }
    }
}
