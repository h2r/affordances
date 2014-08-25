from os import listdir
from os.path import isfile, join
import re
import sys


def setup_files(job_name):
	""" 
	Note:
		Sets up the files corresponding to the STDOUT of each grid task

	Args:
		job_name: the name (with ID) of the associated with the grid tasks to fetch kb files from

	Returns:
		A list of file names
	"""
	# Get the STDOUT files from the grid job in the home directory 
	path = "."
	all_files = [f for f in listdir(path) if isfile(join(path,f))]
	kb_files = []

	for f in all_files:
		if job_name in f:
			kb_files.append(f)

	return kb_files

def merge_kb_files(kb_file_list):
	"""
	Note:
		Merges the provided list of knowledge base files into a single knowledge base

	Args:
		kb_file_list: a list of strings, where each string refers to a knowledge base file of relevance to merge

	Returns:
		knowledge_base: a dictionary containing all the relevant merged information, where:
			Key = ((str) P, (str) G)
			Value = {(str) action_name, [(int) action_count, (int) total_action_count]}
	"""
	# Key = ((str) P, (str) G)
	# Value = {(str) action_name, [(int) action_count, (int) total_action_count]}
	knowledge_base = {}

	# Loop over each file and merge knowledge base counts
	for kb_name in kb_file_list:
		kb_file = file(kb_name,'r')
		affordance = ""
		increment_total_act_counts = True
		for line in kb_file.readlines():
			# Remove uneccessary lines
			if "reachability" in line or ";" in line or "," not in line:
				continue
			else:
				# If we're at a new affordance
				if line[0].isupper():
					affordance = line.strip()
				# Otherwise, if at action counts
				elif line[0].isalpha():
					# action, count, total count
					counts = line.strip().split(",")
					action_name = counts[0]
					action_count = int(counts[1])
					action_total = int(counts[2])

					# If the affordance is in the kowledge base already
					if affordance in knowledge_base:
						# If the action is in that affordance's knowledge base already
						if action_name in knowledge_base[affordance]:
							knowledge_base[affordance][action_name][0] += action_count
							# Only increment total_act_counts once per file
							if increment_total_act_counts:
								knowledge_base[affordance][action_name][1] += action_total
						# If the action is not in the affordance yet, initialize it
						else:
							knowledge_base[affordance][action_name] = [action_count, action_total]

					# Otherwise, if affordance is not in the KB yet
					else:
						knowledge_base[affordance] = {action_name : [action_count, action_total]}

		# After we've gone through the first affordance, 
		increment_total_act_counts = False

	return knowledge_base

def write_kb_to_file(merged_kb):
	""" 
	Note:
		Writes out the final knowledge base file to "grid.kb"

	Args:
		merged_kb: a dictionary containing all the relevant knowledge base information
	"""
	outfile = open("grid.kb", "w+")
	for affordance in merged_kb:
		outfile.write(affordance + "\n")
		for action in merged_kb[affordance]:
			outfile.write(action + ",")
			outfile.write(str(merged_kb[affordance][action][0]) + ",")
			outfile.write(str(merged_kb[affordance][action][1]) + "\n")
		outfile.write("===\n")
	outfile.close()

def main():
	# Get job name
	try:
		job_name = sys.argv[1]
	except:
		print "Error: missing argument.\nUsage: > python kb_parser.py <job_name>"
		quit()
	# Set up the knowledge base files
	kb_files = setup_files(job_name)

	# Merged all knowledge bases into one
	merged_kb = merge_kb_files(kb_files)

	# Write the merged knowledge base out to a file
	write_kb_to_file(merged_kb)

if __name__ == "__main__":
	main()