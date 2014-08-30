#!/usr/bin/env python

import subprocess
import os

#jobID = os.environ['SGE_TASK_ID']
useMAs = "false"
useOptions = "false"
jobID = '1'
subprocess.call(['java', '-jar', '-Xmx2048m', 'minecraftLearner.jar', jobID, useMAs, useOptions])
