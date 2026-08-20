# Browser Session Policy

When a NovalPie source-page review requires an authenticated browser page, reuse the
user-provided, already logged-in Edge window/profile.

The currently open, user-owned Edge session is the authoritative browser session for this project.
Leave its login state, cookies, local storage, tabs, and profile untouched except for the
read-only page navigation the user explicitly asks to review.

- Do not create an incognito, clean-profile, or headless replacement session for that review.
- Do not export, print, persist, or copy browser cookies, tokens, or passwords.
- Do not force a re-login when the existing user session can be reused.
- If the active browser cannot be attached through the available automation channel, use the
  authenticated native-app session for app QA and defer browser-only authenticated review until
  the existing browser is available. Do not attempt a fresh browser login without user direction.
