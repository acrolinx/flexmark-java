package com.vladsch.flexmark.formatter;

import com.vladsch.flexmark.util.ast.Document;
import java.util.Set;

/** A renderer for a document node for a specific rendering phase */
public interface PhasedNodeFormatter extends NodeFormatter {

  Set<FormattingPhase> getFormattingPhases();

  /**
   * Render the specified node.
   *
   * @param context node renderer context instance
   * @param markdown markdown writer instance
   * @param document the document node to render
   * @param phase rendering phase for which to generate the output. Will be any of {@link
   *     FormattingPhase} except {@link FormattingPhase#DOCUMENT} because this phase is used for the
   *     non-phased node rendering
   */
  void renderDocument(
      NodeFormatterContext context,
      MarkdownWriter markdown,
      Document document,
      FormattingPhase phase);
}
