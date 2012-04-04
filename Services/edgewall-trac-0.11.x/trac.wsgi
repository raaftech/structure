import sys
sys.stdout = sys.stderr

import os
os.environ['TRAC_ENV'] = 'TRAC0_DATA_DIRECTORY/repository'
os.environ['PYTHON_EGG_CACHE'] = 'TRAC0_DATA_DIRECTORY/repository/eggs'

import trac.web.main

application = trac.web.main.dispatch_request
