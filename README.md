# VoiceBook AI — Android source

This update adds an Android foreground microphone service so audio capture is independent of the Activity and speech recognition screen. Recordings are saved as separate timestamped `.m4a` files under the app's private `recordings` folder, tagged with the topic name in the filename. The most recently completed recording is stored in the `voicebook` preferences as `last_audio` for the existing playback control.

## Build

Use the repository's GitHub Actions workflow **Build VoiceBook AI APK**.

## Important test notes

- Start recording while the app is visible. Android requires microphone foreground-service start to originate from a visible user action.
- A persistent notification appears with a Stop action while recording.
- Speech recognition is still a separate feature. A speech-recognition error should not stop the recording service.
- Test long recordings on the actual phone, check available storage, lock the screen, switch apps, then stop from the app or notification.
- This is an early reliability update, not a verified six-hour recording guarantee. Device battery policies, storage, microphone hardware and Android vendor restrictions can affect long sessions.
- The current playback UI still plays the latest completed recording. Per-topic recording library, seek/pause controls, cloud accounts, email verification, subscriptions and payments are not implemented in this update.
- Clear distant-speaker capture requires a suitable external wireless microphone or a feed from the church sound system; software cannot overcome arbitrary microphone distance.
