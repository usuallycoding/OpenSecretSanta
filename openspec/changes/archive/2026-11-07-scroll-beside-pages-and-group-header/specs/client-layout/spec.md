## ADDED Requirements

### Requirement: Scrolling works beside a page
Scrolling in the empty space beside a page, by the mouse wheel, the trackpad or a touch drag, SHALL scroll the shown page in the same direction and by the same amount as over it. This applies wherever a page is narrower than its area: on a wide window for the pages that keep the standard width, and between the standard width and the wide breakpoint. Over the page, and for a page that fills the window or has nothing to scroll, nothing SHALL change.

#### Scenario: Wheel in the empty space
- **WHEN** the window is wide, a page is taller than the window and the pointer is in the empty space beside it
- **THEN** turning the mouse wheel scrolls the page

#### Scenario: Drag in the empty space
- **WHEN** the same page is shown on a touch screen and the person drags up in the empty space beside it
- **THEN** the page scrolls with the drag, and keeps moving when the finger is lifted as it does over the page

#### Scenario: Between the standard width and the wide breakpoint
- **WHEN** the window is wider than the standard width but narrower than 840dp, so that the bars and the page are a centered column with empty space on both sides
- **THEN** scrolling in the empty space beside the column scrolls the page

#### Scenario: Over the page
- **WHEN** the person scrolls over the page itself
- **THEN** it scrolls as it did, once, and not twice

#### Scenario: A page that fits
- **WHEN** the page is shorter than the window
- **THEN** scrolling beside it does nothing

#### Scenario: A page that fills the window
- **WHEN** a page uses the whole area, such as Giving and Receiving side by side
- **THEN** each part scrolls over itself as before
