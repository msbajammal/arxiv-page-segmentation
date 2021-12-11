import numpy as np
# from matplotlib import rcParams
# rcParams['font.family'] = 'serif'
# rcParams['font.serif'] = 'Times New Roman' 
import matplotlib.pyplot as plt
import ntpath
import os

data = {'Population of Sweden': 9995000,
        'Population of Canada': 36710000,
        'Population of Italy': 60590000,
        'Population of Germany': 82790000,
        'Individuals with accessibility impairments, European Union': 27841024,
        'Individuals with accessibility impairments, United States': 69601317}

plt.rcParams.update({'font.family': 'serif',
                            'font.serif': 'Times New Roman',
                            'font.size': 9})


SMALL_SIZE = 8
MEDIUM_SIZE = 10
BIGGER_SIZE = 12

plt.rc('font', size=SMALL_SIZE)          # controls default text sizes
plt.rc('axes', titlesize=SMALL_SIZE)     # fontsize of the axes title
plt.rc('axes', labelsize=MEDIUM_SIZE)    # fontsize of the x and y labels
plt.rc('xtick', labelsize=SMALL_SIZE)    # fontsize of the tick labels
plt.rc('ytick', labelsize=SMALL_SIZE)    # fontsize of the tick labels
plt.rc('legend', fontsize=SMALL_SIZE)    # legend fontsize
plt.rc('figure', titlesize=BIGGER_SIZE)  # fontsize of the figure title


plt.figure(figsize=(3.75, 2.5))
y = list(data.values())
x = np.arange(len(y))
ax = plt.gca()
ax.get_yaxis().set_visible(False)
ax.xaxis.grid(color='#AAAAAA', linestyle='dotted')
ax.set_axisbelow(True)
plt.ylim(bottom=-0.5, top=len(x)-0.5)
plt.barh(x, y, height=-0.4, align='edge', zorder=10, color=['#1a334d', '#1a334d', '#1a334d', '#1a334d',
                                                            '#99b3cc', '#99b3cc'])
locs, labels = plt.xticks()
plt.xticks(locs, ['', '20', '40', '60', '80'])
plt.xlabel("Millions of people", {'fontsize': 8.5})

# plt.xticks(x, list(data.keys()))
# plt.ylabel("Number of lawsuits", {'fontsize': 10})

# Plot numbers on top of bars

# set zorder so annotated filled text boxes are behind the axis
for k, spine in ax.spines.items():  #ax.spines is a dictionary
    spine.set_zorder(10)

rects = ax.patches
labels = list(data.keys())
for rect, label in zip(rects, labels):
    yoffset = 0.01
    xoffset = 0.5
    ax.text(rect.get_x() + xoffset, rect.get_y() + yoffset, ' '+label, fontsize=7.5, bbox=dict(facecolor='white', zorder=1, linewidth=0),
            weight='light', ha='left', va='bottom')    

plt.savefig('%s.%s'%(os.path.splitext(ntpath.basename(__file__))[0],'pdf'),bbox_inches='tight')
