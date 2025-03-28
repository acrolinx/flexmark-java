package com.vladsch.flexmark.ext.definition.internal;

import com.vladsch.flexmark.ext.definition.DefinitionItem;
import com.vladsch.flexmark.ext.definition.DefinitionList;
import com.vladsch.flexmark.ext.definition.DefinitionTerm;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.CoreNodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.ListOptions;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import java.util.HashSet;
import java.util.Set;

public class DefinitionNodeRenderer implements NodeRenderer {
  private final ListOptions listOptions;

  private DefinitionNodeRenderer(DataHolder options) {
    this.listOptions = ListOptions.get(options);
  }

  @Override
  public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
    Set<NodeRenderingHandler<?>> set = new HashSet<>();
    set.add(new NodeRenderingHandler<>(DefinitionList.class, this::render));
    set.add(new NodeRenderingHandler<>(DefinitionTerm.class, this::render));
    set.add(new NodeRenderingHandler<>(DefinitionItem.class, this::render));

    return set;
  }

  private void render(DefinitionList node, NodeRendererContext context, HtmlWriter html) {
    html.withAttr().tag("dl").indent();
    context.renderChildren(node);
    html.unIndent().tag("/dl");
  }

  private void render(DefinitionTerm node, NodeRendererContext context, HtmlWriter html) {
    Node childText = node.getFirstChild();
    if (childText != null) {
      html.srcPosWithEOL(node.getChars())
          .withAttr(CoreNodeRenderer.TIGHT_LIST_ITEM)
          .withCondIndent()
          .tagLine(
              "dt",
              () -> {
                html.text(node.getMarkerSuffix().unescape());
                context.renderChildren(node);
              });
    }
  }

  private void render(DefinitionItem node, NodeRendererContext context, HtmlWriter html) {
    if (listOptions.isTightListItem(node)) {
      html.srcPosWithEOL(node.getChars())
          .withAttr(CoreNodeRenderer.TIGHT_LIST_ITEM)
          .withCondIndent()
          .tagLine(
              "dd",
              () -> {
                html.text(node.getMarkerSuffix().unescape());
                context.renderChildren(node);
              });
    } else {
      html.srcPosWithEOL(node.getChars())
          .withAttr(CoreNodeRenderer.LOOSE_LIST_ITEM)
          .tagIndent(
              "dd",
              () -> {
                html.text(node.getMarkerSuffix().unescape());
                context.renderChildren(node);
              });
    }
  }

  public static class Factory implements NodeRendererFactory {

    @Override
    public NodeRenderer apply(DataHolder options) {
      return new DefinitionNodeRenderer(options);
    }
  }
}
