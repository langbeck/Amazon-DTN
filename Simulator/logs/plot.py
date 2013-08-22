#!/usr/bin/env python

from matplotlib.pyplot import *
from matplotlib.ticker import *
from pylab import *

device_name = 'motorolaatrix'

def format_time(t, pos=None):
	t = int(t)
	return '{0}d {1:02d}:{2:02d}'.format(
		t / 86400,
		(t / 3600) % 24,
		(t / 60) % 60
	)


def format_size(s, pos=None):
	s = int(s)
	if s < 0x400:
		return s
	elif s < 0x100000:
		return '{0:.2f} K'.format(s / float(0x400))
	else:
		return  '{0:.2f} M'.format(s / float(0x100000))


with open('summary.dat', 'r') as fp:
	data = eval(fp.read())

	discovery_received_data = []
	discovery_sent_data = []

	cl_received_compressed_data = []
	cl_sent_compressed_data = []

	cl_received_normal_data = []
	cl_sent_normal_data = []

	cl_received_ratio_data = []
	cl_sent_ratio_data = []

	bundles_received_data = []
	bundles_sent_data = []

	time_data = []

	items = data[device_name].items()
	items.sort(lambda d1, d2: d1[0] - d2[0])
	for when, info in items:
		discovery_received, discovery_sent = info['Discovery-Traffic']
		bundles_received, bundles_sent = info['Bundles']
		cl_traffic = info['CL-Traffic']

		cl_received_compressed_data.append(cl_traffic[2])
		cl_sent_compressed_data.append(cl_traffic[3])

		cl_received_normal_data.append(cl_traffic[0])
		cl_sent_normal_data.append(cl_traffic[1])

		cl_received_ratio_data.append(cl_traffic[4])
		cl_sent_ratio_data.append(cl_traffic[5])

		discovery_received_data.append(discovery_received)
		discovery_sent_data.append(discovery_sent)

		bundles_received_data.append(bundles_received)
		bundles_sent_data.append(bundles_sent)

		time_data.append(when)


	min_time = min(time_data)
	time_data = [t - min_time for t in time_data]

	fig = figure()
	axis = fig.add_subplot(1, 1, 1)
	axis.yaxis.set_major_formatter(FuncFormatter(format_size))
	axis.xaxis.set_major_formatter(FuncFormatter(format_time))

	# CL Compressed
	axis.plot(time_data, cl_received_compressed_data, label='CL Received (compressed)')
	axis.plot(time_data, cl_sent_compressed_data, label='CL Sent (compressed)')

	# CL Normal
	axis.plot(time_data, cl_received_normal_data, label='CL Received (normal)')
	axis.plot(time_data, cl_sent_normal_data, label='CL Sent (normal)')

	# CL Ratio
	#axis.plot(time_data, cl_received_ratio_data, label='CL Received (ratio)')
	#axis.plot(time_data, cl_sent_ratio_data, label='CL Sent (ratio)')

	# Bundles
	#axis.plot(time_data, bundles_received_data, label='Bundles seceived')
	#axis.plot(time_data, bundles_sent_data, label='Bundles sent')

	title(device_name)
	legend()
	show()
