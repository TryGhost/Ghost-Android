package me.vickychijwani.spectre.event;

import android.support.annotation.NonNull;

import java.io.InputStream;

public class FileUploadEvent implements ApiCallEvent {

    public final InputStream inputStream;
    @NonNull public final String filename;
    public final String mimeType;

    public FileUploadEvent(InputStream inputStream, @NonNull String filename, String mimeType) {
        this.inputStream = inputStream;
        this.filename = filename;
        this.mimeType = mimeType;
    }

    @Override
    public void loadCachedData() {
        // no-op; this event cannot be handled with cached data
    }

}
