# peer-to-peer-whisper

## What is whisper ?

In this project we are going to build a peer-to-peer distributed chat application. The app provide broadcasting messages up to 9 others chatters on 9 nodes and private texting between two nodes.

## Constraints and assumptions

1. The system accepts at the most a group of 10 nodes for chatting
2. Point-to-point (unreliable & unordered) messages (UDP) are used between nodes
3. Broadcasted messages should be received in a logical order (define it)
4. Private messages should be received in order (up to you to define it)

We will go over two main steps in this project:

### 1. Group communication 
The first step aims to build a library of functions that would serve to exchange messages between nodes based on the identified sequence.

### 2. Fault tolerance
Apply fault tolerant scheme to the application in order to avoid potential issues.


## Architecture

*In this section, we explain the architecture of the system and the issues involved.*

### Network

There are different approaches on how to create the network and make nodes communicate with each others. We can create a central server where nodes ask to join the network. Or we can use a distributed Hash Tables (DHT) to store informations about nodes on the network. Then, nodes can query DHT to see who joined the network and new nodes can add there IP and port to the DHT.
In order to simply the process we will be using a central server that will connect nodes with each others. 
Notice: once peers are connected they don't need the server anymore to exchange messages.

First, there is a central server needed to achieve node discovery. This server will send all information about connected nodes on the network. Then the server may be shutdown, nodes on the network are still able to communicate with each others. The server is also responsible to maintain the maximum number on the network (in this case 10). The new node will broadcast all nodes on the network, him included to all nodes so they know the new node and can start communicate with him. This is the first step to develop for now.

### Node/Peer

Nodes are the main components of the network since they represent an instance of the application running on a machine. In a peer-to-peer (P2P) network all of the nodes are connected with each others and share informations. Unlike the traditional client-server model they can act as both client and server. Since a node needs to communicate with other nodes, it will need its own port. A node also needs a socket, which is used to send messages with the UDP protocol and also a multicast socket in order to broadcast messages. A peer must have a record of all nodes of the network so it will have a hashmap of nodes mapping username with their port.

### Clock

We think that the lamport clock system does not correspond to the expectations since it is not designed to provide strong consistency. Lamport clocks cannot distinguish between concurrent events that do not have a direct causal relationship. If two events occur concurrently at different nodes, their Lamport timestamps may be the same, even though they are not causally related. This lack of discrimination between concurrent events can lead to inconsistent ordering in our chat system.

Lamport timestamps give the happens-before relation. We can detect the order of events that happened in a system. But the limitation is that we can’t say if A happened before B (A->B) or A and B are concurrent (A || B). To detect concurrent events, we need vectors clock.

In order to maintain message ordering we will be using vector clocks and causal broadcast algorithm. When a node receives a message, it checks the vector clock attached to the message to determine if it has already received and processed the causal dependencies of the message. If not, the node waits until it has received and processed all causal dependencies before delivering the message to the application layer.

### Broadcast

Using causal broadcast with vector clocks ensures that messages are delivered in the correct order and that the application layer sees a consistent view of the system state. However, it does add some overhead to the system, as each message must include a vector clock and each node must maintain a copy of the vector clock for each message it receives. Hence, to begin with, we won't be creating any vector clocks, and will simply be creating a basic broadcast function without attaching much importance to the order of the messages.

### Communication

In order to communicate, nodes will use UDP protocol for both broadcasting and point-to-point/private messages.
