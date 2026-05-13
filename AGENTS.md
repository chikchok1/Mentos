# AGENTS.md

## Scope

- This file defines the working instructions Codex must follow in this repository.
- Do not modify files outside this repository.
- npm-related rules apply only to Node.js/npm-based projects.
- For other tech stacks, follow the package manager and build tool conventions of that ecosystem.
- The user's latest explicit instruction takes priority over this file.
- However, for potentially dangerous commands, explain the purpose and risk first and ask for confirmation before running them.

## General Project Rules

- Always run `git status` before making changes.
- Summarize changed files and the reason for each change after completing a task.
- Do not modify files that are unrelated to the user's request.
- Stay within the scope requested by the user.
- Before running security-sensitive commands, explain what they do and what they may affect.
- If existing user changes are present, do not overwrite them. Report them first.

## npm and Package Security Rules

- Never run unknown or untrusted `npx` commands.
  - Forbidden examples: `npx supply-chain-attack`, `npx <unknown-package>`.
- Do not install new npm packages unless the user explicitly asks for it.
- Ask for user approval before running:
  - `npm install <package>`
  - `npm update`
  - `npm audit fix`
  - `npm audit fix --force`
- Do not run `npm audit fix --force` by default because it can introduce breaking changes.
- Install `@latest` versions only when the user explicitly asks for it.
- When adding or updating packages, summarize changes to `package.json` and `package-lock.json`.
- Before installing a new package, check whether it has suspicious lifecycle scripts such as:
  - `preinstall`
  - `install`
  - `postinstall`
  - `prepare`
- When reviewing dependencies, prefer `npm install --ignore-scripts` or `npm ci --ignore-scripts` when appropriate.
- After dependency changes, verify with the following when applicable:
  1. `npm audit`
  2. `npm audit --omit=dev`
  3. The project build command
     - For npm projects: `npm run build`
     - Also run tests if a test command exists
  4. `git diff package.json package-lock.json`

## Dangerous Command Restrictions

Do not run the following commands without explicit user approval:

- `npx <unknown-package>`
- `npm audit fix --force`
- `npm install <package>`
- `npm update`
- `npm publish`
- `npm login`
- `curl ... | bash`
- `wget ... | bash`
- `Invoke-WebRequest ... | Invoke-Expression`
- `iwr ... | iex`
- `irm ... | iex`
- `powershell -ExecutionPolicy Bypass ...`
- Remote PowerShell script execution
- Editing or deleting files outside the repository
- Reading or printing `.env`, tokens, credentials, private keys, or secret files
- `rm -rf`
- `git reset --hard`
- `git clean -fd`
- `git push --force`
- `docker system prune`
- System configuration changes

## Secrets and Privacy

- Do not read, print, or expose `.env` files, API keys, tokens, certificates, or private keys.
- If logs contain tokens, API keys, session values, or secrets, redact them as `[REDACTED]`.
- Do not modify or print GitHub tokens, npm tokens, CI/CD secrets, or cloud credentials.
- If dependency installation adds lifecycle scripts such as `preinstall`, `install`, `postinstall`, or `prepare`, report it to the user.
- Handle personal data, payment data, authentication data, and location data minimally.
- Do not commit files containing sensitive information.

## Codex Permission Rules

- Keep Codex permissions in Default mode by default.
- Do not use Full Access mode.
- If network access, npm registry access, or access outside the workspace is requested, ask the user to approve it.
- Do not use any “don't ask again” option.
- If a command or package source is unclear, do not run it. Suggest a safer alternative.

## Vulnerability Handling Rules

- Do not immediately run `npm audit fix --force` after seeing an `npm audit` report.
- First consider safe fixes only, such as non-force `npm audit fix`.
- If the build breaks after a dependency change, revert the change.
- If remaining vulnerabilities come from framework transitive dependencies, test updates only on a separate branch.
- For deployed apps, also check `npm audit --omit=dev`.
- If `npm audit fix --force` appears necessary, create a separate test branch first.
- Only consider applying forced updates or framework upgrades after:
  - build passes
  - tests pass when available
  - the app is manually verified
- If a supply-chain attack is suspected, check `git status`, `git diff`, `npm audit`, and `package-lock.json` before installing new packages or running remote scripts.

## Build and Test Rules

- Run the project build command after code changes when a build command exists.
- Run tests after changes when a test command exists.
- If build or tests fail, summarize the failure honestly.
- Do not hide failed checks.
- Do not perform large refactors unless the user asks for them.

## Git Rules

- Before committing, check `git status` and `git diff`.
- Do not use `git add .`.
- Stage only the files that are relevant to the task.
- Separate dependency/security changes from feature/UI changes.
- Prefer Conventional Commit messages.
- Examples:
  - `fix: address npm audit vulnerabilities`
  - `fix: improve landmark labels and Klook link handling`
  - `chore: add Codex security rules`
  - `feat: add expense experience system`
  - `refactor: split notification parsing logic`

## Security Limitations

- This file is an instruction document for Codex, not an antivirus tool or malware detector.
- Real protection requires Git diff review, Codex Default permissions, sandboxing, and explicit user approvals.
- If a suspicious command is required, do not run it. Explain the risk and suggest a safer alternative.
- Supply-chain attacks cannot be fully prevented by this file, so dependency changes and remote code execution must be minimized.

## Response Rules

- Respond in Korean when the user writes in Korean.
- Keep summaries clear and practical.
- Separate changed files, reasons, commands run, and build/test results.
- Be honest about failed or skipped work.