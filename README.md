sinalgo-timesync
================

Simulation of different time synchronization protocols (FTSP, PulseSync, GTSP) in the Sinalgo network simulator.


Usage:
======
    cd Sinalgo
    ant Sinalgo

Modify "./Sinalgo/src/projects/clocksync/CustomGlobal.javae" to change simulation parameters.

Protocol implementations can be found in "Sinalgo/src/projects/clocksync/nodes/nodeImplementations/":

    FloodingTimeSyncProtocolNode.java   FTSP
    GradientTimeSyncProtocolNode.java   GTSP
    PulseTimeSyncProtcolNode.java       PulseSync

References:
===========

Sinalgo - Simulator for Network Algorithms:
http://www.dcg.ethz.ch/projects/sinalgo/

FTSP:
The Flooding Time Synchronization Protocol, M. Maroti et al., SenSys'04
http://dl.acm.org/citation.cfm?id=1031501

GTSP:
Gradient Clock Synchronization in Wireless Sensor Networks, P. Sommer and R. Wattenhofer, IPSN'09
http://dl.acm.org/citation.cfm?id=1602171

PulseSync:
Optimal Clock Synchronization in Networks, C. Lenzen, P.Sommer, and R. Wattenhofer, SenSys'09
http://dl.acm.org/citation.cfm?id=1644061
