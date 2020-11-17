package com.mopub.common.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

public class MoPubCollections {
    public static <T> void addAllNonNull(@NonNull final Collection<? super T> c,
            @Nullable final T... a) {
        Collections.addAll(c, a);
        c.removeAll(Collections.singleton(null));
    }
}
