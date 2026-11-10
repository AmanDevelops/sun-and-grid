# Security policy

## Reporting a vulnerability

Please report security issues privately through GitHub's **Security advisories**
for this repository instead of opening a public issue. Include the affected
version, reproduction steps and any relevant logs.

## Scope and design notes

- Sun & Grid is a local-first app. Inverter readings are requested directly
  from the SolaX Pocket Wi-Fi dongle over the local network.
- The dongle speaks plain HTTP only. Cleartext traffic is intentionally allowed
  so the app can reach it. Never expose the dongle to the internet or forward
  its port through your router.
- The local API password is the dongle serial number, as required by the
  protocol. It is stored in app-private DataStore storage.
- UPPCL credentials are stored in app-private DataStore storage and sent only
  to `uppcl.eeslsmartmeter.in`. Android backups are disabled.
- Release signing keys are **never stored in this repository**. The maintainer
  signs release APKs locally with a private key kept outside the project; the CI
  workflow builds only debug and unsigned release artifacts.

## Supported versions

Security fixes are applied to the latest release on the `main` branch.
