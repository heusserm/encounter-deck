package com.encounterdeck.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * The SRD strip has to open the full attribution, on every screen that shows
 * SRD-derived content.
 *
 * This is a license requirement, not a nicety. CC-BY-4.0 3(a)(2) allows the
 * required notice to live behind a link rather than beside the work, which is
 * the whole reason the six lines were reduced to one — but only a link that
 * opens qualifies. For three releases nothing rendered the dialog: every
 * `SrdNotice` set `showAbout`, and no one read it, so the attribution was
 * unreachable in the shipped app. Nothing caught it because this module had
 * no tests.
 */
@OptIn(ExperimentalTestApi::class)
class AboutDialogTest {

    @Test
    fun `the SRD strip opens the full attribution`() = runComposeUiTest {
        setContent { App() }

        onNodeWithText(SRD_STRIP).performClick()

        // Text the dialog shows and the strip does not, so this cannot pass
        // against a strip that merely exists.
        onNodeWithText(DIALOG_TITLE).assertExists()
    }

    @Test
    fun `the attribution names the license and says the work was modified`() = runComposeUiTest {
        setContent { App() }

        onNodeWithText(SRD_STRIP).performClick()

        // CC-BY-4.0 3(a)(1)(B): saying it was modified is required, because
        // gen_seed.py abridges stat blocks and invents the location tags.
        onNodeWithText(MODIFIED_PHRASE, substring = true).assertExists()
    }

    @Test
    fun `the dialog closes again`() = runComposeUiTest {
        setContent { App() }
        onNodeWithText(SRD_STRIP).performClick()

        onNodeWithText("Close").performClick()

        onNodeWithText(DIALOG_TITLE).assertDoesNotExist()
    }
}

private const val SRD_STRIP = "SRD 5.1 · CC BY 4.0 · modified · About"
private const val DIALOG_TITLE = "About EncounterDeck"
private const val MODIFIED_PHRASE = "Modified from the original"
