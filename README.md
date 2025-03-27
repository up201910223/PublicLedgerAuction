# PublicLedgerAuction

## Basic Behaviour

- Nodes are created
- All of them will act as server/client, meaning they will handle the DHT tasks and the auction related tasks
- Initial transaction needs to exist so we can create the genesis block, this genesis block can have whatever information, doesnt matter if it's an actual transaction or just random text
- From now on the following behaviour should repeat
- - When an auction finishes, the transaction is added to a "queue" of the respective Node (Miner part will handle it)
- - When this "queue" of transactions finishes, a new block is generated
- - This new block will then need to be disseminated for all other nodes of the network

- There are probably steps missing or slight errors on this but at least we have a general idea that we can polish. 


### IMPORTANT
- Discuss between us what type of data structure we want to use and figure out how our parts of the work will fit together.