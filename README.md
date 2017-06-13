TrueSight Pulse Remedy Integration Plugin
=========================================

Collects tickets(Incident management and change management) from Remedy servers and collected tickets are sending to Truesight Intelligence. 
The plugin allows multiple Remedy inctances data to be collectedd each of those to set their own Poll interval.

### Prerequisites

#### Supported OS

|     OS    | Linux | Windows | SmartOS | OS X |
|:----------|:-----:|:-------:|:-------:|:----:|
| Supported |   v   |    v    |    -    |  v   |

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
|Host              |The host of Remedy server                                            		   |
|Port              |The port of Remedy server                                            		   |
|Username          |The user of Remedy server                                            		   |
|Password          |The password of Remedy server                                        		   |
|Poll Interval     |How often (in milliseconds) to poll for collect the tickets                    |
|Request Type      |Type of tickets to be collected(IM or CM)                                      |
|Remedy Fields     |Type of fields will be collected(more info please check in template section)   |



### Templates
Need to add information.

### References

Need to add remedy docs here.

