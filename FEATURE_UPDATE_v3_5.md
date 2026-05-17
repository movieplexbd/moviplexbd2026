# CineStream OTT — v3.5.0 Feature Update

## ✅ Smart Features

### 🤖 AI Recommendation
- **File:** `AIRecommendationManager.kt`
- Watch history দেখে genre/category score করে top movies suggest করে
- Home screen এ "🤖 আপনার জন্য" section যোগ হয়েছে
- Watch history নেই? Trending movies দেখাবে
- `getSimilarMovies()` — detail page এ similar movie suggest করতে পারবেন

### 📱 Continue Watching Cross-Device Sync
- **File:** `ContinueWatchingSyncManager.kt`
- Firebase Realtime DB তে progress save হয়
- অন্য device এ login করলে progress pull হয়ে আসবে
- Offline friendly — Internet না থাকলে local এ save করে
- Player থেকে automatically sync হয়

### ⬇️ Offline Download Queue
- **File:** `DownloadService.kt` (updated)
- একসাথে সর্বোচ্চ **3টি** download চলবে
- বেশি হলে automatic queue তে রাখবে, আগেরটা শেষ হলে পরেরটা শুরু হবে
- Queue notification দেখাবে

### 🎬 Download Quality Selector
- Download button চাপলে Quality dialog আসবে: Auto / 1080p / 720p / 480p / 360p
- CDN এর URL structure অনুযায়ী quality parameter append করে

---

## ✅ Social Features

### 🔗 Movie Share (OpenGraph)
- **File:** `MovieDetailFragment.kt` — `shareMovieWithThumbnail()`
- WhatsApp/Facebook share করলে deep link যাবে: `cinestream.app/movie/{id}`
- Server এ `og:image`, `og:title`, `og:description` meta tag থাকলে thumbnail দেখাবে
- Share text এ GitHub release link ও থাকে

---

## ✅ Bug Fixes / Incomplete Features

### 🎉 Watch Party (Fully Implemented)
- **Files:** `WatchPartyManager.kt`, `WatchPartyDialog.kt`, `dialog_watch_party.xml`
- Firebase Realtime DB ব্যবহার করে real-time sync
- Host: room তৈরি করে 6-char code পায়, clipboard এ copy হয়
- Guest: code দিয়ে join করে, Host এর position/play/pause sync হয়
- 5 সেকেন্ড interval এ Host state push করে, Guest 3 সেকেন্ড diff হলে seek করে
- Room 4 ঘণ্টা পরে auto-expire

### 📋 Admin Series Editor
- **Files:** `AdminSeriesEditorActivity.kt`, `AdminEpisodeAdapter.kt`
- Admin panel > Series movie > "📋 Episodes" button
- Season add করা যায়, episode add/edit/delete করা যায়
- `AdminMovieAdapter.kt` — Series movie তে "📋 Episodes" button দেখায়

### 🔄 GitHub Auto-Update (Force/Soft)
- **Files:** `GitHubUpdateManager.kt`, `UpdateActivity.kt`, `SplashActivity.kt`
- `GITHUB_OWNER` এবং `GITHUB_REPO` পরিবর্তন করুন `GitHubUpdateManager.kt` এ
- GitHub Releases এ APK upload করলে auto-detect করবে
- Major version bump (e.g. 3.x → 4.x) = FORCE update (back button disabled)
- Minor/patch = SOFT update (skip করা যায়)
- Firebase DB তে `app_update_config` রাখলে সেটাই priority পাবে

### 📺 Video Quality Selector (Player)
- Player এ "HD" Chip button যোগ হয়েছে
- Tap করলে Auto / 1080p / 720p / 480p / 360p dialog আসে
- ExoPlayer `trackSelectionParameters.maxVideoSize` দিয়ে quality set করে

### 📥 Offline Episode Download
- **File:** `EpisodeAdapter.kt` (updated)
- `item_episode.xml` এ "⬇" button যোগ হয়েছে
- Episode ID format: `{seriesId}_ep{episodeNumber}`
- Downloaded হলে "✓ Downloaded" দেখায়

---

## ⚙️ Setup করতে হবে

### GitHub Update
`GitHubUpdateManager.kt` এ এই দুটো line পরিবর্তন করুন:
```kotlin
const val GITHUB_OWNER = "YOUR_GITHUB_USERNAME"
const val GITHUB_REPO  = "CineStreamOTT"
```
GitHub Release এ APK attach করুন (tag format: `v3.5.0`)

### OpenGraph Share
`cinestream.app/movie/{movieId}` URL এ HTML meta tag যোগ করুন:
```html
<meta property="og:image" content="{bannerImageUrl}" />
<meta property="og:title" content="{movieTitle}" />
<meta property="og:description" content="{movieDesc}" />
```

### Firebase Realtime DB Rules
Watch Party এর জন্য:
```json
{
  "rules": {
    "watch_parties": { ".read": "auth != null", ".write": "auth != null" },
    "users": { "$uid": { ".read": "$uid === auth.uid", ".write": "$uid === auth.uid" } }
  }
}
```
