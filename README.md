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

### Representation

<img width="1129" alt="whisper_archictecture" src="https://github.com/bastien707/peer-to-peer-whisper/assets/73294817/43e3928c-e34f-41d4-9822-e8b1062c9dde">

### Network

There are different approaches on how to create the network and make nodes communicate with each others. We can create a central server where nodes ask to join the network. Or we can use a distributed Hash Tables (DHT) to store informations about nodes on the network. Then, nodes can query DHT to see who joined the network and new nodes can add there IP and port to the DHT.
In order to simply the process we will be using a central server that will connect nodes with each others. 
Notice: once peers are connected they don't need the server anymore to exchange messages.

First, there is a central server needed to achieve node discovery. This server will send all information about connected nodes on the network. Then the server may shutdown, nodes on the network are still able to communicate with each others. The server is also responsible to maintain the maximum number on the network (in this case 10). To be part of the network, the new node send a message of type "CONNECTION_REQUEST" to the server. The server check the number and the name of the future node. If everything is OK, he updates and sends its list of node to the new node in a message type "CONNECTION_ACCEPTED". Thus, the new node will broadcast the new list of peers connected on the network, him included, so they know the new node and can start communicate with him.

If a peer wants to leave the network he just needs to broadcast a message of type "DISCONNECT" over the network. Peers will update their own list by removing the node. The leaving peer must also send a private message to the server so he does the same. As the server is not part of the network, for a node to interact with it, it must send a point-to-point message.

### Node/Peer

Nodes are the main components of the network since they represent an instance of the application running on a machine. In a peer-to-peer (P2P) network all of the nodes are connected with each others and share informations. Unlike the traditional client-server model they can act as both client and server. Since a node needs to communicate with other nodes, it will need its own port. A node also needs a socket, which is used to send messages with the UDP protocol and also a multicast socket in order to listen to the broadcasted messages. A peer must have a record of all nodes of the network so it will have a hashmap of nodes mapping username with their port. Finally a node has a vector clock which is used for broadcast messaging.

### Messaging

The network handles different types of messages. Each message follow this standard :
 `[TYPE_OF_MESSAGE:SENDER:CONTENT:VECTOR_CLOCK]`

- TYPE_OF_MESSAGE allows the receiver to sort which message it is and then act accordingly
- SENDER gives information about the username
- CONTENT is the string to display on the chat; can be null depending on the type of message
- VECTOR_CLOCK is the state of the clock at the time the sender sent the message; can be null

### Private Messages

In private messages we can use the FIFO algorithm because it’s a point to point messaging so the receiver cannot
receive other messages between those sent by the sender. For point to point messaging, we just have to care about
the order of the
messages sent by one node, so a FIFO algorithm is enough. This algorithm ensures the order of the messages and the
comprehension for both nodes.


### Broadcast

We have the choice between 3 broadcast algorithms for the p2p chat system: FIFO, causal, Total. The FIFO algorithm has some drawbacks for a peer to peer messaging system. Since it follows only this sentence: If two messages are broadcasted by the same node then, all nodes will deliver these messages in the same order as they were broadcasted. This means if node A sends two messages m1 and m3, on each node m1 must arrive before m3 which is good. But the thing is that this algorithm doesn’t take into account that some messages may arrive between m1 and m3. The possibilities of valid message FIFO broadcast are (m2, m1, m3) OR (m1, m2, m3) OR (m1, m3, m2). So FIFO algorithms could lead to inconsistent orderings.

To overcome this issue, we may use the causal broadcast algorithm. In this algorithm, causally related messages must be delivered in causal order, where concurrent messages can be delivered in any order.
Causal order means the broadcasting of one message happens before broadcasting of another, those messages must be delivered in that order.
Concurrent means two events don't know each other when they occurred.

Finally, total order broadcast could also be a solution for broadcasting messages in a peer-to-peer chat system. Unlike FIFO and causal algorithms, total order broadcast guarantees that all nodes deliver messages in the same global order, regardless of the causal relationships between messages or the order of their broadcast. This means that even if messages are sent by different nodes and have no causal relationship, they will be delivered in a consistent order across all nodes.
