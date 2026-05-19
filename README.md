# 📱 GitHub Store

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge" alt="Android">
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue?style=for-the-badge" alt="Compose">
  <img src="https://img.shields.io/badge/API-GitHub%20REST%20v3-black?style=for-the-badge" alt="GitHub API">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="MIT License">
</p>

**GitHub Store** is a modern Android application that transforms GitHub into an app store. Browse, discover, and install open-source applications directly from GitHub repositories with a beautiful Material Design 3 interface inspired by Google Play and App Store.

## ✨ Features

- **🔍 Browse & Search** — Explore trending repositories, filter by platform (Android, Desktop, Linux, iOS), or search for anything on GitHub
- **📥 One-Click Install** — Download and install APKs directly from GitHub releases
- **❤️ Favorites** — Save your favorite repositories for quick access
- **📤 Share** — Share any repository with friends
- **🎯 Infinite Scrolling** — Keep browsing as long as there are results
- **⚙️ Settings** — Customize proxy, theme, language, download folder, and GitHub token
- **🌙 Dark Mode** — Beautiful dark and light themes with Material You support
- **🌐 Multi-Language** — Full support for English and Russian
- **📊 Rate Limit Info** — Track your GitHub API usage in real-time
- **🔒 No Auth Required** — Works without login (60 requests/hour), up to 5000 with a personal token

## 📸 Screenshots

| Home Screen | App Details | Settings |
|---|---|---|
| Browse trending repos with category filters | View repo info, download releases | Theme, language, proxy, and more |

## 🏗️ Tech Stack

- **Kotlin** — Primary programming language
- **Jetpack Compose** — Modern declarative UI toolkit
- **Material Design 3** — Beautiful, dynamic theming
- **GitHub REST API v3** — Repository search and data
- **OkHttp** — HTTP client with proxy support
- **Coil** — Async image loading
- **DataStore** — Persistent preferences storage
- **Navigation Compose** — In-app navigation

## 📋 Requirements

- Android 8.0 (API 26) or higher
- Internet connection
- Optional: GitHub Personal Access Token for higher rate limits

## 🚀 Installation

1. Download the latest APK from [Releases](../../releases)
2. Enable "Install from Unknown Sources" in your device settings
3. Install the APK

## ⚙️ Usage

### Without Authentication
The app works out of the box with GitHub's public API rate limit of **60 requests per hour**.

### With GitHub Token (Recommended)
1. Go to **Settings → GitHub Token**
2. Enter your GitHub Personal Access Token
3. Enjoy **5,000 requests per hour**

To create a token: GitHub → Settings → Developer settings → Personal access tokens → Generate new token

## 🛠️ Building from Source

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK 34

### Build Steps
```bash
# Clone the repository
git clone https://github.com/kevinriverrrr-sudo/GitHubStore.git
cd GitHubStore

# Build debug APK
./gradlew assembleDebug

# Build signed release APK
./gradlew assembleRelease
```

APKs will be in `app/build/outputs/apk/`

## 📁 Project Structure

```
app/src/main/java/com/githubstore/
├── data/
│   ├── api/          # GitHub API client (OkHttp + Gson)
│   ├── model/        # Data models (Repo, Release, Asset, etc.)
│   └── repository/   # Repository pattern layer
├── ui/
│   ├── components/   # Reusable Compose components
│   ├── screens/      # Main app screens
│   ├── theme/        # Material 3 theme (colors, typography)
│   └── viewmodel/    # ViewModels for state management
├── util/             # Utility classes (Favorites, Settings)
├── GitHubStoreApp.kt # Application class
└── MainActivity.kt   # Main entry point
```

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- [GitHub REST API](https://docs.github.com/en/rest)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
