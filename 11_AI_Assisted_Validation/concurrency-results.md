# Concurrency Simulation Results

```text
Initial inventory: 100
Concurrent requests: 10000
Successful reservations: 100
Failed reservations: 9895
Final available inventory: 0
Oversold: 0
```

The atomic conditional update in the repository successfully prevented any overselling under heavy threading contention.
