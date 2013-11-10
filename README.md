affordances
===========
A collection of files currently being used in our Affordances project.

Four main sets of files here:

	(1) burlap_files
		- MinecraftDomain.java := OOMDP model of Minecraft
		- MinecraftBehavior.java := runs VI on MinecraftDomain
		- MinecraftStateParser.java := just used to visualize states when run with BURLAP (for testing)
		- MinecraftVisualizer.java := 2D visualizer for Minecraft (for testing)

	(2) minecraft_api 
		- Contains all the necessary files to setup a Minecraft server (in /craftbukkit) and client (minecraft-1.6.4.jar)
		- Contains the interface layer for BURLAP and Mineflayer, the Minecraft API - contained in /mineflayer/affordances

	(3) misc_code
		- Some miscellaneous algorithms we have implemented along the way
	
	(4) paper/presentation/proposal
		- Collection of supplementary material for the project

TO USE:
	
	[a] run sh start_server.sh in located in /minecraft_api

	[b] run the minecraft client ./minecraft-1.6.4.jar

	[c] run move.js in /minecraft_api/mineflayer/affordances/ with node

	[d] See commands listed in move.js (ex: "plan1", "F", "reset", etc.)

Contact Dave Abel for any questions @ David_Abel@brown.edu
