# Bharavahi

Android apps for communities (residents associations, clubs, local bodies) to manage members,
events and recurring due cycles. Firebase is the only backend.

- `docs/REQUIREMENTS.md`: the development requirements (IDs `SA-`, `ORG-`, `BR-`, `NFR-`, `UI-`).
- Design rationale: [Bharavahi Technical Design](https://claude.ai/code/artifact/3582dfd8-2317-46ca-a1b7-04793d99e2fd).

## Modules

| Module | Kind | Contents |
| --- | --- | --- |
| `core:domain` | Pure Kotlin | Models, repository interfaces, use cases, validation. No Android or Firebase imports. |
| `core:data` | Android library | Firebase Auth / Firestore / DataStore implementations of the domain interfaces, Hilt bindings. |
| `core:ui` | Android library | `BharavahiTheme`, the swappable `BrandPalette`s, shared Compose components, shared strings. |
| `app-superadmin` | Android app | Super Admin app: screens and ViewModels only. |
| `app-org` | Android app | Organization app for community admins: screens and ViewModels only. |

Dependencies point inwards only: `app → core:ui, core:data → core:domain`.
Screens talk to ViewModels, ViewModels call use cases, use cases depend on repository interfaces.

## Tech

Kotlin 2.1, Jetpack Compose (Material 3), MVVM with `StateFlow`, Hilt, Navigation Compose with
type-safe routes, Firebase Auth + Firestore (offline cache on), DataStore. Min SDK 26, target 35.

## First-time setup

1. **Firebase project.** Create one at <https://console.firebase.google.com>, then:
   - Enable **Authentication → Email/Password**.
   - Create a **Cloud Firestore** database.
   - Add an Android app with package `com.bharavahi.superadmin`, download `app-superadmin/google-services.json`
     and put it in `app-superadmin/`. (It is git-ignored.)
2. **Rules and indexes.** With the Firebase CLI: `firebase deploy --only firestore` from the repo root
   (uses `firestore.rules` and `firestore.indexes.json`).
3. **Your super admin account.**
   - Authentication → Users → Add user (your email and a password). Copy the **User UID**.
   - Firestore → create collection `superAdmins`, document ID = that UID, fields
     `name` (string) and `email` (string).
4. **Run.** Open the project in Android Studio (Ladybug or newer, JDK 17+) and run `app-superadmin`.
5. **Organization app.** In the same Firebase project, add a second Android app with package
   `com.bharavahi.org`, download its `google-services.json` and put it in `app-org/` (also git-ignored).
   Run `app-org` and sign in with the admin email and temporary password you entered when creating
   the community in the Super Admin app. Redeploy the rules (step 2) whenever `firestore.rules` changes.

## Theming

Each app passes one `BrandPalette` to `BharavahiTheme` in `MainActivity`
(`SlatePalette` for Super Admin, `TealOrangePalette` for the Organization app).
All raw colours live in `core/ui/src/main/kotlin/com/bharavahi/core/ui/theme/Palettes.kt`;
to re-theme an app, edit or replace its palette. Screens only use `MaterialTheme.colorScheme`
and `LocalStatusColors`.

## Tests

```
./gradlew :core:domain:test
```
