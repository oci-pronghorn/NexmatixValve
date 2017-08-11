package com.ociweb.schema;

public enum FieldType {
    integer,
    string,
    floatingPoint,
    int64;

    String getPattern() {
        switch (this) {
            case integer:
                return "%u";
            case string:
                return "\"%b\"";
            case floatingPoint:
                return "%i%.";
            case int64:
                return "%u";
        }
        return null;
    }
}
