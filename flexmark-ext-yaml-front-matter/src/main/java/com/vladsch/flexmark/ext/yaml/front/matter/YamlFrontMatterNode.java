package com.vladsch.flexmark.ext.yaml.front.matter;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import java.util.ArrayList;
import java.util.List;

public class YamlFrontMatterNode extends Node {
  private BasedSequence key;

  @Override
  public BasedSequence[] getSegments() {
    return new BasedSequence[] {key};
  }

  public YamlFrontMatterNode(BasedSequence key, List<BasedSequence> values) {
    this.key = key;
    for (BasedSequence value : values) {
      appendChild(new YamlFrontMatterValue(value));
    }
  }

  public String getKey() {
    return key.toString();
  }

  public BasedSequence getKeySequence() {
    return key;
  }

  public void setKey(BasedSequence key) {
    this.key = key;
  }

  public List<String> getValues() {
    List<String> list = new ArrayList<>();
    Node child = getFirstChild();
    while (child != null) {
      list.add(child.getChars().toString());
      child = child.getNext();
    }
    return list;
  }

  public List<BasedSequence> getValuesSequences() {
    List<BasedSequence> list = new ArrayList<>();
    Node child = getFirstChild();
    while (child != null) {
      list.add(child.getChars());
      child = child.getNext();
    }
    return list;
  }
}
