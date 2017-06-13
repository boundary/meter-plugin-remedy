TrueSight Pulse Remedy Integration Plugin
=========================================

Collects tickets(Incident management and change management) from Remedy servers and collected tickets are sent to Truesight Intelligence as Events. 
The plugin allows multiple Remedy instances data to be collected with independent polling interval.

### Prerequisites

#### Supported OS

|     OS    | Linux | Windows | OS X |
|:----------|:-----:|:-------:|:----:|
| Supported |   v   |    v    |  v   |

#### Runtime Environment

|  Runtime | node.js | Python | Java |
|:---------|:-------:|:------:|:----:|
| Required |         |        |    v*  |
\* java 1.8+ 

* [How to install java?](https://www3.ntu.edu.sg/home/ehchua/programming/howto/JDK_Howto.html)


#### TrueSight Pulse Meter versions v4.6.2-835 or later

- To install new meter go to Settings->Installation or [see instructions](https://help.boundary.com/hc/en-us/sections/200634331-Installation).
- To upgrade the meter to the latest version - [see instructions](https://help.boundary.com/hc/en-us/articles/201573102-Upgrading-the-Boundary-Meter).

#### Plugin Configuration Fields

|Field Name        |Description                                                                    |
|:-----------------|:------------------------------------------------------------------------------|
|AR Server Name    |The host of Remedy server                                            		   |
|Port              |The port of Remedy server                                            		   |
|Username          |The user of Remedy server                                            		   |
|Password          |The password of Remedy server                                        		   |
|Poll Interval     |How often (in minutes) to poll for collecting the tickets                    |
|Ticket Type       |Type of tickets to be collected(Incidents or Change)                                      |
|Field Mapping     |Type of fields will be collected(more info please check in template section)   |



### Templates
 1. [Incident Default Template](https://github.com/boundary/meter-plugin-remedy/blob/master/template/incidentDefaultTemplate.json)
 2. [Change Default Template](https://github.com/boundary/meter-plugin-remedy/blob/master/template/changeDefaultTemplate.json)

### References

Need to add remedy docs here.

