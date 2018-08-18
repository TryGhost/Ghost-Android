package me.vickychijwani.spectre.network

import com.google.gson.*
import me.vickychijwani.spectre.network.entity.*
import me.vickychijwani.spectre.util.NetworkUtils
import me.vickychijwani.spectre.util.log.Log
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*

object GhostApiUtils {

    private val TAG = GhostApiUtils::class.java.simpleName

    fun getRetrofit(blogUrl: String, httpClient: OkHttpClient): Retrofit {
        val baseUrl = NetworkUtils.makeAbsoluteUrl(blogUrl, "ghost/api/v0.1/")
        val gson = GsonBuilder()
                .registerTypeAdapter(Date::class.java, DateDeserializer())
                .registerTypeAdapter(ConfigurationList::class.java, ConfigurationListDeserializer())
                .registerTypeAdapterFactory(PostTypeAdapterFactory())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies(RealmExclusionStrategy(), AnnotationExclusionStrategy())
                .create()
        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                // for HTML output (e.g., to get the client secret)
                .addConverterFactory(StringConverterFactory.create())
                // for raw JSONObject output (e.g., for the /configuration/about call)
                .addConverterFactory(JSONObjectConverterFactory.create())
                // for domain objects
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
    }

    fun parseApiErrors(retrofit: Retrofit, exception: HttpException): ApiErrorList? {
        var apiErrors: ApiErrorList? = null
        try {
            val response = exception.response()
            val errorBody = response.errorBody()
            apiErrors = retrofit.responseBodyConverter<Any>(
                    ApiErrorList::class.java, arrayOfNulls(0)).convert(errorBody!!) as ApiErrorList
        } catch (e: IOException) {
            Log.e(TAG, "Error while parsing login errors! Response code = %d",
                    exception.response().code())
            Log.exception(e)
        } catch (e: ClassCastException) {
            Log.e(TAG, "Error while parsing login errors! Response code = %d", exception.response().code())
            Log.exception(e)
        }

        return apiErrors
    }

    fun hasOnlyMarkdownCard(mobiledoc: String): Boolean {
        val jsonParser = JsonParser()
        val mobiledocJson = jsonParser.parse(mobiledoc).asJsonObject
        return hasOnlyMarkdownCard(mobiledocJson)
    }

    fun hasOnlyMarkdownCard(mobiledocJson: JsonObject): Boolean {
        return try {
            val cardsJson = mobiledocJson.get("cards").asJsonArray
            val firstCardJson = cardsJson.get(0).asJsonArray
            val firstCardName = firstCardJson.get(0).asString
            val firstCardContent = firstCardJson.get(1).asJsonObject

            val sectionsJson = mobiledocJson.get("sections").asJsonArray

            // ignore empty sections, only check non-empty sections
            val nonEmptySections = mutableListOf<JsonElement>()
            for (i in 0..(sectionsJson.size()-1)) {
                val section = sectionsJson.get(i).asJsonArray
                // empty sections are of the form [1, "p", []] - the "1" indicates it's a "markup" section
                // https://github.com/bustle/mobiledoc-kit/blob/master/MOBILEDOC.md#sections
                val isSectionEmpty = section.size() == 3 && section.get(0).asInt == 1
                        && section.get(2).asJsonArray.size() == 0
                if (!isSectionEmpty) {
                    nonEmptySections.add(section)
                }
            }

            val firstNonEmptySectionJson = nonEmptySections[0].asJsonArray

            (cardsJson.size() == 1
                    // the markdown card was named "card-markdown" before Koenig came along
                    && (firstCardName == "markdown" || firstCardName == "card-markdown")
                    && firstCardContent.has("markdown")
                    // https://github.com/bustle/mobiledoc-kit/blob/master/MOBILEDOC.md#sections
                    // sections: [[10, 0]] i.e., a single card (id for any card = 10) at index 0
                    && nonEmptySections.size == 1
                    && firstNonEmptySectionJson.get(0).asInt == 10
                    && firstNonEmptySectionJson.get(1).asInt == 0)
        } catch (e: RuntimeException) {
            false
        }
    }

    fun initializeMobiledoc(): String {
        return """
            {
                "version": "0.3.1",
                "markups": [],
                "atoms": [],
                "cards": [[
                    "card-markdown",
                    {
                        "cardName": "card-markdown",
                        "markdown": ""
                    }
                ]],
                "sections": [[10, 0]]
            }
        """.trimIndent()
    }

    fun insertMarkdownIntoMobiledoc(markdown: String, mobiledoc: String): String {
        val jsonParser = JsonParser()
        val mobiledocJson = jsonParser.parse(mobiledoc).asJsonObject

        if (! hasOnlyMarkdownCard(mobiledocJson)) {
            throw KoenigPostException()
        }

        try {
            mobiledocJson
                    .get("cards").asJsonArray
                    .get(0).asJsonArray
                    .get(1).asJsonObject
                    .addProperty("markdown", markdown)
        } catch (e: RuntimeException) {
            Log.w(TAG, mobiledoc)
            throw KoenigPostException()
        }
        return mobiledocJson.toString()
    }

    fun mobiledocToMarkdown(mobiledoc: String): String? {
        val jsonParser = JsonParser()
        val mobiledocJson = jsonParser.parse(mobiledoc).asJsonObject

        if (! hasOnlyMarkdownCard(mobiledocJson)) {
            throw KoenigPostException()
        }

        try {
            return mobiledocJson
                    .get("cards").asJsonArray
                    .get(0).asJsonArray
                    .get(1).asJsonObject
                    .get("markdown").asString
        } catch (e: RuntimeException) {
            Log.w(TAG, mobiledoc)
            throw KoenigPostException()
        }
    }

}

internal class KoenigPostException
    : RuntimeException("This is a Koenig editor post and therefore currently unsupported")
