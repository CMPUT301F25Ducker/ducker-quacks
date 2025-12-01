# ducker-quacks (DuckDuckGoose)

## Purpose
An Android app to create, manage and register for events. Organizers can create and manage events and attendees; entrants can browse events, register (lottery/waitlist flows), and view event details. Admins can manage users, oversee the system, and perform administrative tasks such as user role assignment and high-level cleanup.

## Main Functionality
- Browse and filter upcoming events (date, registration windows, cost, interest).
- View event details and image gallery.
- Authentication and user profiles via Firebase Auth and Firestore.
- Entrant flows: register for events, join waiting list, see lottery/waitlist status.
- Organizer flows: create/edit events, view/manage attendees.
 - Admin flows: manage user accounts and roles, access admin console for system-level operations.

## High-level Design / Class Structure

- Language & platform: Java on Android. Backend: Firebase Firestore and Auth (for authentication).

This project follows a simple, modular class-structure that separates concerns across UI, data, and utilities:
- Controller: each screen is represented by an Activity that acts as a controller. Activities wire views, adapters and services and handle lifecycle events.
- View: RecyclerView adapters (and associated ViewHolders) are responsible for rendering lists/cards and wiring item click handlers. Adapters keep UI-binding logic isolated from Activity controllers.
- Model: Plain Java model classes represent Firestore documents and domain entities (for example: Event, User, WaitlistEntry). Models are immutable-ish data carriers used by adapters and services.
- Firebase Controller: Small service/helper classes or methods encapsulate Firestore and Auth interactions (queries and event listeners). Centralizing data access simplifies testing and error handling.
- Utilities & UI Wiring: Shared helpers (date/price formatters, TopBar wiring, dialog helpers, animations) are factored into reusable utilities to avoid duplication across Activities.
- Resources & Layouts: XML layouts, drawables, styles and string resources live in `res/` and are referenced by Activities and Adapters for consistent UI.

This organization emphasizes separation of concerns: Activities orchestrate UI flows, Adapters render lists, Models hold data, and Services handle persistence.

## Data Model (Firestore Database)
- `events` collection — event documents (name, description, dates, cost, maxSpots, organizerId, image paths, waitingList, etc.)
- `notifications` collection — user settings and organizer/admin based
- `users` collection — user profiles and role/accountType
- `waitlist` collection — per-user waitlist entries (used for lottery/waitlist flows)

## Notes & Testing
- The app relies on Firestore structure described above; some features assume specific fields exist (ex. `attendees`, `waitingList`).
- Unit and instrumentation tests live under `app/src/test` and `app/src/androidTest` if added; run via Android Studio or Gradle.

### Interactive Prototype
You can explore our full interactive prototype and detailed design on Figma:
**[View Complete UI Design and Prototype](https://www.figma.com/design/Z5DoPnLCSFLIKAF0BKwZkW/301-Project?node-id=0-1&m=dev&t=W6xtrOILcoEZOFJP-1)**

### UML Diagram
You can explore our complete UML diagram here:
**[View Complete UML Diagram]()**

## Contributors
- Dhruv Bhatia — `DhruvBhatia14`
- Yousef Moussa — `YousefMoussa`
- Nasif Qadri — `n5q`
- Muhammad Murtaza — `originalvelocity`
- Tabish Khan — `Tak701`
- Omar Mahmoud — `oamkotb`