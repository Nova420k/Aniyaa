<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_image.png" width="96" alt="Aniyaa logo" />

# Aniyaa

Search [nyaa.si](https://nyaa.si) from your Android phone. [Sukebei](https://sukebei.nyaa.si) is optional and stays off until you turn it on.

[Download the latest version](https://github.com/Nova420k/Aniyaa/releases/latest) · Android 7.0 or newer

</div>

---

## What is Aniyaa?

Aniyaa is a simple Android app for browsing [nyaa.si](https://nyaa.si) without using a web browser. You can search, filter results, save favorites, and send a download to the torrent app you already use.

Nyaa is the default catalog (anime, manga, music, and more). Sukebei is adult content — enable it in **Settings** if you are 18 or older.

It is **not** an official nyaa.si or sukebei.nyaa.si app, and it is **not** a torrent downloader. Aniyaa finds listings; your torrent app (such as LibreTorrent, Flud, or qBittorrent) does the actual downloading.

---

## Download and install

1. On your phone, open the **[latest release](https://github.com/Nova420k/Aniyaa/releases/latest)**.
2. Tap **Aniyaa-v…apk** to download it.
3. Open the downloaded file. If Android warns that the app isn’t from the Play Store, choose **Install anyway**. You may need to allow your browser (or Files) to install apps — Android will show a switch for that.
4. Open **Aniyaa** from your app drawer.

To update later, download a newer APK from the same [Releases](https://github.com/Nova420k/Aniyaa/releases) page and install it over the old one, or use **Settings → Updates → Install**. You won’t lose bookmarks. [Obtainium](https://github.com/ImranR98/Obtainium) can watch `https://github.com/Nova420k/Aniyaa/releases`.

If you previously used **Sukiniyaa**, install Aniyaa separately and export/import a backup from Settings if you want to keep saved data. The two apps use different package names, so they cannot update in place.

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

## License and disclaimer

Aniyaa is free and open source. See [LICENSE](LICENSE) for details.

Aniyaa is an unofficial project and is not affiliated with or endorsed by nyaa.si or sukebei.nyaa.si. Use it in line with the laws where you live.

---

<details>
<summary>For people who want to build the app from source</summary>

You will need JDK 17 and the Android SDK. Then:

```bash
git clone https://github.com/Gourab0002/Aniyaa.git
cd Aniyaa
./gradlew assembleDebug
```

Signed releases are published automatically when a `v*.*.*` tag is pushed. Local signed builds use a gitignored `keystore.properties` file; see `keystore.properties.example`.

</details>
