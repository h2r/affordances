import sys

f = file(sys.argv[1],"r")

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
