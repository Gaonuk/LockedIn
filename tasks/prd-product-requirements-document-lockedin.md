# Product Requirements Document: LockedIn

## Overview
**Product Name:** LockedIn  
**Platform:** Android (Kotlin)  
**Minimum SDK:** Android 10 (API 29)  
**Description:** An NFC-powered app blocker that helps users achieve digital wellbeing and productivity by blocking distracting apps. Users tap an NFC tag to activate pre-configured blocking schedules, with the only way to unblock being another NFC tap—creating a strong commitment device.

## Problem Statement
Users struggle with phone addiction and maintaining focus during work hours. Existing app blockers are too easy to bypass with a simple toggle. LockedIn solves this by requiring physical NFC interaction to control blocking, adding meaningful friction that supports intentional phone usage.

## Target Users
- Individuals seeking to reduce phone addiction and screen time
- Professionals needing distraction-free work periods
- Anyone who wants a "commitment device" for digital wellbeing

---

## User Stories

### US-1: First-Time Setup
**As a** new user  
**I want to** complete initial setup with permissions and app selection  
**So that** LockedIn is ready to use with my NFC tag

**Acceptance Criteria:**
- User is prompted to grant Accessibility Service permission with clear explanation of why it's needed
- User can select apps to block from a checklist of all installed apps
- User can configure their blocking schedule (e.g., 9am - 8pm)
- Setup cannot be completed without granting Accessibility permission
- User sees confirmation when setup is complete

**Dependencies:** None

---

### US-2: NFC Tag Activation
**As a** user  
**I want to** tap my NFC tag to instantly activate my pre-configured blocking schedule  
**So that** I can start my focus session without navigating through the app

**Acceptance Criteria:**
- App registers to receive NFC intents even when not in foreground
- Tapping NFC tag activates the pre-configured schedule immediately
- User receives haptic feedback and/or toast confirmation on activation
- Blocking becomes active within 1 second of NFC tap
- Works with any standard NFC tag (no special encoding required)

**Dependencies:** US-1

---

### US-3: App Blocking with Overlay
**As a** user with active blocking  
**I want to** see a lock screen overlay when I try to open a blocked app  
**So that** I'm reminded of my commitment and shown my remaining focus time

**Acceptance Criteria:**
- When a blocked app is detected in foreground, overlay appears within 500ms
- Overlay displays message: "This app is currently blocked"
- Overlay shows countdown timer with time remaining until schedule ends
- Overlay covers the entire screen and cannot be dismissed by back button
- Blocked app is sent to background when overlay appears

**Dependencies:** US-1, US-2

---

### US-4: Blocked App Detection via Accessibility Service
**As a** user  
**I want** the app to reliably detect when I open blocked apps  
**So that** blocking works consistently without workarounds

**Acceptance Criteria:**
- Accessibility Service monitors foreground app changes
- Detection works for all apps in the block list
- Service runs reliably in background without being killed
- Minimal battery impact (< 2% additional drain per day)
- Service automatically restarts if killed by system

**Dependencies:** US-1

---

### US-5: NFC Tap While Blocking Active
**As a** user with active blocking  
**I want to** tap NFC and see my remaining time with options to extend or end  
**So that** I can adjust my focus session based on my current needs

**Acceptance Criteria:**
- Tapping NFC while blocking is active shows a dialog/screen
- Dialog displays remaining time in the current session
- User can choose to "Extend" (add more time) or "End Now"
- If user chooses "End Now", they must tap NFC again to confirm (preventing accidental unblock)
- If user chooses "Extend", they can add preset durations (30m, 1h, 2h)

**Dependencies:** US-2

---

### US-6: Schedule Configuration
**As a** user  
**I want to** configure my blocking schedule in the app settings  
**So that** NFC activation uses my preferred time window

**Acceptance Criteria:**
- User can set start time and end time for the schedule
- Schedule is saved and persists across app restarts
- User can modify schedule at any time when blocking is not active
- Schedule cannot be modified while blocking is active
- Validation prevents invalid schedules (end time must be after start time)

**Dependencies:** US-1

---

### US-7: Persistent Notification During Blocking
**As a** user with active blocking  
**I want to** see a persistent notification showing my remaining time  
**So that** I always know my focus session status at a glance

**Acceptance Criteria:**
- Notification appears when blocking is activated
- Notification shows remaining time, updated every minute
- Notification displays quick stat (e.g., "3 apps blocked")
- Notification cannot be dismissed while blocking is active
- Notification is removed when blocking ends

**Dependencies:** US-2

---

### US-8: Statistics and Tracking
**As a** user  
**I want to** view detailed statistics about my focus sessions  
**So that** I can track my progress and stay motivated

**Acceptance Criteria:**
- Track number of blocked app open attempts per session
- Calculate and display "time saved" based on blocked attempts
- Show daily and weekly trend graphs
- Display current streak (consecutive days with completed focus sessions)
- Stats persist across app updates and are stored locally

**Dependencies:** US-3

---

### US-9: Blocking Reset on Phone Restart
**As a** user  
**I want** blocking to reset to OFF when my phone restarts  
**So that** I must intentionally tap NFC to re-enable focus mode

**Acceptance Criteria:**
- Blocking state is cleared on device boot
- User must tap NFC to start a new blocking session after restart
- Any active schedule is cancelled on restart
- Notification is cleared on restart
- Stats from interrupted session are still recorded

**Dependencies:** US-2

---

### US-10: App Selection Management
**As a** user  
**I want to** modify my blocked apps list from settings  
**So that** I can adjust which apps are blocked over time

**Acceptance Criteria:**
- User can access app selection from settings menu
- All installed apps shown in alphabetical checklist
- Currently blocked apps are shown as checked
- Changes can only be made when blocking is not active
- System apps can optionally be shown/hidden

**Dependencies:** US-1

---

### US-11: Material Design 3 Dark Theme UI
**As a** user  
**I want** a clean, dark-themed interface  
**So that** the app is visually appealing and easy on the eyes

**Acceptance Criteria:**
- App uses Material Design 3 components throughout
- Dark theme is the default and only theme
- UI is simple and functional with minimal decoration
- Typography follows Material 3 guidelines
- All screens are consistent in visual style

**Dependencies:** None

---

### US-12: Streak Counter Display
**As a** user  
**I want to** see my current streak prominently displayed  
**So that** I'm motivated to maintain my focus habits

**Acceptance Criteria:**
- Streak counter shown on main screen
- Streak increments when a full scheduled session is completed
- Streak resets if a day passes without completing a session
- Visual celebration when streak milestones are reached (7, 30, 100 days)
- Streak history is preserved in statistics

**Dependencies:** US-8

---

## Technical Architecture

### Core Components
1. **Accessibility Service** - Monitors foreground app changes
2. **NFC Receiver** - Handles NFC tag detection via BroadcastReceiver
3. **Overlay Service** - Draws lock screen using SYSTEM_ALERT_WINDOW
4. **Foreground Service** - Maintains blocking state and notification
5. **Room Database** - Stores settings, block list, and statistics

### Required Permissions
- `SYSTEM_ALERT_WINDOW` - For drawing overlay
- `ACCESSIBILITY_SERVICE` - For detecting foreground apps
- `NFC` - For reading NFC tags
- `FOREGROUND_SERVICE` - For persistent blocking service
- `RECEIVE_BOOT_COMPLETED` - For handling device restarts

### Key Dependencies
- Jetpack Compose (UI)
- Material 3 (Design system)
- Room (Local database)
- Kotlin Coroutines & Flow (Async operations)
- MPAndroidChart or similar (Statistics graphs)

---

## Out of Scope (v1)
- Multiple profiles/block lists
- Cloud sync or backup
- Widget support
- Scheduling multiple time windows per day
- Social/accountability features
- iOS version

---

## Success Metrics
- User completes at least 5 focus sessions in first week
- Average blocked app attempts decrease over time
- 30-day retention rate > 40%
- Streak maintenance rate > 50% of active users