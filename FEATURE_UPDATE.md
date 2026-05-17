# CineStream OTT — Feature Update v3.4.0

## পূর্ববর্তী আপডেট (v3.3.0) এর ফিচার
- DiffUtil, Glide thumbnail/placeholder, NetworkMonitor
- Push Notification (FCM), Deep Link, Search History
- Recently Viewed, Error Handling (Bangla messages)

---

## নতুন ফিচার — v3.4.0

---

### 📺 Movie Series / Episodes সাপোর্ট

**Model আপডেট (`data/model/Movie.kt`):**
```
isSeries: Boolean       — সিরিজ কিনা
totalSeasons: Int       — মোট সিজন সংখ্যা  
seasons: List<Season>   — প্রতিটি সিজন
  └── Season:
        seasonNumber, title
        episodes: List<Episode>
          └── Episode:
                episodeNumber, title, description
                duration, thumbnailUrl
                streamUrl, downloadUrl, isFree
```

**Firebase Database Structure:**
```json
{
  "isSeries": true,
  "totalSeasons": 3,
  "seasons": [
    {
      "seasonNumber": 1,
      "title": "Season 1",
      "episodes": [
        {
          "episodeNumber": 1,
          "title": "Pilot",
          "streamUrl": "https://...",
          "isFree": true
        }
      ]
    }
  ]
}
```

**UI:**
- Detail screen-এ Spinner দিয়ে Season select করা যাবে
- Episode list-এ thumbnail + title + duration দেখাবে
- FREE episode-এ "FREE" badge, premium-এ lock icon
- Episode tap করলে সরাসরি player খুলবে
- Center play button সিরিজে লুকানো থাকবে

---

### 🎬 Trailer Play Option

- Movie detail screen-এ "▶ Trailer" button যোগ হয়েছে
- Firebase-এ `trailerUrl` বা `trailer` field রাখলেই কাজ করবে
- Trailer play করলে player title-এ "— Trailer" লেখা থাকবে
- HLS, YouTube (direct URL), MP4 সব format চলবে

---

### 🎯 Related Movies Section

- Movie detail screen-এর নিচে "আরও দেখুন" section
- Same category বা same genre-এর movies দেখাবে
- IMDB rating অনুযায়ী sort হয় (সর্বোচ্চ rating আগে)
- Tap করলে সরাসরি সেই movie-র detail খুলবে
- Background-এ load হয় — main content দেরি করে না

---

### 👤 Cast / Actor Click সব জায়গায় কাজ করে

**সমস্যা ছিল:**
- `ActorProfileFragment` layout-এর ID গুলো মিলছিল না
- `toolbar` ID ছিল না layout-এ (ছিল `btn_back`)
- `rv_movies` ID ছিল না (ছিল `rv_actor_movies`)
- `tv_movie_count` ID ছিল না layout-এ

**Fix করা হয়েছে:**
- Fragment সম্পূর্ণ rewrite — সব binding ID layout-এর সাথে মিলেছে
- Nav graph-এ `action_actor_to_detail` action যোগ করা হয়েছে
- Global action `action_global_to_actor` যোগ — যেকোনো screen থেকে actor profile-এ যাওয়া যাবে
- Actor-এর movies-এ click করলে সেই movie detail-এ navigate করবে

---

### 🏷️ Genre Filter — Home Screen

- Home screen-এ "ধরন অনুযায়ী" section যোগ হয়েছে
- Firebase-এ `genre: "Action, Drama"` বা `genres: ["Action","Drama"]` রাখলেই কাজ করে
- Chip select করলে সেই genre-এর movies filter হয়ে horizontal list-এ দেখাবে
- ViewModel-এ `filterByGenre()` function — reactive filtering

---

### 🛠️ Admin Panel আপডেট

Add/Edit Movie screen-এ নতুন ফিল্ড:
- **Trailer URL** — YouTube বা HLS URL
- **Genre** — comma-separated (Action, Drama, Thriller)
- **Series Toggle** — সিরিজ কিনা mark করা

---

### 📋 নতুন/পরিবর্তিত ফাইল (v3.4.0)

| ফাইল | পরিবর্তন |
|------|---------|
| `data/model/Movie.kt` | Season, Episode, genre, trailerUrl, isSeries যোগ |
| `data/repository/MovieRepository.kt` | Series/episode/genre/trailer parsing, getRelatedMovies() |
| `adapter/EpisodeAdapter.kt` | **নতুন** — Episode list adapter with DiffUtil |
| `adapter/RelatedMoviesAdapter.kt` | **নতুন** — Related movies horizontal adapter |
| `res/layout/item_episode.xml` | **নতুন** — Episode item layout |
| `res/layout/item_related_movie.xml` | **নতুন** — Related movie item layout |
| `res/layout/fragment_detail_container.xml` | Trailer, Series, Related, Genre chips section |
| `ui/detail/MovieDetailFragment.kt` | সম্পূর্ণ rewrite — সব ৫টি ফিচার |
| `ui/detail/ActorProfileFragment.kt` | Binding ID fix, proper navigation |
| `res/navigation/nav_graph.xml` | action_actor_to_detail, action_global_to_actor |
| `ui/home/HomeViewModel.kt` | availableGenres, genreFilteredMovies, filterByGenre() |
| `ui/home/HomeFragment.kt` | Genre filter chips + RV wiring |
| `res/layout/fragment_home.xml` | Genre filter section (ChipGroup + RecyclerView) |
| `ui/admin/AddEditMovieActivity.kt` | trailerUrl, genre, isSeries fields |
| `res/layout/activity_add_edit_movie.xml` | Trailer, Genre, Series toggle inputs |
| `res/values/strings.xml` | নতুন strings যোগ |
