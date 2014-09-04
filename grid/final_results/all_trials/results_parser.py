from os import listdir
from os.path import isfile, join
from collections import defaultdict
import re
import sys
import numpy as np
import scipy.stats as st
import math

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

def merge_result_files(result_file_list):
    """
    Note:
        Merges the provided list of knowledge base files into a single knowledge base

    Args:
        result_file_list: a list of strings, where each string refers to a result file of relevance to merge

    Returns:
        result_files: a dictionary containing all the relevant merged information, where:
            Key = ((str) maptype)
            Value = {(str) planner name, [(int) bellman, (int) reward, (int) cpu time]} results = {}
    """
    # Key = ((str) maptype)
    # Value = {(str) planner name, [(int) bellman, (int) reward, (int) cpu time]}   results = {}

    map_counter = defaultdict(int) # Stores the number of maps solved for each type
    results = {}
    
    # Loop over each file and merge knowledge base counts
    for result_file_name in result_file_list:
        result_file = file(result_file_name,'r')
        map_type = ""
        for line in result_file.readlines():
            # Set map name
            if len(line) == 1 or ("MAs" not in line and "RTDP " not in line and "map" not in line):
                continue
            if "map" in line:
                map_type = line.split(' ')[1][:-5]
                map_counter[map_type] += 1
            else:
                split_line = line.split(' ')
                planner = split_line[0]
                bellman = float(split_line[1])
                reward = float(split_line[2])
                cpu = float(split_line[3])
                # If the map is in the results already
                if map_type in results:
                    if planner in results[map_type]:
                        results[map_type][planner][0] += [bellman]
                        results[map_type][planner][1] += [reward]
                        results[map_type][planner][2] += [cpu]
                    else:
                        results[map_type][planner] = [[bellman], [reward], [cpu]]

                # Otherwise, if map is not in the results yet
                else:
                    results[map_type] = {planner : [[bellman], [reward], [cpu]]}

    return results, map_counter

def write_results_to_file(merged_results, map_counter):
    """ 
    Note:
        Writes out the final knowledge base file to "grid.kb"

    Args:
        merged_kb: a dictionary containing all the relevant knowledge base information
                merged_kb: a dictionary containing all the relevant knowledge base information

    """
    outfile = open("grid.results", "w+")
    for map_type in merged_results:
        outfile.write(map_type + "\n")
        for planner in merged_results[map_type]:
            outfile.write(planner + ",")
            outfile.write(str(merged_results[map_type][planner][0]) + ",")
            outfile.write(str(merged_results[map_type][planner][1]) + ",")
            outfile.write(str(merged_results[map_type][planner][2]) + "\n")
        outfile.write("\n");
    outfile.close()

def write_temp_ext_results_to_file(merged_results):
    """ 
    Note:

    Args:
    """
    outfile = open("grid.results", "w+")
    for planner in merged_results:
        outfile.write(planner + ",")
        outfile.write(str(merged_results[planner][0]) + ",")
        outfile.write(str(merged_results[planner][1]) + ",")
        outfile.write(str(merged_results[planner][2]) + "\n")
        outfile.write("\n");
    outfile.close()

def merge_map_results(merged_results, map_counter):
    results = {} # {planner : [bellman, reward, cpu]}
    num_trials = 0
    for map_type in merged_results:
        num_trials += map_counter[map_type]
        for planner in merged_results[map_type]:
            if planner in results.keys():
                for item in merged_results[map_type]:
                    results[planner][0] += merged_results[map_type][planner][0]
                    results[planner][1] += merged_results[map_type][planner][1]
                    results[planner][2] += merged_results[map_type][planner][2]
            if planner not in results.keys():
                results[planner] = merged_results[map_type][planner]


    # for planner in results:
        # results[planner][0] = float(results[planner][0]) / num_trials
        # results[planner][1] = float(results[planner][1]) / num_trials
        # results[planner][2] = float(results[planner][2]) / num_trials


    return results

def averageAndGetCIs(results, mapCounter):
    for mapType in results:
        for planner in results[mapType]:
            data = results[mapType][planner]
            for index, dataList in enumerate(data):
                results[mapType][planner][index] = dataToAverageAndCI(dataList)

def averageAndGetCIsForTemp(tempData, mapCounter):
    for planner in tempData:
        data = tempData[planner]
        allLists = []
        toChange = []
        for tupleOfData in data:
            currList = list(tupleOfData)
            avgAndCI = dataToAverageAndCI(currList)
            toChange += [tuple(avgAndCI)]

        tempData[planner] = toChange
        

def dataToAverageAndCI(data):
    confidenceLevel = .95
    sampleSize = len(data)


    observedMean = sum(data)/(sampleSize)
    
    STD = np.std(data)

    alpha = 1.0 - confidenceLevel

    zVal = st.norm.ppf(float(alpha)/float(2))

    CIPlusMinues = math.fabs(zVal * STD/(math.sqrt(sampleSize)))

    return (round(observedMean,1), round(CIPlusMinues))

def main():
    
    # Get job name
    try:
        job_name = sys.argv[1]
    except:
        print "Error: missing argument.\nUsage: > python kb_parser.py <job_name> <(opt) temp_flag>"
        quit()
    # Set up the result files
    result_files = setup_files(job_name)
    num_trials = len(result_files)

    # Merged all results into one
    merged_results, map_counter = merge_result_files(result_files)
        

    averageAndGetCIs(merged_results, map_counter)
    if(len(sys.argv) > 2):
        temp_ext_results = merge_map_results(merged_results, map_counter)
        averageAndGetCIsForTemp(temp_ext_results, map_counter)
        print temp_ext_results
        write_temp_ext_results_to_file(temp_ext_results)
        quit()
        
    # Write the merged knowledge base out to a file
    write_results_to_file(merged_results, map_counter)

if __name__ == "__main__":
    main()