package com.vladsch.flexmark.html;

import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.util.ast.Node;

abstract class NodeRendererSubContext implements NodeRendererContext {
  final HtmlWriter htmlWriter;
  Node renderingNode;
  NodeRenderingHandlerWrapper renderingHandlerWrapper;
  int doNotRenderLinksNesting;

  NodeRendererSubContext(HtmlWriter htmlWriter) {
    this.htmlWriter = htmlWriter;
    this.renderingNode = null;
    this.doNotRenderLinksNesting = 0;
  }

  @Override
  public HtmlWriter getHtmlWriter() {
    return htmlWriter;
  }

  int getDoNotRenderLinksNesting() {
    return doNotRenderLinksNesting;
  }

  @Override
  public boolean isDoNotRenderLinks() {
    return doNotRenderLinksNesting != 0;
  }

  @Override
  public void doNotRenderLinks(boolean doNotRenderLinks) {
    if (doNotRenderLinks) {
      doNotRenderLinks();
    } else {
      doRenderLinks();
    }
  }

  @Override
  public void doNotRenderLinks() {
    this.doNotRenderLinksNesting++;
  }

  @Override
  public void doRenderLinks() {
    if (this.doNotRenderLinksNesting == 0)
      throw new IllegalStateException("Not in do not render links context");
    this.doNotRenderLinksNesting--;
  }
}
