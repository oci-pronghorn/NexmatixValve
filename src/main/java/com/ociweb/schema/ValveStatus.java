package com.ociweb.schema;

public final class ValveStatus implements java.io.Serializable {
  public int valveSerialNumber;
  public long timeStamp;
  public int stationNumber;
  public int cycleCountLimit;
  public int cycleCount;
  public float pressurePoint;
  public String pressureFault;
  public String detectedLeak;
  public String input;

  public ValveStatus() {}

  public ValveStatus(int _valveSerialNumber, long _timeStamp, int _stationNumber, int _cycleCountLimit, int _cycleCount, float _pressurePoint, String _pressureFault, String _detectedLeak, String _input) {
    valveSerialNumber = _valveSerialNumber;
    timeStamp = _timeStamp;
    stationNumber = _stationNumber;
    cycleCountLimit = _cycleCountLimit;
    cycleCount = _cycleCount;
    pressurePoint = _pressurePoint;
    pressureFault = _pressureFault;
    detectedLeak = _detectedLeak;
    input = _input;
  }
}
