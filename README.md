# Roach Workload 

Simple command-line tool for running different workloads against CockroachDB.

Available workloads:

- **bank** - financial ledger using double-entry (read-write, explicit transactions)
- **events** - inserts events simulating a transactional outbox (write-only, implicit, multi-table)                      
- **orders** - inserts and reads orders (write-only, optional reads, implicit, multi-table)
- **query** - runs adhoc SQL queries
                                                                                           
Each workload have two main commands, `init` to setup schemas and initial data and `run`.

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

Type `help` for additional guidance.

---

# Appendix: Configuration

All parameters in `application.yaml` can be overridden via CLI. See 
[Common Application Properties](http://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html)
for details.

# Appendix: Cluster Deployment

The `cluster-setup.sh` script sets up a CockroachDB cluster in AWS or GCE using roachprod, 
an internal testing tool by Cockroach Labs. 

For setting up and using the roachprod tool, see: https://github.com/cockroachdb/cockroach/tree/master/pkg/cmd/roachprod.

Edit and run:

    ./cluster-setup.sh

## (Optional) Add admin UI to HAProxy config
                            
Where `N` is the client node number:

    roachprod run $CLUSTER:N

Run:

    killall haproxy

Edit `haproxy.cfg` and replace the IPs below, then add:

    listen crdb-admin
        bind :8080
        mode tcp
        balance roundrobin
        option httpchk GET /health
        server cockroach1 10.6.10.54:26258 check port 26258
        server cockroach2 10.6.19.177:26258 check port 26258
        server cockroach3 10.6.39.87:26258 check port 26258
        server cockroach4 10.6.3.130:26258 check port 26258
        server cockroach5 10.6.19.235:26258 check port 26258
        server cockroach6 10.6.46.228:26258 check port 26258
    
    listen stats # Define a listen section called "stats"
        bind :8090
        mode http
        stats enable  
        stats hide-version  
        stats realm Haproxy\ Statistics  
        stats uri /
        #stats auth admin:admin

Restart `haproxy`:

    nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &

Open the Admin UI:

    roachprod admin --open --ips $CLUSTER:N

