# Tasks

## 1. Server: both lists newest first

- [x] 1.1 Change `findOwnedBy` and `findVisibleTo` in `SqliteGroupRepository` to `ORDER BY created_at DESC, id DESC`; rename and flip `ownedGroupsAreListedInCreationOrderAndOnlyForTheirOwner` to newest first, and add repository tests for the visible list's order, two groups created in the same second (set `created_at` to one value in SQL; the later `id` comes first), and a group that is edited, given participants, moved to Pending and drawn keeping its place; verify `./kotlin test -m server` passes.
- [x] 1.2 Update `listContainsOnlyTheCallersGroups` in `GroupRoutesTest` (it expects oldest first) and add route tests that `GET /admin/groups` and `GET /groups` return newest first, that a group made later (as a duplicate is) is first, and that editing and drawing the oldest group leaves it last; verify the server suite passes.

## 2. Shared: the search rules and the filter state

- [x] 2.1 Add `description` to `GroupRow` (and `toRow()`, blank counts as none), a `GroupListItem` interface (`name`, `description`, `state`) implemented by `GroupRow` and `AdminGroup`, and a pure `filterGroups(items, query, states)`; update the expected `GroupRow` in `GroupsViewModelTest`, and add tests for every scenario of "What the search matches", "State chips", "Search and chips work together" and "Groups in a state without a chip", plus that the order is kept and a missing description counts as empty; verify `./kotlin test -m shared` passes on every platform it runs on.
- [x] 2.2 Add `GroupListFilter` (query and chosen states as Compose state, `setQuery`, `toggle`, `clear`) with tests for toggling several states, switching one off again, clearing both, and a query of only spaces; verify the tests pass.

## 3. Shared: view models and screens

- [x] 3.1 Give `GroupsViewModel` a filter offering Pending, Active and Archived and `AdminGroupsViewModel` a filter offering Draft, Pending, Active and Archived; add view-model tests that each offers exactly those chips, that the filter is kept when the list reloads, and that the loaded list itself is never changed by it; verify the tests pass.
- [x] 3.2 Add `GroupSearchBar` (search field with a clear control, and the chips in a wrapping row) and the strings, and use it in both screens above the list inside the loaded content, with a scrollable "No group matches" message and a "Clear" action when the filter hides everything, and no bar when there are no groups; verify by running the web app on localhost as a participant and as an administrator that each scenario of `client-group-search` behaves as written and that "Duplicate" puts the copy on top.
- [x] 3.3 Check on a real Android device that the keyboard's search action closes the keyboard, that pulling the list down still reloads it with the bar in place, that four chips wrap on a narrow screen, and that the search and chips are still there after opening a group and coming back; verify by running the app and observing each.

## 4. Integration

- [x] 4.1 Run `openspec validate search-and-filter-groups --strict` and the whole test suite for the server, shared and domain modules; verify they pass with no new compiler warnings.
