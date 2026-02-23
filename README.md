# CampingApp

An Android application for exploring campings in the **Comunitat Valenciana** (Valencia region, Spain).  
Data is sourced from the [Generalitat Valenciana open data portal](https://portaldadesobertes.gva.es).

---

## Features

| Feature | Description |
|---------|-------------|
| **Camping list** | Browse all campings in Comunitat Valenciana in a scrollable card list |
| **Camping detail** | Tap any camping to view full details: location, contact info, and capacity |
| **Personal comments** | Add, view, and delete personal notes for each camping (stored locally with Room) |
| **Web service** | Load real-time camping data from the GVA open data API via the overflow menu |

## Architecture

```
app/
├── model/          Camping (Parcelize) · Comment (Room entity)
├── database/       AppDatabase · CommentDao
├── api/            CampingApiService (Retrofit) · RetrofitClient
├── repository/     CampingRepository (JSON + API + Room)
├── adapter/        CampingAdapter · CommentAdapter (ListAdapter)
├── MainActivity    Camping list (RecyclerView + StateFlow)
├── CampingDetailActivity  Detail view + comments
├── MainViewModel   StateFlow for the camping list
└── DetailViewModel StateFlow for comments
```

Technology stack:
- **Kotlin** 2.0 + Coroutines
- **Room** for local comments database
- **Retrofit** + Gson for web service calls
- **Material 3** components

## Data Source

- **Local (Part 1):** `app/src/main/assets/campings.json` — bundled JSON with sample camping entries.
- **Remote (live data):** GVA open data API (`portaldadesobertes.gva.es`), selectable via the overflow menu.

## Building

```bash
./gradlew assembleDebug
```

Requirements: Android SDK 35, Java 11+, Gradle 8.9+.
