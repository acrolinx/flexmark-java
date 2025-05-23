package com.vladsch.flexmark.ast.util;

import com.vladsch.flexmark.util.ast.Node;

public interface AnchorRefTargetBlockPreVisitor {
  /**
   * Test if node needs to have its children visited
   *
   * @param node node
   * @param anchorRefTargetBlockVisitor anchor ref target visitor, can be used to visit anchor ref
   *     targets
   * @return true, if children of block node need to be visited
   */
  boolean preVisit(Node node, AnchorRefTargetBlockVisitor anchorRefTargetBlockVisitor);
}
