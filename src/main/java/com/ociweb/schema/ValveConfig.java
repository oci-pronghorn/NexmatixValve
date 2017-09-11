package com.ociweb.schema;

public final class ValveConfig implements java.io.Serializable {
  public int valveSerialNumber;
  public int manifoldSerialNumber;
  public int stationNumber;
  public int cycleCountLimit;
  public long timeStamp;
  public long fabricationDate;
  public long shippingDate;
  public String productNumber;

  public ValveConfig() {}

  public ValveConfig(int _valveSerialNumber, int _manifoldSerialNumber, int _stationNumber, int _cycleCountLimit, long _timeStamp, long _fabricationDate, long _shippingDate, String _productNumber) {
    valveSerialNumber = _valveSerialNumber;
    manifoldSerialNumber = _manifoldSerialNumber;
    stationNumber = _stationNumber;
    cycleCountLimit = _cycleCountLimit;
    timeStamp = _timeStamp;
    fabricationDate = _fabricationDate;
    shippingDate = _shippingDate;
    productNumber = _productNumber;
  }
}
