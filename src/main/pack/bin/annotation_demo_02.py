#!/usr/bin/env python

import requests
import json

def post(url, data=None):
    if data is None:
        data = {}
    r = requests.post(url, data)
    return json.loads(r.text)

def get(url):
    r = requests.get(url)
    return json.loads(r.text)

base_url = "http://localhost:8080/v1/"
annotation_url = base_url + "annotations"
association_url = base_url + "associations"
imagedmoment_url = base_url + "imagedmoments"
image_url = base_url + "images"

video_reference_uuid = "318bd938-91b0-4d7d-adca-7725cd526339"

obs = post(annotation_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "concept": "Nanomia bijuga",
             "observer": "brian",
             "recorded_timestamp": "2016-07-28T14:29:01.030Z"})

post(image_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "url": "http://foobar.com/anotherimage.png",
             "recorded_timestamp": "2016-07-28T14:29:01.030Z",
             "width_pixels": 1920,
             "height_pixels": 1080,
             "format": "image/png"})

post(image_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "url": "http://foobar.com/anotherimage.jpg",
             "recorded_timestamp": "2016-07-28T14:29:01.030Z",
             "width_pixels": 1920,
             "height_pixels": 1080,
             "format": "image/jpg",
             "description": "Compressed with overlay"})

post(association_url,
     data = {"observation_uuid": obs["observation_uuid"],
             "link_name": "eating",
             "to_concept": "Sergestes",
             "link_value": "nil"})

post(association_url,
     data = {"observation_uuid": obs["observation_uuid"],
             "link_name": "surface color",
             "link_value": "red"})

print("Dump for all annotations for video-reference of " + video_reference_uuid)
query_url = annotation_url + "/videoreference/" + video_reference_uuid
print(get(query_url))

print("Dump for all imaged-moments for video-reference of " + video_reference_uuid)
query_url = imagedmoment_url + "/videoreference/" + video_reference_uuid
print(get(query_url))