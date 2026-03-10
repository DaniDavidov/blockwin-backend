package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.hub.ReportType;

public final class ReportBitmap {

    private int bits;

    public synchronized boolean markSubmitted(ReportType type) {
        int mask = 1 << type.ordinal();

        if ((bits & mask) != 0) {
            return false; // already submitted
        }

        bits |= mask;
        return true;
    }

    public synchronized void clear(ReportType type) {
        int mask = 1 << type.ordinal();
        bits &= ~mask;
    }
}
