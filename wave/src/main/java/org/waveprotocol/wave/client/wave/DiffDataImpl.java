package org.waveprotocol.wave.client.wave;

public class DiffDataImpl implements DiffData<DiffDataImpl.RangeImpl, DiffDataImpl.ValuesImpl> {

  public class ValuesImpl implements DiffData.Values {

    public String author;

    @Override
    public String getAuthor() {
      return author;
    }

  }

  public class RangeImpl implements DiffData.Range<ValuesImpl> {

    public int start, end;
    public ValuesImpl values;

    @Override
    public int getStart() {
      return start;
    }

    @Override
    public int getEnd() {
      return end;
    }

    @Override
    public ValuesImpl getValues() {
      return values;
    }

  }

  public String waveletId;
  public RangeImpl[] ranges;

  @Override
  public String getDocId() {
    return waveletId;
  }

  @Override
  public RangeImpl[] getRanges() {
    return ranges;
  }

}
