# Fedora Tube v1.7.8 (Prerelease)

**Release date:** 2026-11-09
**Status:** Prerelease

## Highlights

- Focus mode is consistent about hiding the subscribe system: on a channel page the Subscribe button is gone from the first frame (no show-then-hide flash), search results no longer show a Subscribe button on the channel card ("Go to channel" takes the row), and the player's action row drops the "Add note" button along with subscribe/like
- Shorts comments animation is now smooth and synced both ways: opening comments slides the sheet up while the video shrinks to the top in the same motion, and closing (drag down or back) slides it back down while the video grows to full screen — one shared animation instead of two fighting ones, with overlays fading in step
- Shorts comment sheet is cleaner, YouTube-style: no sort-category chips (Top/Newest) and no cross (close) button — drag the sheet down or press back to close; the regular video player's comment sheet is unchanged
- Shorts feed can be refreshed like Home: pull down on the first Short (or tap the refresh icon at the top) to reload with a top indicator, the current feed staying on screen until the new one lands
- Shorts comment replies work now: tapping "view replies" under a comment loads and shows its replies (and more replies), instead of spinning forever
- Shorts comments no longer hide the bottom navigation bar: the bar stays fixed and the comment sheet opens above it, sliding up from behind the bar
- Shorts scrolling is smooth like YouTube now: no more black flash or spinner on swipe — the next Shorts' posters and streams preload ahead, and each Short fades in from its poster only when its first frame renders (swiping keeps working, as before)
- Shorts video no longer shows a picture around it when comments are open: once the video is playing, the letterboxed area is pure black
- Shorts video now plays below a black status-bar strip instead of behind the status icons, and the stray loading spinner above the navigation bar on entering Shorts is gone
- Shorts video keeps one ratio with comments open and closed: it's never cropped, and closing comments no longer makes the video jump/bounce — it glides smoothly back to its normal position
- Shorts video now starts right below the status bar in its true ratio, so no black gap sits above it — the leftover black area is only below the video, above the navigation bar
- Shorts video fills the whole area from below the status bar to the navigation bar like YouTube, in one ratio with comments open and closed (no crop switch, no bounce on close), and swiping is smooth: the current Short keeps playing until the pager settles, then the next one plays straight away without flashing the old thumbnail or reloading
- Switching Home ↔ Shorts is smooth now: entering Shorts no longer shows the video as a small centered strip that snaps to full size — the first frame fades in from the poster only once its size has settled, so entry is one clean fade with no shake
- Swiping Short→Short is YouTube-smooth now: the next Short is preloaded with its first frame rendered before the swipe, so the pager shows poster → live video with no black screen or spinner, and nothing freezes mid-scroll
- Shorts comments-open shrink restored: opening comments slides the video up, uncropped, to the top third again, and closing glides it back smoothly
- Shorts bottom layout is back to the YouTube order: timeline at the navigation bar's top edge, caption above it, channel name + logo above the caption, and like/comment/save/share low at the bottom — with no "Save" text under the bookmark icon
- Downloads is now its own bottom-bar tab right after Shorts (download icon): all downloaded videos and songs live there, one tap away — the Library tab stays too
- Library no longer has a Downloads row (it's in the bar now); History now shows watched Shorts too (Shorts row + Shorts filter); Downloads top bar has a search icon right of notifications, and in Focus mode it shows a back button with no notification/search icons
- Subscriptions: swiping on the All-channels (manage) list no longer jumps to the Music category, and the import/export button is removed from the top of that page
- Downloads has no refresh (no pull-to-refresh, no refresh icon) — the list is live; watched Shorts are recorded into History; Focus mode hides subscribe on channel pages entirely
- Subscriptions "View all" page swipes between the All-channels (Video) list and the Music subscriptions list, like the tabs above
- Video card ⋮ now opens a YouTube-style anchored popup menu (Watch later, Save to playlist, Download, Share, Not interested, Don't show this channel) instead of a bottom sheet — Play next and Add to queue are out of it
- Player settings is YouTube's sheet now: the front page is exactly Quality, Playback speed, Captions, Audio Track, Lock screen and More — Lock screen locks the controls right away, and everything else (loop, autoplay, SponsorBlock, notes, PiP, sleep timer, equalizer, ambient mode…) sits behind More
- Cast to TV is removed from the app entirely: no row in player settings, no cast device picker, no cast button toggle in Settings
- Nothing shows below the white home line any more: when the bottom bar hides on scroll (or a page has no bar), the gesture strip stays in the page's background colour like YouTube instead of letting content slide under the line — PiP and the fullscreen player still draw to the bottom edge
- Shorts are back, YouTube-style: a Shorts icon sits in the bottom navigation bar directly right of Home, opening a full-screen vertical Shorts player (swipe up/down, tap to pause, mute, like/dislike, share, progress bar, channel + title overlay)
- Shorts tab plays the interest-based algorithmic feed; channel pages show a Shorts tab and Shorts shelves, the Subscriptions page shows a Shorts strip of your subscribed channels, and search results show a Shorts strip for the searched topic (tapping any Short opens the matching queue and continues into recommendations)
- Focus mode has no Shorts anywhere: no Shorts tab in the navigation bar, no Shorts player (Shorts open in the regular player), no Shorts tab/shelves on channel pages, no Shorts strip in search results or Subscriptions, no Shorts in History/Library
- YouTube-style top categories: Home, Subscriptions, Music chips under the FedoraTube wordmark. Subscriptions and Music left the bottom navigation bar (bottom bar is now Home/Shorts/Library only)
- Search is context-aware: Music search opens from Music, Subscription search from Subscriptions, normal search from Home. Tapping Home chip from Subscriptions/Music returns to Home; tapping the active chip scrolls to top
- Post comments no longer let the feed behind them scroll (superseded by full removal) — the post comments feature no longer exists at all; the sheet and background scroll block are gone with the whole community-posts system
- Playlist, Watch Later and music playlist pages no longer offer "Save as file": the 3-dot menu's save-to-file action and its file-picker system are removed on both video and music playlists, so Watch Later (and other non-editable playlists) no longer show a 3-dot menu at all — only editable playlists keep a 3-dot menu with Edit/Delete and other actions
- Subscriptions search now finds channels: tapping the search icon in Subscriptions opens the full search (with channel results and subscribe) instead of only filtering the already-subscribed list, which could never find a new channel
- Subscriptions layout is tighter: the subscribed-channel row sits closer under the Subs indicator, and the "All" (arrow) button is redesigned as a tonal circle (secondary-container with a larger arrow icon and proper ripple) instead of a flat placeholder disc
- Home feed never re-shows a video you have already watched (exact watch-history dedup, including subscribed-channel uploads, RSS fresh-subs and cache paths) — no setting required — and the Related/similar lane now spreads one video per channel round-robin before repeats, so a watch no longer turns the feed into that channel again and again
- The post comments system is gone end to end, not hidden: posts parsing, the posts controller, the comments sheet, the post attachment cards, shelf rendering, strings and tests, as listed in its own bullet
- Music feed has a new "Recommended for You" shelf seeded from what you actually play: recent distinct-artist history expands through related recall and the genres/moods you listen most, ranked by the on-device brain (new in the section order and hideable in settings); next song also auto-plays on an empty queue via "Endless radio" (on by default, Settings > Playback)
- Music Speed dial now shows history-derived recommendations: each tile is a track the taste model ranks from what you listen to (related picks seeded by your history and the genres you keep most), replaces favorites as the dial pool (fallback in refresh order)
- Refreshing Home no longer re-pins the same followed-channel upload to the top of the feed: fresh-subscription pinning is also skipped for videos that were on screen last time (the on-screen exclusion now applies to refresh rebuilds too), so the one-slot pin can't glue a single older upload in place forever
- Home feed is interest-first like YouTube: subscriptions are kept to the minimum pinned slots and one blended quota by the interest split, Related/Discovery lanes lead the blend, related seeds can never take more than one seed per channel (a binge stops being treated as variety), and the Related lane itself round-robins across channels before repeating
- Community posts are removed entirely, everywhere: no Posts tab on channel pages, no posts in the Subscriptions feed / Home shelves, no post comments sheet, no search posts strip, no posts anywhere in the UI — and the code (parsers, controllers, screens, strings, InnerTube posts browsing) is deleted, not just hidden
- Channel page's sort/filter selector (Latest / Popular / Oldest…) now lives as a translucent down-arrow in the banner's top-right corner: the current tab's dropdown is listed there, Latest sent as server default is selected by default, and picking one re-applies the list exactly as before; the chips bar now only appears when a tab actually has chips
- Community posts on a channel page are gone for good: the Posts/Community tab is not rendered (deep links to a channel's posts page resolve to Home), and the posts deep-link no longer shows up as a stray tab
- Channel in-channel search is deleted outright, every icon, field, and code path
- Channel Live tab's sort control is now the same banner arrow (Latest / Oldest — Popular is dropped for livestreams), and channel Playlists still shows its own sort menu through the same arrow; the arrow itself matches the back button (translucent black circle, white icon)

# Fedora Tube v1.7.7

**Release date:** 2026-11-07
**Status:** Stable

## Highlights

- Onboarding welcome no longer shows the large hero logo - only the FedoraTube wordmark, "Make yours" and subtitle on the wallpaper
- YouTube App Links now cover gaming.youtube.com and studio.youtube.com too, and the link prompt keeps asking on every launch until every address is turned on ("0 verified links" is normal - Google owns youtube.com)
- Update checker only offers stable releases - prereleases and drafts never reach users
- Home top bar order swapped: notification icon on the left, search icon on the right

# Fedora Tube v1.7.6

**Release date:** 2026-11-06
**Status:** Stable

## Highlights

- Removed `ci/` signing keystore from the public repo - the release key now lives only in the GitHub `RELEASE_KEYSTORE_BASE64` secret and local `release.keystore`
- Onboarding "Make yours" shows the FedoraTube wordmark and hero again, with its text back at the bottom above "Get started"
- Fixed Home and Music feeds starting slightly scrolled - they now open at the very top (first item fully visible)
- Bottom navigation bar hides while scrolling on Home again (when "Hide nav bar on scroll" is on), alongside the top app bar
- Channel Videos and in-channel search use the YouTube-style thumbnail-left list (title, views and time beside the thumbnail, like v1.7.4)
- YouTube links are App Links again (`autoVerify`) so Android 12+ can make FedoraTube the default handler for youtube.com, youtu.be and youtube-nocookie.com via Settings > Open links in this app

# Fedora Tube v1.7.5

**Release date:** 2026-11-05
**Status:** Stable

## Highlights

- Update screen: removed the logo above "FedoraTube is up to date" so the message sits directly under the back button
- Channel Videos and in-channel search use the channel grid again instead of the full-width Home-style list
- Onboarding "Make yours" no longer shows the logo or hero, and its text is centred instead of stranded at the bottom
- Nightly builds and their GitHub Actions steps are gone; only stable releases are built and published
- The link-handling setting now clearly explains how to let YouTube links open in FedoraTube

# Fedora Tube v1.7.4

**Release date:** 2026-11-05
**Status:** Stable

## Highlights

- YouTube links shared by others now open and play inside Fedora Tube
- Channel posts show comments inline, and the channel Videos tab uses a full-width YouTube-style card list
- The bottom navigation bar stays put; the app-name bar now hides on scroll on Home, Music and Subscriptions
- Focus mode hides subscribe and like/dislike actions while watching, and silences all app notifications
- Your taste is hidden from Settings and Settings search
- Onboarding "Make yours" fills the empty top space with the animated hero
- Check Update loses its black top bar and gets a raised, prominent back button

# Fedora Tube v1.5.0

**Release date:** 2026-10-22
**Status:** Stable

## Highlights

- Channel community posts in subscriptions feed with inline viewing, comment sheets, and sharing matching YouTube layout
- YouTube-styled unified channel subscription and actions bar in the video player
- YouTube-styled sleek player settings bottom sheet with quality, captions, loop, stable volume, and more
- Updated Home top bar with crisp official Fedora logo
- Performance optimizations: smoother feeds, enhanced image caching, and faster scrolling

# Fedora Tube v1.4.1

**Release date:** 2026-10-21
**Status:** Stable

## Highlights

- Simplified notifications screen: removed settings icon and settings navigation
- Pure vector adaptive launcher icon matching YouTube proportions
- UI cleanups and stability improvements

# Fedora Tube v1.4.0

**Release date:** 2026-10-21
**Status:** Stable

## Highlights

- High-contrast white container launcher icon matching standard system apps
- Clean downloads view layout without sort chips
- Automated in-app update checking and release history viewer
- Fixed background music auto-minimization on app launch and navigation
- Purged legacy folders, unused assets, and optimized build footprint
- Replaced Flow references with Fedora Tube across UI and strings

# Fedora Tube v1.3.0

**Release date:** 2026-10-21
**Status:** Stable

## Highlights

- In-app update system with automatic launch prompt and in-app download & install
- Software update screen with current version display and release history
- Clean downloads section layout (removed sort chips)
- High-contrast white app icon with safe-zone margin
- Clear notification logo
- Downloads folder saved under FedoraTube

# Fedora Tube v1.2.0

**Release date:** 2026-10-20
**Status:** Stable

## Highlights

- UI refinements, redesigned welcome screen, and settings optimizations

# Fedora Tube v1.1.0

**Release date:** 2026-10-20
**Status:** Stable

## Highlights

- Initial release of Fedora Tube
