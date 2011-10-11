#!/usr/bin/python
#
# Copyright 2011 James Purser, Christian Ohler, Michael MacFadden
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


""" This script imports issues from Code.Google and builds a csv file.
The intent is to imported the csv file into JIRA.

In order to run the script you will need the following library:

GData: http://code.google.com/p/gdata-python-client/"""

import gdata.projecthosting.client
import os

""" Configuration Values """

project_name = "wave-protocol"
export_file_name = "exported_issues.csv"

"""Mappings between Google Code Values and Jira Values"""
types = {}
types["Defect"] = "Bug"
types["Enhancement"] = "Improvemnet"
types["Patch"] = "Bug"
types["Review"] = "Bug"
types["Task"] = "Task"

priorities = {}
priorities["Critical"] = "Critical"
priorities["High"] = "Major"
priorities["Medium"] = "Minor"
priorities["Low"] = "Trivial"

statuses = {}
statuses["New"] = "Open"
statuses["Accepted"] = "Open"
statuses["Started"] = "In Progress"
statuses["Fixed"] = "Closed"
statuses["Invalid"] = "Closed"
statuses["Duplicate"] = "Closed"
statuses["WontFix"] = "Closed"

resolutions = {}
resolutions["New"] = ""
resolutions["Accepted"] = ""
resolutions["Started"] = ""
resolutions["Fixed"] = "Fixed"
resolutions["Invalid"] = "Invalid"
resolutions["Duplicate"] = "Duplicate"
resolutions["WontFix"] = "Won't Fix"

""" Helper Functions """

def get_comments(issue_id):
    comments = []
    while True:
        comments_query = gdata.projecthosting.client.Query(start_index=len(comments) + 1)
        comments_feed = hosting_client.get_comments(project_name, issue_id, query=comments_query)
        if not comments_feed.entry:
            break
        comments.extend(comments_feed.entry)
        
    comments_text = ""
    
    if len(comments) > 0:
		for comment in comments:
			if len(comments_text) > 0:
			    comments_text += "---\n"
			comments_text += "%s:\n%s\n\n" % (comment.title.text, comment.content.text)
			updates = comment.updates
			if updates:
				if updates.summary:
					comments_text += "Summary: %s\n\n" % updates.summary.text
				if updates.status:
					comments_text += "Status: %s\n\n" % updates.status.text
				if updates.ownerUpdate:
					comments_text += "OwnerUpdate: %s\n\n" % updates.ownerUpdate.text
				for label in updates.label:
					comments_text += "Label: %s\n\n" % label.text
				for ccUpdate in updates.ccUpdate:
					comments_text += "CcUpdate: %s\n\n" % ccUpdate.text	
    return comments_text

def escape(s):
    return '"' + s.encode('utf-8').replace('"', '""') + '"'

def get_id(issue):
    return issue.get_id().split('/')[-1]

def get_link(issue):
    return "http://code.google.com/p/%s/issues/detail?id=%s" % (project_name, get_id(issue))

def get_description(issue):
    # TODO: The content seems to be HTML, I'm not sure if that's what JIRA expects.
    description = issue.content.text
    description += "\n\n---\nIssue imported from %s\n\n" % issue_link
    if issue.owner:
        description += "Owner: %s\n" % issue.owner.username.text
    for cc in issue.cc:
        description += "Cc: %s\n" % cc.username.text
    for label in issue.label:
        description += "Label: %s\n" % label.text
    if issue.stars:
        description += "Stars: %s\n" % issue.stars.text
    if issue.state:
        description += "State: %s\n" % issue.state.text
    if issue.status:
        description += "Status: %s\n" % issue.status.text
    return description
    
def convert_value(list,value):
	if len(value) > 0:
	    return list[value]
	else:
		return ""

""" Export Process """

hosting_client = gdata.projecthosting.client.ProjectHostingClient()
export_file = open(export_file_name, "w")

# First row with column titles
export_file.write("Id,Summary,Issue Type,Priority,Status,Resolution,Description,Comment Body\n")

print "Exporting issues to: " + os.path.abspath(export_file_name)

issues_total = 0
while True:
    issues_query = gdata.projecthosting.client.Query(start_index=issues_total + 1)
    issues_feed = hosting_client.get_issues(project_name, query=issues_query)
    if not issues_feed.entry:
        break
    for issue in issues_feed.entry:
        issue_id = get_id(issue)
        issue_link = get_link(issue)
        print "Exporting Issue %s: %s" % (issue_id, issue_link)
        
        # Append one row per issue

        # Id, e.g. http://code.google.com/feeds/issues/p/wave-protocol/issues/full/1
        row = escape(issue_id) + ","
        
        # Title
        row += escape(issue.title.text) + ","
        
        # Fetch the type and priority from the labels
    	# There are any number of labels, but we are only interested in two.  Namely 
    	# Type-* and Priority-*
        type = "";
        priority = ""; 
        for label in issue.label:
            if label.text.startswith("Type-"):
                type = label.text[5:]
            elif label.text.startswith("Priority-"):
                priority = label.text[9:]
        row += escape(convert_value(types,type)) + ","
        row += escape(convert_value(priorities,priority)) + ","
        
        # Google Code only has the concept of status, while Jira has status and resolution.
        # Status, e.g. Open or Closed
        row += escape(convert_value(statuses,issue.status.text)) + ","
        # Status, e.g. Fixed, Invalid, etc
        row += escape(convert_value(resolutions,issue.status.text)) + ","
        
		# Fetch the description text
        description = get_description(issue)
        row += escape(description) + ","
        
        # Fetch all the comments and put them in a single column
        comments = get_comments(issue_id)
        row += escape(comments)
        
        export_file.write(row + "\n")
    issues_total += len(issues_feed.entry)

export_file.close()
print "\n%s issues exported" % issues_total
