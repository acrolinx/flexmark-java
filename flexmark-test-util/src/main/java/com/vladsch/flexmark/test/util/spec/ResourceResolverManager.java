package com.vladsch.flexmark.test.util.spec;

import static com.vladsch.flexmark.test.util.spec.ResourceUrlResolver.hasProtocol;

import com.vladsch.flexmark.test.util.TestUtils;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ResourceResolverManager {
  /**
   * urlResolvers map test resource location url to source resource url to allow tests to output
   * file URLs which refer to source location, not copies in test location
   */
  private static final List<Function<String, String>> urlResolvers = new ArrayList<>();

  public static void registerUrlResolver(Function<String, String> resolver) {
    ResourceResolverManager.urlResolvers.add(resolver);
  }

  public static String adjustedFileUrl(URL url) {
    String externalForm = url.toExternalForm();
    String bestProtocolMatch = null;

    for (Function<String, String> resolver : urlResolvers) {
      String filePath = resolver.apply(externalForm);
      if (filePath == null) {
        continue;
      }

      if (hasProtocol(filePath) && bestProtocolMatch == null) {
        bestProtocolMatch = filePath;
      } else {
        File file = new File(filePath);
        if (file.exists()) {
          return TestUtils.FILE_PROTOCOL + filePath;
        }
      }
    }

    return bestProtocolMatch != null ? bestProtocolMatch : externalForm;
  }
}
