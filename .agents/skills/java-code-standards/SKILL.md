---
name: java-code-standards
description: Apply simple and consistent Java and Spring Boot coding standards.
---

# Java Code Standards

Follow these basic coding standards when writing or modifying Java and Spring Boot code.

## Formatting

* Follow standard IntelliJ IDEA Java formatting.
* Follow the current code formating.
* Use LF line endings.
* Every file must end with a newline.
* Do not leave trailing whitespace.
* Keep spacing, braces, and blank lines consistent with IntelliJ IDEA formatting.

## Naming

* Classes and interfaces: `PascalCase`
* Methods and variables: `camelCase`
* Constants: `UPPER_SNAKE_CASE`
* Packages: lowercase
* Enums: `PascalCase`
* Enum values: `UPPER_SNAKE_CASE`

Avoid unnecessary abbreviations.

## Java

* Use `==` only for primitives and identity checks.
* Never use `==` to compare `String`; use `.equals()`.
* Prefer interfaces for collection declarations (`List`, `Set`, `Map`).
* Use enums instead of string literals when values represent a fixed set of options.
* Avoid magic numbers and magic strings when they represent meaningful concepts.
* Do not use `Optional` for fields or method parameters.
* Use `Optional` for return values when absence is meaningful.
* Do not use `var`.
* Prefer `final` when it improves clarity.

## Methods and Classes

* Keep methods focused and reasonably small.
* Use descriptive names.
* Avoid unnecessary nesting.
* Avoid unnecessary abstractions.
* Do not refactor code only for stylistic reasons.

## Spring Boot

* Use constructor injection.
* Avoid field injection with `@Autowired`.
* Keep controllers focused on HTTP concerns.
* Keep business rules outside controllers.
* Keep persistence logic inside repositories or persistence components.
* Keep DTOs separate from entities when the project uses this pattern.

## Imports

* Do not use wildcard imports.
* Remove unused imports.
* Remove duplicate imports.
* Keep imports organized consistently.

## Comments

* Prefer clear code over comments.
* Do not add comments for obvious code.
* Use comments only for non-obvious business or technical decisions.
* Do not leave commented-out code, except when asked for.

## Rules

* Do not change business behavior.
* Do not introduce unnecessary dependencies.
* Do not perform unrelated refactoring.
* Keep the implementation simple and consistent with the existing code.

## Principles

Follow these principles when writing or modifying code:

* **KISS (Keep It Simple, Stupid):** prefer simple solutions over unnecessary complexity.
* **DRY (Don't Repeat Yourself):** avoid duplicating business logic or behavior.
* **YAGNI (You Aren't Gonna Need It):** do not implement abstractions or functionality without a current requirement.
* **SOLID:** keep responsibilities clear and dependencies well-structured, especially the Single Responsibility Principle.
* **Separation of Concerns:** keep different responsibilities separated, especially HTTP, business logic, and persistence.
* **Composition over Inheritance:** prefer composition when it provides a simpler and clearer design.
* **Fail Fast:** detect invalid states and inputs as early as possible.
* **Least Surprise:** code should behave in a predictable and intuitive way.
* **Immutability:** prefer immutable state when practical and when it improves safety and readability.

## Verification

Before finishing:

1. Check formatting.
2. Check naming conventions.
3. Check imports.
4. Check trailing whitespace.
5. Check newline at the end of the file.
6. Check basic Java and Spring conventions.
7. Ensure business behavior was not changed unintentionally.
