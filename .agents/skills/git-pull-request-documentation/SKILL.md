---
name: git-pull-request-documentation
description: Analyze code changes and generate standardized, clear, and concise Pull Request descriptions for software projects.
---

# Pull Request Documentation

Analyze the current code changes and generate a standardized Pull Request description.

The primary goal is to make the Pull Request easy to understand and review by clearly explaining:

* What was changed
* Why it was changed
* How the solution works
* What business rules or technical behavior were affected
* What should be verified during review

The Pull Request description must be based on the actual changes in the codebase.

## Core Principle

Read the changes as a code reviewer would.

Do not simply list modified files, classes, methods, or commits.

Understand the purpose and behavior of the change before writing the Pull Request.

Transform implementation details into a concise explanation of the change.

For example, avoid:

> Added `SessionService`, modified `SessionController` and created `SessionRepository`.

Prefer:

> Added the voting session creation flow, including validation to prevent multiple sessions from being created for the same agenda.

The Pull Request should explain the **change**, not reproduce the Git diff.

## Analyze Before Writing

Inspect the available changes using Git.

Prioritize:

* `git diff`
* `git diff --cached`
* Changed files
* New files
* Deleted files
* Renamed files
* Relevant existing code surrounding the changes
* Tests added or modified
* Configuration changes
* Database migrations
* API contract changes

When necessary, inspect related classes and methods outside the diff to understand the behavior being changed.

Do not assume that the diff alone always provides enough context.

## Understand the Intent

Before writing the PR, determine:

1. What problem does this change solve?
2. What behavior was added, removed, or modified?
3. Is the change functional, technical, structural, or a combination?
4. Does it introduce or modify a business rule?
5. Does it change an API contract?
6. Does it change persistence or data?
7. Does it affect error handling?
8. Does it introduce new dependencies or infrastructure?
9. What should a reviewer pay attention to?

If the intent cannot be determined from the code and available context, do not invent it.

Describe the observable behavior instead.

## Business Rules

Pay special attention to business rules affected by the change.

If the code introduces or modifies a business rule, explicitly describe it.

Example:

> A voting session can no longer be created when another session already exists for the same agenda.

Do not describe implementation details as business rules.

## Technical Changes

Identify relevant technical changes such as:

* New endpoints
* Modified endpoints
* New services/use cases
* Persistence changes
* Database migrations
* New integrations
* Configuration changes
* Exception handling
* Validation
* Messaging
* Caching
* Security
* Tests

Only mention technical details that help the reviewer understand the change.

## Tests

Inspect tests related to the change.

Document:

* What scenarios are covered
* Important success cases
* Important failure cases
* Business rules being protected by tests

Do not claim that something is tested if there is no evidence in the code.

If tests were not added or modified, do not fabricate test coverage.

## API Changes

If the change affects the REST API, explicitly document:

* New endpoints
* Modified endpoints
* Removed endpoints
* Request changes
* Response changes
* HTTP status changes
* Validation changes
* Error response changes

If the API contract did not change, do not include unnecessary API information.

## Breaking Changes

Identify changes that may require consumers or other services to adapt.

Examples:

* Removed endpoint
* Changed request field
* Changed response structure
* Changed required field
* Changed enum value
* Changed authentication requirement
* Changed behavior that existing consumers may depend on

Do not label something as breaking unless the code provides evidence that existing consumers may be affected.

## Output Format

Generate the Pull Request using the following structure:

# [Short title describing the change]

## What

Describe what was changed in clear and concise terms.

Focus on the resulting behavior rather than listing files.

## Why

Explain the problem, requirement, or reason for the change.

If the reason cannot be determined from the available context, describe the observed motivation without inventing one.

## How

Briefly explain how the solution works.

Mention relevant business rules, technical decisions, or important implementation details.

Avoid explaining obvious code.

## Business Rules

Include this section only when the change introduces or modifies business rules.

List the relevant rules in clear language.

## Tests

Describe the tests added or modified and the scenarios they cover.

If relevant, mention important cases such as:

* Success
* Validation failure
* Business rule violation
* Not found
* Conflict
* Exception handling
* Boundary conditions

## API Changes

Include this section only when the API contract or behavior changed.

Document relevant endpoint and contract changes.

## Breaking Changes

Include this section only when applicable.

Clearly describe what consumers or other components need to adapt.

## Review Notes

Include only relevant information that can help the reviewer evaluate the change.

Examples:

* Important implementation decisions
* Areas requiring particular attention
* Dependencies
* Migration requirements
* Configuration requirements
* Known limitations

Do not use this section for generic statements.

## Writing Style

The PR must be:

* Clear
* Concise
* Objective
* Technical when necessary
* Business-oriented when relevant
* Easy to scan
* Focused on the change

Avoid:

* Marketing language
* Excessive detail
* Generic statements
* Repeating the diff
* Listing every changed file
* Unnecessary implementation details
* Claims that cannot be verified
* Phrases such as "This PR makes several improvements" without explaining what they are

Prefer short paragraphs and bullet points.

## Title

Generate a short title that describes the actual change.

The title should:

* Describe the main change
* Be specific
* Avoid implementation details when possible
* Avoid generic titles such as `Update code`, `Fix stuff`, or `Changes`

Examples:

Good:

> Prevent duplicate voting sessions

> Add voting session expiration

> Validate agenda creation

Bad:

> Update services

> Backend changes

> Fix issue

## Commit and Diff Analysis

Use Git history only as supporting context.

The current code changes are the primary source of truth.

Do not assume that the commit message accurately describes the final implementation.

If multiple commits exist, consolidate them into a single coherent description of the resulting change.

## Accuracy Rules

1. Never invent the reason for a change.
2. Never invent business requirements.
3. Never claim tests exist when they do not.
4. Never claim an API is backward compatible without evidence.
5. Never claim a change is breaking without evidence.
6. Never list files simply because they changed.
7. Never describe implementation details without explaining their relevance.
8. Do not include unrelated changes.
9. Do not modify source code.
10. Base the PR on the final state of the changes.
11. If something is unclear, state the uncertainty instead of guessing.
12. Keep the final PR concise enough to be useful during code review.

## Final Review

Before generating the final Pull Request:

1. Read the complete diff.
2. Identify the main purpose of the change.
3. Inspect related code when the diff lacks context.
4. Identify affected business rules.
5. Identify API contract changes.
6. Identify persistence or infrastructure changes.
7. Inspect relevant tests.
8. Identify potential breaking changes.
9. Remove information that is not relevant to the reviewer.
10. Ensure every statement is supported by the code or available context.
11. Ensure the title accurately represents the main change.
12. Ensure the PR explains the change without simply reproducing the diff.
