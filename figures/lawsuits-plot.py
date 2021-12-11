import numpy as np
# from matplotlib import rcParams
# rcParams['font.family'] = 'serif'
# rcParams['font.serif'] = 'Times New Roman' 
import matplotlib.pyplot as plt
import ntpath
import os

data = {'2015': 57, '2016': 262, '2017': 814, '2018': 2258}

plt.rcParams.update({'font.family': 'serif',
                            'font.serif': 'Times New Roman',
                            'font.size': 9})


plt.figure(figsize=(4, 3.3))
y = list(data.values())
x = np.arange(len(y))
ax = plt.gca()
ax.yaxis.grid(color='#AAAAAA', linestyle='dotted')
ax.set_axisbelow(True)
plt.bar(x, y, width=0.6, color='#1a334d')
plt.xticks(x, list(data.keys()))
plt.ylabel("Number of lawsuits", {'fontsize': 10})

# Plot numbers on top of bars

rects = ax.patches
labels = list(data.values())
for rect, label in zip(rects, labels):
    height = rect.get_height()
    ax.text(rect.get_x() + rect.get_width() / 2, height + 5, label,
            size='small', weight='light', ha='center', va='bottom')

plt.savefig('%s.%s'%(os.path.splitext(ntpath.basename(__file__))[0],'pdf'),bbox_inches='tight')
