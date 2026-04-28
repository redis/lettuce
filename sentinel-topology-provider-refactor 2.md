# Refactor `SentinelTopologyProvider`: drop Reactor `Mono`/`Tuple2` for `CompletableFuture`/`Pair`

## What changed

- `connection.reactive()` → `connection.async()` (`RedisSentinelAsyncCommands`).
- `reactive.master(id).zipWith(replicas(id).collectList(), Pair::of)`
  → `async.master(id).toCompletableFuture().thenCombine(async.replicas(id).toCompletableFuture(), Pair::of)`
  (both commands still dispatched concurrently).
- `Mono.timeout(Duration)` → small `withTimeout(CompletableFuture, Duration)` helper
  using `redisClient.getResources().timer()` (same pattern as
  `cluster.topology.Connections`); still emits `java.util.concurrent.TimeoutException`.
- Cleanup collapsed to a single `whenComplete((p, e) -> connection.closeAsync())`.
- `ResumeAfter` no longer referenced here (still used by other callers, so kept).
- No public API changes; `getNodes()` and `getNodesAsync()` signatures are unchanged.

## Deliberate behavioral changes (cleanup path only)

1. **Close is fire-and-forget on success.** Previously `ResumeAfter.thenEmit` awaited
   `closeAsync()` before emitting; the returned future now may complete before close
   resolves.
2. **A failing `closeAsync()` no longer fails the lookup.** Previously, a close error
   on the success path discarded the result; it is now silently dropped, as close is
   cleanup and shouldn't mask a successful topology result.
3. **Close is invoked exactly once.** The previous implementation could invoke it
   twice in a rare edge case (relied on idempotency).

Everything else (parallel fetch, timeout scope, timeout exception type, error-path
close, result shape) is semantically equivalent.

## Testing

Class compiles cleanly. Suggested follow-up: run `SentinelTopologyProviderUnitTests`
and sentinel-based integration tests that exercise topology discovery.
