# CineStreamOTT

CineStreamOTT is a native Android OTT streaming application built with Kotlin, Firebase, ExoPlayer/Media3, Room, MVVM, ViewBinding, and Material Components. The app supports free and premium content, banners, reels, downloads, watch history, Picture-in-Picture playback, Firebase Crashlytics, and an upgraded admin panel for managing movies, users, banners, reels, actors, and subscription requests.

## Current Release

- Version name: `3.1.0`
- Version code: `16`
- Package: `com.ottapp.moviestream`
- Minimum SDK: `24`
- Target SDK: `34`
- Kotlin: `1.9.22`
- Android Gradle Plugin: `8.2.2`
- Gradle wrapper: `8.2`

## Major Features

### User App

- Firebase Authentication with user profile support
- Firebase Realtime Database powered movie, banner, reel, actor, user, and subscription data
- Home screen with cached hero banners, trending movies, category rows, continue watching, search shortcut, and reels shortcut
- Movie detail screen with Bangla-friendly font rendering and lighter shadow overlays
- Media3 ExoPlayer video playback with resume position, playback speed, gesture controls, quality support, and Picture-in-Picture
- Automatic PiP entry when the user leaves while playback is active
- Free/premium subscription gating with pending, premium, free, and blocked account states
- Background download support through WorkManager/foreground service
- Offline playback and cached movie/banner data
- Search with retry-on-resume and token-based multi-word matching
- Watchlist/profile/download sections
- Firebase Crashlytics and Analytics integration
- Room database scaffolding for local movie/banner persistence
- Improved accessibility content descriptions on key media actions/images

### Seven Newly Added/Upgraded Features

1. Android SDK-aware GitHub Actions CI/CD workflow for APK artifacts
2. Admin movie sorting by newest, rating, title, free-first, and premium-first
3. Admin movie data-quality health report for missing video, poster, category, download, and rating data
4. Admin movie report sharing/export through Android share sheet
5. Admin user analytics cards for total, active premium, and risky users
6. Admin user filtering by all, premium, free, blocked, and expired subscriptions
7. Admin payment request filtering and export report for pending, approved, and rejected payments

### Admin Panel

Admin entry: `AdminActivity`

Tabs:

- Movies: list, search, add/edit/delete, dashboard counts, sorting, refresh, health report, shareable movie report
- Banners: list, add/edit/delete banner data
- Reels: list, add/edit/delete short video reels
- Users: search, premium/free/block controls, password reset email, extension dialog, analytics, filters, shareable report
- Payments: approve/reject subscription requests, status filtering, pending/approved/rejected counts, shareable payment report
- Actors: manage actor records used by movie detail/profile screens

The admin top tabs now refresh live counts for movies, banners, reels, users, and pending payment requests. The toolbar subtitle shows premium user count when available.

## Architecture

The app follows a pragmatic MVVM structure:

```text
app/src/main/java/com/ottapp/moviestream/
├── adapter/                 RecyclerView adapters
├── data/
│   ├── local/               Room entities, DAO, AppDatabase
│   ├── model/               Movie, Banner, Reel, User, Actor, Download models
│   └── repository/          Firebase-backed repositories
├── service/                 Download foreground/background service
├── ui/
│   ├── admin/               AdminActivity and admin fragments
│   ├── detail/              Movie detail UI
│   ├── download/            Downloaded movies UI
│   ├── home/                Home screen and HomeViewModel
│   ├── movies/              Movie browsing
│   ├── player/              PlayerActivity using Media3 ExoPlayer
│   ├── profile/             Profile/subscription UI
│   ├── reels/               Reels viewing UI
│   ├── search/              SearchFragment/SearchViewModel
│   └── watchlist/           Watchlist UI
└── util/                    Constants, cache, helpers, managers
```

## Firebase Realtime Database Structure

```text
movies/
  {movieId}/
    id: string
    title: string
    description: string
    bannerImageUrl: string
    detailThumbnailUrl: string
    videoStreamUrl: string
    downloadUrl: string
    category: string
    imdbRating: number
    trending: boolean
    testMovie: boolean
    year: number
    duration: string
    actorIds: string[]
    downloads:
      - quality: string
        url: string
        size: string

banners/
  {bannerId}/
    id: string
    imageUrl: string
    title: string
    category: string
    imdbRating: number
    testMovie: boolean
    movieId: string

reels/
  {reelId}/
    id: string
    movieId: string
    title: string
    videoUrl: string
    thumbnailUrl: string

users/
  {uid}/
    uid: string
    email: string
    displayName: string
    photoUrl: string
    subscriptionStatus: free | premium | pending | blocked
    subscriptionExpiry: unix timestamp in milliseconds

subscriptions/
  {uid}/
    transactionId: string
    deviceId: string
    status: PENDING | APPROVED | REJECTED
    submittedAt: unix timestamp in milliseconds
    expiry: unix timestamp in milliseconds

movie_requests/
  {requestId}/
    title: string
    count: number
```

## Important App Constants

File: `app/src/main/java/com/ottapp/moviestream/util/Constants.kt`

- `DB_MOVIES = "movies"`
- `DB_USERS = "users"`
- `DB_SUBSCRIPTIONS = "subscriptions"`
- `DB_REQUESTS = "movie_requests"`
- `DB_BANNERS = "banners"`
- `DB_REELS = "reels"`
- `SUB_FREE = "free"`
- `SUB_PREMIUM = "premium"`
- `SUB_PENDING = "pending"`
- `SUB_BLOCKED = "blocked"`
- `SUBSCRIPTION_DURATION_MS = 30 days`
- `PENDING_ACCESS_DURATION_MS = 6 hours`
- `PAYMENT_NUMBER` stores the bKash/Nagad payment number

## Local Build

Requirements:

- JDK 17
- Android SDK with API 34 and Build Tools 34.0.0
- `app/google-services.json`

Build debug APK:

```bash
./gradlew assembleDebug --no-daemon
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build release APK:

```bash
KEYSTORE_PATH=../keystore/cinestream-release.jks \
RELEASE_STORE_PASSWORD=your_store_password \
RELEASE_KEY_ALIAS=cinestream_release \
RELEASE_KEY_PASSWORD=your_key_password \
./gradlew assembleRelease --no-daemon
```

Output:

```text
app/build/outputs/apk/release/app-release.apk
```

## GitHub Actions CI/CD

Workflow file:

```text
.github/workflows/build-apk.yml
```

Triggers:

- Push to `main`, `master`, or `develop`
- Pull request to `main` or `master`
- Manual `workflow_dispatch`

CI steps:

1. Checkout repository
2. Set up JDK 17
3. Set up Android SDK
4. Cache Gradle dependencies
5. Build debug APK
6. Optionally decode release keystore from secrets
7. Optionally build release APK if release secrets exist
8. Upload APK artifacts
9. Create GitHub Release for tags

Required secrets for signed release builds:

- `RELEASE_KEYSTORE_BASE64` — base64 encoded keystore file
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS` — optional, defaults to `cinestream_release`
- `RELEASE_KEY_PASSWORD`

Debug APK builds do not require release signing secrets.

## Recent Bug Fixes

- Fixed home banner disappearance after app re-entry by caching banners separately
- Fixed Bangla font rendering by replacing unsupported serif font usage with `sans-serif-medium`
- Reduced heavy shadow overlays on home banner and detail player poster
- Fixed bottom navigation text color and icon/text spacing
- Repaired search by adding retry loading and better multi-word matching
- Added Crashlytics initialization with safe Firebase checks
- Added Room database entities/DAO/database for future offline-first persistence
- Added PiP auto-enter behavior
- Added key accessibility content descriptions
- Hardened ProGuard rules for Firebase, Room, Crashlytics, Media3, Glide, ViewBinding, and app models

## Developer Notes

- Do not commit real signing passwords or private keystore passwords.
- `google-services.json` is required for Firebase builds.
- Debug builds are suitable for testing and direct installation.
- Release builds should be signed through GitHub Actions secrets or a secure local environment.
- Room classes currently provide database scaffolding; repositories can be migrated from SharedPreferences cache to Room read-through caching as a future improvement.
