# Bharavahi: Development Requirements

Version 1.0 · 2026-10-12 · Owner: Shibin

Source of truth for building Bharavahi. Design rationale and diagrams live in the
[Bharavahi Technical Design](https://claude.ai/code/artifact/3582dfd8-2317-46ca-a1b7-04793d99e2fd) doc.
Requirement IDs (`SA-`, `ORG-`, `BR-`, `NFR-`, `UI-`) are stable so issues and commits can reference them.

---

## 1. Product overview

Bharavahi helps local communities (residents associations, clubs, local bodies) run events that are
funded by recurring member dues. Example: a residents association collects ₹3,000 per member in
6 monthly dues of ₹500 for its annual celebration, and tracks event expenses against what was collected.

It consists of **two Android apps** backed by **one Firebase project**, with **no custom server**:

| App | Users | Purpose |
| --- | --- | --- |
| Super Admin app (`app-superadmin`) | The platform owner only | Create and manage communities and their admins, platform settings |
| Organization app (`app-org`) | 2 to 3 admins per community | Manage members, events, roles, due schedules, payments, expenses, reports |

**Members never log in.** They are records that admins create, enable and disable.

---

## 2. Glossary

| Term | Meaning |
| --- | --- |
| Community / Organization | A tenant on the platform (e.g. a residents association). Stored as `organizations/{orgId}`. |
| Admin | A person who can log in to the Organization app for exactly one community. Max 3 per community. |
| Member | A person in the community. No login. Can be active, disabled or archived. |
| Event | A collection drive with its own money (e.g. "Onam 2027"), its own members, roles and schedules. |
| Event role | A title inside an event, e.g. President, Secretary, Treasurer, Member. |
| Schedule (due cycle) | A payment plan within an event: an amount **per member** split into N dues. |
| Due | One instalment of a schedule, with a label, due date and amount. |
| Payment | Money received from one member against one due. |
| Expense | Money spent from an event, paid by a member. |

---

## 3. Tech stack and constraints

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3. One shared theme in `core`, styled from the UI reference Shibin provides (theme only, not layout). |
| Architecture | MVVM, ViewModel + StateFlow, Navigation Compose, unidirectional data flow |
| DI | Hilt |
| Auth | Firebase Authentication, email + password |
| Database | Cloud Firestore, offline persistence enabled |
| Background work | Android WorkManager |
| Email | Jakarta Mail (Android build) over Gmail SMTP (`smtp.gmail.com:587`, STARTTLS) |
| Sheet backup | Google Identity Services authorization (`drive.file` scope) and the Sheets v4 / Drive v3 REST APIs, called from the phone |
| Reports | Android `PdfDocument` for PDF, CSV for spreadsheets |
| Min SDK | 26 (Android 8.0) |
| Backend | **None.** No Cloud Functions, no custom server. |

Constraints:
- Firebase is the only backend. Everything else runs on the phone.
- Money is stored as **integer paise** (`Long`). ₹500 = `50000`. Never use floating point for money.
- Displayed amounts are whole rupees with Indian digit grouping (₹1,50,000).

---

## 4. Project structure

One Android Studio / Gradle project:

```
bharavahi/
├── core/              # shared: models, repositories, money utils, due generation,
│                      # status calculation, report builder, mail sender, WhatsApp text builder, theme
├── app-superadmin/    # Super Admin app
└── app-org/           # Organization app
```

Both apps are registered as separate Android apps in the same Firebase project.
Business logic (due generation, status, reports, message formatting) lives in `core` and is unit tested.

---

## 5. Roles and access

| Role | How identified | Can do |
| --- | --- | --- |
| Super admin | `superAdmins/{uid}` exists | Everything in the Super Admin app; read any community |
| Community admin | `adminIndex/{uid}` with `active == true`, and its community `status == active` | Read and write only their own community's data |
| Member | No account | Nothing (data only) |

Rules:
- An admin belongs to exactly one community.
- If the admin is inactive, or their community is suspended, login is refused and all reads and writes fail.
- A community has at most 3 active admins (enforced in the app).

---

## 6. Data model (Firestore)

All community data lives under `organizations/{orgId}`. Timestamps are Firestore `Timestamp`.
Dates without time (due dates, expense dates, event dates) are stored as `"YYYY-MM-DD"` strings.

### 6.1 Top level

**`superAdmins/{uid}`**: `name: String`, `email: String`

**`adminIndex/{uid}`**: `orgId: String`, `active: Boolean`

**`config/mail`**: `senderEmail: String`, `appPassword: String`, `updatedAt: Timestamp`

**`organizations/{orgId}/config/sheets`**: `googleEmail: String`, `connectedBy: uid`, `connectedAt: Timestamp`, `status: "connected" | "needs_reconnect"` (ORG-35)

### 6.2 `organizations/{orgId}`

| Field | Type | Notes |
| --- | --- | --- |
| name | String | required |
| type | String | e.g. "Residents association", free text |
| address | String | optional |
| contactPhone | String | optional |
| status | String | `active` \| `suspended` |
| memberCount | Long | maintained with increments |
| eventCount | Long | maintained with increments |
| createdAt | Timestamp | |

**`…/admins/{uid}`**: `name`, `email`, `phone`, `active: Boolean`, `addedAt`, `addedBy: uid`

**`…/members/{memberId}`**

| Field | Type | Notes |
| --- | --- | --- |
| name | String | required |
| nickname | String | optional |
| phone | String | optional |
| email | String | optional |
| status | String | `active` \| `disabled` \| `archived` |
| joinedAt | Timestamp | |
| dateOfBirth | String | optional, `YYYY-MM-DD` |
| gender | String | optional, `male` \| `female` \| `other` |
| bloodGroup | String | optional, `a_pos` \| `a_neg` \| `b_pos` \| `b_neg` \| `ab_pos` \| `ab_neg` \| `o_pos` \| `o_neg` |
| job | String | optional |
| houseName | String | optional |
| shirtSize | String | optional, `xs` \| `s` \| `m` \| `l` \| `xl` \| `xxl` \| `xxxl` |

**`…/activity/{logId}`**: `action: String`, `actorUid`, `target: String`, `at: Timestamp` (audit trail)

### 6.3 Events: `organizations/{orgId}/events/{eventId}`

| Field | Type | Notes |
| --- | --- | --- |
| name | String | required |
| description | String | optional |
| startDate, endDate | String (date) | duration is derived, not stored |
| openingBalance | Long (paise) | default 0 |
| status | String | `active` \| `suspended` \| `archived` \| `closed` |
| roles | Array<{id, name}> | event roles, e.g. President, Secretary |
| totalCollected | Long (paise) | sum of payments, maintained with increments |
| totalExpenses | Long (paise) | sum of expenses, maintained with increments |
| createdAt, createdBy | | |
| sheetId | String | optional; Google spreadsheet id of the event's sheet backup (ORG-36) |
| sheetSyncedAt | Timestamp | optional; last successful sheet sync |
| sheetHash | Long | optional; hash of the content last written, so unchanged events are skipped |

**`…/events/{eventId}/eventMembers/{memberId}`** (doc id = memberId)

| Field | Type | Notes |
| --- | --- | --- |
| name | String | copy of member name for fast lists |
| roleId | String | references `event.roles[].id` |
| active | Boolean | false when member disabled |
| totalPaid | Long (paise) | running total across all schedules |
| paidByDue | Map<String, Long> | key `"{scheduleId}_{dueIndex}"`, value paise paid |
| addedAt | Timestamp | |

**`…/events/{eventId}/schedules/{scheduleId}`**

| Field | Type | Notes |
| --- | --- | --- |
| name | String | e.g. "Monthly contribution" |
| description | String | optional |
| amountPerMember | Long (paise) | total each member owes for this schedule |
| numberOfDues | Int | ≥ 1 |
| startDate, endDate | String (date) | |
| dues | Array<{index, label, dueDate, amount}> | generated once, see BR-1 to BR-4 |
| locked | Boolean | true after first payment |

**`…/events/{eventId}/payments/{paymentId}`**

| Field | Type | Notes |
| --- | --- | --- |
| memberId | String | |
| scheduleId | String | |
| dueIndex | Int | |
| amount | Long (paise) | positive; negative only for reversing entries |
| method | String | `online` \| `offline` |
| note | String | optional |
| paidOn | String (date) | defaults to today |
| recordedBy | uid | |
| createdAt | Timestamp | |
| reversesPaymentId | String? | set on reversing entries |
| emailStatus | String | `pending` \| `sent` \| `failed` |

**`…/events/{eventId}/expenses/{expenseId}`**: `title`, `amount: Long (paise)`, `date` (default today),
`description`, `paidByMemberId`, `recordedBy`, `createdAt`

---

## 7. Functional requirements: Super Admin app

| ID | Requirement |
| --- | --- |
| SA-1 | Login with email + password. Allowed only if `superAdmins/{uid}` exists; otherwise sign out and show an error. |
| SA-2 | Dashboard: total communities (active / suspended), total members across all communities, total events, recent activity. |
| SA-3 | Communities list with search by name and filter by status. |
| SA-4 | Add community: name, type, address, phone, **plus first admin** (name, email, phone, temporary password). Creates the org doc, the admin's Firebase Auth account (see SA-8), `admins/{uid}` and `adminIndex/{uid}`. |
| SA-5 | Community detail: edit details, list its admins (activate/deactivate), view its activity log. |
| SA-6 | Suspend / reactivate a community. Suspension blocks all its admins immediately. |
| SA-7 | Delete a community (with typed-name confirmation). Deactivates all its `adminIndex` entries. |
| SA-8 | Admin account creation must not sign out the super admin: create the Auth user through a **secondary `FirebaseApp` instance**, then delete that instance. |
| SA-9 | Settings: set the Gmail sender address and app password (`config/mail`). |
| SA-10 | Audit: every create / update / suspend / delete action writes an `activity` entry. |

---

## 8. Functional requirements: Organization app

### 8.1 Auth and profile

| ID | Requirement |
| --- | --- |
| ORG-1 | Login with email + password. Refuse if `adminIndex` missing or inactive, or community suspended. |
| ORG-2 | Session persists across restarts and works offline after first login. |
| ORG-3 | Profile: view/edit own name and phone, change password, log out. |
| ORG-4 | Co-admins: an admin can add up to 2 more admins (max 3 active) using the SA-8 secondary-app method, and deactivate other admins. |

### 8.2 Dashboard

| ID | Requirement |
| --- | --- |
| ORG-5 | Show member count, active event count, amount collected in the current due period across active events, number of members with pending current dues, recent payments. |

### 8.3 Members

| ID | Requirement |
| --- | --- |
| ORG-6 | Member list with search (name, nickname, phone) and status filter (active / disabled / archived). |
| ORG-7 | Add member: name (required), nickname, phone, email. A collapsible **Additional details** section holds optional fields: date of birth (not in the future), gender, blood group, job, house name, shirt size. |
| ORG-8 | Edit member details. |
| ORG-9 | Enable / disable a member at any time. Archive a member. Members are **never hard-deleted**. |

### 8.4 Events

| ID | Requirement |
| --- | --- |
| ORG-10 | Event list with status filter; actions: view, suspend, archive, close. |
| ORG-11 | Create event as a step-by-step flow: (1) name, description, start date, end date; (2) opening balance; (3) event roles (create/rename/remove role names); (4) add members from the community and assign each a role; (5) create one or more schedules (or skip and add later). |
| ORG-12 | Edit event details, roles and members after creation. Adding a member later makes them owe every due (BR-6). |
| ORG-13 | Event Overview: opening balance, total collected, total expenses, current balance (BR-9), and an income vs expense chart over time. |

### 8.5 Schedules and dues

| ID | Requirement |
| --- | --- |
| ORG-14 | Create schedule: name, description, start date, end date, amount per member, number of dues. Preview the generated dues before saving. |
| ORG-15 | Schedule screen: each due with label, due date, amount, and count of members paid / partly paid / pending. |
| ORG-16 | A schedule cannot be edited after its first payment (`locked`). |

### 8.6 Event members and due status

| ID | Requirement |
| --- | --- |
| ORG-17 | Event Members screen: each member with role, total paid, total due so far, pending amount, and current-due status (Paid / Partly paid / Pending). |
| ORG-18 | Filter by current-due status (Paid / Partly paid / Pending) and by role. |
| ORG-19 | Sort by: most behind first (overdue count, then pending amount), amount paid, name. |
| ORG-20 | Tap a member to see their per-due breakdown and payment history for the event. |

### 8.7 Payments

| ID | Requirement |
| --- | --- |
| ORG-21 | Mark payment: select member → event → schedule → due period. Amount pre-fills with what is still owed on that due and can be lowered (part payment). Fields: method (online/offline), note, date paid (default today). |
| ORG-22 | Reject amounts greater than what is owed on that due (BR-8). |
| ORG-23 | Save with one batched write (BR-7). On success, enqueue the backup email (ORG-28) and show a confirmation with a **Share to WhatsApp** button (ORG-31). |
| ORG-24 | Payments list for the event, newest first, with filters by member and schedule. Show "waiting to sync" for unsynced writes and the email status (sent / pending / failed + Resend). |
| ORG-25 | Correct a mistake by adding a reversing entry (negative amount, linked via `reversesPaymentId`). Payments cannot be edited or deleted. |

### 8.8 Expenses

| ID | Requirement |
| --- | --- |
| ORG-26 | Add expense: select event, title, amount, date (default today), description, paid by (select member). Saved with one batched write that also increments `event.totalExpenses`. |
| ORG-27 | Expense list per event, newest first, with total. |

### 8.9 Payment backup email

| ID | Requirement |
| --- | --- |
| ORG-28 | After every saved payment, enqueue a WorkManager job (network-required constraint, exponential backoff) that emails a report to **all active admins of the community**. |
| ORG-29 | The job reads `config/mail`, the event, its eventMembers and schedules; builds the report; sends via Jakarta Mail over Gmail SMTP; sets `payment.emailStatus` to `sent`. After 5 failed attempts set `failed`; the Payments list offers Resend. |
| ORG-30 | Email content: **Subject** `{Community} · {Event} · ₹{amount} from {Member} ({Due label})`. **Body**: the payment just recorded (member, schedule, due, amount, method, note, recorded by, time); event summary (opening balance, collected, expenses, balance); HTML table of all event members sorted by name with columns Member, Role, Paid so far, Due so far, Pending, Current due status. **Attachment**: same table as CSV. |

### 8.9a Google Sheets backup

A second copy of every due payment, outside Firestore, in the community's own Google Drive.

| ID | Requirement |
| --- | --- |
| ORG-35 | Profile → **Google Sheets backup**: an admin connects one Google account for the community (scope `drive.file`: the app sees only files it creates). Saved in `config/sheets`; the phone remembers it is signed in to that account. Other admins see who connected it and can connect the same account on their phone. Disconnect stops syncing; sheets stay in Drive. If Google refuses the saved access, status becomes `needs_reconnect` and Profile asks to reconnect. |
| ORG-36 | One spreadsheet per event, `{Community} - {Event}`, created with the event's first schedule. Its id is stored on the event in a transaction, so two phones can't both create one; a spreadsheet deleted in Drive is created again on the next sync. Event Overview shows the last sync time and **Open sheet**. |
| ORG-37 | One tab per schedule, named after it. Layout: merged title `{EVENT} · {SCHEDULE}`; header `NO, NAME, DUE{n} - {MONTH}…, TOTAL` (year added when the schedule spans years); one row per event member (disabled members too) sorted by name, with the amount paid on each due in rupees (blank when nothing); `=SUM` totals per row and per column. Tabs are found by hidden developer metadata, so renamed schedules rename their tab, deleted schedules lose theirs, and tabs admins add by hand are left alone. |
| ORG-38 | A WorkManager job (network required, backoff, 5 attempts) rewrites the event's tabs from Firestore after each payment, reversal, schedule change, event member change or event rename, plus an hourly job for all active events, which picks up payments recorded on other admins' phones. Only a phone signed in to the connected account syncs; on others the job does nothing. Unchanged events are skipped; **Sync now** rewrites everything. |
| ORG-39 | The sheet mirrors the app: edits made in the sheet are overwritten on the next sync. Email backup (ORG-28) is independent and unaffected. |

### 8.10 WhatsApp status message

| ID | Requirement |
| --- | --- |
| ORG-31 | **Share to WhatsApp** button on the Event Members screen and on the payment confirmation. Builds a plain-text list and opens WhatsApp (`com.whatsapp`) with it pre-filled via `ACTION_SEND`; the admin chooses the group. If WhatsApp is not installed, fall back to the Android share sheet. |
| ORG-32 | Format: first line `{Event} · paid so far · {d MMM yyyy}`, then one line per active event member: `{Name padded to longest name} - {total paid, whole rupees, right-aligned}`. Whole list wrapped in triple backticks so WhatsApp uses a monospace font. |
| ORG-33 | Order: members who have paid the **full event total** (sum of `amountPerMember` across all schedules) first, then everyone else; alphabetical by name within each group. Disabled members excluded. |

Example output:

````
```
Onam 2027 · paid so far · 12 Oct 2026
Anil   - 3000
Beena  - 1000
Cindy  -    0
Joseph - 1250
```
````

### 8.11 Reports

| ID | Requirement |
| --- | --- |
| ORG-34 | Export per event: payment history and member status, as PDF or CSV, shared through the Android share sheet. |

---

## 9. Business rules

| ID | Rule |
| --- | --- |
| BR-1 | **Amount per due** = floor(amountPerMember / numberOfDues) rounded down to whole rupees; the remainder is added to the **last** due. ₹3,000 / 6 → 6 × ₹500. ₹1,000 / 3 → ₹333, ₹333, ₹334. |
| BR-2 | **Due dates**: if start→end spans whole months and months ÷ numberOfDues is a whole number, due *i* = startDate + i × (months / N) months (same day of month, clamped to month end). Otherwise split the day span evenly: due *i* = startDate + floor(i × days / N). First due (i = 0) is on the start date. An end date one day short of a whole month (1 Jan to 31 Dec) also counts as whole months. |
| BR-3 | **Due label**: `Due {i+1} of {N}, {MMM yyyy}` of its due date. |
| BR-4 | The schedule total is **per member**. Every event member owes `amountPerMember` for each schedule. |
| BR-5 | **Current due** = the latest due whose due date ≤ today; before the first due date it is the first due. **Status** for a member on a due: Paid if paid ≥ due amount, Partly paid if 0 < paid < amount, Pending if 0. **Overdue count** = past dues not fully paid. **Due so far** = sum of amounts of dues with due date ≤ today. |
| BR-6 | A member added to an event at any time owes **every** due of every schedule (full total), including dues already past, which show as overdue immediately. |
| BR-7 | A payment is saved in **one batched write** (not a transaction, so it works offline): create the payment doc; `FieldValue.increment` on `eventMember.totalPaid`, `eventMember.paidByDue.{scheduleId}_{dueIndex}`, and `event.totalCollected`; set `schedule.locked = true`. |
| BR-8 | A payment amount must be > 0 and ≤ (due amount − already paid on that due). Overpayment of a due that happens anyway (two admins offline) is flagged in the UI for correction by a reversing entry. |
| BR-9 | **Event balance** = openingBalance + totalCollected − totalExpenses. |
| BR-10 | Disabled members are hidden from Mark payment pickers, pending lists and the WhatsApp message, but their history is kept and visible. |
| BR-11 | Payments are immutable except `emailStatus`. Never delete payments. |
| BR-12 | Counters (`memberCount`, `eventCount`, `totalCollected`, `totalExpenses`) are always updated with `FieldValue.increment` in the same batch as the change they count. |

---

## 10. Security rules (Firestore)

Requirements:
- A community admin can read and write only their own community's subtree.
- Suspending a community or deactivating an admin locks them out immediately.
- Payments: create only by admins of that community; update only the `emailStatus` field; delete never.
- `config/mail`: readable by super admins and active admins; writable by super admins only.
- `organizations/{orgId}` create/delete: super admin only.

Reference implementation:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    function isSuper() { return request.auth != null
      && exists(/databases/$(db)/documents/superAdmins/$(request.auth.uid)); }
    function idx() { return get(/databases/$(db)/documents/adminIndex/$(request.auth.uid)).data; }
    function isAdminOf(orgId) {
      return request.auth != null && idx().active && idx().orgId == orgId
        && get(/databases/$(db)/documents/organizations/$(orgId)).data.status == 'active';
    }

    match /superAdmins/{uid} { allow read: if request.auth.uid == uid; allow write: if false; }
    match /config/mail { allow read: if isSuper() || (request.auth != null && idx().active);
                         allow write: if isSuper(); }
    match /adminIndex/{uid} {
      allow read: if request.auth.uid == uid || isSuper();
      allow write: if isSuper() || isAdminOf(request.resource.data.orgId);
    }

    match /organizations/{orgId} {
      allow read: if isSuper() || isAdminOf(orgId);
      allow create, delete: if isSuper();
      allow update: if isSuper() || isAdminOf(orgId);

      match /{coll}/{docId} { allow read, write: if isSuper() || isAdminOf(orgId); }

      match /events/{eventId}/{coll}/{docId} {
        allow read: if isSuper() || isAdminOf(orgId);
        allow write: if coll != 'payments' && isAdminOf(orgId);
      }
      match /events/{eventId}/payments/{paymentId} {
        allow read: if isSuper() || isAdminOf(orgId);
        allow create: if isAdminOf(orgId);
        allow update: if isAdminOf(orgId)
          && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['emailStatus']);
        allow delete: if false;
      }
    }
  }
}
```

Test these rules with the Firebase Emulator Suite before release.

---

## 11. Offline behaviour

| ID | Requirement |
| --- | --- |
| NFR-1 | Firestore offline persistence enabled; lists and totals load without network. |
| NFR-2 | Payments and expenses saved offline are queued and sync automatically; the UI marks them "waiting to sync" (`hasPendingWrites`). |
| NFR-3 | Backup emails for offline payments are sent by WorkManager once the network returns, even if the app was closed. |
| NFR-4 | Use batched writes with increments, never transactions, for anything an admin can do offline. |

---

## 12. Email sender setup (operational)

1. Create a dedicated Gmail account used only for Bharavahi reports.
2. Enable 2-Step Verification; create an **app password** (Google Account → Security → App passwords).
3. Enter address + app password in Super Admin → Settings (saved to `config/mail`).

Known trade-offs (accepted):
- The app password is readable by any signed-in admin's device (rooted devices could extract it). If leaked, revoke it in Google and set a new one. **Do not** put it in `.env`, `BuildConfig`, `local.properties` or the APK.
- Gmail sends about 500 emails/day per account, shared by all communities. Moving sending to a Cloud Function later only changes the mail sender in `core`.

---

## 13. Non-functional requirements

| ID | Requirement |
| --- | --- |
| NFR-5 | All money arithmetic uses `Long` paise; formatting only at the UI edge. |
| NFR-6 | Unit tests in `core` for due generation (BR-1 to BR-3), status calculation (BR-5), balance (BR-9), WhatsApp text (ORG-32, ORG-33), email report building (ORG-30) and the sheet layout and sync (ORG-36, ORG-37). |
| NFR-7 | Lists are paged / use Firestore queries with indexes; screens stay responsive with 500 members per community. |
| NFR-8 | Material 3 theming with light and dark modes, per section 14 (UI theming). |
| NFR-10 | English only. All user-visible text lives in `strings.xml` (no hard-coded strings in composables). |
| NFR-9 | No secrets committed to the repository. `../app-superadmin/google-services.json` per app is fine to commit for a private repo; otherwise keep it out of version control. |

---

## 14. UI theming

Reference screenshot: `theme-reference.jpg` (colour reference only). Preview of the result: `theme-preview.png`.
Both files are next to this one.

### 14.1 Swappable theme architecture

| ID | Requirement |
| --- | --- |
| UI-1 | All colours are defined in `core/ui/theme/`. Each app's look is one `BrandPalette` object (light `ColorScheme`, dark `ColorScheme`, light and dark `StatusColors`). Changing the theme = editing or replacing that object. |
| UI-2 | `BharavahiTheme(palette, mode)` wraps `MaterialTheme` and provides `LocalStatusColors`. Each app calls it once in `MainActivity`: `app-org` with `TealOrangePalette`, `app-superadmin` with `SlatePalette`. |
| UI-3 | Screens use only `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, `MaterialTheme.shapes.*` and `LocalStatusColors.current.*`. No `Color(0x…)` outside `core/ui/theme` (enforced by a custom lint rule or detekt check). |
| UI-4 | Dynamic color (Android 12+ wallpaper colours) is disabled. |
| UI-5 | Theme mode: System (default), Light or Dark, chosen in Profile and stored with DataStore. |
| UI-6 | Status (Paid, Partly paid, Pending) is always shown with a text label or icon as well as colour. |
| UI-7 | Every text/background pair meets WCAG AA contrast (4.5:1 for body text). |

```kotlin
data class BrandPalette(
    val light: ColorScheme, val dark: ColorScheme,
    val statusLight: StatusColors, val statusDark: StatusColors,
)
@Immutable data class StatusColors(
    val paid: Color, val partlyPaid: Color, val pending: Color,
    val income: Color, val expense: Color,
)
val LocalStatusColors = staticCompositionLocalOf<StatusColors> { error("No StatusColors") }
```

### 14.2 Organization app palette (`TealOrangePalette`)

| Material role | Light | Dark | Used for |
| --- | --- | --- | --- |
| primary | `#1B4A5A` | `#8FCFE3` | App bar icons, selected tab, links |
| onPrimary | `#FFFFFF` | `#00344A` | |
| primaryContainer | `#1B4A5A` | `#1B4A5A` | Hero cards (event balance, collection ring) |
| onPrimaryContainer | `#FFFFFF` | `#E3F2F7` | |
| secondary | `#FEAE57` | `#FEAE57` | Main buttons, centre Mark payment button, progress ring |
| onSecondary | `#1B4A5A` | `#3A2300` | Text on orange (not white: white on orange is 1.8:1) |
| tertiaryContainer | `#FFEEDE` | `#3D2E20` | Highlight cards (pending dues) |
| onTertiaryContainer | `#17323C` | `#FFDCC0` | |
| background | `#FAFAFA` | `#0F1E24` | |
| surface | `#FFFFFF` | `#16282F` | Cards, sheets |
| onBackground / onSurface | `#17323C` | `#E3EEF1` | Main text |
| onSurfaceVariant | `#6B7478` | `#9DB0B6` | Secondary text |
| outline | `#D2D2D2` | `#2E444C` | Dividers, inactive icons |
| tertiary (orange text accent) | `#B26100` | `#FEAE57` | Highlighted words in headings |

Status colours:

| StatusColors | Light | Dark |
| --- | --- | --- |
| paid | `#2E7D5B` | `#7FD3A8` |
| partlyPaid | `#A15C00` | `#FFC27A` |
| pending | `#B3261E` | `#FFB4AB` |
| income | `#1B4A5A` | `#8FCFE3` |
| expense | `#FEAE57` | `#FEAE57` |

### 14.3 Shape, type and layout (Organization app)

| ID | Requirement |
| --- | --- |
| UI-8 | Shapes: cards 20dp corners, buttons fully rounded (pill), text fields 12dp, centre action button circular. |
| UI-9 | Font: Poppins, bundled in `res/font` (works offline), mapped onto the Material 3 type scale. Font is part of the theme and swappable the same way as colours. |
| UI-10 | Bottom navigation: Dashboard, Events, raised orange **Mark payment** (+) button in the centre, Members, Profile. |
| UI-11 | Dashboard: teal hero card with the active event's collected amount and a progress ring (% of expected total), peach cards for pending dues, white cards for lists. |
| UI-12 | Illustrations are out of scope for v1. |

### 14.4 Super Admin app (`SlatePalette`)

| ID | Requirement |
| --- | --- |
| UI-13 | Keep it simple: standard Material 3 components, top app bar and list-based navigation, no bottom bar, no hero cards. |
| UI-14 | Palette: primary `#455A64` (light) / `#B0C4CE` (dark), Material 3 default neutral surfaces, same `StatusColors` as the Organization app. Light and dark mode supported via UI-5. |

---

## 15. Out of scope (v1)

- Member logins or a member-facing app.
- Online payment collection (UPI/gateway). Admins only *record* payments.
- Automated WhatsApp sending (WhatsApp Business API). Sharing is manual.
- Cloud Functions or any custom backend.
- iOS and web versions.
