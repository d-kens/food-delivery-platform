# Kafka

Single-node Kafka (KRaft) from the root `docker-compose.yml`.

- From your Mac (`bootRun`): `localhost:9092`
- From other containers: `kafka:29092`

Run everything from the repo root.

## 1. Configuration

| Setting | Meaning |
|---|---|
| `image: apache/kafka:4.1.0` | Official Kafka image, pinned version |
| `restart: unless-stopped` | Restarts after a crash or Docker restart, unless you stopped it |
| `ports: 127.0.0.1:9092:9092` | Mac port 9092 → container port 9092, reachable only from your machine |
| `KAFKA_NODE_ID: 1` | This server's unique ID |
| `KAFKA_PROCESS_ROLES: broker,controller` | One server does both jobs: stores data (broker) and manages the cluster (controller) |
| `KAFKA_LISTENERS` | Ports Kafka opens: `INTERNAL` 29092 (containers), `HOST` 9092 (your Mac), `CONTROLLER` 9093 (Kafka itself). The names are labels |
| `KAFKA_ADVERTISED_LISTENERS` | Address Kafka tells clients to reconnect to: `kafka:29092` for containers, `localhost:9092` for your Mac |
| `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP` | Protocol per listener; `PLAINTEXT` = no encryption or login |
| `KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL` | Listener brokers use to talk to each other |
| `KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER` | Listener used for controller traffic |
| `KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093` | Controllers that vote on cluster decisions (`nodeId@host:port`); just this one |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1` | 1 copy of `__consumer_offsets` (group bookmarks); default 3 needs 3 brokers |
| `KAFKA_TRANSACTION_STATE_LOG_*: 1` | 1 copy of `__transaction_state`, and 1 in-sync replica is enough |
| `volumes: kafkadata` | Data survives restarts and `docker compose down` |
| `healthcheck` | Every 10s asks the broker for its API versions; healthy when it answers |

**Why two listeners:** a client first connects to a bootstrap address, then Kafka tells it which address to use. That address must work from where the client runs: `localhost` from your Mac, `kafka` inside Docker.

## 2. Start

```bash
docker compose up -d kafka       # start in the background
docker compose ps kafka          # wait for (healthy)
```

Shortcut for the commands below. It exists only in the current terminal, so paste it once per terminal:

```bash
kbin() { docker compose exec kafka /opt/kafka/bin/"$@" --bootstrap-server localhost:9092; }
```

`kbin kafka-topics.sh --list` runs `kafka-topics.sh --list --bootstrap-server localhost:9092` inside the container.

## 3. Topics

```bash
kbin kafka-topics.sh --list                                       # all topics, incl. internal ones
kbin kafka-topics.sh --create --topic user-events --partitions 3 --replication-factor 1
kbin kafka-topics.sh --describe --topic user-events               # partitions, leader, replicas, ISR
kbin kafka-topics.sh --alter --topic user-events --partitions 4   # can only increase; remaps keys
kbin kafka-topics.sh --delete --topic user-events                 # deletes all its data
```

## 4. Produce

To watch messages arrive, start a consumer (section 5) in another terminal first.

Type `key:value` lines, Ctrl+C to stop. `42:hello` sends key `42` and value `hello`. Uses `-it` (not `kbin`) so you can type.

```bash
docker compose exec -it kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic user-events --property parse.key=true --property key.separator=:
```

## 5. Consume

```bash
# as group "demo": commits offsets and resumes where it stopped; a new group starts at the latest messages
kbin kafka-console-consumer.sh --topic user-events --group demo \
  --property print.key=true --property print.partition=true --property print.offset=true

# everything from offset 0, throwaway group (nothing remembered)
kbin kafka-console-consumer.sh --topic user-events --from-beginning --property print.key=true

# only partition 0
kbin kafka-console-consumer.sh --topic user-events --partition 0 --from-beginning --property print.key=true
```

## 6. Consumer groups

```bash
kbin kafka-consumer-groups.sh --list
kbin kafka-consumer-groups.sh --describe --group demo       # CURRENT-OFFSET, LOG-END-OFFSET, LAG per partition

# replay: move the bookmark to the start (stop the group's consumers first; without --execute it only previews)
kbin kafka-consumer-groups.sh --group demo --topic user-events --reset-offsets --to-earliest --execute
kbin kafka-consumer-groups.sh --delete --group demo         # removes the group and its offsets
```

## 7. Experiments

| # | Try | Shows |
|---|---|---|
| 1 | Send several messages with the same key | Same key → same partition → in order |
| 2 | Run two consumers with `--group demo` | One group splits the partitions |
| 3 | Run a consumer in another group with `--from-beginning` | Each group gets every message |
| 4 | Stop the group, send messages, `--describe`, start it again | Lag, then catching up |
| 5 | Reset the group to earliest | Replay |

## 8. Reset everything

Stops and removes the container, then deletes all topics, messages and offsets:

```bash
docker compose down kafka && docker volume rm food-delivery-platform_kafkadata
```
