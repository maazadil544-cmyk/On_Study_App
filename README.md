# ON Study - Pakistan Academic Guides & Textbooks (Class 1-12)

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="ON Study Logo" width="100" />
</p>

<p align="center">
  <b>Complete Educational Resource Hub for KPK, Punjab, Sindh & Balochistan Boards</b><br>
  Built with Modern Android (Jetpack Compose, Kotlin, Room Database & Firebase Realtime Database Sync)
</p>

<p align="center">
  <a href="#-download-apk"><img src="https://img.shields.io/badge/Download-Latest%20APK-brightgreen?style=for-the-badge&logo=android" alt="Download APK"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-blue?style=for-the-badge&logo=android" alt="Android Version">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-purple?style=for-the-badge&logo=kotlin" alt="Kotlin Compose">
</p>

---

## 📥 Download APK (براہ راست ڈاؤن لوڈ)

Users can download the application APK directly using any of the following methods:

### Method 1: GitHub Releases (Recommended)
1. Go to the **[Releases](../../releases)** section of this repository.
2. Click on the latest release tag (e.g. `v1.0.0`).
3. Under **Assets**, click on **`app-debug.apk`** or **`ON-Study.apk`** to download and install directly on your Android phone.

### Method 2: GitHub Actions Automated Build
Every time code is pushed to this repository, GitHub automatically compiles the latest APK:
1. Go to the **[Actions](../../actions)** tab above.
2. Click on the latest workflow run named **`Build & Release Android APK`**.
3. Scroll down to the **Artifacts** section at the bottom.
4. Click **`ON-Study-App-APK`** to download the latest compiled APK zip file.

---

## ✨ Features (خصوصیات)

- **📚 All Provinces & Boards Covered:**
  - KPK (Khyber Pakhtunkhwa Textbook Board)
  - Punjab (Punjab Curriculum & Textbook Board)
  - Sindh (Sindh Textbook Board)
  - Balochistan (Balochistan Textbook Board)
  - Federal / National Curriculum
- **📖 Class 1 to 12 Materials:** Complete textbooks, solved guide notes, past papers, model papers, and pairing schemes.
- **🔄 Firebase Realtime Database Cloud Sync:**
  - Google Drive links and new books added via Admin Panel sync across all devices in real-time.
  - Offline-first caching with local Room database.
- **📑 In-App PDF Reader & Offline Downloader:**
  - High-speed direct downloading with resume and progress indicators.
  - Built-in PDF reader with bookmarks, night mode, jump to page, and page search.
- **🛠️ Integrated Admin Panel:**
  - Add, edit, or delete books and past papers.
  - Direct Google Drive link parser & validator.
  - Broadcast board news, date sheets, and result announcements.

---

## 🚀 How to Push to GitHub (ریپوزٹری میں پش کرنے کا طریقہ)

If you are pushing this code to your own GitHub account:

```bash
# 1. Initialize git (if not already done)
git init

# 2. Add all project files
git add .

# 3. Commit the changes
git commit -m "Initial commit - ON Study Android App with Firebase Realtime Database"

# 4. Link your remote repository
git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPOSITORY_NAME>.git

# 5. Push to GitHub
git branch -M main
git push -u origin main
```

---

## 📦 How to Create a Release with Direct Download Link

To provide users with a 1-click download button directly on GitHub:
1. Open your repository on GitHub.
2. Click **Create a new release** (under Releases on the right sidebar).
3. Set tag name as `v1.0.0` and title as `ON Study v1.0.0 Release`.
4. Drag and drop your built `.apk` file into the binary attachment box.
5. Click **Publish release**.
6. Users can now download the APK directly with 1 click!

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Local Persistence:** Room Database with KSP
- **Cloud Backend:** Firebase Realtime Database & REST Engine
- **Networking & File I/O:** OkHttp3 & Okio
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
