![MBARI logo](https://raw.githubusercontent.com/underwatervideo/annosaurus/master/src/site/images/logo-mbari-3b.png)

# annosaurus

MBARI is updating its [Video Annotation and Reference System](https://hohonuuli.github.io/vars/) for modern video workflows. The service in this repository is one component of our next generation system. _annosaurus_ is the component that stores and retrieves annotations for a video or images. It is being designed to work as a programming-language agnostic API that can be accessed from any programming language. The goal of this project is to provide a data service that allows developers and scientists to easily build their own tools for annotating video and images collections.

## Design Goals

- Create a web-accessible API for creating, updating and deleting video annotations.
- Use lessons learned from MBARI's video annotation system to create flexible, but searchable video annotations
- This service should stand on it's own and _not require_ any of the other VARS services to function. This is to provide flexibility in how organizations use this service.
- Simplify installation and deployment for organizations. We recognize that many science institutions would benefit from simply installation. 

## Underlying Data Model

This model _might_ not be directly exposed to end users. It's the current internal notional data model.

![Data Model](https://raw.githubusercontent.com/underwatervideo/annosaurus/master/src/site/images/annotation_data_model.png)

```
+--------------+               +----------------+
| ImagedMoment |-[1]--[0..*]->| ImageReference |
+--------------+               +----------------+
       |      \
      [1]      \    +---------------------+
       |        --- | CachedAnxillaryData |
       |            +---------------------+
      [0..*]
       |
       v
+-------------+               +-------------+
| Observation |-[1]--[0..*]-> | Association |
+-------------+               +-------------+
```



- `ImagedMoment`: Reference to some index in a particular video. The other model pieces are joined to this.
- `Observation`: Represents an `annotation`. Includes annotation term, an optional duration, and tracks who made the observation.
- `Association`: Information that augments an observation. Very flexible, the format is `linkName | toConcept | linkValue`. Some examples; `eating | Aegina | nil`, `surface-color | self | red`, `audio-comment | nil | first sighting on this mission`. 
- `ImageReference`: Images, such as framegrabs, linked to the moment. It will also be possible to load image references for image annotation projects.
- `CachedAnxillaryData`: For performance reason, we may want to cache some time indexed information, such as position, CTD, etc, in side the same database as the annotations.
- `CachedMissionInfo`: This may contain information describing a camera deployment.


__Related Projects__:

- [vampire-squid](https://github.com/underwatervideo/vampire-squid): A service for registering and locating video files.

## Building

This project is built using [SBT](http://www.scala-sbt.org/). To create the distribution, run `sbt pack`
