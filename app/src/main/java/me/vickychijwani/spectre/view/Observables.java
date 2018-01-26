package me.vickychijwani.spectre.view;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import me.vickychijwani.spectre.util.AppUtils;
import me.vickychijwani.spectre.util.log.Log;

public class Observables {

    private static final String TAG = "Observables";

    public static <T> Observable<T> getDialog(ObservableDialogMaker<T> dialogMaker) {
        return Observable.create(emitter -> {
            // confirm save for scheduled and published posts
            final AlertDialog alertDialog = dialogMaker.makeDialog(emitter);

            // dismiss the dialog automatically if this subscriber unsubscribes
            emitter.setCancellable(alertDialog::dismiss);
            alertDialog.show();
        });
    }

    public interface ObservableDialogMaker<T> {
        AlertDialog makeDialog(ObservableEmitter<T> emitter);
    }

    public static final class FileUploadMetadata {
        public final InputStream inputStream;
        @Nullable public final String filename;
        public final String mimeType;
        public FileUploadMetadata(InputStream inputStream, @Nullable String filename,
                                  String mimeType) {
            this.inputStream = inputStream;
            this.filename = filename;
            this.mimeType = mimeType;
        }
    }

    public static Observable<FileUploadMetadata> getFileUploadMetadataFromUri(
            @NonNull ContentResolver contentResolver,
            @NonNull Uri fileUri) {
        return Observable.create(emitter -> {
            try {
                Log.d(TAG, "Attempting to read uri: %s", fileUri.toString());
                InputStream inputStream = contentResolver.openInputStream(fileUri);
                String filename = AppUtils.getFileNameFromUri(contentResolver, fileUri);
                String mimeType = contentResolver.getType(fileUri);
                emitter.onNext(new FileUploadMetadata(inputStream, filename, mimeType));
            } catch (IOException e) {
                emitter.onError(e);
                Log.exception(new Exception("Failed to open input stream for uri, " +
                        "see previous exception", e));
            } finally {
                emitter.onComplete();
            }
        });
    }

}
