package com.example.touchgrass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.touchgrass.journal.AdventureLogExporter;
import com.example.touchgrass.journal.JournalEntry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for AdventureLogExporter Markdown document generation,
 * formatting, stats serialization, and null-safety.
 */
public class AdventureLogExporterTest {

    @Test
    public void testGenerateMarkdownWithFullData() {
        List<String> titles = Arrays.asList("Certified Indoor Goblin", "Certified Grass Toucher", "Pigeon Diplomat");

        List<JournalEntry> entries = new ArrayList<>();
        entries.add(new JournalEntry(
                "entry_1",
                "quest_1",
                "Cubbon Park Canopy Stroll",
                "Mindfulness",
                30,
                1700000000000L,
                "Oct 11, 2026 at 10:30 AM",
                "Certified Grass Toucher",
                "Felt calm and spotted two green parakeets in the bamboo grove."
        ));
        entries.add(new JournalEntry(
                "entry_2",
                "quest_2",
                "Pigeon Council Summit",
                "Observation",
                25,
                1700086400000L,
                "Oct 12, 2026 at 4:15 PM",
                null,
                "Observed a council of 12 pigeons discussing important matters."
        ));

        String md = AdventureLogExporter.generateMarkdown(
                "Aditya",
                "Pigeon Diplomat",
                3,
                255,
                4,
                7,
                2,
                titles,
                entries
        );

        assertNotNull(md);
        assertTrue(md.contains("# 🌿 TOUCH GRASS GO — IRL Adventure Log"));
        assertTrue(md.contains("Aditya"));
        assertTrue(md.contains("Pigeon Diplomat"));
        assertTrue(md.contains("Level 3"));
        assertTrue(md.contains("255 XP"));
        assertTrue(md.contains("4 Days (Longest: 7 Days)"));

        // Check titles list
        assertTrue(md.contains("Certified Grass Toucher"));
        assertTrue(md.contains("Pigeon Diplomat"));

        // Check entries
        assertTrue(md.contains("Cubbon Park Canopy Stroll"));
        assertTrue(md.contains("+30 XP"));
        assertTrue(md.contains("Mindfulness"));
        assertTrue(md.contains("Felt calm and spotted two green parakeets"));
        assertTrue(md.contains("Pigeon Council Summit"));
        assertTrue(md.contains("+25 XP"));
    }

    @Test
    public void testGenerateMarkdownEmptyEntries() {
        String md = AdventureLogExporter.generateMarkdown(
                "Grass Rookie",
                "Certified Indoor Goblin",
                1,
                0,
                0,
                0,
                0,
                new ArrayList<>(),
                new ArrayList<>()
        );

        assertNotNull(md);
        assertTrue(md.contains("Level 1"));
        assertTrue(md.contains("0 XP"));
        assertTrue(md.contains("No outdoor quests recorded in this chronicle yet"));
    }

    @Test
    public void testGenerateMarkdownNullSafety() {
        String md = AdventureLogExporter.generateMarkdown(
                null,
                null,
                1,
                0,
                0,
                0,
                0,
                null,
                null
        );

        assertNotNull(md);
        assertTrue(md.contains("Grass Rookie"));
        assertTrue(md.contains("Certified Indoor Goblin"));
        assertTrue(md.contains("No additional titles unlocked yet"));
        assertTrue(md.contains("No outdoor quests recorded in this chronicle yet"));
    }

    @Test
    public void testMarkdownMultilineReflection() {
        List<JournalEntry> entries = new ArrayList<>();
        entries.add(new JournalEntry(
                "entry_multiline",
                "quest_multi",
                "Deep Forest Contemplation",
                "Nature",
                45,
                1700000000000L,
                "Oct 11, 2026 at 2:00 PM",
                null,
                "Line 1: Saw an ancient banyan.\nLine 2: Felt grounded."
        ));

        String md = AdventureLogExporter.generateMarkdown(
                "Aditya", "Wanderer", 2, 120, 1, 1, 1, null, entries
        );

        assertTrue(md.contains("> Line 1: Saw an ancient banyan.\n  > Line 2: Felt grounded."));
    }
}
