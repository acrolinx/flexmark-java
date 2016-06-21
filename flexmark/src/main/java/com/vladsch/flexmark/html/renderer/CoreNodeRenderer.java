package com.vladsch.flexmark.html.renderer;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.internal.ListOptions;
import com.vladsch.flexmark.internal.util.BasedSequence;
import com.vladsch.flexmark.internal.util.DataHolder;
import com.vladsch.flexmark.internal.util.Escaping;
import com.vladsch.flexmark.internal.util.ReferenceRepository;
import com.vladsch.flexmark.node.*;
import com.vladsch.flexmark.parser.Parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The node renderer that renders all the core nodes (comes last in the order of node renderers).
 */
public class CoreNodeRenderer extends AbstractVisitor implements NodeRenderer {

    protected final NodeRendererContext context;
    private final HtmlWriter html;
    private final boolean suppressHtmlBlocks;
    private final boolean suppressInlineHtml;
    private final ReferenceRepository referenceRepository;
    private final ListOptions listOptions;

    public CoreNodeRenderer(NodeRendererContext context) {
        this.context = context;
        this.html = context.getHtmlWriter();
        DataHolder options = context.getOptions();
        this.suppressHtmlBlocks = options.get(HtmlRenderer.SUPPRESS_HTML_BLOCKS);
        this.suppressInlineHtml = options.get(HtmlRenderer.SUPPRESS_INLINE_HTML);
        this.referenceRepository = options.get(Parser.REFERENCES);
        this.listOptions = new ListOptions(options);
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return new HashSet<Class<? extends Node>>(Arrays.asList(
                AutoLink.class,
                BlockQuote.class,
                BulletList.class,
                Code.class,
                CustomBlock.class,
                //CustomNode.class,
                Document.class,
                Emphasis.class,
                FencedCodeBlock.class,
                HardLineBreak.class,
                Heading.class,
                HtmlBlock.class,
                HtmlEntity.class,
                HtmlInline.class,
                Image.class,
                ImageRef.class,
                IndentedCodeBlock.class,
                Link.class,
                LinkRef.class,
                BulletListItem.class,
                OrderedListItem.class,
                MailLink.class,
                OrderedList.class,
                Paragraph.class,
                Reference.class,
                SoftLineBreak.class,
                StrongEmphasis.class,
                Text.class,
                ThematicBreak.class
        ));
    }

    @Override
    public void render(Node node) {
        node.accept(this);
    }

    @Override
    public void visit(Document node) {
        // No rendering itself
        visitChildren(node);
    }

    @Override
    public void visit(Heading node) {
        html.withAttr().tagLine("h" + node.getLevel(), () -> {
            visitChildren(node);
        });
    }

    @Override
    public void visit(BlockQuote node) {
        html.withAttr().tagIndent("blockquote", () -> {
            visitChildren(node);
        });
    }

    @Override
    public void visit(FencedCodeBlock node) {
        BasedSequence info = node.getInfo();
        if (info.isNotNull() && !info.isBlank()) {
            int space = info.indexOf(' ');
            BasedSequence language;
            if (space == -1) {
                language = info;
            } else {
                language = info.subSequence(0, space);
            }
            html.attr("class", "language-" + language.unescape());
        }

        html.line();
        html.tag("pre");
        html.withAttr().tag("code");
        html.text(node.getContentChars().normalizeEOL());
        html.tag("/code");
        html.tag("/pre");
        html.line();
    }

    @Override
    public void visit(HtmlBlock node) {
        if (suppressHtmlBlocks) return;

        html.line();
        if (context.shouldEscapeHtml()) {
            html.text(node.getContentChars().normalizeEOL());
        } else {
            html.raw(node.getContentChars().normalizeEOL());
        }
        html.line();
    }

    @Override
    public void visit(ThematicBreak node) {
        html.withAttr().tagVoidLine("hr");
    }

    @Override
    public void visit(IndentedCodeBlock node) {
        html.line();
        html.tag("pre");
        html.tag("code");
        html.text(node.getContentChars().trimTailBlankLines().normalizeEndWithEOL());
        html.tag("/code");
        html.tag("/pre");
        html.line();
    }

    @Override
    public void visit(BulletList node) {
        html.withAttr().tagIndent("ul", () -> {
            visitChildren(node);
        });
    }

    @Override
    public void visit(OrderedList node) {
        int start = node.getStartNumber();
        if (listOptions.orderedStart && start != 1) html.attr("start", String.valueOf(start));
        html.withAttr().tagIndent("ol", () -> {
            visitChildren(node);
        });
    }

    @Override
    public void visit(BulletListItem node) {
        visit((ListItem) node);
    }

    @Override
    public void visit(OrderedListItem node) {
        visit((ListItem) node);
    }

    protected void visit(ListItem node) {
        if (node.getFirstChild() == null || listOptions.isTightListItem(node)) {
            html.withAttr().withCondIndent().tagLine("li", () -> {
                visitChildren(node);
            });
        } else {
            html.withAttr().tagIndent("li", () -> {
                visitChildren(node);
            });
        }
    }

    @Override
    public void visit(Paragraph node) {
        boolean inTightList = listOptions.isInTightListItem(node);
        if (!inTightList) {
            html.tagLine("p", () -> {
                visitChildren(node);
            });
        } else {
            visitChildren(node);
        }
    }

    @Override
    public void visit(Emphasis node) {
        html.tag("em");
        visitChildren(node);
        html.tag("/em");
    }

    @Override
    public void visit(StrongEmphasis node) {
        html.tag("strong");
        visitChildren(node);
        html.tag("/strong");
    }

    @Override
    public void visit(Text node) {
        html.text(Escaping.normalizeEOL(node.getChars().unescape()));
    }

    @Override
    public void visit(Code node) {
        html.withAttr().tag("code");
        html.text(Escaping.collapseWhitespace(node.getText(), true));
        html.tag("/code");
    }

    @Override
    public void visit(HtmlInline node) {
        if (suppressInlineHtml) return;

        if (context.shouldEscapeHtml()) {
            html.text(node.getChars().normalizeEOL());
        } else {
            html.raw(node.getChars().normalizeEOL());
        }
    }

    @Override
    public void visit(SoftLineBreak node) {
        html.raw(context.getSoftBreak());
    }

    @Override
    public void visit(HardLineBreak node) {
        html.tagVoid("br").line();
    }

    @Override
    public void visit(Reference node) {

    }

    @Override
    public void visit(HtmlEntity node) {
        html.text(node.getChars().unescape());
    }

    @Override
    public void visit(AutoLink node) {
        String text = node.getText().toString();
        html.attr("href", context.encodeUrl(text))
                .withAttr()
                .tag("a", () -> html.text(text));
    }

    @Override
    public void visit(MailLink node) {
        String url = context.encodeUrl(node.getText().unescape());
        html.attr("href", "mailto:" + url)
                .withAttr()
                .tag("a")
                .text(url)
                .tag("/a");
    }

    @Override
    public void visit(Image node) {
        String url = context.encodeUrl(node.getUrl().unescape());

        AltTextVisitor altTextVisitor = new AltTextVisitor();
        node.accept(altTextVisitor);
        String altText = altTextVisitor.getAltText();

        html.attr("src", url);
        html.attr("alt", altText);
        if (node.getTitle().isNotNull()) {
            html.attr("title", node.getTitle().unescape());
        }
        html.withAttr().tagVoid("img");
    }

    @Override
    public void visit(Link node) {
        html.attr("href", context.encodeUrl(node.getUrl().unescape()));
        if (node.getTitle().isNotNull()) {
            html.attr("title", node.getTitle().unescape());
        }
        html.withAttr().tag("a");
        visitChildren(node);
        html.tag("/a");
    }

    @Override
    public void visit(ImageRef node) {
        if (!node.isDefined()) {
            // empty ref, we treat it as text
            assert !node.isDefined();
            html.text(node.getChars().unescape());
        } else {
            Reference reference = node.getReferenceNode(referenceRepository);
            assert reference != null;
            AltTextVisitor altTextVisitor = new AltTextVisitor();
            node.accept(altTextVisitor);
            String altText = altTextVisitor.getAltText();

            html.attr("src", context.encodeUrl(reference.getUrl().unescape()));
            html.attr("alt", altText);

            BasedSequence title = reference.getTitle();
            if (title.isNotNull()) {
                html.attr("title", title.unescape());
            }

            html.withAttr().tagVoid("img");
        }
    }

    @Override
    public void visit(LinkRef node) {
        if (!node.isDefined()) {
            // empty ref, we treat it as text
            assert !node.isDefined();
            html.raw("[");
            visitChildren(node);
            html.raw("]");

            if (!node.isReferenceTextCombined()) {
                html.raw("[");
                html.raw(node.getReference().unescape());
                html.raw("]");
            }
        } else {
            Reference reference = node.getReferenceNode(referenceRepository);
            assert reference != null;
            String url = context.encodeUrl(reference.getUrl().unescape());
            html.attr("href", url);
            BasedSequence title = reference.getTitle();
            if (title.isNotNull()) {
                html.attr("title", title.unescape());
            }
            html.withAttr().tag("a");
            visitChildren(node);
            html.tag("/a");
        }
    }

    @Override
    public void visit(CustomBlock node) {

    }

    @Override
    public void visit(CustomNode node) {

    }

    @Override
    protected void visitChildren(Node parent) {
        context.renderChildren(parent);
    }

    private static class AltTextVisitor extends AbstractVisitor {
        private final StringBuilder sb = new StringBuilder();

        String getAltText() {
            return sb.toString();
        }

        @Override
        public void visit(Text node) {
            sb.append(node.getChars());
        }

        @Override
        public void visit(HtmlEntity node) {
            sb.append(node.getChars().unescape());
        }

        @Override
        public void visit(SoftLineBreak node) {
            sb.append('\n');
        }

        @Override
        public void visit(HardLineBreak node) {
            sb.append('\n');
        }
    }
}
