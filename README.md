# Second Memory

Android app: share a link, photo, PDF or note and it stays pinned in the notification shade until you unpin it.

## Build

```
cd android
# keystore.properties is local (not committed). Example:
#   storeFile=release.keystore
#   storePassword=…
#   keyAlias=secondmemory
#   keyPassword=…
./gradlew :app:assembleRelease
```

Output: `android/app/build/outputs/apk/release/app-release.apk`

Requires JDK 17 and Android SDK 35.

## Notes

- Pins live in the notification shade. Later snoozes until tonight, then they come back.
- Metadata fetch is HTTPS-only with a size cap. Cleartext HTTP is disabled.
- Room schema lives in `android/app/schemas/`. Do not use destructive migrations.
