# Third-Party Notices

ImageSorterPro's own source code is licensed under the MIT License (see
[`LICENSE`](LICENSE)). However, the distributed application bundles third-party
software under different licenses. This file documents those components and the
obligations that come with redistributing them.

---

## FFmpeg

ImageSorterPro bundles prebuilt **FFmpeg** binaries to extract still-frame
thumbnails from video files:

- `ext-dist/ffmpeg` — macOS build
- `ext-dist/ffmpeg.exe` — Windows build

### License: GNU General Public License, version 3 (GPLv3)

The bundled macOS binary reports the following:

```
ffmpeg version 8.1.2-tessus  https://evermeet.cx/ffmpeg/
configuration: --enable-gpl --enable-version3 --enable-libx264 --enable-libx265
  --enable-libxvid ... (full configuration below)
```

Because this build was configured with `--enable-gpl` and `--enable-version3`,
**these FFmpeg binaries are licensed under the GNU GPL, version 3**. The full
text of that license is included in this repository as
[`LICENSES-GPL-3.0.txt`](LICENSES-GPL-3.0.txt).

Full build configuration of the bundled macOS binary:

```
--cc=/usr/bin/clang --prefix=/opt/ffmpeg --extra-version=tessus --enable-avisynth
--enable-fontconfig --enable-gpl --enable-libaom --enable-libass --enable-libbluray
--enable-libdav1d --enable-libfreetype --enable-libgsm --enable-libharfbuzz
--enable-libmodplug --enable-libmp3lame --enable-libmysofa --enable-libopencore-amrnb
--enable-libopencore-amrwb --enable-libopenh264 --enable-libopenjpeg --enable-libopus
--enable-librubberband --enable-libshine --enable-libsnappy --enable-libsoxr
--enable-libspeex --enable-libtheora --enable-libtwolame --enable-libvidstab
--enable-libvmaf --enable-libvo-amrwbenc --enable-libvorbis --enable-libvpx
--enable-libwebp --enable-libx264 --enable-libx265 --enable-libxavs --enable-libxml2
--enable-libxvid --enable-libzimg --enable-libzmq --enable-libzvbi --enable-version3
--pkg-config-flags=--static --disable-ffplay
```

> **Note on the Windows binary (`ext-dist/ffmpeg.exe`):** verify its license by
> running `ffmpeg.exe -version` and reading the `configuration:` line. If it also
> shows `--enable-gpl`, it is GPL-licensed and the same obligations below apply.
> If you ever replace these with builds configured *without* `--enable-gpl`
> (LGPL builds), update this notice accordingly — your obligations would be
> lighter.

### How this affects ImageSorterPro's own MIT license

ImageSorterPro invokes FFmpeg as a **separate program** via `ProcessBuilder`
(see `FastVideoThumbnailUtil`); it does not link against, statically embed, or
incorporate FFmpeg's code into its own executable. Under the FSF's interpretation
this is **"mere aggregation"** of two independent works on the same distribution
medium, so the GPL does **not** extend to ImageSorterPro's own source code, which
remains under the MIT License. The GPLv3 obligations below apply only to the
FFmpeg binaries themselves.

### Obligations when you redistribute this application

If you distribute ImageSorterPro **together with** these FFmpeg binaries, GPLv3
§6 requires that you also make the **Corresponding Source** of FFmpeg available.
This repository satisfies that as follows:

1. The complete GPLv3 license text accompanies the binaries
   ([`LICENSES-GPL-3.0.txt`](LICENSES-GPL-3.0.txt)).
2. The exact version and build configuration of the binaries are documented
   above, so the build is reproducible.
3. **Written offer (valid for at least three years):** The corresponding source
   code for the bundled FFmpeg version is available from the FFmpeg project at
   <https://ffmpeg.org/download.html> and <https://ffmpeg.org/releases/>
   (source for FFmpeg 8.1.2). In addition, the maintainer of this repository
   will, on request via the project's issue tracker at
   <https://github.com/nirmal-mewada/ImageSorterPro/issues>, provide the
   complete corresponding source for the exact FFmpeg build distributed here,
   at no charge beyond the cost of physical distribution, for a period of at
   least three years from the date you received the binary.

You must keep this notice, the `LICENSES-GPL-3.0.txt` file, and the written
offer intact in any redistribution that includes the FFmpeg binaries.

### Source

- FFmpeg homepage: <https://ffmpeg.org>
- Prebuilt binary provenance (macOS): <https://evermeet.cx/ffmpeg/>
- FFmpeg source releases: <https://ffmpeg.org/releases/>
