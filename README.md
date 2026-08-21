# cache-regression

A minimal reproduction for a Quarkus Cache regression where `@CacheResult` on a `Uni`-returning method incorrectly **caches failures** when `runSubscriptionOn` is used to offload blocking I/O to a worker thread.

- Passes on Quarkus **3.19.4**
- Fails on Quarkus **3.38.3**

## The bug

When a `@CacheResult`-annotated method returns a `Uni` that uses `runSubscriptionOn(...)`, a failed `Uni` leaves its backing `CompletableFuture` in the Caffeine cache. Subsequent calls retrieve the same failed future instead of retrying, violating the documented behaviour that failures must not be cached.

The test `getUserAttributes_notCached_whenErrors` demonstrates this: the second call should throw a fresh "Second call LDAP error" but instead re-throws "First call LDAP error" from the cached failed future.

## Probably root cause (AI-assisted)

`CaffeineCacheImpl.getAsync()` stores the `CompletableFuture` in the Caffeine map via `cache.asMap().computeIfAbsent()`. Caffeine registers a `whenComplete` cleanup callback to remove the entry on failure, but this fires asynchronously on the worker thread **after** `runSubscriptionOn` offloads execution. There is a race: the calling thread's `.await().indefinitely()` can unblock and make a second call before the cleanup callback has run, finding the still-present failed future.

Without `runSubscriptionOn` the future completes synchronously inside `computeIfAbsent`, so Caffeine's cleanup runs before the method returns — no race. However, removing `runSubscriptionOn` is not a viable production fix because the underlying LDAP call is blocking I/O that must not run on the Quarkus event loop.

The synchronous `get()` path in `CaffeineCacheImpl` handles failures correctly by explicitly calling `cache.asMap().remove(key, newCacheValue)` — `getAsync()` lacks the equivalent.

This is a regression of [#51928](https://github.com/quarkusio/quarkus/issues/51928), which was fixed in 3.27.4.

## Reproducing

```shell
./mvnw test
```

The test `MockLdapServiceTest#getUserAttributes_notCached_whenErrors` fails. Switch to Quarkus 3.19.4 (the commented-out version in `pom.xml`) to confirm it passes.
