package com.example.touchgrass.journal;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generates and exports the player's IRL Character Sheet and Adventure Journal
 * into Markdown (.md) and printable PDF (.pdf) documents.
 */
public class AdventureLogExporter {

    private static final String EXPORT_DIR = "exports";
    private static final int A4_WIDTH = 595;
    private static final int A4_HEIGHT = 842;
    private static final int MARGIN_HORIZONTAL = 40;
    private static final int CONTENT_WIDTH = A4_WIDTH - (MARGIN_HORIZONTAL * 2);

    /**
     * Pure Java generator that produces a GitHub-flavored Markdown adventure sheet.
     */
    public static String generateMarkdown(
            String characterName,
            String equippedTitle,
            int level,
            int totalXp,
            int currentStreak,
            int longestStreak,
            int completedQuestsCount,
            List<String> unlockedTitles,
            List<JournalEntry> entries
    ) {
        StringBuilder sb = new StringBuilder();
        String exportDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date());

        sb.append("# 🌿 TOUCH GRASS GO — IRL Adventure Log\n\n");
        sb.append("> *\"Your screen has side quests. Your actual life should too.\"*\n\n");
        sb.append("**Export Date:** ").append(exportDate).append("\n\n");

        sb.append("## 👤 Adventurer Profile\n\n");
        sb.append("| Stat | Value |\n");
        sb.append("| :--- | :--- |\n");
        sb.append("| **Character Name** | ").append(characterName != null ? characterName : "Grass Rookie").append(" |\n");
        sb.append("| **Equipped Title** | 🏆 ").append(equippedTitle != null ? equippedTitle : "Certified Indoor Goblin").append(" |\n");
        sb.append("| **Level** | Level ").append(level).append(" |\n");
        sb.append("| **Total XP** | ").append(totalXp).append(" XP |\n");
        sb.append("| **Completed Quests** | ").append(completedQuestsCount).append(" |\n");
        sb.append("| **Active Daily Streak** | 🔥 ").append(currentStreak).append(" Days (Longest: ").append(longestStreak).append(" Days) |\n\n");

        sb.append("## 🎖️ Unlocked Titles\n\n");
        if (unlockedTitles != null && !unlockedTitles.isEmpty()) {
            for (String title : unlockedTitles) {
                sb.append("- 🏆 **").append(title).append("**\n");
            }
        } else {
            sb.append("- *No additional titles unlocked yet. Touch grass to earn milestones!*\n");
        }
        sb.append("\n");

        sb.append("## 📜 Adventure Chronicle & Completed Quests\n\n");
        if (entries == null || entries.isEmpty()) {
            sb.append("*No outdoor quests recorded in this chronicle yet. Go outside and complete your first quest!*\n\n");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                JournalEntry entry = entries.get(i);
                sb.append("### ").append(i + 1).append(". ").append(entry.getQuestName()).append("\n\n");
                sb.append("- **Date:** ").append(entry.getFormattedDate()).append("\n");
                sb.append("- **Category:** ").append(entry.getCategory()).append("\n");
                sb.append("- **XP Earned:** +").append(entry.getXpEarned()).append(" XP\n");

                if (entry.getUnlockedTitle() != null && !entry.getUnlockedTitle().trim().isEmpty()) {
                    sb.append("- **Unlocked Milestone:** 🏆 ").append(entry.getUnlockedTitle()).append("\n");
                }

                if (entry.getReflection() != null && !entry.getReflection().trim().isEmpty()) {
                    sb.append("- **Personal Reflection:**\n\n");
                    sb.append("  > ").append(entry.getReflection().replace("\n", "\n  > ")).append("\n");
                }
                sb.append("\n---\n\n");
            }
        }

        sb.append("*\nGenerated with Touch Grass Go (India Outdoor Exploration) — OpenStreetMap & Local AI* 🇮🇳\n");
        return sb.toString();
    }

    /**
     * Saves Markdown to disk in app cache directory.
     */
    public static File exportMarkdownFile(
            Context context,
            String characterName,
            String equippedTitle,
            int level,
            int totalXp,
            int currentStreak,
            int longestStreak,
            int completedQuestsCount,
            List<String> unlockedTitles,
            List<JournalEntry> entries
    ) throws IOException {
        String markdown = generateMarkdown(characterName, equippedTitle, level, totalXp,
                currentStreak, longestStreak, completedQuestsCount, unlockedTitles, entries);

        File dir = new File(context.getCacheDir(), EXPORT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, "touch_grass_adventure_log.md");
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(markdown);
        }
        return file;
    }

    /**
     * Renders and exports a printable A4 PDF document using native Android Canvas.
     */
    public static File exportPdfFile(
            Context context,
            String characterName,
            String equippedTitle,
            int level,
            int totalXp,
            int currentStreak,
            int longestStreak,
            int completedQuestsCount,
            List<JournalEntry> entries
    ) throws IOException {
        PdfDocument document = new PdfDocument();
        int pageIndex = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIndex).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Setup Paints
        Paint paintHeaderBg = new Paint();
        paintHeaderBg.setColor(Color.parseColor("#1B5E20")); // Deep forest green

        Paint paintHeaderTitle = new Paint();
        paintHeaderTitle.setColor(Color.WHITE);
        paintHeaderTitle.setTextSize(20);
        paintHeaderTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintHeaderTitle.setAntiAlias(true);

        Paint paintHeaderSub = new Paint();
        paintHeaderSub.setColor(Color.parseColor("#C8E6C9"));
        paintHeaderSub.setTextSize(10);
        paintHeaderSub.setAntiAlias(true);

        Paint paintCardBg = new Paint();
        paintCardBg.setColor(Color.parseColor("#F1F8E9")); // Pale meadow

        Paint paintCardStroke = new Paint();
        paintCardStroke.setColor(Color.parseColor("#A5D6A7"));
        paintCardStroke.setStyle(Paint.Style.STROKE);
        paintCardStroke.setStrokeWidth(1);

        Paint paintTextPrimary = new Paint();
        paintTextPrimary.setColor(Color.parseColor("#1B2A1E"));
        paintTextPrimary.setTextSize(12);
        paintTextPrimary.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintTextPrimary.setAntiAlias(true);

        Paint paintTextSecondary = new Paint();
        paintTextSecondary.setColor(Color.parseColor("#435B47"));
        paintTextSecondary.setTextSize(10);
        paintTextSecondary.setAntiAlias(true);

        Paint paintBadgeBg = new Paint();
        paintBadgeBg.setColor(Color.parseColor("#2E7D32"));

        Paint paintBadgeText = new Paint();
        paintBadgeText.setColor(Color.WHITE);
        paintBadgeText.setTextSize(9);
        paintBadgeText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintBadgeText.setAntiAlias(true);

        Paint paintDivider = new Paint();
        paintDivider.setColor(Color.parseColor("#D7E5D9"));
        paintDivider.setStrokeWidth(1);

        // Draw Header Banner on Page 1
        RectF headerRect = new RectF(MARGIN_HORIZONTAL, 35, MARGIN_HORIZONTAL + CONTENT_WIDTH, 105);
        canvas.drawRoundRect(headerRect, 10, 10, paintHeaderBg);
        canvas.drawText("TOUCH GRASS GO — IRL ADVENTURE LOG", MARGIN_HORIZONTAL + 16, 65, paintHeaderTitle);
        canvas.drawText("“Your screen has side quests. Your actual life should too.”", MARGIN_HORIZONTAL + 16, 85, paintHeaderSub);

        // Draw Player Profile Card (Page 1)
        float profileTop = 118;
        float profileBottom = 195;
        RectF profileRect = new RectF(MARGIN_HORIZONTAL, profileTop, MARGIN_HORIZONTAL + CONTENT_WIDTH, profileBottom);
        canvas.drawRoundRect(profileRect, 8, 8, paintCardBg);
        canvas.drawRoundRect(profileRect, 8, 8, paintCardStroke);

        String name = characterName != null ? characterName : "Grass Rookie";
        String title = equippedTitle != null ? equippedTitle : "Certified Indoor Goblin";
        canvas.drawText(name + "  •  " + title, MARGIN_HORIZONTAL + 14, profileTop + 24, paintTextPrimary);

        String statsLine1 = String.format(Locale.ROOT, "Level %d  |  Total XP: %d XP  |  Quests Completed: %d",
                level, totalXp, completedQuestsCount);
        canvas.drawText(statsLine1, MARGIN_HORIZONTAL + 14, profileTop + 45, paintTextSecondary);

        String statsLine2 = String.format(Locale.ROOT, "🔥 Active Streak: %d Days  |  Longest Streak: %d Days",
                currentStreak, longestStreak);
        canvas.drawText(statsLine2, MARGIN_HORIZONTAL + 14, profileTop + 63, paintTextSecondary);

        // Section Title: Adventure Chronicle
        float currentY = profileBottom + 26;
        Paint paintSectionTitle = new Paint(paintTextPrimary);
        paintSectionTitle.setTextSize(14);
        paintSectionTitle.setColor(Color.parseColor("#2E7D32"));
        canvas.drawText("ADVENTURE CHRONICLE & COMPLETED QUESTS", MARGIN_HORIZONTAL, currentY, paintSectionTitle);
        currentY += 14;

        if (entries == null || entries.isEmpty()) {
            currentY += 20;
            canvas.drawText("No quests logged yet. Go outdoors, complete quests, and touch grass!", MARGIN_HORIZONTAL, currentY, paintTextSecondary);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                JournalEntry entry = entries.get(i);

                // Estimate height required for this entry
                boolean hasReflection = entry.getReflection() != null && !entry.getReflection().trim().isEmpty();
                boolean hasTitle = entry.getUnlockedTitle() != null && !entry.getUnlockedTitle().trim().isEmpty();
                float entryHeight = 50 + (hasReflection ? 26 : 0) + (hasTitle ? 16 : 0);

                // Check if page capacity exceeded (leaving 50pt bottom margin for footer)
                if (currentY + entryHeight > A4_HEIGHT - 50) {
                    drawFooter(canvas, pageIndex);
                    document.finishPage(page);

                    pageIndex++;
                    pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIndex).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    currentY = 40;
                }

                // Render Journal Entry Item
                RectF entryRect = new RectF(MARGIN_HORIZONTAL, currentY, MARGIN_HORIZONTAL + CONTENT_WIDTH, currentY + entryHeight);
                canvas.drawRoundRect(entryRect, 6, 6, paintCardBg);
                canvas.drawRoundRect(entryRect, 6, 6, paintCardStroke);

                // Title & Category
                canvas.drawText((i + 1) + ". " + entry.getQuestName(), MARGIN_HORIZONTAL + 12, currentY + 18, paintTextPrimary);

                // XP Badge (right side)
                String xpBadge = "+" + entry.getXpEarned() + " XP";
                float xpWidth = paintBadgeText.measureText(xpBadge);
                RectF badgeRect = new RectF(MARGIN_HORIZONTAL + CONTENT_WIDTH - xpWidth - 20, currentY + 8,
                        MARGIN_HORIZONTAL + CONTENT_WIDTH - 10, currentY + 24);
                canvas.drawRoundRect(badgeRect, 4, 4, paintBadgeBg);
                canvas.drawText(xpBadge, badgeRect.left + 5, badgeRect.top + 11, paintBadgeText);

                // Subtitle details: Date and Category
                String details = entry.getFormattedDate() + "  •  Category: " + entry.getCategory();
                canvas.drawText(details, MARGIN_HORIZONTAL + 12, currentY + 34, paintTextSecondary);

                float offset = currentY + 34;

                if (hasTitle) {
                    offset += 14;
                    canvas.drawText("🏆 Unlocked Title: " + entry.getUnlockedTitle(), MARGIN_HORIZONTAL + 12, offset, paintTextPrimary);
                }

                if (hasReflection) {
                    offset += 14;
                    String note = "Note: \"" + truncate(entry.getReflection(), 95) + "\"";
                    canvas.drawText(note, MARGIN_HORIZONTAL + 12, offset, paintTextSecondary);
                }

                currentY += entryHeight + 8;
            }
        }

        // Draw Footer on last page
        drawFooter(canvas, pageIndex);
        document.finishPage(page);

        // Save PDF to cache directory
        File dir = new File(context.getCacheDir(), EXPORT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, "touch_grass_adventure_log.pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }

        return file;
    }

    private static void drawFooter(Canvas canvas, int pageNumber) {
        Paint paintFooter = new Paint();
        paintFooter.setColor(Color.parseColor("#78909C"));
        paintFooter.setTextSize(8);
        paintFooter.setAntiAlias(true);

        String footerText = "Touch Grass Go — Real Life Side Quests & Character Sheet   |   Page " + pageNumber;
        canvas.drawText(footerText, MARGIN_HORIZONTAL, A4_HEIGHT - 24, paintFooter);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Dispatches an Android Share Intent via FileProvider for external viewing or printing.
     */
    public static void shareFile(Context context, File file, String mimeType, String chooserTitle) {
        if (context == null || file == null || !file.exists()) return;

        Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Touch Grass Go Adventure Log");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(shareIntent, chooserTitle);
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }
}
