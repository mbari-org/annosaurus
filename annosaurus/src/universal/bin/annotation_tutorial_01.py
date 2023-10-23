#!/usr/bin/env python

#

import json
import pprint
import requests
import uuid

def show(s, data = None):
    pp = pprint.PrettyPrinter(indent=2)
    print("--- " + s)
    if data:
      pp.pprint(data)

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~ Define some simple HTTP support functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def post(url, data = {}):
    """
    Submit http post to a URL and parse the JSON that's returned
    :param url: The URL to post to
    :param data: Map defining the post params to submit. keys and values should be strings
    :return: The JSON response
    """
    r = requests.post(url, data)
    return json.loads(r.text)

def get(url):
    """
    Submit http get to a URL and parse the JSON that's returned
    :param url: The URL to submit get to
    :return: The JSON response
    """
    r = requests.get(url)
    return json.loads(r.text)

def put(url, data = {}):
    """
    Submit http put to a URL and parse the JSON that's returned
    :param url: The URL to submit put to
    :return: The JSON response
    """
    r = requests.put(url, data)
    return json.loads(r.text)

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~ Define the URLS. This tutorial assumes that you are running localhost ~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

# 1. Define endpoint. Normally in an app you just need to define an endpoint then build the other
#    API's from the endpoint
endpoint = "http://localhost:8080"

# 2. Build APIs from endpoint.
# The annotation and image url are what most users will primarily usefor creating annotations
annotation_url = "%s/v1/annotations" % (endpoint)

# The annotation and image url are what most users will primarily use
#    for creating annotations and registering images
image_url = "%s/v1/images" % (endpoint)

# These URLS are for more fine graine access and for deleting stuff from the datastore
imaged_moment_url = "%s/v1/imagedmoments" % (endpoint)
observation_url = "%s/v1/observations" % (endpoint)
association_url = "%s/v1/associations" % (endpoint)

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~ Annotation API ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# The annotation API is an abstraction to make it very simple to create and modify annotations.
# It provides a simple search if you know your video_reference_uuid. It does not allow you
# to delete. You use a different API (Observation API) for that



# 1. We need a UUID that is a reference to a video file or image set. The UUID is used to logically
#    group a set of annotations. Here we just create one
video_reference_uuid = str(uuid.uuid4())

# 2. Create

# Minimum fields needed to create. Requires at least one of (recorded_timestamp,
# elapsed_time_millis, or timecode). Here we use recorded_timestamp
annotation = post(annotation_url,
                  data = {"video_reference_uuid": video_reference_uuid,
                          "concept": "Nanomia bijuga",
                          "observer": "brian",
                          "recorded_timestamp": "2016-07-28T14:29:01.030Z"})
show("Created:", annotation)

# All possible fields, including optional values
annotation = post(annotation_url,
                  data = {"video_reference_uuid": video_reference_uuid,      # video or image grouping
                          "concept": "Aegina citrea",                        # Name of what you saw
                          "observer": "schlin",                              # Who made the observation
                          "observation_timestamp": "2016-07-28T15:01:02Z",   # When the observation was make. Default is the servers timestamp
                          "timecode": "01:23:34:09",                         # A tape timecode of annotation
                          "elapsed_time_millis": "112345",                   # Time since start of video of annotation
                          "duration_millis": "1200",                         # How long was object observed
                          "group": "ROV",                                    # A logical group. At MBARI, we might use "ROV", "AUV", "Station M"
                          "activity": "transect",                            # Another logical group. At MBARI, we would use, ascent, descent, transect, cruise, etc.
                          "recorded_timestamp": "2016-07-28T14:39:02.123Z"}) # The time the frame was recorded. e.g. We saw this Aegina on this date.
show("Created:", annotation)

# 3. Update (Modify an existing annotation)

# You need an observation_uuid value to indicate which Annotation to modify. We'll pull
# that from our existing annotation. There is a reason it's called observation_uuid and
# not annotation_uuid; but I won't explain that here.
observation_uuid = annotation["observation_uuid"]

# At a minimum you need the observation_uuid and one field. The observation_timestamp
# will automatically be updated to the time on the server (UTC). Here we just change
# the concept name. Normally, you might need to update the observer field too.
put_url = "%s/%s" % (annotation_url, observation_uuid)
annotation = put(put_url,
                 data = {"observation_uuid": observation_uuid,
                         "concept": "Atolla"})
show("Updated using " + put_url, annotation)

# You can update any and all fields in one call as we do here.
annotation = put(put_url,
                  data = {"video_reference_uuid": str(uuid.uuid4()),         # Here we move the annotation to a new video
                          "concept": "Pandalus platyceros",                  # Name of what you saw
                          "observer": "danelle",                             # Who made the observation
                          "observation_timestamp": "2016-09-22T15:01:02Z",   # When the observation was make. Default is the servers timestamp
                          "timecode": "08:00:34:09",                         # A tape timecode of annotation
                          "elapsed_time_millis": "3045999",                  # Time since start of video of annotation
                          "duration_millis": "8",                            # How long was object observed
                          "group": "AUV",                                    # A logical group. At MBARI, we might use "ROV", "AUV", "Station M"
                          "activity": "descent",                             # Another logical group. At MBARI, we would use, ascent, descent, transect, cruise, etc.
                          "recorded_timestamp": "2021-07-28T14:39:02.123Z"}) # The time the frame was recorded. e.g. We saw this Aegina on this date.
show("Updated using " + put_url, annotation)

# 4. Find

# Find an annotation by it's observation_uuid
get_url = "%s/%s" % (annotation_url, annotation["observation_uuid"])
annotation = get(get_url)
show("Search Result for " + get_url, annotation)

# Find ALL annotations for a given video_reference_uuid. Remember a video_reference_uuid
# is essentially a key referring to a video or image set. To look up video_reference_uuids
# You will use the VideoReference API described later.
get_url = "%s/videoreference/%s" % (annotation_url, annotation["video_reference_uuid"])
annotations = get(get_url)
show("Search results for " + get_url, annotations)

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~ Image API ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# The image API is for registering images that can be annotated. Each image needs to be indexed
# either to a video using elpased_time_millis or timecode or to actual reality with recorded_timestamp

# 1. Create


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~ Association API ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# Associations store additional descriptive information about an annotation.

# 1. Create an Association with minimum fields
association = post(association_url, data = {"observation_uuid": observation_uuid, "link_name": "swimming"})
show("Created using " + association_url, association)

# 2. Create using all possible fields
association = post(association_url,
                   data = {
                       "observation_uuid": observation_uuid,
                       "link_name": "distance measurement",
                       "to_concept": "self",
                       "link_value": '{"x0": 100, "y0: 89", "x1'
                   })

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~ Observation API ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# Observation API is slightly lower level than the annotation. It is specifically for editing,
# modifying and searching for observation. For the record an annotation is actually composed of:
# An ImagedMoment, one or more Observations, one or more ImageReferences and zero or one
# CachedAncillaryDatum.

# 1. Find an observation by it's uuid
get_url = "%s/%s" % (observation_url, observation_uuid)
observation = get(get_url)
show("Search results for " + get_url, observation)

# 2. Find all observations in a video
get_url = "%s/videoreference/%s" % (observation_url, annotation['video_reference_uuid'])
observation = get(get_url)
show("Search results for " + get_url, observation)

# 3.










