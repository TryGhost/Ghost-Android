package me.vickychijwani.spectre.network

import me.vickychijwani.spectre.network.GhostApiUtils.hasOnlyMarkdownCard
import me.vickychijwani.spectre.network.GhostApiUtils.initializeMobiledoc
import me.vickychijwani.spectre.network.GhostApiUtils.insertMarkdownIntoMobiledoc
import me.vickychijwani.spectre.network.GhostApiUtils.mobiledocToMarkdown
import me.vickychijwani.spectre.testing.JvmLoggingRule
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.isEmptyString
import org.junit.*
import uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs
import org.hamcrest.Matchers.`is` as Is


/**
 * TYPE: unit tests
 * PURPOSE: testing logic for the mobiledoc utility methods
 */

class GhostApiUtilsTest {

    companion object {
        @ClassRule @JvmField val loggingRule = JvmLoggingRule()
    }

    private val MARKDOWN = "**bold** _italic_"
    private val NEW_MARKDOWN = "**bold** _italic_\n\n`code`"

    private val MOBILEDOC_BEFORE_KOENIG = makeMobiledoc("""[
        [ "card-markdown", { "cardName": "card-markdown", "markdown": "$MARKDOWN" } ]
    ]""")
    private val MOBILEDOC_KOENIG_ONLY_MARKDOWN = makeMobiledoc("""[
        [ "markdown", { "markdown": "$MARKDOWN" } ]
    ]""")
    private val MOBILEDOC_KOENIG_NOT_MARKDOWN = makeMobiledoc("""[
        [ "code", { "code": "int x = 5" } ]
    ]""")
    private val MOBILEDOC_KOENIG_MIXED_MARKDOWN = makeMobiledoc("""[
        [ "markdown", { "markdown": "$MARKDOWN" } ],
        [ "code", { "code": "int x = 5" } ]
    ]""")
    private val MOBILEDOC_KOENIG_NON_CARD_SECTIONS = """{
        "version": "0.3.1",
        "atoms": [],
        "cards": [["markdown", { "markdown": "$MARKDOWN"}]],
        "markups": [],
        "sections":[
            [10,0],
            [1,"p",[[0,[],0,"New card"]]]
        ]
    }"""



    // hasOnlyMarkdownCard
    @Test
    fun hasOnlyMarkdownCard_beforeKoenig() {
        assertThat(hasOnlyMarkdownCard(MOBILEDOC_BEFORE_KOENIG), Is(true))
    }

    @Test
    fun hasOnlyMarkdownCard_koenigOnlyMarkdown() {
        assertThat(hasOnlyMarkdownCard(MOBILEDOC_KOENIG_ONLY_MARKDOWN), Is(true))
    }

    @Test
    fun hasOnlyMarkdownCard_koenigNotMarkdown() {
        assertThat(hasOnlyMarkdownCard(MOBILEDOC_KOENIG_NOT_MARKDOWN), Is(false))
    }

    @Test
    fun hasOnlyMarkdownCard_koenigMixedMarkdown() {
        assertThat(hasOnlyMarkdownCard(MOBILEDOC_KOENIG_MIXED_MARKDOWN), Is(false))
    }

    @Test
    fun hasOnlyMarkdownCard_koenigNonCardSections() {
        assertThat(hasOnlyMarkdownCard(MOBILEDOC_KOENIG_NON_CARD_SECTIONS), Is(false))
    }



    // mobiledocToMarkdown
    @Test
    fun mobiledocToMarkdown_canParsePreKoenig() {
        assertThat(mobiledocToMarkdown(MOBILEDOC_BEFORE_KOENIG), Is(MARKDOWN))
    }

    @Test
    fun mobiledocToMarkdown_canParseWhenOnlyMarkdown() {
        assertThat(mobiledocToMarkdown(MOBILEDOC_KOENIG_ONLY_MARKDOWN), Is(MARKDOWN))
    }

    @Test(expected = KoenigPostException::class)
    fun mobiledocToMarkdown_cannotParseNonMarkdown() {
        mobiledocToMarkdown(MOBILEDOC_KOENIG_NOT_MARKDOWN)
    }

    @Test(expected = KoenigPostException::class)
    fun mobiledocToMarkdown_cannotParseMixedMarkdown() {
        mobiledocToMarkdown(MOBILEDOC_KOENIG_MIXED_MARKDOWN)
    }

    @Test(expected = KoenigPostException::class)
    fun mobiledocToMarkdown_cannotParseNonCardSections() {
        mobiledocToMarkdown(MOBILEDOC_KOENIG_NON_CARD_SECTIONS)
    }



    // initializeMobiledoc
    @Test
    fun initializeMobiledoc_hasOnlyMarkdownCard() {
        assertThat(hasOnlyMarkdownCard(initializeMobiledoc()), Is(true))
    }

    @Test
    fun initializeMobiledoc_hasEmptyMarkdown() {
        assertThat(mobiledocToMarkdown(initializeMobiledoc()), isEmptyString())
    }



    // insertMarkdownIntoMobiledoc
    @Test
    fun insertMarkdownIntoMobiledoc_beforeKoenig() {
        assertThat(mobiledocToMarkdown(insertMarkdownIntoMobiledoc(NEW_MARKDOWN, MOBILEDOC_BEFORE_KOENIG)),
                Is(NEW_MARKDOWN))
    }

    @Test
    fun insertMarkdownIntoMobiledoc_koenigOnlyMarkdown() {
        assertThat(mobiledocToMarkdown(insertMarkdownIntoMobiledoc(NEW_MARKDOWN, MOBILEDOC_KOENIG_ONLY_MARKDOWN)),
                Is(NEW_MARKDOWN))
    }

    @Test(expected = KoenigPostException::class)
    fun insertMarkdownIntoMobiledoc_koenigNotMarkdown() {
        insertMarkdownIntoMobiledoc(NEW_MARKDOWN, MOBILEDOC_KOENIG_NOT_MARKDOWN)
    }

    @Test(expected = KoenigPostException::class)
    fun insertMarkdownIntoMobiledoc_koenigMixedMarkdown() {
        insertMarkdownIntoMobiledoc(NEW_MARKDOWN, MOBILEDOC_KOENIG_MIXED_MARKDOWN)
    }

    @Test
    fun insertEmptyMarkdownIntoNewlyInitializedMobiledoc() {
        assertThat(insertMarkdownIntoMobiledoc("", initializeMobiledoc()),
                sameJSONAs(initializeMobiledoc()))
    }

    @Test
    fun insertNonEmptyMarkdownIntoNewlyInitializedMobiledoc() {
        assertThat(insertMarkdownIntoMobiledoc(MARKDOWN, initializeMobiledoc()),
                sameJSONAs(MOBILEDOC_BEFORE_KOENIG))
    }

    @Test
    fun insertMarkdownIntoMobiledoc_isTransitive() {
        // inserting markdown must be transitive: a -> b and b -> c is equivalent to a -> c
        val a = initializeMobiledoc()
        val b = insertMarkdownIntoMobiledoc(MARKDOWN, a)
        val cFromB = insertMarkdownIntoMobiledoc(NEW_MARKDOWN, b)
        val cFromA = insertMarkdownIntoMobiledoc(NEW_MARKDOWN, a)
        assertThat(cFromB, Is(cFromA))
    }



    // helpers
    private fun makeMobiledoc(cards: String): String {
        return """
            {
                "version": "0.3.1",
                "markups": [],
                "atoms": [],
                "cards": $cards,
                "sections": [[10, 0]]
            }
        """
    }

}
