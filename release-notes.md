# Fedora Tube v1.7.8 (Prerelease)

**Release date:** 2026-11-07
**Status:** Prerelease

## Highlights

- YouTube-style top categories: Home, Subscriptions, Music chips under the FedoraTube wordmark. Subscriptions and Music left the bottom navigation bar (bottom bar is now Home/Library only)
- Search is context-aware: Music search opens from Music, Subscription search from Subscriptions, normal search from Home. Tapping Home chip from Subscriptions/Music returns to Home; tapping the active chip scrolls to top

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
