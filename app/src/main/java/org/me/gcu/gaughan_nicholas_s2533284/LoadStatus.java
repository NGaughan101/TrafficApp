package org.me.gcu.gaughan_nicholas_s2533284;

public enum LoadStatus {
    SUCCESS, // both feeds downloaded
    OFFLINE, // at least one feed failed. Cached copy used instead
    ERROR // at least one feed failed. No cached copy
}
