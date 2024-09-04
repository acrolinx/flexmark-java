package com.vladsch.flexmark.ast.util;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.Reference;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.VisitHandler;

public class BlockVisitorExt {
  public static <V extends BlockVisitor> VisitHandler<?>[] VISIT_HANDLERS(V visitor) {
    return new VisitHandler<?>[] {
      new VisitHandler<>(BlockQuote.class, visitor::visit),
      new VisitHandler<>(BulletList.class, visitor::visit),
      new VisitHandler<>(Document.class, visitor::visit),
      new VisitHandler<>(FencedCodeBlock.class, visitor::visit),
      new VisitHandler<>(Heading.class, visitor::visit),
      new VisitHandler<>(HtmlBlock.class, visitor::visit),
      new VisitHandler<>(HtmlCommentBlock.class, visitor::visit),
      new VisitHandler<>(IndentedCodeBlock.class, visitor::visit),
      new VisitHandler<>(BulletListItem.class, visitor::visit),
      new VisitHandler<>(OrderedListItem.class, visitor::visit),
      new VisitHandler<>(OrderedList.class, visitor::visit),
      new VisitHandler<>(Paragraph.class, visitor::visit),
      new VisitHandler<>(Reference.class, visitor::visit),
      new VisitHandler<>(ThematicBreak.class, visitor::visit)
    };
  }
}
