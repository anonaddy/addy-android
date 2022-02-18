# Changelog

## [v3.3.6] - 2022-02-18

### Fixed/Improved

- Fixed a bug where self-hosted instances with version 0.10.0 or higher would be marked as incompatible

## [v3.3.5] - 2022-02-02

> **_NOTE:_**  3.3.0-3.3.4 Never released because Google refused the new updates multiple times.

### Added

- 📧 You can now filter and sort aliases in the aliases tab for a clearer view
- ➕ Thanks to a new floating action button you can now create aliases while scrolling

### Fixed/Improved

- 🎨 Moved to MD3 design
  - The app got a slightly different but more vibrant color palette
  - Enable "use dynamic colors" in the appearance settings of the app to let it blend in with your system theme (Android 12 or higher required). This
    eliminates the need for the "Material You" builds
  - You can now tap the appbar to scroll up the page
  - The (new) bottomnavigation now has animated icons
  - The big widget-style has been redesigned
- 📃 Added the "Run rule on" options for creating rules
- 🕷 Fixed a bug where folding/unfolding your foldable while creating a rule would reset the screen
- 🕷 Fixed a bug where the app would crash when coming back to it after having it idle for a certain amount of time
- ✨ The number of API requests that are made is now reduced.
- ✨ Performance improvements and other things I might have forgot to make the app even smoother

## [v3.2.1] - 2021-10-17

### Added

- 🗃️ Introducing AnonAddy Backup, securely backup app configuration for the move to another device or re-installation of the app.
  - Choose the location where the backups are stored on your device
  - Encrypt the backup with your own password
  - Make periodic backups without worry
  - Use the backup log to view the status of previous backup jobs
  - Get notified when a periodic backup fails
- Added the ability to restore backups from the setup screen

### Fixed/Improved

- Re-designed the logmanager
- Re-designed the setup screen
- Performance improvements and other things I might have forgot to make the app even smoother

## [v3.1.3] - 2021-10-02

### Fixed/Improved

- Fixed some string issues
- Changed the return key into a search key on the keyboard when using the global search option
- Performance improvements and other things I might have forgot to make the app even smoother

## [v3.1.2] - 2021-09-11

### Fixed/Improved

- Fixed the web intent resolution not working on some devices
- Created a separate integration section for web intent resolution under features/integrations settings
- Performance improvements and other things I might have forgot to make the app even smoother

## [v3.1.1] - 2021-08-21

### Fixed/Improved

- Fixed the alias integration not working on some devices
- Performance improvements and other things I might have forgot to make the app even smoother

## [v3.1.0] - 2021-08-15

### Added

- View and manage your failed deliveries! Optionally, you can also receive a notification in case of failed deliveries

### Fixed/Improved

- More UI changes that make the app look better
  - The iconography has changed from Material to Tabler for a unique experience
  - More animations

- Performance improvements and other things I might have forgot to make the app even smoother

## [v3.0.0] - 2021-07-31

### Added

- Added a brand new statistic widget for flexing with your AnonAddy stats, resize the widget to show and rearrange the stats
- It's now possible to "forget" your alias from the app
- Introducing "Email action integration", create aliases and send emails from aliases from... anywhere! Without opening the app! Open the new "
  Features and integrations" section in the app settings to learn more
- Keep the app up-to-date with the new "AnonAddy updater", allowing your to check for updates and get notified when new updates are available

### Fixed/Improved

- 🎨 Massive UI redesign with Android 12 and simplicity in mind
  - One-handed interface, simply swipe down from any screen to move content down for better accessibility
  - Cards and raises are now used for a clean-looking, easier-to-understand design
  - The initials of your AnonAddy username are now used instead of the user icon, along with your system accent color
  - A new app icon, that changes according to your system accent (Android 12 only)
  - Softer colors
  - Bigger, clearer buttons with more space between elements
  - Animations! Most progressbars are now replaced with morphing buttons
  - Improved shimmers
  - Improved tablet layout

- Updated shared domains list
- Select the recipients for an alias when creating a new alias
- Performance improvements and other things I might have forgot to make the app even smoother

## [v2.1.0] - 2021-05-16

### Fixed/Improved

- [IMPROVED] Support for Android 12
- [IMPROVED] Added keyboard animations
- [IMPROVED] Move content from bottomdialogs above keyboard if there is space
- [IMPROVED] Overall app improvements

## [v2.0.3] - 2021-04-19

### Fixed/Improved
- [IMPROVED] Updated libraries
- [IMPROVED] Added "Use Reply-To Header For Replying" to AnonAddy settings
- [IMPROVED] Overall app improvements

## [v2.0.2] - 2021-02-08

### Fixed/Improved
- [IMPROVED] Updated shared domains

## [v2.0.1] - 2021-01-18

### Added
- 📋 [NEW] Sending an email from an alias will now also copy the recipients to the clipboard

## [v2.0.0] - 2021-01-11

### Added
- 🌐 [NEW] Get full control over how often background data such as widgets are refreshed
- 👁️ [NEW] [APP EXCLUSIVE] Watch your aliases to get notified when it forwards a new email
- ✉  [NEW] Quickly send emails from an alias
- 🔤  [NEW] Added random characters alias format
- 💳 [NEW] View the subscription end date
### Fixed/Improved
- 🖌️ [IMPROVED] There are some major and minor UI improvements making the app look even better on phones, tablets and foldables
- 🔐 [IMPROVED] The app just got even more secure as it now also requires authentication for actions being executed from the widget or intents
- 🧈 [IMPROVED] Lot's of love and butter has gone into this release to make the app smoother and more lightweight than ever before. Especially the widget got a big improvement
- 🦋 [IMPROVED] Minor bugs have been fixed

## [v1.1.3] - 2020-12-08

### Fixed/Improved
- 🐛 [BUG FIX] Deleted alias section is not collapsed by default
- 🙋 [FEATURE REQUEST] Switch between panels by swiping left/right

## [v1.1.2] - 2020-11-21

### Fixed/Improved
- 🖥️ Optimized UI for big screens
- 🐛 [BUG FIX] Improved responsiveness when switching fragments
- 🙋 [FEATURE REQUEST] Added link to gitlab page
- 🙋 [FEATURE REQUEST] Separated deleted aliases into a different section

## [v1.1.1] - 2020-10-24

### Fixed/Improved
-  Added the forgotten piece of the custom alias format >.<
- Fixed crash at double Biometrics authentication prompt when dark mode was enabled

## [v1.1.0] - 2020-10-24

### Added

- 📋 Added new rule-editor (beta)
- The feature is still in beta, and might not be available on the hosted instance - For self-hosted instanced, enable the rule feature in order to use
  this editor
- 🔤 Added catch-all switch for AnonAddy v0.4.0>
- ⌨ Added custom alias format option
- 💸 Added subscription check for random words alias format
- 💌 Show changelog on update
- 🛠️ Added version check (for self hosted instances)


### Fixed/Improved
- 🔎 Improved the search function
- 🌟 UI improvements