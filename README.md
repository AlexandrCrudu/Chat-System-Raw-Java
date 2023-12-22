# University Project - Chat System
## Chat application developed in Java, featuring a client-server model that leverages Java sockets for network communication. 
### It is a fully functional chat, employing the following features: 
- Private Messaging: Allows users to send private messages to specific chat members.
- Public Messaging: Enables sending messages that are visible to all chat members.
- Survey Creation and Participation: Facilitates creating surveys where selected members can participate and view the results.
- File Transfers: Supports transferring files directly to another chat participant.
- Encrypted Messaging: Offers the capability to send encrypted messages to ensure secure communication between chat participants.

### Key techniques and technologies: 
- Message Encryption: Utilizes RSA for key exchange and AES for encrypting the actual messages, ensuring secure communication.
- Server Implementation: Employs Java sockets for network communication, handling multiple client connections and facilitating real-time data exchange.
- Client Implementation: Also uses Java sockets to connect with the server, enabling interactive communication and data transfer.
- Employes Automated Testing that uses unit tests for each chat feature.
- Custom Protocol Implementation: Me and my teammate came up with a custom protocol (see chat-protocol.pdf) for all the different message exchanges between the server and the client.

P.S This was not individual work, I worked together with a good friend of mine towards completing this university course that required teams of 2 people. 
