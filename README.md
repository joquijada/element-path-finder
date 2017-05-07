# element-path-finder

# Consistent API for searching disparate data formats

By leveraging the Adapter design pattern in Java, this project makes it possible to decouple a client from the underlying format of the read-only data it wishes search on.  All accesses to data are read-only. The calling code will have the data in raw format, and obtain the wrapper that makes the common interface possible, by invoking factory method ContextFactory.INSTANCE.obtainContext(<raw data>), which in turns on a 3rd party API to handle the data format in question to provide implementations for the various operations that interface "com.exsoinn.epf.Context" provides.

Currenly the factory can handle only JSON in either string format or an already constructed JsonElement (see "https://google.github.io/gson/apidocs/"). However it's not that difficult to provide support for other data formats like XML, again by leveraging an existing 3rd party API. It's just a matter of sub-classing "com.exsoinn.epf.AbstractContext" and filling in the missing method implementations in accordance with the operations of the 3rd party API used.


# Examples
Coming soon.