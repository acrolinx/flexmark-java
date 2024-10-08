package com.vladsch.flexmark.ext.tables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.vladsch.flexmark.formatter.MarkdownWriter;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.util.format.MarkdownTable;
import com.vladsch.flexmark.util.format.TrackedOffset;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.LineAppendable;
import org.junit.Test;

public class MarkdownTransposeTableTest extends MarkdownTableTestBase {
  @Test
  public void test_basic1() {
    MarkdownTable[] tables =
        getTables(
            "" + "| First Header  |\n" + "| ------------- |\n" + "| Content Cell  |\n" + "\n" + "");

    MarkdownWriter out = new MarkdownWriter(LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = tables[0].transposed(1);
    transposed.appendTable(out);

    assertEquals(
        "",
        "" + "| First Header | Content Cell |\n" + "|--------------|--------------|\n" + "",
        out.toString());
  }

  @Test
  public void test_basic2() {
    MarkdownTable[] tables =
        getTables(
            ""
                + "| Left-aligned1 | Right-aligned1 |\n"
                + "| Left-aligned2 | Right-aligned2 |\n"
                + "| :---         |          ---: |\n"
                + "| git status   | git status1    |\n"
                + "| git diff     | git diff2      |\n"
                + "[  ]\n"
                + "");

    MarkdownWriter out = new MarkdownWriter(LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = tables[0].transposed(1);
    transposed.appendTable(out);

    assertEquals(
        "",
        ""
            + "| Left-aligned1  | Left-aligned2  | git status  | git diff  |\n"
            + "|----------------|----------------|-------------|-----------|\n"
            + "| Right-aligned1 | Right-aligned2 | git status1 | git diff2 |\n"
            + "[ ]\n"
            + "",
        out.toString());
  }

  @Test
  public void test_basic3() {
    MarkdownTable[] tables =
        getTables(
            "| Left-aligned | Center-aligned | Right-aligned1 |\n"
                + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
                + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
                + "| :---         |     :---:      |          ---: |\n"
                + "| git status11   | git status21     | git status31    |\n"
                + "| git diff12     | git diff22       | git diff32      |\n"
                + "| git diff13     | git diff23       | git diff33      |\n"
                + "[Table Caption]\n"
                + "");

    MarkdownWriter out = new MarkdownWriter(LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = tables[0].transposed(0);
    transposed.appendTable(out);

    assertEquals(
        "",
        ""
            + "|----------------|-----------------|-----------------|--------------|------------|------------|\n"
            + "| Left-aligned   | Left-aligned1   | Left-aligned2   | git status11 | git diff12 | git diff13 |\n"
            + "| Center-aligned | Center-aligned1 | Center-aligned2 | git status21 | git diff22 | git diff23 |\n"
            + "| Right-aligned1 | Right-aligned2  | Right-aligned3  | git status31 | git diff32 | git diff33 |\n"
            + "[Table Caption]\n"
            + "",
        out.toString());
  }

  @Test
  public void test_basic4() {
    MarkdownTable[] tables =
        getTables(
            ""
                + "| Left-aligned | Center-aligned | Right-aligned1 |\n"
                + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
                + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
                + "| :---         |     :---:      |          ---: |\n"
                + "| git status11   | git status21     | git status31    |\n"
                + "| git diff12     | git diff22       | git diff32      |\n"
                + "| git diff13     | git diff23       | git diff33      |\n"
                + "[Table Caption]\n"
                + "");

    MarkdownWriter out = new MarkdownWriter(LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = tables[0].transposed(1);
    transposed.appendTable(out);

    assertEquals(
        "",
        ""
            + "|  Left-aligned  |  Left-aligned1  |  Left-aligned2  | git status11 | git diff12 | git diff13 |\n"
            + "|----------------|-----------------|-----------------|--------------|------------|------------|\n"
            + "| Center-aligned | Center-aligned1 | Center-aligned2 | git status21 | git diff22 | git diff23 |\n"
            + "| Right-aligned1 | Right-aligned2  | Right-aligned3  | git status31 | git diff32 | git diff33 |\n"
            + "[Table Caption]\n"
            + "",
        out.toString());
  }

  @Test
  public void test_basic5() {
    MarkdownTable[] tables =
        getTables(
            "| Left-aligned | Center-aligned | Right-aligned1 |\n"
                + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
                + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
                + "| :---         |     :---:      |          ---: |\n"
                + "| git status11   | git status21     | git status31    |\n"
                + "| git diff12     | git diff22       | git diff32      |\n"
                + "| git diff13     | git diff23       | git diff33      |\n"
                + "[Table Caption]\n"
                + "");

    MarkdownWriter out = new MarkdownWriter(LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = tables[0].transposed(2);
    transposed.appendTable(out);

    assertEquals(
        "",
        ""
            + "|  Left-aligned  |  Left-aligned1  |  Left-aligned2  | git status11 | git diff12 | git diff13 |\n"
            + "| Center-aligned | Center-aligned1 | Center-aligned2 | git status21 | git diff22 | git diff23 |\n"
            + "|----------------|-----------------|-----------------|--------------|------------|------------|\n"
            + "| Right-aligned1 | Right-aligned2  | Right-aligned3  | git status31 | git diff32 | git diff33 |\n"
            + "[Table Caption]\n"
            + "",
        out.toString());
  }

  @Test
  public void test_basic6() {
    MarkdownTable[] tables =
        getTables(
            "| Left-aligned | Center-aligned | Right-aligned1 |\n"
                + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
                + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
                + "| :---         |     :---:      |          ---: |\n"
                + "| git status11   | git status21     | git status31    |\n"
                + "| git diff12     | git diff22       | git diff32      |\n"
                + "| git diff13     | git diff23       | git diff33      |\n"
                + "[Table Caption]\n"
                + "");

    MarkdownWriter out = new MarkdownWriter(LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = tables[0].transposed(3);
    transposed.appendTable(out);

    assertEquals(
        "",
        ""
            + "|  Left-aligned  |  Left-aligned1  |  Left-aligned2  | git status11 | git diff12 | git diff13 |\n"
            + "| Center-aligned | Center-aligned1 | Center-aligned2 | git status21 | git diff22 | git diff23 |\n"
            + "| Right-aligned1 | Right-aligned2  | Right-aligned3  | git status31 | git diff32 | git diff33 |\n"
            + "|----------------|-----------------|-----------------|--------------|------------|------------|\n"
            + "[Table Caption]\n"
            + "",
        out.toString());
  }

  @Test
  public void test_basic7() {
    MarkdownTable[] tables =
        getTables(
            "| Left-aligned | Center-aligned | Right-aligned1 |\n"
                + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
                + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
                + "| :---         |     :---:      |          ---: |\n"
                + "| git status11   | git status21     | git status31    |\n"
                + "| git diff12     | git diff22       | git diff32      |\n"
                + "| git diff13     | git diff23       | git diff33      |\n"
                + "[Table Caption]\n"
                + "");

    MarkdownWriter out = new MarkdownWriter(LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = tables[0].transposed(4);
    transposed.appendTable(out);

    assertEquals(
        "",
        ""
            + "|  Left-aligned  |  Left-aligned1  |  Left-aligned2  | git status11 | git diff12 | git diff13 |\n"
            + "| Center-aligned | Center-aligned1 | Center-aligned2 | git status21 | git diff22 | git diff23 |\n"
            + "| Right-aligned1 | Right-aligned2  | Right-aligned3  | git status31 | git diff32 | git diff33 |\n"
            + "|----------------|-----------------|-----------------|--------------|------------|------------|\n"
            + "[Table Caption]\n"
            + "",
        out.toString());
  }

  @Test
  public void test_trackingOffset1() {
    String markdown =
        ""
            + "| Left-aligned   | Left-^aligned1   | Left-aligned2   | git status11 | git diff12 | git diff13 |\n"
            + "|----------------|-----------------|-----------------|--------------|------------|------------|\n"
            + "| Center-aligned | Center-aligned1 | Center-aligned2 | git status21 | git diff22 | git diff23 |\n"
            + "| Right-aligned1 | Right-aligned2  | Right-aligned3  | git status31 | git diff32 | git diff33 |\n"
            + "[Table Caption]\n"
            + "";

    int pos = markdown.indexOf("^");
    CharSequence charSequence = markdown.substring(0, pos) + markdown.substring(pos + 1);
    BasedSequence source = BasedSequence.of(charSequence);
    MarkdownTable table = getTable(source, formatOptions(null));
    assertTrue(table.addTrackedOffset(TrackedOffset.track(pos, null, false)));
    HtmlWriter out = new HtmlWriter(0, LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = table.transposed(1);
    transposed.appendTable(out);
    String transposedTable = out.toString(0, 0);
    int offset = transposed.getTrackedOffsetIndex(pos);

    assertEquals(
        ""
            + "| Left-aligned  | Center-aligned  | Right-aligned1 |\n"
            + "|:--------------|:----------------|:---------------|\n"
            + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
            + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
            + "| git status11  | git status21    | git status31   |\n"
            + "| git diff12    | git diff22      | git diff32     |\n"
            + "| git diff13    | git diff23      | git diff33     |\n"
            + "[ Table Caption ]\n"
            + "",
        transposedTable);
    assertEquals(
        ""
            + "| Left-aligned  | Center-aligned  | Right-aligned1 |\n"
            + "|:--------------|:----------------|:---------------|\n"
            + "| Left-^aligned1 | Center-aligned1 | Right-aligned2 |\n"
            + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
            + "| git status11  | git status21    | git status31   |\n"
            + "| git diff12    | git diff22      | git diff32     |\n"
            + "| git diff13    | git diff23      | git diff33     |\n"
            + "[ Table Caption ]\n"
            + "",
        transposedTable.substring(0, offset) + "^" + transposedTable.substring(offset));
  }

  @Test
  public void test_trackingOffset2() {
    String markdown =
        ""
            + "| Left-aligned   | Left-aligned1   | Left-aligned2   | git status11 | git diff12 | git diff13 |\n"
            + "|----------------|-----------------|---------^--------|--------------|------------|------------|\n"
            + "| Center-aligned | Center-aligned1 | Center-aligned2 | git status21 | git diff22 | git diff23 |\n"
            + "| Right-aligned1 | Right-aligned2  | Right-aligned3  | git status31 | git diff32 | git diff33 |\n"
            + "[Table Caption]\n"
            + "";

    int pos = markdown.indexOf("^");
    CharSequence charSequence = markdown.substring(0, pos) + markdown.substring(pos + 1);
    BasedSequence source = BasedSequence.of(charSequence);
    MarkdownTable table = getTable(source, formatOptions(null));
    assertTrue(table.addTrackedOffset(TrackedOffset.track(pos, null, false)));
    HtmlWriter out = new HtmlWriter(0, LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = table.transposed(1);
    transposed.appendTable(out);
    String transposedTable = out.toString(0, 0);
    int offset = transposed.getTrackedOffsetIndex(pos);

    assertEquals(
        ""
            + "| Left-aligned  | Center-aligned  | Right-aligned1 |\n"
            + "|:--------------|:----------------|:---------------|\n"
            + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
            + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
            + "| git status11  | git status21    | git status31   |\n"
            + "| git diff12    | git diff22      | git diff32     |\n"
            + "| git diff13    | git diff23      | git diff33     |\n"
            + "[ Table Caption ]\n"
            + "",
        transposedTable);
    assertEquals(
        ""
            + "| Left-aligned  | Center-aligned  | Right-aligned1 |\n"
            + "|:--------------|:----------------|:---------^------|\n"
            + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
            + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
            + "| git status11  | git status21    | git status31   |\n"
            + "| git diff12    | git diff22      | git diff32     |\n"
            + "| git diff13    | git diff23      | git diff33     |\n"
            + "[ Table Caption ]\n"
            + "",
        transposedTable.substring(0, offset) + "^" + transposedTable.substring(offset));
  }

  @Test
  public void test_trackingOffset3() {
    String markdown =
        ""
            + "| Left-aligned   | Left-aligned1   | Left-aligned2   | git status11 | git diff12 | git diff13 |\n"
            + "|----------------|-----------------|-----------------|--------------|------------|------------|\n"
            + "| Center-aligned | Center-aligned1 | Center-aligned2 | git status21 ^| git diff22 | git diff23 |\n"
            + "| Right-aligned1 | Right-aligned2  | Right-aligned3  | git status31 | git diff32 | git diff33 |\n"
            + "[Table Caption]\n"
            + "";

    int pos = markdown.indexOf("^");
    CharSequence charSequence = markdown.substring(0, pos) + markdown.substring(pos + 1);
    BasedSequence source = BasedSequence.of(charSequence);
    MarkdownTable table = getTable(source, formatOptions(null));
    assertTrue(table.addTrackedOffset(TrackedOffset.track(pos, null, false)));
    HtmlWriter out = new HtmlWriter(0, LineAppendable.F_FORMAT_ALL);
    MarkdownTable transposed = table.transposed(1);
    transposed.appendTable(out);
    String transposedTable = out.toString(0, 0);
    int offset = transposed.getTrackedOffsetIndex(pos);

    assertEquals(
        ""
            + "| Left-aligned  | Center-aligned  | Right-aligned1 |\n"
            + "|:--------------|:----------------|:---------------|\n"
            + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
            + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
            + "| git status11  | git status21    | git status31   |\n"
            + "| git diff12    | git diff22      | git diff32     |\n"
            + "| git diff13    | git diff23      | git diff33     |\n"
            + "[ Table Caption ]\n"
            + "",
        transposedTable);
    assertEquals(
        ""
            + "| Left-aligned  | Center-aligned  | Right-aligned1 |\n"
            + "|:--------------|:----------------|:---------------|\n"
            + "| Left-aligned1 | Center-aligned1 | Right-aligned2 |\n"
            + "| Left-aligned2 | Center-aligned2 | Right-aligned3 |\n"
            + "| git status11  | git status21 ^   | git status31   |\n"
            + "| git diff12    | git diff22      | git diff32     |\n"
            + "| git diff13    | git diff23      | git diff33     |\n"
            + "[ Table Caption ]\n"
            + "",
        transposedTable.substring(0, offset) + "^" + transposedTable.substring(offset));
  }
}
