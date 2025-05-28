# PublicLedgerAuction

## How to run

### We will be including the already built '.jar' file, you can find this one in the folder 'build/libs'. However if you still need to build the project yourself you can use the following command:

`./gradlew clean shadowJar`

### ===================================

### Next you must initialize the bootstrap node using:


`java -jar PublicLedgerAuction-1.0-all.jar 5000`

or

`java -cp PublicLedgerAuction-1.0-all.jar main.java.Main.NodeClient 5000`

In this example we use port '5000', however you can use whichever port is available in your computer.

### ===================================

### After the boostrap node, all subsequent nodes need to be created as such:

`java -jar PublicLedgerAuction-1.0-all.jar 5X00 127.0.0.1:5000`

or

`java -cp PublicLedgerAuction-1.0-all.jar main.java.Main.NodeClient 5X00 127.0.0.1:5000`

Where 'X' is the number of the node (1,2,3,4,...)

### ===================================

### Once all nodes are created you can interact with the program using the options you see on the main menu.