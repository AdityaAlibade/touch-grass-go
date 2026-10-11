package com.example.touchgrass;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.touchgrass.ai.AiQuestService;
import com.example.touchgrass.ai.QuestGenerationRequest;
import com.example.touchgrass.avatar.AvatarManager;
import com.example.touchgrass.databinding.ActivityMainBinding;
import com.example.touchgrass.journal.JournalEntry;
import com.example.touchgrass.journal.JournalManager;
import com.example.touchgrass.model.Quest;
import com.example.touchgrass.model.QuestStatus;
import com.example.touchgrass.progression.ProgressionManager;
import com.example.touchgrass.repository.QuestRepository;
import com.example.touchgrass.streak.StreakManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * TOUCH GRASS GO — Main Activity handling:
 * - Opening Splash Sequence & Home Dashboard
 * - Step 1: Character Sheet Foundation
 * - Step 2: Interactive OpenStreetMap & Location Support
 * - Step 3: Quest Discovery & Acceptance
 * - Step 4: Self-Reported Quest Completion, XP, Leveling, and Titles
 * - Step 5: AI-Powered Personalized Quest Generation using Gemma Model Abstraction & Fallback
 * - Step 6: Character Profile, Adventure Journal, and Daily Streaks
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    // Map UI components
    private MapView mapView;
    private ProgressBar progressMapLoading;
    private MaterialCardView bannerOffline;

    // Location Overlay
    private MyLocationNewOverlay myLocationOverlay;

    // Repositories & Managers
    private QuestRepository questRepository;
    private ProgressionManager progressionManager;
    private AiQuestService aiQuestService;
    private StreakManager streakManager;
    private JournalManager journalManager;
    private AvatarManager avatarManager;
    private com.example.touchgrass.location.ProximityManager proximityManager;
    private com.example.touchgrass.theme.ThemeManager themeManager;

    // Default Starter Character Data
    private static final String DEFAULT_NAME = "Grass Rookie";

    // Location Permission Launcher
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    enableAndCenterUserLocation();
                } else {
                    Toast.makeText(this, R.string.map_location_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure osmdroid before layout inflation
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE);
        Configuration.getInstance().load(ctx, prefs);
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Trigger Room SQLite database migration from legacy SharedPreferences
        com.example.touchgrass.db.DataMigrationHelper.checkAndMigrate(this);

        // Initialize repositories & managers
        questRepository = QuestRepository.getInstance(this);
        progressionManager = ProgressionManager.getInstance(this);
        aiQuestService = new AiQuestService();
        streakManager = StreakManager.getInstance(this);
        journalManager = JournalManager.getInstance(this);
        avatarManager = AvatarManager.getInstance(this);
        proximityManager = com.example.touchgrass.location.ProximityManager.getInstance(this);
        themeManager = com.example.touchgrass.theme.ThemeManager.getInstance(this);
        com.example.touchgrass.theme.ThemeManager.applyTheme(themeManager.getThemeMode());

        // Inflate view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Splash & Tabs
        setupSplashSequence();
        refreshHomeTab();
        refreshCharacterTab();
        setupMapScreen();
        refreshQuestsTab();
        refreshJournalTab();

        // Setup bottom navigation bar
        setupBottomNavigation();
    }

    /**
     * Controls the opening splash sequence and transitions smoothly to the Home screen.
     */
    private void setupSplashSequence() {
        View splashOverlay = binding.getRoot().findViewById(R.id.layoutSplashOverlay);
        if (splashOverlay != null) {
            splashOverlay.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                splashOverlay.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction(() -> splashOverlay.setVisibility(View.GONE))
                        .start();
            }, 1400);
        }
    }

    /**
     * Binds updated character overview, streak, and quick action buttons to the Home Page.
     */
    private void refreshHomeTab() {
        View homeLayout = binding.layoutHomeTab;
        if (homeLayout == null) return;

        int totalXp = progressionManager.getTotalXp();
        int currentLevel = progressionManager.getCurrentLevel();
        String equippedTitle = progressionManager.getEquippedTitle();
        int avatarResId = avatarManager.getEquippedAvatarResId();
        int currentStreak = streakManager.getCurrentStreak();

        ImageView imgAvatar = homeLayout.findViewById(R.id.imgHomeAvatar);
        TextView tvName = homeLayout.findViewById(R.id.tvHomeCharacterName);
        TextView tvTitle = homeLayout.findViewById(R.id.tvHomeCharacterTitle);
        TextView tvLevel = homeLayout.findViewById(R.id.tvHomeLevelBadge);
        TextView tvXp = homeLayout.findViewById(R.id.tvHomeXpProgress);
        ProgressBar progressBar = homeLayout.findViewById(R.id.progressBarHomeXp);
        TextView tvStreak = homeLayout.findViewById(R.id.tvHomeStreakBadge);
        MaterialButton btnExploreMap = homeLayout.findViewById(R.id.btnHomeExploreMap);
        MaterialButton btnStartQuest = homeLayout.findViewById(R.id.btnHomeStartQuest);

        if (imgAvatar != null) imgAvatar.setImageResource(avatarResId);
        if (tvName != null) tvName.setText(DEFAULT_NAME);
        if (tvTitle != null) tvTitle.setText(equippedTitle);
        if (tvLevel != null) tvLevel.setText(getString(R.string.level_format, currentLevel));

        int levelBaseXp = ProgressionManager.getLevelBaseXp(currentLevel);
        int nextLevelThreshold = ProgressionManager.getNextLevelThreshold(currentLevel);
        int xpInLevel = totalXp - levelBaseXp;
        int xpLevelSpan = nextLevelThreshold - levelBaseXp;

        if (tvXp != null) tvXp.setText(getString(R.string.xp_format, totalXp, nextLevelThreshold));
        if (progressBar != null) {
            progressBar.setMax(xpLevelSpan);
            progressBar.setProgress(Math.min(xpInLevel, xpLevelSpan));
        }

        if (tvStreak != null) {
            tvStreak.setText(getString(R.string.streak_format, currentStreak, streakManager.getLongestStreak()));
        }

        if (btnExploreMap != null) {
            btnExploreMap.setOnClickListener(v -> binding.bottomNavigation.setSelectedItemId(R.id.nav_map));
        }

        if (btnStartQuest != null) {
            btnStartQuest.setOnClickListener(v -> binding.bottomNavigation.setSelectedItemId(R.id.nav_quests));
        }
    }

    /**
     * Initializes the osmdroid interactive map view centered on India.
     */
    private void setupMapScreen() {
        View mapLayout = binding.layoutMapTab;
        mapView = mapLayout.findViewById(R.id.mapView);
        progressMapLoading = mapLayout.findViewById(R.id.progressMapLoading);
        bannerOffline = mapLayout.findViewById(R.id.bannerOffline);
        FloatingActionButton btnMyLocation = mapLayout.findViewById(R.id.btnMyLocation);
        FloatingActionButton btnZoomIn = mapLayout.findViewById(R.id.btnZoomIn);
        FloatingActionButton btnZoomOut = mapLayout.findViewById(R.id.btnZoomOut);

        // Configure map properties
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Set initial view centered on India (Latitude: 20.5937, Longitude: 78.9629)
        GeoPoint indiaCenter = new GeoPoint(20.5937, 78.9629);
        mapView.getController().setZoom(5.5);
        mapView.getController().setCenter(indiaCenter);

        // Add OpenStreetMap Copyright / Attribution Overlay
        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(this);
        mapView.getOverlays().add(copyrightOverlay);

        // Coordinate OpenStreetMap tiles overlay with current app theme
        com.example.touchgrass.theme.ThemeManager.coordinateMapTheme(
                mapView, com.example.touchgrass.theme.ThemeManager.isNightModeActive(this));

        // Network check for tile loading state
        checkNetworkAndSetOfflineBanner();

        // Hide progress spinner after initial map view scroll/zoom
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (progressMapLoading != null) progressMapLoading.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                if (progressMapLoading != null) progressMapLoading.setVisibility(View.GONE);
                return false;
            }
        });

        // Add DEMO & AI Quest Markers on Map
        refreshMapQuestMarkers();

        // Zoom button listeners
        btnZoomIn.setOnClickListener(v -> {
            if (mapView != null) mapView.getController().zoomIn();
        });

        btnZoomOut.setOnClickListener(v -> {
            if (mapView != null) mapView.getController().zoomOut();
        });

        // Location FAB click listener
        btnMyLocation.setOnClickListener(v -> handleMyLocationClick());
    }

    /**
     * Refreshes all Quest Markers on the Map based on repository state.
     */
    private void refreshMapQuestMarkers() {
        if (mapView == null) return;

        // Retain overlays except markers
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker);

        Drawable availableIcon = ContextCompat.getDrawable(this, R.drawable.ic_quest_pin);
        Drawable activeIcon = ContextCompat.getDrawable(this, R.drawable.ic_quest_pin_active);
        Drawable completedIcon = ContextCompat.getDrawable(this, R.drawable.ic_quest_pin_completed);

        List<Quest> quests = questRepository.getAllQuests();
        for (Quest quest : quests) {
            GeoPoint position = new GeoPoint(quest.getLatitude(), quest.getLongitude());
            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setTitle(quest.getName());

            // Set distinct marker icon based on quest status
            if (quest.getStatus() == QuestStatus.COMPLETED) {
                if (completedIcon != null) marker.setIcon(completedIcon);
            } else if (quest.getStatus() == QuestStatus.ACTIVE) {
                if (activeIcon != null) marker.setIcon(activeIcon);
            } else {
                if (availableIcon != null) marker.setIcon(availableIcon);
            }

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            // Tapping a marker opens the Quest Detail Dialog
            marker.setOnMarkerClickListener((m, mapView1) -> {
                showQuestDetailDialog(quest);
                return true;
            });

            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }

    /**
     * Displays a styled Quest Detail Dialog allowing acceptance or completion.
     */
    private void showQuestDetailDialog(Quest quest) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_quest_detail_dialog, null);

        TextView tvCategory = dialogView.findViewById(R.id.tvDialogCategory);
        TextView tvDuration = dialogView.findViewById(R.id.tvDialogDuration);
        TextView tvXp = dialogView.findViewById(R.id.tvDialogXp);
        TextView tvName = dialogView.findViewById(R.id.tvDialogQuestName);
        TextView tvDesc = dialogView.findViewById(R.id.tvDialogDescription);
        TextView tvStatus = dialogView.findViewById(R.id.tvDialogStatus);
        TextView tvProximity = dialogView.findViewById(R.id.tvDialogProximity);
        MaterialButton btnAccept = dialogView.findViewById(R.id.btnAcceptQuest);
        MaterialButton btnComplete = dialogView.findViewById(R.id.btnCompleteQuest);

        tvCategory.setText(quest.getCategory());
        tvDuration.setText(quest.getEstimatedDuration());
        tvXp.setText(getString(R.string.xp_reward_format, quest.getXpReward()));
        tvName.setText(quest.getName());
        String tag = quest.isAiGenerated() ? "[AI GENERATED QUEST]" : getString(R.string.demo_quest_tag);
        tvDesc.setText(getString(R.string.quest_desc_format, quest.getDescription(), tag));

        // Evaluate live proximity if current GPS location is known
        if (tvProximity != null && myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint userLoc = myLocationOverlay.getMyLocation();
            com.example.touchgrass.location.ProximityManager.ProximityResult prox =
                    proximityManager.evaluateProximity(userLoc.getLatitude(), userLoc.getLongitude(), quest.getLatitude(), quest.getLongitude());
            tvProximity.setVisibility(View.VISIBLE);
            tvProximity.setText("📍 " + prox.statusMessage);
            if (prox.isWithinRadius) {
                tvProximity.setTextColor(ContextCompat.getColor(this, R.color.green_light));
            } else {
                tvProximity.setTextColor(ContextCompat.getColor(this, R.color.gold_rpg));
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (quest.getStatus() == QuestStatus.COMPLETED) {
            tvStatus.setText(getString(R.string.status_format, getString(R.string.status_completed)));
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_light));
            btnAccept.setText(R.string.btn_quest_completed);
            btnAccept.setEnabled(false);
            btnAccept.setVisibility(View.VISIBLE);
            btnComplete.setVisibility(View.GONE);
            btnAccept.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.disabled_button_bg));
            btnAccept.setTextColor(ContextCompat.getColor(this, R.color.text_disabled));
        } else if (quest.getStatus() == QuestStatus.ACTIVE) {
            tvStatus.setText(getString(R.string.status_format, getString(R.string.status_active)));
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.gold_rpg));
            btnAccept.setVisibility(View.GONE);
            btnComplete.setVisibility(View.VISIBLE);

            btnComplete.setOnClickListener(v -> {
                dialog.dismiss();
                showSelfReportConfirmationDialog(quest);
            });
        } else { // AVAILABLE
            tvStatus.setText(getString(R.string.status_format, getString(R.string.status_available)));
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_light));
            btnAccept.setVisibility(View.VISIBLE);
            btnComplete.setVisibility(View.GONE);
            btnAccept.setText(R.string.btn_accept_quest);
            btnAccept.setEnabled(true);

            btnAccept.setOnClickListener(v -> {
                questRepository.acceptQuest(quest.getId());
                quest.setStatus(QuestStatus.ACTIVE);

                Toast.makeText(this, R.string.quest_accepted_toast, Toast.LENGTH_SHORT).show();

                refreshMapQuestMarkers();
                refreshQuestsTab();

                dialog.dismiss();
            });
        }

        dialog.show();
    }

    /**
     * Prompts the user to confirm that they actually completed the outdoor activity in real life,
     * checking optional GPS proximity when enabled.
     */
    private void showSelfReportConfirmationDialog(Quest quest) {
        String confirmMessage;
        if (proximityManager.isProximityVerificationEnabled()) {
            GeoPoint userLoc = (myLocationOverlay != null) ? myLocationOverlay.getMyLocation() : null;
            if (userLoc != null) {
                com.example.touchgrass.location.ProximityManager.ProximityResult prox =
                        proximityManager.evaluateProximity(userLoc.getLatitude(), userLoc.getLongitude(), quest.getLatitude(), quest.getLongitude());
                if (prox.isWithinRadius) {
                    confirmMessage = "📍 Proximity Verified!\nYou are " + prox.formattedDistance + " away from \"" + quest.getName() + "\".\n\nDid you complete the outdoor activity in real life?";
                } else {
                    confirmMessage = "⚠️ Proximity Notice:\nYou are " + prox.formattedDistance + " away from \"" + quest.getName() + "\" (Configured radius: " + proximityManager.getProximityRadiusMeters() + " m).\n\nDo you want to confirm completion via exploration mode anyway?";
                }
            } else {
                confirmMessage = "ℹ️ GPS location currently unavailable.\n\nDid you complete \"" + quest.getName() + "\" in real life?";
            }
        } else {
            confirmMessage = getString(R.string.confirm_complete_message, quest.getName());
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_complete_title)
                .setMessage(confirmMessage)
                .setPositiveButton(R.string.confirm_complete_yes, (dialog, which) -> handleQuestCompletion(quest))
                .setNegativeButton(R.string.confirm_complete_no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Handles atomic progression processing when a quest is completed.
     */
    private void handleQuestCompletion(Quest quest) {
        ProgressionManager.CompletionResult result = progressionManager.completeQuest(quest);

        if (result.success) {
            Toast.makeText(this, getString(R.string.quest_completed_toast, result.xpGained), Toast.LENGTH_SHORT).show();

            // Record Daily Streak
            streakManager.recordQuestCompletionOnDateString(null);

            // Record Adventure Journal Entry
            long now = System.currentTimeMillis();
            String formattedDate = JournalManager.formatTimestamp(now);
            String newlyUnlockedTitle = !result.newlyUnlockedTitles.isEmpty()
                    ? result.newlyUnlockedTitles.get(result.newlyUnlockedTitles.size() - 1)
                    : "";

            JournalEntry entry = new JournalEntry(
                    "journal_" + quest.getId(),
                    quest.getId(),
                    quest.getName(),
                    quest.getCategory(),
                    result.xpGained,
                    now,
                    formattedDate,
                    newlyUnlockedTitle,
                    ""
            );
            journalManager.addJournalEntry(entry);

            // Check if user leveled up
            if (result.leveledUp) {
                showLevelUpDialog(result);
            }

            // Prompt optional personal reflection note
            showReflectionInputDialog(quest.getId(), quest.getName());

            // Refresh all screens
            refreshMapQuestMarkers();
            refreshHomeTab();
            refreshQuestsTab();
            refreshCharacterTab();
            refreshJournalTab();
        }
    }

    /**
     * Shows a level-up congratulations dialog when a cumulative threshold is crossed.
     */
    private void showLevelUpDialog(ProgressionManager.CompletionResult result) {
        StringBuilder msg = new StringBuilder(getString(R.string.level_up_message, result.newLevel, result.newXp));

        if (!result.newlyUnlockedTitles.isEmpty()) {
            msg.append(getString(R.string.title_unlocked_message, result.newlyUnlockedTitles.get(result.newlyUnlockedTitles.size() - 1)));
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.level_up_title)
                .setMessage(msg.toString())
                .setPositiveButton("Awesome!", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Displays an optional dialog allowing the player to add a personal reflection note to their journal entry.
     */
    private void showReflectionInputDialog(String questId, String questName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_reflection_dialog, null);

        TextView tvQuestName = dialogView.findViewById(R.id.tvReflectionQuestName);
        EditText etInput = dialogView.findViewById(R.id.etReflectionInput);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelReflection);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveReflection);

        tvQuestName.setText(questName);

        // Pre-fill existing reflection if present
        for (JournalEntry entry : journalManager.getAllJournalEntries()) {
            if (questId.equals(entry.getQuestId())) {
                if (entry.getReflection() != null && !entry.getReflection().isEmpty()) {
                    etInput.setText(entry.getReflection());
                }
                break;
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String text = etInput.getText() != null ? etInput.getText().toString().trim() : "";
            journalManager.updateReflection(questId, text);
            Toast.makeText(this, "Reflection note saved!", Toast.LENGTH_SHORT).show();
            refreshJournalTab();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Populates and refreshes the Quests Tab UI with AI Quest Generator banner, Active, Available, and Completed quests.
     */
    private void refreshQuestsTab() {
        View questsLayout = binding.layoutQuestsTab;
        MaterialCardView cardGenerateAiQuest = questsLayout.findViewById(R.id.cardGenerateAiQuest);
        MaterialCardView cardEmptyActive = questsLayout.findViewById(R.id.cardEmptyActive);
        MaterialCardView cardEmptyCompleted = questsLayout.findViewById(R.id.cardEmptyCompleted);
        LinearLayout containerActive = questsLayout.findViewById(R.id.containerActiveQuests);
        LinearLayout containerAvailable = questsLayout.findViewById(R.id.containerAvailableQuests);
        LinearLayout containerCompleted = questsLayout.findViewById(R.id.containerCompletedQuests);

        if (containerActive == null || containerAvailable == null || containerCompleted == null) return;

        // Hook up AI Quest Generator banner card
        if (cardGenerateAiQuest != null) {
            cardGenerateAiQuest.setOnClickListener(v -> showAiQuestGeneratorDialog());
        }

        containerActive.removeAllViews();
        containerAvailable.removeAllViews();
        containerCompleted.removeAllViews();

        List<Quest> activeQuests = questRepository.getActiveQuests();
        List<Quest> availableQuests = questRepository.getAvailableQuests();
        List<Quest> completedQuests = questRepository.getCompletedQuests();

        // Active Quests
        if (activeQuests.isEmpty()) {
            if (cardEmptyActive != null) cardEmptyActive.setVisibility(View.VISIBLE);
            containerActive.setVisibility(View.GONE);
        } else {
            if (cardEmptyActive != null) cardEmptyActive.setVisibility(View.GONE);
            containerActive.setVisibility(View.VISIBLE);

            for (Quest quest : activeQuests) {
                View cardView = createQuestCardView(quest, containerActive);
                containerActive.addView(cardView);
            }
        }

        // Available Quests
        for (Quest quest : availableQuests) {
            View cardView = createQuestCardView(quest, containerAvailable);
            containerAvailable.addView(cardView);
        }

        // Completed Quests
        if (completedQuests.isEmpty()) {
            if (cardEmptyCompleted != null) cardEmptyCompleted.setVisibility(View.VISIBLE);
            containerCompleted.setVisibility(View.GONE);
        } else {
            if (cardEmptyCompleted != null) cardEmptyCompleted.setVisibility(View.GONE);
            containerCompleted.setVisibility(View.VISIBLE);

            for (Quest quest : completedQuests) {
                View cardView = createQuestCardView(quest, containerCompleted);
                containerCompleted.addView(cardView);
            }
        }
    }

    /**
     * Displays the AI Quest Generator dialog allowing user preference input and quest preview.
     */
    private void showAiQuestGeneratorDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_ai_quest_generator_dialog, null);

        CheckBox cbPlants = dialogView.findViewById(R.id.cbPlants);
        CheckBox cbBirds = dialogView.findViewById(R.id.cbBirds);
        CheckBox cbPhoto = dialogView.findViewById(R.id.cbPhoto);
        CheckBox cbWalking = dialogView.findViewById(R.id.cbWalking);

        RadioGroup rgEnvironment = dialogView.findViewById(R.id.rgEnvironment);
        RadioGroup rgDuration = dialogView.findViewById(R.id.rgDuration);

        MaterialButton btnGenerate = dialogView.findViewById(R.id.btnGenerateAiQuest);
        LinearLayout layoutInputs = dialogView.findViewById(R.id.layoutAiInputs);
        LinearLayout layoutLoading = dialogView.findViewById(R.id.layoutAiLoading);
        LinearLayout layoutPreview = dialogView.findViewById(R.id.layoutAiPreview);

        TextView tvName = dialogView.findViewById(R.id.tvAiName);
        TextView tvCategory = dialogView.findViewById(R.id.tvAiCategory);
        TextView tvDuration = dialogView.findViewById(R.id.tvAiDuration);
        TextView tvXp = dialogView.findViewById(R.id.tvAiXp);
        TextView tvDesc = dialogView.findViewById(R.id.tvAiDescription);
        TextView tvSafety = dialogView.findViewById(R.id.tvAiSafetyNotes);
        TextView tvInference = dialogView.findViewById(R.id.tvAiInferenceTag);
        MaterialButton btnAcceptAi = dialogView.findViewById(R.id.btnAcceptAiQuest);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnGenerate.setOnClickListener(v -> {
            List<String> interests = new ArrayList<>();
            if (cbPlants.isChecked()) interests.add("Plants");
            if (cbBirds.isChecked()) interests.add("Birds");
            if (cbPhoto.isChecked()) interests.add("Photography");
            if (cbWalking.isChecked()) interests.add("Walking");
            if (interests.isEmpty()) interests.add("Nature");

            int minutes = 10;
            int checkedDurId = rgDuration.getCheckedRadioButtonId();
            if (checkedDurId == R.id.rb15m) minutes = 15;
            else if (checkedDurId == R.id.rb30m) minutes = 30;

            String envType = "Park / Garden";
            int checkedEnvId = rgEnvironment.getCheckedRadioButtonId();
            if (checkedEnvId == R.id.rbUrban) envType = "Urban Neighborhood";
            else if (checkedEnvId == R.id.rbStreet) envType = "Quiet Street";

            QuestGenerationRequest req = new QuestGenerationRequest(
                    progressionManager.getCurrentLevel(),
                    interests,
                    minutes,
                    envType
            );

            // Switch to loading state
            layoutInputs.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.VISIBLE);

            // Generate AI quest
            aiQuestService.generateQuest(req, new AiQuestService.AiCallback() {
                @Override
                public void onSuccess(Quest quest, String inferenceSource) {
                    layoutLoading.setVisibility(View.GONE);
                    layoutPreview.setVisibility(View.VISIBLE);

                    tvName.setText(quest.getName());
                    tvCategory.setText(quest.getCategory());
                    tvDuration.setText(quest.getEstimatedDuration());
                    tvXp.setText(getString(R.string.xp_reward_format, quest.getXpReward()));
                    tvDesc.setText(quest.getDescription());
                    tvSafety.setText(quest.getSafetyNotes());
                    tvInference.setText(getString(R.string.ai_inference_format, inferenceSource));

                    btnAcceptAi.setOnClickListener(v1 -> {
                        questRepository.saveAiQuest(quest);
                        questRepository.acceptQuest(quest.getId());
                        quest.setStatus(QuestStatus.ACTIVE);

                        Toast.makeText(MainActivity.this, R.string.ai_quest_accepted_toast, Toast.LENGTH_SHORT).show();

                        refreshMapQuestMarkers();
                        refreshQuestsTab();

                        dialog.dismiss();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    layoutLoading.setVisibility(View.GONE);
                    layoutInputs.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * Helper method to inflate and populate an item_quest_card view for a quest.
     */
    private View createQuestCardView(Quest quest, LinearLayout parent) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_quest_card, parent, false);

        TextView tvCategory = cardView.findViewById(R.id.tvCardCategory);
        TextView tvDuration = cardView.findViewById(R.id.tvCardDuration);
        TextView tvStatus = cardView.findViewById(R.id.tvCardStatus);
        TextView tvName = cardView.findViewById(R.id.tvCardName);
        TextView tvDesc = cardView.findViewById(R.id.tvCardDescription);
        TextView tvXp = cardView.findViewById(R.id.tvCardXp);
        MaterialButton btnViewMap = cardView.findViewById(R.id.btnCardViewMap);
        MaterialButton btnAccept = cardView.findViewById(R.id.btnCardAccept);
        MaterialButton btnComplete = cardView.findViewById(R.id.btnCardComplete);

        tvCategory.setText(quest.getCategory());
        tvDuration.setText(quest.getEstimatedDuration());
        tvName.setText(quest.getName());
        tvDesc.setText(quest.getDescription());
        tvXp.setText(getString(R.string.xp_reward_format, quest.getXpReward()));

        if (quest.getStatus() == QuestStatus.COMPLETED) {
            tvStatus.setText(R.string.status_completed);
            tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_light));
            btnAccept.setVisibility(View.GONE);
            btnComplete.setVisibility(View.GONE);
        } else if (quest.getStatus() == QuestStatus.ACTIVE) {
            tvStatus.setText(R.string.status_active);
            tvStatus.setBackgroundResource(R.drawable.bg_badge_gold);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.gold_rpg));
            btnAccept.setVisibility(View.GONE);
            btnComplete.setVisibility(View.VISIBLE);

            btnComplete.setOnClickListener(v -> showSelfReportConfirmationDialog(quest));
        } else { // AVAILABLE
            tvStatus.setText(R.string.status_available);
            tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_light));
            btnAccept.setVisibility(View.VISIBLE);
            btnComplete.setVisibility(View.GONE);

            btnAccept.setOnClickListener(v -> {
                questRepository.acceptQuest(quest.getId());
                quest.setStatus(QuestStatus.ACTIVE);
                Toast.makeText(this, R.string.quest_accepted_toast, Toast.LENGTH_SHORT).show();

                refreshMapQuestMarkers();
                refreshQuestsTab();
            });
        }

        // Focus map on this quest's marker and switch to Map tab
        btnViewMap.setOnClickListener(v -> {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_map);
            if (mapView != null) {
                GeoPoint point = new GeoPoint(quest.getLatitude(), quest.getLongitude());
                mapView.getController().animateTo(point);
                mapView.getController().setZoom(14.0);
            }
        });

        return cardView;
    }

    /**
     * Binds level, XP progress, daily streaks, equipped title, avatar, and completed stats to Character Sheet.
     */
    private void refreshCharacterTab() {
        int totalXp = progressionManager.getTotalXp();
        int currentLevel = progressionManager.getCurrentLevel();
        String equippedTitle = progressionManager.getEquippedTitle();
        int completedQuestsCount = questRepository.getCompletedQuestIds().size();
        int totalQuestsCount = questRepository.getAllQuests().size();

        // Bind Avatar
        int avatarResId = avatarManager.getEquippedAvatarResId();
        binding.imgAvatar.setImageResource(avatarResId);
        binding.imgAvatar.setOnClickListener(v -> showAvatarSelectionDialog());

        binding.tvCharacterName.setText(DEFAULT_NAME);
        binding.tvCharacterTitle.setText(equippedTitle);
        binding.tvLevelBadge.setText(getString(R.string.level_format, currentLevel));

        // Daily Streaks
        int currentStreak = streakManager.getCurrentStreak();
        int longestStreak = streakManager.getLongestStreak();
        binding.tvStreakText.setText(getString(R.string.streak_format, currentStreak, longestStreak));

        // XP Progress calculation
        int levelBaseXp = ProgressionManager.getLevelBaseXp(currentLevel);
        int nextLevelThreshold = ProgressionManager.getNextLevelThreshold(currentLevel);

        int xpInLevel = totalXp - levelBaseXp;
        int xpLevelSpan = nextLevelThreshold - levelBaseXp;

        binding.tvXpProgress.setText(getString(R.string.xp_format, totalXp, nextLevelThreshold));
        binding.progressBarXp.setMax(xpLevelSpan);
        binding.progressBarXp.setProgress(Math.min(xpInLevel, xpLevelSpan));

        // Quests Completed Counter
        binding.tvCompletedQuestsCount.setText(getString(R.string.completed_quests_count, completedQuestsCount, totalQuestsCount));

        // Unlocked Titles List / Selector
        refreshUnlockedTitlesSelector(equippedTitle);

        // Proximity Geofencing Status Card
        if (binding.tvProximityStatus != null && binding.btnConfigureProximity != null) {
            boolean proxEnabled = proximityManager.isProximityVerificationEnabled();
            int radius = proximityManager.getProximityRadiusMeters();
            String proxStatusStr = proxEnabled
                    ? getString(R.string.proximity_status_on, radius)
                    : getString(R.string.proximity_status_off);
            binding.tvProximityStatus.setText(getString(R.string.proximity_enabled_label, proxStatusStr));

            binding.btnConfigureProximity.setOnClickListener(v -> showProximityConfigurationDialog());
        }

        // App Theme Status Card
        if (binding.tvCurrentThemeStatus != null && binding.btnSwitchTheme != null) {
            String currentMode = themeManager.getThemeMode();
            String modeName;
            if (com.example.touchgrass.theme.ThemeManager.MODE_LIGHT.equals(currentMode)) {
                modeName = getString(R.string.theme_light_name);
            } else if (com.example.touchgrass.theme.ThemeManager.MODE_SYSTEM.equals(currentMode)) {
                modeName = getString(R.string.theme_system_name);
            } else {
                modeName = getString(R.string.theme_dark_name);
            }
            binding.tvCurrentThemeStatus.setText(getString(R.string.theme_current_label, modeName));

            binding.btnSwitchTheme.setOnClickListener(v -> showThemeSelectionDialog());
        }
    }

    /**
     * Displays a dialog allowing user to switch between Dark Forest, Daylight Meadow, or System theme.
     */
    private void showThemeSelectionDialog() {
        String[] options = {
                getString(R.string.theme_dark_name),
                getString(R.string.theme_light_name),
                getString(R.string.theme_system_name)
        };

        new AlertDialog.Builder(this)
                .setTitle("Select App Theme")
                .setItems(options, (dialog, which) -> {
                    String chosenMode;
                    if (which == 0) {
                        chosenMode = com.example.touchgrass.theme.ThemeManager.MODE_DARK;
                    } else if (which == 1) {
                        chosenMode = com.example.touchgrass.theme.ThemeManager.MODE_LIGHT;
                    } else {
                        chosenMode = com.example.touchgrass.theme.ThemeManager.MODE_SYSTEM;
                    }

                    themeManager.setThemeMode(chosenMode);
                    if (mapView != null) {
                        com.example.touchgrass.theme.ThemeManager.coordinateMapTheme(
                                mapView, com.example.touchgrass.theme.ThemeManager.isNightModeActive(this));
                    }
                    Toast.makeText(this, "Theme set to " + options[which], Toast.LENGTH_SHORT).show();
                    refreshCharacterTab();
                })
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Displays a dialog allowing user to configure GPS proximity verification radius or disable it.
     */
    private void showProximityConfigurationDialog() {
        String[] options = {
                "Disabled (Relaxed Exploration)",
                "Tight Proximity (200 meters)",
                "Standard Radius (500 meters)",
                "Wide Area (1000 meters)"
        };

        new AlertDialog.Builder(this)
                .setTitle("GPS Proximity Geofencing")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        proximityManager.setProximityVerificationEnabled(false);
                        Toast.makeText(this, "Proximity Check disabled (Relaxed Mode)", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        proximityManager.setProximityVerificationEnabled(true);
                        proximityManager.setProximityRadiusMeters(200);
                        Toast.makeText(this, "Proximity Check enabled: 200m radius", Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        proximityManager.setProximityVerificationEnabled(true);
                        proximityManager.setProximityRadiusMeters(500);
                        Toast.makeText(this, "Proximity Check enabled: 500m radius", Toast.LENGTH_SHORT).show();
                    } else if (which == 3) {
                        proximityManager.setProximityVerificationEnabled(true);
                        proximityManager.setProximityRadiusMeters(1000);
                        Toast.makeText(this, "Proximity Check enabled: 1000m radius", Toast.LENGTH_SHORT).show();
                    }
                    refreshCharacterTab();
                })
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Displays a selection dialog allowing user to choose a character avatar.
     */
    private void showAvatarSelectionDialog() {
        List<AvatarManager.AvatarOption> options = avatarManager.getAvailableAvatarOptions();
        String[] optionNames = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            optionNames[i] = options.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Character Avatar")
                .setItems(optionNames, (dialog, which) -> {
                    AvatarManager.AvatarOption selected = options.get(which);
                    avatarManager.setEquippedAvatarResId(selected.resId);
                    binding.imgAvatar.setImageResource(selected.resId);
                    Toast.makeText(this, "Avatar updated to " + selected.name, Toast.LENGTH_SHORT).show();
                    refreshHomeTab();
                })
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Populates the Titles card list allowing user to equip unlocked titles and view locked title requirements.
     */
    private void refreshUnlockedTitlesSelector(String currentEquippedTitle) {
        LinearLayout containerTitles = binding.containerUnlockedTitles;
        if (containerTitles == null) return;

        containerTitles.removeAllViews();
        List<String> unlockedTitles = progressionManager.getUnlockedTitles();

        // All title milestones
        String[] allTitles = {
                ProgressionManager.TITLE_DEFAULT,
                ProgressionManager.TITLE_GRASS_TOUCHER,
                ProgressionManager.TITLE_LEAF_ENTHUSIAST,
                ProgressionManager.TITLE_PIGEON_DIPLOMAT,
                ProgressionManager.TITLE_GOBLIN_BOTANIST
        };

        String[] lockRequirements = {
                "Default Starter Title",
                "Requires 1 Completed Quest",
                "Requires 3 Completed Quests",
                "Requires 5 Completed Quests",
                "Requires 10 Completed Quests"
        };

        for (int i = 0; i < allTitles.length; i++) {
            String title = allTitles[i];
            boolean isUnlocked = unlockedTitles.contains(title);
            boolean isEquipped = title.equals(currentEquippedTitle);

            View cardView = LayoutInflater.from(this).inflate(R.layout.item_title_card, containerTitles, false);

            TextView tvTitleName = cardView.findViewById(R.id.tvTitleName);
            TextView tvEquippedTag = cardView.findViewById(R.id.tvEquippedTag);
            MaterialButton btnEquip = cardView.findViewById(R.id.btnEquipTitle);

            tvTitleName.setText(title);

            if (isUnlocked) {
                if (isEquipped) {
                    tvEquippedTag.setVisibility(View.VISIBLE);
                    tvEquippedTag.setText(R.string.equipped_tag);
                    btnEquip.setVisibility(View.GONE);
                } else {
                    tvEquippedTag.setVisibility(View.GONE);
                    btnEquip.setVisibility(View.VISIBLE);
                    btnEquip.setEnabled(true);
                    btnEquip.setText(R.string.btn_equip_title);

                    btnEquip.setOnClickListener(v -> {
                        progressionManager.setEquippedTitle(title);
                        Toast.makeText(this, getString(R.string.title_equipped_toast, title), Toast.LENGTH_SHORT).show();
                        refreshHomeTab();
                        refreshCharacterTab();
                    });
                }
            } else { // Locked Title
                tvEquippedTag.setVisibility(View.VISIBLE);
                tvEquippedTag.setText(getString(R.string.locked_title_format, lockRequirements[i]));
                tvEquippedTag.setBackgroundResource(R.drawable.bg_badge_green);
                tvEquippedTag.setTextColor(ContextCompat.getColor(this, R.color.text_disabled));
                btnEquip.setVisibility(View.GONE);
            }

            containerTitles.addView(cardView);
        }
    }

    /**
     * Populates and renders completed quest entries in the Adventure Journal tab.
     */
    private void refreshJournalTab() {
        View journalLayout = binding.layoutJournalTab;
        MaterialCardView cardEmptyJournal = journalLayout.findViewById(R.id.cardEmptyJournal);
        LinearLayout containerEntries = journalLayout.findViewById(R.id.containerJournalEntries);

        if (containerEntries == null) return;

        containerEntries.removeAllViews();
        List<JournalEntry> entries = journalManager.getAllJournalEntries();

        if (entries.isEmpty()) {
            if (cardEmptyJournal != null) cardEmptyJournal.setVisibility(View.VISIBLE);
            containerEntries.setVisibility(View.GONE);
        } else {
            if (cardEmptyJournal != null) cardEmptyJournal.setVisibility(View.GONE);
            containerEntries.setVisibility(View.VISIBLE);

            for (JournalEntry entry : entries) {
                View cardView = LayoutInflater.from(this).inflate(R.layout.item_journal_card, containerEntries, false);

                TextView tvDate = cardView.findViewById(R.id.tvJournalDate);
                TextView tvXp = cardView.findViewById(R.id.tvJournalXp);
                TextView tvName = cardView.findViewById(R.id.tvJournalQuestName);
                TextView tvCategory = cardView.findViewById(R.id.tvJournalCategory);
                TextView tvUnlockedTitle = cardView.findViewById(R.id.tvJournalUnlockedTitle);
                LinearLayout layoutReflection = cardView.findViewById(R.id.layoutJournalReflection);
                TextView tvReflection = cardView.findViewById(R.id.tvJournalReflection);
                MaterialButton btnAddNote = cardView.findViewById(R.id.btnJournalAddNote);

                tvDate.setText(entry.getFormattedDate());
                tvXp.setText(getString(R.string.xp_reward_format, entry.getXpEarned()));
                tvName.setText(entry.getQuestName());
                tvCategory.setText(entry.getCategory());

                if (entry.getUnlockedTitle() != null && !entry.getUnlockedTitle().isEmpty()) {
                    tvUnlockedTitle.setVisibility(View.VISIBLE);
                    tvUnlockedTitle.setText(getString(R.string.unlocked_title_format, entry.getUnlockedTitle()));
                } else {
                    tvUnlockedTitle.setVisibility(View.GONE);
                }

                if (entry.getReflection() != null && !entry.getReflection().isEmpty()) {
                    layoutReflection.setVisibility(View.VISIBLE);
                    tvReflection.setText(entry.getReflection());
                    btnAddNote.setText(R.string.btn_journal_edit_note);
                } else {
                    layoutReflection.setVisibility(View.GONE);
                    btnAddNote.setText(R.string.btn_journal_add_note);
                }

                btnAddNote.setOnClickListener(v -> showReflectionInputDialog(entry.getQuestId(), entry.getQuestName()));

                containerEntries.addView(cardView);
            }
        }
    }

    /**
     * Handles location request when user taps the My Location button.
     */
    private void handleMyLocationClick() {
        if (hasLocationPermission()) {
            enableAndCenterUserLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Checks if location permission is granted.
     */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Enables user location overlay and centers the map on current coordinates if available.
     */
    private void enableAndCenterUserLocation() {
        if (myLocationOverlay == null) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            myLocationOverlay.enableMyLocation();
            mapView.getOverlays().add(myLocationOverlay);
        }

        myLocationOverlay.runOnFirstFix(() -> {
            GeoPoint userLoc = myLocationOverlay.getMyLocation();
            if (userLoc != null && mapView != null) {
                runOnUiThread(() -> {
                    mapView.getController().animateTo(userLoc);
                    mapView.getController().setZoom(14.0);
                });
            }
        });

        // Trigger animation if location already known
        GeoPoint myLoc = myLocationOverlay.getMyLocation();
        if (myLoc != null) {
            mapView.getController().animateTo(myLoc);
            mapView.getController().setZoom(14.0);
        } else {
            Toast.makeText(this, R.string.map_location_searching, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Checks for internet availability and shows a friendly offline banner if disconnected.
     */
    private void checkNetworkAndSetOfflineBanner() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isConnected = false;
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            isConnected = capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }

        if (!isConnected && bannerOffline != null) {
            bannerOffline.setVisibility(View.VISIBLE);
        } else if (bannerOffline != null) {
            bannerOffline.setVisibility(View.GONE);
        }
    }

    /**
     * Configures tab switching for the bottom navigation bar.
     */
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            binding.layoutHomeTab.setVisibility(itemId == R.id.nav_home ? View.VISIBLE : View.GONE);
            binding.layoutMapTab.setVisibility(itemId == R.id.nav_map ? View.VISIBLE : View.GONE);
            binding.layoutQuestsTab.setVisibility(itemId == R.id.nav_quests ? View.VISIBLE : View.GONE);
            binding.layoutCharacterTab.setVisibility(itemId == R.id.nav_character ? View.VISIBLE : View.GONE);
            binding.layoutJournalTab.setVisibility(itemId == R.id.nav_journal ? View.VISIBLE : View.GONE);

            if (itemId == R.id.nav_home) {
                refreshHomeTab();
            } else if (itemId == R.id.nav_map && mapView != null) {
                mapView.onResume();
            } else if (itemId == R.id.nav_quests) {
                refreshQuestsTab();
            } else if (itemId == R.id.nav_character) {
                refreshCharacterTab();
            } else if (itemId == R.id.nav_journal) {
                refreshJournalTab();
            }

            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (binding.bottomNavigation.getSelectedItemId() != R.id.nav_home) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            com.example.touchgrass.theme.ThemeManager.coordinateMapTheme(
                    mapView, com.example.touchgrass.theme.ThemeManager.isNightModeActive(this));
        }
        checkNetworkAndSetOfflineBanner();
        refreshMapQuestMarkers();
        refreshHomeTab();
        refreshQuestsTab();
        refreshCharacterTab();
        refreshJournalTab();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
