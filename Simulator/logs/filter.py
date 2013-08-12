#!/usr/bin/env python

import time
import sys
import re


PROCESS_TRAFFIC_PATTERN = r'Normal: ([0-9]+) / ([0-9]+) ; Compressed: ([0-9]+) / ([0-9]+) ; Ratio: ([0-9]+\.[0-9]+|NaN) / ([0-9]+\.[0-9]+|NaN)'
LINE_PATTERN = r'^ *(?P<level>[A-Z]+) (?P<node>[a-z0-9]+)\.(?P<router>[a-z]+)\.node +(?P<tag>[A-Za-z0-9.]+): +(?P<message>.+)$'
MESSAGE_PATTERN = r'^\((?P<date>[^)]+)\) (?P<type>[^ ]+) \[ *(?P<data>[^\]]+) *\]$'
DEVICES = ('motorolaatrix', 'galaxyw1', 'galaxyw2', 'galaxyw3', 'galaxytab1', 'galaxytab2')
ROUTERS = ('dlife', 'prophet')


def process_discovery_traffic(data):
	data = data.split('/')
	return (int(data[0]), int(data[1]))


def process_cl_traffic(data):
	data = re.match(PROCESS_TRAFFIC_PATTERN, data).groups()
	return (int(data[0]), int(data[1]), int(data[2]), int(data[3]), float(data[4]), float(data[5]))


def process_bundles(data):
	data = data.split('/')
	return (int(data[0]), int(data[1]))


TRACED_DATA = {
	'Discovery-Traffic': process_discovery_traffic,
	'CL-Traffic': process_cl_traffic,
	'Bundles': process_bundles
}

nodes = { }
for node in DEVICES:
	nodes[node] = { }

for line in sys.stdin:
	r = re.match(LINE_PATTERN, line)
	if not r:
		continue

	level, node_name, router, tag, message = r.groups()
	if tag != 'InfoLogger':
		continue

	if not node_name in DEVICES:
		continue

	r = re.match(MESSAGE_PATTERN, message)
	if not r:
		continue

	date, _type, data = r.groups()
	if not _type in TRACED_DATA:
		continue

	seconds = int(time.mktime(time.strptime(date, '%a %b %d %H:%M:%S %Z %Y')))
	node = nodes[node_name]

	if seconds in node:
		when = node[seconds]
	else:
		when = node[seconds] = { }

	parser = TRACED_DATA[_type]
	when[_type] = parser(data)


out = sys.stdout
out.write('{\n')
for node, data in nodes.iteritems():
	out.write('\t\'')
	out.write(node)
	out.write('\': {\n')
	items = data.items()
	items.sort(lambda d1, d2: d1[0] - d2[0])
	for when, info in items:
		out.write('\t\t')
		out.write(str(when))
		out.write(': ')
		out.write(str(info))
		out.write(',\n')

	out.write('\t},\n')
out.write('}\n')
out.flush()

