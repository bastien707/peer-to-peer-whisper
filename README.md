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

In this section we will explain the system architecture the stakes of it.

### Nodes
Nodes are the main parts of the network since they represent an instance of the application running on a machine. In a peer-to-peer (P2P) network all of the nodes are connected with each others and share informations. Unlike the traditional client-server model they can act as both client and server.
