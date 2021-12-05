# Roach Workload 

Simple command-line tool for running different workloads against CockroachDB.

Available workloads:

- **bank** - financial ledger using double-entry principle (read-write, explicit transactions)
- **events** - inserts events simulating a transactional outbox (write-only, implicit, multi-table)                      
- **orders** - inserts and reads orders (read-write, implicit, multi-table)
- **query** - runs adhoc SQL queries
                                                                                           
Each workload have two main commands, `init` to setup schema and load initial data 
and `run`.

## Project Setup

### Prerequisites

- JDK8+ with 1.8 language level (OpenJDK compatible)
- Maven 3+ (optional, embedded)

Install the JDK (Linux):

    sudo apt-get -qq install -y openjdk-8-jdk

### Clone the project

    git clone git@github.com:kai-niemi/roach-workload.git
    cd roach-workload

### Build the executable jar 

    chmod +x mvnw
    ./mvnw clean install

## Usage

Create the target database:

    cockroach sql --url postgresql://localhost:26257?sslmode=disable -e "CREATE database workload"

Start the shell with:

    java -jar target/roach-workload.jar --help
    
or just:
    
    ./target/roach-workload.jar --help

Type `help` for additional CLI guidance.

---

# Appendix: Configuration

All parameters in `application.yaml` can be overridden via CLI. See 
[Common Application Properties](http://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html)
for details.

# Appendix: Cluster Deployment

The `cluster-setup-(secure|insecure).sh` scripts sets up a CockroachDB cluster in AWS 
or GCE using roachprod, an internal testing tool by Cockroach Labs. For setting up and 
using the roachprod tool, see: https://github.com/cockroachdb/cockroach/tree/master/pkg/cmd/roachprod.

Open the Admin UI:

    roachprod admin --open --ips $CLUSTER:N

SSH to one of the nodes:

    roachprod run $CLUSTER:N

