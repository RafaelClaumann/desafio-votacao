---
name: java-business-rules-documentation
description: Analyze a Java Spring Boot REST API and generate clear business-rule documentation based on the actual implementation.
---

# Java Spring Boot Business Rules Documentation

Analyze the Java Spring Boot application and generate documentation that makes the implemented **business rules and application behavior** easy to understand.

The primary goal is to explain **what the system does and why**, based on the actual source code.

API endpoints, controllers, DTOs, repositories, and technical details are supporting information. The main focus must be the **business rules implemented by the application**.

## Core Principle

Read the code as a business analyst would.

Do not simply describe classes and methods.

Identify the rules that determine:

* What can happen
* What cannot happen
* Under which conditions an operation is allowed
* What happens when a condition is not satisfied
* Which state transitions are possible
* Which validations are business rules
* Which exceptions represent business constraints
* How different domain objects interact
* What conditions affect the result of an operation

Translate implementation details into clear business language.

## Analyze the Codebase

Inspect the relevant parts of the application, including:

* REST Controllers
* Application Services
* Use Cases
* Domain Services
* Entities
* Aggregates
* Value Objects
* Repositories
* DTOs
* Enums
* Custom Exceptions
* Exception Handlers
* Validation annotations
* Configuration when it affects business behavior
* Tests, when available

Follow the execution flow of important operations instead of analyzing classes in isolation.

For example:

```text
Controller
   ↓
Use Case / Service
   ↓
Domain logic
   ↓
Repository
```

If a controller delegates the operation to another component, inspect that component before documenting the behavior.

## Business Rule Identification

Look specifically for conditions such as:

```java
if (...)
    throw new ...
```

```java
if (...)
    return ...
```

```java
if (...)
    ...
```

Also identify rules implemented through:

* `switch`
* `enum` decisions
* validation annotations
* state checks
* date/time comparisons
* existence checks
* uniqueness checks
* authorization checks
* conditional persistence
* transaction boundaries
* entity methods
* domain methods
* exception handling

Do not assume that an `if` statement is automatically a business rule.

Determine whether it represents:

* business behavior
* technical validation
* infrastructure behavior
* defensive programming

Prioritize business behavior.

## Rules Must Be Explained in Business Terms

Avoid documentation like:

> `SessionService.execute()` checks whether `expiresAt.isAfter(now)`.

Prefer:

> A voting session can only receive votes while its expiration time has not been reached.

The implementation detail may be included afterward:

> This rule is enforced by comparing the session expiration time with the current time.

The business meaning comes first.

## Explicit Business Rules

For each identified rule, document:

### Rule: [Clear business description]

**Condition**

Describe when the rule applies.

**Behavior**

Describe what the system does when the condition is satisfied.

**Violation**

Describe what happens when the rule is violated.

**Implementation**

Optionally identify the relevant class, method, or component responsible for enforcing the rule.

Example:

### Rule: An expired voting session cannot receive votes

**Condition**

A vote can only be registered while the voting session is still within its configured validity period.

**Behavior**

When the session is active, the system accepts the vote.

**Violation**

When the session has expired, the system rejects the vote.

**Implementation**

Enforced by the voting use case before registering the vote.

## State and Lifecycle Rules

Pay special attention to state transitions.

Document:

* Possible states
* Initial state
* Conditions for entering each state
* Conditions for leaving each state
* Operations allowed in each state
* Operations forbidden in each state
* Terminal states

Represent lifecycle rules clearly.

Example:

```text
CREATED
   ↓
OPEN
   ↓
EXPIRED
```

Explain the business meaning of each transition.

Do not create states that do not exist in the implementation.

## Relationships Between Domain Objects

Identify business relationships between entities.

For example:

* A `Pauta` can have one `Sessao`.
* A `Sessao` belongs to a `Pauta`.
* A `Voto` belongs to a `Sessao`.

Document the relationship only when it is supported by the code.

Explain why the relationship matters to the business behavior.

## Business Invariants

Identify conditions that must always remain true.

Examples:

* An entity cannot be created without a required field.
* A voting session cannot accept votes after expiration.
* A session cannot be started twice.
* A vote cannot be registered for an invalid session.

For each invariant, explain:

1. What must always be true.
2. Where it is enforced.
3. What happens if it is violated.

## Validation vs Business Rules

Distinguish simple input validation from actual business rules.

For example:

```java
@NotBlank
private String title;
```

This is an input validation.

But:

```java
if (session.isExpired()) {
    throw new SessionExpiredException();
}
```

is a business rule.

Document both when relevant, but give significantly more attention to business rules.

## Exceptions

Do not document exceptions merely because they exist.

Determine what business condition causes each relevant exception.

Instead of:

> `SessionExpiredException` is thrown by `SessionService`.

Write:

> The system rejects voting when the voting session has expired.

Then identify:

> Implementation: `SessionService.execute()` throws `SessionExpiredException`.

## API Documentation

After documenting the business rules, document the API endpoints necessary to understand how those rules are triggered.

For each endpoint include:

### `METHOD /path`

**Purpose**

Explain the business operation performed by the endpoint.

**Input**

Describe the relevant request fields.

**Business Rules**

List the rules that affect this operation.

**Success**

Describe the resulting business state.

**Errors**

Describe business conditions that cause the operation to fail.

Do not make the endpoint documentation the primary focus.

## Main Business Flows

Identify the most important business flows in the application.

Describe them step by step.

Example:

### Start Voting Session

1. A user selects a voting agenda.
2. The system verifies that a session has not already been created for the agenda.
3. The system creates a voting session.
4. The session receives its start time.
5. The session receives its expiration time based on the configured duration.
6. The session becomes available for voting.

Only document steps that are actually implemented.

## Business Rules Summary

At the beginning or end of the document, provide a concise list of the identified business rules.

Example:

| # | Business Rule                                                |
| - | ------------------------------------------------------------ |
| 1 | A voting session can only be created for an existing agenda. |
| 2 | An agenda can have only one voting session.                  |
| 3 | Votes are accepted only while the session is active.         |
| 4 | An expired session cannot receive votes.                     |

Do not invent rules to make the list complete.

## Source Traceability

Every important business rule should be traceable to the implementation.

When useful, identify:

* Class
* Method
* Exception
* Entity
* Relevant condition

Example:

> **Implementation:** `VotingService.registerVote()`

This allows developers to quickly move from the business documentation back to the source code.

## Documentation Rules

1. Never invent business rules.
2. Never infer a rule solely from a class or variable name.
3. Follow the execution flow to understand the actual behavior.
4. Prefer business language over implementation language.
5. Explain conditions and consequences explicitly.
6. Distinguish validation from business rules.
7. Pay special attention to state transitions and invariants.
8. Explain why an operation is accepted or rejected.
9. Document exceptions according to the business condition that causes them.
10. Use tests as additional evidence of intended behavior when available.
11. If the implementation is ambiguous, state the ambiguity instead of guessing.
12. Do not propose architectural improvements unless explicitly requested.
13. Do not change the source code.
14. Keep the documentation synchronized with the current implementation.

## Output

Generate or update:

`DOCUMENTATION.md`

The document should prioritize this structure:

# Business Documentation

## System Overview

Brief description of what the system does.

## Business Concepts

Describe the main domain concepts and their relationships.

## Business Rules

Document all relevant business rules in clear business language.

## States and Lifecycle

Document relevant states and transitions.

## Business Invariants

Document conditions that must always remain true.

## Main Business Flows

Describe the main operations step by step.

## API Operations

Document how the REST API exposes the business operations.

## Error and Rejection Rules

Explain why business operations can be rejected.

## Business Rules Summary

Provide a concise table of the identified rules.

## Implementation Traceability

Map important rules to the classes and methods that enforce them.

## Final Verification

Before finishing the documentation:

1. Read the relevant service/use-case/domain code.
2. Trace each important operation from the controller to the business logic.
3. Identify every meaningful business condition.
4. Identify every relevant state transition.
5. Identify important invariants.
6. Check custom exceptions and their causes.
7. Check tests for additional evidence of intended behavior.
8. Remove assumptions that cannot be verified.
9. Ensure every documented rule reflects the current implementation.
10. Ensure the documentation explains **what the system does**, not merely **how the code is organized**.
