# Inkling Phase 1a Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An Android launcher that keeps a 4-year-old inside an allowlist of apps with daily caps, logs usage, lets the parent out with a PIN, and runs the speech-recognition spike that gates the tutor.

**Architecture:** Single-module Android app. Pure-Kotlin decision logic in `core/` (no Android imports, fully unit tested). Room for storage. One `AccessibilityService` does blocking, usage logging, and cap enforcement. Compose UI with animations disabled. Inkling registers as a HOME app; no Device Owner.

**Tech Stack:** Kotlin 2.0, Jetpack Compose (BOM 2024.09), Room 2.6 with KSP, Robolectric for DAO tests, JUnit 4. Min SDK 29, target 34.

**Spec:** `docs/superpowers/specs/2026-09-04-inkling-phase1-design.md`

## Global Constraints

- Min SDK 29, compile and target SDK 34.
- Application id and package: `dev.inkling`.
- Kid-facing copy comes verbatim from `docs/prototype/inkling-sim.html`. No exclamation marks except "Nice reading!". No "wrong", "fail", "incorrect".
- No animations anywhere. Every `NavHost` transition is `EnterTransition.None` / `ExitTransition.None`.
- No rewards UI. No stars, coins, streaks.
- Kid text uses the Andika typeface (SIL Open Font License), shipped in `res/font`.
- `core/` has zero `android.*` imports. Enforced by a unit test in Task 3.
- Every task ends with `./gradlew :app:testDebugUnitTest :app:assembleDebug` green, then a commit.
- Commits end with `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.
- Version numbers below are pinned. If Gradle cannot resolve one, bump to the nearest newer stable and note it in the commit message. Do not downgrade.

---

### Task 1: Gradle scaffold and empty app

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/gradle/libs.versions.toml`
- Create: `android/gradle/wrapper/gradle-wrapper.properties`
- Create: `android/gradlew` (downloaded)
- Create: `android/gradle/wrapper/gradle-wrapper.jar` (downloaded)
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/kotlin/dev/inkling/MainActivity.kt`
- Create: `android/app/src/main/res/values/strings.xml`
- Create: `android/app/src/main/res/values/themes.xml`
- Create: `android/app/src/main/res/font/andika_regular.ttf` (downloaded)
- Create: `android/app/src/main/res/font/andika_bold.ttf` (downloaded)
- Create: `android/app/src/test/kotlin/dev/inkling/SmokeTest.kt`

**Interfaces:**
- Produces: the module `:app`, package `dev.inkling`, and the `libs` version catalog aliases used by every later task.

- [ ] **Step 1: Write the version catalog**

`android/gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.6.0"
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
composeBom = "2024.09.03"
activityCompose = "1.9.2"
navigation = "2.8.1"
room = "2.6.1"
coreKtx = "1.13.1"
lifecycle = "2.8.6"
junit = "4.13.2"
robolectric = "4.13"
androidxTest = "1.6.1"

[libraries]
core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
junit = { module = "junit:junit", version.ref = "junit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidxTest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Write settings, root build, properties**

`android/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "inkling"
include(":app")
```

`android/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

`android/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`android/gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 3: Download the wrapper script and jar, and the Andika fonts**

```bash
cd android
curl -fsSL -o gradlew https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradlew
curl -fsSL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew
mkdir -p app/src/main/res/font
curl -fsSL -o app/src/main/res/font/andika_regular.ttf "https://github.com/google/fonts/raw/main/ofl/andika/Andika-Regular.ttf"
curl -fsSL -o app/src/main/res/font/andika_bold.ttf "https://github.com/google/fonts/raw/main/ofl/andika/Andika-Bold.ttf"
file app/src/main/res/font/*.ttf
```

Expected: both `.ttf` files report `TrueType Font data`. If the google/fonts path 404s, download from https://software.sil.org/andika/ and unzip the two TTFs instead.

- [ ] **Step 4: Write the app module build file**

`android/app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.inkling"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.inkling"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
    sourceSets["test"].kotlin.srcDirs("src/test/kotlin")
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
```

- [ ] **Step 5: Manifest, strings, theme, activity**

`android/app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:label="@string/app_name"
        android:theme="@style/Theme.Inkling"
        android:allowBackup="false"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:excludeFromRecents="true"
            android:stateNotNeeded="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`android/app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Inkling</string>
</resources>
```

`android/app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Inkling" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">#EDEBE6</item>
        <item name="android:windowAnimationStyle">@null</item>
        <item name="android:windowDisablePreview">true</item>
    </style>
</resources>
```

`android/app/src/main/kotlin/dev/inkling/MainActivity.kt`:

```kotlin
package dev.inkling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Inkling") }
    }
}
```

- [ ] **Step 6: Smoke test**

`android/app/src/test/kotlin/dev/inkling/SmokeTest.kt`:

```kotlin
package dev.inkling

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun gradleRunsTests() {
        assertEquals(4, 2 + 2)
    }
}
```

- [ ] **Step 7: Build**

Run: `cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, and `app/build/outputs/apk/debug/app-debug.apk` exists. First run downloads Gradle and dependencies; allow 5 minutes.

- [ ] **Step 8: Commit**

```bash
git add android
git commit -m "chore(android): gradle scaffold, empty app, Andika font

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 2: ReadingDiff (pure Kotlin, TDD)

**Files:**
- Create: `android/app/src/main/kotlin/dev/inkling/core/ReadingDiff.kt`
- Test: `android/app/src/test/kotlin/dev/inkling/core/ReadingDiffTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class WordResult { OK, MISS, UNSURE }
  data class DiffResult(val words: List<Pair<String, WordResult>>, val lowConfidence: Boolean) {
      val missed: List<String>
  }
  object ReadingDiff {
      const val CONFIDENCE_FLOOR = 0.5f
      fun score(expected: String, transcript: String, confidence: Float): DiffResult
  }
  ```

- [ ] **Step 1: Write the failing tests**

`android/app/src/test/kotlin/dev/inkling/core/ReadingDiffTest.kt`:

```kotlin
package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingDiffTest {
    private val line = "The big red hen sat in the pen."

    @Test
    fun exactReadIsAllOk() {
        val r = ReadingDiff.score(line, "the big red hen sat in the pen", 0.9f)
        assertTrue(r.words.all { it.second == WordResult.OK })
        assertFalse(r.lowConfidence)
        assertEquals(emptyList<String>(), r.missed)
    }

    @Test
    fun oneSubstitutionIsOneMiss() {
        val r = ReadingDiff.score(line, "the big red hen sat in the pin", 0.9f)
        assertEquals(listOf("pen"), r.missed)
    }

    @Test
    fun skippedWordIsAMiss() {
        val r = ReadingDiff.score(line, "the big hen sat in the pen", 0.9f)
        assertEquals(listOf("red"), r.missed)
    }

    @Test
    fun extraWordsFromSelfCorrectionAreIgnored() {
        val r = ReadingDiff.score(line, "the big red hen hen sat in the pen", 0.9f)
        assertEquals(emptyList<String>(), r.missed)
    }

    @Test
    fun typicalArticulationIsNotAMiss() {
        // r -> w, l -> w, th -> f
        assertEquals(emptyList<String>(), ReadingDiff.score("the red lamp", "the wed wamp", 0.9f).missed)
        assertEquals(emptyList<String>(), ReadingDiff.score("thin", "fin", 0.9f).missed)
    }

    @Test
    fun initialClusterReductionIsNotAMiss() {
        assertEquals(emptyList<String>(), ReadingDiff.score("stop the truck", "top the tuck", 0.9f).missed)
    }

    @Test
    fun lowConfidenceNeverProducesMiss() {
        val r = ReadingDiff.score(line, "the big red hen sat in the pin", 0.3f)
        assertTrue(r.lowConfidence)
        assertTrue(r.words.none { it.second == WordResult.MISS })
        assertTrue(r.words.any { it.second == WordResult.UNSURE })
    }

    @Test
    fun emptyTranscriptIsLowConfidence() {
        val r = ReadingDiff.score(line, "", 0.9f)
        assertTrue(r.lowConfidence)
        assertEquals(emptyList<String>(), r.missed)
    }

    @Test
    fun punctuationAndCaseAreIgnored() {
        val r = ReadingDiff.score("Hop, on Pop!", "hop on pop", 0.9f)
        assertEquals(emptyList<String>(), r.missed)
        assertEquals(listOf("Hop,", "on", "Pop!"), r.words.map { it.first })
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.core.ReadingDiffTest'`
Expected: compilation error, `Unresolved reference: ReadingDiff`.

- [ ] **Step 3: Implement**

`android/app/src/main/kotlin/dev/inkling/core/ReadingDiff.kt`:

```kotlin
package dev.inkling.core

enum class WordResult { OK, MISS, UNSURE }

/**
 * One scored read-aloud attempt.
 * @property words the expected words in order (original spelling) with their result
 * @property lowConfidence true when the recognizer was not sure enough to flag anything
 */
data class DiffResult(val words: List<Pair<String, WordResult>>, val lowConfidence: Boolean) {
    val missed: List<String> get() = words.filter { it.second == WordResult.MISS }.map { it.first }
}

/**
 * Compares what a child said with the line on the page.
 *
 * Rules, in order:
 * 1. Empty transcript or confidence under [CONFIDENCE_FLOOR] means every word is UNSURE.
 * 2. Words are aligned by edit distance so a skipped or repeated word does not shift the rest.
 * 3. A word matches if the normalized forms are equal, or differ only by a typical
 *    3-to-5-year-old articulation pattern (r/l to w, th to f or d, initial cluster reduction).
 * 4. Everything else is MISS.
 */
object ReadingDiff {
    const val CONFIDENCE_FLOOR = 0.5f

    fun score(expected: String, transcript: String, confidence: Float): DiffResult {
        val expectedWords = expected.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val heard = transcript.trim().split(Regex("\\s+")).map(::normalize).filter { it.isNotEmpty() }
        if (heard.isEmpty() || confidence < CONFIDENCE_FLOOR) {
            return DiffResult(expectedWords.map { it to WordResult.UNSURE }, lowConfidence = true)
        }
        val exp = expectedWords.map(::normalize)
        val matched = align(exp, heard)
        val results = expectedWords.mapIndexed { i, original ->
            original to if (matched[i]) WordResult.OK else WordResult.MISS
        }
        return DiffResult(results, lowConfidence = false)
    }

    private fun normalize(w: String): String = w.lowercase().filter { it.isLetter() || it == '\'' }

    /** Standard edit-distance alignment. Returns, per expected word, whether it was matched. */
    private fun align(exp: List<String>, heard: List<String>): BooleanArray {
        val n = exp.size
        val m = heard.size
        val cost = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) cost[i][0] = i
        for (j in 0..m) cost[0][j] = j
        for (i in 1..n) for (j in 1..m) {
            val sub = if (sameWord(exp[i - 1], heard[j - 1])) 0 else 1
            cost[i][j] = minOf(cost[i - 1][j] + 1, cost[i][j - 1] + 1, cost[i - 1][j - 1] + sub)
        }
        val matched = BooleanArray(n)
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            val same = sameWord(exp[i - 1], heard[j - 1])
            when {
                same && cost[i][j] == cost[i - 1][j - 1] -> { matched[i - 1] = true; i--; j-- }
                cost[i][j] == cost[i][j - 1] + 1 -> j--          // extra heard word (self-correction)
                cost[i][j] == cost[i - 1][j] + 1 -> i--          // skipped expected word
                else -> { i--; j-- }                             // substitution
            }
        }
        return matched
    }

    private fun sameWord(expected: String, heard: String): Boolean {
        if (expected == heard) return true
        return articulationVariants(expected).contains(heard)
    }

    /** Forms a typical 3-to-5-year-old might produce for a correctly decoded word. */
    private fun articulationVariants(w: String): Set<String> {
        val out = mutableSetOf<String>()
        val subs = listOf("r" to "w", "l" to "w", "th" to "f", "th" to "d")
        var forms = setOf(w)
        for ((from, to) in subs) {
            forms = forms + forms.map { it.replace(from, to) }
        }
        out += forms
        // Initial cluster reduction: "stop" -> "top", "truck" -> "tuck", "blue" -> "bue"
        val clusters = listOf("st", "sp", "sk", "tr", "dr", "br", "bl", "cl", "fl", "gl", "pl", "sl", "cr", "fr", "gr", "pr", "sn", "sm", "sw")
        for (c in clusters) {
            if (w.startsWith(c)) {
                out += w.drop(1)          // drop first consonant: stop -> top
                out += c[0] + w.drop(2)   // drop second: truck -> tuck
            }
        }
        // Reduction combined with r/l/th substitution, e.g. "truck" -> "tuck" is already covered;
        // "thrill" -> "fwill" would need both, handled by applying subs to reduced forms.
        val reduced = out.toList()
        for (r in reduced) {
            var f = setOf(r)
            for ((from, to) in subs) f = f + f.map { it.replace(from, to) }
            out += f
        }
        return out
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.core.ReadingDiffTest'`
Expected: 9 tests pass. If `skippedWordIsAMiss` or `extraWordsFromSelfCorrectionAreIgnored` fail, the backtrace order in `align` is wrong; the diagonal-match branch must be checked first, then the heard-extra branch.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/dev/inkling/core/ReadingDiff.kt android/app/src/test/kotlin/dev/inkling/core/ReadingDiffTest.kt
git commit -m "feat(core): ReadingDiff scores a read-aloud line against the page

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 3: Budget, quiet hours, block decision, PIN policy (pure Kotlin, TDD)

**Files:**
- Create: `android/app/src/main/kotlin/dev/inkling/core/Budget.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/core/QuietHours.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/core/BlockDecision.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/core/PinPolicy.kt`
- Test: `android/app/src/test/kotlin/dev/inkling/core/BudgetTest.kt`
- Test: `android/app/src/test/kotlin/dev/inkling/core/QuietHoursTest.kt`
- Test: `android/app/src/test/kotlin/dev/inkling/core/BlockDecisionTest.kt`
- Test: `android/app/src/test/kotlin/dev/inkling/core/PinPolicyTest.kt`
- Test: `android/app/src/test/kotlin/dev/inkling/core/NoAndroidImportsTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class Span(val packageName: String, val startedAt: Long, val endedAt: Long?)   // epoch millis
  data class Rule(val packageName: String, val enabled: Boolean, val dailyCapMinutes: Int) // 0 = no cap
  object Budget {
      fun usedMinutesToday(spans: List<Span>, packageName: String, now: Long, zoneOffsetMillis: Long): Int
      fun usedMinutesTodayAll(spans: List<Span>, packages: Set<String>, now: Long, zoneOffsetMillis: Long): Int
      fun startOfDay(now: Long, zoneOffsetMillis: Long): Long
  }
  object QuietHours { fun isQuiet(minuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean }
  enum class Verdict { ALLOW, BLOCK_NOT_ALLOWED, BLOCK_CAPPED, BLOCK_CEILING, BLOCK_QUIET }
  object BlockDecision {
      val SYSTEM_ALLOWLIST: Set<String>
      fun decide(packageName: String, selfPackage: String, rules: List<Rule>, kidsModeOn: Boolean,
                 usedMinutes: Int, usedAllMinutes: Int, ceilingMinutes: Int, quiet: Boolean, readPackage: String): Verdict
  }
  object PinPolicy {
      const val FREE_ATTEMPTS = 3
      fun lockoutSeconds(failures: Int): Int
  }
  ```

- [ ] **Step 1: Write the failing tests**

`BudgetTest.kt`:

```kotlin
package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetTest {
    private val day = 24L * 60 * 60 * 1000
    private val minute = 60L * 1000
    private val noon = 3 * day + 12 * 60 * minute   // some day at 12:00 UTC
    private val zone = 0L

    @Test
    fun startOfDayIsMidnightInZone() {
        assertEquals(3 * day, Budget.startOfDay(noon, zone))
        // UTC-7: local midnight is 07:00 UTC
        assertEquals(3 * day + 7 * 60 * minute, Budget.startOfDay(noon, -7 * 60 * minute))
    }

    @Test
    fun sumsOnlyTodayAndOnlyThatPackage() {
        val spans = listOf(
            Span("chess", noon - 30 * minute, noon - 10 * minute),   // 20 min today
            Span("chess", noon - day - 30 * minute, noon - day),     // yesterday, ignored
            Span("hangman", noon - 5 * minute, noon),                // other app
        )
        assertEquals(20, Budget.usedMinutesToday(spans, "chess", noon, zone))
    }

    @Test
    fun openSpanCountsUpToNow() {
        val spans = listOf(Span("chess", noon - 7 * minute, null))
        assertEquals(7, Budget.usedMinutesToday(spans, "chess", noon, zone))
    }

    @Test
    fun spanCrossingMidnightCountsOnlyTodayPart() {
        val midnight = 3 * day
        val spans = listOf(Span("chess", midnight - 10 * minute, midnight + 5 * minute))
        assertEquals(5, Budget.usedMinutesToday(spans, "chess", noon, zone))
    }

    @Test
    fun allPackagesSum() {
        val spans = listOf(
            Span("chess", noon - 30 * minute, noon - 10 * minute),
            Span("hangman", noon - 5 * minute, noon),
            Span("read", noon - 50 * minute, noon - 40 * minute),
        )
        assertEquals(25, Budget.usedMinutesTodayAll(spans, setOf("chess", "hangman"), noon, zone))
    }
}
```

`QuietHoursTest.kt`:

```kotlin
package dev.inkling.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    private val h = 60
    @Test fun insideSameDayRange() { assertTrue(QuietHours.isQuiet(13 * h, 12 * h, 14 * h)) }
    @Test fun outsideSameDayRange() { assertFalse(QuietHours.isQuiet(15 * h, 12 * h, 14 * h)) }
    @Test fun crossesMidnightLateEvening() { assertTrue(QuietHours.isQuiet(20 * h, 19 * h + 30, 7 * h)) }
    @Test fun crossesMidnightEarlyMorning() { assertTrue(QuietHours.isQuiet(6 * h, 19 * h + 30, 7 * h)) }
    @Test fun crossesMidnightDaytimeIsNotQuiet() { assertFalse(QuietHours.isQuiet(12 * h, 19 * h + 30, 7 * h)) }
    @Test fun startEqualsEndMeansNeverQuiet() { assertFalse(QuietHours.isQuiet(12 * h, 8 * h, 8 * h)) }
}
```

`BlockDecisionTest.kt`:

```kotlin
package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockDecisionTest {
    private val self = "dev.inkling"
    private val rules = listOf(
        Rule("com.chess", enabled = true, dailyCapMinutes = 30),
        Rule("com.hangman", enabled = false, dailyCapMinutes = 15),
    )
    private fun decide(pkg: String, kids: Boolean = true, used: Int = 0, usedAll: Int = 0, ceiling: Int = 60, quiet: Boolean = false) =
        BlockDecision.decide(pkg, self, rules, kids, used, usedAll, ceiling, quiet, readPackage = self)

    @Test fun kidsModeOffAllowsEverything() { assertEquals(Verdict.ALLOW, decide("com.android.settings", kids = false)) }
    @Test fun selfIsAllowed() { assertEquals(Verdict.ALLOW, decide(self)) }
    @Test fun systemUiIsAllowed() { assertEquals(Verdict.ALLOW, decide("com.android.systemui")) }
    @Test fun enabledAppUnderCapIsAllowed() { assertEquals(Verdict.ALLOW, decide("com.chess", used = 29)) }
    @Test fun enabledAppAtCapIsBlocked() { assertEquals(Verdict.BLOCK_CAPPED, decide("com.chess", used = 30)) }
    @Test fun disabledAppIsBlocked() { assertEquals(Verdict.BLOCK_NOT_ALLOWED, decide("com.hangman")) }
    @Test fun unknownAppIsBlocked() { assertEquals(Verdict.BLOCK_NOT_ALLOWED, decide("com.android.settings")) }
    @Test fun ceilingBlocksEnabledApp() { assertEquals(Verdict.BLOCK_CEILING, decide("com.chess", used = 5, usedAll = 60)) }
    @Test fun ceilingZeroMeansNoCeiling() { assertEquals(Verdict.ALLOW, decide("com.chess", usedAll = 500, ceiling = 0)) }
    @Test fun quietBlocksEnabledApp() { assertEquals(Verdict.BLOCK_QUIET, decide("com.chess", quiet = true)) }
    @Test fun quietNeverBlocksRead() {
        val v = BlockDecision.decide(self, self, rules, true, 0, 999, 60, quiet = true, readPackage = self)
        assertEquals(Verdict.ALLOW, v)
    }
}
```

`PinPolicyTest.kt`:

```kotlin
package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PinPolicyTest {
    @Test fun firstThreeFailuresAreFree() {
        assertEquals(0, PinPolicy.lockoutSeconds(1))
        assertEquals(0, PinPolicy.lockoutSeconds(3))
    }
    @Test fun thenThirtySecondsDoubling() {
        assertEquals(30, PinPolicy.lockoutSeconds(4))
        assertEquals(60, PinPolicy.lockoutSeconds(5))
        assertEquals(120, PinPolicy.lockoutSeconds(6))
    }
    @Test fun capsAtOneHour() { assertEquals(3600, PinPolicy.lockoutSeconds(20)) }
}
```

`NoAndroidImportsTest.kt`:

```kotlin
package dev.inkling.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoAndroidImportsTest {
    @Test
    fun coreHasNoAndroidImports() {
        val dir = File("src/main/kotlin/dev/inkling/core")
        assertTrue("core dir missing at ${dir.absolutePath}", dir.isDirectory)
        val offenders = dir.walk().filter { it.extension == "kt" }
            .filter { f -> f.readLines().any { it.trimStart().startsWith("import android") || it.trimStart().startsWith("import androidx") } }
            .map { it.name }.toList()
        assertTrue("core files import Android: $offenders", offenders.isEmpty())
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.core.*'`
Expected: compilation errors for `Budget`, `QuietHours`, `BlockDecision`, `PinPolicy`.

- [ ] **Step 3: Implement**

`Budget.kt`:

```kotlin
package dev.inkling.core

/** A foreground stint of one app. [endedAt] is null while the app is still in front. */
data class Span(val packageName: String, val startedAt: Long, val endedAt: Long?)

/** Per-app rule. [dailyCapMinutes] of 0 means no cap. */
data class Rule(val packageName: String, val enabled: Boolean, val dailyCapMinutes: Int)

object Budget {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MINUTE_MS = 60L * 1000

    /** Local midnight before [now], as epoch millis, for a fixed zone offset. */
    fun startOfDay(now: Long, zoneOffsetMillis: Long): Long {
        val local = now + zoneOffsetMillis
        val localMidnight = local - Math.floorMod(local, DAY_MS)
        return localMidnight - zoneOffsetMillis
    }

    fun usedMinutesToday(spans: List<Span>, packageName: String, now: Long, zoneOffsetMillis: Long): Int =
        usedMinutesTodayAll(spans, setOf(packageName), now, zoneOffsetMillis)

    fun usedMinutesTodayAll(spans: List<Span>, packages: Set<String>, now: Long, zoneOffsetMillis: Long): Int {
        val dayStart = startOfDay(now, zoneOffsetMillis)
        var total = 0L
        for (s in spans) {
            if (s.packageName !in packages) continue
            val start = maxOf(s.startedAt, dayStart)
            val end = minOf(s.endedAt ?: now, now)
            if (end > start) total += end - start
        }
        return (total / MINUTE_MS).toInt()
    }
}
```

`QuietHours.kt`:

```kotlin
package dev.inkling.core

object QuietHours {
    /** All arguments are minutes since local midnight. A range that ends before it starts crosses midnight. */
    fun isQuiet(minuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean {
        if (startMinute == endMinute) return false
        return if (startMinute < endMinute) {
            minuteOfDay in startMinute until endMinute
        } else {
            minuteOfDay >= startMinute || minuteOfDay < endMinute
        }
    }
}
```

`BlockDecision.kt`:

```kotlin
package dev.inkling.core

enum class Verdict { ALLOW, BLOCK_NOT_ALLOWED, BLOCK_CAPPED, BLOCK_CEILING, BLOCK_QUIET }

object BlockDecision {
    /** Packages that must never be bounced: the shell, keyboards, permission dialogs. */
    val SYSTEM_ALLOWLIST: Set<String> = setOf(
        "android",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin",
        "com.onyx.android.sdk",           // Boox system overlays
    )

    fun decide(
        packageName: String,
        selfPackage: String,
        rules: List<Rule>,
        kidsModeOn: Boolean,
        usedMinutes: Int,
        usedAllMinutes: Int,
        ceilingMinutes: Int,
        quiet: Boolean,
        readPackage: String,
    ): Verdict {
        if (!kidsModeOn) return Verdict.ALLOW
        if (packageName == selfPackage || packageName == readPackage) return Verdict.ALLOW
        if (packageName in SYSTEM_ALLOWLIST) return Verdict.ALLOW
        val rule = rules.firstOrNull { it.packageName == packageName && it.enabled }
            ?: return Verdict.BLOCK_NOT_ALLOWED
        if (quiet) return Verdict.BLOCK_QUIET
        if (rule.dailyCapMinutes > 0 && usedMinutes >= rule.dailyCapMinutes) return Verdict.BLOCK_CAPPED
        if (ceilingMinutes > 0 && usedAllMinutes >= ceilingMinutes) return Verdict.BLOCK_CEILING
        return Verdict.ALLOW
    }
}
```

`PinPolicy.kt`:

```kotlin
package dev.inkling.core

object PinPolicy {
    const val FREE_ATTEMPTS = 3
    private const val BASE_SECONDS = 30
    private const val MAX_SECONDS = 3600

    /** Seconds the PIN pad stays locked after [failures] consecutive wrong entries. */
    fun lockoutSeconds(failures: Int): Int {
        if (failures <= FREE_ATTEMPTS) return 0
        val doublings = failures - FREE_ATTEMPTS - 1
        val seconds = BASE_SECONDS.toLong() shl minOf(doublings, 20)
        return minOf(seconds, MAX_SECONDS.toLong()).toInt()
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.core.*'`
Expected: all tests in the 5 new classes plus `ReadingDiffTest` pass.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/dev/inkling/core android/app/src/test/kotlin/dev/inkling/core
git commit -m "feat(core): budget, quiet hours, block decision, PIN lockout

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 4: Room database and repository

**Files:**
- Create: `android/app/src/main/kotlin/dev/inkling/data/Entities.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/data/Daos.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/data/InklingDb.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/data/Repo.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/InklingApp.kt`
- Modify: `android/app/src/main/AndroidManifest.xml` (add `android:name=".InklingApp"` on `<application>`)
- Test: `android/app/src/test/kotlin/dev/inkling/data/RepoTest.kt`

**Interfaces:**
- Consumes: `Span`, `Rule` from Task 3.
- Produces:
  ```kotlin
  @Entity data class Child(id: Long = 0, name: String, ageYears: Int, createdAt: Long)
  @Entity data class AppRule(id: Long = 0, childId: Long, packageName: String, label: String, enabled: Boolean, dailyCapMinutes: Int)
  @Entity data class UsageEvent(id: Long = 0, childId: Long, packageName: String, startedAt: Long, endedAt: Long?)
  @Entity data class Settings(childId: Long, kidsModeOn: Boolean, deviceCeilingMinutes: Int, warningMinutes: Int,
                              quietStartMinute: Int, quietEndMinute: Int, pinHash: String?, pinFailures: Int, lockoutUntil: Long)
  @Entity data class SpikeRow(id: Long = 0, expected: String, transcript: String, confidence: Float, flagged: String, verdict: String, at: Long)
  class Repo(db: InklingDb) {
      suspend fun ensureChild(): Child                     // creates "Cove", 4 if none
      fun settingsFlow(childId: Long): Flow<Settings>
      suspend fun settings(childId: Long): Settings
      suspend fun saveSettings(s: Settings)
      fun rulesFlow(childId: Long): Flow<List<AppRule>>
      suspend fun rules(childId: Long): List<AppRule>
      suspend fun upsertRule(r: AppRule)
      suspend fun openSpan(childId: Long, pkg: String, now: Long)   // closes any open span first
      suspend fun closeOpenSpan(now: Long)
      suspend fun spansToday(childId: Long, dayStart: Long): List<Span>
      suspend fun addSpike(row: SpikeRow); suspend fun spikes(): List<SpikeRow>
  }
  object Pin { fun hash(pin: String): String }             // SHA-256 hex
  ```
  and `InklingApp.repo` as the process-wide instance.

- [ ] **Step 1: Write the failing test**

`android/app/src/test/kotlin/dev/inkling/data/RepoTest.kt`:

```kotlin
package dev.inkling.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepoTest {
    private lateinit var db: InklingDb
    private lateinit var repo: Repo

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), InklingDb::class.java)
            .allowMainThreadQueries().build()
        repo = Repo(db)
    }
    @After fun tearDown() { db.close() }

    @Test fun ensureChildCreatesOnce() = runBlocking {
        val a = repo.ensureChild()
        val b = repo.ensureChild()
        assertEquals(a.id, b.id)
        assertEquals("Cove", a.name)
        assertEquals(false, repo.settings(a.id).kidsModeOn)
    }

    @Test fun openSpanClosesPrevious() = runBlocking {
        val c = repo.ensureChild()
        repo.openSpan(c.id, "com.chess", 1_000)
        repo.openSpan(c.id, "com.hangman", 5_000)
        val spans = repo.spansToday(c.id, 0)
        assertEquals(2, spans.size)
        assertEquals(5_000L, spans.first { it.packageName == "com.chess" }.endedAt)
        assertNull(spans.first { it.packageName == "com.hangman" }.endedAt)
    }

    @Test fun rulesUpsertByPackage() = runBlocking {
        val c = repo.ensureChild()
        repo.upsertRule(AppRule(childId = c.id, packageName = "com.chess", label = "Chess", enabled = true, dailyCapMinutes = 30))
        repo.upsertRule(AppRule(childId = c.id, packageName = "com.chess", label = "Chess", enabled = false, dailyCapMinutes = 10))
        val rules = repo.rules(c.id)
        assertEquals(1, rules.size)
        assertEquals(10, rules[0].dailyCapMinutes)
    }

    @Test fun pinHashIsStable() {
        assertEquals(Pin.hash("1234"), Pin.hash("1234"))
        assertEquals(64, Pin.hash("1234").length)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.data.RepoTest'`
Expected: compilation errors, `InklingDb`, `Repo`, `Pin` unresolved.

- [ ] **Step 3: Implement entities and DAOs**

`Entities.kt`:

```kotlin
package dev.inkling.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ageYears: Int,
    val createdAt: Long,
)

@Entity(indices = [Index(value = ["childId", "packageName"], unique = true)])
data class AppRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val dailyCapMinutes: Int,
)

@Entity(indices = [Index("childId"), Index("startedAt")])
data class UsageEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val packageName: String,
    val startedAt: Long,
    val endedAt: Long?,
)

@Entity
data class Settings(
    @PrimaryKey val childId: Long,
    val kidsModeOn: Boolean = false,
    val deviceCeilingMinutes: Int = 60,
    val warningMinutes: Int = 2,
    val quietStartMinute: Int = 19 * 60 + 30,
    val quietEndMinute: Int = 7 * 60,
    val pinHash: String? = null,
    val pinFailures: Int = 0,
    val lockoutUntil: Long = 0,
)

@Entity
data class SpikeRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expected: String,
    val transcript: String,
    val confidence: Float,
    val flagged: String,
    val verdict: String,
    val at: Long,
)
```

`Daos.kt`:

```kotlin
package dev.inkling.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM Child ORDER BY id LIMIT 1") suspend fun first(): Child?
    @Insert suspend fun insert(c: Child): Long
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM Settings WHERE childId = :childId") fun flow(childId: Long): Flow<Settings?>
    @Query("SELECT * FROM Settings WHERE childId = :childId") suspend fun get(childId: Long): Settings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(s: Settings)
}

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM AppRule WHERE childId = :childId ORDER BY label") fun flow(childId: Long): Flow<List<AppRule>>
    @Query("SELECT * FROM AppRule WHERE childId = :childId ORDER BY label") suspend fun list(childId: Long): List<AppRule>
    @Query("SELECT * FROM AppRule WHERE childId = :childId AND packageName = :pkg") suspend fun find(childId: Long, pkg: String): AppRule?
    @Insert suspend fun insert(r: AppRule): Long
    @Update suspend fun update(r: AppRule)
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM UsageEvent WHERE endedAt IS NULL") suspend fun open(): List<UsageEvent>
    @Query("SELECT * FROM UsageEvent WHERE childId = :childId AND (endedAt IS NULL OR endedAt >= :dayStart)") suspend fun since(childId: Long, dayStart: Long): List<UsageEvent>
    @Insert suspend fun insert(e: UsageEvent): Long
    @Update suspend fun update(e: UsageEvent)
}

@Dao
interface SpikeDao {
    @Insert suspend fun insert(r: SpikeRow)
    @Query("SELECT * FROM SpikeRow ORDER BY at") suspend fun all(): List<SpikeRow>
}
```

`InklingDb.kt`:

```kotlin
package dev.inkling.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Child::class, AppRule::class, UsageEvent::class, Settings::class, SpikeRow::class], version = 1, exportSchema = false)
abstract class InklingDb : RoomDatabase() {
    abstract fun children(): ChildDao
    abstract fun settings(): SettingsDao
    abstract fun rules(): AppRuleDao
    abstract fun usage(): UsageDao
    abstract fun spikes(): SpikeDao

    companion object {
        fun open(context: Context): InklingDb =
            Room.databaseBuilder(context.applicationContext, InklingDb::class.java, "inkling.db").build()
    }
}
```

- [ ] **Step 4: Implement Repo, Pin, InklingApp**

`Repo.kt`:

```kotlin
package dev.inkling.data

import dev.inkling.core.Span
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import java.security.MessageDigest

object Pin {
    /** SHA-256 hex of the PIN. Not salted: a 4-digit PIN has 10,000 values, salt buys nothing against a curious parent. */
    fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
}

class Repo(private val db: InklingDb) {
    suspend fun ensureChild(): Child {
        db.children().first()?.let { return it }
        val id = db.children().insert(Child(name = "Cove", ageYears = 4, createdAt = System.currentTimeMillis()))
        db.settings().upsert(Settings(childId = id))
        return db.children().first()!!
    }

    fun settingsFlow(childId: Long): Flow<Settings> = db.settings().flow(childId).filterNotNull()
    suspend fun settings(childId: Long): Settings = db.settings().get(childId) ?: Settings(childId).also { db.settings().upsert(it) }
    suspend fun saveSettings(s: Settings) = db.settings().upsert(s)

    fun rulesFlow(childId: Long): Flow<List<AppRule>> = db.rules().flow(childId)
    suspend fun rules(childId: Long): List<AppRule> = db.rules().list(childId)
    suspend fun upsertRule(r: AppRule) {
        val existing = db.rules().find(r.childId, r.packageName)
        if (existing == null) db.rules().insert(r) else db.rules().update(r.copy(id = existing.id))
    }

    suspend fun openSpan(childId: Long, pkg: String, now: Long) {
        closeOpenSpan(now)
        db.usage().insert(UsageEvent(childId = childId, packageName = pkg, startedAt = now, endedAt = null))
    }

    suspend fun closeOpenSpan(now: Long) {
        for (e in db.usage().open()) db.usage().update(e.copy(endedAt = now))
    }

    suspend fun spansToday(childId: Long, dayStart: Long): List<Span> =
        db.usage().since(childId, dayStart).map { Span(it.packageName, it.startedAt, it.endedAt) }

    suspend fun addSpike(row: SpikeRow) = db.spikes().insert(row)
    suspend fun spikes(): List<SpikeRow> = db.spikes().all()
}
```

`InklingApp.kt`:

```kotlin
package dev.inkling

import android.app.Application
import dev.inkling.data.InklingDb
import dev.inkling.data.Repo

class InklingApp : Application() {
    lateinit var repo: Repo
        private set

    override fun onCreate() {
        super.onCreate()
        repo = Repo(InklingDb.open(this))
        instance = this
    }

    companion object {
        lateinit var instance: InklingApp
            private set
    }
}
```

Manifest: add `android:name=".InklingApp"` to the `<application>` element.

- [ ] **Step 5: Run to verify pass**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.data.RepoTest'`
Expected: 4 tests pass. Robolectric downloads an Android SDK jar on first run; allow 3 minutes. If it fails with `SDK 34 not supported`, add `@Config(sdk = [33])` from `org.robolectric.annotation.Config` to the test class.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/kotlin/dev/inkling/data android/app/src/main/kotlin/dev/inkling/InklingApp.kt android/app/src/main/AndroidManifest.xml android/app/src/test/kotlin/dev/inkling/data
git commit -m "feat(data): Room db, repo, single child seed

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 5: Speech spike screen (gate 0)

**Files:**
- Create: `android/app/src/main/kotlin/dev/inkling/spike/Recognizer.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/spike/SpikeScreen.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/spike/SpikeLines.kt`
- Modify: `android/app/src/main/AndroidManifest.xml` (RECORD_AUDIO permission, speech-service query)
- Modify: `android/app/src/main/kotlin/dev/inkling/MainActivity.kt` (temporarily show `SpikeScreen`; Task 8 replaces this with navigation)

**Interfaces:**
- Consumes: `ReadingDiff.score`, `Repo.addSpike`, `Repo.spikes`, `SpikeRow`.
- Produces:
  ```kotlin
  class Recognizer(context: Context) {
      fun listen(onResult: (transcript: String, confidence: Float) -> Unit, onError: (String) -> Unit)
      fun stop(); fun destroy()
  }
  @Composable fun SpikeScreen(repo: Repo)
  object SpikeLines { val lines: List<String> }   // 20 decodable lines
  ```

- [ ] **Step 1: Manifest additions**

Inside `<manifest>`, before `<application>`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<queries>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
</queries>
```

- [ ] **Step 2: The 20 lines**

`SpikeLines.kt`:

```kotlin
package dev.inkling.spike

/** 20 decodable lines, CVC and short vowels only, plus the sight words the, a, in, on, is. */
object SpikeLines {
    val lines: List<String> = listOf(
        "The cat sat on a mat.",
        "The dog can hop.",
        "Sam has a red hat.",
        "The hen is in the pen.",
        "A pig can dig in the mud.",
        "The sun is hot.",
        "Tom ran to the van.",
        "The bug is on the rug.",
        "Dad has a big box.",
        "The pup can nap.",
        "Ben fed the hen.",
        "The fox sat in the den.",
        "Kim has a wet mop.",
        "The kid can jog.",
        "Pat the cat on the leg.",
        "The rat hid in a pot.",
        "Jim got a big net.",
        "The bat is in the bag.",
        "Nan can hum a lot.",
        "The cub sat on a log.",
    )
}
```

- [ ] **Step 3: Recognizer wrapper**

`Recognizer.kt`:

```kotlin
package dev.inkling.spike

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Thin wrapper over Android's on-device SpeechRecognizer.
 * Offline is requested via EXTRA_PREFER_OFFLINE; the device decides. Wi-Fi off during the spike
 * proves whether that request is honored.
 */
class Recognizer(context: Context) {
    private val sr: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null

    val available: Boolean get() = sr != null

    fun listen(onResult: (String, Float) -> Unit, onError: (String) -> Unit) {
        val s = sr ?: return onError("No speech recognizer on this device")
        s.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                val conf = scores?.firstOrNull() ?: -1f   // -1 means the engine gave no score
                onResult(texts.firstOrNull().orEmpty(), conf)
            }
            override fun onError(error: Int) { onError("recognizer error $error") }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }
        s.startListening(intent)
    }

    fun stop() { sr?.stopListening() }
    fun destroy() { sr?.destroy() }
}
```

- [ ] **Step 4: Spike screen**

`SpikeScreen.kt`:

```kotlin
package dev.inkling.spike

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.inkling.core.DiffResult
import dev.inkling.core.ReadingDiff
import dev.inkling.data.Repo
import dev.inkling.data.SpikeRow
import kotlinx.coroutines.launch

/**
 * Gate 0. Shows one line, listens, diffs, and asks the parent for a verdict.
 * Rows go to the SpikeRow table; Task 8 adds a CSV export button on the parent screen.
 */
@Composable
fun SpikeScreen(repo: Repo) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val recognizer = remember { Recognizer(ctx) }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    var index by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Tap Listen, then read the line.") }
    var transcript by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf(-1f) }
    var result by remember { mutableStateOf<DiffResult?>(null) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    val line = SpikeLines.lines[index % SpikeLines.lines.size]

    fun record(verdict: String) {
        val r = result ?: return
        scope.launch {
            repo.addSpike(SpikeRow(expected = line, transcript = transcript, confidence = confidence,
                flagged = r.missed.joinToString(" "), verdict = verdict, at = System.currentTimeMillis()))
            index += 1; result = null; transcript = ""; status = "Saved. Next line."
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Speech test  ${index + 1} of ${SpikeLines.lines.size}", fontSize = 14.sp)
        Text(line, fontSize = 30.sp)
        if (!recognizer.available) Text("This device has no speech recognizer.")
        if (!granted) Button(onClick = { ask.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Allow microphone") }
        Button(enabled = granted && recognizer.available, onClick = {
            status = "Listening…"
            recognizer.listen(
                onResult = { t, c ->
                    transcript = t; confidence = c
                    result = ReadingDiff.score(line, t, if (c < 0) 1f else c)
                    status = "Heard: \"$t\"  conf=${"%.2f".format(c)}"
                },
                onError = { status = it },
            )
        }) { Text("Listen") }
        Text(status)
        result?.let { r ->
            Text(if (r.lowConfidence) "Low confidence, nothing flagged" else "Flagged: ${r.missed.ifEmpty { listOf("none") }.joinToString(" ")}")
            Text("Did he actually read it right?", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { record("correct") }) { Text("Read it right") }
                Button(onClick = { record("wrong") }) { Text("Missed a word") }
                Button(onClick = { record("unclear") }) { Text("Unclear") }
            }
        }
    }
}
```

- [ ] **Step 5: Wire it into MainActivity for now**

Replace `MainActivity.setContent` body with:

```kotlin
setContent { dev.inkling.spike.SpikeScreen((application as InklingApp).repo) }
```

- [ ] **Step 6: Build, install, run on the Boox**

```bash
cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.inkling/.MainActivity
```

Then, with Wi-Fi off on the Boox: tap Listen, read one line yourself, confirm a transcript appears. If the status says `recognizer error 2` (network) with Wi-Fi off, the device's engine does not honor offline: switch to Vosk (add `com.alphacephei:vosk-android:0.3.47`, bundle the `vosk-model-small-en-us-0.15` model in assets) and re-run. Record which engine was used in `docs/plans/progress.md`.

- [ ] **Step 7: Run the actual spike with Cove**

20 lines, normal room, Wi-Fi off. Parent taps the verdict after each. Then pull the numbers:

```bash
adb shell "run-as dev.inkling sqlite3 databases/inkling.db 'select verdict, flagged, transcript, confidence from SpikeRow'"
```

If `sqlite3` is missing on the device, Task 8 adds a CSV export; use that. False-positive rate = rows with verdict `correct` and non-empty `flagged`, divided by rows with verdict `correct`. Write the rate, engine, and date into `docs/plans/progress.md` under "Spike result".

- [ ] **Step 8: Commit**

```bash
git add android/app/src/main/kotlin/dev/inkling/spike android/app/src/main/AndroidManifest.xml android/app/src/main/kotlin/dev/inkling/MainActivity.kt docs/plans/progress.md
git commit -m "feat(spike): on-device speech recognition spike screen with parent verdicts

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 6: Kid home and Done for today

**Files:**
- Create: `android/app/src/main/kotlin/dev/inkling/ui/Theme.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/ui/KidHome.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/ui/DoneScreen.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/ui/HomeViewModel.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/apps/InstalledApps.kt`
- Test: `android/app/src/test/kotlin/dev/inkling/ui/HomeStateTest.kt`

**Interfaces:**
- Consumes: `Repo`, `Budget`, `QuietHours`, `BlockDecision`, `AppRule`, `Settings`.
- Produces:
  ```kotlin
  object InklingTheme { val Paper: Color; val Ink: Color; val Ink2: Color; val Ink3: Color; val Paper2: Color; val Moss: Color; val Ochre: Color; val Rust: Color; val Teal: Color; val Andika: FontFamily }
  @Composable fun InklingTheme(content: @Composable () -> Unit)
  data class Tile(val packageName: String, val label: String, val fraction: Float, val done: Boolean)
  data class HomeState(val childName: String, val booksToday: Int, val tiles: List<Tile>)
  fun buildHomeState(childName: String, rules: List<AppRule>, spans: List<Span>, settings: Settings, now: Long, zoneOffsetMillis: Long, minuteOfDay: Int): HomeState
  class HomeViewModel(repo: Repo) : ViewModel() { val state: StateFlow<HomeState>; fun refresh() }
  @Composable fun KidHome(state: HomeState, onOpen: (Tile) -> Unit, onRead: () -> Unit, onGearLongPress: () -> Unit)
  @Composable fun DoneScreen(appLabel: String, onPickBook: () -> Unit, onBack: () -> Unit)
  object InstalledApps { fun launchable(pm: PackageManager, selfPackage: String): List<Pair<String, String>> }  // (package, label)
  ```

- [ ] **Step 1: Write the failing test for the pure state builder**

`HomeStateTest.kt`:

```kotlin
package dev.inkling.ui

import dev.inkling.core.Span
import dev.inkling.data.AppRule
import dev.inkling.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {
    private val minute = 60_000L
    private val now = 1_000_000_000_000L
    private val rules = listOf(
        AppRule(childId = 1, packageName = "com.chess", label = "Chess", enabled = true, dailyCapMinutes = 30),
        AppRule(childId = 1, packageName = "com.off", label = "Off", enabled = false, dailyCapMinutes = 0),
        AppRule(childId = 1, packageName = "com.hoopla", label = "Hoopla", enabled = true, dailyCapMinutes = 0),
    )
    private val settings = Settings(childId = 1, kidsModeOn = true, deviceCeilingMinutes = 60)

    @Test fun onlyEnabledAppsBecomeTiles() {
        val s = buildHomeState("Cove", rules, emptyList(), settings, now, 0, 12 * 60)
        assertEquals(listOf("Chess", "Hoopla"), s.tiles.map { it.label })
    }

    @Test fun fractionReflectsUsage() {
        val spans = listOf(Span("com.chess", now - 15 * minute, now))
        val s = buildHomeState("Cove", rules, spans, settings, now, 0, 12 * 60)
        assertEquals(0.5f, s.tiles.first { it.label == "Chess" }.fraction, 0.01f)
        assertFalse(s.tiles.first { it.label == "Chess" }.done)
    }

    @Test fun cappedAppIsDone() {
        val spans = listOf(Span("com.chess", now - 30 * minute, now))
        val s = buildHomeState("Cove", rules, spans, settings, now, 0, 12 * 60)
        assertTrue(s.tiles.first { it.label == "Chess" }.done)
    }

    @Test fun noCapAppShowsZeroFractionAndNeverDone() {
        val spans = listOf(Span("com.hoopla", now - 500 * minute, now))
        val s = buildHomeState("Cove", rules, spans, settings.copy(deviceCeilingMinutes = 0), now, 0, 12 * 60)
        val t = s.tiles.first { it.label == "Hoopla" }
        assertEquals(0f, t.fraction, 0.01f); assertFalse(t.done)
    }

    @Test fun quietHoursMarkEverythingDone() {
        val s = buildHomeState("Cove", rules, emptyList(), settings, now, 0, 21 * 60)
        assertTrue(s.tiles.all { it.done })
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.ui.HomeStateTest'`
Expected: `Unresolved reference: buildHomeState`.

- [ ] **Step 3: Theme**

`Theme.kt`:

```kotlin
package dev.inkling.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.inkling.R

/** Kaleido 3 palette from the prototype. Muted on purpose; color e-ink is dim. */
object InklingColors {
    val Paper = Color(0xFFEDEBE6)
    val Paper2 = Color(0xFFDCD9D2)
    val Ink = Color(0xFF17160F)
    val Ink2 = Color(0xFF5A574F)
    val Ink3 = Color(0xFF9A968C)
    val Teal = Color(0xFF7AA59F)
    val Ochre = Color(0xFFC9A66B)
    val Rust = Color(0xFFB8776A)
    val Moss = Color(0xFF8FA574)
}

val Andika = FontFamily(
    Font(R.font.andika_regular, FontWeight.Normal),
    Font(R.font.andika_bold, FontWeight.Bold),
)

@Composable
fun InklingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = InklingColors.Ink,
            onPrimary = InklingColors.Paper,
            background = InklingColors.Paper,
            onBackground = InklingColors.Ink,
            surface = InklingColors.Paper,
            onSurface = InklingColors.Ink,
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontFamily = Andika, fontSize = 18.sp),
            titleLarge = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 30.sp),
        ),
        content = content,
    )
}
```

- [ ] **Step 4: State builder and view model**

`HomeViewModel.kt`:

```kotlin
package dev.inkling.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.inkling.core.BlockDecision
import dev.inkling.core.Budget
import dev.inkling.core.QuietHours
import dev.inkling.core.Rule
import dev.inkling.core.Span
import dev.inkling.core.Verdict
import dev.inkling.data.AppRule
import dev.inkling.data.Repo
import dev.inkling.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class Tile(val packageName: String, val label: String, val fraction: Float, val done: Boolean)
data class HomeState(val childName: String = "", val booksToday: Int = 0, val tiles: List<Tile> = emptyList())

/** Pure. Turns rules, today's spans, and settings into what the kid sees. */
fun buildHomeState(
    childName: String, rules: List<AppRule>, spans: List<Span>, settings: Settings,
    now: Long, zoneOffsetMillis: Long, minuteOfDay: Int,
): HomeState {
    val enabled = rules.filter { it.enabled }
    val coreRules = enabled.map { Rule(it.packageName, it.enabled, it.dailyCapMinutes) }
    val quiet = QuietHours.isQuiet(minuteOfDay, settings.quietStartMinute, settings.quietEndMinute)
    val usedAll = Budget.usedMinutesTodayAll(spans, enabled.map { it.packageName }.toSet(), now, zoneOffsetMillis)
    val tiles = enabled.map { r ->
        val used = Budget.usedMinutesToday(spans, r.packageName, now, zoneOffsetMillis)
        val verdict = BlockDecision.decide(
            r.packageName, selfPackage = "dev.inkling", rules = coreRules, kidsModeOn = true,
            usedMinutes = used, usedAllMinutes = usedAll, ceilingMinutes = settings.deviceCeilingMinutes,
            quiet = quiet, readPackage = "dev.inkling",
        )
        val fraction = if (r.dailyCapMinutes > 0) (used.toFloat() / r.dailyCapMinutes).coerceIn(0f, 1f) else 0f
        Tile(r.packageName, r.label, fraction, done = verdict != Verdict.ALLOW)
    }
    return HomeState(childName, booksToday = 0, tiles = tiles)
}

class HomeViewModel(private val repo: Repo) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    fun refresh() = viewModelScope.launch {
        val child = repo.ensureChild()
        val settings = repo.settings(child.id)
        val rules = repo.rules(child.id)
        val now = System.currentTimeMillis()
        val tz = TimeZone.getDefault().getOffset(now).toLong()
        val cal = Calendar.getInstance()
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val spans = repo.spansToday(child.id, Budget.startOfDay(now, tz))
        _state.value = buildHomeState(child.name, rules, spans, settings, now, tz, minuteOfDay)
    }
}
```

- [ ] **Step 5: Run the state test**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.ui.HomeStateTest'`
Expected: 5 pass.

- [ ] **Step 6: Installed apps helper**

`InstalledApps.kt`:

```kotlin
package dev.inkling.apps

import android.content.Intent
import android.content.pm.PackageManager

object InstalledApps {
    /** Every app with a launcher icon, except Inkling itself. Sorted by label. */
    fun launchable(pm: PackageManager, selfPackage: String): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != selfPackage }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }
}
```

- [ ] **Step 7: Kid home and Done screens**

`KidHome.kt`:

```kotlin
package dev.inkling.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KidHome(state: HomeState, onOpen: (Tile) -> Unit, onRead: () -> Unit, onGearLongPress: () -> Unit) {
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(1.dp))
            Box(
                Modifier.size(28.dp).border(1.5.dp, InklingColors.Ink3, CircleShape)
                    .combinedClickable(onClick = {}, onLongClick = onGearLongPress),
                contentAlignment = Alignment.Center,
            ) { Text("⚙", fontSize = 13.sp, color = InklingColors.Ink2) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Hi, ${state.childName}.", fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = InklingColors.Ink)
        Text("${state.booksToday} books today.", fontFamily = Andika, fontSize = 16.sp, color = InklingColors.Ink2)
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(span = { GridItemSpan(2) }) {
                TileCard(label = "Read", fraction = null, done = false, tall = true, onClick = onRead)
            }
            items(state.tiles, key = { it.packageName }) { t ->
                TileCard(label = t.label, fraction = t.fraction, done = t.done, tall = false, onClick = { onOpen(t) })
            }
        }
    }
}

@Composable
private fun TileCard(label: String, fraction: Float?, done: Boolean, tall: Boolean, onClick: () -> Unit) {
    val ink = if (done) InklingColors.Ink3 else InklingColors.Ink
    Column(
        Modifier.fillMaxWidth().height(if (tall) 132.dp else 118.dp)
            .border(2.dp, ink, RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ink)
        if (done) Text("Done for today", fontFamily = Andika, fontSize = 12.sp, color = ink)
        if (fraction != null) {
            Box(Modifier.fillMaxWidth().height(7.dp).background(InklingColors.Paper2, RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxWidth(if (done) 1f else fraction).height(7.dp).background(ink, RoundedCornerShape(4.dp)))
            }
        }
    }
}
```

`DoneScreen.kt`:

```kotlin
package dev.inkling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DoneScreen(appLabel: String, onPickBook: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(InklingColors.Paper).padding(32.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$appLabel is done\nfor today.", fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 28.sp, textAlign = TextAlign.Center, color = InklingColors.Ink)
        Text("It comes back tomorrow. Want to read one more book?", fontFamily = Andika, fontSize = 17.sp, textAlign = TextAlign.Center, color = InklingColors.Ink2, modifier = Modifier.padding(top = 14.dp, bottom = 24.dp))
        BigButton("Pick a book", filled = true, onClick = onPickBook)
        BigButton("Back", filled = false, onClick = onBack)
    }
}

@Composable
fun BigButton(label: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth(0.7f).padding(vertical = 5.dp)
            .background(if (filled) InklingColors.Ink else InklingColors.Paper, RoundedCornerShape(10.dp))
            .border(2.dp, InklingColors.Ink, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (filled) InklingColors.Paper else InklingColors.Ink) }
}
```

- [ ] **Step 8: Build**

Run: `cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: green. Screens are wired in Task 8.

- [ ] **Step 9: Commit**

```bash
git add android/app/src/main/kotlin/dev/inkling/ui android/app/src/main/kotlin/dev/inkling/apps android/app/src/test/kotlin/dev/inkling/ui
git commit -m "feat(ui): kid home, done-for-today, theme, installed apps helper

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 7: Kids mode accessibility service (block, log, cap, warn)

**Files:**
- Create: `android/app/src/main/kotlin/dev/inkling/service/KidsModeService.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/service/ServiceState.kt`
- Create: `android/app/src/main/res/xml/kids_mode_service.xml`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Test: `android/app/src/test/kotlin/dev/inkling/service/ServiceStateTest.kt`

**Interfaces:**
- Consumes: `BlockDecision`, `Budget`, `QuietHours`, `Repo`.
- Produces:
  ```kotlin
  data class Snapshot(val kidsModeOn: Boolean, val rules: List<Rule>, val ceiling: Int, val warning: Int, val quietStart: Int, val quietEnd: Int)
  sealed class Action { object None; object SendHome; data class Warn(val minutesLeft: Int) }
  object ServiceState {
      fun onForeground(pkg: String, self: String, snap: Snapshot, spans: List<Span>, now: Long, zone: Long, minuteOfDay: Int, warnedFor: Set<String>): Pair<Action, Verdict>
  }
  class KidsModeService : AccessibilityService()   // declared in manifest
  ```

- [ ] **Step 1: Write the failing test**

`ServiceStateTest.kt`:

```kotlin
package dev.inkling.service

import dev.inkling.core.Rule
import dev.inkling.core.Span
import dev.inkling.core.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceStateTest {
    private val minute = 60_000L
    private val now = 1_000_000_000_000L
    private val snap = Snapshot(kidsModeOn = true, rules = listOf(Rule("com.chess", true, 30)), ceiling = 60, warning = 2, quietStart = 19 * 60 + 30, quietEnd = 7 * 60)

    @Test fun allowedAppUnderCapDoesNothing() {
        val (a, v) = ServiceState.onForeground("com.chess", "dev.inkling", snap, emptyList(), now, 0, 12 * 60, emptySet())
        assertEquals(Action.None, a); assertEquals(Verdict.ALLOW, v)
    }

    @Test fun notAllowedAppIsSentHome() {
        val (a, v) = ServiceState.onForeground("com.android.settings", "dev.inkling", snap, emptyList(), now, 0, 12 * 60, emptySet())
        assertEquals(Action.SendHome, a); assertEquals(Verdict.BLOCK_NOT_ALLOWED, v)
    }

    @Test fun withinWarningWindowWarnsOnce() {
        val spans = listOf(Span("com.chess", now - 28 * minute, null))
        val (a, _) = ServiceState.onForeground("com.chess", "dev.inkling", snap, spans, now, 0, 12 * 60, emptySet())
        assertEquals(Action.Warn(2), a)
        val (again, _) = ServiceState.onForeground("com.chess", "dev.inkling", snap, spans, now, 0, 12 * 60, setOf("com.chess"))
        assertEquals(Action.None, again)
    }

    @Test fun atCapIsSentHome() {
        val spans = listOf(Span("com.chess", now - 30 * minute, null))
        val (a, v) = ServiceState.onForeground("com.chess", "dev.inkling", snap, spans, now, 0, 12 * 60, setOf("com.chess"))
        assertEquals(Action.SendHome, a); assertEquals(Verdict.BLOCK_CAPPED, v)
    }

    @Test fun kidsModeOffNeverActs() {
        val (a, _) = ServiceState.onForeground("com.android.settings", "dev.inkling", snap.copy(kidsModeOn = false), emptyList(), now, 0, 12 * 60, emptySet())
        assertEquals(Action.None, a)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.service.ServiceStateTest'`
Expected: unresolved references.

- [ ] **Step 3: Pure decision**

`ServiceState.kt`:

```kotlin
package dev.inkling.service

import dev.inkling.core.BlockDecision
import dev.inkling.core.Budget
import dev.inkling.core.QuietHours
import dev.inkling.core.Rule
import dev.inkling.core.Span
import dev.inkling.core.Verdict

data class Snapshot(val kidsModeOn: Boolean, val rules: List<Rule>, val ceiling: Int, val warning: Int, val quietStart: Int, val quietEnd: Int)

sealed class Action {
    data object None : Action()
    data object SendHome : Action()
    data class Warn(val minutesLeft: Int) : Action()
}

object ServiceState {
    /** Decides what the service does when [pkg] comes to the front. Pure, so it's testable. */
    fun onForeground(
        pkg: String, self: String, snap: Snapshot, spans: List<Span>,
        now: Long, zone: Long, minuteOfDay: Int, warnedFor: Set<String>,
    ): Pair<Action, Verdict> {
        val enabledPkgs = snap.rules.filter { it.enabled }.map { it.packageName }.toSet()
        val used = Budget.usedMinutesToday(spans, pkg, now, zone)
        val usedAll = Budget.usedMinutesTodayAll(spans, enabledPkgs, now, zone)
        val quiet = QuietHours.isQuiet(minuteOfDay, snap.quietStart, snap.quietEnd)
        val verdict = BlockDecision.decide(pkg, self, snap.rules, snap.kidsModeOn, used, usedAll, snap.ceiling, quiet, readPackage = self)
        if (verdict != Verdict.ALLOW) return Action.SendHome to verdict
        val rule = snap.rules.firstOrNull { it.packageName == pkg }
        if (rule != null && rule.dailyCapMinutes > 0 && pkg !in warnedFor) {
            val left = rule.dailyCapMinutes - used
            if (left in 1..snap.warning) return Action.Warn(left) to verdict
        }
        return Action.None to verdict
    }
}
```

- [ ] **Step 4: Run test**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.service.ServiceStateTest'`
Expected: 5 pass.

- [ ] **Step 5: The service**

`res/xml/kids_mode_service.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="false"
    android:description="@string/service_description"
    android:notificationTimeout="100" />
```

Add to `strings.xml`:

```xml
<string name="service_description">Keeps the child inside the apps a parent allowed, and counts their time. Reads only which app is in front.</string>
```

`KidsModeService.kt`:

```kotlin
package dev.inkling.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import dev.inkling.InklingApp
import dev.inkling.MainActivity
import dev.inkling.core.Budget
import dev.inkling.core.Rule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

/**
 * Watches which app is in front. Blocks, logs, caps, warns.
 * Reads no window content: the service XML sets canRetrieveWindowContent=false.
 */
class KidsModeService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastPkg: String? = null
    private val warnedFor = mutableSetOf<String>()
    private var warnedDay: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == lastPkg) return
        lastPkg = pkg
        scope.launch { handle(pkg) }
    }

    private suspend fun handle(pkg: String) {
        val repo = InklingApp.instance.repo
        val child = repo.ensureChild()
        val s = repo.settings(child.id)
        val rules = repo.rules(child.id).map { Rule(it.packageName, it.enabled, it.dailyCapMinutes) }
        val now = System.currentTimeMillis()
        val zone = TimeZone.getDefault().getOffset(now).toLong()
        val dayStart = Budget.startOfDay(now, zone)
        if (dayStart != warnedDay) { warnedFor.clear(); warnedDay = dayStart }
        val cal = Calendar.getInstance()
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        // Log first, so the span that just ended is counted.
        val tracked = rules.any { it.packageName == pkg && it.enabled }
        if (tracked) repo.openSpan(child.id, pkg, now) else repo.closeOpenSpan(now)

        val spans = repo.spansToday(child.id, dayStart)
        val snap = Snapshot(s.kidsModeOn, rules, s.deviceCeilingMinutes, s.warningMinutes, s.quietStartMinute, s.quietEndMinute)
        val (action, _) = ServiceState.onForeground(pkg, packageName, snap, spans, now, zone, minuteOfDay, warnedFor)
        when (action) {
            is Action.SendHome -> withContext(Dispatchers.Main) { sendHome() }
            is Action.Warn -> { warnedFor += pkg; withContext(Dispatchers.Main) { showWarning(action.minutesLeft) } }
            Action.None -> if (tracked) scheduleCapCheck(pkg)
        }
    }

    /** Re-evaluates once a minute while a tracked app stays in front, so caps fire mid-session. */
    private fun scheduleCapCheck(pkg: String) {
        scope.launch {
            delay(60_000)
            if (lastPkg == pkg) { lastPkg = null; handle(pkg) }
        }
    }

    private fun sendHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun showWarning(minutesLeft: Int) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = if (minutesLeft == 1) "1 minute left" else "$minutesLeft minutes left"
            textSize = 28f
            setPadding(48, 32, 48, 32)
            setBackgroundColor(0xFF17160F.toInt())
            setTextColor(0xFFEDEBE6.toInt())
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.OPAQUE,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 80 }
        wm.addView(view, lp)
        scope.launch { delay(3_000); withContext(Dispatchers.Main) { runCatching { wm.removeView(view) } } }
    }

    override fun onInterrupt() {}
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
```

Manifest, inside `<application>`:

```xml
<service
    android:name=".service.KidsModeService"
    android:exported="false"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data android:name="android.accessibilityservice" android:resource="@xml/kids_mode_service" />
</service>
```

- [ ] **Step 6: Build and commit**

Run: `cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: green.

```bash
git add android/app/src/main/kotlin/dev/inkling/service android/app/src/main/res android/app/src/main/AndroidManifest.xml android/app/src/test/kotlin/dev/inkling/service
git commit -m "feat(service): accessibility service blocks, logs, caps, and warns

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 8: PIN, parent screen, navigation, HOME registration, boot check

**Files:**
- Create: `android/app/src/main/kotlin/dev/inkling/ui/PinScreen.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/ui/ParentScreen.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/ui/ParentViewModel.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/ui/Nav.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/service/BootReceiver.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/service/SetupCheck.kt`
- Modify: `android/app/src/main/kotlin/dev/inkling/MainActivity.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Test: `android/app/src/test/kotlin/dev/inkling/ui/ParentStateTest.kt`

**Interfaces:**
- Consumes: everything above.
- Produces:
  ```kotlin
  data class AppRow(val packageName: String, val label: String, val enabled: Boolean, val cap: Int)
  data class TodayRow(val label: String, val minutes: Int)
  data class ParentState(val settings: Settings?, val apps: List<AppRow>, val today: List<TodayRow>, val totalMinutes: Int, val setupProblems: List<String>)
  fun buildToday(rules: List<AppRule>, spans: List<Span>, now: Long, zone: Long): Pair<List<TodayRow>, Int>
  class ParentViewModel(repo: Repo, pm: PackageManager, self: String) : ViewModel()
  object SetupCheck { fun problems(context: Context): List<String> }   // "Inkling is not the Home app", "Accessibility service is off"
  ```

- [ ] **Step 1: Failing test for the today aggregation**

`ParentStateTest.kt`:

```kotlin
package dev.inkling.ui

import dev.inkling.core.Span
import dev.inkling.data.AppRule
import org.junit.Assert.assertEquals
import org.junit.Test

class ParentStateTest {
    private val minute = 60_000L
    private val now = 1_000_000_000_000L
    private val rules = listOf(
        AppRule(childId = 1, packageName = "com.chess", label = "Chess", enabled = true, dailyCapMinutes = 30),
        AppRule(childId = 1, packageName = "com.hangman", label = "Hangman", enabled = true, dailyCapMinutes = 15),
    )

    @Test fun rowsSortedByMinutesDescWithTotal() {
        val spans = listOf(
            Span("com.chess", now - 12 * minute, now),
            Span("com.hangman", now - 40 * minute, now - 20 * minute),
        )
        val (rows, total) = buildToday(rules, spans, now, 0)
        assertEquals(listOf("Hangman" to 20, "Chess" to 12), rows.map { it.label to it.minutes })
        assertEquals(32, total)
    }

    @Test fun zeroMinuteAppsStillListed() {
        val (rows, total) = buildToday(rules, emptyList(), now, 0)
        assertEquals(2, rows.size); assertEquals(0, total)
    }
}
```

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.ui.ParentStateTest'`
Expected: unresolved `buildToday`.

- [ ] **Step 2: Parent view model**

`ParentViewModel.kt`:

```kotlin
package dev.inkling.ui

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.inkling.apps.InstalledApps
import dev.inkling.core.Budget
import dev.inkling.core.PinPolicy
import dev.inkling.core.Span
import dev.inkling.data.AppRule
import dev.inkling.data.Pin
import dev.inkling.data.Repo
import dev.inkling.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone

data class AppRow(val packageName: String, val label: String, val enabled: Boolean, val cap: Int)
data class TodayRow(val label: String, val minutes: Int)
data class ParentState(
    val settings: Settings? = null,
    val apps: List<AppRow> = emptyList(),
    val today: List<TodayRow> = emptyList(),
    val totalMinutes: Int = 0,
    val setupProblems: List<String> = emptyList(),
)

fun buildToday(rules: List<AppRule>, spans: List<Span>, now: Long, zone: Long): Pair<List<TodayRow>, Int> {
    val rows = rules.filter { it.enabled }
        .map { TodayRow(it.label, Budget.usedMinutesToday(spans, it.packageName, now, zone)) }
        .sortedByDescending { it.minutes }
    return rows to rows.sumOf { it.minutes }
}

class ParentViewModel(private val repo: Repo, private val pm: PackageManager, private val self: String) : ViewModel() {
    private val _state = MutableStateFlow(ParentState())
    val state: StateFlow<ParentState> = _state
    var setupProblems: () -> List<String> = { emptyList() }

    fun refresh() = viewModelScope.launch {
        val child = repo.ensureChild()
        val settings = repo.settings(child.id)
        val rules = repo.rules(child.id)
        val installed = InstalledApps.launchable(pm, self)
        val apps = installed.map { (pkg, label) ->
            val r = rules.firstOrNull { it.packageName == pkg }
            AppRow(pkg, label, r?.enabled ?: false, r?.dailyCapMinutes ?: 0)
        }
        val now = System.currentTimeMillis()
        val zone = TimeZone.getDefault().getOffset(now).toLong()
        val (today, total) = buildToday(rules, repo.spansToday(child.id, Budget.startOfDay(now, zone)), now, zone)
        _state.value = ParentState(settings, apps, today, total, setupProblems())
    }

    fun setKidsMode(on: Boolean) = update { it.copy(kidsModeOn = on) }
    fun setCeiling(minutes: Int) = update { it.copy(deviceCeilingMinutes = minutes.coerceIn(0, 600)) }
    /** Empty [pin] clears the PIN so the next PIN screen sets a new one. */
    fun setPin(pin: String) = update { it.copy(pinHash = if (pin.isEmpty()) null else Pin.hash(pin), pinFailures = 0, lockoutUntil = 0) }

    fun setApp(row: AppRow, enabled: Boolean, cap: Int) = viewModelScope.launch {
        val child = repo.ensureChild()
        repo.upsertRule(AppRule(childId = child.id, packageName = row.packageName, label = row.label, enabled = enabled, dailyCapMinutes = cap.coerceIn(0, 600)))
        refresh()
    }

    /** Returns true when the PIN is right. Wrong entries count toward lockout. */
    suspend fun tryPin(pin: String, now: Long): Boolean {
        val child = repo.ensureChild()
        val s = repo.settings(child.id)
        if (now < s.lockoutUntil) return false
        if (s.pinHash == null || s.pinHash == Pin.hash(pin)) {
            repo.saveSettings(s.copy(pinFailures = 0, lockoutUntil = 0)); return true
        }
        val failures = s.pinFailures + 1
        val lock = PinPolicy.lockoutSeconds(failures) * 1000L
        repo.saveSettings(s.copy(pinFailures = failures, lockoutUntil = if (lock > 0) now + lock else 0))
        return false
    }

    suspend fun lockoutRemainingSeconds(now: Long): Int {
        val s = repo.settings(repo.ensureChild().id)
        return ((s.lockoutUntil - now) / 1000).coerceAtLeast(0).toInt()
    }

    suspend fun spikeCsv(): String = buildString {
        append("at,expected,transcript,confidence,flagged,verdict\n")
        for (r in repo.spikes()) append("${r.at},\"${r.expected}\",\"${r.transcript}\",${r.confidence},\"${r.flagged}\",${r.verdict}\n")
    }

    private fun update(f: (Settings) -> Settings) = viewModelScope.launch {
        val child = repo.ensureChild()
        repo.saveSettings(f(repo.settings(child.id)))
        refresh()
    }
}
```

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests 'dev.inkling.ui.ParentStateTest'`
Expected: 2 pass.

- [ ] **Step 3: PIN screen**

`PinScreen.kt`:

```kotlin
package dev.inkling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * @param hasPin false on first use: the entered 4 digits become the PIN.
 */
@Composable
fun PinScreen(hasPin: Boolean, tryPin: suspend (String) -> Boolean, lockoutSeconds: suspend () -> Int, onSetPin: (String) -> Unit, onUnlocked: () -> Unit, onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var locked by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf(if (hasPin) "Enter parent PIN" else "Choose a 4-digit parent PIN") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { locked = lockoutSeconds() }

    fun submit(p: String) = scope.launch {
        if (!hasPin) { onSetPin(p); onUnlocked(); return@launch }
        if (tryPin(p)) onUnlocked() else {
            locked = lockoutSeconds()
            message = if (locked > 0) "Try again in $locked seconds" else "That's not it"
            pin = ""
        }
    }

    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("‹ Back", color = InklingColors.Ink2, modifier = Modifier.clickable(onClick = onBack).align(Alignment.Start))
        Spacer(Modifier.height(24.dp))
        Text(message, color = InklingColors.Ink2, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { i ->
                Box(Modifier.size(16.dp).border(2.dp, InklingColors.Ink, CircleShape).background(if (i < pin.length) InklingColors.Ink else InklingColors.Paper, CircleShape))
            }
        }
        Spacer(Modifier.height(24.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier.size(72.dp, 60.dp)
                            .border(1.5.dp, if (k.isEmpty()) InklingColors.Paper else InklingColors.Ink2, RoundedCornerShape(8.dp))
                            .clickable(enabled = k.isNotEmpty() && locked == 0) {
                                if (k == "⌫") pin = pin.dropLast(1)
                                else if (pin.length < 4) { pin += k; if (pin.length == 4) submit(pin) }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(k, fontSize = 22.sp, color = InklingColors.Ink) }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Parent screen**

`ParentScreen.kt`:

```kotlin
package dev.inkling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ParentScreen(
    state: ParentState,
    onKidsMode: (Boolean) -> Unit,
    onApp: (AppRow, Boolean, Int) -> Unit,
    onCeiling: (Int) -> Unit,
    onOpenBooxHome: () -> Unit,
    onFixSetup: (String) -> Unit,
    onSpeechTest: () -> Unit,
    onExportSpike: () -> Unit,
    onChangePin: () -> Unit,
    onLock: () -> Unit,
) {
    var tab by remember { mutableStateOf("Today") }
    val s = state.settings ?: return
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹ Lock", color = InklingColors.Ink2, modifier = Modifier.clickable(onClick = onLock))
            Text("Parent", color = InklingColors.Ink2)
            Spacer(Modifier.width(1.dp))
        }
        Spacer(Modifier.height(12.dp))
        state.setupProblems.forEach { p ->
            Row(Modifier.fillMaxWidth().background(InklingColors.Rust, RoundedCornerShape(8.dp)).clickable { onFixSetup(p) }.padding(10.dp)) {
                Text("$p. Tap to fix.", color = InklingColors.Ink)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(
            Modifier.fillMaxWidth().border(2.dp, InklingColors.Ink, RoundedCornerShape(10.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Kids mode", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(if (s.kidsModeOn) "On. Only the apps switched on below can open." else "Off. Device returns to the normal Boox home screen for you.", fontSize = 12.sp, color = InklingColors.Ink2)
            }
            Switch(checked = s.kidsModeOn, onCheckedChange = onKidsMode)
        }
        if (!s.kidsModeOn) {
            Spacer(Modifier.height(8.dp))
            BigButton("Open Boox home", filled = false, onClick = onOpenBooxHome)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Today", "Apps", "Rules").forEach { t ->
                Box(
                    Modifier.border(1.5.dp, if (tab == t) InklingColors.Ink else InklingColors.Ink3, RoundedCornerShape(14.dp))
                        .background(if (tab == t) InklingColors.Paper2 else InklingColors.Paper, RoundedCornerShape(14.dp))
                        .clickable { tab = t }.padding(horizontal = 10.dp, vertical = 5.dp),
                ) { Text(t, fontSize = 12.sp, color = if (tab == t) InklingColors.Ink else InklingColors.Ink2) }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            "Today" -> TodayTab(state)
            "Apps" -> AppsTab(state, onApp)
            "Rules" -> RulesTab(s.deviceCeilingMinutes, onCeiling, onChangePin, onSpeechTest, onExportSpike)
        }
    }
}

@Composable
private fun TodayTab(state: ParentState) {
    Column {
        KeyValue("Screen time today", "${state.totalMinutes} min")
        Spacer(Modifier.height(10.dp))
        Text("BY APP", fontSize = 11.sp, color = InklingColors.Ink2)
        val max = (state.today.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
        state.today.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(r.label, Modifier.width(92.dp), fontSize = 13.sp)
                Box(Modifier.weight(1f).height(10.dp).background(InklingColors.Paper2, RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(r.minutes.toFloat() / max).height(10.dp).background(InklingColors.Ink, RoundedCornerShape(5.dp)))
                }
                Text("${r.minutes}", Modifier.width(44.dp), fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

@Composable
private fun AppsTab(state: ParentState, onApp: (AppRow, Boolean, Int) -> Unit) {
    LazyColumn {
        items(state.apps, key = { it.packageName }) { a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(a.label, Modifier.weight(1f), fontSize = 13.sp, color = if (a.enabled) InklingColors.Ink else InklingColors.Ink3)
                Stepper(value = a.cap, enabled = a.enabled, onChange = { onApp(a, a.enabled, it) })
                Spacer(Modifier.width(10.dp))
                Switch(checked = a.enabled, onCheckedChange = { onApp(a, it, a.cap) })
            }
        }
        item { Text("Switch on = tile on the home screen. Minutes = daily cap, 0 = no cap.", fontSize = 11.sp, color = InklingColors.Ink3, modifier = Modifier.padding(top = 6.dp)) }
    }
}

@Composable
private fun RulesTab(ceiling: Int, onCeiling: (Int) -> Unit, onChangePin: () -> Unit, onSpeechTest: () -> Unit, onExportSpike: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Whole-device daily ceiling", fontSize = 13.sp)
            Stepper(value = ceiling, enabled = true, onChange = onCeiling)
        }
        KeyValue("Warning before a cap", "2 min")
        KeyValue("Games unlock after", "off")
        KeyValue("Quiet hours", "7:30 pm – 7:00 am")
        KeyValue("Children", "Cove")
        Spacer(Modifier.height(12.dp))
        BigButton("Change PIN", filled = false, onClick = onChangePin)
        BigButton("Speech test", filled = false, onClick = onSpeechTest)
        BigButton("Export speech test CSV", filled = false, onClick = onExportSpike)
    }
}

@Composable
private fun KeyValue(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, fontSize = 13.sp, color = InklingColors.Ink2)
        Text(v, fontSize = 13.sp)
    }
}

@Composable
private fun Stepper(value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val ink = if (enabled) InklingColors.Ink else InklingColors.Ink3
    Row(Modifier.border(1.5.dp, ink, RoundedCornerShape(6.dp)), verticalAlignment = Alignment.CenterVertically) {
        Text("−", Modifier.clickable(enabled) { onChange(value - 5) }.background(InklingColors.Paper2).padding(horizontal = 10.dp, vertical = 3.dp), color = ink)
        Text("$value", Modifier.width(44.dp).padding(vertical = 3.dp), fontSize = 12.sp, color = ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("+", Modifier.clickable(enabled) { onChange(value + 5) }.background(InklingColors.Paper2).padding(horizontal = 10.dp, vertical = 3.dp), color = ink)
    }
}
```

- [ ] **Step 5: Setup check and boot receiver**

`SetupCheck.kt`:

```kotlin
package dev.inkling.service

import android.content.Context
import android.content.Intent
import android.provider.Settings

object SetupCheck {
    const val NOT_HOME = "Inkling is not the Home app"
    const val SERVICE_OFF = "Accessibility service is off"

    fun problems(context: Context): List<String> {
        val out = mutableListOf<String>()
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val home = context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
        if (home != context.packageName) out += NOT_HOME
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        if (!enabled.contains("${context.packageName}/")) out += SERVICE_OFF
        return out
    }

    /** The settings page that fixes [problem]. */
    fun fixIntent(problem: String): Intent = when (problem) {
        NOT_HOME -> Intent(Settings.ACTION_HOME_SETTINGS)
        else -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** The stock launcher, for "Open Boox home". Falls back to the first non-Inkling HOME app. */
    fun stockHomeIntent(context: Context): Intent? {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val other = context.packageManager.queryIntentActivities(homeIntent, 0)
            .firstOrNull { it.activityInfo.packageName != context.packageName } ?: return null
        return Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setClassName(other.activityInfo.packageName, other.activityInfo.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
```

`BootReceiver.kt`:

```kotlin
package dev.inkling.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.inkling.MainActivity

/** After a reboot, bring Inkling up so the setup banner is visible if anything got reset. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
```

- [ ] **Step 6: Navigation and MainActivity**

`Nav.kt`:

```kotlin
package dev.inkling.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.inkling.service.SetupCheck
import dev.inkling.spike.SpikeScreen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InklingNav(home: HomeViewModel, parent: ParentViewModel) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    NavHost(nav, startDestination = "home",
        enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) {
        composable("home") {
            val s by home.state.collectAsState()
            LaunchedEffect(Unit) { home.refresh() }
            KidHome(
                state = s,
                onOpen = { t ->
                    if (t.done) nav.navigate("done/${t.label}")
                    else ctx.packageManager.getLaunchIntentForPackage(t.packageName)?.let { ctx.startActivity(it) }
                },
                onRead = { Toast.makeText(ctx, "Books come next.", Toast.LENGTH_SHORT).show() },
                onGearLongPress = { nav.navigate("pin") },
            )
        }
        composable("done/{label}") { back ->
            DoneScreen(appLabel = back.arguments?.getString("label") ?: "", onPickBook = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
        composable("pin") {
            val s by parent.state.collectAsState()
            LaunchedEffect(Unit) { parent.refresh() }
            PinScreen(
                hasPin = s.settings?.pinHash != null,
                tryPin = { parent.tryPin(it, System.currentTimeMillis()) },
                lockoutSeconds = { parent.lockoutRemainingSeconds(System.currentTimeMillis()) },
                onSetPin = { parent.setPin(it) },
                onUnlocked = { nav.navigate("parent") { popUpTo("home") } },
                onBack = { nav.popBackStack() },
            )
        }
        composable("parent") {
            val s by parent.state.collectAsState()
            LaunchedEffect(Unit) { parent.setupProblems = { SetupCheck.problems(ctx) }; parent.refresh() }
            ParentScreen(
                state = s,
                onKidsMode = { parent.setKidsMode(it) },
                onApp = { row, en, cap -> parent.setApp(row, en, cap) },
                onCeiling = { parent.setCeiling(it) },
                onOpenBooxHome = { SetupCheck.stockHomeIntent(ctx)?.let { ctx.startActivity(it) } ?: Toast.makeText(ctx, "No other home app found", Toast.LENGTH_SHORT).show() },
                onFixSetup = { ctx.startActivity(SetupCheck.fixIntent(it)) },
                onSpeechTest = { nav.navigate("spike") },
                onExportSpike = {
                    scope.launch {
                        val f = File(ctx.getExternalFilesDir(null), "spike.csv")
                        f.writeText(parent.spikeCsv())
                        Toast.makeText(ctx, "Saved ${f.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                },
                onChangePin = { parent.setPin(""); nav.navigate("pin") },
                onLock = { nav.navigate("home") { popUpTo("home") { inclusive = true } } },
            )
        }
        composable("spike") { SpikeScreen(dev.inkling.InklingApp.instance.repo) }
    }
}
```

`MainActivity.kt`:

```kotlin
package dev.inkling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.inkling.ui.HomeViewModel
import dev.inkling.ui.InklingNav
import dev.inkling.ui.InklingTheme
import dev.inkling.ui.ParentViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as InklingApp).repo
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                HomeViewModel::class.java -> HomeViewModel(repo) as T
                ParentViewModel::class.java -> ParentViewModel(repo, packageManager, packageName) as T
                else -> throw IllegalArgumentException("Unknown ViewModel $modelClass")
            }
        }
        setContent {
            InklingTheme {
                InklingNav(home = viewModel(factory = factory), parent = viewModel(factory = factory))
            }
        }
    }
}
```

Add `androidx.lifecycle:lifecycle-viewmodel-compose` to the catalog and dependencies:

```toml
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
```

```kotlin
implementation(libs.lifecycle.viewmodel.compose)
```

- [ ] **Step 7: Manifest: HOME intent and boot receiver**

Replace the `MainActivity` intent filter with:

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

Add permission and receiver:

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

```xml
<receiver android:name=".service.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

- [ ] **Step 8: Build, install, device journey**

```bash
cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On the Boox, follow README "Set up the device", then run the spec's verification list. Photograph each step. Record results in `docs/plans/progress.md`. Known things to check first:

1. Does Settings > Apps > Default apps > Home app list Inkling? If Onyx hides that page, run `adb shell cmd package set-home-activity dev.inkling/.MainActivity`.
2. Does pressing the Boox's navigation-bar home go to Kid home, or does Onyx intercept it? If intercepted, note it; that is the first reason to go Device Owner.
3. Does the accessibility service survive a reboot?

- [ ] **Step 9: Commit and push**

```bash
git add android docs/plans/progress.md
git commit -m "feat(app): PIN, parent screen, navigation, HOME registration, boot check

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
git push -u origin main
```

---

## Self-review

**Spec coverage.** Kid home, Done for today, PIN with lockout, parent screen with Kids mode toggle, Today, Apps, Rules (ceiling stepper; warning, quiet hours, children shown as fixed values in 1a since the spec fixes their defaults and 1b adds editing), Speech test and CSV export, blocker, usage log, cap with warning overlay, whole-device ceiling, quiet hours, Open Boox home, boot check with banner, HOME registration: all have tasks. Read tile opens a toast, as the spec allows for 1a. `EinkRefresh` wrapper is deferred to 1b with the reader, where flicker first matters; 1a screens are static.

**Gaps accepted.** Quiet hours and warning minutes are not editable in 1a. Warning overlay uses a plain `TextView` on purpose; Compose in a service window is more code for no gain.

**Type consistency.** `Rule`, `Span` (core) vs `AppRule`, `UsageEvent` (data) are distinct on purpose; `Repo.spansToday` maps to `Span`, `HomeViewModel` and `KidsModeService` map `AppRule` to `Rule`. `BlockDecision.decide` signature is identical in Tasks 3, 6, 7. `PinScreen.tryPin` is `suspend (String) -> Boolean`, matching `ParentViewModel.tryPin`.
