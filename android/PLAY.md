# Google Play — Second Memory 1.0

**Feature freeze.** This is v1.0. Do not add features. Ship bugs only.

## Locked identity

| | |
|---|---|
| App name | Second Memory |
| Package | `com.secondmemory.app` — do not rename |
| Version | **1.0.0** (`versionCode` 19) |
| Price | **£3.99** one-time, UK store default |
| Model | Paid app. No IAP. No ads. No subscription. |
| Tagline | Pay once. No ads. No subscription. |
| Category | Productivity |
| Min Android | 8.0 (API 26) |
| Target | Android 16 (API 36) |

Do not create a second package. Play treats a new package as a new app.

## Store listing

**Short description (80 chars max)**

Share anything. It stays in your notification shade until you unpin it.

**Full description**

Second Memory keeps the things you meant to come back to in the one place you already look: the notification shade.

Share a link, a photo, a PDF, a note or a place from any app. It pins itself. Pull down the shade and it is still there. Unpin it when you are done. Later snoozes it. Unpinned things wait in Saved.

Pay once. No ads. No subscription.

Everything lives on your phone. Optional encrypted backup. No account.

**Graphic assets required in Play Console**

- App icon: 512 × 512 PNG (`play/icon-512.png`)
- Feature graphic: 1024 × 500 PNG (`play/feature-graphic.png`)
- Phone screenshots: `play/screenshot-pinned.png`, `play/screenshot-shade.png` (1080×1920)

`versionCode` starts at **19** because sideloaded test APKs already used 1–18. Do not reset it.

**Privacy policy URL** (required because the app uses INTERNET)

https://rentclock.com/second-memory/privacy

## Data safety form

- Collects personal data: **No**
- Data is encrypted in transit: N/A (nothing sent to us). Link-title fetches use HTTPS to the site you saved.
- Users can request deletion: they delete the app / Reset this device
- Account: **No**
- Advertising ID: **No**
- Ads: **No**
- Independent security review: No

## Permissions (declare why)

| Permission | Why |
|---|---|
| INTERNET | Fetch HTTPS titles for saved links |
| POST_NOTIFICATIONS | Show pinned items in the shade |
| RECEIVE_BOOT_COMPLETED | Restore pins after reboot |
| WAKE_LOCK | WorkManager can finish a shade refresh |

No photos/media permission — share uses the URI the other app grants.
No advertising ID.

## Content rating

Typical utility / productivity. Not news, not health, not kids.

## Release signing

Upload key: local `release.keystore` (gitignored). Fingerprint:

`54:1B:57:CB:6D:A6:17:3D:B2:4D:EA:72:F0:53:F6:2E:A8:1B:5C:C6:5A:6A:3F:BB:E3:C5:F9:33:60:1C:96:AB`

First AAB upload: enrol **Play App Signing**. Google holds the app signing key; this keystore stays the upload key. Keep a backup of the keystore off the laptop.

## Create the Play app (once)

You have to do this in [Play Console](https://play.google.com/console) — there is no API connected here.

1. Create app → name **Second Memory** → default language English (UK) → **App** → **Paid**.
2. Package name will be taken from the first AAB: `com.secondmemory.app`.
3. Set price **£3.99 GBP**. Do not add in-app products.
4. Complete Store listing, Privacy policy, Data safety, Ads declaration (**No**), Content rating, Target audience (18+ is fine; 13+ also fine), News / COVID declarations as No.
5. App access: all functionality available without a login.
6. Internal testing → Create email list with your Gmail → Create release → Upload the AAB → Review → Start rollout to internal testing.
7. Install from the Play Store internal-testing link on your phone. Confirm shade pins, share, backup, widget match the sideloaded APK.

Do **not** promote to production until that internal build is lived-in for a few days.

## AAB

Built as `app/build/outputs/bundle/release/app-release.aab` and copied to play artifacts as `SecondMemory-1.0.0.aab`.
