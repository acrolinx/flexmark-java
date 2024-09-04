package com.vladsch.flexmark.formatter;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.format.CharWidthProvider;
import com.vladsch.flexmark.util.format.options.BlockQuoteMarker;
import com.vladsch.flexmark.util.format.options.CodeFenceMarker;
import com.vladsch.flexmark.util.format.options.DiscretionaryText;
import com.vladsch.flexmark.util.format.options.ElementAlignment;
import com.vladsch.flexmark.util.format.options.ElementPlacement;
import com.vladsch.flexmark.util.format.options.ElementPlacementSort;
import com.vladsch.flexmark.util.format.options.EqualizeTrailingMarker;
import com.vladsch.flexmark.util.format.options.HeadingStyle;
import com.vladsch.flexmark.util.format.options.ListBulletMarker;
import com.vladsch.flexmark.util.format.options.ListNumberedMarker;
import com.vladsch.flexmark.util.format.options.ListSpacing;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public class FormatterOptions {
  public final boolean itemContentIndent;

  public final ParserEmulationProfile emulationProfile;
  public final boolean setextHeadingEqualizeMarker;
  public final int formatFlags;
  public final int maxBlankLines;
  public final int maxTrailingBlankLines;
  public final int rightMargin;
  public final int minSetextMarkerLength;
  public final DiscretionaryText spaceAfterAtxMarker;
  public final EqualizeTrailingMarker atxHeadingTrailingMarker;
  public final HeadingStyle headingStyle;
  public final boolean blockQuoteBlankLines;
  public final BlockQuoteMarker blockQuoteMarkers;
  public final String thematicBreak;
  public final String translationIdFormat;
  public final String translationHtmlBlockPrefix;
  public final String translationHtmlInlinePrefix;
  public final String translationExcludePattern;
  public final String translationHtmlBlockTagPattern;
  public final String translationHtmlInlineTagPattern;
  public final String translationAutolinkPrefix;
  public final boolean indentedCodeMinimizeIndent;
  public final boolean fencedCodeMinimizeIndent;
  public final boolean fencedCodeMatchClosingMarker;
  public final boolean fencedCodeSpaceBeforeInfo;
  public final int fencedCodeMarkerLength;
  public final CodeFenceMarker fencedCodeMarkerType;
  public final boolean listAddBlankLineBefore;
  public final boolean listRenumberItems;
  public final boolean listRemoveEmptyItems;
  public final boolean listsItemContentAfterSuffix;
  public final ListBulletMarker listBulletMarker;
  public final ListNumberedMarker listNumberedMarker;
  public final ListSpacing listSpacing;
  public final ElementPlacement referencePlacement;
  public final ElementPlacementSort referenceSort;
  public final boolean keepImageLinksAtStart;
  public final boolean keepExplicitLinksAtStart;
  public final boolean keepHardLineBreaks;
  public final boolean keepSoftLineBreaks;
  public final boolean appendTransferredReferences;
  public final boolean optimizedInlineRendering;
  public final boolean applySpecialLeadInHandlers;
  public final boolean escapeSpecialCharsOnWrap;
  public final boolean escapeNumberedLeadInOnWrap;
  public final boolean unescapeSpecialCharsOnWrap;
  public final CharWidthProvider charWidthProvider;
  public final ElementAlignment listAlignNumeric;
  public final boolean listResetFirstItemNumber;
  public final String formatterOnTag;
  public final String formatterOffTag;
  public final boolean formatterTagsEnabled;
  public final boolean formatterTagsAcceptRegexp;
  public final boolean blankLinesInAst;
  public final @Nullable Pattern linkMarkerCommentPattern;

  public FormatterOptions(DataHolder options) {
    emulationProfile = Formatter.FORMATTER_EMULATION_PROFILE.get(options);
    itemContentIndent = emulationProfile.family != ParserEmulationProfile.FIXED_INDENT;

    setextHeadingEqualizeMarker = Formatter.SETEXT_HEADING_EQUALIZE_MARKER.get(options);
    formatFlags = Formatter.FORMAT_FLAGS.get(options);
    maxBlankLines = Formatter.MAX_BLANK_LINES.get(options);
    maxTrailingBlankLines = Formatter.MAX_TRAILING_BLANK_LINES.get(options);
    rightMargin = Formatter.RIGHT_MARGIN.get(options);
    minSetextMarkerLength = Parser.HEADING_SETEXT_MARKER_LENGTH.get(options);
    spaceAfterAtxMarker = Formatter.SPACE_AFTER_ATX_MARKER.get(options);
    atxHeadingTrailingMarker = Formatter.ATX_HEADING_TRAILING_MARKER.get(options);
    headingStyle = Formatter.HEADING_STYLE.get(options);
    thematicBreak = Formatter.THEMATIC_BREAK.get(options);
    translationIdFormat = Formatter.TRANSLATION_ID_FORMAT.get(options);
    translationHtmlBlockPrefix = Formatter.TRANSLATION_HTML_BLOCK_PREFIX.get(options);
    translationHtmlInlinePrefix = Formatter.TRANSLATION_HTML_INLINE_PREFIX.get(options);
    translationAutolinkPrefix = Formatter.TRANSLATION_AUTOLINK_PREFIX.get(options);
    translationExcludePattern = Formatter.TRANSLATION_EXCLUDE_PATTERN.get(options);
    translationHtmlBlockTagPattern = Formatter.TRANSLATION_HTML_BLOCK_TAG_PATTERN.get(options);
    translationHtmlInlineTagPattern = Formatter.TRANSLATION_HTML_INLINE_TAG_PATTERN.get(options);
    blockQuoteBlankLines = Formatter.BLOCK_QUOTE_BLANK_LINES.get(options);
    blockQuoteMarkers = Formatter.BLOCK_QUOTE_MARKERS.get(options);
    indentedCodeMinimizeIndent = Formatter.INDENTED_CODE_MINIMIZE_INDENT.get(options);
    fencedCodeMinimizeIndent = Formatter.FENCED_CODE_MINIMIZE_INDENT.get(options);
    fencedCodeMatchClosingMarker = Formatter.FENCED_CODE_MATCH_CLOSING_MARKER.get(options);
    fencedCodeSpaceBeforeInfo = Formatter.FENCED_CODE_SPACE_BEFORE_INFO.get(options);
    fencedCodeMarkerLength = Formatter.FENCED_CODE_MARKER_LENGTH.get(options);
    fencedCodeMarkerType = Formatter.FENCED_CODE_MARKER_TYPE.get(options);
    listAddBlankLineBefore = Formatter.LIST_ADD_BLANK_LINE_BEFORE.get(options);
    listAlignNumeric = Formatter.LIST_ALIGN_NUMERIC.get(options);
    listResetFirstItemNumber = Formatter.LIST_RESET_FIRST_ITEM_NUMBER.get(options);
    listRenumberItems = Formatter.LIST_RENUMBER_ITEMS.get(options);
    listRemoveEmptyItems = Formatter.LIST_REMOVE_EMPTY_ITEMS.get(options);
    listBulletMarker = Formatter.LIST_BULLET_MARKER.get(options);
    listNumberedMarker = Formatter.LIST_NUMBERED_MARKER.get(options);
    listSpacing = Formatter.LIST_SPACING.get(options);
    listsItemContentAfterSuffix = Formatter.LISTS_ITEM_CONTENT_AFTER_SUFFIX.get(options);
    referencePlacement = Formatter.REFERENCE_PLACEMENT.get(options);
    referenceSort = Formatter.REFERENCE_SORT.get(options);
    keepImageLinksAtStart = Formatter.KEEP_IMAGE_LINKS_AT_START.get(options);
    keepExplicitLinksAtStart = Formatter.KEEP_EXPLICIT_LINKS_AT_START.get(options);
    charWidthProvider = Formatter.FORMAT_CHAR_WIDTH_PROVIDER.get(options);
    keepHardLineBreaks = Formatter.KEEP_HARD_LINE_BREAKS.get(options);
    keepSoftLineBreaks = Formatter.KEEP_SOFT_LINE_BREAKS.get(options);
    formatterOnTag = Formatter.FORMATTER_ON_TAG.get(options);
    formatterOffTag = Formatter.FORMATTER_OFF_TAG.get(options);
    formatterTagsEnabled = Formatter.FORMATTER_TAGS_ENABLED.get(options);
    formatterTagsAcceptRegexp = Formatter.FORMATTER_TAGS_ACCEPT_REGEXP.get(options);
    linkMarkerCommentPattern = Formatter.LINK_MARKER_COMMENT_PATTERN.get(options);
    appendTransferredReferences = Formatter.APPEND_TRANSFERRED_REFERENCES.get(options);
    optimizedInlineRendering = Formatter.OPTIMIZED_INLINE_RENDERING.get(options);
    applySpecialLeadInHandlers = Formatter.APPLY_SPECIAL_LEAD_IN_HANDLERS.get(options);
    escapeSpecialCharsOnWrap = Formatter.ESCAPE_SPECIAL_CHARS.get(options);
    escapeNumberedLeadInOnWrap = Formatter.ESCAPE_NUMBERED_LEAD_IN.get(options);
    unescapeSpecialCharsOnWrap = Formatter.UNESCAPE_SPECIAL_CHARS.get(options);
    blankLinesInAst = Parser.BLANK_LINES_IN_AST.get(options);
  }
}
