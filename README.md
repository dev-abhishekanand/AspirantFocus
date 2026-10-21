# 🎯 Aspirant Focus (Open-Source ReGain Alternative)

**Aspirant Focus** is a high-discipline Android application built specifically for competitive exam aspirants (JEE, NEET, UPSC, CA, GATE, Coding) who struggle with phone addiction (spending 10–12 hours a day on mindless scrolling) and need to aggressively reclaim that time for focused studies.

It replicates the most critical **PRO / Paid features** of apps like ReGain completely free, with zero ads and zero paywalls.

---

## ⚡ Core Features Built

### 1. 🛡️ Targeted Reels & Shorts Blocker (The Deal Breaker)
* **Instagram Reels Blocker:** Detects the moment you enter the Reels tab or vertical video player and immediately executes a back action and displays a mindful pause overlay. Your direct messages (DMs) and search remain fully functional for study discussions.
* **YouTube Shorts Blocker:** Blocks vertical short-form shelves and players while preserving access to long-form lectures and tutorials.
* **YouTube Study Mode:** Shields you from addictive homepage recommendation algorithms.

### 2. 🔒 Strict Mode & Anti-Uninstall (Zero Loopholes)
* **No Early Quit:** Once a strict study session starts (e.g. 25m, 50m, 90m), the countdown cannot be canceled or stopped until it finishes.
* **Anti-Uninstall Guard:** Intercepts package installer screens. If you try to uninstall the app in a moment of craving, you are immediately kicked back to the home screen.
* **Settings Lockout:** Prevents opening Phone Settings -> Apps -> Aspirant Focus to force stop or disable accessibility permissions during an active session.
* **Reboot Persistence:** If you restart your phone to bypass the timer, the `BootReceiver` automatically resumes the locked countdown upon boot.

### 3. 🍅 Pomodoro & Subject-Tagged Focus Timer
* Supports standard countdown, stopwatch, and customizable **Pomodoro cycles** (e.g., 25-minute deep focus + 5-minute automated rest breaks).
* Tag sessions by academic subjects (*Physics*, *Mathematics*, *Chemistry*, *General Studies*, *Coding*, *Revision*).
* Persistent sticky foreground notification displays remaining time live in the notification drawer.

### 4. 🌐 Website & Adult Content Filtering
* Real-time browser URL inspection across Google Chrome, Brave, Firefox, Opera, and Microsoft Edge.
* Automatically blocks adult sites, gambling, and web versions of short-form feeds (`instagram.com/reels`, `m.youtube.com/shorts`, `tiktok.com`).

### 5. 📊 Reality Check Analytics
* Eye-opening daily comparison between **Total Phone Screen Time** (e.g., 11 hours) vs **Focused Study Time** (e.g., 2 hours).
* Per-app breakdown identifying which apps are your biggest time sinkholes.
* Local historical session logs stored via SQLite Room database.

---

## 🏗️ Architecture & Technology Stack

* **Language:** Kotlin 2.0+
* **UI Toolkit:** Jetpack Compose + Material 3 (Minimalist, dark-mode focused aesthetic)
* **Background Engine:** Android `AccessibilityService` (`flagRetrieveInteractiveWindows` & `canRetrieveWindowContent`)
* **Session Manager:** Android Foreground Service with live `NotificationCompat` updates
* **Local Persistence:** Room Database + Shared Preferences StateFlows
* **Usage Aggregator:** Android `UsageStatsManager`

---

## 🚀 How to Build and Install on Your Android Phone

### Prerequisites
1. An Android phone running **Android 8.0 (Oreo / API 26) or higher** (Android 12, 13, 14, 15 fully supported).
2. A computer with [Android Studio](https://developer.android.com/studio) installed.

### Step 1: Open in Android Studio
1. Launch Android Studio.
2. Select **Open** and select the folder:
   ```
   /Users/abhishekanand/.gemini/antigravity/scratch/AspirantFocus
   ```
3. Let Gradle sync the dependencies automatically.

### Step 2: Connect Your Phone
1. On your Android phone, enable **Developer Options**:
   * Go to **Settings** -> **About Phone** -> Tap **Build Number** 7 times.
2. In Developer Options, enable **USB Debugging**.
3. Connect your phone to your computer with a USB cable.

### Step 3: Run / Install
1. In Android Studio, select your connected device in the top toolbar.
2. Click the green **Run (Play)** button, or open terminal and execute:
   ```bash
   ./gradlew installDebug
   ```

*(Alternatively, to generate a standalone APK file to share or install manually, run `./gradlew assembleDebug`. The APK will be saved at `app/build/outputs/apk/debug/app-debug.apk`).*

---

## ⚙️ First-Time Phone Setup (Permissions)

When you first launch the app, click the **"Permissions"** button at the top right to enable the four required system settings:

1. **Accessibility Service (`Aspirant Focus`):**
   * *Why:* Inspects window node trees to block Reels/Shorts and prevent uninstall/settings tampering during Strict Mode.
2. **Display Over Other Apps:**
   * *Why:* Allows the mindful pause and reality check overlay to appear over blocked apps.
3. **Usage Access:**
   * *Why:* Reads daily screen time so you can track your 11–12 hour phone usage drops.
4. **Ignore Battery Optimizations:**
   * *Why:* Ensures Android's battery saver doesn't terminate your active study timer in the background.

---

## 🎯 Final Note for the Aspirant
> *"11–12 hours on a smartphone is 300+ hours a month lost to short-form loops. Aspirant Focus was designed as a fortress so your rational self wins over your impulsive self. Treat every study block as non-negotiable."*
