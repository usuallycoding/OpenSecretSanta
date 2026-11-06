## MODIFIED Requirements

### Requirement: A switch for this device
The sheet SHALL start with a switch for the current device. Turning it on SHALL ask for the system's notification permission (Android 13 and later, the browser) when it has not been granted, and when granted SHALL register the device with the server. Turning it off SHALL remove the device's registration. The switch SHALL show on only while the permission is granted and the device is registered. On Android, when the permission is refused and the system will not ask again, the sheet SHALL offer a button that opens the app's notification settings.

#### Scenario: Turn on
- **WHEN** the person turns the switch on and grants the permission
- **THEN** the device is registered with the server and the switch stays on

#### Scenario: Permission refused
- **WHEN** the person turns the switch on and refuses the permission
- **THEN** the switch goes back off and the sheet says that notifications must be allowed in the device or browser settings

#### Scenario: Refused, and the system can still ask
- **WHEN** an Android 13 or later device is asked for the first time and the person refuses
- **THEN** the switch goes back off and the sheet says so, with no settings button, because turning the switch on again asks again

#### Scenario: Refused, and the system will not ask again
- **WHEN** the person turns the switch on and Android refuses the permission without asking, because it was refused before until the system stopped asking
- **THEN** the switch goes back off and the sheet says that notifications are blocked for the app, with an "Open settings" button

#### Scenario: Android 12 or older
- **WHEN** the person turns the switch on, on a device that cannot ask for the permission, while the app's notifications are off in the system settings
- **THEN** the switch goes back off and the sheet says that notifications are blocked, with an "Open settings" button

#### Scenario: Open settings
- **WHEN** the person chooses "Open settings"
- **THEN** the system's notification settings of the app open, where notifications can be allowed

#### Scenario: Back from the settings
- **WHEN** the person returns to the app after allowing notifications there
- **THEN** the message and the button are gone, and the switch stays off until the person turns it on

#### Scenario: Back from the settings, still blocked
- **WHEN** the person returns to the app without having allowed notifications
- **THEN** the message and the button are still shown

#### Scenario: The web
- **WHEN** the person turns the switch on in a browser that has blocked notifications for the site
- **THEN** the switch goes back off and the sheet says that notifications must be allowed in the browser settings, with no settings button

#### Scenario: Turn off
- **WHEN** the person turns the switch off
- **THEN** the device is removed from the server and receives nothing more

#### Scenario: Permission taken away
- **WHEN** the person withdraws the permission in the system settings and then opens the sheet
- **THEN** the switch is off

#### Scenario: Only on request
- **WHEN** the app starts or the person signs in
- **THEN** no permission is asked for until the person turns the switch on
