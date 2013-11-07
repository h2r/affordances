affordances
===========
A collection of files currently beind used in our Affordances project.

Three main sets of files here:
	(1) Minecraft domain in BURLAP.
		- OOMDP model of Minecraft + Minecraft Bot
		- Currently runs VI (effectively forward search with k = 100) on basic path planning example
		- Primarily *.java
	(2) code that interfaces with the Mineflayer API
		- Additional layer of functionality so that BURLAP can 'easily' interface with Mineflayer
		- Most code is in move.js
	(3) Miscellaneous other algorithms/papers/bits of the project
		- Just about everything else

Note: None of these are intended to run in this directory as is.

In the near future, we will refactor the code base and include the entire Mineflayer API, BURLAP, Craftbukkit, and all other dependencies so that our code can run directly from the repo.

Contact Dave Abel for any questions @ David_Abel@brown.edu