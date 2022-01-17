#!/bin/bash
# Cluster setup using roachprod.
# For setting up roachprod, see:
# https://github.com/cockroachdb/cockroach/tree/master/pkg/cmd/roachprod
#
# To reset without destroying/recreating cluster:
# roachprod run $CLUSTER "sudo killall -9 cockroach"
# roachprod wipe $CLUSTER --preserve-certs

cloud="aws"
nodes="1-3"
client="4"

if [ "${cloud}" = "aws" ]; then
region=eu-central-1
roachprod create "$CLUSTER" --clouds=aws --aws-machine-type-ssd=c5d.4xlarge --geo --local-ssd --nodes=4 \
--aws-zones=\
eu-central-1a,\
eu-central-1b,\
eu-central-1c,\
eu-central-1a
fi

if [ "${cloud}" = "gce" ]; then
region=europe-west1
roachprod create "$CLUSTER" --clouds=gce --gce-machine-type=n1-standard-16 --geo --local-ssd --nodes=4 \
--gce-zones=\
europe-west1-b,\
europe-west1-b,\
europe-west1-b,\
europe-west1-b
fi

if [ "${cloud}" = "azure" ]; then
region=westeurope
roachprod create "$CLUSTER" --clouds=azure --azure-machine-type=Standard_DS4_v2 --geo --local-ssd --nodes=4 \
--azure-locations=westeurope
fi

echo "----------------"
echo "Stage Binaries"
echo "----------------"

roachprod stage $CLUSTER release v21.2.4

echo "----------------"
echo "Start Up Services"
echo "----------------"

roachprod start $CLUSTER:$nodes --sequential
roachprod admin --open --ips $CLUSTER:1

echo "---------------------"
echo "Installing haproxy..."
echo "---------------------"

roachprod run ${CLUSTER}:$client 'sudo apt-get -qq update'
roachprod run ${CLUSTER}:$client 'sudo apt-get -qq install -y openjdk-11-jre-headless htop dstat haproxy'
roachprod run ${CLUSTER}:$client "./cockroach gen haproxy --insecure --host $(roachprod ip $CLUSTER:1 --external) --locality=region=$region"
roachprod run ${CLUSTER}:$client 'nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &'
roachprod put ${CLUSTER}:$client target/roach-workload.jar

echo "---------------------"
echo "Creating database..."
echo "---------------------"

roachprod run ${CLUSTER}:1 './cockroach sql --insecure --host=`roachprod ip $CLUSTER:1` -e "CREATE DATABASE workload"'

echo "Public admin URLs of nodes:"
roachprod admin --ips $CLUSTER:$nodes

echo "Public admin URL of LB:"
roachprod admin --ips $CLUSTER:$client

echo "Connect to the client node using:"
echo "roachprod run $CLUSTER:$client"

echo "Cluster setup complete!"

exit 0
