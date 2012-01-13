# Conrad

Conrad is a REST-like HTTP API for administering apps in the Discovery
Environment.  These are the back-end services for Belphegor, the app
administration console for the Discovery Environment.

## Deploying Conrad

Conrad is currently packaged as a WAR file, so it can be deployed in any
existing servlet container.  This may change at some point in the future, but
for the time being a pre-existing servlet container is required.

## Configuring Conrad

The primary configuration settings for Conrad are stored in a properties file
inside the WAR file.  The path is WEB-INF/classes/conrad.properties.  By
default, the file will look like this:

    # The database vendor ("postgresql" or "mysql")
    conrad.db.vendor=postgresql
    
    # The database connection settings.
    conrad.db.host=localhost
    conrad.db.port=5432
    conrad.db.name=de
    conrad.db.user=user
    conrad.db.password=password
    
    # Expire connections after the specified number of minutes.
    conrad.db.max-idle-minutes=180
    
    # The settings to use for proxy authentication.
    conrad.cas.server=https://cas-server-hostname/cas/
    conrad.server-name=https://local-hostname

Generally, the only database settings that will have to be changed are
the database connection settings.  Since the discovery environment
currently always uses PostgreSQL, the vendor should never have to be
changed.  The rest of the database settings are fairly
self-explanatory.

The CAS settings deserve some explanation.  conrad.cas.server refers
to the base URL of the CAS server.  This URL will be used when the
services are validating CAS proxy tickets.  conrad.server-namne refers
to the name on the server on which conrad is deployed.  Typically,
this should be an HTTPS URL, but HTTP URLs are acceptable for testing
when signed certificates are not available.

The logging settings are stored in WEB-INF/classes/log4j.properties inside the
WAR file.  By default, the file will look like this:

    log4j.rootLogger=WARN, A, B
    
    log4j.appender.B=org.apache.log4j.ConsoleAppender
    log4j.appender.B.layout=org.apache.log4j.PatternLayout
    log4j.appender.B.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n
    
    log4j.appender.A=org.apache.log4j.FileAppender
    log4j.appender.A.File=${catalina.home}/logs/conrad.log
    log4j.appender.A.layout=org.apache.log4j.PatternLayout
    log4j.appender.A.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n

See [the log4j documentation](http://logging.apache.org/log4j/1.2/manual.html)
for logging configuration instructions.

## Services

All URLs referenced below are listed as relative URLs with value names enclosed
in braces.  For example, the service to retrieve the list of applications in a
category is accessed using the URL, /get-apps-in-group/{group-id}, where
{group-id} refers to the UUID used to identify the application group.  On the
other hand, all examples use fully qualified URLs.

All request and response bodies are in JSON format.  To avoid confusion between
the braces used to denote JSON objects and braces used to denote example values,
example values in JSON bodies are not enclosed in braces, but instead listed as
hyphen-separated-names without enclosing quotes.

The terms, "group" and "category," both refer to an application category and are
treated synonymously in the text below.

### Verifying that Conrad is Running

Endpoint: GET /

The root path in Conrad can be used to verify that Conrad is actually running.
Currently, the response to this URL contains only a welcome message.  The
response may be enhanced to include usage instructions or status information at
some time in the future if it proves to be helpful.  Here's an example:

    dennis$ curl -s http://hostname:14444/conrad/
    Welcome to Conrad!

### Listing App Groups

Endpoint: GET /get-app-groups

The /get-app-groups endpoint is used to obtain a hierarchical list of public app
categories.  The output from this service is very similar to the output from the
Discovery Environment's /get-only-analysis-groups/{workspace-id} endpoint.
Here's an example:

    dennis$ curl -s http://hostname:14444/conrad/get-app-groups | python -mjson.tool
    {
        "groups": [
            {
                "description": "", 
                "groups": [
                    {
                        "description": "", 
                        "groups": [], 
                        "id": "g5401bd146c144470aedd57b47ea1b979", 
                        "is_public": true, 
                        "name": "Beta", 
                        "template_count": 8
                    },
                    ...
                ], 
                "id": "g12c7a585ec233352e31302e323112a7ccf18bfd7364", 
                "is_public": true, 
                "name": "Public Applications", 
                "template_count": 82
            }
        ]
    }

### Listing Apps in a Category

Endpoint: GET /get-analyses-in-group/{group-id}

The /get-apps-in-group/{group-id} endpoint is used to obtain a list of apps in
an analysis group.  The output from this service is very similar to the output
from the Discovery Environment's /get-analyses-in-group/{group-id} endpoint.
Here's an example:

    dennis$ curl -s http://hostname:14444/conrad/get-apps-in-group/8AC3DD03-B5EE-453A-832E-EED848A2D3CB | python -mjson.tool
    {
        "description": null, 
        "hid": 189, 
        "id": "8AC3DD03-B5EE-453A-832E-EED848A2D3CB", 
        "is_public": true, 
        "name": "Simple", 
        "template_count": 1, 
        "templates": [
            {
                "deployed_components": [
                    {
                        "attribution": "Naim Matasci", 
                        "description": "", 
                        "id": "C1D46AFE-02EC-4AB7-B6BE-D190A08B026E", 
                        "location": "/usr/local2/bin", 
                        "name": "run_muscle.pl", 
                        "type": "executable", 
                        "version": ""
                    }
                ], 
                "description": "MUltiple Sequence Comparison by Log- Expectation for DNA or protein sequences", 
                "id": "F50FE07D-91AA-AA36-1D47-BB2E7FDC7BB4", 
                "integrator_email": "smckay@iplantcollaborative.org", 
                "integrator_name": "Sheldon McKay", 
                "is_favorite": false, 
                "is_public": true, 
                "name": "MUSCLE", 
                "pipeline_eligibility": {
                    "is_valid": true, 
                    "reason": ""
                }, 
                "rating": {
                    "average": 1.0
                }, 
                "suggested_categories": [], 
                "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/MUSCLE"
            }
        ], 
        "workspace_id": 0
    }

### Updating an App

Endpoint: POST /update-app

Much of the information included in the app listing can be updated using the
/update-app endpoint.  The post body of this service contains the app ID along
with the new values to be included in the app listing:

    {
        "id": application-id,
        "name": new-application-name,
        "description": new-application-description,
        "integration_date": new-integration-date,
        "wiki_url": new-documentation-url,
        "integrator_name": new-integrator-name,
        "integrator_email": new-integrator-email-address
    }

Upon success, the response body contains a success flag along with a complete
listing of the updated app.  Upon failure, the response body contains a success
flag along with a brief description of the reason for the failure.

Here are some examples:

    dennis$ curl -sd '
    {
        "id": "EB6EDA51-1D5C-4466-8901-E0194D5D2A53",
        "name": "COLLAR",
        "description": "Description of COLLAR",
        "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/COLLAR",
        "integrator_name": "Scooby Dooby Doo",
        "integrator_email": "scooby@iplantcollaborative.org"
    }
    ' http://hostname:14444/conrad/update-app | python -mjson.tool
    {
        "application": {
            "deployed_components": [
                {
                    "attribution": "", 
                    "description": "generic script for submitting jobs to the Foundational API", 
                    "id": "c0d7c27ee23784641a568dc9cbdc4e11d", 
                    "location": "/usr/bin", 
                    "name": "foundational_api_adapter.pl", 
                    "type": "fAPI", 
                    "version": "0.0.1"
                }
            ], 
            "description": "Description of COLLAR", 
            "id": "EB6EDA51-1D5C-4466-8901-E0194D5D2A53", 
            "integrator_email": "scooby@iplantcollaborative.org", 
            "integrator_name": "Scooby Dooby Doo", 
            "is_favorite": false, 
            "is_public": true, 
            "name": "COLLAR", 
            "pipeline_eligibility": {
                "is_valid": true, 
                "reason": ""
            }, 
            "rating": {
                "average": 0.0
            }, 
            "suggested_categories": [], 
            "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/COLLAR"
        }, 
        "success": true
    }

    dennis$ curl -sd '
    {
        "id": "Foo",
        "name": "COLLAR",
        "description": "Description of COLLAR",
        "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/COLLAR",
        "integrator_name": "Scooby Dooby Doo",
        "integrator_email": "scooby@iplantcollaborative.org"
    }
    ' http://hostname:14444/conrad/update-app | python -mjson.tool
    {
        "reason": "app, Foo, not found", 
        "success": false
    }

This service will fail under the following circumstances:

* the application with the specified identifier can't be found;
* a database error occurs.

Validation of the fields in the request body is not currently being done.  if
these services are exposed to direct access by users at some point in the future
then validation will be added.

### Renaming a Category

Endpoint: POST /rename-category

An application category can be renamed using the /rename-category endpoint.  The
request body for this service should contain a JSON object that specifies the
category identifier and the new category name:

    {
        "categoryId": category-id,
        "name": category-name
    }

The response body is a JSON object containing a success flag along with the new
category name.

Here are some examples:

    dennis$ curl -sd '
    {
        "categoryId": "g5401bd146c144470aedd57b47ea1b979",
        "name": "Etabay"
    }
    ' http://hostname:14444/conrad/rename-category | python -mjson.tool
    {
        "name": "Etabay", 
        "success": true
    }

    dennis$ curl -sd '
    {
        "categoryId": "Foo",
        "name": "Etabay"
    }
    ' http://hostname:14444/conrad/rename-category | python -mjson.tool
    {
        "reason": "category, Foo, does not exist", 
        "success": false
    }

This service will fail under the following circumstances:

* the category ID is not specified in the request body;
* the new category name is not specified in the request body;
* the category with the specified identifier can't be found;
* a database error occurs.

### Deleting a Category

Endpoint: DELETE /category/{category-id}

An application category can be deleted by sending an HTTP DELETE request to the
/category/{category-id} endpoint.  This service takes no request body.  Upon
success, the response body contains a success flag along with the category
identifier.  Upon failure, the response body contains a success flag along with
a brief description of the reason for the failure.

Here are some examples:

    dennis$ curl -sXDELETE http://hostname:14444/conrad/category/6FB48BDB-B034-48CE-8242-096525F50662 | format_json
    {
       "success" : true,
       "categoryId" : "6FB48BDB-B034-48CE-8242-096525F50662"
    }
    
    dennis$ curl -sXDELETE http://hostname:14444/conrad/category/g5401bd146c144470aedd57b47ea1b979 | format_json
    {
       "success" : false,
       "reason" : "category, g5401bd146c144470aedd57b47ea1b979, contains apps"
    }

This service will fail under the following circumstances:

* a category with the specified identifier can't be found;
* the specified category contains apps;
* the specified category contains subgroups;
* a database error occurs.

### Creating a New Category

Endpoint PUT /category

An application category can be created by sending an HTTP PUT request to the
/category endpoint.  The request body for this service should contain the
identifier of the parent category, the name of the new category, and an optional
description of the new category:

    {
        "parentCategoryId": parent-category-id,
        "name": new-category-name,
        "description": new-category-description
    }

Upon success, the response body contains a success flag along with a complete
listing of the newly created category.  Upon failure, the response body contains
a success flag along with a brief description of the reason for the failure.
Here are some examples:

    dennis$ curl -sXPUT -d '
    {
        "parentCategoryId": "g12c7a585ec233352e31302e323112a7ccf18bfd7364",
        "name": "Foo"
    }
    ' http://hostname:14444/conrad/category | python -mjson.tool
    {
        "category": {
            "description": "", 
            "hid": 244, 
            "id": "FF7155FC-132B-4AAB-9FBB-57DC40DC572B", 
            "is_public": true, 
            "name": "Foo", 
            "template_count": 0, 
            "templates": [], 
            "workspace_id": 0
        }, 
        "success": true
    }
    
    dennis$ curl -sXPUT -d '
    {
        "parentCategoryId": "g12c7a585ec233352e31302e323112a7ccf18bfd7364",
        "name": "Foo"
    }
    ' http://hostname:14444/conrad/category | python -mjson.tool
    {
        "reason": "category, g12c7a585ec233352e31302e323112a7ccf18bfd7364, already contains a subcategory named, \"Foo\"", 
        "success": false
    }

This service will fail under the following circumstances:

* the parent category ID is not specified in the request body;
* the new categbory name is not specified in the request body;
* the parent category can't be found;
* the parent category directly contains apps;
* the parent category already contains a subcategory with the specified name;
* a database error occurs.

### Deleting an App

Endpoint: DELETE /app/{app-id}

An app can be logically deleted by sending a DELETE request to the /app/{app-id}
endpoint.  This service takes no request body.  Upon success, this service
returns a success flag along with the identifier of the application that was
deleted.  Upon failure, this service returns a success flag along with a brief
description of the reason for the failure.

Here are some examples:

    dennis$ curl -sXDELETE http://hostname:14444/conrad/app/2BB00471-502E-42DE-A57E-9B516CEA1493 | python -mjson.tool
    {
        "id": "2BB00471-502E-42DE-A57E-9B516CEA1493", 
        "success": true
    }
    
    dennis$ curl -sXDELETE http://hostname:14444/conrad/app/Foo | python -mjson.tool
    {
        "reason": "app, Foo, not found", 
        "success": false
    }

This service will fail under the following circumstances:

* an app with the specified identifier can't be found;
* a database error occurs.

### Moving an App

Endpoint: POST /move-app

An app can be moved to a new category by sending an HTTP POST request to the
/move-app endpoint.  The request body should contain a JSON object with fields
containing the application ID and the identifier of the new category:

    {
        "id": app-id
        "categoryId": category-id
    }

Upon success, this service returns a JSON object containing a success flag and a
complete listing of the new parent category.  Upon failure, this service returns
a JSON object containing a success flag and a brief description of the reason
for the failure.

Here are some examples:

    dennis$ curl -sd '
    {
        "id": "EB6EDA51-1D5C-4466-8901-E0194D5D2A53",
        "categoryId": "06DFCE72-BC04-4556-9659-B4D87A471947"
    }
    ' http://hostname:14444/conrad/move-app | python -mjson.tool
    {
        "category": {
            "description": null, 
            "hid": 168, 
            "id": "06DFCE72-BC04-4556-9659-B4D87A471947", 
            "is_public": true, 
            "name": "Utility Tool", 
            "template_count": 10, 
            "templates": [
                {
                    "deployed_components": [
                        {
                            "attribution": "Wrapper for GNU grep. iPlant DE tool developed by Matt Vaughn (vaughn at iplantcollaborative dot org).", 
                            "description": "Grep", 
                            "id": "c58d961d2d2434e2c824401e121c6f90c", 
                            "location": "/usr/local3/bin/grep_tool-1.00/", 
                            "name": "grep_wrapper.sh", 
                            "type": "executable", 
                            "version": "1.00"
                        }
                    ], 
                    "description": "Identify and extract matching lines from a file", 
                    "id": "EE8A6728-FEEE-F94C-ECCD-59C5DB1791B8", 
                    "integrator_email": "vaughn@iplantcollaborative.org", 
                    "integrator_name": "Matt Vaughn", 
                    "is_favorite": false, 
                    "is_public": true, 
                    "name": "Find Lines Matching a Regular Expression", 
                    "pipeline_eligibility": {
                        "is_valid": true, 
                        "reason": ""
                    }, 
                    "rating": {
                        "average": 5.0
                    }, 
                    "suggested_categories": [], 
                    "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Find%20Lines%20Matching%20a%20Regular%20Expression"
                },
                ...
            ], 
            "workspace_id": 0
        }, 
        "success": true
    }
    
    dennis$ curl -sd '
    {
        "id": "Foo",          
        "categoryId": "06DFCE72-BC04-4556-9659-B4D87A471947"
    }
    ' http://hostname:14444/conrad/move-app | python -mjson.tool
    {
        "reason": "app, Foo, not found", 
        "success": false
    }

This service will fail under the following circumstances:

* the application identifier is not specified in the request body;
* the destination category identifier is not specified in the request body;
* the specified application can't be found;
* the specified destination category can't be found;
* the specified destination category contains subcategories;
* a database error occurs.

### Moving a Category

Endpoint: POST /move-category

A category can be moved to a new parent category by sending an HTTP POST request
to the /move-category endpoint.  The request body should contain a JSON object
containing the identifier of the category to move along with the identifier of
the new parent category:

    {
        "categoryId": category-id,
        "parentCategoryId": new-parent-category-id
    }

Upon success, this service returns a success flag along with a complete category
listing.  Upon failure, this service returns a success flag along with a brief
description of the reason for the failure.

Here are some examples:

    dennis$ curl -sd '
    {
        "categoryId": "A8D08BAA-D930-4178-9647-2A17DB17E309",
        "parentCategoryId": "g12c7a585ec233352e31302e323112a7ccf18bfd7364"
    }
    ' http://hostname:14444/conrad/move-category | python -mjson.tool
    {
        "categories": {
            "groups": [
                {
                    "description": "", 
                    "groups": [
                        {
                            "description": null, 
                            "groups": [
                                {
                                    "description": null, 
                                    "groups": [], 
                                    "id": "DE2F3D2B-1D0B-4A2D-BD6C-EBDE2F7E974A", 
                                    "is_public": true, 
                                    "name": "Aligners", 
                                    "template_count": 5
                                },
                            ]
                        },
                        ...
                    ], 
                    "id": "g12c7a585ec233352e31302e323112a7ccf18bfd7364", 
                    "is_public": true, 
                    "name": "Public Applications", 
                    "template_count": 82
                }
            ]
        }, 
        "success": true
    }
    
    dennis$ curl -sd '
    {
        "categoryId": "A8D08BAA-D930-4178-9647-2A17DB17E309",
        "parentCategoryId": "Foo"                                        
    }
    ' http://hostname:14444/conrad/move-category | python -mjson.tool
    {
        "reason": "category, Foo, does not exist", 
        "success": false
    }

This service will fail under the following circumstances:

* the child category identifier is not specified in the request body;
* the parent category identifier is not specified in the request body;
* the child category can't be found;
* the parent category can't be found;
* the parent category contains apps;
* the parent category is a descendent of the child category;
* a database error occurs.

### Unrecognized Service Paths

If an unrecognized service path is used in a request to Conrad then the response
will contain a success flag along with a message indicating that the service
path is unrecognized:

    dennis$ curl -s http://hostname:14444/conrad/foo | python -mjson.tool
    {
        "reason": "unrecognized service path", 
        "success": false
    }
