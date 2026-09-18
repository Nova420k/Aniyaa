<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_image.png" width="96" alt="Aniyaa logo" />

# Aniyaa

[![Build](https://github.com/Nova420k/Aniyaa/actions/workflows/build.yml/badge.svg)](https://github.com/Nova420k/Aniyaa/actions/workflows/build.yml) [![Release](https://github.com/Nova420k/Aniyaa/actions/workflows/release.yml/badge.svg)](https://github.com/Nova420k/Aniyaa/actions/workflows/release.yml)

Search [nyaa.si](https://nyaa.si) from your Android phone. [Sukebei](https://sukebei.nyaa.si) is optional and stays off until you turn it on.

[Download the latest version](https://github.com/Nova420k/Aniyaa/releases/latest) · Android 7.0 or newer · distributed as an APK via GitHub Releases (not on the Play Store)

</div>

---

## What is Aniyaa?

Aniyaa is a simple Android app for browsing [nyaa.si](https://nyaa.si) without using a web browser. You can search, filter results, save favorites, and send a download to the torrent app you already use.

Nyaa is the default catalog (anime, manga, music, and more). Sukebei is adult content — enable it in **Settings** if you are 18 or older.

It is **not** an official nyaa.si or sukebei.nyaa.si app, and it is **not** a torrent downloader. Aniyaa finds listings; your torrent app (such as LibreTorrent, Flud, or qBittorrent) does the actual downloading.

---

## Screenshots

<!-- Maintainers: add captures under docs/screenshots/ and reference them here, e.g. docs/screenshots/search.png -->
Screenshots coming soon. If you are contributing UI changes, please attach before/after captures to your pull request.

---

## Download and install

1. On your phone, open the **[latest release](https://github.com/Nova420k/Aniyaa/releases/latest)**.
2. Tap the **`Aniyaa-vX.Y.Z.apk`** asset (for example `Aniyaa-v2.5.0.apk`) to download it. See the [release notes](https://github.com/Nova420k/Aniyaa/releases) for what changed.
3. Open the downloaded file. If Android warns that the app isn’t from the Play Store, choose **Install anyway**. You may need to allow your browser (or Files) to install unknown apps — Android will show a switch for that.
4. Open **Aniyaa** from your app drawer.

To update later, download a newer APK from the same [Releases](https://github.com/Nova420k/Aniyaa/releases) page and install it over the old one, or use **Settings → Updates → Install**. You won’t lose bookmarks. [Obtainium](https://github.com/ImranR98/Obtainium) can watch `https://github.com/Nova420k/Aniyaa/releases`.

Updates must be signed with the same key to install over the old app. Builds from different forks or signers cannot update in place — install them separately and migrate via **Settings → Export/Import** instead. (**Sukiniyaa** additionally uses a different package name, so it always installs as a separate app.)

---

## How to use it

**Search**  
Latest listings load when you open the app (you can turn that off in Settings). Type in the search bar and press search, or tap **Show latest**. Use `user:Name` to find an uploader. Category chips (Anime, Audio, and so on) sit under the bar. **Trusted** and **No remakes** are one tap. A dot on the filter button means extra filters are on. Long-press a result for magnet, bookmark, share, or follow a show.

**Filters**  
The filter button opens sort options and subcategories (**More**). You can also save the current search from that sheet.

**Sukebei**  
Off by default. Turn on **Enable Sukebei (18+)** in Settings. Nyaa / Sukebei chips then appear on Search.

**Open a listing**  
Tap a result for the description, file list, and comments. Swipe a result right for magnet, left to bookmark.

From the listing you can:

- **Magnet** — start the download in your torrent app (also in the top bar)
- **Copy magnet** — copy the link to paste elsewhere
- **Download** — get the `.torrent` file
- **Share** — send the listing to someone else
- **View on Nyaa / Sukebei** — open the page in your browser

**Bookmarks**  
Star a listing to find it later. Swipe a bookmark left to remove it. When **All** catalogs are shown, each bookmark is labeled Nyaa or Sukebei.

**History**  
Recent searches and recently viewed listings are on the **History** tab. Saved searches can notify you of new results (Aniyaa will ask for notification permission). You can choose how often alerts run in Settings.

**Open from the browser**  
Links to nyaa.si or sukebei.nyaa.si listings and searches can open in Aniyaa, including `http` and `www` URLs. A Sukebei link asks for the 18+ confirmation if that catalog is still off.

**Look and feel**  
Settings has **Material You** (wallpaper colors on Android 12+), extra palettes, light/dark mode, default category and sort, and a preferred torrent app.

**Privacy**  
App lock (PIN pad or biometrics), a lock delay, hide screenshots, or hide from recents. Export or reset local data from Settings. Import can merge or replace.

---

## What you need

- An Android phone or tablet running **Android 7.0** or later
- An internet connection
- A torrent app if you want to download files (Aniyaa only hands off the link)

Bookmarks, search history, and saved searches stay on your device. Aniyaa does not create an account.

If nyaa.si or sukebei.nyaa.si is blocked, pick or paste a mirror URL in Settings and tap **Test**.

---

## Permissions

Aniyaa requests only what it needs to work as a sideloaded app:

- **Internet** — fetch listings, descriptions, and update info from nyaa.si / sukebei.nyaa.si (or your configured mirror).
- **Notifications** — alert you about new results for saved searches (only if you enable alerts; Aniyaa asks first).
- **Biometrics** — unlock the app with fingerprint or face when you turn on app lock (optional; a PIN works instead).
- **Install packages** — install an update APK downloaded via **Settings → Updates** (Android shows its own confirmation).
- **Storage (Android 9 and older only)** — read and write `.torrent` and backup files where scoped storage does not apply yet.

There is no account, no ads, and no analytics SDK.

---

## FAQ

**Nothing loads / the site seems blocked.**
Pick or paste a mirror URL in Settings and tap **Test**. If the site itself is down, Aniyaa has to wait with you until it is back.

**Tapping Magnet does nothing.**
Install a torrent app first — Aniyaa only hands off the link. If Android asks which app to open it with, set your preferred one as default (you can also pick a preferred torrent app in Aniyaa's Settings).

**I don't get saved-search alerts.**
Allow notifications when Aniyaa asks, and exempt the app from battery optimization so background checks can run at your chosen interval.

**Merge or Replace on import?**
Merge keeps what is on the device and adds the backup's entries; Replace wipes device data first. When in doubt, export a fresh backup before replacing.

---

## License and disclaimer

Aniyaa is free and open source. See [LICENSE](LICENSE) for details.

Aniyaa is an unofficial project and is not affiliated with or endorsed by nyaa.si or sukebei.nyaa.si. Use it in line with the laws where you live.

---

<details>
<summary>For people who want to build the app from source</summary>

You will need JDK 17 and the Android SDK (compileSdk 35). Then:

```bash
git clone https://github.com/Nova420k/Aniyaa.git
cd Aniyaa
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Signed releases are published automatically when a `v*.*.*` tag is pushed: the workflow takes `versionName` from the tag and derives `versionCode` as `MAJOR * 10000 + MINOR * 100 + PATCH` (an explicit `VERSION_CODE` env var wins, e.g. for hotfixes). Local signed builds use a gitignored `keystore.properties` file; see `keystore.properties.example`.

</details>
