#!/usr/bin/env python

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
image_url = base_url + "images"

video_reference_uuid = "318bd938-91b0-4d7d-adca-7725cd526339"
post(anno_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "concept": "Aegina",
             "observer": "brian",
             "elapsed_time_millis": 12345})

post(image_url,
     data = {"video_reference_uuid": video_reference_uuid,
             "url": "http://foobar.com/someimage.png",
             "width_pixels": 1920,
             "height_pixels": 1080,
             "format": "image/png"})