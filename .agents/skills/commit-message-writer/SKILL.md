---
name: commit-message-writer
description: Writes a Conventional Commits message based on the current staged diff
---

When asked to generate a commit message:

- Run `git diff --cached` to read the staged changes.
- Summarize them in the Conventional Commits format `type(scope): subject`.
- Body lines should explain *why*, not *what*.
- Keep the subject ≤ 72 chars.
- The only output should be the commit message with description
