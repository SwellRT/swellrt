package org.waveprotocol.wave.client.editor.playback;

import org.waveprotocol.wave.client.editor.playback.DocHistory.DocOpResult;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.version.HashedVersion;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
public class DocRevision {

  final DocHistory history;
  final int revisionIndex;
  final HashedVersion appliedAtVersion;
  final HashedVersion resultingVersion;
  final long resultingTime;
  final String participant;
  DocOp[] ops;

  DocRevision(DocHistory history, HashedVersion appliedAtVersion, HashedVersion resultingVersion,
      long resultingTime, String participant, int nextRevisionIndex) {
    super();
    this.history = history;
    this.appliedAtVersion = appliedAtVersion;
    this.resultingVersion = resultingVersion;
    this.resultingTime = resultingTime;
    this.participant = participant;
    this.revisionIndex = nextRevisionIndex;
    this.ops = null;
  }

  @JsProperty
  public double getAppliedAtVersion() {
    return appliedAtVersion.getVersion();
  }

  @JsProperty
  public double getResultingVersion() {
    return resultingVersion.getVersion();
  }

  @JsProperty
  public double getTime() {
    return resultingTime;
  }

  @JsProperty
  public String getAuthor() {
    return participant;
  }

  @JsIgnore
  public void getDocOps(DocOpResult callback) {
    if (ops == null) {
      history.fetchOps(this, fetchedOps -> {
        ops = fetchedOps;
        callback.result(ops);
      });
    }

    callback.result(ops);
  }

  @JsProperty
  public int getRevisionIndex() {
    return revisionIndex;
  }

  @JsIgnore
  @Override
  public String toString() {
    return "[DocRevision#" + revisionIndex + "] version ( " + appliedAtVersion.getVersion() + " -> "
        + resultingVersion.getVersion() + ") by " + participant;
  }
}
