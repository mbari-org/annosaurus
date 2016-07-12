#!/usr/bin/env python

# This is a script for loading some test data into the video-annotation-service using
# REST calls. Each insert is being done as a separate call, which is the only type of
# insert that the API supports at this point.
#

import requests
import json

def post(url, data = {}):
    r = requests.post(url, data)
    return json.loads(r.text)

def get(url):
    r = requests.get(url)
    return json.loads(r.text)

base_url = "http://localhost:8080/v1/"
anno_url = base_url + "annotations"
im_url = base_url + "imagedmoments"


# Create some annotations. The object model of ImagedMoment --> Observation is generated
# invisibly. Duplicate indices into the same video_reference will use the same ImageMoment
# object/database row
video_reference_uuid = "318bd938-91b0-4d7d-adca-7725cd526339"
post(anno_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "concept": "Aegina",
             "observer": "brian",
             "elapsed_time_millis": 12345})

post(anno_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "concept": "Nanomia bijuga",
             "observer": "brian",
             "elapsed_time_millis": 12345, # Same timestamp as above
             "duration_millis": 3500})     # This one includes a duration

post(anno_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "concept": "Pandalus platyceros",
             "observer": "brian",
             "elapsed_time_millis": 14001})

video_reference_uuid = "73a436f6-788a-4f0b-b28f-9e6e47b71c01"
post(anno_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "concept": "Decapoda",
             "observer": "brian",
             "elapsed_time_millis": 1000})  # Use elapsed time since start of video as index

post(anno_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "concept": "Nanomia bijuga",
             "observer": "brian",
             "timecode": "01:03:24:20",             # Use a timecode as an index
             "duration_millis": 1000})     # This one includes a duration

post(anno_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "concept": "Pandalus platyceros",
             "observer": "brian",
             "recorded_timestamp": "2016-07-12T16:47:03.12Z"}) # Use date video frame was recorded as index


print("Dump for all annotations for video-reference of " + video_reference_uuid)
query_url = anno_url + "/videoreference/" + video_reference_uuid
print(get(query_url))
