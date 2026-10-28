# client-layout Specification

## Purpose
How the signed-in app lays itself out on wide screens, so that every screen, bar and button shares one standard width.

## Requirements

### Requirement: One standard maximum width
On a window wider than the standard width the signed-in app SHALL keep its top bar, its content, its floating buttons and its bottom bar within one centered column of that same width, the same on every screen, instead of stretching them across the window. On a window as narrow as the standard width or narrower, nothing SHALL change. The standard width SHALL be defined once for the whole app. Sheets and dialogs are not part of this requirement.

#### Scenario: Wide window
- **WHEN** the app is shown in a window much wider than the standard width
- **THEN** the top bar, the content and the bottom bar are the same width and centered, and a floating button sits at the bottom end of that column, not at the window's edge

#### Scenario: Narrow window
- **WHEN** the app is shown on a phone
- **THEN** the screens, bars and buttons fill the width as before

#### Scenario: Every screen
- **WHEN** the person moves between the lists, a group, the profile and the owner's screens on a wide window
- **THEN** all of them are the same width
