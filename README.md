![MBARI logo](https://raw.githubusercontent.com/underwatervideo/video-annotation-service/master/src/site/images/logo-mbari-3b.png)

# video-annotation-service

MBARI is updating its [Video Annotation and Reference System](https://hohonuuli.github.io/vars/) for modern video workflows. The service in this repository is one component of our next generation system. The _video-annotation-service_ is the component that stores and retrieves annotations for a video or images. It is being designed to work as a programming-language agnostic API that can be accessed from any programming language. The goal of this project is to provide a data service that allows developers and scientists to easily build their own tools for annotating video and images collections.

## Design Goals

- Create a web-accessible API for creating, updating and deleting video annotations.
- Use lessons learned from MBARI's video annotation system to create flexible, but searchable video annotations
- This service should stand on it's own and _not require_ any of the other VARS services to function. This is to provide flexibility in how organizations use this service.
- Simplify installation and deployment for organizations. We recognize that many science institutions would benefit from simply installation. 

__Related Projects__:

- [video-asset-manager](https://github.com/underwatervideo/video-asset-manager): A service for registering and locating video files.

## Building

This project is built using [SBT](http://www.scala-sbt.org/). To create the distribution, run `sbt pack`
