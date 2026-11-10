# Changelog

All notable changes to Sun & Grid are documented here.

## [1.0.0] - 2026-11-10

First public release.

### Added

- Four-tab Compose interface: **Today**, **Solar**, **Usage** and **Device**.
- Live inverter readings over the SolaX Pocket Wi-Fi local API.
- Solar flow card with an explicit panels-to-grid path, animated flow and
  import/export direction.
- Today dashboard with generation, lifetime total, sun arc and a comparison
  against the latest UPPCL day.
- UPPCL month-to-date grid usage with Sunday-first calendar weeks, a
  horizontally swipeable chart and per-day selection.
- Solar screen with panel voltage, current and power plus grid voltage,
  current and frequency.
- Usage screen with month total, daily average, peak day and a full-month
  calendar heatmap.
- Device screen with inverter gauges, a full register list, settings and
  reading history shortcuts.
- Local JSON history with 7/30-day views and background sampling every
  15 minutes.
- Light, dark and system themes.
- Deterministic demo mode enabled on first launch.
- UPPCL Smart Consumers login and daily-consumption integration.
- Minified release build with a 6 MB APK size guard, signed with a
  maintainer-held private key that is not stored in this repository.

### Notes

- The flow card is tailored to this installation's grid-tied topology, where
  all generated solar is exported to the grid.
- History and credentials stay on the device; backups are disabled.
