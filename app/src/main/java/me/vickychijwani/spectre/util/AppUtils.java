package me.vickychijwani.spectre.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.util.functions.Action1;
import me.vickychijwani.spectre.util.log.Log;

public class AppUtils {

    private static final String TAG = AppUtils.class.getSimpleName();

    @NonNull
    public static String getAppVersion(@NonNull Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (packageInfo != null) {
                return packageInfo.versionName;
            } else {
                return context.getString(R.string.version_unknown);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.exception(new RuntimeException("Failed to get package info, " +
                    "see previous exception for details", e));
            return context.getString(R.string.version_unknown);
        }
    }

    public static void showSystemAppSettingsActivity(@NonNull final Activity context) {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    /**
     * Set the app to use the given locale. Useful for testing translations. This is normally
     * not needed because the device locale is applied automatically.
     * @param context - context from which to get resources
     * @param locale - the locale to use
     */
    public static void setLocale(@NonNull Context context, @NonNull Locale locale) {
        Locale.setDefault(locale);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = Locale.getDefault();
        res.updateConfiguration(conf, dm);
    }

    public static void openUri(@NonNull Context context, @NonNull String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, R.string.intent_no_apps,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void openUri(@NonNull Fragment fragment, @NonNull String uri) {
        openUri(fragment.getActivity(), uri);
    }

    // credits: https://stackoverflow.com/a/25005243/504611
    @Nullable
    public static String getFileNameFromUri(@NonNull ContentResolver contentResolver,
                                            @NonNull Uri uri) {
        String filename = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = contentResolver.query(uri, null, null,
                    null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (filename == null) {
            filename = uri.getPath();
            int cut = filename.lastIndexOf('/');
            if (cut != -1) {
                filename = filename.substring(cut + 1);
            }
        }
        return filename;
    }

    // Add a custom event handler for link clicks in TextView HTML
    // credits: https://stackoverflow.com/a/19989677/504611
    public static void setHtmlWithLinkClickHandler(TextView tv, String html,
                                            Action1<String> linkClickHandler) {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            ClickableSpan clickable = new ClickableSpan() {
                public void onClick(View view) {
                    linkClickHandler.call(span.getURL());
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }
        tv.setText(strBuilder);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

}
