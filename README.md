# HALdr #
HAL resource representations builder for spray-json

## Usage ##

Cross-built for Scala 2.10 and 2.11

```sbt
libraryDependencies += "com.github.ancane" % "haldr" % "0.1"
```

```scala
import haldr._
```

### API ###

`Resource` class acts as a hal builder. Main methods are `link` and `embed`.
There is support for uri templates.
You still have to provide `JsonProtocol` for your domain classes.
Additional uri properties may be added with `prop` method on links.
It's still possible to have `String` or spray's `Path` link with `props`.

#### URIs ####

There are 3 string interpolators at your service:

* `u` - absolute uri
* `r` - relative uri (relative to parent resource uri)
* `t` - absolute uri template; checks for [RFC 6570](https://tools.ietf.org/html/rfc6570)) compliance

It possible to append path parts to uris with `/`.

### Simple resource ###

Use `u` and `r` interpolation for DRY links

```scala
Resource(
  Customer("test@mail.com"), u"/customers/deadbeaf")
   .link("books", u"/customers/deadbeaf/books")
   .link("files", u"/customers/deadbeaf/files")
```
```scala
Resource(
  Customer("test@mail.com"), u"/customers/deadbeaf")
  .link("books", r"/books")
  .link("files", r"/files")
```

```json
{
   "email": "test@mail.com",
   "_links": {
      "self": {"href": "/customers/deadbeaf"},
      "books": {"href": "/customers/deadbeaf/books"},
      "files": {"href": "/customers/deadbeaf/files"}
   }
}
```

### Link properties ###

Use `prop` on `u`, `r`, `t` or `String`.

```scala
Resource(
  Customer("test@mail.com"),
    u"/customers/deadbeaf"
      .prop("name", "John Doe")
      .prop("title", "Dear customer"))
```

```json
{
   "email": "test@mail.com",
   "_links": {
      "self": {
          "href": "/customers/deadbeaf",
          "name": "John Doe",
          "title": "Dear customer"
      }
   }
}
```

### Uri template ###

```scala
Resource(Map("email" -> "test@mail.com"), u"/customers/deadbeaf")
  .link("search-by-name", t"/customers?name={name}")
```

```json
{
   "email": "test@mail.com",
   "_links": {
      "self": {"href": "/customers/deadbeaf"},
      "by-name": {
          "href": "/customers?name={name}",
          "templated": true
      }
   }
}
```

### Embedding resource property ###

```scala
Resource(
  Customer("test@mail.com"), u"/customers/deadbeaf")
  .embed("car", Vehicle("BMW", "i3"))
```

```json
{
  "email": "test@mail.com",
  "_embedded": {
     "car": {
        "make": "BMW",
        "model": "i3"
     }
  },
  "_links": {
     "self": {"href": "/customers/deadbeaf"}
  }
}
```

### Embedding related resource ###

```scala
Resource(Customer("test@mail.com"), u"/customers/deadbeaf")
  .embed("car", Resource(Car("BMW", "i3"), u"/customers/deadbeaf/cars/bmw_i3"))
  .link("books", u"/customers/deadbeaf/books")
```

```json
{
  "email": "test@mail.com",
  "_embedded": {
     "car": {
        "make": "BMW",
        "model": "i3",
        "_links": {
           "self": {"href": "/customers/deadbeaf/cars/bmw_i3"}
        }
     }
  },
  "_links": {
     "self": {"href": "/customers/deadbeaf"},
     "books": {"href": "/customers/deadbeaf/books"}
  }
}
```

### Multilevel embedding with relative links ###

`embed` inside another `embed` with relative links.

```scala
Resource(Customer("test@mail.com"), u"/customers/deadbeaf")
  .embed("addressHistory",
    Resource(Address("Argonautenstraat", 3), r"/address/1")
      .link("business", r"/business")
      .embed("locations", Resource(Location(10.0, 12.0), r"/location")
        .link("hide", r"/hide") :: Nil) :: Nil)
```

```json
{
  "email": "test@mail.com",
  "_embedded": {
     "addressHistory": [{
        "street": "Argonautenstraat",
        "house": 3,
        "_embedded": {
            "locations": [{
               "lng": 10.0,
               "lat": 12.0,
               "_links": {
                   "self": { "href" : "/customers/deadbeaf/address/1/location"},
                   "hide": { "href" : "/customers/deadbeaf/address/1/location/hide"}
               }
             }]
        },
        "_links": {
           "self": {"href": "/customers/deadbeaf/address/1"},
           "business": {"href": "/customers/deadbeaf/address/1/business"}
        }
     }]
  },
  "_links": {
      "self":   {"href": "/customers/deadbeaf"}
  }
}
```

### URL encoding ###

`u`, `r` and spray's Path parts are encoded, when String is lifted to spray's Path type.
One important thing to know, is when your path segment contains '/' - correct way to have it encoded is following:

```scala
u"/books" / "Slash / book"
```

```json
"/books/Slash%20%2F%20book"
```
otherwise `/` will be parsed as path separator.

### URL template encoding ###

Put the part into string interpolator arguments, like this:

```scala
val book = "Slash / book"
Resource(Map("email" -> "test@mail.com"), u"/customers/deadbeaf")
  .link("book", t"/books/$book{?format}")
```

```json
{
   "email": "test@mail.com",
   "_links": {
      "self": {"href": "/customers/deadbeaf"},
      "book": {
          "href": "/books/Slash%20%2F%20book?{format}",
          "templated": true
       }
    }
}
```
