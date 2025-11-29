Kotlin was chosen "semi randomly" based on discussions at the office.

Spring was chosen over Ktor/similar due to it possibly being more similar to Laravel (easing the transition for Kristian)
The expected cost of simply migrating to a different framework is fairly low.
Business logic should be kept separate from the HTTP layer, and simply changing frameworks should be a minimal effort should the choices be wrong.

All the GraphQL libraries seems to be built on the same underlying framework. (graphql-java)
As such, one was more or less picked at random.

There may be some additional overhead involved if we wish to move to an async
