episodeFile = file("planResult.episode")

outfile = file("plan.p","w")

for line in episodeFile:
	if line[0].isalpha():
		line = line.strip() + ","
		outfile.write(line)