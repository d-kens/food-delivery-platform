# Simulator

A standalone app that generates realistic traffic so the platform can be tested under load.

- Registers and approves test restaurants and riders.
- Spawns riders that go online, move along routes around a city, send positions and accept or decline offers.
- Spawns customers that browse, add to cart and place orders at a configurable rate (for example, a simulated lunch rush).
- Calls the platform through the gateway like a real client.
- Used together with k6 for load tests and Grafana for watching the system.

All generated data is fake.
