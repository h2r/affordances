import sy
from os import listdir
from os.path import isfile, join

path = "."

all_kbs = [f for f in listdir(mypath) if isfile(join(mypath,f)) ]
kb_files = []

# Get kb files
for f in all_files:
	if ".kb" in f and "old" not in f:
		kb_files.append(f)

# Rename all files
for kb in kb_files:
	os.rename(kb, "old_" + kb)


out_file = file("new_" + sys.argv[1], "w+")

for line in f.readlines():
	if len(line) <= 1:
		print line
		continue
	elif ";" in line:
		print line
		continue
	else:
		out_file.write(line)
