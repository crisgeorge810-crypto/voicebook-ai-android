# VoiceBook AI

Android speech-to-notes starter app. Features include speech recognition, typed notes, local topic-labelled note saving, a spoken “New topic” / “New topic [name]” command, audio recording and playback of the latest recording.

## Build using GitHub Actions on your phone
1. Download and extract this ZIP on your phone.
2. Upload the **contents** (folders and files), not the ZIP itself, to the root of `crisgeorge810-crypto/voicebook-ai-android`.
3. Commit to `main`.
4. Open the repository's **Actions** tab. Choose **Build VoiceBook AI APK** and tap **Run workflow** if it has not started automatically.
5. When the run succeeds, open it and download artifact **VoiceBook-AI-APK**.
6. Extract that downloaded artifact ZIP and install `app-debug.apk`.

Speech recognition uses the phone's installed Android recognition service; offline support depends on that service. Notes and the latest recording are stored locally on the phone. This is a starter build, not yet a Play Store release.
