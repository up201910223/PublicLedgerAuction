# PublicLedgerAuction

## Behaviour

- Nodes are created
- All of them will act as server/client, meaning they will handle the DHT tasks and the auction related tasks
- Initial transaction needs to exist so we can create the genesis block
- Seller initiates first auction
- Buyers bid on the auction until the time ends
- The auction ends and information about it is stored (final buyer, price, item, whatever info is needed)
- We use this information to fill the genesis block
- From now one the following behaviour should repeat
- - New finalized auctions are now stored in the respective block (each node has its own block (?confirm with teacher))
- - This new information must then be present in all the Miner nodes (how do we do that?)

- There are probably steps missing or slight errors on this but at least we have a general idea that we can polish. 


### IMPORTANT
- Discuss between us what type of data structure we want to use and figure out how our parts of the work will fit together.