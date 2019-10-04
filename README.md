# element-path-finder

[![Build Status](https://travis-ci.com/joquijada/element-path-finder.svg?branch=master)](https://travis-ci.com/joquijada/element-path-finder)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=joquijada_element-path-finder&metric=alert_status)](https://sonarcloud.io/dashboard?id=joquijada_element-path-finder)


# Consistent API for searching disparate data formats

By leveraging the Adapter and Factory design patterns in Java, this project makes it possible to decouple a client from the underlying format of the data it wishes search on.  This project provides a way to wrap a data object of some format (E.g. JSON, XML, etc...), inside another object of type Context, which provides consistent API interface to search the underlying data object. The only work involved is creating a child class of AbstractContext.java in order to handle other data formats. Currently the project only provides an implementation for handling JSON, in JsonContext.java, and by using 3rd party Google JSON API (see "https://google.github.io/gson/apidocs/").

The calling code will have the data in raw format, and obtain the wrapper that makes the common interface possible, by invoking factory method ContextFactory.INSTANCE.obtainContext(<raw data>), which in turns relies on a 3rd party API to handle the data format in question to provide implementations for the various operations that interface "com.exsoinn.epf.Context" puts forth.

Currently the factory can handle only JSON in either string format or an already constructed JsonElement (see "https://google.github.io/gson/apidocs/"). However it's not that difficult to provide support for other data formats like XML, again by leveraging an existing 3rd party API. It's just a matter of sub-classing "com.exsoinn.epf.AbstractContext" and filling in the missing method implementations in accordance with the operations of the 3rd party API used.


# Examples
First thing you'd want to do is to wrap the client's data in a Context object. You do this by invoking factory method ContextFactory.INSTANCE.obtainContext(<raw data>). Only JSON data, either a java.lang.String or an already created com.google.gson.JsonElement object, will be recognized. Anything else will throw exception, because implementations to handle other data formats are not available yet.

To search for an element, examples will be provided soon.
