package org.waveprotocol.wave.client.editor.playback;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * A DocRevision represents a one or more contiguous wave operations performed
 * against a Wave's document for the same author.
 * <p>
 * Revisions can be generated grouping deltas so, they don't necessarily match
 * one-to-one with a Wavelet-delta.
 * <p>
 *
 */
@JsType
public class DocRevision {

  final DocHistory history;
  final int revisionIndex;
  HashedVersion appliedAtVersion;
  final HashedVersion resultingVersion;
  final double resultingTime;
  final String participant;
  DocOp op;

  @JsIgnore
  public DocRevision(DocHistory history,
      HashedVersion resultingVersion, HashedVersion appliedAtVersion,
      double resultingTime, String participant, int nextRevisionIndex) {
    super();
    this.history = history;
    this.resultingVersion = resultingVersion;
    this.appliedAtVersion = appliedAtVersion;
    this.resultingTime = resultingTime;
    this.participant = participant;
    this.revisionIndex = nextRevisionIndex;
  }

  @JsProperty
  public double getAppliedAtVersion() {
    if (appliedAtVersion != null)
      return appliedAtVersion.getVersion();
    else
      return -1;
  }

  @JsProperty
  public double getResultingVersion() {
    if (resultingVersion != null)
      return resultingVersion.getVersion();
    else
      return -1;
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
  public DocOp getDocOp() {
    return this.op;
  }

  @JsIgnore
  public void setDocOp(DocOp op) {
    Preconditions.checkArgument(this.op == null, "Can't overwrite the revision's doc op.");
    this.op = op;
  }

  @JsProperty
  public int getRevisionIndex() {
    return revisionIndex;
  }

  @JsIgnore
  public void setAppliedAtHashedVersion(HashedVersion appliedAtVersion) {
    this.appliedAtVersion = appliedAtVersion;
  }

  @JsIgnore
  public HashedVersion getAppliedAtHashedVersion() {
    return this.appliedAtVersion;
  }

  @JsIgnore
  public HashedVersion getResultingHashedVersion() {
    return this.resultingVersion;
  }

  @JsIgnore
  @Override
  public String toString() {
    return "[DocRevision#" + revisionIndex + "] version ( " + appliedAtVersion.getVersion() + " -> "
        + resultingVersion.getVersion() + ") by " + participant;
  }

}
