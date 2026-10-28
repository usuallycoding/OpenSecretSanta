# Spec Delta

## REMOVED Requirements

### Requirement: Promote a user to admin
**Reason**: Administrators can no longer be created through the API. The first administrator comes from the server's configuration and is the only way to have one.
**Migration**: None. Nothing in the app called the route; a client that still does gets 404.
