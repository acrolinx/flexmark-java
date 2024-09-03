package com.vladsch.flexmark.ext.definition.internal;

import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.format.options.DefinitionMarker;

public class DefinitionFormatOptions {
  public final int markerSpaces;
  public final DefinitionMarker markerType;

  public DefinitionFormatOptions(DataHolder options) {
    markerSpaces = DefinitionExtension.FORMAT_MARKER_SPACES.get(options);
    markerType = DefinitionExtension.FORMAT_MARKER_TYPE.get(options);
  }
}
