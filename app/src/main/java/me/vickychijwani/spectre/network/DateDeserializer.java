package me.vickychijwani.spectre.network;

import android.annotation.SuppressLint;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import me.vickychijwani.spectre.util.log.Log;

class DateDeserializer implements JsonDeserializer<Date> {

    private static final String TAG = "DateDeserializer";

    @Override
    public Date deserialize(JsonElement element, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        String date = element.getAsString();

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            Log.e(TAG, "Date parsing failed");
            Log.exception(e);
            return new Date();
        }
    }

}
