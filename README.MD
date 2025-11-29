### Running the application

* Running `main` in `Application` runs the application locally.
* `./gradlew clean` deletes any existing artifacts
* `./gradlew build` builds a standalone jar (as well?)
* `./gradlew bootJar` builds a standalone jar



TODO:
* Login rate limiting
* Test with db
* Run & fix tests


### Configuration

See application.properties

### Dependency management

Dependencies are managed in `build.gradle.kts` (Grade-kotlin dependency manager)
Although gradle is an alternative to maven, the most commonly used package manager for java, it still uses
the [maven repository](https://mvnrepository.com/) for packages.
When upgrading dependencies, it might be beneficial to run `./gradlew dependencies --configuration compileClasspath` to
detect any conflicts with transient dependencies.

### IntelliJ

Recommended settings:

* Auto import: Add unambiguous imports on the fly
* Auto import: Optimize imports on the fly
* On save: Optimize imports
* On save: Reformat code

### Testcontainers

The tests using a real MySQL database, see https://testcontainers.com/desktop/ for improving performance for local dev

### GraphQL

Reading material:

* [Custom @Annotations](https://netflix.github.io/dgs/advanced/custom-directives/) (validation and transformations)
* [Codegen](https://netflix.github.io/dgs/advanced/kotlin-codegen/) (useful or not?)
* [Validation @Constraints](https://github.com/graphql-java/graphql-java-extended-validation) such as @Max

### Linting

[Ktlint](https://github.com/pinterest/ktlint) is used for linting.
If we, for some reason, want to deviate from the standard ruleset, we use [.editorconfig](.editorconfig).

A format check is automatically run on build, failing the build on linting errors.

* `./gradlew ktlintFormat` formats the code according to the linter rules.
