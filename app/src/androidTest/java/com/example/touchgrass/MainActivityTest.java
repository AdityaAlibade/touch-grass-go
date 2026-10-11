package com.example.touchgrass;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * End-to-end Espresso UI instrumentation tests for TOUCH GRASS GO:
 * - Activity launch and Home Tab character overview
 * - Bottom Navigation 5-tab switching (Home, Map, Quests, Character, Journal)
 * - Character Sheet settings (Theme switcher dialog, GPS Proximity dialog)
 * - Journal Export controls (Markdown & PDF buttons)
 * - Back-stack navigation back to Home tab
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Test
    public void testActivityLaunchAndHomeTabDisplayed() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Home tab layout and elements should be visible
            onView(withId(R.id.layoutHomeTab)).check(matches(isDisplayed()));
            onView(withId(R.id.tvHomeCharacterName)).check(matches(isDisplayed()));
            onView(withId(R.id.tvHomeLevelBadge)).check(matches(isDisplayed()));
            onView(withId(R.id.tvHomeStreakBadge)).check(matches(isDisplayed()));
            onView(withId(R.id.btnHomeExploreMap)).check(matches(isDisplayed()));
            onView(withId(R.id.btnHomeStartQuest)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testBottomNavigationTabSwitching() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 1. Switch to Map tab
            onView(withId(R.id.nav_map)).perform(click());
            onView(withId(R.id.layoutMapTab)).check(matches(isDisplayed()));
            onView(withId(R.id.layoutHomeTab)).check(matches(not(isDisplayed())));

            // 2. Switch to Quests tab
            onView(withId(R.id.nav_quests)).perform(click());
            onView(withId(R.id.layoutQuestsTab)).check(matches(isDisplayed()));
            onView(withId(R.id.layoutMapTab)).check(matches(not(isDisplayed())));

            // 3. Switch to Character tab
            onView(withId(R.id.nav_character)).perform(click());
            onView(withId(R.id.layoutCharacterTab)).check(matches(isDisplayed()));
            onView(withId(R.id.tvCharacterName)).check(matches(isDisplayed()));

            // 4. Switch to Journal tab
            onView(withId(R.id.nav_journal)).perform(click());
            onView(withId(R.id.layoutJournalTab)).check(matches(isDisplayed()));
            onView(withId(R.id.cardJournalExport)).check(matches(isDisplayed()));

            // 5. Return to Home tab
            onView(withId(R.id.nav_home)).perform(click());
            onView(withId(R.id.layoutHomeTab)).check(matches(isDisplayed()));
            onView(withId(R.id.layoutJournalTab)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void testCharacterTabSettingsCards() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Character Tab
            onView(withId(R.id.nav_character)).perform(click());
            onView(withId(R.id.layoutCharacterTab)).check(matches(isDisplayed()));

            // Verify Theme settings card and trigger selection dialog
            onView(withId(R.id.tvCurrentThemeStatus)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btnSwitchTheme)).perform(scrollTo(), click());

            // Verify dialog opened with theme options
            onView(withText("Select App Theme")).check(matches(isDisplayed()));
            onView(withText("Cancel")).perform(click());

            // Verify Proximity settings card and trigger configuration dialog
            onView(withId(R.id.tvProximityStatus)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btnConfigureProximity)).perform(scrollTo(), click());

            // Verify dialog opened with geofencing options
            onView(withText("GPS Proximity Geofencing")).check(matches(isDisplayed()));
            onView(withText("Cancel")).perform(click());
        }
    }

    @Test
    public void testJournalTabExportControls() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Navigate to Journal Tab
            onView(withId(R.id.nav_journal)).perform(click());
            onView(withId(R.id.layoutJournalTab)).check(matches(isDisplayed()));

            // Verify Export Action Card and buttons
            onView(withId(R.id.cardJournalExport)).check(matches(isDisplayed()));
            onView(withId(R.id.btnExportMarkdown)).check(matches(isDisplayed()));
            onView(withId(R.id.btnExportPdf)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testBackButtonReturnsToHomeTab() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Switch to Quests tab
            onView(withId(R.id.nav_quests)).perform(click());
            onView(withId(R.id.layoutQuestsTab)).check(matches(isDisplayed()));

            // Press back -> should navigate back to Home tab rather than exiting
            pressBack();
            onView(withId(R.id.layoutHomeTab)).check(matches(isDisplayed()));
        }
    }
}
