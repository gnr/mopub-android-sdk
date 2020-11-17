package com.mopub.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents the orientation returned for MoPub ads from the MoPub ad server.
 */
public enum CreativeOrientation {
    PORTRAIT, LANDSCAPE, UNDEFINED;

    @NonNull
    public static CreativeOrientation fromHeader(@Nullable String orientation) {
        if ("l".equalsIgnoreCase(orientation)) {
            return LANDSCAPE;
        }

        if ("p".equalsIgnoreCase(orientation)) {
            return PORTRAIT;
        }

        return UNDEFINED;
    }
}
